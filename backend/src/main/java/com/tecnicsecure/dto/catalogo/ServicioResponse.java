package com.tecnicsecure.dto.catalogo;

import com.tecnicsecure.entity.Servicio;

public record ServicioResponse(
        Integer idServicio,
        String titulo,
        String descripcion,
        String icono,
        Integer orden
) {
    public static ServicioResponse de(Servicio s) {
        return new ServicioResponse(s.getIdServicio(), s.getTitulo(), s.getDescripcion(),
                s.getIcono(), s.getOrden());
    }
}
