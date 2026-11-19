package com.tecnicsecure.service;

import com.tecnicsecure.dto.soporte.MensajeChatResponse;
import com.tecnicsecure.entity.Cliente;
import com.tecnicsecure.entity.MensajeChat;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.impl.SoporteServiceImpl;
import com.tecnicsecure.util.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del chat de soporte: listado de clientes para el panel y
 * publicacion del mensaje en el topico STOMP (tiempo real).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SoporteService · chat y tiempo real")
class SoporteServiceImplChatTest {

    @Mock private ContactoRepository contactoRepository;
    @Mock private SolicitudServicioRepository solicitudRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private MensajeChatRepository chatRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioActual usuarioActual;
    @Mock private SimpMessagingTemplate mensajeria;

    @InjectMocks private SoporteServiceImpl soporteService;

    private Cliente cliente;

    @BeforeEach
    void preparar() {
        cliente = new Cliente();
        cliente.setIdCliente(5);
        cliente.setNombre("Ana Martínez");
        cliente.setCorreo("ana@ejemplo.com");
    }

    @Test
    @DisplayName("Lista los clientes con idCliente, nombre y correo")
    void listaClientesParaElPanel() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        List<Map<String, Object>> filas = soporteService.listarClientesChat();

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0)).containsEntry("idCliente", 5)
                .containsEntry("nombre", "Ana Martínez")
                .containsEntry("correo", "ana@ejemplo.com");
    }

    @Test
    @DisplayName("Tolera clientes con nombre o correo nulos")
    void listaClientesConDatosNulos() {
        cliente.setNombre(null);
        cliente.setCorreo(null);
        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        Map<String, Object> fila = soporteService.listarClientesChat().get(0);

        assertThat(fila).containsEntry("nombre", "").containsEntry("correo", "");
    }

    @Test
    @DisplayName("Publica el mensaje nuevo en /topic/chat/{idCliente}")
    void publicaMensajeEnElTopico() {
        when(clienteRepository.findById(5)).thenReturn(java.util.Optional.of(cliente));
        when(chatRepository.save(any(MensajeChat.class))).thenAnswer(inv -> {
            MensajeChat m = inv.getArgument(0);
            m.setIdMensaje(99);
            return m;
        });

        MensajeChatResponse respuesta = soporteService.enviarMensajeCliente(5, "Hola", 1, true);

        assertThat(respuesta.idMensaje()).isEqualTo(99);
        verify(mensajeria).convertAndSend(eq("/topic/chat/5"), any(MensajeChatResponse.class));
    }
}
