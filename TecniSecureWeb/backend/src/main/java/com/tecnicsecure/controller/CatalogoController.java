/*
 * Este archivo alimenta la vitrina pública de productos, la portada de destacados, la página
 * de Servicios y la sección de gestión de productos del panel interno. Permite buscar y filtrar
 * equipos de seguridad, ver sus detalles y, al personal autorizado, crear o actualizar el catálogo.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.dto.catalogo.*;
import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "2. Catalogo", description = "Productos, categorias, marcas y servicios")
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService catalogoService;

    // Conecta con el encargado de buscar y modificar productos, categorías, marcas y servicios
    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    // Busca productos por palabra, categoría o marca y los entrega por páginas (hasta 100 por página)
    @Operation(summary = "Listar productos (paginado, con busqueda y filtros)")
    @GetMapping("/productos")
    public PageResponse<ProductoResumen> listar(@RequestParam(required = false) String texto,
                                                @RequestParam(required = false) Integer idCategoria,
                                                @RequestParam(required = false) Integer idMarca,
                                                @RequestParam(defaultValue = "0") int pagina,
                                                @RequestParam(defaultValue = "12") int tamanio,
                                                @RequestParam(defaultValue = "idProducto") String orden) {
        // Ajusta los límites de página para evitar números negativos o listados demasiado grandes
        return catalogoService.listar(texto, idCategoria, idMarca,
                PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanio)),
                        Sort.by(Sort.Direction.ASC, orden)));
    }

    // Entrega los productos destacados que se exhiben en la página principal de la tienda
    @Operation(summary = "Productos destacados para la portada")
    @GetMapping("/productos/destacados")
    public List<ProductoResumen> destacados() {
        return catalogoService.destacados();
    }

    // Muestra toda la información completa de un producto cuando el cliente hace clic en él
    @Operation(summary = "Detalle de un producto")
    @GetMapping("/productos/{id}")
    public ProductoDetalle detalle(@PathVariable Integer id) {
        return catalogoService.obtener(id);
    }

    // Agrega un producto nuevo al catálogo desde el panel (solo administradores o empleados)
    @Operation(summary = "Crear producto (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/productos")
    public ResponseEntity<ProductoDetalle> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crear(request));
    }

    // Edita el nombre, precio, descripción o foto de un producto existente (solo personal autorizado)
    @Operation(summary = "Actualizar producto (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PutMapping("/productos/{id}")
    public ProductoDetalle actualizar(@PathVariable Integer id, @Valid @RequestBody ProductoRequest request) {
        return catalogoService.actualizar(id, request);
    }

    // Retira un producto de la venta para que ya no aparezca en la tienda (solo administrador)
    @Operation(summary = "Desactivar producto (administrador)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        catalogoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Entrega la lista de categorías para llenar los filtros de búsqueda en el catálogo
    @Operation(summary = "Listar categorias")
    @GetMapping("/categorias")
    public List<CategoriaRequest> categorias() {
        return catalogoService.categorias();
    }

    // Crea una categoría nueva para organizar productos (solo administradores o empleados)
    @Operation(summary = "Crear categoria")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/categorias")
    public ResponseEntity<Map<String, Object>> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("idCategoria", catalogoService.crearCategoria(request)));
    }

    // Entrega todas las marcas de equipos disponibles para los filtros del catálogo
    @Operation(summary = "Listar marcas")
    @GetMapping("/marcas")
    public List<MarcaRequest> marcas() {
        return catalogoService.marcas();
    }

    // Registra una marca nueva de equipos (solo administradores o empleados)
    @Operation(summary = "Crear marca")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/marcas")
    public ResponseEntity<Map<String, Object>> crearMarca(@Valid @RequestBody MarcaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("idMarca", catalogoService.crearMarca(request)));
    }

    // Muestra los servicios técnicos disponibles en la página de Servicios (instalación, mantenimiento y asesoría)
    @Operation(summary = "Listar servicios ofrecidos")
    @GetMapping("/servicios")
    public List<ServicioResponse> servicios() {
        return catalogoService.servicios();
    }
}
