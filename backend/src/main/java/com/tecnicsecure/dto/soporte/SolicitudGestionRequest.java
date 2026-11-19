package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Asignacion de tecnico, seguimiento de estado y observaciones de una solicitud. */
public record SolicitudGestionRequest(
        @Size(max = 120) String tecnicoAsignado,
        String observaciones,
        @Pattern(regexp = "pendiente|contactado|agendado|completado|cancelado",
                message = "Estado no valido")
        String estado
) {
}
