/*
 * Este archivo recoge los datos personales que cualquier usuario puede editar desde
 * la pantalla de su perfil, como su nombre, teléfono, documento, foto de perfil o
 * el cambio de contraseña. Se usa cuando la persona guarda actualizaciones en su cuenta.
 */
package com.tecnicsecure.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        // Nombre completo: máximo 120 caracteres y únicamente letras y espacios
        @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios")
        String nombre,
        // Teléfono de contacto: solo números y entre 7 y 10 dígitos
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)")
        String telefono,
        // Documento de identidad: solo números y entre 5 y 15 dígitos
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.DOCUMENTO,
                message = "El documento debe tener entre 5 y 15 digitos, sin letras")
        String documento,
        // Imagen o foto de perfil elegida por la persona
        String avatar,
        // Contraseña actual, necesaria únicamente si desea cambiarla por una nueva
        String passwordActual,
        // Nueva contraseña que reemplazará a la anterior
        String passwordNueva
) {
}
