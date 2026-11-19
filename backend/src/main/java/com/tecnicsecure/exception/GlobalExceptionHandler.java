package com.tecnicsecure.exception;

import com.tecnicsecure.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce errores Java a respuestas JSON uniformes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> manejarApi(ApiException ex, HttpServletRequest request) {
        return construir(ex.getStatus(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return construir(400, "Datos invalidos", request, campos);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(DataIntegrityViolationException ex,
                                                           HttpServletRequest request) {
        return construir(409, "La operacion viola una restriccion de la base de datos", request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametro(MissingServletRequestParameterException ex,
                                                          HttpServletRequest request) {
        return construir(400, "Falta el parametro: " + ex.getParameterName(), request, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> manejarCredenciales(BadCredentialsException ex,
                                                             HttpServletRequest request) {
        return construir(401, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAcceso(AccessDeniedException ex, HttpServletRequest request) {
        return construir(403, "No tiene permisos para realizar esta accion", request, null);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(
            org.springframework.web.servlet.resource.NoResourceFoundException ex,
            HttpServletRequest request) {
        return construir(404, "Recurso no encontrado: " + ex.getResourcePath(), request, null);
    }

    @ExceptionHandler(org.springframework.security.authentication.LockedException.class)
    public ResponseEntity<ErrorResponse> manejarBloqueo(
            org.springframework.security.authentication.LockedException ex, HttpServletRequest request) {
        return construir(423, ex.getMessage(), request, null);
    }

    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<ErrorResponse> manejarDeshabilitado(
            org.springframework.security.authentication.DisabledException ex, HttpServletRequest request) {
        return construir(403, ex.getMessage(), request, null);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonInvalido(
            org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construir(400, "El formato de los datos no es valido", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGeneral(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return construir(500, "Error interno del servidor: " + ex.getClass().getSimpleName(), request, null);
    }

    private ResponseEntity<ErrorResponse> construir(int status, String mensaje,
                                                    HttpServletRequest request,
                                                    Map<String, String> campos) {
        ErrorResponse cuerpo = new ErrorResponse(
                LocalDateTime.now(),
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                mensaje,
                request.getRequestURI(),
                campos);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
