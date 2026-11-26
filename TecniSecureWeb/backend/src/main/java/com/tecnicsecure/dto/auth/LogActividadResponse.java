/*
 * Este archivo agrupa los datos de cada registro del historial de actividad que se
 * muestra en el panel de administración. Permite saber qué persona hizo una acción
 * importante en el sistema, qué ocurrió exactamente y en qué fecha y hora sucedió.
 */
package com.tecnicsecure.dto.auth;

import com.tecnicsecure.entity.LogActividad;

import java.time.LocalDateTime;

public record LogActividadResponse(
        // Número único que identifica este registro del historial
        Integer idLog,
        // Identificador de la cuenta que realizó la acción
        Integer idUsuario,
        // Nombre visible de la persona que hizo el movimiento
        String nombreUsuario,
        // Tipo de usuario que actuó (cliente, empleado o administrador)
        String rolUsuario,
        // Nombre de la acción registrada, como inicio de sesión o cambio de estado
        String accion,
        // Explicación detallada de lo que sucedió en esa operación
        String detalle,
        // Dirección de red desde la cual se realizó la operación
        String ip,
        // Fecha y hora exacta en que quedó guardado el evento
        LocalDateTime creadoEn
) {
    // Prepara un registro guardado para mostrarlo en pantalla cuando no se requiere incluir el rol
    public static LogActividadResponse de(LogActividad log, String nombreUsuario) {
        return de(log, nombreUsuario, null);
    }

    // Prepara un registro guardado para mostrarlo en pantalla junto con el nombre y el rol de la persona
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
