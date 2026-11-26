/*
 * Este archivo define la ficha de cada servicio técnico que ofrece la empresa, como
 * instalación de cámaras, mantenimiento o asesoría en seguridad electrónica. Esta
 * información se muestra en la sección de Servicios del sitio público y al pedir cotizaciones.
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
@Table(name = "servicios")
public class Servicio {

    // Número único que identifica a cada servicio técnico
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer idServicio;

    // Título y explicación que ven los visitantes sobre en qué consiste el servicio
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // Dibujo o ícono representativo y posición en que aparece listado en la página
    @Column(name = "icono", length = 100)
    private String icono = "shield";

    @Column(name = "orden")
    private Integer orden = 0;
}
