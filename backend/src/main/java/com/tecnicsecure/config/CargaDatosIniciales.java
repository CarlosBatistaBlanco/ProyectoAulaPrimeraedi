package com.tecnicsecure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * Carga los datos iniciales (categorías, productos, servicios, marcas y roles)
 * DESPUÉS de que Hibernate haya creado las tablas.
 *
 * Se hace así (y no con spring.sql.init) porque ese mecanismo ejecuta el
 * script ANTES de crear el esquema.
 *
 * IMPORTANTE sobre acentos: el script se lee siempre como UTF-8 y las
 * sentencias se envían por JDBC (que trabaja con Unicode nativo), por lo
 * que la ñ y las tildes se guardan correctamente sin importar la
 * configuración regional del equipo.
 */
@Configuration
public class CargaDatosIniciales {

    private static final Logger log = LoggerFactory.getLogger(CargaDatosIniciales.class);
    private static final String RUTA_SCRIPT = "data.sql";
    private static final String SEPARADOR = "^^^";

    private final DataSource dataSource;

    public CargaDatosIniciales(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cargarDatos() {
        String script;
        try {
            script = leerScript();
        } catch (Exception ex) {
            log.warn("No se pudo leer {}: {}", RUTA_SCRIPT, ex.getMessage());
            return;
        }

        int ejecutadas = 0;
        try (Connection conexion = dataSource.getConnection()) {
            for (String sentencia : script.split(java.util.regex.Pattern.quote(SEPARADOR))) {
                String sql = limpiar(sentencia);
                if (sql.isEmpty()) {
                    continue;
                }
                try (var st = conexion.createStatement()) {
                    st.execute(sql);
                    ejecutadas++;
                } catch (Exception ex) {
                    log.warn("No se pudo ejecutar una sentencia de datos iniciales: {}", ex.getMessage());
                }
            }
            log.info("Datos iniciales aplicados: {} sentencias", ejecutadas);
        } catch (Exception ex) {
            log.warn("No se pudieron cargar los datos iniciales: {}", ex.getMessage());
        }
    }

    private String leerScript() throws Exception {
        var recurso = new org.springframework.core.io.ClassPathResource(RUTA_SCRIPT);
        String contenido;
        try (var entrada = recurso.getInputStream()) {
            contenido = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        return contenido;
    }

    /** Quita comentarios de línea y el BOM inicial de cada sentencia. */
    private String limpiar(String sentencia) {
        String texto = sentencia;
        if (texto.startsWith("\uFEFF")) {
            texto = texto.substring(1);
        }
        StringBuilder limpio = new StringBuilder();
        for (String linea : texto.split("\r?\n")) {
            String recortada = linea.trim();
            if (recortada.startsWith("--")) {
                continue;
            }
            limpio.append(linea).append('\n');
        }
        return limpio.toString().trim();
    }
}
