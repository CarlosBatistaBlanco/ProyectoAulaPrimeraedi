package com.tecnicsecure.security;

import com.tecnicsecure.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Carga el usuario para Spring Security. El rol se expone como autoridad
 * ROLE_<nombre> (ej. ROLE_ADMINISTRADOR) para usar @PreAuthorize.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        boolean habilitado = Boolean.TRUE.equals(usuario.getActivo())
                && !estaBloqueado(usuario);

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getNombreRol().toUpperCase())))
                .accountLocked(!habilitado)
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }

    private boolean estaBloqueado(com.tecnicsecure.entity.Usuario usuario) {
        return Boolean.TRUE.equals(usuario.getBloqueado())
                && usuario.getBloqueadoHasta() != null
                && usuario.getBloqueadoHasta().isAfter(java.time.LocalDateTime.now());
    }
}
