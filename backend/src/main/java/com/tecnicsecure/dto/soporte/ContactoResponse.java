package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.Contacto;

import java.time.LocalDateTime;

public record ContactoResponse(
        Integer idContacto,
        String nombre,
        String correo,
        String telefono,
        String asunto,
        String tipo,
        String mensaje,
        String respuesta,
        Boolean leido,
        LocalDateTime creadoEn
) {
    public static ContactoResponse de(Contacto c) {
        return new ContactoResponse(c.getIdContacto(), c.getNombre(), c.getCorreo(), c.getTelefono(),
                c.getAsunto(), c.getTipo(), c.getMensaje(), c.getRespuesta(), c.getLeido(), c.getCreadoEn());
    }
}
