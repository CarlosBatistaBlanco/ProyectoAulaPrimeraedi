/*
 * Este archivo reúne los datos que llena un administrador cuando crea o edita una
 * cuenta desde el panel de gestión de usuarios. Incluye información de contacto,
 * el rol asignado, la contraseña y si la cuenta queda activa o bloqueada.
 */
package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de gestion de usuario por el administrador")
public record UsuarioAdminRequest(
        // Nombre completo obligatorio; máximo 120 caracteres y solo letras con espacios
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios")
        String nombre,
        // Correo electrónico obligatorio, con formato completo válido y hasta 150 caracteres
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo invalido") @Size(max = 150)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com")
        String correo,
        // Documento de identidad; debe tener entre 5 y 15 dígitos sin letras
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.DOCUMENTO,
                message = "El documento debe tener entre 5 y 15 digitos, sin letras")
        String documento,
        // Teléfono de contacto; solo números y entre 7 y 10 dígitos
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)")
        String telefono,
        // Perfil de permisos obligatorio para la cuenta (administrador, empleado o cliente)
        @NotBlank(message = "El rol es obligatorio") String rol,
        // Contraseña con la misma regla de seguridad del registro para evitar claves fáciles de adivinar
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,100}$",
                message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número")
        String password,
        // Indica si la cuenta está habilitada para entrar al sistema
        Boolean activo,
        // Indica si la cuenta está bloqueada temporalmente por seguridad o decisión administrativa
        Boolean bloqueado
) {
}
