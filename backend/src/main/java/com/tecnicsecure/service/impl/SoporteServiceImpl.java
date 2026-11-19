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

    @Autowired(required = false)
    public void setBotAsistenteService(BotAsistenteService botAsistenteService) {
        this.botAsistenteService = botAsistenteService;
    }

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactoResponse> listarContactos(Boolean soloNoLeidos, Pageable pageable) {
        return Boolean.TRUE.equals(soloNoLeidos)
                ? PageResponse.de(contactoRepository.findByLeidoFalseOrderByCreadoEnDesc(pageable), ContactoResponse::de)
                : PageResponse.de(contactoRepository.findAll(pageable), ContactoResponse::de);
    }

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

    @Override
    @Transactional
    public SolicitudResponse crearSolicitud(SolicitudRequest request) {
        Servicio servicio = servicioRepository.findById(request.idServicio())
                .orElseThrow(() -> ApiException.badRequest("El servicio indicado no existe"));

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SolicitudResponse> listarSolicitudes(String estado, Pageable pageable) {
        return StringUtils.hasText(estado)
                ? PageResponse.de(solicitudRepository.findByEstadoOrderByCreadoEnDesc(estado, pageable), SolicitudResponse::de)
                : PageResponse.de(solicitudRepository.findAll(pageable), SolicitudResponse::de);
    }

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

    @Override
    @Transactional
    public List<MensajeChatResponse> conversacion(Integer idCliente) {
        marcarLeidosCliente(idCliente, "cliente");
        return chatRepository.findByIdClienteOrderByCreadoEnAsc(idCliente)
                .stream().map(MensajeChatResponse::de).toList();
    }

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

    @Override
    @Transactional(readOnly = true)
    public Integer clienteIdDelUsuario(Integer idUsuario) {
        return clienteRepository.findByUsuarioIdUsuario(idUsuario)
                .map(Cliente::getIdCliente)
                .orElseThrow(() -> ApiException.notFound("No tienes un perfil de cliente asociado"));
    }

    @Override
    @Transactional
    public List<MensajeChatResponse> conversacionPropia() {
        Cliente cliente = clienteDelUsuarioActual();
        return conversacion(cliente.getIdCliente());
    }

    @Override
    @Transactional
    public MensajeChatResponse enviarMensajeCliente(Integer idCliente, String mensaje,
                                                    Integer idUsuario, boolean esCliente) {
        if (!StringUtils.hasText(mensaje)) {
            throw ApiException.badRequest("El mensaje no puede estar vacío");
        }
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> ApiException.notFound("Cliente no encontrado"));

        boolean yaEsperabaAsesor = esCliente
                && estaEsperandoAsesorHumano(chatRepository.findByIdClienteOrderByCreadoEnAsc(idCliente));

        MensajeChat chat = new MensajeChat();
        chat.setIdCliente(idCliente);
        chat.setRemitente(esCliente ? "cliente" : "asesor");
        chat.setIdUsuarioEnvia(idUsuario);
        chat.setMensaje(mensaje.trim());
        chat.setLeido(false);
        chatRepository.save(chat);

        MensajeChatResponse respuesta = MensajeChatResponse.de(chat);
        mensajeria.convertAndSend("/topic/chat/" + idCliente, respuesta);

        if (esCliente) {
            boolean pideAsesorAhora = botAsistenteService != null && botAsistenteService.esSolicitudAsesor(chat.getMensaje());
            String tituloNoti = (pideAsesorAhora || yaEsperabaAsesor)
                    ? "Cliente solicita asesor en vivo"
                    : "Nuevo mensaje de chat";
            String cuerpoNoti = (cliente.getNombre() != null ? cliente.getNombre() : "Cliente #" + idCliente)
                    + ": " + chat.getMensaje();
            notificarEquipoSoporte(tituloNoti, cuerpoNoti, "chat.html?cliente=" + idCliente);

            // Si el cliente YA solicitó hablar con asesor humano y aún no ha respondido un asesor real,
            // el bot no interrumpe los mensajes posteriores para permitir la atención directa.
            if (botAsistenteService != null && !yaEsperabaAsesor) {
                botAsistenteService.responderAutomatico(idCliente, chat.getMensaje());
            }
        } else {
            if (cliente.getUsuario() != null) {
                notificar(cliente.getUsuario().getIdUsuario(), "Nueva respuesta del asesor",
                        chat.getMensaje(), "/panel/cliente/chat.html");
            }
        }

        return respuesta;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> misNotificaciones() {
        Usuario usuario = usuarioActual.requerir();
        return notificacionRepository.findTop30ByIdUsuarioOrderByCreadoEnDesc(usuario.getIdUsuario())
                .stream().map(NotificacionResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNotificacionesSinLeer() {
        Usuario usuario = usuarioActual.requerir();
        return notificacionRepository.countByIdUsuarioAndLeidaFalse(usuario.getIdUsuario());
    }

    @Override
    @Transactional
    public void marcarNotificacionesLeidas() {
        Usuario usuario = usuarioActual.requerir();
        List<Notificacion> pendientes =
                notificacionRepository.findTop30ByIdUsuarioOrderByCreadoEnDesc(usuario.getIdUsuario());
        pendientes.stream().filter(n -> !Boolean.TRUE.equals(n.getLeida())).forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

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

    @Override
    public void notificarEquipo(String titulo, String mensaje, String rutaRelativaPanel) {
        notificarEquipoSoporte(titulo, mensaje, rutaRelativaPanel);
    }

    private void notificarEquipoSoporte(String titulo, String mensaje, String rutaRelativaPanel) {        usuarioRepository.findByRolNombre(Rol.ADMINISTRADOR)
                .forEach(u -> notificar(u.getIdUsuario(), titulo, mensaje, "/panel/administrador/" + rutaRelativaPanel));
        usuarioRepository.findByRolNombre(Rol.EMPLEADO)
                .forEach(u -> notificar(u.getIdUsuario(), titulo, mensaje, "/panel/empleado/" + rutaRelativaPanel));
    }

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

    private boolean esTextoSolicitudAsesor(String msg) {
        if (botAsistenteService != null) {
            return botAsistenteService.esSolicitudAsesor(msg);
        }
        if (msg == null) return false;
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("asesor") || lower.contains("humano") || lower.contains("operador");
    }

    private Cliente clienteDelUsuarioActual() {
        Usuario usuario = usuarioActual.requerir();
        return clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> ApiException.notFound("No tienes un perfil de cliente asociado"));
    }

    private void marcarLeidosCliente(Integer idCliente, String remitente) {
        var pendientes = chatRepository.findByIdClienteOrderByCreadoEnAsc(idCliente).stream()
                .filter(m -> remitente.equals(m.getRemitente()) && !Boolean.TRUE.equals(m.getLeido()))
                .toList();
        pendientes.forEach(m -> m.setLeido(true));
        chatRepository.saveAll(pendientes);
    }
}
