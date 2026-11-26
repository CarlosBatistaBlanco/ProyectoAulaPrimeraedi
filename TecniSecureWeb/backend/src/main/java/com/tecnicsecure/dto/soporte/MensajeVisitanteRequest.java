/*
 * Este archivo recibe cada mensaje que escribe un visitante sin cuenta cuando sigue
 * conversando con un asesor desde el chat del sitio público. Incluye el código de la
 * conversación para mantener todo en la misma charla y sus datos de contacto opcionales.
 */
package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensajeVisitanteRequest(
        // Texto obligatorio escrito por el visitante (máximo 2000 caracteres)
        @NotBlank(message = "El mensaje no puede estar vacio") @Size(max = 2000) String mensaje,
        // Código único de la conversación para enlazar este mensaje con los anteriores
        String token,
        // Datos de contacto opcionales del visitante (nombre, correo y teléfono)
        String nombre,
        String correo,
        String telefono
) {
}
