/*
 * Este archivo transporta el código de respaldo que la aplicación envía de manera
 * automática para mantener la sesión abierta cuando está por vencerse. Gracias a este
 * proceso, la persona puede seguir navegando sin tener que escribir su contraseña otra vez.
 */
package com.tecnicsecure.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        // Código obligatorio de renovación que se entregó al iniciar sesión
        @NotBlank(message = "El refresh token es obligatorio") String refreshToken
) {
}
