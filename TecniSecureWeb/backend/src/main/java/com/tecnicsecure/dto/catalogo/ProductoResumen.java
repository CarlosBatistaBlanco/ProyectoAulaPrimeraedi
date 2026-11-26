/*
 * Este archivo agrupa los datos esenciales de cada producto para dibujarlo en forma
 * de tarjeta dentro del catálogo público, los resultados de búsqueda y los destacados.
 * Solo incluye lo necesario para explorar rápido sin cargar toda la ficha técnica.
 */
package com.tecnicsecure.dto.catalogo;

import com.tecnicsecure.entity.Producto;

import java.math.BigDecimal;

public record ProductoResumen(
        // Identificador, nombre, código de referencia y resumen del artículo
        Integer idProducto,
        String nombre,
        String sku,
        String descripcion,
        // Precio normal, precio final con descuento y porcentaje de rebaja aplicado
        BigDecimal precio,
        BigDecimal precioFinal,
        BigDecimal descuentoPct,
        // Cantidad disponible en bodega y aviso cuando quedan pocas unidades
        Integer stock,
        Boolean stockBajo,
        // Fotografía principal y marca que indica si es un artículo destacado
        String imagen,
        Boolean destacado,
        // Categoría y marca del producto para mostrar filtros y etiquetas
        Integer idCategoria,
        String categoria,
        String marca
) {
    // Toma un producto guardado y prepara únicamente los datos necesarios para su tarjeta en la tienda
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
