package com.tecnicsecure.dto.inventario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Entrada, salida o ajuste de inventario (Kardex). */
public record MovimientoInventarioRequest(
        @NotBlank(message = "El tipo es obligatorio (entrada, salida o ajuste)") String tipo,
        @NotNull(message = "La cantidad es obligatoria") @Min(0) Integer cantidad,
        String motivo,
        BigDecimal precio
) {
}
