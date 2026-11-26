/*
 * Este archivo protege la pantalla de inicio de sesion contra intentos automaticos hechos
 * por programas maliciosos. Genera una suma matematica facil para humanos con un sello de
 * seguridad que dura 10 minutos, de modo que nadie pueda hacer trampa ni adivinar la respuesta.
 */
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

@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private static final String ALGORITMO = "HmacSHA256";
    // La pregunta dura 10 minutos: tiempo suficiente para que una persona escriba su correo y clave sin afan.
    private static final long VIGENCIA_SEGUNDOS = 600;

    private final byte[] secreto;
    private final SecureRandom azar = new SecureRandom();

    // Prepara la clave privada con la que la tienda sella cada pregunta de verificacion.
    public CaptchaService(@Value("${tecnisecure.captcha.secreto:}") String secretoConfigurado) {
        String base = secretoConfigurado == null ? "" : secretoConfigurado.trim();
        if (base.isEmpty()) {
            // Si no se configuro una clave fija, crea una clave temporal al azar cada vez que enciende el sistema.
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            this.secreto = aleatorio;
            log.debug("Captcha sin secreto configurado: se usa una clave aleatoria temporal");
        } else {
            this.secreto = base.getBytes(StandardCharsets.UTF_8);
        }
    }

    // Guarda juntos el texto de la pregunta y su ficha sellada para enviarlos a la pantalla de acceso.
    public record Desafio(String pregunta, String ficha) {
    }

    // Inventa una suma sencilla al azar y la entrega acompanada de una ficha sellada y oculta valida por 10 minutos.
    public Desafio generar() {
        int a = 2 + azar.nextInt(8);
        int b = 2 + azar.nextInt(8);
        int resultado = a + b;
        long vence = Instant.now().getEpochSecond() + VIGENCIA_SEGUNDOS;

        // Junta la respuesta y la hora de vencimiento, le agrega el sello de seguridad y lo oculta para que nadie lo lea en pantalla.
        String contenido = resultado + "." + vence;
        String ficha = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((contenido + "." + firmar(contenido)).getBytes(StandardCharsets.UTF_8));

        return new Desafio("¿Cuánto es " + a + " + " + b + "?", ficha);
    }

    // Comprueba que la ficha no haya sido alterada, que siga dentro de los 10 minutos y que el numero escrito sea el correcto.
    public boolean validar(String ficha, String respuesta) {
        if (ficha == null || ficha.isBlank() || respuesta == null || respuesta.isBlank()) {
            return false;
        }
        try {
            // Destapa el contenido de la ficha para separar la respuesta esperada, la hora limite y el sello.
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

            // Revisa que el sello coincida exactamente para confirmar que nadie modifico la ficha.
            if (!MessageDigest.isEqual(firmaRecibida.getBytes(StandardCharsets.UTF_8),
                    firmaEsperada.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            // Si ya pasaron mas de 10 minutos desde que se creo la pregunta, la rechaza por vencida.
            if (Instant.now().getEpochSecond() > vence) {
                return false;
            }

            // Compara el numero que escribio la persona con el resultado real de la suma.
            String limpia = respuesta.trim().replaceAll("\\s+", "");
            return limpia.equals(resultado);
        } catch (IllegalArgumentException ex) {
            // Si la ficha llega danada o incompleta, se considera invalida.
            return false;
        }
    }

    // Crea un sello unico con la clave privada de la tienda para garantizar que la ficha sea autentica.
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
