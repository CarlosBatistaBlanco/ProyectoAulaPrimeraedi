package com.tecnicsecure.controller;

import com.tecnicsecure.dto.soporte.AsistenteRequest;
import com.tecnicsecure.dto.soporte.AsistenteResponse;
import com.tecnicsecure.dto.soporte.ConversacionVisitanteResponse;
import com.tecnicsecure.dto.soporte.MensajeVisitanteRequest;
import com.tecnicsecure.dto.soporte.MensajeVisitanteResponse;
import com.tecnicsecure.service.AsistentePublicoService;
import com.tecnicsecure.service.ConversacionVisitanteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "11. Asistente virtual", description = "Chat automatico del sitio publico con escalado a un asesor")
@RestController
@RequestMapping("/api/asistente")
public class AsistenteController {

    private final AsistentePublicoService asistente;
    private final ConversacionVisitanteService conversaciones;

    public AsistenteController(AsistentePublicoService asistente,
                               ConversacionVisitanteService conversaciones) {
        this.asistente = asistente;
        this.conversaciones = conversaciones;
    }

    @Operation(summary = "Conversar con el asistente virtual (publico)")
    @PostMapping
    public AsistenteResponse conversar(@Valid @RequestBody AsistenteRequest peticion) {
        return asistente.conversar(peticion);
    }

    /**
     * El visitante consulta su hilo (incluye lo que respondio el asesor).
     * Es publico a proposito: el token es aleatorio y no revela otros hilos.
     */
    @Operation(summary = "Hilo de la conversacion del visitante (publico)")
    @GetMapping("/conversacion/{token}")
    public ResponseEntity<ConversacionVisitanteResponse> conversacion(@PathVariable String token) {
        ConversacionVisitanteResponse hilo = conversaciones.obtener(token);
        return hilo != null ? ResponseEntity.ok(hilo) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Mensajes del hilo del visitante (publico)")
    @GetMapping("/conversacion/{token}/mensajes")
    public List<MensajeVisitanteResponse> mensajes(@PathVariable String token) {
        return conversaciones.mensajes(token);
    }

    @Operation(summary = "El visitante continua la conversacion con el asesor (publico)")
    @PostMapping("/mensajes")
    public ConversacionVisitanteResponse enviarMensaje(@Valid @RequestBody MensajeVisitanteRequest peticion) {
        return conversaciones.enviarVisitante(peticion);
    }
}
