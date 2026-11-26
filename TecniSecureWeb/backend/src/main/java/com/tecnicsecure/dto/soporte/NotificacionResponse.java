/*
 * Este archivo prepara cada aviso que aparece al abrir la campana de notificaciones
 * en la parte superior de la aplicación. Muestra un título breve, la explicación de
 * lo ocurrido, un acceso directo opcional y si la persona ya lo marcó como leído.
 */
package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.Notificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        // Número único que identifica esta notificación
        Integer idNotificacion,
        // Título corto y mensaje explicativo del aviso
        String titulo,
        String mensaje,
        // Acceso directo opcional para abrir el pedido, chat o pantalla relacionada
        String enlace,
        // Indica si el aviso ya fue leído y la fecha exacta en que se generó
        Boolean leida,
        LocalDateTime creadoEn
) {
    // Toma una notificación guardada y prepara sus datos para mostrarla en la campana de avisos
    public static NotificacionResponse de(Notificacion n) {
        return new NotificacionResponse(n.getIdNotificacion(), n.getTitulo(), n.getMensaje(),
                n.getEnlace(), n.getLeida(), n.getCreadoEn());
    }
}
