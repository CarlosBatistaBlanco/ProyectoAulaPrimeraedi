/*
 * Este archivo contiene pruebas automáticas para las llaves de acceso que mantienen
 * iniciada la sesión de cada persona. Comprueba que guarden bien el correo y el rol,
 * que caduquen al vencerse su tiempo y que rechacen cualquier intento de alteración.
 */
package com.tecnicsecure.security;

import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService · generación y validación de tokens")
class JwtServiceTest {

    private static final String SECRETO =
            "5b3f8d2a91c64e7fb0a4d8e2c6f19b7a3d5e8f2c4b6a9d1e3f7c8b2a5d9e4f6c";

    private JwtService jwtService;
    private Usuario usuario;

    // Prepara el generador de llaves de sesión y una cuenta administradora de ejemplo antes de cada prueba
    @BeforeEach
    void preparar() {
        jwtService = new JwtService(SECRETO, 120, 10080);

        Rol rol = new Rol();
        rol.setIdRol(1);
        rol.setNombre(Rol.ADMINISTRADOR);

        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Administrador Principal");
        usuario.setCorreo("admin@tecnicsecure.com");
        usuario.setRol(rol);
    }

    // Comprueba que al emitir una llave de sesión se pueda leer correctamente el correo y el rol del usuario
    @Test
    @DisplayName("Genera un token con el correo y el rol del usuario")
    void generaTokenConClaims() {
        String token = jwtService.generarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraerUsuario(token)).isEqualTo("admin@tecnicsecure.com");
        assertThat(jwtService.extraerRol(token)).isEqualTo("administrador");
        assertThat(jwtService.esTokenValido(token)).isTrue();
    }

    // Comprueba que el sistema distinga sin confusiones entre la llave de uso diario y la llave de renovación
    @Test
    @DisplayName("Distingue el refresh token del access token")
    void distingueRefreshToken() {
        String access = jwtService.generarToken(usuario);
        String refresh = jwtService.generarRefreshToken(usuario);

        assertThat(jwtService.esRefreshToken(access)).isFalse();
        assertThat(jwtService.esRefreshToken(refresh)).isTrue();
    }

    // Comprueba que si alguien manipula una credencial de sesión, el sistema la detecte y la rechace
    @Test
    @DisplayName("Un token alterado no es válido")
    void tokenAlteradoEsInvalido() {
        String token = jwtService.generarToken(usuario);
        String alterado = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.esTokenValido(alterado)).isFalse();
    }

    // Comprueba que una credencial cuyo tiempo de vigencia ya venció deje de ser aceptada
    @Test
    @DisplayName("Un token expirado no es válido")
    void tokenExpiradoEsInvalido() {
        JwtService servicioCaduco = new JwtService(SECRETO, -1, -1);
        String token = servicioCaduco.generarToken(usuario);

        assertThat(servicioCaduco.esTokenValido(token)).isFalse();
    }

    // Comprueba que el sistema impida arrancar con una clave secreta demasiado corta e insegura
    @Test
    @DisplayName("Rechaza secretos demasiado cortos (firma HS256)")
    void rechazaSecretoCorto() {
        assertThatThrownBy(() -> new JwtService("corto", 10, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }
}
