/*
 * Este archivo define la ficha de cada opinión y calificación en estrellas que una
 * persona deja sobre un producto; cada comprador solo puede opinar una vez por artículo.
 * Esta información se muestra en la página de detalle del producto dentro de la tienda.
 */
package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resenas",
        uniqueConstraints = @UniqueConstraint(name = "unico_resena", columnNames = {"id_producto", "id_usuario"}))
public class Resena {

    // Número único de la opinión y conexión con el producto evaluado y la persona que opinó
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Integer idResena;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    // Puntaje en estrellas y comentario escrito por el comprador
    @Column(name = "calificacion", nullable = false)
    private Integer calificacion;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    // Fecha y hora en que se publicó la opinión
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
