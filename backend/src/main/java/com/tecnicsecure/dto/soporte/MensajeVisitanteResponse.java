package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.MensajeVisitante;

import java.time.LocalDateTime;

/** Mensaje del hilo de un visitante sin cuenta. */
public record MensajeVisitanteResponse(
        Integer idMensaje,
        Integer idConversacion,
        String remitente,
        Integer idUsuarioEnvia,
        String mensaje,
        Boolean leido,
        LocalDateTime creadoEn
) {
    public static MensajeVisitanteResponse de(MensajeVisitante m) {
        return new MensajeVisitanteResponse(m.getIdMensaje(), m.getIdConversacion(), m.getRemitente(),
                m.getIdUsuarioEnvia(), m.getMensaje(), m.getLeido(), m.getCreadoEn());
    }
}
