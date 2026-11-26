/*
 * Este archivo reúne en un solo lugar las reglas y ajustes propios del negocio que el
 * sistema lee al encenderse, como cuánto dura una sesión abierta, cuántos intentos de
 * clave se permiten antes de bloquear una cuenta, dónde se guardan las fotos subidas y
 * los datos iniciales del administrador.
 */
package com.tecnicsecure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Carga las opciones propias de la tienda definidas en la hoja de configuración general
@ConfigurationProperties(prefix = "tecnisecure")
public record TecniSecureProperties(
        Jwt jwt,
        Security security,
        Uploads uploads,
        Cors cors,
        Admin admin
) {

    // Firma secreta y duración en minutos de los pases de sesión y renovación
    public record Jwt(String secret, long expirationMinutes, long refreshExpirationMinutes) {
    }

    // Límite de intentos fallidos al iniciar sesión y minutos que dura el bloqueo temporal
    public record Security(int maxIntentosFallidos, int bloqueoMinutos) {
    }

    // Carpeta física donde se almacenan las fotos y archivos que suben los usuarios
    public record Uploads(String dir) {
    }

    // Lista de direcciones web autorizadas para comunicarse con la tienda
    public record Cors(List<String> allowedOrigins) {
    }

    // Correo y clave de arranque para crear la cuenta del administrador principal
    public record Admin(String correo, String password) {
    }
}
