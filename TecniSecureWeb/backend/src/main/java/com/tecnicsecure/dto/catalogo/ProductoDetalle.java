/*
 * Este archivo reúne toda la información detallada de un producto para mostrarla
 * cuando un cliente abre su ficha completa en la tienda. Incluye fotos, precios,
 * garantía, unidades disponibles, categoría, marca y el promedio de calificaciones.
 */
package com.tecnicsecure.dto.catalogo;

import com.tecnicsecure.entity.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductoDetalle(
        // Identificador único y datos principales que describen el producto
        Integer idProducto,
        String nombre,
        String sku,
        String codigoBarras,
        String descripcion,
        // Precio normal, precio final con descuento, impuestos y meses de garantía
        BigDecimal precio,
        BigDecimal precioFinal,
        BigDecimal descuentoPct,
        BigDecimal impuestoPct,
        Integer garantiaMeses,
        // Unidades disponibles en bodega, límites configurados y alerta de pocas existencias
        Integer stock,
        Integer stockMinimo,
        Integer stockMaximo,
        Boolean stockBajo,
        // Fotografía principal y lista de imágenes adicionales para la galería
        String imagen,
        List<String> imagenes,
        // Opciones que indican si el producto aparece destacado o visible en la tienda
        Boolean destacado,
        Boolean activo,
        // Categoría y marca a las que pertenece el artículo
        Integer idCategoria,
        String categoria,
        Integer idMarca,
        String marca,
        // Calificación promedio y cantidad total de reseñas escritas por compradores
        Double calificacionPromedio,
        Long totalResenas,
        // Fecha y hora en que se registró el producto en el catálogo
        LocalDateTime creadoEn
) {
    // Arma la ficha completa combinando los datos guardados del producto, su galería y sus reseñas
    public static ProductoDetalle de(Producto p, List<String> imagenes,
                                     Double promedio, Long totalResenas) {
        return new ProductoDetalle(
                p.getIdProducto(), p.getNombre(), p.getSku(), p.getCodigoBarras(), p.getDescripcion(),
                p.getPrecio(), p.getPrecioFinal(), p.getDescuentoPct(), p.getImpuestoPct(),
                p.getGarantiaMeses(), p.getStock(), p.getStockMinimo(), p.getStockMaximo(), p.isStockBajo(),
                p.getImagen(), imagenes, p.getDestacado(), p.getActivo(),
                p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                p.getMarca() != null ? p.getMarca().getIdMarca() : null,
                p.getMarca() != null ? p.getMarca().getNombre() : null,
                promedio, totalResenas, p.getCreadoEn());
    }
}
