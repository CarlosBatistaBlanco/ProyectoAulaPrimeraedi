/*
 * Este archivo se ejecuta en silencio cada vez que se enciende el sistema para revisar
 * los permisos de los empleados ya registrados. Garantiza que ningún trabajador pierda
 * el acceso a sus secciones personales básicas (Mi Perfil y Direcciones) en su menú lateral,
 * sin quitar los permisos que el administrador haya decidido restringir.
 */
package com.tecnicsecure.config;

import com.tecnicsecure.entity.EmpleadoPerfil;
import com.tecnicsecure.repository.EmpleadoPerfilRepository;
import com.tecnicsecure.service.impl.EmpleadoGestionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class ActualizacionModulosEmpleado {

    private static final Logger log = LoggerFactory.getLogger(ActualizacionModulosEmpleado.class);

    // Secciones personales que todo empleado debe poder ver siempre en su panel
    private static final List<String> SIEMPRE_ACCESIBLES = List.of("perfil", "direccion");

    // Programa esta revisión automática para que corra apenas termine de encender el sistema
    @Bean
    public ApplicationRunner actualizarModulosEmpleados(EmpleadoPerfilRepository repositorio) {
        return new ApplicationRunner() {
            @Override
            @Transactional
            public void run(ApplicationArguments args) {
                // Intenta completar las secciones faltantes sin detener el arranque si ocurre algún problema
                try {
                    completar(repositorio);
                } catch (Exception ex) {
                    log.warn("No se pudieron actualizar los modulos de empleado: {}", ex.getMessage());
                }
            }
        };
    }

    // Recorre cada ficha de empleado guardada y agrega las secciones personales que le falten
    private void completar(EmpleadoPerfilRepository repositorio) {
        List<String> disponibles = EmpleadoGestionServiceImpl.MODULOS_VALIDOS;
        int actualizados = 0;

        for (EmpleadoPerfil perfil : repositorio.findAll()) {
            // Separa el texto de secciones permitidas por comas para revisarlas una por una
            String guardados = perfil.getModulosPermitidos() == null ? "" : perfil.getModulosPermitidos();
            List<String> actuales = Arrays.stream(guardados.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            // Solo agrega las secciones personales obligatorias; los demás permisos quedan a criterio del administrador
            List<String> faltantes = SIEMPRE_ACCESIBLES.stream()
                    .filter(m -> disponible(disponibles, m) && !actuales.contains(m))
                    .toList();

            // Si el empleado ya tiene sus secciones personales activas, continúa con el siguiente
            if (faltantes.isEmpty()) {
                continue;
            }

            // Suma las secciones que faltaban a las que ya tenía y guarda el cambio
            StringBuilder nuevo = new StringBuilder(String.join(",", actuales));
            for (String m : faltantes) {
                if (nuevo.length() > 0) nuevo.append(',');
                nuevo.append(m);
            }
            perfil.setModulosPermitidos(nuevo.toString());
            perfil.setActualizadoEn(LocalDateTime.now());
            repositorio.save(perfil);
            actualizados++;
        }

        // Deja un aviso en el registro del sistema cuando se corrige al menos un empleado
        if (actualizados > 0) {
            log.info("Perfiles de empleado actualizados con los modulos nuevos: {}", actualizados);
        }
    }

    // Comprueba que el nombre de la sección exista en la lista oficial del panel
    private boolean disponible(List<String> disponibles, String modulo) {
        return disponibles.contains(modulo);
    }
}
