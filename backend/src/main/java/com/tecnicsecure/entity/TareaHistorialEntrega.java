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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Integer idEntrega;

    @Column(name = "id_tarea", nullable = false)
    private Integer idTarea;

    @Column(name = "numero_intento", nullable = false)
    private Integer numeroIntento;

    @Column(name = "evidencia", columnDefinition = "TEXT")
    private String evidencia;

    @Column(name = "comentario_empleado", columnDefinition = "TEXT")
    private String comentarioEmpleado;

    /** Valores: en_revision, aprobada, devuelta */
    @Column(name = "estado_revision", nullable = false, length = 30)
    private String estadoRevision = "en_revision";

    @Column(name = "calificacion")
    private Integer calificacion;

    @Column(name = "observacion_admin", columnDefinition = "TEXT")
    private String observacionAdmin;

    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn = LocalDateTime.now();

    @Column(name = "revisado_en")
    private LocalDateTime revisadoEn;

    @Column(name = "id_revisor_usuario")
    private Integer idRevisorUsuario;
}
