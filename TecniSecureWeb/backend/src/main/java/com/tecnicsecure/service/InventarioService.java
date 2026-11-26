/*
 * Este archivo define las acciones para controlar la mercancia en bodega: revisar cuantas
 * unidades quedan, detectar productos por agotarse y anotar cada entrada o salida. Su trabajo
 * se nota en el panel de inventario y automaticamente cada vez que se vende o cancela un pedido.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.inventario.KardexProducto;
import com.tecnicsecure.dto.inventario.MovimientoInventarioRequest;
import com.tecnicsecure.dto.inventario.MovimientoResponse;

import java.util.List;

public interface InventarioService {

    // Muestra los productos que estan por agotarse porque llegaron o bajaron de su minimo permitido.
    List<KardexProducto> stockBajo();

    // Muestra las existencias actuales de todos los productos registrados en la tienda.
    List<KardexProducto> todos();

    // Consulta el historial completo de entradas, salidas y ajustes que ha tenido un producto.
    List<MovimientoResponse> kardex(Integer idProducto);

    // Muestra los movimientos de entrada y salida mas recientes de toda la bodega.
    List<MovimientoResponse> ultimosMovimientos();

    // Registra una entrada de mercancia, una salida o un ajuste manual en la cantidad de un producto.
    MovimientoResponse registrarMovimiento(Integer idProducto, MovimientoInventarioRequest request, Integer idUsuario);

    // Descuenta las unidades vendidas de la bodega y anota el movimiento cuando un cliente compra.
    void descontarPorVenta(Integer idProducto, Integer cantidad, Integer idUsuario);

    // Devuelve las unidades a la bodega y anota el reintegro cuando se cancela o anula una compra.
    void reintegrarPorCancelacion(Integer idProducto, Integer cantidad, String motivo, Integer idUsuario);
}
