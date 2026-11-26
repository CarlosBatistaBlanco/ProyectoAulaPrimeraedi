/*
 * Este archivo recibe el correo electrónico que escribe un empleado o administrador
 * cuando decide reenviar manualmente la factura de un pedido desde el panel de gestión.
 * Comprueba que el correo no esté vacío y tenga un formato válido antes de enviarlo.
 */
package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EnvioFacturaRequest(
        // Correo electrónico de destino obligatorio y con formato válido
        @NotBlank(message = "Indica el correo de destino")
        @Email(message = "El correo no es valido")
        String correo
) {
}
