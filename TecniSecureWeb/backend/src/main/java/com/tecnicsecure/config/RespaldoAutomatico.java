/*
 * Este archivo crea copias de seguridad automáticas de toda la información de la tienda
 * en segundo plano mientras los clientes siguen comprando normalmente. Cada cierto tiempo
 * guarda un paquete comprimido con fecha y hora y elimina los más viejos para no llenar el disco.
 */
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

@Configuration
@EnableScheduling
public class RespaldoAutomatico {

    private static final Logger log = LoggerFactory.getLogger(RespaldoAutomatico.class);
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DataSource dataSource;

    // Permite encender o apagar el guardado automático de copias de seguridad
    @Value("${tecnisecure.respaldo.habilitado:true}")
    private boolean habilitado;

    // Ubicación del archivo principal donde vive toda la información de la tienda
    @Value("${tecnisecure.respaldo.origen:/app/data/tecnicsecure.mv.db}")
    private String rutaOrigen;

    // Carpeta donde se van guardando los archivos comprimidos de respaldo
    @Value("${tecnisecure.respaldo.destino:/app/respaldos}")
    private String carpetaDestino;

    // Número máximo de copias recientes que se mantienen antes de borrar las más antiguas
    @Value("${tecnisecure.respaldo.copias:6}")
    private int copiasAConservar;

    // Recibe el acceso a la base de datos para poder sacar copias limpias sin cerrar la tienda
    public RespaldoAutomatico(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Reloj automático que dispara una copia de seguridad cada 10 minutos por defecto
    @Scheduled(initialDelayString = "${tecnisecure.respaldo.initial-delay-ms:60000}",
               fixedDelayString = "${tecnisecure.respaldo.intervalo-ms:600000}")
    public void copiarBase() {
        // Si los respaldos están apagados o aún no existe el archivo de datos, no hace nada
        if (!habilitado) {
            return;
        }
        Path origen = Path.of(rutaOrigen);
        if (!Files.exists(origen)) {
            return;
        }
        try (Connection conn = dataSource.getConnection()) {
            // Comprueba que la tienda esté usando la base de datos interna en archivo antes de copiarla
            String url = conn.getMetaData().getURL();
            if (url == null || !url.toLowerCase().startsWith("jdbc:h2:")) {
                return;
            }

            // Crea la carpeta de respaldos si falta y arma el nombre del archivo con fecha y hora
            Path destinoDir = Path.of(carpetaDestino).toAbsolutePath().normalize();
            Files.createDirectories(destinoDir);

            String nombre = "tecnicsecure-" + LocalDateTime.now().format(SELLO) + ".zip";
            Path archivoDestino = destinoDir.resolve(nombre);
            String rutaSql = archivoDestino.toString().replace("\\", "/").replace("'", "''");

            // Genera una copia completa y segura en paquete comprimido sin interrumpir a los usuarios
            try (Statement st = conn.createStatement()) {
                st.execute("BACKUP TO '" + rutaSql + "'");
            }

            // Conserva únicamente las copias más recientes y elimina las sobrantes para ahorrar espacio
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
