package com.tecnicsecure.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        Integer idCategoria
) {
    public CategoriaRequest(String nombre) {
        this(nombre, null);
    }
}
