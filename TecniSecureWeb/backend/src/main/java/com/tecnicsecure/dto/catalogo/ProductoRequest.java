/*
 * Este archivo recibe todos los datos que un administrador llena en el formulario
 * para crear o editar un producto del catálogo. Valida que el nombre, el precio,
 * los porcentajes y las unidades en bodega sean valores correctos antes de guardarlos.
 */
package com.tecnicsecure.dto.catalogo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Producto a crear o actualizar")
public record ProductoRequest(
        // Nombre obligatorio del producto; máximo 150 caracteres
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String nombre,
        // Códigos opcionales de referencia interna y código de barras (máximo 40 caracteres)
        @Size(max = 40) String sku,
        @Size(max = 40) String codigoBarras,
        // Descripción detallada que verán los clientes en la tienda
        String descripcion,
        // Precio base obligatorio; debe ser cero o mayor
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal precio,
        // Porcentajes opcionales de descuento e impuesto, siempre entre 0% y 100%
        @DecimalMin(value = "0.0") @DecimalMax("100.0") BigDecimal descuentoPct,
        @DecimalMin(value = "0.0") @DecimalMax("100.0") BigDecimal impuestoPct,
        // Cantidad de meses de garantía; no puede ser un número negativo
        @Min(0) Integer garantiaMeses,
        // Unidades disponibles obligatorias (desde 0) y topes opcionales mínimo y máximo de bodega
        @NotNull(message = "El stock es obligatorio") @Min(0) Integer stock,
        @Min(0) Integer stockMinimo,
        @Min(0) Integer stockMaximo,
        // Imagen principal y opciones para decidir si el producto se destaca o se oculta en la tienda
        String imagen,
        Boolean destacado,
        Boolean activo,
        // Identificadores de la categoría y la marca seleccionadas en el formulario
        Integer idCategoria,
        Integer idMarca
) {
}
