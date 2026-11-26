/*
 * Este archivo atiende las peticiones de compra cuando un cliente confirma su carrito en la
 * tienda y muestra el estado de los envíos en la pantalla Mis Pedidos del panel. También
 * permite a empleados y administradores listar todas las ventas y actualizar cada etapa de entrega.
 */
package com.tecnicsecure.controller;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.pedido.CambioEstadoRequest;
import com.tecnicsecure.dto.pedido.PedidoRequest;
import com.tecnicsecure.dto.pedido.PedidoResponse;
import com.tecnicsecure.entity.PedidoHistorial;
import com.tecnicsecure.service.PedidoService;
import com.tecnicsecure.util.UsuarioActual;
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

@Tag(name = "3. Pedidos", description = "Checkout, seguimiento y cambios de estado")
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioActual usuarioActual;

    // Conecta con el encargado de gestionar compras y con los datos de quien realiza cambios
    public PedidoController(PedidoService pedidoService, UsuarioActual usuarioActual) {
        this.pedidoService = pedidoService;
        this.usuarioActual = usuarioActual;
    }

    // Recibe la compra nueva cuando el cliente confirma su carrito y la guarda en el sistema
    @Operation(summary = "Registrar un pedido (checkout del carrito)")
    @PostMapping
    public ResponseEntity<PedidoResponse> registrar(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.registrar(request));
    }

    // Muestra al cliente con sesión iniciada la lista de sus propias compras, empezando por la más reciente
    @Operation(summary = "Mis pedidos (cliente autenticado)")
    @GetMapping("/mis-pedidos")
    public PageResponse<PedidoResponse> misPedidos(@RequestParam(defaultValue = "0") int pagina,
                                                   @RequestParam(defaultValue = "10") int tamanio) {
        return pedidoService.misPedidos(pagina(pagina, tamanio));
    }

    // Muestra todos los pedidos de la tienda con filtros por cliente o estado (solo personal interno)
    @Operation(summary = "Listar pedidos (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @GetMapping
    public PageResponse<PedidoResponse> listar(@RequestParam(required = false) Integer idCliente,
                                               @RequestParam(required = false) String estado,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "10") int tamanio) {
        return pedidoService.listar(idCliente, estado, pagina(pagina, tamanio));
    }

    // Muestra el resumen completo de una compra: productos elegidos, cantidades, totales y datos del comprador
    @Operation(summary = "Detalle de un pedido")
    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Integer id) {
        return pedidoService.obtener(id);
    }

    // Cambia la etapa de un pedido (pendiente, confirmado, enviado, entregado o cancelado; solo personal interno)
    @Operation(summary = "Cambiar el estado de un pedido (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Integer id,
                                        @Valid @RequestBody CambioEstadoRequest request) {
        Integer idUsuario = usuarioActual.idOpcional();
        return pedidoService.cambiarEstado(id, request.estado(), request.observacion(), idUsuario);
    }

    // Entrega la línea de tiempo con todas las actualizaciones que ha tenido el envío de un pedido
    @Operation(summary = "Historial de cambios de estado de un pedido")
    @GetMapping("/{id}/historial")
    public List<PedidoHistorial> historial(@PathVariable Integer id) {
        return pedidoService.historial(id);
    }

    // Organiza las listas por páginas (máximo 100 compras por hoja) mostrando siempre las más recientes arriba
    private PageRequest pagina(int pagina, int tamanio) {
        return PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanio)),
                Sort.by(Sort.Direction.DESC, "creadoEn"));
    }
}
