/*
 * Este archivo resume el estado de una charla abierta por un visitante sin cuenta en
 * el chat público. Se muestra en el panel de asesores para ver los datos de contacto
 * dejados por la persona, si generó una solicitud y cuándo fue su último mensaje.
 */
package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.ConversacionVisitante;

import java.time.LocalDateTime;

public record ConversacionVisitanteResponse(
        // Código de seguimiento e identificador interno de la conversación
        String token,
        Integer idConversacion,
        // Datos de contacto proporcionados por el visitante durante la charla
        String nombre,
        String correo,
        String telefono,
        // Número de solicitud vinculada, estado de la charla y fechas de inicio y última actividad
        Integer idSolicitud,
        String estado,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
    // Toma una conversación guardada y prepara su resumen listo para el panel de atención
    public static ConversacionVisitanteResponse de(ConversacionVisitante c) {
        return new ConversacionVisitanteResponse(c.getToken(), c.getIdConversacion(),
                c.getNombre(), c.getCorreo(), c.getTelefono(), c.getIdSolicitud(),
                c.getEstado(), c.getCreadoEn(), c.getActualizadoEn());
    }
}
