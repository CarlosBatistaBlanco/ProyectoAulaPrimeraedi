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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Integer idImagen;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "ruta", nullable = false, length = 255)
    private String ruta;

    @Column(name = "orden")
    private Integer orden = 0;
}
