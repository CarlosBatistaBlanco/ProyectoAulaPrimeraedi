/*
 * Este archivo representa cada fila de la tabla de control de existencias en la
 * pantalla de inventario del panel interno. Permite ver de un vistazo cuántas unidades
 * tiene cada producto en bodega y si se encendió una alerta por pocas existencias.
 */
package com.tecnicsecure.dto.inventario;

import com.tecnicsecure.entity.Producto;

public record KardexProducto(
        // Identificador, nombre y código de referencia del producto
        Integer idProducto,
        String nombre,
        String sku,
        // Unidades disponibles actualmente y límites mínimo y máximo definidos para la bodega
        Integer stock,
        Integer stockMinimo,
        Integer stockMaximo,
        // Indicador que alerta cuando las existencias están por debajo del mínimo esperado
        Boolean stockBajo
) {
    // Toma un producto guardado y prepara su resumen de existencias para la tabla de bodega
    public static KardexProducto de(Producto p) {
        return new KardexProducto(p.getIdProducto(), p.getNombre(), p.getSku(), p.getStock(),
                p.getStockMinimo(), p.getStockMaximo(), p.isStockBajo());
    }
}
