/*
 * Este archivo define las acciones de atencion al cliente y comunicacion interna: mensajes del
 * formulario de contacto, solicitudes de cotizacion o visita tecnica, chat en vivo y avisos de
 * la campanita. Su trabajo se ve en la pagina de contacto, en el chat y en el panel de soporte.
 */
package com.tecnicsecure.service;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.soporte.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SoporteService {

    // Guarda un mensaje enviado desde el formulario de contacto de la web y avisa al equipo.
    ContactoResponse registrarContacto(ContactoRequest request);

    // Muestra los mensajes de contacto recibidos, con opcion de ver solo los que faltan por leer.
    PageResponse<ContactoResponse> listarContactos(Boolean soloNoLeidos, Pageable pageable);

    // Guarda la respuesta del equipo a un mensaje de contacto y lo marca como atendido.
    ContactoResponse responderContacto(Integer idContacto, String respuesta);

    // Crea una solicitud de cotizacion o cita tecnica para un servicio de la empresa.
    SolicitudResponse crearSolicitud(SolicitudRequest request);

    // Muestra las solicitudes de servicio con opcion de filtrar por su estado actual.
    PageResponse<SolicitudResponse> listarSolicitudes(String estado, Pageable pageable);

    // Asigna un tecnico, agrega notas o cambia el estado de una solicitud de servicio.
    SolicitudResponse gestionarSolicitud(Integer idSolicitud, SolicitudGestionRequest request);

    // Trae todo el historial de mensajes del chat de un cliente especifico.
    List<MensajeChatResponse> conversacion(Integer idCliente);

    // Entrega la lista de clientes con los que el equipo puede abrir o continuar un chat.
    List<java.util.Map<String, Object>> listarClientesChat();

    // Muestra la conversacion de chat del cliente que tiene la sesion iniciada.
    List<MensajeChatResponse> conversacionPropia();

    // Encuentra el numero de ficha de cliente que corresponde a una cuenta de usuario para el chat.
    Integer clienteIdDelUsuario(Integer idUsuario);

    // Envia un mensaje de chat entre un cliente y un asesor, mostrandolo al instante en pantalla.
    MensajeChatResponse enviarMensajeCliente(Integer idCliente, String mensaje, Integer idUsuario, boolean esCliente);

    // Muestra los avisos recientes de la campanita de la persona conectada.
    List<NotificacionResponse> misNotificaciones();

    // Cuenta cuantos avisos sin leer tiene la persona conectada para mostrar el globito rojo.
    long contarNotificacionesSinLeer();

    // Marca como leidos todos los avisos pendientes de la persona conectada.
    void marcarNotificacionesLeidas();

    // Crea un aviso para una persona especifica y lo hace aparecer en su pantalla de inmediato.
    void notificar(Integer idUsuario, String titulo, String mensaje, String enlace);

    // Envia un mismo aviso a todos los administradores y empleados activos del equipo.
    void notificarEquipo(String titulo, String mensaje, String rutaRelativaPanel);
}
