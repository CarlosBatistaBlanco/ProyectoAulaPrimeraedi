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

    @Test
    @DisplayName("Genera un token con el correo y el rol del usuario")
    void generaTokenConClaims() {
        String token = jwtService.generarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extraerUsuario(token)).isEqualTo("admin@tecnicsecure.com");
        assertThat(jwtService.extraerRol(token)).isEqualTo("administrador");
        assertThat(jwtService.esTokenValido(token)).isTrue();
    }

    @Test
    @DisplayName("Distingue el refresh token del access token")
    void distingueRefreshToken() {
        String access = jwtService.generarToken(usuario);
        String refresh = jwtService.generarRefreshToken(usuario);

        assertThat(jwtService.esRefreshToken(access)).isFalse();
        assertThat(jwtService.esRefreshToken(refresh)).isTrue();
    }

    @Test
    @DisplayName("Un token alterado no es válido")
    void tokenAlteradoEsInvalido() {
        String token = jwtService.generarToken(usuario);
        String alterado = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.esTokenValido(alterado)).isFalse();
    }

    @Test
    @DisplayName("Un token expirado no es válido")
    void tokenExpiradoEsInvalido() {
        JwtService servicioCaduco = new JwtService(SECRETO, -1, -1);
        String token = servicioCaduco.generarToken(usuario);

        assertThat(servicioCaduco.esTokenValido(token)).isFalse();
    }

    @Test
    @DisplayName("Rechaza secretos demasiado cortos (firma HS256)")
    void rechazaSecretoCorto() {
        assertThatThrownBy(() -> new JwtService("corto", 10, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }
}
