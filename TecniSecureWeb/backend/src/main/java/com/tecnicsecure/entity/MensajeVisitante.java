/*
 * Este archivo define la ficha de cada mensaje enviado dentro de una charla del
 * asistente público con un visitante que no tiene cuenta. Se utiliza en la burbuja
 * de chat del sitio web y en la sección de Chat de Visitantes del panel de atención.
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
@Table(name = "mensajes_visitante")
public class MensajeVisitante {

    // Número único del mensaje y conexión con la conversación a la que pertenece
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer idMensaje;

    @Column(name = "id_conversacion", nullable = false)
    private Integer idConversacion;

    // Indica quién escribió este mensaje: el visitante, el asistente automático o un asesor humano
    @Column(name = "remitente", nullable = false, length = 12)
    private String remitente;

    // Conexión con el asesor que respondió, cuando el mensaje proviene del equipo
    @Column(name = "id_usuario_envia")
    private Integer idUsuarioEnvia;

    // Texto escrito en el mensaje
    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // Indica si el mensaje ya fue leído y la fecha en que se envió
    @Column(name = "leido", nullable = false)
    private Boolean leido = false;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
