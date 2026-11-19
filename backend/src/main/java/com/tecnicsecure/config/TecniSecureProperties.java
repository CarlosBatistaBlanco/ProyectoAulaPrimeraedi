package com.tecnicsecure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades propias de la aplicacion (prefijo "tecnisecure" en application.yml).
 */
@ConfigurationProperties(prefix = "tecnisecure")
public record TecniSecureProperties(
        Jwt jwt,
        Security security,
        Uploads uploads,
        Cors cors,
        Admin admin
) {

    public record Jwt(String secret, long expirationMinutes, long refreshExpirationMinutes) {
    }

    public record Security(int maxIntentosFallidos, int bloqueoMinutos) {
    }

    public record Uploads(String dir) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Admin(String correo, String password) {
    }
}
