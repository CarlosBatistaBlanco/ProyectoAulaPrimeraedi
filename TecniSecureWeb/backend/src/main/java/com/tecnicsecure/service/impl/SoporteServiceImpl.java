/*
 * Este archivo ejecuta todas las tareas de atencion al cliente y avisos en vivo de la tienda:
 * recibe mensajes del formulario de contacto, agenda citas y cotizaciones tecnicas, maneja el
 * chat privado de los clientes (incluyendo el paso de asistente automatico a asesor humano) y
 * envia las alertas de la campanita en los paneles de clientes, empleados y administradores.
 */
package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.dto.soporte.*;
import com.tecnicsecure.entity.*;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.BotAsistenteService;
import com.tecnicsecure.service.SoporteService;
import com.tecnicsecure.util.UsuarioActual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SoporteServiceImpl implements SoporteService {

    private final ContactoRepository contactoRepository;
    private final SolicitudServicioRepository solicitudRepository;
    private final ServicioRepository servicioRepository;
    private final MensajeChatRepository chatRepository;
    private final NotificacionRepository notificacionRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioActual usuarioActual;
    private final SimpMessagingTemplate mensajeria;
    private BotAsistenteService botAsistenteService;

    // Prepara el acceso a mensajes de contacto, solicitudes de servicio, chat, campanita de avisos y envios en vivo.
    public SoporteServiceImpl(ContactoRepository contactoRepository,
                              SolicitudServicioRepository solicitudRepository,
                              ServicioRepository servicioRepository,
                              MensajeChatRepository chatRepository,
                              NotificacionRepository notificacionRepository,
                              ClienteRepository clienteRepository,
                              UsuarioRepository usuarioRepository,
                              UsuarioActual usuarioActual,
                              SimpMessagingTemplate mensajeria) {
        this.contactoRepository = contactoRepository;
        this.solicitudRepository = solicitudRepository;
        this.servicioRepository = servicioRepository;
        this.chatRepository = chatRepository;
        this.notificacionRepository = notificacionRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioActual = usuarioActual;
        this.mensajeria = mensajeria;
    }

    // Conecta el asistente automatico del chat cuando esta disponible en la tienda.
    @Autowired(required = false)
    public void setBotAsistenteService(BotAsistenteService botAsistenteService) {
        this.botAsistenteService = botAsistenteService;
    }

    // Guarda el mensaje enviado desde el formulario de Contacto del sitio web y manda una alerta al equipo.
    @Override
    @Transactional
    public ContactoResponse registrarContacto(ContactoRequest request) {
        Contacto contacto = new Contacto();
        contacto.setNombre(request.nombre().trim());
        contacto.setCorreo(request.correo().trim().toLowerCase());
        contacto.setTelefono(request.telefono());
        contacto.setAsunto(request.asunto());
        contacto.setTipo(StringUtils.hasText(request.tipo()) ? request.tipo() : "mensaje");
        contacto.setMensaje(request.mensaje().trim());
        contacto.setIdUsuario(usuarioActual.idOpcional());
        contacto.setLeido(false);
        contactoRepository.save(contacto);

        notificarEquipoSoporte("Nuevo mensaje de contacto",
                contacto.getNombre() + ": " + contacto.getAsunto(),
                "soporte.html");

        return ContactoResponse.de(contacto);
    }

    // Muestra los mensajes recibidos en Contacto, con opcion de ver solo los que aun no se han leido.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactoResponse> listarContactos(Boolean soloNoLeidos, Pageable pageable) {
        return Boolean.TRUE.equals(soloNoLeidos)
                ? PageResponse.de(contactoRepository.findByLeidoFalseOrderByCreadoEnDesc(pageable), ContactoResponse::de)
                : PageResponse.de(contactoRepository.findAll(pageable), ContactoResponse::de);
    }

    // Guarda la respuesta escrita por el equipo a un mensaje de contacto y lo marca como leido.
    @Override
    @Transactional
    public ContactoResponse responderContacto(Integer idContacto, String respuesta) {
        Contacto contacto = contactoRepository.findById(idContacto)
                .orElseThrow(() -> ApiException.notFound("Mensaje no encontrado"));
        contacto.setRespuesta(respuesta);
        contacto.setLeido(true);
        contactoRepository.save(contacto);
        return ContactoResponse.de(contacto);
    }

    // Registra una nueva solicitud de cotizacion o cita tecnica comprobando los datos requeridos y avisando al equipo.
    @Override
    @Transactional
    public SolicitudResponse crearSolicitud(SolicitudRequest request) {
        Servicio servicio = servicioRepository.findById(request.idServicio())
                .orElseThrow(() -> ApiException.badRequest("El servicio indicado no existe"));

        // Verifica que el pedido sea de tipo cotizacion o cita; si es cita exige escoger fecha.
        String tipo = request.tipo() == null ? "" : request.tipo().trim().toLowerCase();
        if (!tipo.equals("cotizacion") && !tipo.equals("cita")) {
            throw ApiException.badRequest("El tipo debe ser 'cotizacion' o 'cita'");
        }
        if (tipo.equals("cita") && request.fechaPreferida() == null) {
            throw ApiException.badRequest("Para agendar una cita indica la fecha preferida");
        }

        SolicitudServicio solicitud = new SolicitudServicio();
        solicitud.setServicio(servicio);
        solicitud.setNombre(request.nombre().trim());
        solicitud.setCorreo(request.correo().trim().toLowerCase());
        solicitud.setTelefono(request.telefono().trim());
        solicitud.setDireccion(request.direccion());
        solicitud.setTipo(tipo);
        solicitud.setFechaPreferida(request.fechaPreferida());
        solicitud.setHoraPreferida(request.horaPreferida());
        solicitud.setDescripcion(request.descripcion());
        solicitud.setEstado("pendiente");
        solicitudRepository.save(solicitud);

        notificarEquipoSoporte("Nueva solicitud de servicio",
                solicitud.getNombre() + " - " + servicio.getTitulo(),
                "soporte.html");

        return SolicitudResponse.de(solicitud);
    }

    // Muestra las solicitudes de cotizacion y citas tecnicas, permitiendo filtrar por estado (pendiente, en proceso, etc.).
    @Override
    @Transactional(readOnly = true)
    public PageResponse<SolicitudResponse> listarSolicitudes(String estado, Pageable pageable) {
        return StringUtils.hasText(estado)
                ? PageResponse.de(solicitudRepository.findByEstadoOrderByCreadoEnDesc(estado, pageable), SolicitudResponse::de)
                : PageResponse.de(solicitudRepository.findAll(pageable), SolicitudResponse::de);
    }

    // Deja que el equipo asigne un tecnico, agregue notas de seguimiento y cambie el estado de la solicitud.
    @Override
    @Transactional
    public SolicitudResponse gestionarSolicitud(Integer idSolicitud, SolicitudGestionRequest request) {
        SolicitudServicio solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));

        if (request.tecnicoAsignado() != null) {
            solicitud.setTecnicoAsignado(request.tecnicoAsignado());
        }
        if (request.observaciones() != null) {
            solicitud.setObservaciones(request.observaciones());
        }
        if (StringUtils.hasText(request.estado())) {
            solicitud.setEstado(request.estado());
        }
        solicitudRepository.save(solicitud);
        return SolicitudResponse.de(solicitud);
    }

    // Abre el historial de mensajes de un cliente y marca como leido lo que ese cliente habia escrito.
    @Override
    @Transactional
    public List<MensajeChatResponse> conversacion(Integer idCliente) {
        marcarLeidosCliente(idCliente, "cliente");
        return chatRepository.findByIdClienteOrderByCreadoEnAsc(idCliente)
                .stream().map(MensajeChatResponse::de).toList();
    }

    // Arma la lista de conversaciones de clientes registrados poniendo arriba a quienes piden asesor humano o tienen mensajes sin leer.
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarClientesChat() {
        List<Cliente> clientes = clienteRepository.findAll();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Cliente c : clientes) {
            List<MensajeChat> mensajes = chatRepository.findByIdClienteOrderByCreadoEnAsc(c.getIdCliente());
            MensajeChat ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
            long sinLeer = mensajes.stream()
                    .filter(m -> "cliente".equals(m.getRemitente()) && !Boolean.TRUE.equals(m.getLeido()))
                    .count();
            boolean solicitaAsesor = estaEsperandoAsesorHumano(mensajes);

            String preview = "";
            if (ultimo != null && ultimo.getMensaje() != null) {
                String txt = ultimo.getMensaje().replace("\n", " ").trim();
                preview = txt.length() > 80 ? txt.substring(0, 77) + "..." : txt;
            }

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idCliente", c.getIdCliente());
            fila.put("nombre", c.getNombre() == null ? "" : c.getNombre());
            fila.put("correo", c.getCorreo() == null ? "" : c.getCorreo());
            fila.put("totalMensajes", mensajes.size());
            fila.put("ultimoMensaje", preview);
            fila.put("remitenteUltimo", ultimo != null ? ultimo.getRemitente() : "");
            fila.put("creadoEnUltimo", ultimo != null ? ultimo.getCreadoEn() : null);
            fila.put("sinLeer", sinLeer);
            fila.put("solicitaAsesor", solicitaAsesor);
            resultado.add(fila);
        }

        // Ordena primero a quienes esperan asesor humano, luego los chats con mensajes sin leer y despues los mas recientes.
        resultado.sort((a, b) -> {
            int cmpSol = Boolean.compare(Boolean.TRUE.equals(b.get("solicitaAsesor")),
                    Boolean.TRUE.equals(a.get("solicitaAsesor")));
            if (cmpSol != 0) return cmpSol;

            long slA = ((Number) a.get("sinLeer")).longValue();
            long slB = ((Number) b.get("sinLeer")).longValue();
            int cmpLeido = Long.compare(slB > 0 ? 1 : 0, slA > 0 ? 1 : 0);
            if (cmpLeido != 0) return cmpLeido;

            LocalDateTime fA = (LocalDateTime) a.get("creadoEnUltimo");
            LocalDateTime fB = (LocalDateTime) b.get("creadoEnUltimo");
            if (fA == null && fB == null) return 0;
            if (fA == null) return 1;
            if (fB == null) return -1;
            return fB.compareTo(fA);
        });

        return resultado;
    }

    // Encuentra el numero de ficha de cliente que pertenece a una cuenta de usuario.
    @Override
    @Transactional(readOnly = true)
    public Integer clienteIdDelUsuario(Integer idUsuario) {
        return clienteRepository.findByUsuarioIdUsuario(idUsuario)
                .map(Cliente::getIdCliente)
                .orElseThrow(() -> ApiException.notFound("No tienes un perfil de cliente asociado"));
    }

    // Muestra unicamente los mensajes del chat pertenecientes al cliente que tiene la sesion abierta.
    @Override
    @Transactional
    public List<MensajeChatResponse> conversacionPropia() {
        Cliente cliente = clienteDelUsuarioActual();
        return conversacion(cliente.getIdCliente());
    }

    // Guarda un mensaje del chat (de cliente o asesor), lo muestra en vivo en pantalla, envia alertas y activa la respuesta automatica si corresponde.
    @Override
    @Transactional
    public MensajeChatResponse enviarMensajeCliente(Integer idCliente, String mensaje,
                                                    Integer idUsuario, boolean esCliente) {
        if (!StringUtils.hasText(mensaje)) {
            throw ApiException.badRequest("El mensaje no puede estar vacío");
        }
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> ApiException.notFound("Cliente no encontrado"));

        // Comprueba si el cliente ya estaba esperando a una persona real antes de enviar este nuevo mensaje.
        boolean yaEsperabaAsesor = esCliente
                && estaEsperandoAsesorHumano(chatRepository.findByIdClienteOrderByCreadoEnAsc(idCliente));

        MensajeChat chat = new MensajeChat();
        chat.setIdCliente(idCliente);
        chat.setRemitente(esCliente ? "cliente" : "asesor");
        chat.setIdUsuarioEnvia(idUsuario);
        chat.setMensaje(mensaje.trim());
        chat.setLeido(false);
        chatRepository.save(chat);

        // Muestra el mensaje al instante en la ventana de chat abierta sin tener que recargar la pagina.
        MensajeChatResponse respuesta = MensajeChatResponse.de(chat);
        mensajeria.convertAndSend("/topic/chat/" + idCliente, respuesta);

        // Si escribio el cliente, avisa al equipo (destacando si pidio asesor) y contesta con el asistente automatico cuando aplica.
        if (esCliente) {
            boolean pideAsesorAhora = botAsistenteService != null && botAsistenteService.esSolicitudAsesor(chat.getMensaje());
            String tituloNoti = (pideAsesorAhora || yaEsperabaAsesor)
                    ? "Cliente solicita asesor en vivo"
                    : "Nuevo mensaje de chat";
            String cuerpoNoti = (cliente.getNombre() != null ? cliente.getNombre() : "Cliente #" + idCliente)
                    + ": " + chat.getMensaje();
            notificarEquipoSoporte(tituloNoti, cuerpoNoti, "chat.html?cliente=" + idCliente);

            // Si el cliente ya pidio hablar con una persona y aun espera respuesta, el asistente guarda silencio para no interrumpir.
            if (botAsistenteService != null && !yaEsperabaAsesor) {
                botAsistenteService.responderAutomatico(idCliente, chat.getMensaje());
            }
        } else {
            // Si quien contesto fue un asesor del equipo, manda un aviso en vivo a la campanita del cliente.
            if (cliente.getUsuario() != null) {
                notificar(cliente.getUsuario().getIdUsuario(), "Nueva respuesta del asesor",
                        chat.getMensaje(), "/panel/cliente/chat.html");
            }
        }

        return respuesta;
    }

    // Muestra hasta los 30 avisos mas recientes de la campanita del usuario conectado.
    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> misNotificaciones() {
        Usuario usuario = usuarioActual.requerir();
        return notificacionRepository.findTop30ByIdUsuarioOrderByCreadoEnDesc(usuario.getIdUsuario())
                .stream().map(NotificacionResponse::de).toList();
    }

    // Cuenta cuantos avisos sin leer tiene la persona conectada para mostrar el numero en la campanita.
    @Override
    @Transactional(readOnly = true)
    public long contarNotificacionesSinLeer() {
        Usuario usuario = usuarioActual.requerir();
        return notificacionRepository.countByIdUsuarioAndLeidaFalse(usuario.getIdUsuario());
    }

    // Marca como leidos los avisos recientes de la campanita del usuario conectado.
    @Override
    @Transactional
    public void marcarNotificacionesLeidas() {
        Usuario usuario = usuarioActual.requerir();
        List<Notificacion> pendientes =
                notificacionRepository.findTop30ByIdUsuarioOrderByCreadoEnDesc(usuario.getIdUsuario());
        pendientes.stream().filter(n -> !Boolean.TRUE.equals(n.getLeida())).forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

    // Guarda un aviso para una persona especifica y lo hace aparecer al instante en su pantalla.
    @Override
    @Transactional
    public void notificar(Integer idUsuario, String titulo, String mensaje, String enlace) {
        if (idUsuario == null) {
            return;
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setIdUsuario(idUsuario);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setEnlace(enlace);
        notificacion.setLeida(false);
        notificacionRepository.save(notificacion);

        mensajeria.convertAndSend("/topic/notificaciones/" + idUsuario, NotificacionResponse.de(notificacion));
    }

    // Envia un aviso al mismo tiempo a todos los administradores y empleados de la empresa.
    @Override
    public void notificarEquipo(String titulo, String mensaje, String rutaRelativaPanel) {
        notificarEquipoSoporte(titulo, mensaje, rutaRelativaPanel);
    }

    // Reparte el aviso entre administradores y empleados abriendo a cada uno la pantalla que corresponde a su panel.
    private void notificarEquipoSoporte(String titulo, String mensaje, String rutaRelativaPanel) {        usuarioRepository.findByRolNombre(Rol.ADMINISTRADOR)
                .forEach(u -> notificar(u.getIdUsuario(), titulo, mensaje, "/panel/administrador/" + rutaRelativaPanel));
        usuarioRepository.findByRolNombre(Rol.EMPLEADO)
                .forEach(u -> notificar(u.getIdUsuario(), titulo, mensaje, "/panel/empleado/" + rutaRelativaPanel));
    }

    // Revisa si el cliente pidio hablar con una persona real despues de la ultima respuesta de un asesor humano.
    private boolean estaEsperandoAsesorHumano(List<MensajeChat> historial) {
        if (historial == null || historial.isEmpty()) {
            return false;
        }
        int idxUltimoHumano = -1;
        int idxSolicitudAsesor = -1;
        for (int i = 0; i < historial.size(); i++) {
            MensajeChat m = historial.get(i);
            if ("asesor".equals(m.getRemitente()) && m.getIdUsuarioEnvia() != null) {
                idxUltimoHumano = i;
            }
            if ("cliente".equals(m.getRemitente()) && esTextoSolicitudAsesor(m.getMensaje())) {
                idxSolicitudAsesor = i;
            }
        }
        return idxSolicitudAsesor > idxUltimoHumano;
    }

    // Detecta si un mensaje contiene palabras que piden atencion de una persona real.
    private boolean esTextoSolicitudAsesor(String msg) {
        if (botAsistenteService != null) {
            return botAsistenteService.esSolicitudAsesor(msg);
        }
        if (msg == null) return false;
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("asesor") || lower.contains("humano") || lower.contains("operador");
    }

    // Obtiene la ficha comercial de cliente que corresponde a la persona que tiene la sesion iniciada.
    private Cliente clienteDelUsuarioActual() {
        Usuario usuario = usuarioActual.requerir();
        return clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> ApiException.notFound("No tienes un perfil de cliente asociado"));
    }

    // Marca como leidos todos los mensajes pendientes de un remitente dentro de la conversacion de un cliente.
    private void marcarLeidosCliente(Integer idCliente, String remitente) {
        var pendientes = chatRepository.findByIdClienteOrderByCreadoEnAsc(idCliente).stream()
                .filter(m -> remitente.equals(m.getRemitente()) && !Boolean.TRUE.equals(m.getLeido()))
                .toList();
        pendientes.forEach(m -> m.setLeido(true));
        chatRepository.saveAll(pendientes);
    }
}
