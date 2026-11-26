/*
 * Este archivo prepara cada burbuja de mensaje del chat interno para mostrarla tanto
 * en la pantalla del cliente como en el panel de atención del equipo. Indica quién
 * escribió el mensaje, su contenido, si ya fue leído y a qué hora se envió.
 */
package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.MensajeChat;

import java.time.LocalDateTime;

public record MensajeChatResponse(
        // Identificador del mensaje y del cliente al que pertenece esta conversación
        Integer idMensaje,
        Integer idCliente,
        // Quién escribió el mensaje (cliente o asesor) y la cuenta que lo envió
        String remitente,
        Integer idUsuarioEnvia,
        // Texto del mensaje, indicador de lectura y fecha exacta en que se envió
        String mensaje,
        Boolean leido,
        LocalDateTime creadoEn
) {
    // Toma un mensaje guardado del chat y lo prepara para dibujarlo en pantalla
    public static MensajeChatResponse de(MensajeChat m) {
        return new MensajeChatResponse(m.getIdMensaje(), m.getIdCliente(), m.getRemitente(),
                m.getIdUsuarioEnvia(), m.getMensaje(), m.getLeido(), m.getCreadoEn());
    }
}
