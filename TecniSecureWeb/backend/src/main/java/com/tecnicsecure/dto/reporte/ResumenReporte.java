/*
 * Este archivo concentra los indicadores principales que aparecen en las tarjetas de
 * resumen del tablero administrativo y en los reportes gerenciales. Muestra ventas,
 * pedidos, alertas de bodega, cuentas activas, solicitudes pendientes y los más vendidos.
 */
package com.tecnicsecure.dto.reporte;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ResumenReporte(
        // Fecha de inicio y fecha de cierre del periodo analizado
        LocalDateTime desde,
        LocalDateTime hasta,
        // Número de pedidos, ingresos del periodo elegido e ingresos acumulados desde el inicio
        Long cantidadPedidos,
        java.math.BigDecimal ingresosPeriodo,
        java.math.BigDecimal ingresosHistoricos,
        // Totales del catálogo y alertas de productos con pocas existencias en bodega
        Long productosActivos,
        Long productosStockBajo,
        // Indicadores operativos: usuarios activos, solicitudes por atender y mensajes de contacto sin leer
        Long usuariosActivos,
        Long solicitudesPendientes,
        Long mensajesContactoSinLeer,
        // Listado ordenado de los productos más vendidos durante el periodo
        List<ProductoVendido> masVendidos
) {
}
