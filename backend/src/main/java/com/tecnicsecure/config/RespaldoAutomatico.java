package com.tecnicsecure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Respaldo automatico de la base de datos H2.
 *
 * Cada cierto tiempo (por defecto 10 minutos) copia el archivo de la base a
 * /app/respaldos, que esta montado en backend/data del proyecto. Asi, si el
 * disco del contenedor falla, siempre hay una copia reciente a mano.
 *
 * Se activa en el perfil "docker"; se puede desactivar con RESPALDO_ENABLED=false.
 */
@Configuration
@EnableScheduling
public class RespaldoAutomatico {

    private static final Logger log = LoggerFactory.getLogger(RespaldoAutomatico.class);
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Value("${tecnisecure.respaldo.habilitado:true}")
    private boolean habilitado;

    @Value("${tecnisecure.respaldo.origen:/app/data/tecnicsecure.mv.db}")
    private String rutaOrigen;

    @Value("${tecnisecure.respaldo.destino:/app/respaldos}")
    private String carpetaDestino;

    @Value("${tecnisecure.respaldo.copias:6}")
    private int copiasAConservar;

    @Scheduled(initialDelayString = "${tecnisecure.respaldo.initial-delay-ms:60000}",
               fixedDelayString = "${tecnisecure.respaldo.intervalo-ms:600000}")
    public void copiarBase() {
        if (!habilitado) {
            return;
        }
        Path origen = Path.of(rutaOrigen);
        if (!Files.exists(origen)) {
            return;
        }
        try {
            Path destinoDir = Path.of(carpetaDestino);
            Files.createDirectories(destinoDir);

            String nombre = "tecnicsecure-" + LocalDateTime.now().format(SELLO) + ".mv.db";
            Files.copy(origen, destinoDir.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);

            // Mantener solo las ultimas copias
            try (var lista = Files.list(destinoDir)) {
                var copias = lista
                        .filter(p -> p.getFileName().toString().startsWith("tecnicsecure-"))
                        .filter(p -> p.getFileName().toString().endsWith(".mv.db"))
                        .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                        .toList();
                for (int i = copiasAConservar; i < copias.size(); i++) {
                    Files.deleteIfExists(copias.get(i));
                }
            }
            log.info("Respaldo de la base creado: {}", nombre);
        } catch (IOException ex) {
            log.warn("No se pudo crear el respaldo automatico: {}", ex.getMessage());
        }
    }
}
