/*
 * Este archivo define las acciones para administrar las cuentas de personas en la tienda
 * (clientes, empleados y administradores) y revisar el registro de actividad del negocio.
 * Su trabajo se nota en los modulos de Usuarios y Auditoria del panel administrativo.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.auth.LogActividadResponse;
import com.tecnicsecure.dto.auth.UsuarioAdminRequest;
import com.tecnicsecure.dto.auth.UsuarioResponse;
import com.tecnicsecure.dto.comun.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface UsuarioService {

    // Busca usuarios por nombre, correo o rol y los muestra divididos en paginas.
    PageResponse<UsuarioResponse> listar(String texto, String rol, Pageable pageable);

    // Consulta la informacion completa de una cuenta usando su numero identificador.
    UsuarioResponse obtener(Integer idUsuario);

    // Crea una cuenta nueva desde el panel administrativo asignandole su rol (cliente, empleado o admin).
    UsuarioResponse crear(UsuarioAdminRequest request);

    // Cambia los datos, el rol, la contrasena o el estado de bloqueo de una cuenta existente.
    UsuarioResponse actualizar(Integer idUsuario, UsuarioAdminRequest request);

    // Activa o pausa temporalmente el acceso de una persona sin borrar su cuenta.
    void cambiarEstado(Integer idUsuario, boolean activo);

    // Borra definitivamente una cuenta pidiendo la clave del administrador, pero conserva intactas sus facturas y pedidos pasados.
    void eliminarDefinitivo(Integer idUsuario, String claveAdministrador);

    // Muestra el historial general de acciones importantes realizadas en la plataforma.
    PageResponse<LogActividadResponse> auditoria(Pageable pageable);

    // Filtra el historial de acciones por palabra clave, tipo de movimiento y rango de fechas.
    PageResponse<LogActividadResponse> auditoria(String texto, String accion, LocalDate desde, LocalDate hasta, Pageable pageable);

    // Borra registros muy antiguos del historial de actividad conservando siempre los dias recientes obligatorios.
    int depurarAuditoria(int diasRetencion);
}
