/*
 * Este archivo recibe el correo electrónico que una persona escribe cuando olvida
 * su contraseña y pide recuperar el acceso a su cuenta. Se utiliza en la pantalla
 * de recuperación para enviarle el código temporal con el que podrá crear una clave nueva.
 */
package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de recuperacion de contrasena")
public record RecuperarRequest(
        // Correo registrado de la cuenta; es obligatorio y debe tener un formato válido
        @NotBlank @Email(message = "El correo no es valido") String correo
) {
}
