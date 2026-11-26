/*
 * Este archivo busca la cuenta de una persona por su correo cuando intenta iniciar sesión
 * o cuando el sistema revisa su pase de entrada. Entrega al módulo de seguridad su clave
 * protegida, su tipo de cuenta (administrador, empleado o cliente) y si está activa o bloqueada.
 */
package com.tecnicsecure.security;

import com.tecnicsecure.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    // Conecta con el registro de cuentas para buscar usuarios por correo electrónico
    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Busca el usuario por correo y le entrega al sistema de seguridad su perfil y si puede ingresar
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Revisa que la cuenta esté encendida y no tenga un bloqueo temporal activo
        boolean habilitado = Boolean.TRUE.equals(usuario.getActivo())
                && !estaBloqueado(usuario);

        // Arma la ficha de seguridad interna con el permiso que corresponde al tipo de cuenta
        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getNombreRol().toUpperCase())))
                .accountLocked(!habilitado)
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }

    // Comprueba si la cuenta sigue dentro del tiempo de espera por demasiados intentos fallidos de clave
    private boolean estaBloqueado(com.tecnicsecure.entity.Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getBloqueado())
                && usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(java.time.LocalDateTime.now());
    }
}
