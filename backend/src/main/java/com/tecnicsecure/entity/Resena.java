package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Resena de un producto. Un usuario solo puede resenar una vez cada producto
 * (restriccion unica en la BD).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resenas",
        uniqueConstraints = @UniqueConstraint(name = "unico_resena", columnNames = {"id_producto", "id_usuario"}))
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Integer idResena;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "calificacion", nullable = false)
    private Integer calificacion;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
