package com.tecnicsecure.exception;

import lombok.Getter;

/** Error de negocio con estado HTTP asociado. */
@Getter
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public static ApiException badRequest(String mensaje) {
        return new ApiException(400, mensaje);
    }

    public static ApiException unauthorized(String mensaje) {
        return new ApiException(401, mensaje);
    }

    public static ApiException forbidden(String mensaje) {
        return new ApiException(403, mensaje);
    }

    public static ApiException notFound(String mensaje) {
        return new ApiException(404, mensaje);
    }

    public static ApiException conflict(String mensaje) {
        return new ApiException(409, mensaje);
    }
}
