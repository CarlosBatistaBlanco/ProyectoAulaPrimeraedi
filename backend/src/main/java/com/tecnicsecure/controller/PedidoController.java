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

    public PedidoController(PedidoService pedidoService, UsuarioActual usuarioActual) {
        this.pedidoService = pedidoService;
        this.usuarioActual = usuarioActual;
    }

    @Operation(summary = "Registrar un pedido (checkout del carrito)")
    @PostMapping
    public ResponseEntity<PedidoResponse> registrar(@Valid @RequestBody PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.registrar(request));
    }

    @Operation(summary = "Mis pedidos (cliente autenticado)")
    @GetMapping("/mis-pedidos")
    public PageResponse<PedidoResponse> misPedidos(@RequestParam(defaultValue = "0") int pagina,
                                                   @RequestParam(defaultValue = "10") int tamanio) {
        return pedidoService.misPedidos(pagina(pagina, tamanio));
    }

    @Operation(summary = "Listar pedidos (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @GetMapping
    public PageResponse<PedidoResponse> listar(@RequestParam(required = false) Integer idCliente,
                                               @RequestParam(required = false) String estado,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "10") int tamanio) {
        return pedidoService.listar(idCliente, estado, pagina(pagina, tamanio));
    }

    @Operation(summary = "Detalle de un pedido")
    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Integer id) {
        return pedidoService.obtener(id);
    }

    @Operation(summary = "Cambiar el estado de un pedido (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Integer id,
                                        @Valid @RequestBody CambioEstadoRequest request) {
        Integer idUsuario = usuarioActual.idOpcional();
        return pedidoService.cambiarEstado(id, request.estado(), request.observacion(), idUsuario);
    }

    @Operation(summary = "Historial de cambios de estado de un pedido")
    @GetMapping("/{id}/historial")
    public List<PedidoHistorial> historial(@PathVariable Integer id) {
        return pedidoService.historial(id);
    }

    private PageRequest pagina(int pagina, int tamanio) {
        return PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanio)),
                Sort.by(Sort.Direction.DESC, "creadoEn"));
    }
}
