package com.tecnicsecure.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens emitidos al iniciar sesion")
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiraEnSegundos,
        UsuarioResponse usuario
) {
    public static TokenResponse de(String access, String refresh, long segundos, UsuarioResponse usuario) {
        return new TokenResponse(access, refresh, "Bearer", segundos, usuario);
    }
}
