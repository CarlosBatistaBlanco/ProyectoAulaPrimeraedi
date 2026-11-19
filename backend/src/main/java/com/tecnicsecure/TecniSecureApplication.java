package com.tecnicsecure;

import com.tecnicsecure.config.TecniSecureProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada de la API REST de TECNISECURE.
 *
 * Arquitectura de tres capas:
 *   - Presentacion (controller + security): recibe HTTP, valida y responde JSON (MVC).
 *   - Negocio (service): reglas del sistema (stock, estados, permisos, calculos).
 *   - Datos (repository + entity): acceso a la base de datos mediante Spring Data JPA.
 *
 * Base de datos: H2 embebida en archivo (backend/data/). No requiere instalar
 * ni arrancar ningun motor de base de datos externo.
 */
@SpringBootApplication
@EnableConfigurationProperties(TecniSecureProperties.class)
public class TecniSecureApplication {

    public static void main(String[] args) {
        SpringApplication.run(TecniSecureApplication.class, args);
    }
}