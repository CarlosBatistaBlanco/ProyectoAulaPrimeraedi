/*
 * Este archivo recibe los datos que llena un administrador cuando crea o edita una
 * categoría para organizar los productos de la tienda (por ejemplo, Cámaras o Alarmas).
 * Se usa en el formulario de gestión de categorías del panel administrativo.
 */
package com.tecnicsecure.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        // Nombre obligatorio de la categoría; no puede quedar vacío ni superar 80 caracteres
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        // Identificador opcional que se usa únicamente al editar una categoría ya existente
        Integer idCategoria
) {
    // Permite preparar los datos indicando solo el nombre cuando se va a crear una categoría nueva
    public CategoriaRequest(String nombre) {
        this(nombre, null);
    }
}
