/*
 * Este archivo prepara los datos de una dirección guardada para mostrarla en la
 * libreta de direcciones del cliente y permitirle seleccionarla con un clic al
 * confirmar un pedido. Incluye datos de contacto, referencias y ubicación en el mapa.
 */
package com.tecnicsecure.dto.direccion;

import com.tecnicsecure.entity.DireccionUsuario;

import java.time.LocalDateTime;

public record DireccionResponse(
        // Número identificador y datos completos de la ubicación y de quien recibe
        Integer idDireccion,
        String etiqueta,
        String direccion,
        String ciudad,
        String barrio,
        String telefono,
        String nombreRecibe,
        String referencia,
        // Coordenadas guardadas para ubicar el punto exacto en el mapa
        Double latitud,
        Double longitud,
        // Indica si es la dirección favorita del cliente y la fecha en que se guardó
        Boolean predeterminada,
        LocalDateTime creadoEn
) {
    // Toma una dirección guardada y prepara sus datos listos para mostrarlos en pantalla
    public static DireccionResponse de(DireccionUsuario d) {
        return new DireccionResponse(d.getIdDireccion(), d.getEtiqueta(), d.getDireccion(),
                d.getCiudad(), d.getBarrio(), d.getTelefono(), d.getNombreRecibe(),
                d.getReferencia(), d.getLatitud(), d.getLongitud(), d.getPredeterminada(),
                d.getCreadoEn());
    }
}
