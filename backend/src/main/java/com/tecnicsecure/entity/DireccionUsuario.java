package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Direccion de entrega guardada por un usuario (administrador, empleado o
 * cliente). Se usa para rellenar automaticamente el checkout y para tener
 * varias alternativas (casa, oficina, bodega...).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "direcciones_usuario")
public class DireccionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_direccion")
    private Integer idDireccion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    /** Nombre corto para reconocerla: "Casa", "Oficina", "Bodega". */
    @Column(name = "etiqueta", nullable = false, length = 60)
    private String etiqueta;

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "barrio", length = 100)
    private String barrio;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "nombre_recibe", length = 120)
    private String nombreRecibe;

    @Column(name = "referencia", length = 255)
    private String referencia;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    /** Solo una direccion por usuario puede estar marcada como predeterminada. */
    @Column(name = "predeterminada", nullable = false)
    private Boolean predeterminada = false;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
