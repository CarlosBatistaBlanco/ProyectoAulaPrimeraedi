/*
 * Este archivo define cada intento de entrega que envía un empleado para demostrar
 * que cumplió una tarea, junto con sus pruebas y la revisión del administrador. Esta
 * información se muestra en el historial de entregas dentro de la sección de Tareas del panel.
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
@Table(name = "tareas_historial_entregas")
public class TareaHistorialEntrega {

    // Número único de la entrega, conexión con la tarea y número consecutivo del intento
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Integer idEntrega;

    @Column(name = "id_tarea", nullable = false)
    private Integer idTarea;

    @Column(name = "numero_intento", nullable = false)
    private Integer numeroIntento;

    // Evidencia de cumplimiento y comentarios que envió el empleado en este intento
    @Column(name = "evidencia", columnDefinition = "TEXT")
    private String evidencia;

    @Column(name = "comentario_empleado", columnDefinition = "TEXT")
    private String comentarioEmpleado;

    // Resultado de la revisión del administrador: en revisión, aprobada o devuelta para corregir
    @Column(name = "estado_revision", nullable = false, length = 30)
    private String estadoRevision = "en_revision";

    @Column(name = "calificacion")
    private Integer calificacion;

    @Column(name = "observacion_admin", columnDefinition = "TEXT")
    private String observacionAdmin;

    // Fechas de envío y de revisión, y conexión con el administrador que evaluó la entrega
    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn = LocalDateTime.now();

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    @Column(name = "id_revisor_usuario")
    private Integer idRevisorUsuario;
}
