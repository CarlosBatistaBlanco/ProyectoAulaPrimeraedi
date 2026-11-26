/*
 * Este archivo permite que cualquier parte de la tienda consulte quién es la persona que
 * tiene la sesión abierta en ese instante y qué tipo de cuenta posee (cliente, empleado o
 * administrador). Se usa al comprar, guardar favoritos, revisar permisos y registrar actividad.
 */
package com.tecnicsecure.util;

import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    private final UsuarioRepository usuarioRepository;

    // Conecta con el registro de usuarios para obtener la ficha completa de quien inició sesión
    public UsuarioActual(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Entrega los datos de la persona conectada; si nadie inició sesión, avisa que debe entrar primero
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

    // Entrega el número de cuenta de quien está conectado, o vacío si se trata de un visitante sin sesión
    public Integer idOpcional() {
        try {
            return requerir().getIdUsuario();
        } catch (ApiException ex) {
            return null;
        }
    }

    // Responde si la persona conectada tiene una cuenta de administrador
    public boolean esAdministrador() {
        return tieneRol("administrador");
    }

    // Responde si la persona conectada tiene una cuenta de empleado
    public boolean esEmpleado() {
        return tieneRol("empleado");
    }

    // Responde si la persona conectada tiene una cuenta de cliente
    public boolean esCliente() {
        return tieneRol("cliente");
    }

    // Comprueba si la sesión activa cuenta con el permiso de un tipo de usuario específico
    public boolean tieneRol(String rol) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || autenticacion.getAuthorities() == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + rol.toUpperCase()).equals(a.getAuthority()));
    }

    // Indica si la persona conectada forma parte del equipo interno de la tienda (administrador o empleado)
    public boolean puedeGestionar() {
        return esAdministrador() || esEmpleado();
    }
}
