/*
 * Este archivo agrupa los datos que una persona escribe cuando llena el formulario
 * de inicio de sesión: su correo, su contraseña y la respuesta de la pregunta de
 * seguridad. Es la información que viaja desde la pantalla de ingreso para validar su identidad.
 */
package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales de inicio de sesion")
public record LoginRequest(
        // Correo electrónico obligatorio y con un formato válido
        @Schema(example = "admin@tecnicsecure.com")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        String correo,

        // Contraseña obligatoria que escribió la persona para ingresar
        @Schema(example = "Admin123!")
        @NotBlank(message = "La contrasena es obligatoria")
        String password,

        // Respuesta que escribió la persona en la verificación de seguridad
        @Schema(example = "13")
        String captcha,

        // Comprobante interno que conecta la pregunta de seguridad mostrada con la respuesta enviada
        @Schema(description = "Token opaco entregado junto con la pregunta")
        String captchaToken
) {
}
