package com.tecnicsecure.controller;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.soporte.*;
import com.tecnicsecure.service.ConversacionVisitanteService;
import com.tecnicsecure.service.SoporteService;
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
import java.util.Map;

@Tag(name = "5. Soporte", description = "Contacto, solicitudes de servicio, chat y notificaciones")
@RestController
@RequestMapping("/api")
public class SoporteController {

    private final SoporteService soporteService;
    private final ConversacionVisitanteService conversacionVisitanteService;
    private final UsuarioActual usuarioActual;

    public SoporteController(SoporteService soporteService,
                             ConversacionVisitanteService conversacionVisitanteService,
                             UsuarioActual usuarioActual) {
        this.soporteService = soporteService;
        this.conversacionVisitanteService = conversacionVisitanteService;
        this.usuarioActual = usuarioActual;
    }

    @Operation(summary = "Enviar un mensaje de contacto (publico)")
    @PostMapping("/contactos")
    public ResponseEntity<Map<String, Object>> contacto(@Valid @RequestBody ContactoRequest request) {
        ContactoResponse creado = soporteService.registrarContacto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "ok", true,
                "mensaje", "¡Gracias! Tu mensaje fue recibido.",
                "idContacto", creado.idContacto()));
    }

    @Operation(summary = "Listar mensajes de contacto (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @GetMapping("/contactos")
    public PageResponse<ContactoResponse> contactos(@RequestParam(required = false) Boolean soloNoLeidos,
                                                    @RequestParam(defaultValue = "0") int pagina,
                                                    @RequestParam(defaultValue = "20") int tamanio) {
        return soporteService.listarContactos(soloNoLeidos, pagina(pagina, tamanio));
    }

    @Operation(summary = "Responder un mensaje de contacto (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/contactos/{id}/respuesta")
    public ContactoResponse responder(@PathVariable Integer id, @RequestBody Map<String, String> cuerpo) {
        return soporteService.responderContacto(id, cuerpo.get("respuesta"));
    }

    @Operation(summary = "Solicitar cotizacion o cita tecnica (publico)")
    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudResponse> solicitud(@Valid @RequestBody SolicitudRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(soporteService.crearSolicitud(request));
    }

    @Operation(summary = "Listar solicitudes de servicio (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @GetMapping("/solicitudes")
    public PageResponse<SolicitudResponse> solicitudes(@RequestParam(required = false) String estado,
                                                       @RequestParam(defaultValue = "0") int pagina,
                                                       @RequestParam(defaultValue = "20") int tamanio) {
        return soporteService.listarSolicitudes(estado, pagina(pagina, tamanio));
    }

    @Operation(summary = "Asignar tecnico, estado y observaciones (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PatchMapping("/solicitudes/{id}")
    public SolicitudResponse gestionar(@PathVariable Integer id,
                                       @Valid @RequestBody SolicitudGestionRequest request) {
        return soporteService.gestionarSolicitud(id, request);
    }

    @Operation(summary = "Mi conversacion de chat (cliente)")
    @GetMapping("/chat/mi-conversacion")
    public List<MensajeChatResponse> miConversacion() {
        return soporteService.conversacionPropia();
    }

    @Operation(summary = "Listar clientes para abrir su chat (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @GetMapping("/chat/clientes")
    public List<Map<String, Object>> clientesChat() {
        return conversacionVisitanteService.bandeja();
    }

    @Operation(summary = "Conversacion de un visitante del sitio web (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @GetMapping("/chat/visita/{idConversacion}")
    public List<MensajeVisitanteResponse> conversacionVisita(@PathVariable Integer idConversacion) {
        return conversacionVisitanteService.conversacionVisita(idConversacion);
    }

    @Operation(summary = "Responder a un visitante del sitio web (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/chat/visita/{idConversacion}")
    public MensajeVisitanteResponse responderVisita(@PathVariable Integer idConversacion,
                                                    @Valid @RequestBody MensajeChatRequest request) {
        var usuario = usuarioActual.requerir();
        String token = conversacionVisitanteService.tokenDeVisita(idConversacion);
        if (token == null) {
            throw com.tecnicsecure.exception.ApiException.notFound("Conversacion no encontrada");
        }
        return conversacionVisitanteService.enviarAsesor(token, request.mensaje(), usuario.getIdUsuario());
    }

    @Operation(summary = "Conversacion de un cliente (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @GetMapping("/chat/{idCliente}")
    public List<MensajeChatResponse> conversacion(@PathVariable Integer idCliente) {
        return soporteService.conversacion(idCliente);
    }

    @Operation(summary = "Enviar mensaje en mi conversacion (cliente)")
    @PostMapping("/chat/mi-conversacion")
    public MensajeChatResponse enviarComoCliente(@Valid @RequestBody MensajeChatRequest request) {
        var cliente = clientePropio();
        var usuario = usuarioActual.requerir();
        return soporteService.enviarMensajeCliente(cliente, request.mensaje(), usuario.getIdUsuario(), true);
    }

    @Operation(summary = "Responder en la conversacion de un cliente (administrador o empleado)")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/chat/{idCliente}")
    public MensajeChatResponse responderChat(@PathVariable Integer idCliente,
                                             @Valid @RequestBody MensajeChatRequest request) {
        var usuario = usuarioActual.requerir();
        return soporteService.enviarMensajeCliente(idCliente, request.mensaje(), usuario.getIdUsuario(), false);
    }

    @Operation(summary = "Mis notificaciones")
    @GetMapping("/notificaciones")
    public List<NotificacionResponse> notificaciones() {
        return soporteService.misNotificaciones();
    }

    @Operation(summary = "Cantidad de notificaciones sin leer")
    @GetMapping("/notificaciones/sin-leer")
    public Map<String, Long> sinLeer() {
        return Map.of("total", soporteService.contarNotificacionesSinLeer());
    }

    @Operation(summary = "Marcar todas mis notificaciones como leidas")
    @PatchMapping("/notificaciones/leidas")
    public Map<String, Boolean> marcarLeidas() {
        soporteService.marcarNotificacionesLeidas();
        return Map.of("ok", true);
    }

    private Integer clientePropio() {
        return soporteService.clienteIdDelUsuario(usuarioActual.requerir().getIdUsuario());
    }

    private PageRequest pagina(int pagina, int tamanio) {
        return PageRequest.of(Math.max(0, pagina), Math.min(100, Math.max(1, tamanio)),
                Sort.by(Sort.Direction.DESC, "creadoEn"));
    }
}
