/*
 * Este archivo define la ficha de cada conversación iniciada en el asistente de chat
 * por un visitante que aún no tiene cuenta en la tienda. Permite guardar sus datos de
 * contacto, retomar la charla más tarde y atenderla desde la sección de Chat del panel.
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
@Table(name = "conversaciones_visitante")
public class ConversacionVisitante {

    // Número único de la conversación y código privado que recuerda la charla en el navegador del visitante
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conversacion")
    private Integer idConversacion;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    // Datos opcionales que el visitante deja para que el equipo pueda contactarlo
    @Column(name = "nombre", length = 120)
    private String nombre;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "telefono", length = 30)
    private String telefono;

    // Número de la solicitud de servicio relacionada, si esta charla generó una
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    // Estado actual de la conversación, por ejemplo abierta o cerrada
    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "abierta";

    // Cantidad de mensajes del visitante que el equipo todavía no ha leído
    @Column(name = "sin_leer", nullable = false)
    private Integer sinLeer = 0;

    // Fechas en que inició la charla y en que se envió el último mensaje
    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
