package com.tecnicsecure.dto.catalogo;

import com.tecnicsecure.entity.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Detalle completo de un producto (ficha). */
public record ProductoDetalle(
        Integer idProducto,
        String nombre,
        String sku,
        String codigoBarras,
        String descripcion,
        BigDecimal precio,
        BigDecimal precioFinal,
        BigDecimal descuentoPct,
        BigDecimal impuestoPct,
        Integer garantiaMeses,
        Integer stock,
        Integer stockMinimo,
        Integer stockMaximo,
        Boolean stockBajo,
        String imagen,
        List<String> imagenes,
        Boolean destacado,
        Boolean activo,
        Integer idCategoria,
        String categoria,
        Integer idMarca,
        String marca,
        Double calificacionPromedio,
        Long totalResenas,
        LocalDateTime creadoEn
) {
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
