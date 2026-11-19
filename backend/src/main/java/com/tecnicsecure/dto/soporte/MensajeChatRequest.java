package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.NotBlank;

public record MensajeChatRequest(
        @NotBlank(message = "El mensaje no puede estar vacio") String mensaje
) {
}
