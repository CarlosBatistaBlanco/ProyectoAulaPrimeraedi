package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de recuperacion de contrasena")
public record RecuperarRequest(
        @NotBlank @Email(message = "El correo no es valido") String correo
) {
}
