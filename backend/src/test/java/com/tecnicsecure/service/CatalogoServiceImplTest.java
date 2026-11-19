package com.tecnicsecure.service;

import com.tecnicsecure.dto.catalogo.ProductoRequest;
import com.tecnicsecure.entity.Categoria;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.impl.CatalogoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoService · productos y precio final")
class CatalogoServiceImplTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private MarcaRepository marcaRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private ProductoImagenRepository productoImagenRepository;
    @Mock private ResenaRepository resenaRepository;

    @InjectMocks private CatalogoServiceImpl catalogoService;

    private Categoria categoria;

    @BeforeEach
    void preparar() {
        categoria = new Categoria();
        categoria.setIdCategoria(1);
        categoria.setNombre("Cámaras de Seguridad");
    }

    private ProductoRequest solicitud(String sku) {
        return new ProductoRequest("Cámara Domo 4K IP", sku, "7701234567890",
                "Cámara domo 4K con visión nocturna", new BigDecimal("249900.00"),
                new BigDecimal("15.00"), new BigDecimal("19.00"), 24, 25, 5, 100,
                "img/productos/camara-domo.jpg", true, true, 1, null);
    }

    @Test
    @DisplayName("Calcula el precio final aplicando el descuento")
    void calculaPrecioFinalConDescuento() {
        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("249900.00"));
        producto.setDescuentoPct(new BigDecimal("15.00"));

        // 249900 * 0.85 = 212415
        assertThat(producto.getPrecioFinal()).isEqualByComparingTo(new BigDecimal("212415.00"));
        assertThat(producto.isTieneDescuento()).isTrue();
    }

    @Test
    @DisplayName("Sin descuento el precio final es el de lista")
    void sinDescuentoMantienePrecio() {
        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("179900.00"));
        producto.setDescuentoPct(BigDecimal.ZERO);

        assertThat(producto.getPrecioFinal()).isEqualByComparingTo(new BigDecimal("179900.00"));
        assertThat(producto.isTieneDescuento()).isFalse();
    }

    @Test
    @DisplayName("Crea un producto correctamente")
    void creaProducto() {
        when(productoRepository.existsBySku("CAM-NEW-001")).thenReturn(false);
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setIdProducto(99);
            return p;
        });
        when(productoImagenRepository.findByIdProductoOrderByOrdenAsc(99)).thenReturn(java.util.List.of());
        when(resenaRepository.promedioPorProducto(99)).thenReturn(0.0);
        when(resenaRepository.countByIdProducto(99)).thenReturn(0L);
        when(productoRepository.findById(99)).thenAnswer(inv -> {
            Producto p = new Producto();
            p.setIdProducto(99);
            p.setNombre("Cámara Domo 4K IP");
            p.setPrecio(new BigDecimal("249900.00"));
            p.setDescuentoPct(new BigDecimal("15.00"));
            p.setStock(25);
            p.setCategoria(categoria);
            return Optional.of(p);
        });

        var creado = catalogoService.crear(solicitud("CAM-NEW-001"));

        assertThat(creado.idProducto()).isEqualTo(99);
        assertThat(creado.categoria()).isEqualTo("Cámaras de Seguridad");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    @DisplayName("Rechaza SKU duplicado")
    void rechazaSkuDuplicado() {
        when(productoRepository.existsBySku("CAM-DOM-4K-001")).thenReturn(true);

        assertThatThrownBy(() -> catalogoService.crear(solicitud("CAM-DOM-4K-001")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("SKU");

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("La baja de producto es lógica (no borra el historial)")
    void eliminarEsBajaLogica() {
        Producto producto = new Producto();
        producto.setIdProducto(3);
        producto.setActivo(true);
        when(productoRepository.findById(3)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);

        catalogoService.eliminar(3);

        assertThat(producto.getActivo()).isFalse();
        verify(productoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Producto inexistente devuelve 404")
    void productoInexistente() {
        when(productoRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogoService.obtener(404))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no encontrado");
    }
}
