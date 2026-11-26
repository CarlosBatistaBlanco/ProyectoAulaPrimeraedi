/*
 * Este archivo prepara toda la información de una solicitud de cotización o cita
 * técnica para mostrarla en el panel de soporte y en el seguimiento del cliente.
 * Incluye el servicio elegido, los datos de contacto, el técnico asignado y su estado.
 */
package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.SolicitudServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SolicitudResponse(
        // Identificador de la solicitud junto con el número y nombre del servicio pedido
        Integer idSolicitud,
        Integer idServicio,
        String servicio,
        // Datos de contacto y dirección proporcionados por el cliente
        String nombre,
        String correo,
        String telefono,
        String direccion,
        // Tipo de pedido (cotización o cita), fecha y hora deseadas y descripción del caso
        String tipo,
        LocalDate fechaPreferida,
        LocalTime horaPreferida,
        String descripcion,
        // Seguimiento interno: técnico asignado, notas del equipo, estado actual y fecha de registro
        String tecnicoAsignado,
        String observaciones,
        String estado,
        LocalDateTime creadoEn
) {
    // Toma una solicitud guardada y prepara todos sus datos para mostrarlos en pantalla
    public static SolicitudResponse de(SolicitudServicio s) {
        return new SolicitudResponse(s.getIdSolicitud(),
                s.getServicio() != null ? s.getServicio().getIdServicio() : null,
                s.getServicio() != null ? s.getServicio().getTitulo() : null,
                s.getNombre(), s.getCorreo(), s.getTelefono(), s.getDireccion(), s.getTipo(),
                s.getFechaPreferida(), s.getHoraPreferida(), s.getDescripcion(),
                s.getTecnicoAsignado(), s.getObservaciones(), s.getEstado(), s.getCreadoEn());
    }
}
