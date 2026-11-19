package com.tecnicsecure.service;

import com.tecnicsecure.dto.inventario.MovimientoInventarioRequest;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.InventarioMovimientoRepository;
import com.tecnicsecure.repository.ProductoRepository;
import com.tecnicsecure.service.impl.InventarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventarioService · entradas, salidas y ajustes")
class InventarioServiceImplTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private InventarioMovimientoRepository movimientoRepository;

    @InjectMocks private InventarioServiceImpl inventarioService;

    private Producto producto;

    @BeforeEach
    void preparar() {
        producto = new Producto();
        producto.setIdProducto(5);
        producto.setNombre("Alarma Perimetral Inalámbrica");
        producto.setStock(10);
        producto.setStockMinimo(5);
    }

    @Test
    @DisplayName("Una entrada suma al stock y registra el movimiento")
    void registraEntrada() {
        when(productoRepository.findById(5)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        var respuesta = inventarioService.registrarMovimiento(5,
                new MovimientoInventarioRequest("entrada", 15, "Compra a proveedor", null), 1);

        assertThat(producto.getStock()).isEqualTo(25);
        assertThat(respuesta.stockResultante()).isEqualTo(25);
        assertThat(respuesta.tipo()).isEqualTo("entrada");
    }

    @Test
    @DisplayName("Una salida descuenta el stock")
    void registraSalida() {
        when(productoRepository.findById(5)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        inventarioService.registrarMovimiento(5,
                new MovimientoInventarioRequest("salida", 4, "Venta directa", null), 1);

        assertThat(producto.getStock()).isEqualTo(6);
    }

    @Test
    @DisplayName("Una salida mayor al stock es rechazada")
    void rechazaSalidaSinStock() {
        when(productoRepository.findById(5)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> inventarioService.registrarMovimiento(5,
                new MovimientoInventarioRequest("salida", 50, "Venta", null), 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Stock insuficiente");

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un ajuste fija el stock al valor contado")
    void registraAjuste() {
        when(productoRepository.findById(5)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        inventarioService.registrarMovimiento(5,
                new MovimientoInventarioRequest("ajuste", 3, "Conteo físico", null), 1);

        assertThat(producto.getStock()).isEqualTo(3);
    }

    @Test
    @DisplayName("Rechaza tipos de movimiento desconocidos")
    void rechazaTipoInvalido() {
        assertThatThrownBy(() -> inventarioService.registrarMovimiento(5,
                new MovimientoInventarioRequest("transferencia", 1, null, null), 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("tipo debe ser");
    }

    @Test
    @DisplayName("Detecta stock por debajo del mínimo")
    void detectaStockBajo() {
        producto.setStock(5);
        assertThat(producto.isStockBajo()).isTrue();

        producto.setStock(6);
        assertThat(producto.isStockBajo()).isFalse();
    }
}
