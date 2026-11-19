package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.SolicitudServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record SolicitudResponse(
        Integer idSolicitud,
        Integer idServicio,
        String servicio,
        String nombre,
        String correo,
        String telefono,
        String direccion,
        String tipo,
        LocalDate fechaPreferida,
        LocalTime horaPreferida,
        String descripcion,
        String tecnicoAsignado,
        String observaciones,
        String estado,
        LocalDateTime creadoEn
) {
    public static SolicitudResponse de(SolicitudServicio s) {
        return new SolicitudResponse(s.getIdSolicitud(),
                s.getServicio() != null ? s.getServicio().getIdServicio() : null,
                s.getServicio() != null ? s.getServicio().getTitulo() : null,
                s.getNombre(), s.getCorreo(), s.getTelefono(), s.getDireccion(), s.getTipo(),
                s.getFechaPreferida(), s.getHoraPreferida(), s.getDescripcion(),
                s.getTecnicoAsignado(), s.getObservaciones(), s.getEstado(), s.getCreadoEn());
    }
}
