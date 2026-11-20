package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.pedido.PedidoRequest;
import com.tecnicsecure.dto.pedido.PedidoResponse;
import com.tecnicsecure.entity.*;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.InventarioService;
import com.tecnicsecure.service.PedidoService;
import com.tecnicsecure.service.SoporteService;
import com.tecnicsecure.util.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoHistorialRepository historialRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final InventarioService inventarioService;
    private final SoporteService soporteService;
    private final UsuarioActual usuarioActual;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             PedidoHistorialRepository historialRepository,
                             ProductoRepository productoRepository,
                             ClienteRepository clienteRepository,
                             InventarioService inventarioService,
                             SoporteService soporteService,
                             UsuarioActual usuarioActual) {
        this.pedidoRepository = pedidoRepository;
        this.historialRepository = historialRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.inventarioService = inventarioService;
        this.soporteService = soporteService;
        this.usuarioActual = usuarioActual;
    }

    /**
     * Registra el checkout completo en una unica transaccion:
     * cliente, pedido, detalle, descuento de stock e historial.
     * El precio SIEMPRE se toma de la base de datos, nunca del frontend.
     */
    @Override
    @Transactional
    public PedidoResponse registrar(PedidoRequest request) {
        Cliente cliente = resolverCliente(request);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setDireccionEntrega(StringUtils.hasText(request.direccion()) ? request.direccion().trim() : cliente.getDireccion());
        pedido.setLatitud(request.latitud());
        pedido.setLongitud(request.longitud());
        pedido.setMetodoPago(StringUtils.hasText(request.metodoPago()) ? request.metodoPago().trim().toLowerCase() : "contraentrega");
        pedido.setDetallePago(StringUtils.hasText(request.detallePago()) ? request.detallePago().trim() : null);

        Integer idUsuario = usuarioActual.idOpcional();
        if (idUsuario != null && usuarioActual.esEmpleado()) {
            pedido.setEmpleadoRegistro(usuarioActual.requerir());
        }

        List<PedidoDetalle> lineas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (PedidoRequest.ItemCarrito item : request.carrito()) {
            Producto producto = productoRepository.findById(item.idProducto())
                    .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                    .orElseThrow(() -> ApiException.badRequest(
                            "Producto no disponible: " + item.idProducto()));

            int cantidad = item.cantidad() == null || item.cantidad() < 1 ? 1 : item.cantidad();
            if (producto.getStock() == null || producto.getStock() < cantidad) {
                throw ApiException.badRequest("Stock insuficiente para: " + producto.getNombre());
            }

            BigDecimal precioUnitario = producto.getPrecioFinal();
            PedidoDetalle detalle = new PedidoDetalle();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            pedido.agregarDetalle(detalle);
            lineas.add(detalle);
            total = total.add(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
        }

        if (lineas.isEmpty()) {
            throw ApiException.badRequest("Ningún producto del carrito es válido.");
        }

        pedido.setTotal(total);
        pedidoRepository.save(pedido);

        for (PedidoDetalle detalle : lineas) {
            inventarioService.descontarPorVenta(
                    detalle.getProducto().getIdProducto(), detalle.getCantidad(), idUsuario);
        }

        registrarHistorial(pedido.getIdPedido(), null, EstadoPedido.PENDIENTE, idUsuario);
        notificarNuevoPedido(pedido);

        return PedidoResponse.de(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> listar(Integer idCliente, String estado, Pageable pageable) {
        if (idCliente != null) {
            return PageResponse.de(pedidoRepository.findByClienteIdCliente(idCliente, pageable), PedidoResponse::de);
        }
        if (StringUtils.hasText(estado)) {
            return PageResponse.de(pedidoRepository.findByEstado(estado, pageable), PedidoResponse::de);
        }
        return PageResponse.de(pedidoRepository.findAll(pageable), PedidoResponse::de);
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtener(Integer idPedido) {
        Pedido pedido = buscar(idPedido);
        verificarPropietario(pedido);
        return PedidoResponse.de(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse cambiarEstado(Integer idPedido, String nuevoEstado, String observacion, Integer idUsuario) {
        Pedido pedido = buscar(idPedido);
        String destino = nuevoEstado == null ? "" : nuevoEstado.trim().toLowerCase();

        if (!EstadoPedido.esValido(destino)) {
            throw ApiException.badRequest("Estado no válido: " + nuevoEstado
                    + ". Valores permitidos: " + String.join(", ", EstadoPedido.TODOS));
        }
        if (!EstadoPedido.permiteTransicion(pedido.getEstado(), destino)) {
            throw ApiException.badRequest(
                    "No se puede pasar de '" + pedido.getEstado() + "' a '" + destino + "'");
        }

        String anterior = pedido.getEstado();
        pedido.setEstado(destino);
        pedidoRepository.save(pedido);
        registrarHistorial(idPedido, anterior, destino, idUsuario);

        // Si se cancela el pedido, reintegrar automáticamente las unidades al inventario
        if (EstadoPedido.CANCELADO.equals(destino) && !EstadoPedido.CANCELADO.equals(anterior)
                && pedido.getDetalles() != null) {
            for (PedidoDetalle d : pedido.getDetalles()) {
                if (d.getProducto() != null && d.getProducto().getIdProducto() != null) {
                    inventarioService.reintegrarPorCancelacion(
                            d.getProducto().getIdProducto(),
                            d.getCantidad(),
                            "Reingreso por cancelacion pedido #" + idPedido,
                            idUsuario);
                }
            }
        }

        if (pedido.getCliente() != null && pedido.getCliente().getUsuario() != null) {
            String detalle = StringUtils.hasText(observacion) ? " " + observacion : "";
            soporteService.notificar(pedido.getCliente().getUsuario().getIdUsuario(),
                    "Tu pedido #" + idPedido + " cambió de estado",
                    "Ahora está: " + destino + "." + detalle,
                    "/panel/cliente/pedidos.html");
        }

        return PedidoResponse.de(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoHistorial> historial(Integer idPedido) {
        Pedido pedido = buscar(idPedido);
        verificarPropietario(pedido);
        return historialRepository.findByIdPedidoOrderByCreadoEnAsc(idPedido);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> misPedidos(Pageable pageable) {
        Usuario usuario = usuarioActual.requerir();
        Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> ApiException.notFound("No tienes un perfil de cliente asociado"));
        return PageResponse.de(pedidoRepository.findByClienteIdCliente(cliente.getIdCliente(), pageable),
                PedidoResponse::de);
    }

    private Cliente resolverCliente(PedidoRequest request) {
        Integer idUsuario = usuarioActual.idOpcional();
        if (idUsuario != null) {
            var clienteDeSesion = clienteRepository.findByUsuarioIdUsuario(idUsuario);
            if (clienteDeSesion.isPresent()) {
                return clienteDeSesion.get();
            }
        }

        String correo = request.correo().trim().toLowerCase();
        return clienteRepository.findByCorreoIgnoreCase(correo).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setNombre(request.nombre().trim());
            nuevo.setCorreo(correo);
            nuevo.setTelefono(request.telefono());
            nuevo.setDireccion(request.direccion());
            return clienteRepository.save(nuevo);
        });
    }

    private Pedido buscar(Integer idPedido) {
        return pedidoRepository.findById(idPedido)
                .orElseThrow(() -> ApiException.notFound("Pedido no encontrado"));
    }

    /** Un cliente solo puede ver sus propios pedidos. */
    private void verificarPropietario(Pedido pedido) {
        if (!usuarioActual.esCliente()) {
            return;
        }
        Usuario usuario = usuarioActual.requerir();
        if (pedido.getCliente() == null
                || pedido.getCliente().getUsuario() == null
                || !usuario.getIdUsuario().equals(pedido.getCliente().getUsuario().getIdUsuario())) {
            throw ApiException.forbidden("Este pedido pertenece a otro cliente");
        }
    }

    private void registrarHistorial(Integer idPedido, String anterior, String nuevo, Integer idUsuario) {
        PedidoHistorial historial = new PedidoHistorial();
        historial.setIdPedido(idPedido);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setIdUsuario(idUsuario);
        historialRepository.save(historial);
    }

    private void notificarNuevoPedido(Pedido pedido) {
        if (pedido.getCliente() != null && pedido.getCliente().getUsuario() != null) {
            soporteService.notificar(pedido.getCliente().getUsuario().getIdUsuario(),
                    "Pedido #" + pedido.getIdPedido() + " recibido",
                    "Estamos preparando tu pedido.",
                    "/panel/cliente/pedidos.html");
        }
    }
}
