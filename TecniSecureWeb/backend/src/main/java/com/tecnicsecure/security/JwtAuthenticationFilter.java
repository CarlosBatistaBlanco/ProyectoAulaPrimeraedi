/*
 * Este archivo actúa como el portero automático que revisa cada acción que envían las
 * pantallas de la tienda antes de dejarla pasar. Comprueba si la persona trae un pase
 * de sesión válido y si su cuenta sigue activa, para que cada panel sepa quién está conectado.
 */
package com.tecnicsecure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    // Conecta con el verificador de pases de sesión y con el buscador de cuentas registradas
    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    // Revisa cada solicitud que llega desde la página web para verificar si trae un pase de sesión válido
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Busca el pase de seguridad adjunto a la solicitud; si no trae uno, la deja seguir como visitante
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Toma solo el código del pase quitando la palabra inicial que lo acompaña
        String token = cabecera.substring(7).trim();
        try {
            // Revisa que el pase sea auténtico, que sea de acceso diario (no de renovación) y que aún no esté cargado
            if (jwtService.esTokenValido(token) && !jwtService.esRefreshToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Lee el correo guardado dentro del pase y busca la cuenta real en el sistema
                String correo = jwtService.extraerUsuario(token);
                UserDetails detalles = usuarioDetailsService.loadUserByUsername(correo);

                // Solo reconoce la sesión si la cuenta sigue activa y no fue bloqueada
                if (detalles.isEnabled() && detalles.isAccountNonLocked()) {
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            detalles, null, detalles.getAuthorities());
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            }
        } catch (RuntimeException ex) {
            // Si el pase es falso o ya venció, limpia la sesión para que el sistema pida entrar de nuevo
            SecurityContextHolder.clearContext();
        }

        // Deja que la solicitud continúe su camino hacia la pantalla o acción solicitada
        filterChain.doFilter(request, response);
    }
}
