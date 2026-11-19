package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Conversacion del asistente del sitio publico (sin login). */
public record AsistenteRequest(
        @NotBlank(message = "El mensaje no puede estar vacio") @Size(max = 2000) String mensaje,

        /** Historial previo (opcional) para que el modelo mantenga el contexto. */
        List<Mensaje> historial,

        /** Datos de contacto para cuando se escala a un asesor (opcionales). */
        String nombre,
        String correo,
        String telefono,

        /** Token del hilo (lo devuelve el servidor la primera vez). */
        String token
) {

    public record Mensaje(String rol, @Size(max = 2000) String texto) {
    }
}
