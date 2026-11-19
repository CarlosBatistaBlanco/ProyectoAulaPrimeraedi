package com.tecnicsecure.dto.soporte;

import java.util.List;

/** Respuesta del asistente virtual del sitio publico. */
public record AsistenteResponse(
        String respuesta,
        String fuente,
        boolean escalado,
        Integer idSolicitud,
        String mensajeEscalado,
        List<String> sugerencias,

        /** Identificador del hilo para que el visitante lo guarde y siga la conversacion. */
        String token
) {
    /** Compatibilidad: respuestas sin hilo (uso interno). */
    public AsistenteResponse(String respuesta, String fuente, boolean escalado,
                             Integer idSolicitud, String mensajeEscalado, List<String> sugerencias) {
        this(respuesta, fuente, escalado, idSolicitud, mensajeEscalado, sugerencias, null);
    }
}
