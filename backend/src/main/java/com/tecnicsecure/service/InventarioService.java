package com.tecnicsecure.service;

import com.tecnicsecure.dto.inventario.KardexProducto;
import com.tecnicsecure.dto.inventario.MovimientoInventarioRequest;
import com.tecnicsecure.dto.inventario.MovimientoResponse;

import java.util.List;

public interface InventarioService {

    List<KardexProducto> stockBajo();

    List<KardexProducto> todos();

    List<MovimientoResponse> kardex(Integer idProducto);

    List<MovimientoResponse> ultimosMovimientos();

    MovimientoResponse registrarMovimiento(Integer idProducto, MovimientoInventarioRequest request, Integer idUsuario);

    /** Descuenta stock y registra el movimiento (lo usa el checkout de pedidos). */
    void descontarPorVenta(Integer idProducto, Integer cantidad, Integer idUsuario);
}
