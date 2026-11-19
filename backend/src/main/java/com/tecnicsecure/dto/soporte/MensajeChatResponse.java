package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.MensajeChat;

import java.time.LocalDateTime;

public record MensajeChatResponse(
        Integer idMensaje,
        Integer idCliente,
        String remitente,
        Integer idUsuarioEnvia,
        String mensaje,
        Boolean leido,
        LocalDateTime creadoEn
) {
    public static MensajeChatResponse de(MensajeChat m) {
        return new MensajeChatResponse(m.getIdMensaje(), m.getIdCliente(), m.getRemitente(),
                m.getIdUsuarioEnvia(), m.getMensaje(), m.getLeido(), m.getCreadoEn());
    }
}
