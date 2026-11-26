/*
 * Este archivo es el botón de encendido general de todo el sistema de la tienda.
 * Cuando se inicia el servidor, pone en marcha la página web, la seguridad de las
 * cuentas, el guardado de datos y todos los módulos visibles para clientes y personal.
 */
package com.tecnicsecure;

import com.tecnicsecure.config.TecniSecureProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// Enciende la tienda completa y carga los ajustes generales guardados del proyecto
@SpringBootApplication
@EnableConfigurationProperties(TecniSecureProperties.class)
public class TecniSecureApplication {

    // Arranque principal: enciende el servidor y deja la tienda lista para recibir visitas
    public static void main(String[] args) {
        SpringApplication.run(TecniSecureApplication.class, args);
    }
}