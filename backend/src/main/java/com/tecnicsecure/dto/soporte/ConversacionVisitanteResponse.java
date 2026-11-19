package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.ConversacionVisitante;

import java.time.LocalDateTime;

/** Estado del hilo de un visitante: token, datos de contacto y solicitud generada. */
public record ConversacionVisitanteResponse(
        String token,
        Integer idConversacion,
        String nombre,
        String correo,
        String telefono,
        Integer idSolicitud,
        String estado,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
    public static ConversacionVisitanteResponse de(ConversacionVisitante c) {
        return new ConversacionVisitanteResponse(c.getToken(), c.getIdConversacion(),
                c.getNombre(), c.getCorreo(), c.getTelefono(), c.getIdSolicitud(),
                c.getEstado(), c.getCreadoEn(), c.getActualizadoEn());
    }
}
