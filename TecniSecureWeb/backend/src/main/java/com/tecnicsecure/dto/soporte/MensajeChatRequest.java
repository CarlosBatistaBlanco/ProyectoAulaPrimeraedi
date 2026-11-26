/*
 * Este archivo recoge el texto que escribe un cliente registrado o un asesor dentro
 * de la ventana de chat interno de la aplicación. Solo verifica que el mensaje no se
 * envíe completamente vacío antes de guardarlo y entregarlo a la otra persona.
 */
package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.NotBlank;

public record MensajeChatRequest(
        // Contenido del mensaje; campo obligatorio que no puede quedar vacío
        @NotBlank(message = "El mensaje no puede estar vacio") String mensaje
) {
}
