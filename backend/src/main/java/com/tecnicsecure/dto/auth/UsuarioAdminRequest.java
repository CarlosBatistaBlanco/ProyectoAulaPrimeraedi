package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para crear o actualizar un usuario desde el panel administrador. */
@Schema(description = "Datos de gestion de usuario por el administrador")
public record UsuarioAdminRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios")
        String nombre,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") @Size(max = 150)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com")
        String correo,
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.DOCUMENTO,
                message = "El documento debe tener entre 5 y 15 digitos, sin letras")
        String documento,
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)")
        String telefono,
        @NotBlank(message = "El rol es obligatorio") String rol,
        // Misma regla que el registro: evita claves como "123456"
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$",
                message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número")
        String password,
        Boolean activo,
        Boolean bloqueado
) {
}
