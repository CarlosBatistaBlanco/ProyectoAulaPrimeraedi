package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mensaje que envia el visitante desde el asistente del sitio publico.
 * El token lo genera el servidor la primera vez; el navegador lo guarda y lo
 * reenvia para continuar el mismo hilo con el asesor.
 */
public record MensajeVisitanteRequest(
        @NotBlank(message = "El mensaje no puede estar vacio") @Size(max = 2000) String mensaje,
        String token,
        String nombre,
        String correo,
        String telefono
) {
}
