package com.tecnicsecure.security;

import com.tecnicsecure.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Generacion y validacion de tokens JWT (access + refresh).
 * Se usa el mismo usuario propio como "subject" (correo) y el rol como claim.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final long minutosExpiracion;
    private final long minutosExpiracionRefresh;

    public JwtService(@Value("${tecnisecure.jwt.secret}") String secreto,
                      @Value("${tecnisecure.jwt.expiration-minutes}") long minutosExpiracion,
                      @Value("${tecnisecure.jwt.refresh-expiration-minutes}") long minutosExpiracionRefresh) {
        this.clave = Keys.hmacShaKeyFor(resolverClave(secreto));
        this.minutosExpiracion = minutosExpiracion;
        this.minutosExpiracionRefresh = minutosExpiracionRefresh;
    }

    private byte[] resolverClave(String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }
        try {
            byte[] decodificados = Decoders.BASE64.decode(secreto);
            if (decodificados.length >= 32) {
                return decodificados;
            }
        } catch (RuntimeException ignored) {
            // el secreto tampoco es base64: se usa el texto tal cual
        }
        throw new IllegalStateException(
                "JWT_SECRET debe tener al menos 32 caracteres para firma HS256");
    }

    public String generarToken(Usuario usuario) {
        return construir(usuario, Map.of("tipo", "access"), minutosExpiracion);
    }

    public String generarRefreshToken(Usuario usuario) {
        return construir(usuario, Map.of("tipo", "refresh"), minutosExpiracionRefresh);
    }

    private String construir(Usuario usuario, Map<String, Object> claimsExtra, long minutos) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .claims(claimsExtra)
                .subject(usuario.getCorreo())
                .claim("id", usuario.getIdUsuario())
                .claim("nombre", usuario.getNombre())
                .claim("rol", usuario.getNombreRol())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public String extraerUsuario(String token) {
        return obtenerClaim(token, Claims::getSubject);
    }

    public String extraerRol(String token) {
        return obtenerClaim(token, c -> c.get("rol", String.class));
    }

    public boolean esRefreshToken(String token) {
        return "refresh".equals(obtenerClaim(token, c -> c.get("tipo", String.class)));
    }

    public boolean esTokenValido(String token) {
        try {
            return !estaExpirado(token);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean estaExpirado(String token) {
        return obtenerClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T obtenerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        return resolver.apply(claims);
    }
}
