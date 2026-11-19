package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.Notificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Integer idNotificacion,
        String titulo,
        String mensaje,
        String enlace,
        Boolean leida,
        LocalDateTime creadoEn
) {
    public static NotificacionResponse de(Notificacion n) {
        return new NotificacionResponse(n.getIdNotificacion(), n.getTitulo(), n.getMensaje(),
                n.getEnlace(), n.getLeida(), n.getCreadoEn());
    }
}
