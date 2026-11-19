package com.tecnicsecure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentacion OpenAPI (Swagger UI en /swagger-ui.html).
 * Permite autenticarse con "Authorize" usando el access token JWT.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI tecniSecureOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TECNISECURE API")
                        .version("2.0.0")
                        .description("""
                                API REST de TECNISECURE (seguridad electrónica).
                                Arquitectura de tres capas + MVC con Spring Boot.
                                Autenticación con JWT: usa /api/auth/login y luego el botón Authorize.
                                """)
                        .contact(new Contact().name("TECNISECURE").email("contacto@tecnicsecure.com")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
