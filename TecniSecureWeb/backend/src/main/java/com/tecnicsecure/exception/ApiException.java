/*
 * Este archivo define los avisos de error propios de la tienda cuando algo no se puede
 * completar en cualquier pantalla (por ejemplo: falta un dato en un formulario, falta
 * iniciar sesión, no hay permiso, un producto no existe o se intentó repetir una acción).
 */
package com.tecnicsecure.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    // Código interno de respuesta que ayuda a la pantalla a saber qué tipo de problema ocurrió
    private final int status;

    // Crea un aviso de error con su código y el mensaje claro que verá la persona en pantalla
    public ApiException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    // Avisa que algún dato enviado desde el formulario está vacío, mal escrito o fuera de regla
    public static ApiException badRequest(String mensaje) {
        return new ApiException(400, mensaje);
    }

    // Avisa que la persona debe iniciar sesión o volver a entrar porque su sesión caducó
    public static ApiException unauthorized(String mensaje) {
        return new ApiException(401, mensaje);
    }

    // Avisa que la persona ya entró con su cuenta, pero su rol no tiene permiso para esa acción
    public static ApiException forbidden(String mensaje) {
        return new ApiException(403, mensaje);
    }

    // Avisa que el producto, pedido, usuario o registro buscado no existe
    public static ApiException notFound(String mensaje) {
        return new ApiException(404, mensaje);
    }

    // Avisa que la acción choca con algo ya guardado (como un correo repetido o una factura ya pagada)
    public static ApiException conflict(String mensaje) {
        return new ApiException(409, mensaje);
    }
}
