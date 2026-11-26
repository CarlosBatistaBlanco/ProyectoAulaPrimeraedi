/*
 * Este archivo atiende la burbuja de chat flotante que ven los visitantes en la página
 * pública de la tienda. Responde preguntas automáticas y, cuando la persona necesita
 * hablar con un asesor humano, guarda sus mensajes y le muestra las respuestas sin pedir registro.
 */
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

    // Conecta con los encargados de responder dudas automáticas y llevar las charlas con asesores
    public AsistenteController(AsistentePublicoService asistente,
                               ConversacionVisitanteService conversaciones) {
        this.asistente = asistente;
        this.conversaciones = conversaciones;
    }

    // Recibe lo que escribe el visitante en el chat flotante y devuelve la respuesta automática
    @Operation(summary = "Conversar con el asistente virtual (publico)")
    @PostMapping
    public AsistenteResponse conversar(@Valid @RequestBody AsistenteRequest peticion) {
        return asistente.conversar(peticion);
    }

    // Permite al visitante ver el estado de su charla con el asesor usando su código privado de conversación
    @Operation(summary = "Hilo de la conversacion del visitante (publico)")
    @GetMapping("/conversacion/{token}")
    public ResponseEntity<ConversacionVisitanteResponse> conversacion(@PathVariable String token) {
        // Busca la charla con la clave única del visitante; si no existe, avisa que no se encontró
        ConversacionVisitanteResponse hilo = conversaciones.obtener(token);
        return hilo != null ? ResponseEntity.ok(hilo) : ResponseEntity.notFound().build();
    }

    // Entrega todos los mensajes enviados y recibidos en la conversación del visitante
    @Operation(summary = "Mensajes del hilo del visitante (publico)")
    @GetMapping("/conversacion/{token}/mensajes")
    public List<MensajeVisitanteResponse> mensajes(@PathVariable String token) {
        return conversaciones.mensajes(token);
    }

    // Guarda un nuevo mensaje del visitante para que aparezca en la bandeja del asesor
    @Operation(summary = "El visitante continua la conversacion con el asesor (publico)")
    @PostMapping("/mensajes")
    public ConversacionVisitanteResponse enviarMensaje(@Valid @RequestBody MensajeVisitanteRequest peticion) {
        return conversaciones.enviarVisitante(peticion);
    }
}
