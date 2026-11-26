/*
 * Este archivo define el mensaje sencillo de confirmación que el sistema envía
 * a la pantalla cuando una acción termina correctamente (por ejemplo, al guardar
 * un cambio, cerrar sesión o enviar un formulario) y solo necesita avisar al usuario.
 */
package com.tecnicsecure.dto;

public record ApiResponse(boolean ok, String mensaje) {

    // Prepara de forma rápida un aviso de éxito junto con el texto que verá la persona
    public static ApiResponse ok(String mensaje) {
        return new ApiResponse(true, mensaje);
    }
}
