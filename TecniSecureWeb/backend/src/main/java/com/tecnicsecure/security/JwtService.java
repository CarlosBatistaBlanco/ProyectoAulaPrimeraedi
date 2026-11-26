/*
 * Este archivo fabrica y revisa los pases digitales de sesión que recibe cada persona al
 * entrar a su cuenta. Cuando alguien navega por su panel o compra en la tienda, aquí se
 * comprueba que su pase tenga el sello secreto del sistema y que todavía no haya caducado.
 */
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

@Service
public class JwtService {

    private final SecretKey clave;
    private final long minutosExpiracion;
    private final long minutosExpiracionRefresh;

    // Toma la clave secreta y el tiempo de vigencia configurados para emitir pases de sesión
    public JwtService(@Value("${tecnisecure.jwt.secret}") String secreto,
                      @Value("${tecnisecure.jwt.expiration-minutes}") long minutosExpiracion,
                      @Value("${tecnisecure.jwt.refresh-expiration-minutes}") long minutosExpiracionRefresh) {
        this.clave = Keys.hmacShaKeyFor(resolverClave(secreto));
        this.minutosExpiracion = minutosExpiracion;
        this.minutosExpiracionRefresh = minutosExpiracionRefresh;
    }

    // Revisa que la clave secreta sea suficientemente larga (mínimo 32 caracteres) para sellar los pases con seguridad
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
            // Si no venía en formato codificado especial, continúa con la comprobación normal
        }
        throw new IllegalStateException(
                "JWT_SECRET debe tener al menos 32 caracteres para firma HS256");
    }

    // Fabrica el pase principal de sesión que el navegador presenta al abrir pantallas privadas
    public String generarToken(Usuario usuario) {
        return construir(usuario, Map.of("tipo", "access"), minutosExpiracion);
    }

    // Fabrica un pase de renovación más duradero para mantener la sesión abierta sin pedir la clave otra vez
    public String generarRefreshToken(Usuario usuario) {
        return construir(usuario, Map.of("tipo", "refresh"), minutosExpiracionRefresh);
    }

    // Arma el pase con correo, nombre, tipo de cuenta y fecha de vencimiento, sellándolo con la clave secreta
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

    // Lee el correo electrónico de la persona guardado dentro del pase
    public String extraerUsuario(String token) {
        return obtenerClaim(token, Claims::getSubject);
    }

    // Lee el tipo de cuenta (administrador, empleado o cliente) guardado dentro del pase
    public String extraerRol(String token) {
        return obtenerClaim(token, c -> c.get("rol", String.class));
    }

    // Verifica si el pase es de renovación y no un pase de uso cotidiano
    public boolean esRefreshToken(String token) {
        return "refresh".equals(obtenerClaim(token, c -> c.get("tipo", String.class)));
    }

    // Comprueba que el sello del pase sea legítimo y que todavía siga vigente
    public boolean esTokenValido(String token) {
        try {
            return !estaExpirado(token);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    // Revisa si ya pasó la hora límite de vigencia del pase
    private boolean estaExpirado(String token) {
        return obtenerClaim(token, Claims::getExpiration).before(new Date());
    }

    // Verifica la autenticidad del pase con la clave secreta y lee el dato específico solicitado
    public <T> T obtenerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        return resolver.apply(claims);
    }
}
