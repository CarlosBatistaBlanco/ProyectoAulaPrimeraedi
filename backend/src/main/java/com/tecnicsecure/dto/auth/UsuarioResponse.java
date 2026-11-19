package com.tecnicsecure.dto.auth;

import com.tecnicsecure.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Datos publicos del usuario autenticado")
public record UsuarioResponse(
        Integer idUsuario,
        String nombre,
        String correo,
        String documento,
        String telefono,
        String avatar,
        String rol,
        Boolean activo,
        Boolean bloqueado,
        LocalDateTime ultimoAcceso,
        LocalDateTime creadoEn,
        @Schema(description = "Id del cliente asociado (solo para el rol cliente). "
                + "El frontend lo usa para suscribirse a /topic/chat/{idCliente}.")
        Integer idCliente
) {
    public static UsuarioResponse de(Usuario u) {
        return de(u, null);
    }

    public static UsuarioResponse de(Usuario u, Integer idCliente) {
        return new UsuarioResponse(u.getIdUsuario(), u.getNombre(), u.getCorreo(), u.getDocumento(),
                u.getTelefono(), u.getAvatar(), u.getNombreRol(), u.getActivo(), u.getBloqueado(),
                u.getUltimoAcceso(), u.getCreadoEn(), idCliente);
    }
}
