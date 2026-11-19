package com.tecnicsecure.dto.inventario;

import com.tecnicsecure.entity.Producto;

/** Fila del kardex con el producto y su stock actual. */
public record KardexProducto(
        Integer idProducto,
        String nombre,
        String sku,
        Integer stock,
        Integer stockMinimo,
        Integer stockMaximo,
        Boolean stockBajo
) {
    public static KardexProducto de(Producto p) {
        return new KardexProducto(p.getIdProducto(), p.getNombre(), p.getSku(), p.getStock(),
                p.getStockMinimo(), p.getStockMaximo(), p.isStockBajo());
    }
}
