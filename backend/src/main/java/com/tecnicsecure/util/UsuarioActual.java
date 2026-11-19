package com.tecnicsecure.util;

import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acceso al usuario autenticado desde cualquier capa sin depender del
 * controller (el service necesita saber "quien" para auditar y filtrar).
 */
@Component
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActual(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario requerir() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(autenticacion.getPrincipal())) {
            throw ApiException.unauthorized("Debe iniciar sesion");
        }
        String correo = autenticacion.getName();
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> ApiException.unauthorized("Sesion no valida"));
    }

    public Integer idOpcional() {
        try {
            return requerir().getIdUsuario();
        } catch (ApiException ex) {
            return null;
        }
    }

    public boolean esAdministrador() {
        return tieneRol("administrador");
    }

    public boolean esEmpleado() {
        return tieneRol("empleado");
    }

    public boolean esCliente() {
        return tieneRol("cliente");
    }

    public boolean tieneRol(String rol) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || autenticacion.getAuthorities() == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + rol.toUpperCase()).equals(a.getAuthority()));
    }

    /** Administradores y empleados comparten la gestion operativa. */
    public boolean puedeGestionar() {
        return esAdministrador() || esEmpleado();
    }
}
