package com.tecnicsecure.service;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.soporte.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SoporteService {

    ContactoResponse registrarContacto(ContactoRequest request);

    PageResponse<ContactoResponse> listarContactos(Boolean soloNoLeidos, Pageable pageable);

    ContactoResponse responderContacto(Integer idContacto, String respuesta);

    SolicitudResponse crearSolicitud(SolicitudRequest request);

    PageResponse<SolicitudResponse> listarSolicitudes(String estado, Pageable pageable);

    SolicitudResponse gestionarSolicitud(Integer idSolicitud, SolicitudGestionRequest request);

    List<MensajeChatResponse> conversacion(Integer idCliente);

    /** Clientes con los que el personal puede abrir una conversacion. */
    List<java.util.Map<String, Object>> listarClientesChat();

    List<MensajeChatResponse> conversacionPropia();

    /** Id del cliente asociado a un usuario (para el chat). */
    Integer clienteIdDelUsuario(Integer idUsuario);

    MensajeChatResponse enviarMensajeCliente(Integer idCliente, String mensaje, Integer idUsuario, boolean esCliente);

    List<NotificacionResponse> misNotificaciones();

    long contarNotificacionesSinLeer();

    void marcarNotificacionesLeidas();

    void notificar(Integer idUsuario, String titulo, String mensaje, String enlace);

    /** Avisa a todos los administradores y empleados (misma ruta relativa en cada panel). */
    void notificarEquipo(String titulo, String mensaje, String rutaRelativaPanel);
}
