package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Correo al que se envia manualmente una factura desde el panel. */
public record EnvioFacturaRequest(
        @NotBlank(message = "Indica el correo de destino")
        @Email(message = "El correo no es valido")
        String correo
) {
}
