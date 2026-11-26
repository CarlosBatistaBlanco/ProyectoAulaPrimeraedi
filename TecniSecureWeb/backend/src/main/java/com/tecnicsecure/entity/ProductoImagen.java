/*
 * Este archivo define la ficha de cada fotografía adicional que forma parte de la
 * galería de un artículo, junto con el orden en que debe mostrarse. Esta información
 * aparece en el carrusel de fotos del detalle de cada producto y al editarlo en el panel.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "producto_imagenes")
public class ProductoImagen {

    // Número único de la fotografía y conexión con el producto al que pertenece
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Integer idImagen;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    // Ubicación del archivo de la foto y posición en la que aparece dentro de la galería
    @Column(name = "ruta", nullable = false, length = 255)
    private String ruta;

    @Column(name = "orden")
    private Integer orden = 0;
}
