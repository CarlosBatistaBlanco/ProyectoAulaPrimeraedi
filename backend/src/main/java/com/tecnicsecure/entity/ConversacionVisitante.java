package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Conversacion iniciada desde el asistente virtual del sitio publico por un
 * visitante que no tiene cuenta. El navegador guarda el token y con el puede
 * recuperar el hilo y seguir escribiendo aunque el asesor responda despues.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conversaciones_visitante")
public class ConversacionVisitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Integer idConversacion;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "nombre", length = 120)
    private String nombre;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    /** Si ya se genero una solicitud de servicio se guarda su numero. */
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "abierta";

    /** Ultimo mensaje del visitante que el equipo todavia no ha leido. */
    @Column(name = "sin_leer", nullable = false)
    private Integer sinLeer = 0;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
