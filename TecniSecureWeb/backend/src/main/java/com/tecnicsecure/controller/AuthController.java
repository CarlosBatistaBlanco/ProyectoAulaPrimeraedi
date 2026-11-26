/*
 * Este archivo atiende las pantallas de Iniciar Sesión, Crear Cuenta, Recuperar Contraseña
 * y Mi Perfil. Revisa el correo y la clave cuando alguien entra, protege el registro con una
 * pregunta sencilla contra robots y mantiene activa la sesión del usuario mientras navega.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.dto.ApiResponse;
import com.tecnicsecure.dto.auth.*;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.service.AuthService;
import com.tecnicsecure.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "1. Autenticacion", description = "Login JWT, registro, recuperacion y perfil")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    // Conecta con los encargados de validar entradas de usuarios y generar preguntas anti-robots
    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    // Revisa el correo y la clave escritos en Iniciar Sesión y entrega los pases de acceso a la cuenta
    @Operation(summary = "Iniciar sesion y obtener tokens JWT")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    // Renueva el pase de sesión vencido usando el pase de renovación, sin pedir la clave de nuevo
    @Operation(summary = "Renovar el access token con el refresh token")
    @PostMapping("/refresh")
    public TokenResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authService.refrescar(request.refreshToken());
    }

    // Crea una cuenta nueva desde la pantalla de Registro y confirma que ya se puede entrar
    @Operation(summary = "Registrar una cuenta de cliente")
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registro(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = authService.registrar(request);
        // Confirma que la cuenta se creó correctamente y devuelve su número de identificación
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ok", true,
                "mensaje", "Cuenta creada con éxito. Ya puedes iniciar sesión.",
                "idUsuario", usuario.getIdUsuario()));
    }

    // Crea una pregunta matemática sencilla para comprobar que quien se registra es una persona real
    @Operation(summary = "Generar la pregunta del captcha")
    @GetMapping("/captcha")
    public Map<String, String> captcha() {
        CaptchaService.Desafio desafio = captchaService.generar();
        return Map.of(
                "pregunta", desafio.pregunta(),
                "token", desafio.ficha());
    }

    // Inicia el proceso de recuperación cuando alguien olvida su contraseña y escribe su correo
    @Operation(summary = "Solicitar enlace de recuperacion de contrasena")
    @PostMapping("/recuperar")
    public Map<String, Object> recuperar(@Valid @RequestBody RecuperarRequest request) {
        String token = authService.solicitarRecuperacion(request.correo());
        // Por privacidad siempre da la misma respuesta sin confirmar si ese correo existe o no
        return Map.of(
                "ok", true,
                "mensaje", "Si el correo está registrado, recibirás un enlace de recuperación.",
                "tokenDesarrollo", token == null ? "" : token);
    }

    // Guarda la contraseña nueva luego de comprobar el código de recuperación enviado al usuario
    @Operation(summary = "Restablecer la contrasena usando el token")
    @PostMapping("/restablecer")
    public ApiResponse restablecer(@Valid @RequestBody RestablecerRequest request) {
        authService.restablecer(request.token(), request.password());
        return ApiResponse.ok("Contraseña actualizada correctamente.");
    }

    // Muestra los datos actuales de la persona que tiene la sesión abierta en la pantalla Mi Perfil
    @Operation(summary = "Datos del usuario autenticado")
    @GetMapping("/me")
    public UsuarioResponse yo() {
        return authService.perfil(authService.usuarioAutenticado().getIdUsuario());
    }

    // Guarda los cambios que la persona hace sobre su propio nombre, teléfono o foto en Mi Perfil
    @Operation(summary = "Actualizar el propio perfil")
    @PutMapping("/me")
    public UsuarioResponse actualizarPerfil(@Valid @RequestBody PerfilRequest request) {
        Usuario usuario = authService.usuarioAutenticado();
        return authService.actualizarPerfil(usuario.getIdUsuario(), request);
    }
}
