package com.tecnicsecure.service;

import com.tecnicsecure.dto.soporte.MensajeVisitanteRequest;
import com.tecnicsecure.dto.soporte.MensajeVisitanteResponse;
import com.tecnicsecure.entity.ConversacionVisitante;
import com.tecnicsecure.repository.ConversacionVisitanteRepository;
import com.tecnicsecure.repository.MensajeVisitanteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flujo completo del chat publico: el visitante escribe sin cuenta, el equipo
 * lo ve en la bandeja y le responde, y el visitante recibe la respuesta en su
 * mismo hilo usando solo el token.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConversacionVisitanteTest {

    @Autowired private ConversacionVisitanteService servicio;
    @Autowired private ConversacionVisitanteRepository conversacionRepository;
    @Autowired private MensajeVisitanteRepository mensajeRepository;

    @Test
    @DisplayName("El visitante inicia un hilo y recibe un token")
    void visitanteIniciaHilo() {
        var respuesta = servicio.enviarVisitante(
                new MensajeVisitanteRequest("Hola, necesito una cotizacion", null, "Ana", "ana@gmail.com", "3001234567"));

        assertThat(respuesta.token()).isNotBlank().hasSizeGreaterThanOrEqualTo(32);
        assertThat(respuesta.token()).matches("[a-z0-9]+");
        assertThat(respuesta.nombre()).isEqualTo("Ana");

        List<MensajeVisitanteResponse> mensajes = servicio.mensajes(respuesta.token());
        assertThat(mensajes).hasSize(1);
        assertThat(mensajes.get(0).remitente()).isEqualTo("visitante");
        assertThat(mensajes.get(0).mensaje()).contains("cotizacion");
    }

    @Test
    @DisplayName("El hilo se reutiliza con el mismo token")
    void mismoTokenContinuaElHilo() {
        var primera = servicio.enviarVisitante(
                new MensajeVisitanteRequest("Primer mensaje", null, "Ana", "ana@gmail.com", null));
        var segunda = servicio.enviarVisitante(
                new MensajeVisitanteRequest("Segundo mensaje", primera.token(), null, null, null));

        assertThat(segunda.token()).isEqualTo(primera.token());
        assertThat(servicio.mensajes(primera.token())).hasSize(2);
    }

    @Test
    @DisplayName("El equipo ve al visitante en la bandeja y le responde")
    void equipoRespondeYVisitanteLoVe() {
        var hilo = servicio.enviarVisitante(
                new MensajeVisitanteRequest("Necesito hablar con una persona", null, "Luis", "luis@mail.com", null));

        List<Map<String, Object>> bandeja = servicio.bandeja();
        var fila = bandeja.stream()
                .filter(f -> String.valueOf(f.get("idCliente")).equals("cv-" + hilo.idConversacion()))
                .findFirst().orElseThrow();

        assertThat(fila.get("origen")).isEqualTo("visitante");
        assertThat(fila.get("nombre")).isEqualTo("Luis");
        assertThat(((Number) fila.get("sinLeer")).intValue()).isEqualTo(1);

        servicio.enviarAsesor(hilo.token(), "Claro, te ayudo enseguida.", 1);

        List<MensajeVisitanteResponse> mensajes = servicio.mensajes(hilo.token());
        assertThat(mensajes).hasSize(2);
        assertThat(mensajes.get(1).remitente()).isEqualTo("asesor");
        assertThat(mensajes.get(1).mensaje()).contains("te ayudo");
        assertThat(mensajes.get(1).idUsuarioEnvia()).isEqualTo(1);

        // Al leer el visitante, el contador del equipo vuelve a cero.
        ConversacionVisitante actualizada = conversacionRepository
                .findByToken(hilo.token()).orElseThrow();
        assertThat(actualizada.getSinLeer()).isZero();
    }

    @Test
    @DisplayName("Un token desconocido no devuelve conversaciones")
    void tokenInvalidoNoExiste() {
        assertThat(servicio.obtener("token-que-no-existe")).isNull();
    }

    @Test
    @DisplayName("Los datos de contacto se actualizan sin duplicar el hilo")
    void actualizaContacto() {
        var primera = servicio.enviarVisitante(
                new MensajeVisitanteRequest("Hola", null, "Ana", null, null));
        servicio.enviarVisitante(
                new MensajeVisitanteRequest("Sigo aqui", primera.token(), "Ana Maria", "ana@mail.com", "3001112233"));

        var recuperada = servicio.obtener(primera.token());
        assertThat(recuperada.nombre()).isEqualTo("Ana Maria");
        assertThat(recuperada.correo()).isEqualTo("ana@mail.com");
        assertThat(recuperada.telefono()).isEqualTo("3001112233");

        // No se creo un hilo nuevo: ambos mensajes viven en la misma conversacion.
        var hilosConEseToken = conversacionRepository.findAll().stream()
                .filter(c -> primera.token().equals(c.getToken()))
                .count();
        assertThat(hilosConEseToken).isEqualTo(1);
        assertThat(servicio.mensajes(primera.token())).hasSize(2);
    }
}
