package com.tecnicsecure.controller;

import com.tecnicsecure.dto.inventario.KardexProducto;
import com.tecnicsecure.dto.inventario.MovimientoInventarioRequest;
import com.tecnicsecure.dto.inventario.MovimientoResponse;
import com.tecnicsecure.service.InventarioService;
import com.tecnicsecure.util.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4. Inventario", description = "Existencias, alertas de stock minimo y kardex")
@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
public class InventarioController {

    private final InventarioService inventarioService;
    private final UsuarioActual usuarioActual;

    public InventarioController(InventarioService inventarioService, UsuarioActual usuarioActual) {
        this.inventarioService = inventarioService;
        this.usuarioActual = usuarioActual;
    }

    @Operation(summary = "Productos con stock igual o por debajo del minimo")
    @GetMapping("/stock-bajo")
    public List<KardexProducto> stockBajo() {
        return inventarioService.stockBajo();
    }

    @Operation(summary = "Existencias de todos los productos")
    @GetMapping
    public List<KardexProducto> todos() {
        return inventarioService.todos();
    }

    @Operation(summary = "Kardex (historial) de un producto")
    @GetMapping("/productos/{id}/kardex")
    public List<MovimientoResponse> kardex(@PathVariable Integer id) {
        return inventarioService.kardex(id);
    }

    @Operation(summary = "Ultimos 50 movimientos de inventario")
    @GetMapping("/movimientos")
    public List<MovimientoResponse> ultimosMovimientos() {
        return inventarioService.ultimosMovimientos();
    }

    @Operation(summary = "Registrar entrada, salida o ajuste de inventario")
    @PostMapping("/productos/{id}/movimientos")
    public ResponseEntity<MovimientoResponse> registrar(@PathVariable Integer id,
                                                        @Valid @RequestBody MovimientoInventarioRequest request) {
        MovimientoResponse respuesta = inventarioService.registrarMovimiento(
                id, request, usuarioActual.idOpcional());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
