/*
 * Este archivo define las acciones disponibles para manejar las cuentas y el acceso
 * a la plataforma: iniciar sesion, registrarse como cliente, recuperar una clave olvidada
 * y editar el perfil. Su trabajo se nota en las pantallas de ingreso, registro y Mi perfil.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.auth.*;
import com.tecnicsecure.entity.Usuario;

public interface AuthService {

    // Revisa el correo, la clave y la verificacion de persona para dejar entrar al usuario.
    TokenResponse login(LoginRequest request, String ip);

    // Mantiene la sesion abierta automaticamente sin pedir de nuevo la contrasena.
    TokenResponse refrescar(String refreshToken);

    // Crea una cuenta nueva para un cliente que se registra en la tienda.
    Usuario registrar(RegistroRequest request);

    // Inicia la recuperacion de una clave olvidada generando un enlace temporal para cambiarla.
    String solicitarRecuperacion(String correo);

    // Guarda la nueva contrasena del usuario usando el enlace temporal de recuperacion.
    void restablecer(String token, String nuevaPassword);

    // Consulta los datos personales de la cuenta para mostrarlos en la pantalla de perfil.
    UsuarioResponse perfil(Integer idUsuario);

    // Actualiza los datos personales o cambia la contrasena desde el perfil del usuario.
    UsuarioResponse actualizarPerfil(Integer idUsuario, PerfilRequest request);

    // Crea una suma sencilla para comprobar que quien intenta entrar es una persona real.
    String generarPreguntaCaptcha();

    // Comprueba si el numero que escribio la persona coincide con la suma de verificacion.
    boolean validarCaptcha(String respuesta);

    // Identifica cual es la persona que tiene la sesion abierta en este momento.
    Usuario usuarioAutenticado();
}
