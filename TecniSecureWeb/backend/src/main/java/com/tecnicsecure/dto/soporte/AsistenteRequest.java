/*
 * Este archivo agrupa el mensaje y los datos opcionales que envía un visitante cuando
 * conversa con el asistente virtual de la página pública sin haber iniciado sesión.
 * Permite recordar preguntas anteriores y recoger datos de contacto si pide hablar con un asesor.
 */
package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AsistenteRequest(
        // Pregunta o mensaje obligatorio escrito por el visitante (máximo 2000 caracteres)
        @NotBlank(message = "El mensaje no puede estar vacio") @Size(max = 2000) String mensaje,

        // Mensajes previos de la charla para que el asistente recuerde de qué estaban hablando
        List<Mensaje> historial,

        // Datos de contacto opcionales por si el visitante pide que lo atienda un asesor humano
        String nombre,
        String correo,
        String telefono,

        // Código de seguimiento de la conversación que entrega el sistema en el primer mensaje
        String token
) {

    // Representa cada mensaje anterior de la conversación indicando quién escribió y su texto (máximo 2000 caracteres)
    public record Mensaje(String rol, @Size(max = 2000) String texto) {
    }
}
