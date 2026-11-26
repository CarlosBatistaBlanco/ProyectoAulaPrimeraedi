/*
 * Este archivo atiende la pantalla de Inventario en los paneles de administrador y empleado.
 * Permite revisar cuántas unidades hay disponibles en bodega, ver las alertas de productos
 * por agotarse, consultar el registro de entradas y salidas de cada equipo y anotar nuevos movimientos.
 */
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
// Solo administradores y empleados pueden ver y mover existencias de bodega
@PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
public class InventarioController {

    private final InventarioService inventarioService;
    private final UsuarioActual usuarioActual;

    // Conecta con el encargado del inventario y con los datos de quien realiza el movimiento
    public InventarioController(InventarioService inventarioService, UsuarioActual usuarioActual) {
        this.inventarioService = inventarioService;
        this.usuarioActual = usuarioActual;
    }

    // Muestra solo los productos que llegaron a su cantidad mínima o están cerca de agotarse
    @Operation(summary = "Productos con stock igual o por debajo del minimo")
    @GetMapping("/stock-bajo")
    public List<KardexProducto> stockBajo() {
        return inventarioService.stockBajo();
    }

    // Entrega la tabla completa de productos junto con las unidades disponibles hoy en bodega
    @Operation(summary = "Existencias de todos los productos")
    @GetMapping
    public List<KardexProducto> todos() {
        return inventarioService.todos();
    }

    // Muestra el registro completo de entradas, salidas y ajustes de bodega de un producto puntual
    @Operation(summary = "Kardex (historial) de un producto")
    @GetMapping("/productos/{id}/kardex")
    public List<MovimientoResponse> kardex(@PathVariable Integer id) {
        return inventarioService.kardex(id);
    }

    // Muestra los últimos 50 movimientos de bodega ocurridos en toda la tienda
    @Operation(summary = "Ultimos 50 movimientos de inventario")
    @GetMapping("/movimientos")
    public List<MovimientoResponse> ultimosMovimientos() {
        return inventarioService.ultimosMovimientos();
    }

    // Anota una nueva entrada de mercancía, salida o ajuste manual dejando constancia de quién lo hizo
    @Operation(summary = "Registrar entrada, salida o ajuste de inventario")
    @PostMapping("/productos/{id}/movimientos")
    public ResponseEntity<MovimientoResponse> registrar(@PathVariable Integer id,
                                                        @Valid @RequestBody MovimientoInventarioRequest request) {
        MovimientoResponse respuesta = inventarioService.registrarMovimiento(
                id, request, usuarioActual.idOpcional());
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
