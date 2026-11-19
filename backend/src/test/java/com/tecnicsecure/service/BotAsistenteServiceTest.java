package com.tecnicsecure.service;

import com.tecnicsecure.entity.MensajeChat;
import com.tecnicsecure.entity.Pedido;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.entity.Servicio;
import com.tecnicsecure.repository.MensajeChatRepository;
import com.tecnicsecure.repository.PedidoRepository;
import com.tecnicsecure.repository.ProductoRepository;
import com.tecnicsecure.repository.ServicioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotAsistenteServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private MensajeChatRepository chatRepository;
    @Mock private SimpMessagingTemplate mensajeria;

    @InjectMocks
    private BotAsistenteService bot;

    @Test
    void consultaPrecioProductoRespondeDesdeInventarioLocal() {
        Producto cam = new Producto();
        cam.setNombre("Camara Domo IP Full HD");
        cam.setPrecio(new BigDecimal("180000"));
        cam.setStock(14);
        cam.setActivo(true);
        when(productoRepository.findAll()).thenReturn(List.of(cam));
        when(chatRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = bot.responderAutomatico(3, "Hola, cuanto vale la camara?");

        assertNotNull(resp);
        assertTrue(resp.mensaje().contains("Camara Domo IP Full HD"));
        assertTrue(resp.mensaje().contains("14 disp."));
        verify(mensajeria).convertAndSend(eq("/topic/chat/3"), any(Object.class));
    }

    @Test
    void consultaPedidoRespondeEstadoRealDelCliente() {
        Pedido ped = new Pedido();
        ped.setIdPedido(12);
        ped.setEstado("enviado");
        ped.setTotal(new BigDecimal("250000"));
        when(pedidoRepository.findByClienteIdCliente(eq(7), any()))
                .thenReturn(new PageImpl<>(List.of(ped)));

        String txt = bot.generarRespuesta(7, "Como va mi pedido #12?");
        assertTrue(txt.contains("#12"));
        assertTrue(txt.contains("enviado"));
    }

    @Test
    void consultaServiciosListaCatalogoLocal() {
        Servicio s = new Servicio();
        s.setTitulo("Instalacion CCTV");
        when(servicioRepository.findAllByOrderByOrdenAsc()).thenReturn(List.of(s));

        String txt = bot.generarRespuesta(1, "Que servicios de instalacion tienen?");
        assertTrue(txt.contains("Instalacion CCTV"));
    }
}
