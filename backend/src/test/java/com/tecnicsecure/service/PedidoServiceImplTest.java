package com.tecnicsecure.service;

import com.tecnicsecure.dto.pedido.PedidoRequest;
import com.tecnicsecure.entity.Cliente;
import com.tecnicsecure.entity.EstadoPedido;
import com.tecnicsecure.entity.Pedido;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.impl.PedidoServiceImpl;
import com.tecnicsecure.util.UsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del registro de pedidos (Mockito).
 * Verifican las reglas de negocio más delicadas: cálculo del total con
 * precio de la base de datos, descuento de stock y validación de estados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService · registro y estados")
class PedidoServiceImplTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private PedidoHistorialRepository historialRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private InventarioService inventarioService;
    @Mock private SoporteService soporteService;
    @Mock private UsuarioActual usuarioActual;

    @InjectMocks private PedidoServiceImpl pedidoService;

    private Producto producto;

    @BeforeEach
    void preparar() {
        producto = new Producto();
        producto.setIdProducto(1);
        producto.setNombre("Cámara Domo 4K IP");
        producto.setPrecio(new BigDecimal("249900.00"));
        producto.setDescuentoPct(new BigDecimal("15.00"));
        producto.setStock(25);
        producto.setActivo(true);
    }

    private PedidoRequest solicitud(int cantidad) {
        return new PedidoRequest("Ana Martínez", "ana@ejemplo.com", "3001234567",
                "Calle 1 #2-3", List.of(new PedidoRequest.ItemCarrito(1, cantidad)));
    }

    @Test
    @DisplayName("Calcula el total con el precio de la base de datos y el descuento")
    void registraPedidoConPrecioReal() {
        when(clienteRepository.findByCorreoIgnoreCase("ana@ejemplo.com")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.findById(1)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setIdPedido(10);
            return p;
        });

        var respuesta = pedidoService.registrar(solicitud(2));

        // 249.900 con 15% de descuento = 212.415 por unidad -> 424.830
        assertThat(respuesta.total()).isEqualByComparingTo(new BigDecimal("424830.00"));
        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.PENDIENTE);
        assertThat(respuesta.idPedido()).isEqualTo(10);
        verify(inventarioService, times(1)).descontarPorVenta(eq(1), eq(2), any());
    }

    @Test
    @DisplayName("Rechaza el pedido cuando no hay stock suficiente")
    void rechazaSinStock() {
        when(clienteRepository.findByCorreoIgnoreCase(any())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        producto.setStock(1);
        when(productoRepository.findById(1)).thenReturn(Optional.of(producto));

        var request = solicitud(5);

        assertThatThrownBy(() -> pedidoService.registrar(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(inventarioService, never()).descontarPorVenta(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Rechaza productos inexistentes o inactivos")
    void rechazaProductoInactivo() {
        when(clienteRepository.findByCorreoIgnoreCase(any())).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        producto.setActivo(false);
        when(productoRepository.findById(1)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> pedidoService.registrar(solicitud(1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no disponible");
    }

    @Test
    @DisplayName("Cambia el estado siguiendo el flujo permitido")
    void cambiaEstadoValido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7);
        pedido.setEstado(EstadoPedido.PENDIENTE);

        when(pedidoRepository.findById(7)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        var respuesta = pedidoService.cambiarEstado(7, "confirmado", null, 1);

        assertThat(respuesta.estado()).isEqualTo(EstadoPedido.CONFIRMADO);
        verify(historialRepository).save(any());
    }

    @Test
    @DisplayName("Impide saltar estados del flujo")
    void rechazaSaltoDeEstado() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        when(pedidoRepository.findById(7)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstado(7, "entregado", null, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No se puede pasar");
    }

    @Test
    @DisplayName("Rechaza estados inexistentes")
    void rechazaEstadoInvalido() {
        Pedido pedido = new Pedido();
        pedido.setIdPedido(7);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        when(pedidoRepository.findById(7)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cambiarEstado(7, "inventado", null, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Estado no válido");
    }
}
