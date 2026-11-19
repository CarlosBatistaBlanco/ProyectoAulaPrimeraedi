package com.tecnicsecure.dto.catalogo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Datos para crear o actualizar un producto. */
@Schema(description = "Producto a crear o actualizar")
public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        @Size(max = 40) String sku,
        @Size(max = 40) String codigoBarras,
        String descripcion,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal precio,
        @DecimalMin(value = "0.0") @DecimalMax("100.0") BigDecimal descuentoPct,
        @DecimalMin(value = "0.0") @DecimalMax("100.0") BigDecimal impuestoPct,
        @Min(0) Integer garantiaMeses,
        @NotNull(message = "El stock es obligatorio") @Min(0) Integer stock,
        @Min(0) Integer stockMinimo,
        @Min(0) Integer stockMaximo,
        String imagen,
        Boolean destacado,
        Boolean activo,
        Integer idCategoria,
        Integer idMarca
) {
}
