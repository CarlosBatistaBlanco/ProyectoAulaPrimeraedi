/*
 * Este archivo organiza la información de un aviso de error cuando algo sale mal
 * en cualquier pantalla de la aplicación, como un dato mal escrito o un elemento
 * que no existe. Permite mostrar mensajes claros e indicar qué campo debe corregirse.
 */
package com.tecnicsecure.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        // Fecha y hora exacta en la que ocurrió el inconveniente
        LocalDateTime timestamp,
        // Número interno que clasifica el tipo de problema ocurrido
        int status,
        // Título breve del tipo de inconveniente encontrado
        String error,
        // Explicación clara del problema para mostrarla en pantalla
        String mensaje,
        // Sección o dirección del sistema donde se produjo el error
        String ruta,
        // Lista de errores por cada campo cuando un formulario tiene datos incompletos o incorrectos
        Map<String, String> campos
) {
}
