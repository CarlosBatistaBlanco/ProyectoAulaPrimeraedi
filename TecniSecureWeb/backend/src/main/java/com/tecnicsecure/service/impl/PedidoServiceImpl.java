/*
 * Este archivo contiene las reglas principales para procesar una compra desde que el cliente
 * presiona Confirmar pedido en el carrito. Revisa que existan unidades disponibles en bodega,
 * calcula los precios y descuentos reales de la tienda, descuenta el inventario, controla cada
 * cambio de estado hasta la entrega y avisa al cliente en su panel de Mis pedidos.
 */
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

    // Prepara el acceso a pedidos, clientes, catalogo, inventario y avisos al cliente.
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

    // Procesa la compra completa con precios oficiales de la tienda; si falta mercancia o algo falla a mitad de camino, todo se deshace para no cobrar ni descontar de mas.
    @Override
    @Transactional
    public PedidoResponse registrar(PedidoRequest request) {
        // Identifica al cliente conectado, o busca/crea su ficha usando el correo si compra sin cuenta.
        Cliente cliente = resolverCliente(request);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setDireccionEntrega(StringUtils.hasText(request.direccion()) ? request.direccion().trim() : cliente.getDireccion());
        pedido.setLatitud(request.latitud());
        pedido.setLongitud(request.longitud());
        pedido.setMetodoPago(StringUtils.hasText(request.metodoPago()) ? request.metodoPago().trim().toLowerCase() : "contraentrega");
        pedido.setDetallePago(StringUtils.hasText(request.detallePago()) ? request.detallePago().trim() : null);

        // Si un empleado registra una venta presencial o por telefono, queda anotado como el vendedor que atendio.
        Integer idUsuario = usuarioActual.idOpcional();
        if (idUsuario != null && usuarioActual.esEmpleado()) {
            pedido.setEmpleadoRegistro(usuarioActual.requerir());
        }

        List<PedidoDetalle> lineas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // Revisa cada producto del carrito: confirma que siga a la venta, que haya unidades en bodega y suma el total con el precio real oficial.
        for (PedidoRequest.ItemCarrito item : request.carrito()) {
            Producto producto = productoRepository.findById(item.idProducto())
                    .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                    .orElseThrow(() -> ApiException.badRequest(
                            "Producto no disponible: " + item.idProducto()));

            // Comprueba que haya suficientes unidades en bodega antes de aceptar la compra.
            int cantidad = item.cantidad() == null || item.cantidad() < 1 ? 1 : item.cantidad();
            if (producto.getStock() == null || producto.getStock() < cantidad) {
                throw ApiException.badRequest("Stock insuficiente para: " + producto.getNombre());
            }

            // Toma siempre el precio oficial de la tienda (con su descuento vigente) para evitar alteraciones desde la pantalla.
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

        // Descuenta de la bodega cada producto comprado y deja registrado su movimiento de salida.
        for (PedidoDetalle detalle : lineas) {
            inventarioService.descontarPorVenta(
                    detalle.getProducto().getIdProducto(), detalle.getCantidad(), idUsuario);
        }

        // Anota el estado inicial en la linea de tiempo del pedido y avisa al cliente en su panel.
        registrarHistorial(pedido.getIdPedido(), null, EstadoPedido.PENDIENTE, idUsuario);
        notificarNuevoPedido(pedido);

        return PedidoResponse.de(pedido);
    }

    // Muestra los pedidos en el panel del equipo divididos en paginas, con opcion de filtrar por cliente o por estado.
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

    // Consulta un pedido por su numero verificando primero que realmente pertenezca al cliente conectado.
    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtener(Integer idPedido) {
        Pedido pedido = buscar(idPedido);
        verificarPropietario(pedido);
        return PedidoResponse.de(pedido);
    }

    // Actualiza el estado de un pedido siguiendo el orden permitido; si se cancela devuelve las unidades a bodega y avisa al cliente.
    @Override
    @Transactional
    public PedidoResponse cambiarEstado(Integer idPedido, String nuevoEstado, String observacion, Integer idUsuario) {
        Pedido pedido = buscar(idPedido);
        String destino = nuevoEstado == null ? "" : nuevoEstado.trim().toLowerCase();

        // Revisa que el nuevo estado exista y que tenga sentido pasar a el desde el estado actual.
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

        // Si el pedido pasa a cancelado, devuelve automaticamente todos sus productos al inventario disponible.
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

        // Manda un aviso en vivo a la campanita del cliente contandole en que paso va su pedido.
        if (pedido.getCliente() != null && pedido.getCliente().getUsuario() != null) {
            String detalle = StringUtils.hasText(observacion) ? " " + observacion : "";
            soporteService.notificar(pedido.getCliente().getUsuario().getIdUsuario(),
                    "Tu pedido #" + idPedido + " cambió de estado",
                    "Ahora está: " + destino + "." + detalle,
                    "/panel/cliente/pedidos.html");
        }

        return PedidoResponse.de(pedido);
    }

    // Muestra paso a paso todos los cambios de estado que ha tenido un pedido, verificando que pertenezca al cliente.
    @Override
    @Transactional(readOnly = true)
    public List<PedidoHistorial> historial(Integer idPedido) {
        Pedido pedido = buscar(idPedido);
        // Comprueba que un cliente solo pueda consultar la ruta de sus propios pedidos.
        verificarPropietario(pedido);
        return historialRepository.findByIdPedidoOrderByCreadoEnAsc(idPedido);
    }

    // Muestra unicamente las compras realizadas por el cliente que tiene la sesion abierta.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> misPedidos(Pageable pageable) {
        Usuario usuario = usuarioActual.requerir();
        Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> ApiException.notFound("No tienes un perfil de cliente asociado"));
        return PageResponse.de(pedidoRepository.findByClienteIdCliente(cliente.getIdCliente(), pageable),
                PedidoResponse::de);
    }

    // Encuentra la ficha del cliente conectado o crea una con los datos escritos en el formulario de compra.
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

    // Busca un pedido por su numero y muestra un aviso si no existe.
    private Pedido buscar(Integer idPedido) {
        return pedidoRepository.findById(idPedido)
                .orElseThrow(() -> ApiException.notFound("Pedido no encontrado"));
    }

    // Verifica que el pedido realmente pertenezca a esta persona cuando quien consulta es un cliente.
    private void verificarPropietario(Pedido pedido) {
        // Administradores y empleados pueden revisar cualquier pedido; los clientes unicamente los suyos.
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

    // Anota en la linea de tiempo el estado anterior, el estado nuevo y quien realizo el cambio.
    private void registrarHistorial(Integer idPedido, String anterior, String nuevo, Integer idUsuario) {
        PedidoHistorial historial = new PedidoHistorial();
        historial.setIdPedido(idPedido);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setIdUsuario(idUsuario);
        historialRepository.save(historial);
    }

    // Envia un aviso al panel del cliente confirmandole que su compra fue recibida correctamente.
    private void notificarNuevoPedido(Pedido pedido) {
        if (pedido.getCliente() != null && pedido.getCliente().getUsuario() != null) {
            soporteService.notificar(pedido.getCliente().getUsuario().getIdUsuario(),
                    "Pedido #" + pedido.getIdPedido() + " recibido",
                    "Estamos preparando tu pedido.",
                    "/panel/cliente/pedidos.html");
        }
    }
}
