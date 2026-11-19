package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tareas_empleados")
public class TareaEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea")
    private Integer idTarea;

    @Column(name = "id_empleado_usuario", nullable = false)
    private Integer idEmpleadoUsuario;

    @Column(name = "id_asignador_usuario")
    private Integer idAsignadorUsuario;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "prioridad", nullable = false, length = 20)
    private String prioridad = "media";

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "estado", nullable = false, length = 25)
    private String estado = "pendiente";

    @Column(name = "evidencia", columnDefinition = "TEXT")
    private String evidencia;

    @Column(name = "comentario_empleado", columnDefinition = "TEXT")
    private String comentarioEmpleado;

    @Column(name = "calificacion")
    private Integer calificacion;

    @Column(name = "observacion_admin", columnDefinition = "TEXT")
    private String observacionAdmin;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "completada_en")
    private LocalDateTime completadaEn;
}
