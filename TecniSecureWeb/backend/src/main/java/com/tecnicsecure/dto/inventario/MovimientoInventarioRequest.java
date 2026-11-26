/*
 * Este archivo recoge los datos del formulario que usa el equipo de bodega para
 * registrar una entrada de mercancía, una salida o un ajuste manual de existencias.
 * También permite actualizar el precio del artículo en la misma operación si se requiere.
 */
package com.tecnicsecure.dto.inventario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MovimientoInventarioRequest(
        // Tipo de movimiento obligatorio: entrada, salida o ajuste
        @NotBlank(message = "El tipo es obligatorio (entrada, salida o ajuste)") String tipo,
        // Cantidad de unidades obligatoria; no puede ser un número negativo
        @NotNull(message = "La cantidad es obligatoria") @Min(0) Integer cantidad,
        // Explicación del motivo del movimiento, como compra a proveedor o conteo físico
        String motivo,
        // Nuevo precio opcional por si también se desea actualizar el valor de venta del producto
        BigDecimal precio
) {
}
