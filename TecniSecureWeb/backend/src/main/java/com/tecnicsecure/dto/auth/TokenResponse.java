/*
 * Este archivo reúne las llaves de acceso y los datos básicos del usuario que el
 * sistema entrega cuando alguien inicia sesión correctamente o renueva su sesión.
 * Permite que las distintas pantallas reconozcan quién está conectado y qué permisos tiene.
 */
package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens emitidos al iniciar sesion")
public record TokenResponse(
        // Llave principal de acceso que comprueba la identidad de la persona en cada pantalla
        String accessToken,
        // Llave de respaldo que permite extender la sesión cuando la principal vence
        String refreshToken,
        // Etiqueta estándar que indica cómo presentar la llave de acceso
        String tokenType,
        // Tiempo en segundos que permanecerá vigente la llave principal
        long expiraEnSegundos,
        // Información pública de la cuenta que acaba de iniciar sesión
        UsuarioResponse usuario
) {
    // Arma la respuesta completa de inicio de sesión dejando configurado el tipo de credencial estándar
    public static TokenResponse de(String access, String refresh, long segundos, UsuarioResponse usuario) {
        return new TokenResponse(access, refresh, "Bearer", segundos, usuario);
    }
}
