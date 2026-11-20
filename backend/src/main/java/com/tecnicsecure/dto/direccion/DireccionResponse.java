package com.tecnicsecure.dto.direccion;

import com.tecnicsecure.entity.DireccionUsuario;

import java.time.LocalDateTime;

public record DireccionResponse(
        Integer idDireccion,
        String etiqueta,
        String direccion,
        String ciudad,
        String barrio,
        String telefono,
        String nombreRecibe,
        String referencia,
        Double latitud,
        Double longitud,
        Boolean predeterminada,
        LocalDateTime creadoEn
) {
    public static DireccionResponse de(DireccionUsuario d) {
        return new DireccionResponse(d.getIdDireccion(), d.getEtiqueta(), d.getDireccion(),
                d.getCiudad(), d.getBarrio(), d.getTelefono(), d.getNombreRecibe(),
                d.getReferencia(), d.getLatitud(), d.getLongitud(), d.getPredeterminada(),
                d.getCreadoEn());
    }
}
