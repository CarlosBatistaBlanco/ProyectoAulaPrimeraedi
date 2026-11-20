package com.tecnicsecure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Verificacion del login sin estado (la API usa JWT y no guarda sesion HTTP,
 * asi que no se puede confiar en la sesion del servlet).
 *
 * El servidor envia la pregunta junto con una ficha firmada:
 *
 *   ficha = "<respuesta>.<vencimiento>.<firma>"
 *
 * La firma es un HMAC-SHA256 con una clave secreta del servidor, de modo que el
 * navegador no puede inventarse la respuesta. Al iniciar sesion el cliente
 * devuelve la ficha y lo que escribio, y aqui se comparan.
 *
 * Se entrega la ficha en Base64 para que el usuario no vea el resultado en el
 * codigo fuente de la pagina.
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private static final String ALGORITMO = "HmacSHA256";
    /** La verificacion vale 10 minutos: tiempo de sobra para escribir la clave. */
    private static final long VIGENCIA_SEGUNDOS = 600;

    private final byte[] secreto;
    private final SecureRandom azar = new SecureRandom();

    public CaptchaService(@Value("${tecnisecure.captcha.secreto:}") String secretoConfigurado) {
        String base = secretoConfigurado == null ? "" : secretoConfigurado.trim();
        if (base.isEmpty()) {
            // Sin clave configurada se genera una aleatoria por arranque: las
            // fichas viejas dejan de valer al reiniciar, que es lo esperado.
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            this.secreto = aleatorio;
            log.debug("Captcha sin secreto configurado: se usa una clave aleatoria temporal");
        } else {
            this.secreto = base.getBytes(StandardCharsets.UTF_8);
        }
    }

    /** Pregunta y ficha que se envian al navegador. */
    public record Desafio(String pregunta, String ficha) {
    }

    public Desafio generar() {
        int a = 2 + azar.nextInt(8);
        int b = 2 + azar.nextInt(8);
        int resultado = a + b;
        long vence = Instant.now().getEpochSecond() + VIGENCIA_SEGUNDOS;

        String contenido = resultado + "." + vence;
        String ficha = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((contenido + "." + firmar(contenido)).getBytes(StandardCharsets.UTF_8));

        return new Desafio("¿Cuánto es " + a + " + " + b + "?", ficha);
    }

    /**
     * Comprueba la respuesta del usuario contra la ficha entregada.
     * Devuelve true solo si la firma es valida, no ha vencido y el numero coincide.
     */
    public boolean validar(String ficha, String respuesta) {
        if (ficha == null || ficha.isBlank() || respuesta == null || respuesta.isBlank()) {
            return false;
        }
        try {
            String decodificada = new String(
                    Base64.getUrlDecoder().decode(ficha.trim()), StandardCharsets.UTF_8);
            String[] partes = decodificada.split("\\.");
            if (partes.length != 3) {
                return false;
            }

            String resultado = partes[0];
            long vence = Long.parseLong(partes[1]);
            String firmaRecibida = partes[2];
            String firmaEsperada = firmar(resultado + "." + vence);

            if (!MessageDigest.isEqual(firmaRecibida.getBytes(StandardCharsets.UTF_8),
                    firmaEsperada.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            if (Instant.now().getEpochSecond() > vence) {
                return false;
            }

            String limpia = respuesta.trim().replaceAll("\\s+", "");
            return limpia.equals(resultado);
        } catch (IllegalArgumentException ex) {
            // Base64 corrupto o vencimiento no numerico: ficha invalida.
            return false;
        }
    }

    private String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto, ALGORITMO));
            byte[] firma = mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(firma);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el captcha", ex);
        }
    }
}
