/*
 * Este archivo recoge los cambios que registra un coordinador o técnico desde el panel
 * interno al gestionar una cotización o cita de servicio. Permite asignar el técnico
 * encargado, escribir notas de seguimiento y actualizar el estado del caso.
 */
package com.tecnicsecure.dto.soporte;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudGestionRequest(
        // Nombre del técnico responsable de atender la visita o cotización (máximo 120 caracteres)
        @Size(max = 120) String tecnicoAsignado,
        // Notas internas de seguimiento o acuerdos confirmados con el cliente
        String observaciones,
        // Nuevo estado permitido: pendiente, contactado, agendado, completado o cancelado
        @Pattern(regexp = "pendiente|contactado|agendado|completado|cancelado",
                message = "Estado no valido")
        String estado
) {
}
