/*
 * Este archivo atiende la pantalla Mis Direcciones del panel y el paso de envío al finalizar
 * una compra en el carrito. Permite a cada usuario guardar varias direcciones de entrega,
 * elegir una como principal para llenar el formulario más rápido, editarlas o eliminarlas.
 */
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

    // Conecta con el encargado de guardar, cambiar y borrar direcciones de entrega
    public DireccionController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    // Muestra todas las direcciones guardadas por la persona que tiene sesión abierta
    @Operation(summary = "Listar mis direcciones")
    @GetMapping
    public List<DireccionResponse> listar() {
        return direccionService.misDirecciones();
    }

    // Trae la dirección marcada como favorita para llenar automáticamente el formulario de envío
    @Operation(summary = "Direccion predeterminada (para el checkout)")
    @GetMapping("/principal")
    public ResponseEntity<DireccionResponse> principal() {
        DireccionResponse principal = direccionService.direccionPrincipal();
        // Si la persona todavía no ha marcado una dirección principal, responde vacío sin mostrar error
        return principal != null ? ResponseEntity.ok(principal) : ResponseEntity.noContent().build();
    }

    // Guarda una dirección de entrega nueva para el usuario conectado
    @Operation(summary = "Crear una direccion")
    @PostMapping
    public ResponseEntity<DireccionResponse> crear(@Valid @RequestBody DireccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(direccionService.crear(request));
    }

    // Guarda los cambios hechos sobre una dirección existente
    @Operation(summary = "Actualizar una direccion")
    @PutMapping("/{id}")
    public DireccionResponse actualizar(@PathVariable Integer id,
                                        @Valid @RequestBody DireccionRequest request) {
        return direccionService.actualizar(id, request);
    }

    // Marca una dirección como la predeterminada para futuras compras
    @Operation(summary = "Marcar una direccion como predeterminada")
    @PatchMapping("/{id}/predeterminada")
    public DireccionResponse marcarPredeterminada(@PathVariable Integer id) {
        return direccionService.marcarPredeterminada(id);
    }

    // Quita una dirección guardada de la lista del usuario
    @Operation(summary = "Eliminar una direccion")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        direccionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
