package com.tecnicsecure.dto.pedido;

import jakarta.validation.constraints.NotBlank;

public record CambioEstadoRequest(
        @NotBlank(message = "El nuevo estado es obligatorio") String estado,
        String observacion
) {
}
