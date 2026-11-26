/*
 * Este archivo llena la tienda con su catálogo inicial apenas el sistema termina de encender.
 * Carga automáticamente las categorías, marcas, productos, servicios y tipos de usuario base
 * para que la página principal y el catálogo aparezcan con información lista desde el primer uso,
 * cuidando que las tildes y la letra ñ se guarden correctamente en cualquier computador.
 */
package com.tecnicsecure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

@Configuration
public class CargaDatosIniciales {

    private static final Logger log = LoggerFactory.getLogger(CargaDatosIniciales.class);
    private static final String RUTA_SCRIPT = "data.sql";
    private static final String SEPARADOR = "^^^";

    private final DataSource dataSource;

    // Guarda el acceso a la base de datos para poder insertar los registros iniciales de la tienda
    public CargaDatosIniciales(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Corre en automático cuando el sistema ya terminó de encender y aplica la carga base del catálogo
    @EventListener(ApplicationReadyEvent.class)
    public void cargarDatos() {
        // Primero lee el archivo con la lista inicial de categorías, productos y servicios
        String script;
        try {
            script = leerScript();
        } catch (Exception ex) {
            log.warn("No se pudo leer {}: {}", RUTA_SCRIPT, ex.getMessage());
            return;
        }

        // Abre la base de datos y aplica cada bloque de carga separado por la marca especial
        int ejecutadas = 0;
        try (Connection conexion = dataSource.getConnection()) {
            for (String sentencia : script.split(java.util.regex.Pattern.quote(SEPARADOR))) {
                String sql = limpiar(sentencia);
                if (sql.isEmpty()) {
                    continue;
                }
                // Aplica cada bloque por separado para que un dato repetido o con fallo no detenga los demás
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

    // Lee el archivo de datos iniciales respetando acentos, tildes y la letra ñ
    private String leerScript() throws Exception {
        var recurso = new org.springframework.core.io.ClassPathResource(RUTA_SCRIPT);
        String contenido;
        try (var entrada = recurso.getInputStream()) {
            contenido = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
        return contenido;
    }

    // Quita las líneas de notas y marcas invisibles del texto antes de guardarlo en la base de datos
    private String limpiar(String sentencia) {
        String texto = sentencia;
        // Quita la marca invisible que algunos editores de texto agregan al comienzo de un archivo
        if (texto.startsWith("\uFEFF")) {
            texto = texto.substring(1);
        }
        // Recorre el texto línea por línea y omite las notas aclaratorias que empiezan por "--"
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
