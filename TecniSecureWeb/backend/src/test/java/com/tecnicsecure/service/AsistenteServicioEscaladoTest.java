/*
 * Este archivo contiene pruebas automáticas para el asistente virtual del sitio web
 * cuando transfiere una consulta a un asesor humano. Comprueba que la solicitud creada
 * quede clasificada en el servicio correcto según lo que escribió el visitante.
 */
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

@SpringBootTest
@ActiveProfiles("test")
class AsistenteServicioEscaladoTest {

    @Autowired private AsistentePublicoService asistente;
    @Autowired private SolicitudServicioRepository solicitudRepository;

    // Limpia las solicitudes de prueba al terminar cada verificación para no dejar registros sobrantes
    @AfterEach
    void limpiar() {
        solicitudRepository.findAll().stream()
                .filter(s -> "visitante@tecnicsecure.local".equalsIgnoreCase(s.getCorreo()))
                .forEach(solicitudRepository::delete);
    }

    // Simula a un visitante escribiendo en el asistente para pedir ayuda de un asesor humano
    private AsistenteResponse escalar(String mensaje) {
        return asistente.conversar(new AsistenteRequest(
                mensaje, List.of(), "Visitante Prueba", "visitante@tecnicsecure.local", "3001234567", null));
    }

    // Consulta qué servicio técnico quedó asignado en la solicitud recién creada
    private String servicioDe(Integer idSolicitud) {
        return solicitudRepository.findById(idSolicitud).orElseThrow().getServicio().getTitulo();
    }

    // Comprueba que una consulta sobre alarmas se clasifique en el servicio de alarmas y no como cámaras
    @Test
    @DisplayName("Una consulta de alarma no se marca como instalacion de camaras")
    void alarmaSeClasificaComoAlarma() {
        AsistenteResponse r = escalar("Quiero hablar con un asesor porque mi alarma no suena");
        assertThat(r.escalado()).isTrue();
        assertThat(r.idSolicitud()).isNotNull();
        assertThat(servicioDe(r.idSolicitud())).contains("Alarma");
    }

    // Comprueba que una consulta sobre vigilancia o monitoreo 24/7 quede clasificada en el servicio de monitoreo
    @Test
    @DisplayName("Una consulta de monitoreo se clasifica como monitoreo")
    void monitoreoSeClasificaBien() {
        AsistenteResponse r = escalar("Necesito asesor sobre el monitoreo 24/7 de mi local");
        assertThat(servicioDe(r.idSolicitud())).contains("Monitoreo");
    }

    // Comprueba que una pregunta sobre tarjetas o control de entradas se asigne a control de acceso
    @Test
    @DisplayName("Una consulta de control de acceso se clasifica como control de acceso")
    void controlAccesoSeClasificaBien() {
        AsistenteResponse r = escalar("Quiero asesor sobre control de acceso con tarjetas RFID");
        assertThat(servicioDe(r.idSolicitud())).contains("Control de Acceso");
    }

    // Comprueba que un pedido de mantenimiento preventivo quede registrado bajo el servicio de mantenimiento
    @Test
    @DisplayName("Una consulta de mantenimiento preventivo se clasifica bien")
    void mantenimientoSeClasificaBien() {
        AsistenteResponse r = escalar("Necesito asesor para mantenimiento preventivo de equipos");
        assertThat(servicioDe(r.idSolicitud())).contains("Mantenimiento");
    }

    // Comprueba que una solicitud general sin tema específico se clasifique como asesoría
    @Test
    @DisplayName("Una pregunta generica cae en asesoria y diseno")
    void genericoCaeEnAsesoria() {
        AsistenteResponse r = escalar("Quiero hablar con un asesor humano");
        assertThat(servicioDe(r.idSolicitud())).contains("Asesor");
    }
}
