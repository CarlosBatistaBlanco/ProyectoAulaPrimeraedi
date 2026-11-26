/*
 * Este archivo define la ficha de cada aviso o alerta que recibe una persona dentro
 * de la plataforma, como actualizaciones de un pedido o nuevas tareas asignadas. Esta
 * información se muestra en la campana de Notificaciones de la barra superior.
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
@Table(name = "notificaciones")
public class Notificacion {

    // Número único del aviso y conexión con la persona que debe recibirlo
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer idNotificacion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    // Título, texto explicativo y enlace directo a la pantalla relacionada con el aviso
    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "mensaje", length = 255)
    private String mensaje;

    @Column(name = "enlace", length = 255)
    private String enlace;

    // Indica si la persona ya abrió el aviso y cuándo se generó
    @Column(name = "leida")
    private Boolean leida = false;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
