package com.tecnicsecure.dto;

/** Respuesta generica para operaciones sin cuerpo propio. */
public record ApiResponse(boolean ok, String mensaje) {

    public static ApiResponse ok(String mensaje) {
        return new ApiResponse(true, mensaje);
    }
}
