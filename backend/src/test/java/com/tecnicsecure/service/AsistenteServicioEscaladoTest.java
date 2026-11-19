package com.tecnicsecure.service;

import com.tecnicsecure.dto.soporte.AsistenteRequest;
import com.tecnicsecure.dto.soporte.AsistenteResponse;
import com.tecnicsecure.repository.SolicitudServicioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Antes toda solicitud escalada desde el chat quedaba como "Instalacion de
 * Camaras" porque el codigo tomaba siempre el primer servicio del catalogo.
 * Ahora el servicio se deduce del mensaje del visitante.
 */
@SpringBootTest
@ActiveProfiles("test")
class AsistenteServicioEscaladoTest {

    @Autowired private AsistentePublicoService asistente;
    @Autowired private SolicitudServicioRepository solicitudRepository;

    @AfterEach
    void limpiar() {
        solicitudRepository.findAll().stream()
                .filter(s -> "visitante@tecnicsecure.local".equalsIgnoreCase(s.getCorreo()))
                .forEach(solicitudRepository::delete);
    }

    private AsistenteResponse escalar(String mensaje) {
        return asistente.conversar(new AsistenteRequest(
                mensaje, List.of(), "Visitante Prueba", "visitante@tecnicsecure.local", "3001234567", null));
    }

    private String servicioDe(Integer idSolicitud) {
        return solicitudRepository.findById(idSolicitud).orElseThrow().getServicio().getTitulo();
    }

    @Test
    @DisplayName("Una consulta de alarma no se marca como instalacion de camaras")
    void alarmaSeClasificaComoAlarma() {
        AsistenteResponse r = escalar("Quiero hablar con un asesor porque mi alarma no suena");
        assertThat(r.escalado()).isTrue();
        assertThat(r.idSolicitud()).isNotNull();
        assertThat(servicioDe(r.idSolicitud())).contains("Alarma");
    }

    @Test
    @DisplayName("Una consulta de monitoreo se clasifica como monitoreo")
    void monitoreoSeClasificaBien() {
        AsistenteResponse r = escalar("Necesito asesor sobre el monitoreo 24/7 de mi local");
        assertThat(servicioDe(r.idSolicitud())).contains("Monitoreo");
    }

    @Test
    @DisplayName("Una consulta de control de acceso se clasifica como control de acceso")
    void controlAccesoSeClasificaBien() {
        AsistenteResponse r = escalar("Quiero asesor sobre control de acceso con tarjetas RFID");
        assertThat(servicioDe(r.idSolicitud())).contains("Control de Acceso");
    }

    @Test
    @DisplayName("Una consulta de mantenimiento preventivo se clasifica bien")
    void mantenimientoSeClasificaBien() {
        AsistenteResponse r = escalar("Necesito asesor para mantenimiento preventivo de equipos");
        assertThat(servicioDe(r.idSolicitud())).contains("Mantenimiento");
    }

    @Test
    @DisplayName("Una pregunta generica cae en asesoria y diseno")
    void genericoCaeEnAsesoria() {
        AsistenteResponse r = escalar("Quiero hablar con un asesor humano");
        assertThat(servicioDe(r.idSolicitud())).contains("Asesor");
    }
}
