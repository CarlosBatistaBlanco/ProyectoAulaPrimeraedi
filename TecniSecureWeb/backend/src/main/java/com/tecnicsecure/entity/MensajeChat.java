/*
 * Este archivo define la ficha de cada mensaje enviado dentro del chat entre un cliente
 * registrado y el equipo de atención de la tienda. Esta información se muestra en la
 * ventana de Chat del cliente y en la bandeja de conversaciones del panel administrativo.
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
@Table(name = "mensajes_chat")
public class MensajeChat {

    // Número único del mensaje y conexión con el cliente dueño de la conversación
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer idMensaje;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    // Indica si el mensaje lo escribió el cliente o un asesor, y quién fue el remitente
    @Column(name = "remitente", nullable = false, length = 10)
    private String remitente;

    @Column(name = "id_usuario_envia")
    private Integer idUsuarioEnvia;

    // Contenido escrito del mensaje
    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // Indica si la otra persona ya leyó el mensaje y la fecha en que se envió
    @Column(name = "leido")
    private Boolean leido = false;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
