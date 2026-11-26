/*
 * Este archivo arma una página de catálogo técnico interactivo donde se pueden ver y probar
 * todas las acciones disponibles de la tienda desde el navegador. Aparece al visitar la
 * dirección /swagger-ui.html y permite identificarse con un pase de sesión para hacer pruebas.
 */
package com.tecnicsecure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Nombre interno del candado de seguridad que aparece en la página de pruebas
    private static final String ESQUEMA_JWT = "bearerAuth";

    // Construye la portada interactiva con el listado de acciones de la tienda y activa el botón de acceso
    @Bean
    public OpenAPI tecniSecureOpenApi() {
        return new OpenAPI()
                // Datos generales visibles en el encabezado de la guía interactiva
                .info(new Info()
                        .title("TECNISECURE API")
                        .version("2.0.0")
                        .description("""
                                API REST de TECNISECURE (seguridad electrónica).
                                Arquitectura de tres capas + MVC con Spring Boot.
                                Autenticación con JWT: usa /api/auth/login y luego el botón Authorize.
                                """)
                        .contact(new Contact().name("TECNISECURE").email("contacto@tecnicsecure.com")))
                // Habilita el botón para pegar el pase de sesión y probar acciones privadas desde la guía
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
