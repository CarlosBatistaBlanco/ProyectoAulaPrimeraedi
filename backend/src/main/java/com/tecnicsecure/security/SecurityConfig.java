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

/**
 * Configuracion central de Spring Security.
 * - API sin estado (stateless) autenticada con JWT.
 * - Rutas publicas: catalogo, contacto, registro, login, Swagger y el frontend.
 * - Todo lo demas exige token; los roles se controlan con @PreAuthorize.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioDetailsService usuarioDetailsService;
    private final com.tecnicsecure.config.TecniSecureProperties propiedades;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          UsuarioDetailsService usuarioDetailsService,
                          com.tecnicsecure.config.TecniSecureProperties propiedades) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.usuarioDetailsService = usuarioDetailsService;
        this.propiedades = propiedades;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF no aplica: la API es stateless y usa JWT en cabecera.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Permite /h2-console y Swagger (frames propios)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Frontend estatico y recursos
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/img/**",
                                 "/video/**", "/audio/**", "/uploads/**", "/pages/**",
                                 "/panel/**", "/assets/**", "/favicon.ico").permitAll()
                // Documentacion
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                 "/h2-console/**", "/ws/**", "/actuator/health", "/actuator/health/**").permitAll()
                // Autenticacion publica
                .requestMatchers("/api/auth/login", "/api/auth/registro",
                                 "/api/auth/recuperar", "/api/auth/restablecer",
                                 "/api/auth/refresh", "/api/auth/captcha").permitAll()
                // Asistente virtual del sitio publico (el visitante no tiene cuenta:
                // usa un token aleatorio que solo conoce su navegador)
                .requestMatchers(HttpMethod.POST, "/api/asistente", "/api/asistente/mensajes").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/asistente/conversacion/**").permitAll()
                // Catalogo de solo lectura
                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**",
                                 "/api/marcas/**", "/api/servicios/**").permitAll()
                // Formularios publicos
                .requestMatchers(HttpMethod.POST, "/api/contactos", "/api/solicitudes").permitAll()
                // Operaciones de escritura del catalogo solo para administrador/empleado
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasAnyRole("ADMINISTRADOR", "EMPLEADO")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasAnyRole("ADMINISTRADOR", "EMPLEADO")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/categorias/**", "/api/marcas/**", "/api/servicios/**")
                    .hasAnyRole("ADMINISTRADOR", "EMPLEADO")
                // Cualquier otra ruta requiere autenticacion
                .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        escribirError(response, 401, "No autenticado o token invalido/expirado"))
                .accessDeniedHandler((request, response, deniedException) ->
                        escribirError(response, 403, "No tiene permisos para esta accion")));

        return http.build();
    }

    private void escribirError(jakarta.servlet.http.HttpServletResponse response,
                               int status, String mensaje) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":" + status + ",\"error\":\"" + (status == 401 ? "Unauthorized" : "Forbidden")
                        + "\",\"mensaje\":\"" + mensaje + "\"}");
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt es compatible con los hashes generados por PHP (password_hash).
     * Los hashes antiguos con prefijo $2y$ se aceptan gracias a la
     * implementacion de Spring Security, que reconoce la variante.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

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
