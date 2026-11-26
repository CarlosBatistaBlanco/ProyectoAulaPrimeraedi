/*
 * Este archivo define las reglas generales de seguridad de toda la tienda: qué páginas
 * puede visitar cualquier persona sin cuenta (portada, catálogo, contacto y chat público),
 * qué acciones exigen iniciar sesión y cuáles son exclusivas de empleados o administradores.
 * También protege las contraseñas guardándolas de forma ilegible.
 */
package com.tecnicsecure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioDetailsService usuarioDetailsService;
    private final com.tecnicsecure.config.TecniSecureProperties propiedades;

    // Conecta con el revisor de pases de sesión, el buscador de cuentas y los ajustes de direcciones permitidas
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          UsuarioDetailsService usuarioDetailsService,
                          com.tecnicsecure.config.TecniSecureProperties propiedades) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.usuarioDetailsService = usuarioDetailsService;
        this.propiedades = propiedades;
    }

    // Arma el reglamento completo de permisos para cada pantalla y acción del sistema
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desactiva el control antiguo de formularios porque la tienda valida cada acción con pase de sesión
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Permite abrir la consola interna de datos y la guía interactiva dentro del mismo sitio
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // No retiene sesiones abiertas en memoria: cada solicitud se comprueba con su propio pase
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Archivos visuales públicos de la tienda: pantallas, estilos, fotos, videos y soporte sin conexión
                .requestMatchers("/", "/*.html", "/manifest.webmanifest", "/manifest.json",
                                 "/sw.js", "/offline.html", "/css/**", "/js/**", "/img/**",
                                 "/video/**", "/audio/**", "/uploads/**", "/pages/**",
                                 "/panel/**", "/assets/**", "/favicon.ico").permitAll()
                // Herramientas de consulta técnica, canal de avisos en vivo y revisión de estado del servidor
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                 "/h2-console/**", "/ws/**", "/actuator/health", "/actuator/health/**").permitAll()
                // Pantallas públicas de entrada: iniciar sesión, crear cuenta, recuperar clave y pregunta anti-robots
                .requestMatchers("/api/auth/login", "/api/auth/registro",
                                 "/api/auth/recuperar", "/api/auth/restablecer",
                                 "/api/auth/refresh", "/api/auth/captcha").permitAll()
                // Burbuja de chat pública para visitantes sin cuenta (usa una clave privada de su conversación)
                .requestMatchers(HttpMethod.POST, "/api/asistente", "/api/asistente/mensajes").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/asistente/conversacion/**").permitAll()
                // Vitrina pública de solo lectura: cualquiera puede ver productos, categorías, marcas y servicios
                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**",
                                 "/api/marcas/**", "/api/servicios/**").permitAll()
                // Formularios abiertos para escribir a contacto o pedir una cotización técnica
                .requestMatchers(HttpMethod.POST, "/api/contactos", "/api/solicitudes").permitAll()
                // Cambios al catálogo reservados solo para administradores y empleados autorizados
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyRole("ADMINISTRADOR", "EMPLEADO")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyRole("ADMINISTRADOR", "EMPLEADO")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/categorias/**", "/api/marcas/**", "/api/servicios/**")
                    .hasAnyRole("ADMINISTRADOR", "EMPLEADO")
                // Todo lo demás (paneles, compras, perfil, bodega, reportes) requiere haber iniciado sesión
                .anyRequest().authenticated())
            // Conecta el verificador de cuentas y antepone el revisor de pases de sesión
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // Envía un aviso claro a la pantalla cuando falta iniciar sesión o cuando el usuario no tiene permiso
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        escribirError(response, 401, "No autenticado o token invalido/expirado"))
                .accessDeniedHandler((request, response, deniedException) ->
                        escribirError(response, 403, "No tiene permisos para esta accion")));

        return http.build();
    }

    // Escribe el mensaje ordenado que recibe la pantalla cuando se niega un acceso por seguridad
    private void escribirError(jakarta.servlet.http.HttpServletResponse response,
                               int status, String mensaje) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":" + status + ",\"error\":\"" + (status == 401 ? "Unauthorized" : "Forbidden")
                        + "\",\"mensaje\":\"" + mensaje + "\"}");
    }

    // Define cómo se busca la cuenta y cómo se compara la clave protegida al iniciar sesión
    @Bean
    public org.springframework.security.authentication.AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // Convierte cada contraseña en una clave protegida que nadie puede leer; además sigue reconociendo claves antiguas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Deja listo el verificador principal de credenciales que usa la pantalla de Iniciar Sesión
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Lista qué direcciones web tienen permiso para enviar y recibir información con el servidor de la tienda
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.cors() != null && propiedades.cors().allowedOrigins() != null
                ? propiedades.cors().allowedOrigins()
                : List.of("http://localhost:8080"));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("*"));
        configuracion.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
