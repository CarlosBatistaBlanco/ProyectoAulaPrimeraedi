/*
 * Este archivo recibe el nuevo estado y la nota opcional cuando un empleado o
 * administrador actualiza el avance de un pedido desde el panel de gestión.
 * Por ejemplo, al pasar una compra de "pendiente" a "en preparación", "enviado" o "entregado".
 */
package com.tecnicsecure.dto.pedido;

import jakarta.validation.constraints.NotBlank;

public record CambioEstadoRequest(
        // Nuevo estado obligatorio del pedido (por ejemplo: procesando, enviado, entregado o cancelado)
        @NotBlank(message = "El nuevo estado es obligatorio") String estado,
        // Comentario opcional que explica la razón o el detalle del cambio
        String observacion
) {
}
