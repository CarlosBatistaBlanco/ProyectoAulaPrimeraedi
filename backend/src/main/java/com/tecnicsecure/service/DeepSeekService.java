package com.tecnicsecure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API de DeepSeek (compatible con OpenAI).
 *
 * Si no hay clave configurada (DEEPSEEK_API_KEY) o la API falla, devuelve null
 * para que el asistente use sus respuestas locales: el chat NUNCA se queda
 * sin contestar.
 */
@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    @Value("${tecnisecure.ia.habilitado:true}")
    private boolean habilitado;

    @Value("${tecnisecure.ia.api-key:}")
    private String apiKey;

    @Value("${tecnisecure.ia.url:https://api.deepseek.com/chat/completions}")
    private String url;

    @Value("${tecnisecure.ia.modelo:deepseek-chat}")
    private String modelo;

    @Value("${tecnisecure.ia.timeout-segundos:25}")
    private int timeoutSegundos;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public boolean estaDisponible() {
        return habilitado && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Envia la conversacion y devuelve la respuesta del modelo.
     * @return texto de la respuesta, o null si no se pudo obtener.
     */
    public String responder(String instrucciones, List<Map<String, String>> mensajes) {
        if (!estaDisponible()) {
            return null;
        }
        try {
            StringBuilder cuerpo = new StringBuilder();
            cuerpo.append("{\"model\":\"").append(escape(modelo)).append("\",")
                    .append("\"stream\":false,\"max_tokens\":600,\"temperature\":0.4,")
                    .append("\"messages\":[");
            cuerpo.append("{\"role\":\"system\",\"content\":\"").append(escape(instrucciones)).append("\"}");
            for (Map<String, String> m : mensajes) {
                String rol = "assistant".equals(m.get("rol")) ? "assistant" : "user";
                cuerpo.append(",{\"role\":\"").append(rol).append("\",\"content\":\"")
                        .append(escape(m.get("texto"))).append("\"}");
            }
            cuerpo.append("]}");

            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSegundos))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (respuesta.statusCode() != 200) {
                log.warn("DeepSeek respondio {}: {}", respuesta.statusCode(),
                        recortar(respuesta.body(), 200));
                return null;
            }
            return extraerContenido(respuesta.body());
        } catch (Exception ex) {
            log.warn("No se pudo consultar DeepSeek: {}", ex.getMessage());
            return null;
        }
    }

    /** Lee "choices[0].message.content" del JSON de respuesta. */
    private String extraerContenido(String json) {
        int iChoices = json.indexOf("\"choices\"");
        if (iChoices < 0) return null;
        int iContent = json.indexOf("\"content\"", iChoices);
        if (iContent < 0) return null;
        int iComillas = json.indexOf('"', json.indexOf(':', iContent) + 1);
        if (iComillas < 0) return null;

        StringBuilder texto = new StringBuilder();
        for (int i = iComillas + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char sig = json.charAt(++i);
                switch (sig) {
                    case 'n' -> texto.append('\n');
                    case 't' -> texto.append('\t');
                    case '"' -> texto.append('"');
                    case '\\' -> texto.append('\\');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            texto.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                    }
                    default -> texto.append(sig);
                }
            } else if (c == '"') {
                break;
            } else {
                texto.append(c);
            }
        }
        String limpio = texto.toString().trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String escape(String texto) {
        if (texto == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private String recortar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
