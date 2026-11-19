package com.tecnicsecure.service;

import com.tecnicsecure.dto.soporte.ConversacionVisitanteResponse;
import com.tecnicsecure.dto.soporte.MensajeVisitanteRequest;
import com.tecnicsecure.dto.soporte.MensajeVisitanteResponse;

import java.util.List;

/**
 * Hilo de conversacion del asistente publico para visitantes sin cuenta.
 *
 * El visitante nunca se autentica: recibe un token opaco que guarda su
 * navegador. Con ese token recupera el hilo y ve las respuestas que el
 * administrador o un empleado escriben desde el panel.
 */
public interface ConversacionVisitanteService {

    /** Devuelve el hilo del token o null si no existe. */
    ConversacionVisitanteResponse obtener(String token);

    /** Mensajes del hilo, marcando como leidos los que venian del asesor. */
    List<MensajeVisitanteResponse> mensajes(String token);

    /** Agrega el mensaje del visitante y devuelve el hilo actualizado. */
    ConversacionVisitanteResponse enviarVisitante(MensajeVisitanteRequest peticion);

    /** Registra un mensaje del bot o del asesor dentro del hilo. */
    MensajeVisitanteResponse enviarAsesor(String token, String mensaje, Integer idUsuario);

    /** Guarda una respuesta automatica del asistente (no la escribio una persona). */
    MensajeVisitanteResponse enviarBot(String token, String mensaje);

    /** Bandeja unificada: clientes registrados + visitantes sin cuenta. */
    List<java.util.Map<String, Object>> bandeja();

    List<MensajeVisitanteResponse> conversacionVisita(Integer idConversacion);

    String tokenDeVisita(Integer idConversacion);
}
