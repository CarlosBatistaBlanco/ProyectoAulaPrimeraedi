/*
 * Este archivo define cada anotación de la bitácora general del sistema, donde queda
 * constancia de qué acción importante se realizó, quién la hizo y en qué momento.
 * Esta información se consulta en la sección de Auditoría o Registro de Actividad del panel.
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
@Table(name = "logs_actividad")
public class LogActividad {

    // Número único de la anotación y conexión con la persona que realizó la acción
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Integer idLog;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    // Resumen de la acción realizada, detalles adicionales y dirección de red desde donde se hizo
    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "detalle", length = 255)
    private String detalle;

    @Column(name = "ip", length = 45)
    private String ip;

    // Fecha y hora exacta en que ocurrió el evento
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
