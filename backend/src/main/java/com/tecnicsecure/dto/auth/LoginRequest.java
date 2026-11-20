package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de inicio de sesion")
public record LoginRequest(
        @Schema(example = "admin@tecnicsecure.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        String correo,

        @Schema(example = "Admin123!")
        @NotBlank(message = "La contrasena es obligatoria")
        String password,

        /** Respuesta que escribio el usuario en la verificacion. */
        @Schema(example = "13")
        String captcha,

        /** Ficha firmada que devolvio /api/auth/captcha; liga la pregunta con su respuesta. */
        @Schema(description = "Token opaco entregado junto con la pregunta")
        String captchaToken
) {
}
