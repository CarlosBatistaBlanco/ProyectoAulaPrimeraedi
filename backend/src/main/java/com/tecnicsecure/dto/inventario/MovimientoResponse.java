package com.tecnicsecure.dto.inventario;

import com.tecnicsecure.entity.InventarioMovimiento;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Integer idMovimiento,
        Integer idProducto,
        String producto,
        String tipo,
        Integer cantidad,
        Integer stockResultante,
        String motivo,
        Integer idUsuario,
        LocalDateTime creadoEn
) {
    public static MovimientoResponse de(InventarioMovimiento m, String producto) {
        return new MovimientoResponse(m.getIdMovimiento(), m.getIdProducto(), producto, m.getTipo(),
                m.getCantidad(), m.getStockResultante(), m.getMotivo(), m.getIdUsuario(), m.getCreadoEn());
    }
}
