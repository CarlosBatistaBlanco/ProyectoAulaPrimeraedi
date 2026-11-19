package com.tecnicsecure.dto.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que el usuario puede modificar de su propio perfil. */
public record PerfilRequest(
        @Size(max = 120)
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.NOMBRE,
                message = "El nombre solo puede tener letras y espacios")
        String nombre,
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.TELEFONO,
                message = "El telefono debe tener entre 7 y 10 digitos (solo numeros)")
        String telefono,
        @Pattern(regexp = com.tecnicsecure.util.PatronesValidacion.DOCUMENTO,
                message = "El documento debe tener entre 5 y 15 digitos, sin letras")
        String documento,
        String avatar,
        String passwordActual,
        String passwordNueva
) {
}
