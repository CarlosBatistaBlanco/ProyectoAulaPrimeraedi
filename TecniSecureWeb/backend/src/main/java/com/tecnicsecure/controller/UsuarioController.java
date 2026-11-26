/*
 * Este archivo atiende las pantallas de Usuarios y Auditoría que solo puede ver el
 * administrador en su panel. Permite crear, editar, suspender o eliminar cuentas de la
 * tienda, así como revisar, exportar a Excel o limpiar el historial de actividad del sistema.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.dto.auth.LogActividadResponse;
import com.tecnicsecure.dto.auth.UsuarioAdminRequest;
import com.tecnicsecure.dto.auth.UsuarioResponse;
import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

@Tag(name = "7. Usuarios y Auditoria", description = "Gestion administrativa de usuarios y registro de actividad")
@RestController
@RequestMapping("/api")
// Todas las acciones de gestión de cuentas y revisión de historial son exclusivas del administrador
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // Conecta con el encargado de administrar cuentas y consultar el registro histórico de eventos
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Busca usuarios por nombre, correo o tipo de cuenta y los entrega por páginas (hasta 100 por hoja)
    @Operation(summary = "Listar usuarios con filtros por texto y rol")
    @GetMapping("/usuarios")
    public PageResponse<UsuarioResponse> listar(@RequestParam(required = false) String texto,
                                                @RequestParam(required = false) String rol,
                                                @RequestParam(defaultValue = "0") int pagina,
                                                @RequestParam(defaultValue = "50") int tamanio) {
        return usuarioService.listar(texto, rol,
                PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanio)),
                        Sort.by(Sort.Direction.ASC, "idUsuario")));
    }

    // Muestra la ficha completa de una cuenta específica al seleccionarla en la tabla
    @Operation(summary = "Obtener un usuario por id")
    @GetMapping("/usuarios/{id}")
    public UsuarioResponse obtener(@PathVariable Integer id) {
        return usuarioService.obtener(id);
    }

    // Crea una cuenta nueva de administrador, empleado o cliente desde el formulario administrativo
    @Operation(summary = "Crear un usuario (administrador, empleado o cliente)")
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    // Guarda cambios en los datos personales, tipo de cuenta o estado de un usuario existente
    @Operation(summary = "Actualizar datos, rol o estado de un usuario")
    @PutMapping("/usuarios/{id}")
    public UsuarioResponse actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioAdminRequest request) {
        return usuarioService.actualizar(id, request);
    }

    // Suspende temporalmente una cuenta para que no pueda entrar, sin borrar su historial
    @Operation(summary = "Desactivar un usuario (baja logica)")
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Integer id) {
        usuarioService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }

    // Elimina una cuenta de forma permanente luego de confirmar la clave del administrador
    @Operation(summary = "Eliminar un usuario DEFINITIVAMENTE (requiere la clave del administrador)")
    @DeleteMapping("/usuarios/{id}/definitivo")
    public ResponseEntity<Void> eliminarDefinitivo(@PathVariable Integer id,
                                                   @RequestBody(required = false) Map<String, String> body) {
        String clave = body != null ? body.get("clave") : null;
        usuarioService.eliminarDefinitivo(id, clave);
        return ResponseEntity.noContent().build();
    }

    // Enciende o apaga el interruptor de acceso de una cuenta (activa o bloqueada)
    @Operation(summary = "Activar o desactivar un usuario")
    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Integer id,
                                              @RequestBody Map<String, Boolean> body) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        usuarioService.cambiarEstado(id, activo);
        return ResponseEntity.noContent().build();
    }

    // Muestra el historial de movimientos y acciones hechas en la tienda con filtros por texto, tipo o fechas
    @Operation(summary = "Consultar historial de auditoria del sistema con filtros")
    @GetMapping("/auditoria")
    public PageResponse<LogActividadResponse> auditoria(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {
        return usuarioService.auditoria(texto, accion, desde, hasta,
                PageRequest.of(Math.max(0, pagina), Math.min(200, Math.max(1, tamanio))));
    }

    // Descarga hasta 1.000 eventos del historial de actividad en una hoja de cálculo lista para Excel
    @Operation(summary = "Exportar registro de auditoria en CSV")
    @GetMapping(value = "/auditoria/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportarAuditoria(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        var datos = usuarioService.auditoria(texto, accion, desde, hasta, PageRequest.of(0, 1000));
        // Arma la tabla separada por punto y coma con una marca inicial para que Excel respete tildes y eñes
        StringBuilder sb = new StringBuilder("FechaHora;Usuario;Rol;Accion;Detalle\r\n");
        for (var l : datos.contenido()) {
            sb.append(l.creadoEn()).append(';')
              .append(limpiarCsv(l.nombreUsuario())).append(';')
              .append(limpiarCsv(l.rolUsuario())).append(';')
              .append(limpiarCsv(l.accion())).append(';')
              .append(limpiarCsv(l.detalle())).append("\r\n");
        }
        byte[] bytes = ("\uFEFF" + sb).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    // Borra registros antiguos del historial conservando siempre al menos los últimos 30 días
    @Operation(summary = "Depurar registros antiguos de auditoria (minimo 30 dias de retencion)")
    @DeleteMapping("/auditoria/depurar")
    public Map<String, Integer> depurarAuditoria(@RequestParam(defaultValue = "90") int dias) {
        return Map.of("eliminados", usuarioService.depurarAuditoria(dias));
    }

    // Limpia saltos de línea y puntos y comas de un texto para que las columnas de Excel queden alineadas
    private String limpiarCsv(String val) {
        return val == null ? "" : val.replace(';', ',').replace('\r', ' ').replace('\n', ' ');
    }
}
