package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.inventario.KardexProducto;
import com.tecnicsecure.dto.inventario.MovimientoInventarioRequest;
import com.tecnicsecure.dto.inventario.MovimientoResponse;
import com.tecnicsecure.entity.InventarioMovimiento;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.InventarioMovimientoRepository;
import com.tecnicsecure.repository.ProductoRepository;
import com.tecnicsecure.service.InventarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventarioServiceImpl implements InventarioService {

    private static final Set<String> TIPOS_VALIDOS = Set.of("entrada", "salida", "ajuste");

    private final ProductoRepository productoRepository;
    private final InventarioMovimientoRepository movimientoRepository;

    public InventarioServiceImpl(ProductoRepository productoRepository,
                                 InventarioMovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KardexProducto> stockBajo() {
        return productoRepository.findProductosConStockBajo().stream().map(KardexProducto::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KardexProducto> todos() {
        return productoRepository.findAll().stream().map(KardexProducto::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponse> kardex(Integer idProducto) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
        return movimientoRepository.findByIdProductoOrderByCreadoEnDesc(idProducto).stream()
                .map(m -> MovimientoResponse.de(m, producto.getNombre()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponse> ultimosMovimientos() {
        List<InventarioMovimiento> movimientos = movimientoRepository.findTop50ByOrderByCreadoEnDesc();
        Map<Integer, String> nombres = productoRepository.findAll().stream()
                .collect(Collectors.toMap(Producto::getIdProducto, Producto::getNombre, (a, b) -> a));
        return movimientos.stream()
                .map(m -> MovimientoResponse.de(m, nombres.getOrDefault(m.getIdProducto(), "Producto " + m.getIdProducto())))
                .toList();
    }

    @Override
    @Transactional
    public MovimientoResponse registrarMovimiento(Integer idProducto,
                                                  MovimientoInventarioRequest request,
                                                  Integer idUsuario) {
        String tipo = request.tipo() == null ? "" : request.tipo().trim().toLowerCase(Locale.ROOT);
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw ApiException.badRequest("El tipo debe ser entrada, salida o ajuste");
        }

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        int cantidad = request.cantidad() == null ? 0 : request.cantidad();
        int stockAntes = producto.getStock() == null ? 0 : producto.getStock();
        int stockResultante;

        switch (tipo) {
            case "entrada" -> stockResultante = stockAntes + cantidad;
            case "salida" -> {
                if (cantidad > stockAntes) {
                    throw ApiException.badRequest("Stock insuficiente: disponible " + stockAntes);
                }
                stockResultante = stockAntes - cantidad;
            }
            // En un ajuste, la cantidad ingresada es el stock real contado.
            default -> stockResultante = cantidad;
        }

        producto.setStock(stockResultante);
        productoRepository.save(producto);

        InventarioMovimiento movimiento = new InventarioMovimiento();
        movimiento.setIdProducto(idProducto);
        movimiento.setIdUsuario(idUsuario);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo(request.motivo());
        movimientoRepository.save(movimiento);

        return MovimientoResponse.de(movimiento, producto.getNombre());
    }

    @Override
    @Transactional
    public void descontarPorVenta(Integer idProducto, Integer cantidad, Integer idUsuario) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        int stockActual = producto.getStock() == null ? 0 : producto.getStock();
        if (stockActual < cantidad) {
            throw ApiException.badRequest("Stock insuficiente para: " + producto.getNombre());
        }

        int stockResultante = stockActual - cantidad;
        producto.setStock(stockResultante);
        productoRepository.save(producto);

        InventarioMovimiento movimiento = new InventarioMovimiento();
        movimiento.setIdProducto(idProducto);
        movimiento.setIdUsuario(idUsuario);
        movimiento.setTipo("salida");
        movimiento.setCantidad(cantidad);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo("Venta");
        movimientoRepository.save(movimiento);
    }
}
