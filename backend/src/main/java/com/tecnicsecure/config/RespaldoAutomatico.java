package com.tecnicsecure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Respaldo automatico en caliente y consistente de la base de datos H2.
 *
 * Usa el comando nativo SQL "BACKUP TO '...'" de H2 en lugar de copiar el archivo
 * .mv.db abierto con Files.copy(), garantizando copias atomicas sin riesgo de
 * corrupcion si hay escrituras concurrentes.
 */
@Configuration
@EnableScheduling
public class RespaldoAutomatico {

    private static final Logger log = LoggerFactory.getLogger(RespaldoAutomatico.class);
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;

    @Value("${tecnisecure.respaldo.habilitado:true}")
    private boolean habilitado;

    @Value("${tecnisecure.respaldo.origen:/app/data/tecnicsecure.mv.db}")
    private String rutaOrigen;

    @Value("${tecnisecure.respaldo.destino:/app/respaldos}")
    private String carpetaDestino;

    @Value("${tecnisecure.respaldo.copias:6}")
    private int copiasAConservar;

    public RespaldoAutomatico(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            if (url == null || !url.toLowerCase().startsWith("jdbc:h2:")) {
                return;
            }

            Path destinoDir = Path.of(carpetaDestino).toAbsolutePath().normalize();
            Files.createDirectories(destinoDir);

            String nombre = "tecnicsecure-" + LocalDateTime.now().format(SELLO) + ".zip";
            Path archivoDestino = destinoDir.resolve(nombre);
            String rutaSql = archivoDestino.toString().replace("\\", "/").replace("'", "''");

            try (Statement st = conn.createStatement()) {
                st.execute("BACKUP TO '" + rutaSql + "'");
            }

            // Mantener solo las ultimas copias consistentes
            try (var lista = Files.list(destinoDir)) {
                var copias = lista
                        .filter(p -> p.getFileName().toString().startsWith("tecnicsecure-"))
                        .filter(p -> p.getFileName().toString().endsWith(".zip"))
                        .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                        .toList();
                for (int i = copiasAConservar; i < copias.size(); i++) {
                    Files.deleteIfExists(copias.get(i));
                }
            }
            log.info("Respaldo consistente de H2 creado: {}", nombre);
        } catch (Exception ex) {
            log.warn("No se pudo crear el respaldo automatico: {}", ex.getMessage());
        }
    }
}
