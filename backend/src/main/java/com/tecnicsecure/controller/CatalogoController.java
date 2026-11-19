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

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @Operation(summary = "Listar productos (paginado, con busqueda y filtros)")
    @GetMapping("/productos")
    public PageResponse<ProductoResumen> listar(@RequestParam(required = false) String texto,
                                                @RequestParam(required = false) Integer idCategoria,
                                                @RequestParam(required = false) Integer idMarca,
                                                @RequestParam(defaultValue = "0") int pagina,
                                                @RequestParam(defaultValue = "12") int tamanio,
                                                @RequestParam(defaultValue = "idProducto") String orden) {
        return catalogoService.listar(texto, idCategoria, idMarca,
                PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanio)),
                        Sort.by(Sort.Direction.ASC, orden)));
    }

    @Operation(summary = "Productos destacados para la portada")
    @GetMapping("/productos/destacados")
    public List<ProductoResumen> destacados() {
        return catalogoService.destacados();
    }

    @Operation(summary = "Detalle de un producto")
    @GetMapping("/productos/{id}")
    public ProductoDetalle detalle(@PathVariable Integer id) {
        return catalogoService.obtener(id);
    }

    @Operation(summary = "Crear producto (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/productos")
    public ResponseEntity<ProductoDetalle> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crear(request));
    }

    @Operation(summary = "Actualizar producto (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PutMapping("/productos/{id}")
    public ProductoDetalle actualizar(@PathVariable Integer id, @Valid @RequestBody ProductoRequest request) {
        return catalogoService.actualizar(id, request);
    }

    @Operation(summary = "Desactivar producto (administrador)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        catalogoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar categorias")
    @GetMapping("/categorias")
    public List<CategoriaRequest> categorias() {
        return catalogoService.categorias();
    }

    @Operation(summary = "Crear categoria")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/categorias")
    public ResponseEntity<Map<String, Object>> crearCategoria(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("idCategoria", catalogoService.crearCategoria(request)));
    }

    @Operation(summary = "Listar marcas")
    @GetMapping("/marcas")
    public List<MarcaRequest> marcas() {
        return catalogoService.marcas();
    }

    @Operation(summary = "Crear marca")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/marcas")
    public ResponseEntity<Map<String, Object>> crearMarca(@Valid @RequestBody MarcaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("idMarca", catalogoService.crearMarca(request)));
    }

    @Operation(summary = "Listar servicios ofrecidos")
    @GetMapping("/servicios")
    public List<ServicioResponse> servicios() {
        return catalogoService.servicios();
    }
}
