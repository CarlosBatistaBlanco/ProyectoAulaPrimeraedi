/*
 * Este archivo agrupa toda la información que llena una persona nueva en el formulario
 * de registro para abrir su cuenta de cliente en TecniServicios. Verifica que el nombre,
 * el documento, el correo, el teléfono y la contraseña cumplan las reglas básicas antes de guardar.
 */
package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Datos para crear una cuenta de cliente")
public record RegistroRequest(
        // Nombre completo obligatorio; máximo 120 caracteres y solo letras con espacios
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios")
        String nombre,

        // Documento de identidad obligatorio; entre 5 y 15 dígitos sin letras
        @NotBlank(message = "El documento es obligatorio")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.DOCUMENTO,
                message = "El documento debe tener entre 5 y 15 digitos, sin letras")
        String documento,

        // Correo electrónico obligatorio y escrito con un formato completo válido
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.CORREO,
                message = "Escribe un correo completo, por ejemplo nombre@gmail.com")
        String correo,

        // Teléfono de contacto opcional; si se escribe, debe tener entre 7 y 10 números
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)")
        String telefono,

        // Contraseña segura obligatoria: mínimo 8 caracteres, una mayúscula, una minúscula y un número
        @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contrasena debe tener minimo 8 caracteres, una mayuscula, una minuscula y un numero")
        String password
) {
}
