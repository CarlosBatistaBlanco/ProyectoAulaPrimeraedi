package com.tecnicsecure.service;

import com.tecnicsecure.dto.auth.*;
import com.tecnicsecure.entity.Usuario;

public interface AuthService {

    TokenResponse login(LoginRequest request, String ip);

    TokenResponse refrescar(String refreshToken);

    Usuario registrar(RegistroRequest request);

    /** Devuelve el enlace de restablecimiento (en desarrollo se muestra en pantalla). */
    String solicitarRecuperacion(String correo);

    void restablecer(String token, String nuevaPassword);

    UsuarioResponse perfil(Integer idUsuario);

    UsuarioResponse actualizarPerfil(Integer idUsuario, PerfilRequest request);

    String generarPreguntaCaptcha();

    boolean validarCaptcha(String respuesta);

    Usuario usuarioAutenticado();
}
