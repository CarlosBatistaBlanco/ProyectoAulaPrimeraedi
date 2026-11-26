/*
 * Este archivo prepara cada mensaje de una charla pública con un visitante sin cuenta
 * para mostrarlo tanto en la burbuja de ayuda del sitio web como en la pantalla del
 * asesor. Indica quién envió el texto, si ya fue leído y en qué momento ocurrió.
 */
package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.MensajeVisitante;

import java.time.LocalDateTime;

public record MensajeVisitanteResponse(
        // Identificador del mensaje y de la conversación a la que pertenece
        Integer idMensaje,
        Integer idConversacion,
        // Quién escribió (visitante, asistente o asesor) y la cuenta interna si respondió el equipo
        String remitente,
        Integer idUsuarioEnvia,
        // Texto del mensaje, estado de lectura y fecha exacta en que se envió
        String mensaje,
        Boolean leido,
        LocalDateTime creadoEn
) {
    // Toma un mensaje guardado de la charla pública y lo prepara para mostrarlo en pantalla
    public static MensajeVisitanteResponse de(MensajeVisitante m) {
        return new MensajeVisitanteResponse(m.getIdMensaje(), m.getIdConversacion(), m.getRemitente(),
                m.getIdUsuarioEnvia(), m.getMensaje(), m.getLeido(), m.getCreadoEn());
    }
}
