/*
 * Este archivo agrupa los datos de cada registro del historial de movimientos de
 * bodega que se consulta en el panel de inventario. Muestra qué producto cambió,
 * cuántas unidades se movieron, cómo quedó el saldo final y quién hizo la operación.
 */
package com.tecnicsecure.dto.inventario;

import com.tecnicsecure.entity.InventarioMovimiento;

import java.time.LocalDateTime;

public record MovimientoResponse(
        // Número del movimiento e identificador del producto afectado
        Integer idMovimiento,
        Integer idProducto,
        // Nombre visible del producto en el catálogo
        String producto,
        // Clase de operación realizada (entrada, salida o ajuste)
        String tipo,
        // Cantidad movida y unidades finales que quedaron disponibles después del cambio
        Integer cantidad,
        Integer stockResultante,
        // Motivo registrado, usuario responsable y fecha exacta del movimiento
        String motivo,
        Integer idUsuario,
        LocalDateTime creadoEn
) {
    // Toma un movimiento guardado y prepara su información lista para mostrarla en el historial
    public static MovimientoResponse de(InventarioMovimiento m, String producto) {
        return new MovimientoResponse(m.getIdMovimiento(), m.getIdProducto(), producto, m.getTipo(),
                m.getCantidad(), m.getStockResultante(), m.getMotivo(), m.getIdUsuario(), m.getCreadoEn());
    }
}
