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

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @Operation(summary = "Iniciar sesion y obtener tokens JWT")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    @Operation(summary = "Renovar el access token con el refresh token")
    @PostMapping("/refresh")
    public TokenResponse refrescar(@Valid @RequestBody RefreshRequest request) {
        return authService.refrescar(request.refreshToken());
    }

    @Operation(summary = "Registrar una cuenta de cliente")
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registro(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ok", true,
                "mensaje", "Cuenta creada con éxito. Ya puedes iniciar sesión.",
                "idUsuario", usuario.getIdUsuario()));
    }

    @Operation(summary = "Generar la pregunta del captcha")
    @GetMapping("/captcha")
    public Map<String, String> captcha() {
        CaptchaService.Desafio desafio = captchaService.generar();
        return Map.of(
                "pregunta", desafio.pregunta(),
                "token", desafio.ficha());
    }

    @Operation(summary = "Solicitar enlace de recuperacion de contrasena")
    @PostMapping("/recuperar")
    public Map<String, Object> recuperar(@Valid @RequestBody RecuperarRequest request) {
        String token = authService.solicitarRecuperacion(request.correo());
        // No se revela si el correo existe; en desarrollo se devuelve el token para probar el flujo.
        return Map.of(
                "ok", true,
                "mensaje", "Si el correo está registrado, recibirás un enlace de recuperación.",
                "tokenDesarrollo", token == null ? "" : token);
    }

    @Operation(summary = "Restablecer la contrasena usando el token")
    @PostMapping("/restablecer")
    public ApiResponse restablecer(@Valid @RequestBody RestablecerRequest request) {
        authService.restablecer(request.token(), request.password());
        return ApiResponse.ok("Contraseña actualizada correctamente.");
    }

    @Operation(summary = "Datos del usuario autenticado")
    @GetMapping("/me")
    public UsuarioResponse yo() {
        return authService.perfil(authService.usuarioAutenticado().getIdUsuario());
    }

    @Operation(summary = "Actualizar el propio perfil")
    @PutMapping("/me")
    public UsuarioResponse actualizarPerfil(@Valid @RequestBody PerfilRequest request) {
        Usuario usuario = authService.usuarioAutenticado();
        return authService.actualizarPerfil(usuario.getIdUsuario(), request);
    }
}
