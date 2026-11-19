package com.tecnicsecure.dto.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcaRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String nombre,
        Integer idMarca
) {
    public MarcaRequest(String nombre) {
        this(nombre, null);
    }
}
