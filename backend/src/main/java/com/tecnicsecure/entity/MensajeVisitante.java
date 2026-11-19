package com.tecnicsecure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Mensaje dentro de una conversacion de visitante sin cuenta. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mensajes_visitante")
public class MensajeVisitante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer idMensaje;

    @Column(name = "id_conversacion", nullable = false)
    private Integer idConversacion;

    /** visitante | bot | asesor */
    @Column(name = "remitente", nullable = false, length = 12)
    private String remitente;

    @Column(name = "id_usuario_envia")
    private Integer idUsuarioEnvia;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
