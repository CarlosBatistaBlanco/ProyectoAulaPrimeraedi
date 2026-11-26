/*
 * Este archivo prepara la contestación que aparece en la ventana del asistente virtual
 * del sitio público. Incluye el texto de respuesta, los botones con preguntas sugeridas
 * y el aviso correspondiente cuando el caso pasa a manos de un asesor humano.
 */
package com.tecnicsecure.dto.soporte;

import java.util.List;

public record AsistenteResponse(
        // Texto principal que lee el visitante dentro de la ventana de ayuda
        String respuesta,
        // Indica de dónde salió la respuesta (asistente automático o inteligencia artificial)
        String fuente,
        // Indica si la consulta fue transferida a un asesor humano
        boolean escalado,
        // Número de solicitud generada cuando el caso pasa a atención personalizada
        Integer idSolicitud,
        // Mensaje adicional de confirmación que se muestra al transferir la consulta
        String mensajeEscalado,
        // Botones con preguntas rápidas sugeridas para facilitar la siguiente consulta
        List<String> sugerencias,

        // Código único de la conversación para que el visitante pueda retomarla sin perder el historial
        String token
) {
    // Permite armar una respuesta sencilla cuando aún no existe un código de seguimiento asociado
    public AsistenteResponse(String respuesta, String fuente, boolean escalado,
                             Integer idSolicitud, String mensajeEscalado, List<String> sugerencias) {
        this(respuesta, fuente, escalado, idSolicitud, mensajeEscalado, sugerencias, null);
    }
}
