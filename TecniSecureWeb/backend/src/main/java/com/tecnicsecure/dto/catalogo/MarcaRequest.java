/*
 * Este archivo recibe los datos que llena un administrador cuando agrega o modifica
 * una marca de productos en el catálogo. Aparece en la pantalla de administración
 * de marcas para mantener ordenados los equipos por fabricante.
 */
package com.tecnicsecure.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcaRequest(
        // Nombre obligatorio de la marca; no puede quedar vacío ni superar 80 caracteres
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        // Identificador opcional que se usa solo cuando se actualiza una marca existente
        Integer idMarca
) {
    // Permite preparar los datos indicando solo el nombre cuando se va a registrar una marca nueva
    public MarcaRequest(String nombre) {
        this(nombre, null);
    }
}
