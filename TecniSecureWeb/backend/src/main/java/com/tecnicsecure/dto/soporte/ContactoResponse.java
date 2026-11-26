/*
 * Este archivo prepara cada mensaje enviado desde el formulario de contacto para
 * mostrarlo en la bandeja de soporte del panel interno. Permite al equipo leer el
 * motivo de la consulta, ver si ya fue atendida y revisar la respuesta enviada.
 */
package com.tecnicsecure.dto.soporte;

import com.tecnicsecure.entity.Contacto;

import java.time.LocalDateTime;

public record ContactoResponse(
        // Identificador del mensaje y datos de contacto de la persona que escribió
        Integer idContacto,
        String nombre,
        String correo,
        String telefono,
        // Tema, categoría elegida, texto enviado y respuesta escrita por el equipo
        String asunto,
        String tipo,
        String mensaje,
        String respuesta,
        // Indica si el mensaje ya fue revisado y la fecha exacta en que llegó
        Boolean leido,
        LocalDateTime creadoEn
) {
    // Toma un mensaje de contacto guardado y prepara sus datos para la bandeja de soporte
    public static ContactoResponse de(Contacto c) {
        return new ContactoResponse(c.getIdContacto(), c.getNombre(), c.getCorreo(), c.getTelefono(),
                c.getAsunto(), c.getTipo(), c.getMensaje(), c.getRespuesta(), c.getLeido(), c.getCreadoEn());
    }
}
