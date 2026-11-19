package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Auditoria general del sistema (quien hizo que y cuando). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "logs_actividad")
public class LogActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Integer idLog;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "detalle", length = 255)
    private String detalle;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
