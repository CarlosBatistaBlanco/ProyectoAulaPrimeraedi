/*
 * Este archivo reúne la información visible de una cuenta de usuario que se muestra
 * en la barra superior, en la pantalla de perfil y en las tablas del panel administrativo.
 * Siempre deja por fuera la contraseña para proteger la privacidad de cada persona.
 */
package com.tecnicsecure.dto.auth;

import com.tecnicsecure.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos publicos del usuario autenticado")
public record UsuarioResponse(
        // Número único que identifica la cuenta
        Integer idUsuario,
        // Nombre completo de la persona
        String nombre,
        // Correo electrónico asociado a la cuenta
        String correo,
        // Documento de identidad registrado
        String documento,
        // Número telefónico de contacto
        String telefono,
        // Foto o imagen de perfil elegida
        String avatar,
        // Tipo de cuenta dentro del sistema (cliente, empleado o administrador)
        String rol,
        // Indica si la cuenta se encuentra activa
        Boolean activo,
        // Indica si la cuenta está bloqueada
        Boolean bloqueado,
        // Última fecha y hora en que la persona entró al sistema
        LocalDateTime ultimoAcceso,
        // Fecha y hora en que se creó la cuenta
        LocalDateTime creadoEn,
        // Identificador del perfil de cliente cuando la cuenta pertenece a un comprador
        @Schema(description = "Id del cliente asociado (solo para el rol cliente). "
                + "El frontend lo usa para suscribirse a /topic/chat/{idCliente}.")
        Integer idCliente
) {
    // Toma una cuenta guardada y prepara sus datos visibles sin número de cliente asociado
    public static UsuarioResponse de(Usuario u) {
        return de(u, null);
    }

    // Toma una cuenta guardada y prepara sus datos visibles incluyendo su identificador de cliente
    public static UsuarioResponse de(Usuario u, Integer idCliente) {
        return new UsuarioResponse(u.getIdUsuario(), u.getNombre(), u.getCorreo(), u.getDocumento(),
                u.getTelefono(), u.getAvatar(), u.getNombreRol(), u.getActivo(), u.getBloqueado(),
                u.getUltimoAcceso(), u.getCreadoEn(), idCliente);
    }
}
