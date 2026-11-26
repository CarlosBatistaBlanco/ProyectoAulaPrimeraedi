/*
 * Este archivo define como funcionan las conversaciones del chat publico para personas que
 * visitan la tienda sin tener cuenta. El navegador guarda un codigo secreto del chat para que
 * el visitante no pierda sus mensajes y vea en vivo las respuestas de los asesores en el panel.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.soporte.ConversacionVisitanteResponse;
import com.tecnicsecure.dto.soporte.MensajeVisitanteRequest;
import com.tecnicsecure.dto.soporte.MensajeVisitanteResponse;

import java.util.List;

public interface ConversacionVisitanteService {

    // Busca la conversacion del visitante usando su codigo guardado, o avisa si aun no existe.
    ConversacionVisitanteResponse obtener(String token);

    // Trae los mensajes del visitante y marca como leidos los que le respondio el equipo.
    List<MensajeVisitanteResponse> mensajes(String token);

    // Guarda un mensaje nuevo escrito por el visitante y actualiza su conversacion.
    ConversacionVisitanteResponse enviarVisitante(MensajeVisitanteRequest peticion);

    // Guarda una respuesta escrita por un asesor o administrador dentro del chat del visitante.
    MensajeVisitanteResponse enviarAsesor(String token, String mensaje, Integer idUsuario);

    // Guarda una respuesta automatica del asistente virtual para dejar registro en el historial.
    MensajeVisitanteResponse enviarBot(String token, String mensaje);

    // Reune en una sola bandeja de soporte a los clientes con cuenta y a los visitantes sin cuenta.
    List<java.util.Map<String, Object>> bandeja();

    // Muestra todos los mensajes de un visitante dentro del panel del administrador o empleado.
    List<MensajeVisitanteResponse> conversacionVisita(Integer idConversacion);

    // Busca el codigo interno de una conversacion de visitante para poder enviarle una respuesta.
    String tokenDeVisita(Integer idConversacion);
}
