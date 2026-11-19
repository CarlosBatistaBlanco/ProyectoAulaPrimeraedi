package com.tecnicsecure.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Cuerpo estandar de error de la API. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> campos
) {
}
