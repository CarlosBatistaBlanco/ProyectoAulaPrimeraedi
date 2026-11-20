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

/**
 * Al arrancar, revisa los perfiles de empleado ya guardados y les agrega los
 * modulos nuevos del sistema que aun no tuvieran (por ejemplo "direccion" o
 * "facturacion").
 *
 * Sin esto, un empleado creado antes de que existiera un modulo nunca lo ve en
 * su menu, porque su lista de permisos quedo guardada en la base de datos con
 * los modulos que existian en ese momento.
 *
 * Solo AGREGA modulos: nunca quita los que el administrador haya restringido
 * a proposito. Los que el admin desmarque se respetan en la siguiente edicion.
 */
@Configuration
public class ActualizacionModulosEmpleado {

    private static final Logger log = LoggerFactory.getLogger(ActualizacionModulosEmpleado.class);

    /** Modulos que todo empleado debe ver siempre (datos personales). */
    private static final List<String> SIEMPRE_ACCESIBLES = List.of("perfil", "direccion");

    @Bean
    public ApplicationRunner actualizarModulosEmpleados(EmpleadoPerfilRepository repositorio) {
        return new ApplicationRunner() {
            @Override
            @Transactional
            public void run(ApplicationArguments args) {
                try {
                    completar(repositorio);
                } catch (Exception ex) {
                    log.warn("No se pudieron actualizar los modulos de empleado: {}", ex.getMessage());
                }
            }
        };
    }

    private void completar(EmpleadoPerfilRepository repositorio) {
        List<String> disponibles = EmpleadoGestionServiceImpl.MODULOS_VALIDOS;
        int actualizados = 0;

        for (EmpleadoPerfil perfil : repositorio.findAll()) {
            String guardados = perfil.getModulosPermitidos() == null ? "" : perfil.getModulosPermitidos();
            List<String> actuales = Arrays.stream(guardados.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            // Se agregan los modulos de datos personales si faltan; los modulos
            // funcionales nuevos quedan disponibles para que el admin los active,
            // excepto direccion/perfil que son siempre necesarios.
            List<String> faltantes = SIEMPRE_ACCESIBLES.stream()
                    .filter(m -> disponible(disponibles, m) && !actuales.contains(m))
                    .toList();

            if (faltantes.isEmpty()) {
                continue;
            }

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

        if (actualizados > 0) {
            log.info("Perfiles de empleado actualizados con los modulos nuevos: {}", actualizados);
        }
    }

    private boolean disponible(List<String> disponibles, String modulo) {
        return disponibles.contains(modulo);
    }
}
