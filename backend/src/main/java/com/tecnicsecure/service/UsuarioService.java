package com.tecnicsecure.service;

import com.tecnicsecure.dto.auth.LogActividadResponse;
import com.tecnicsecure.dto.auth.UsuarioAdminRequest;
import com.tecnicsecure.dto.auth.UsuarioResponse;
import com.tecnicsecure.dto.comun.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface UsuarioService {

    PageResponse<UsuarioResponse> listar(String texto, String rol, Pageable pageable);

    UsuarioResponse obtener(Integer idUsuario);

    UsuarioResponse crear(UsuarioAdminRequest request);

    UsuarioResponse actualizar(Integer idUsuario, UsuarioAdminRequest request);

    void cambiarEstado(Integer idUsuario, boolean activo);

    /**
     * Elimina un usuario definitivamente. Requiere la clave del administrador
     * que ejecuta la accion (doble confirmacion) y conserva el historial de
     * pedidos/facturas desvinculandolo en lugar de romper las referencias.
     */
    void eliminarDefinitivo(Integer idUsuario, String claveAdministrador);

    PageResponse<LogActividadResponse> auditoria(Pageable pageable);

    PageResponse<LogActividadResponse> auditoria(String texto, String accion, LocalDate desde, LocalDate hasta, Pageable pageable);

    int depurarAuditoria(int diasRetencion);
}
