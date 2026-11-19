package com.tecnicsecure.controller;

import com.tecnicsecure.dto.catalogo.ProductoDetalle;
import com.tecnicsecure.dto.catalogo.ProductoResumen;
import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la capa web (MockMvc standalone): comprueban el JSON de salida
 * del controlador, incluidos los caracteres especiales (ñ, tildes).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogoController · respuestas JSON")
class CatalogoControllerTest {

    @Mock private CatalogoService catalogoService;

    @InjectMocks private CatalogoController catalogoController;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogoController)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    private ProductoResumen producto() {
        return new ProductoResumen(1, "Cámara Domo 4K IP", "CAM-DOM-4K-001",
                "Domo antivandálico de 8 MP con visión nocturna IR de 30 m.",
                new BigDecimal("249900.00"), new BigDecimal("212415.00"),
                new BigDecimal("15.00"), 25, false,
                "img/productos/camara-domo.jpg", true,
                1, "Cámaras de Seguridad", "Hikvision");
    }

    @Test
    @DisplayName("GET /api/productos devuelve el listado paginado")
    void listarProductos() throws Exception {
        when(catalogoService.listar(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(producto()), 0, 12, 1, 1, true));

        mockMvc.perform(get("/api/productos").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].nombre").value("Cámara Domo 4K IP"))
                .andExpect(jsonPath("$.contenido[0].precioFinal").value(212415.00))
                .andExpect(jsonPath("$.contenido[0].categoria").value("Cámaras de Seguridad"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("GET /api/productos/destacados devuelve los productos de portada")
    void destacados() throws Exception {
        when(catalogoService.destacados()).thenReturn(List.of(producto()));

        mockMvc.perform(get("/api/productos/destacados").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destacado").value(true))
                .andExpect(jsonPath("$[0].descuentoPct").value(15.00));
    }

    @Test
    @DisplayName("GET /api/productos/{id} devuelve el detalle con calificación")
    void detalle() throws Exception {
        when(catalogoService.obtener(1)).thenReturn(new ProductoDetalle(
                1, "Cámara Domo 4K IP", "CAM-DOM-4K-001", "7701234567890",
                "Cámara domo 4K", new BigDecimal("249900.00"), new BigDecimal("212415.00"),
                new BigDecimal("15.00"), new BigDecimal("19.00"), 24,
                25, 5, 100, false, "img/productos/camara-domo.jpg",
                List.of(), true, true, 1, "Cámaras de Seguridad", 1, "Hikvision",
                4.5, 2L, null));

        mockMvc.perform(get("/api/productos/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProducto").value(1))
                .andExpect(jsonPath("$.calificacionPromedio").value(4.5));
    }
}
