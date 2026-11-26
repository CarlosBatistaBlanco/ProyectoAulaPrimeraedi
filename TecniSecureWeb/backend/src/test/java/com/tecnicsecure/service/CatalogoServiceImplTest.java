/*
 * Este archivo contiene pruebas automáticas para la gestión del catálogo de productos.
 * Verifica que los descuentos se calculen bien, que no se repitan códigos de referencia
 * y que retirar un producto solo lo oculte sin borrar el historial de ventas pasadas.
 */
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

    // Prepara una categoría de ejemplo ("Cámaras de Seguridad") antes de cada prueba
    @BeforeEach
    void preparar() {
        categoria = new Categoria();
        categoria.setIdCategoria(1);
        categoria.setNombre("Cámaras de Seguridad");
    }

    // Prepara los datos de un producto de ejemplo para reutilizarlos en las pruebas de registro
    private ProductoRequest solicitud(String sku) {
        return new ProductoRequest("Cámara Domo 4K IP", sku, "7701234567890",
                "Cámara domo 4K con visión nocturna", new BigDecimal("249900.00"),
                new BigDecimal("15.00"), new BigDecimal("19.00"), 24, 25, 5, 100,
                "img/productos/camara-domo.jpg", true, true, 1, null);
    }

    // Comprueba que cuando un producto tiene descuento, el precio final refleje la rebaja exacta
    @Test
    @DisplayName("Calcula el precio final aplicando el descuento")
    void calculaPrecioFinalConDescuento() {
        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("249900.00"));
        producto.setDescuentoPct(new BigDecimal("15.00"));

        // Con un 15% de descuento sobre 249.900, el precio final debe ser 212.415
        assertThat(producto.getPrecioFinal()).isEqualByComparingTo(new BigDecimal("212415.00"));
        assertThat(producto.isTieneDescuento()).isTrue();
    }

    // Comprueba que si un producto no tiene descuento, conserve su precio normal sin cambios
    @Test
    @DisplayName("Sin descuento el precio final es el de lista")
    void sinDescuentoMantienePrecio() {
        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("179900.00"));
        producto.setDescuentoPct(BigDecimal.ZERO);

        assertThat(producto.getPrecioFinal()).isEqualByComparingTo(new BigDecimal("179900.00"));
        assertThat(producto.isTieneDescuento()).isFalse();
    }

    // Comprueba que un producto nuevo con datos completos se guarde bien junto con su categoría
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

    // Comprueba que el sistema impida registrar dos productos distintos con el mismo código de referencia
    @Test
    @DisplayName("Rechaza SKU duplicado")
    void rechazaSkuDuplicado() {
        when(productoRepository.existsBySku("CAM-DOM-4K-001")).thenReturn(true);

        assertThatThrownBy(() -> catalogoService.crear(solicitud("CAM-DOM-4K-001")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("SKU");

        verify(productoRepository, never()).save(any());
    }

    // Comprueba que retirar un producto del catálogo solo lo oculte para conservar intacto el historial de pedidos
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

    // Comprueba que intentar abrir un producto que no existe muestre un aviso claro de "no encontrado"
    @Test
    @DisplayName("Producto inexistente devuelve 404")
    void productoInexistente() {
        when(productoRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogoService.obtener(404))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no encontrado");
    }
}
