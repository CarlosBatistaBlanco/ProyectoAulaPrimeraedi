/*
 * Este archivo recibe el código temporal de recuperación y la nueva contraseña que
 * escribe una persona cuando está restableciendo el acceso a su cuenta. Se utiliza en
 * el segundo paso de la pantalla de recuperación de clave.
 */
package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Restablecer contrasena con el token recibido")
public record RestablecerRequest(
        // Código temporal obligatorio que la persona recibió para autorizar el cambio
        @NotBlank(message = "El token es obligatorio") String token,

        // Nueva contraseña obligatoria: mínimo 8 caracteres, al menos una mayúscula y un número
        @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contrasena debe tener minimo 8 caracteres, una mayuscula y un numero")
        String password
) {
}
