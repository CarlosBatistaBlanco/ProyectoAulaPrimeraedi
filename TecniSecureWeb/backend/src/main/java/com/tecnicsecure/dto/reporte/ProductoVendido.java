/*
 * Este archivo agrupa el resultado de ventas de un producto individual para mostrarlo
 * en la tabla y gráficas de los artículos más vendidos dentro del panel de reportes
 * administrativos. Permite comparar cuántas unidades y cuánto dinero generó cada uno.
 */
package com.tecnicsecure.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoVendido(
        // Identificador del producto dentro del catálogo
        Integer idProducto,
        // Nombre comercial del producto
        String nombre,
        // Cantidad total de unidades vendidas en el periodo consultado
        Long unidades,
        // Dinero total generado por las ventas de este producto
        BigDecimal ingresos
) {
}
