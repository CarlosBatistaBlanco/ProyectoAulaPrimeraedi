package com.tecnicsecure.dto.catalogo;

import com.tecnicsecure.entity.Producto;

import java.math.BigDecimal;

/** Vista resumida de producto para listados y catalogo publico. */
public record ProductoResumen(
        Integer idProducto,
        String nombre,
        String sku,
        String descripcion,
        BigDecimal precio,
        BigDecimal precioFinal,
        BigDecimal descuentoPct,
        Integer stock,
        Boolean stockBajo,
        String imagen,
        Boolean destacado,
        Integer idCategoria,
        String categoria,
        String marca
) {
    public static ProductoResumen de(Producto p) {
        return new ProductoResumen(
                p.getIdProducto(),
                p.getNombre(),
                p.getSku(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getPrecioFinal(),
                p.getDescuentoPct(),
                p.getStock(),
                p.isStockBajo(),
                p.getImagen(),
                p.getDestacado(),
                p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getMarca() != null ? p.getMarca().getNombre() : null);
    }
}
