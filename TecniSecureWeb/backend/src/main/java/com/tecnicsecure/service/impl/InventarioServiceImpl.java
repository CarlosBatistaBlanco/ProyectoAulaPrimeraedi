/*
 * Este archivo controla las existencias reales de la bodega: revisa que productos estan por
 * agotarse, registra cada entrada, salida o conteo fisico manual, descuenta unidades cuando se
 * confirma una compra y las devuelve a bodega si un pedido se cancela. Su trabajo se nota en
 * el panel de inventario y en cada compra o cancelacion de la tienda.
 */
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

    // Prepara el acceso a los productos en bodega y a su historial de entradas y salidas.
    public InventarioServiceImpl(ProductoRepository productoRepository,
                                 InventarioMovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // Muestra los productos que conviene reponer pronto porque sus unidades llegaron o bajaron del minimo configurado.
    @Override
    @Transactional(readOnly = true)
    public List<KardexProducto> stockBajo() {
        return productoRepository.findProductosConStockBajo().stream().map(KardexProducto::de).toList();
    }

    // Entrega el listado completo de productos con la cantidad exacta que hay hoy en bodega.
    @Override
    @Transactional(readOnly = true)
    public List<KardexProducto> todos() {
        return productoRepository.findAll().stream().map(KardexProducto::de).toList();
    }

    // Muestra el historial detallado de todas las entradas, salidas y ajustes que ha tenido un producto.
    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponse> kardex(Integer idProducto) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
        return movimientoRepository.findByIdProductoOrderByCreadoEnDesc(idProducto).stream()
                .map(m -> MovimientoResponse.de(m, producto.getNombre()))
                .toList();
    }

    // Muestra los 50 movimientos de entrada y salida mas recientes de toda la bodega.
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

    // Registra un movimiento manual (entrada, salida o ajuste por conteo fisico) y actualiza el saldo en bodega en un solo paso seguro.
    @Override
    @Transactional
    public MovimientoResponse registrarMovimiento(Integer idProducto,
                                                  MovimientoInventarioRequest request,
                                                  Integer idUsuario) {
        // Comprueba que el tipo elegido sea unicamente entrada, salida o ajuste.
        String tipo = request.tipo() == null ? "" : request.tipo().trim().toLowerCase(Locale.ROOT);
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw ApiException.badRequest("El tipo debe ser entrada, salida o ajuste");
        }

        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        int cantidad = request.cantidad() == null ? 0 : request.cantidad();
        int stockAntes = producto.getStock() == null ? 0 : producto.getStock();
        int stockResultante;

        // Calcula cuantas unidades quedaran en bodega segun el tipo de movimiento.
        switch (tipo) {
            case "entrada" -> stockResultante = stockAntes + cantidad;
            case "salida" -> {
                // Impide retirar mas unidades de las que realmente existen para no quedar con inventario negativo.
                if (cantidad > stockAntes) {
                    throw ApiException.badRequest("Stock insuficiente: disponible " + stockAntes);
                }
                stockResultante = stockAntes - cantidad;
            }
            // Si es un ajuste manual, el numero escrito reemplaza directamente al conteo real en bodega.
            default -> stockResultante = cantidad;
        }

        // Guarda las nuevas unidades disponibles y deja anotado el comprobante del movimiento en el historial.
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

    // Descuenta automaticamente las unidades vendidas al confirmar un pedido y anota la salida en el historial de bodega.
    @Override
    @Transactional
    public void descontarPorVenta(Integer idProducto, Integer cantidad, Integer idUsuario) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado"));

        // Si no alcanzan las unidades en bodega, detiene la venta para no prometer mercancia agotada.
        int stockActual = producto.getStock() == null ? 0 : producto.getStock();
        if (stockActual < cantidad) {
            throw ApiException.badRequest("Stock insuficiente para: " + producto.getNombre());
        }

        int stockResultante = stockActual - cantidad;
        producto.setStock(stockResultante);
        productoRepository.save(producto);

        // Anota la salida por venta para que las cuentas de bodega siempre cuadren.
        InventarioMovimiento movimiento = new InventarioMovimiento();
        movimiento.setIdProducto(idProducto);
        movimiento.setIdUsuario(idUsuario);
        movimiento.setTipo("salida");
        movimiento.setCantidad(cantidad);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo("Venta");
        movimientoRepository.save(movimiento);
    }

    // Devuelve las unidades a la bodega y anota el reingreso cuando se cancela un pedido o se emite una nota credito.
    @Override
    @Transactional
    public void reintegrarPorCancelacion(Integer idProducto, Integer cantidad, String motivo, Integer idUsuario) {
        if (idProducto == null || cantidad == null || cantidad <= 0) {
            return;
        }
        Producto producto = productoRepository.findById(idProducto).orElse(null);
        if (producto == null) {
            return;
        }
        // Vuelve a sumar las unidades canceladas al inventario disponible para que otro cliente pueda comprarlas.
        int stockActual = producto.getStock() == null ? 0 : producto.getStock();
        int stockResultante = stockActual + cantidad;
        producto.setStock(stockResultante);
        productoRepository.save(producto);

        // Deja anotado el motivo del reingreso en el historial de movimientos de bodega.
        InventarioMovimiento movimiento = new InventarioMovimiento();
        movimiento.setIdProducto(idProducto);
        movimiento.setIdUsuario(idUsuario);
        movimiento.setTipo("entrada");
        movimiento.setCantidad(cantidad);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo(motivo != null && !motivo.isBlank() ? motivo : "Reingreso por cancelacion");
        movimientoRepository.save(movimiento);
    }
}
