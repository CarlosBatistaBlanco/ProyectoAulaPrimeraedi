package com.tecnicsecure.controller;

import com.tecnicsecure.dto.direccion.DireccionRequest;
import com.tecnicsecure.dto.direccion.DireccionResponse;
import com.tecnicsecure.service.DireccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "12. Direcciones", description = "Direcciones de entrega de cada usuario")
@RestController
@RequestMapping("/api/direcciones")
public class DireccionController {

    private final DireccionService direccionService;

    public DireccionController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @Operation(summary = "Listar mis direcciones")
    @GetMapping
    public List<DireccionResponse> listar() {
        return direccionService.misDirecciones();
    }

    @Operation(summary = "Direccion predeterminada (para el checkout)")
    @GetMapping("/principal")
    public ResponseEntity<DireccionResponse> principal() {
        DireccionResponse principal = direccionService.direccionPrincipal();
        return principal != null ? ResponseEntity.ok(principal) : ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear una direccion")
    @PostMapping
    public ResponseEntity<DireccionResponse> crear(@Valid @RequestBody DireccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(direccionService.crear(request));
    }

    @Operation(summary = "Actualizar una direccion")
    @PutMapping("/{id}")
    public DireccionResponse actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody DireccionRequest request) {
        return direccionService.actualizar(id, request);
    }

    @Operation(summary = "Marcar una direccion como predeterminada")
    @PatchMapping("/{id}/predeterminada")
    public DireccionResponse marcarPredeterminada(@PathVariable Integer id) {
        return direccionService.marcarPredeterminada(id);
    }

    @Operation(summary = "Eliminar una direccion")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        direccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
