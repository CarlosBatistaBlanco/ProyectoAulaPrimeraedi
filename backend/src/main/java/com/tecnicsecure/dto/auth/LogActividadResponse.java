package com.tecnicsecure.dto.auth;

import com.tecnicsecure.entity.LogActividad;

import java.time.LocalDateTime;

/** Registro legible de auditoria del sistema. */
public record LogActividadResponse(
        Integer idLog,
        Integer idUsuario,
        String nombreUsuario,
        String rolUsuario,
        String accion,
        String detalle,
        String ip,
        LocalDateTime creadoEn
) {
    public static LogActividadResponse de(LogActividad log, String nombreUsuario) {
        return de(log, nombreUsuario, null);
    }

    public static LogActividadResponse de(LogActividad log, String nombreUsuario, String rolUsuario) {
        return new LogActividadResponse(
                log.getIdLog(),
                log.getIdUsuario(),
                nombreUsuario,
                rolUsuario,
                log.getAccion(),
                log.getDetalle(),
                log.getIp(),
                log.getCreadoEn());
    }
}
