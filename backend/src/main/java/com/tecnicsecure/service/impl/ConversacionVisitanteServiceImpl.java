package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.soporte.ConversacionVisitanteResponse;
import com.tecnicsecure.dto.soporte.MensajeVisitanteRequest;
import com.tecnicsecure.dto.soporte.MensajeVisitanteResponse;
import com.tecnicsecure.entity.Cliente;
import com.tecnicsecure.entity.ConversacionVisitante;
import com.tecnicsecure.entity.MensajeChat;
import com.tecnicsecure.entity.MensajeVisitante;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.ClienteRepository;
import com.tecnicsecure.repository.ConversacionVisitanteRepository;
import com.tecnicsecure.repository.MensajeChatRepository;
import com.tecnicsecure.repository.MensajeVisitanteRepository;
import com.tecnicsecure.service.ConversacionVisitanteService;
import com.tecnicsecure.service.SoporteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ConversacionVisitanteServiceImpl implements ConversacionVisitanteService {

    private static final Logger log = LoggerFactory.getLogger(ConversacionVisitanteServiceImpl.class);
    private static final String ALFABETO = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom AZAR = new SecureRandom();

    private final ConversacionVisitanteRepository conversacionRepository;
    private final MensajeVisitanteRepository mensajeRepository;
    private final ClienteRepository clienteRepository;
    private final MensajeChatRepository chatRepository;
    private final SoporteService soporteService;
    private final SimpMessagingTemplate mensajeria;

    public ConversacionVisitanteServiceImpl(ConversacionVisitanteRepository conversacionRepository,
                                            MensajeVisitanteRepository mensajeRepository,
                                            ClienteRepository clienteRepository,
                                            MensajeChatRepository chatRepository,
                                            SoporteService soporteService,
                                            SimpMessagingTemplate mensajeria) {
        this.conversacionRepository = conversacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.clienteRepository = clienteRepository;
        this.chatRepository = chatRepository;
        this.soporteService = soporteService;
        this.mensajeria = mensajeria;
    }

    /* --------------------------- Visitante --------------------------- */

    @Override
    @Transactional(readOnly = true)
    public ConversacionVisitanteResponse obtener(String token) {
        if (!StringUtils.hasText(token)) return null;
        return conversacionRepository.findByToken(token.trim())
                .map(ConversacionVisitanteResponse::de)
                .orElse(null);
    }

    @Override
    @Transactional
    public List<MensajeVisitanteResponse> mensajes(String token) {
        ConversacionVisitante conversacion = buscar(token);
        List<MensajeVisitante> todos =
                mensajeRepository.findByIdConversacionOrderByCreadoEnAsc(conversacion.getIdConversacion());

        // Lo que responde el equipo queda leido para el visitante.
        List<MensajeVisitante> pendientes = todos.stream()
                .filter(m -> "asesor".equals(m.getRemitente()) && !Boolean.TRUE.equals(m.getLeido()))
                .toList();
        if (!pendientes.isEmpty()) {
            pendientes.forEach(m -> m.setLeido(true));
            mensajeRepository.saveAll(pendientes);
        }
        return todos.stream().map(MensajeVisitanteResponse::de).toList();
    }

    @Override
    @Transactional
    public ConversacionVisitanteResponse enviarVisitante(MensajeVisitanteRequest peticion) {
        String texto = peticion.mensaje() == null ? "" : peticion.mensaje().trim();
        if (texto.isEmpty()) {
            throw ApiException.badRequest("El mensaje no puede estar vacio");
        }

        ConversacionVisitante conversacion = crearOContinuar(peticion);

        MensajeVisitante mensaje = new MensajeVisitante();
        mensaje.setIdConversacion(conversacion.getIdConversacion());
        mensaje.setRemitente("visitante");
        mensaje.setMensaje(texto);
        mensaje.setLeido(false);
        mensajeRepository.save(mensaje);

        conversacion.setSinLeer(conversacion.getSinLeer() + 1);
        conversacion.setActualizadoEn(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        avisarEquipo(conversacion, texto);
        publicar("visita", conversacion.getIdConversacion(), MensajeVisitanteResponse.de(mensaje));

        return ConversacionVisitanteResponse.de(conversacion);
    }

    /** Crea el hilo la primera vez y actualiza los datos de contacto si llegan. */
    private ConversacionVisitante crearOContinuar(MensajeVisitanteRequest peticion) {
        ConversacionVisitante conversacion = null;
        if (StringUtils.hasText(peticion.token())) {
            conversacion = conversacionRepository.findByToken(peticion.token().trim()).orElse(null);
        }
        if (conversacion == null) {
            conversacion = new ConversacionVisitante();
            conversacion.setToken(nuevoToken());
            conversacion.setEstado("abierta");
        }
        if (StringUtils.hasText(peticion.nombre())) conversacion.setNombre(peticion.nombre().trim());
        if (StringUtils.hasText(peticion.correo())) conversacion.setCorreo(peticion.correo().trim().toLowerCase());
        if (StringUtils.hasText(peticion.telefono())) conversacion.setTelefono(peticion.telefono().trim());
        conversacion.setActualizadoEn(LocalDateTime.now());
        return conversacionRepository.save(conversacion);
    }

    /* ------------------------------ Equipo --------------------------- */

    @Override
    @Transactional
    public MensajeVisitanteResponse enviarAsesor(String token, String mensaje, Integer idUsuario) {
        if (!StringUtils.hasText(mensaje)) {
            throw ApiException.badRequest("El mensaje no puede estar vacio");
        }
        ConversacionVisitante conversacion = buscar(token);

        MensajeVisitante respuesta = new MensajeVisitante();
        respuesta.setIdConversacion(conversacion.getIdConversacion());
        respuesta.setRemitente("asesor");
        respuesta.setIdUsuarioEnvia(idUsuario);
        respuesta.setMensaje(mensaje.trim());
        respuesta.setLeido(false);
        mensajeRepository.save(respuesta);

        conversacion.setSinLeer(0);
        conversacion.setEstado("atendida");
        conversacion.setActualizadoEn(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        publicar("visita", conversacion.getIdConversacion(), MensajeVisitanteResponse.de(respuesta));
        return MensajeVisitanteResponse.de(respuesta);
    }

    @Override
    @Transactional
    public MensajeVisitanteResponse enviarBot(String token, String mensaje) {
        if (!StringUtils.hasText(mensaje) || !StringUtils.hasText(token)) {
            return null;
        }
        ConversacionVisitante conversacion = conversacionRepository.findByToken(token.trim()).orElse(null);
        if (conversacion == null) {
            return null;
        }

        MensajeVisitante bot = new MensajeVisitante();
        bot.setIdConversacion(conversacion.getIdConversacion());
        bot.setRemitente("bot");
        bot.setMensaje(mensaje.trim());
        bot.setLeido(true);
        mensajeRepository.save(bot);

        conversacion.setActualizadoEn(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        publicar("visita", conversacion.getIdConversacion(), MensajeVisitanteResponse.de(bot));
        return MensajeVisitanteResponse.de(bot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensajeVisitanteResponse> conversacionVisita(Integer idConversacion) {
        if (idConversacion == null) {
            throw ApiException.badRequest("Conversacion no indicada");
        }
        ConversacionVisitante conversacion = conversacionRepository.findById(idConversacion)
                .orElseThrow(() -> ApiException.notFound("Conversacion no encontrada"));
        // Al abrirla desde el panel, el equipo la da por leida.
        return mensajeRepository.findByIdConversacionOrderByCreadoEnAsc(conversacion.getIdConversacion())
                .stream().map(MensajeVisitanteResponse::de).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String tokenDeVisita(Integer idConversacion) {
        return conversacionRepository.findById(idConversacion).map(ConversacionVisitante::getToken).orElse(null);
    }

    /**
     * Bandeja del panel: mezcla los clientes registrados (chat clasico) con los
     * visitantes del sitio. El id de un visitante se devuelve como "cv-<id>"
     * para que el frontend sepa que endpoint usar.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> bandeja() {
        List<Map<String, Object>> filas = new ArrayList<>();

        for (Cliente c : clienteRepository.findAll()) {
            List<MensajeChat> mensajes = chatRepository.findByIdClienteOrderByCreadoEnAsc(c.getIdCliente());
            MensajeChat ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
            long sinLeer = mensajes.stream()
                    .filter(m -> "cliente".equals(m.getRemitente()) && !Boolean.TRUE.equals(m.getLeido()))
                    .count();

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idCliente", c.getIdCliente());
            fila.put("origen", "cliente");
            fila.put("nombre", c.getNombre() == null ? "" : c.getNombre());
            fila.put("correo", c.getCorreo() == null ? "" : c.getCorreo());
            fila.put("totalMensajes", mensajes.size());
            fila.put("ultimoMensaje", recortar(ultimo != null ? ultimo.getMensaje() : null));
            fila.put("remitenteUltimo", ultimo != null ? ultimo.getRemitente() : "");
            fila.put("creadoEnUltimo", ultimo != null ? ultimo.getCreadoEn() : null);
            fila.put("sinLeer", sinLeer);
            fila.put("solicitaAsesor", false);
            filas.add(fila);
        }

        for (ConversacionVisitante v : conversacionRepository.findAllByOrderByActualizadoEnDesc()) {
            List<MensajeVisitante> mensajes =
                    mensajeRepository.findByIdConversacionOrderByCreadoEnAsc(v.getIdConversacion());
            MensajeVisitante ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
            if (ultimo == null) {
                continue; // hilo creado pero sin mensajes: no se muestra
            }

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idCliente", "cv-" + v.getIdConversacion());
            fila.put("origen", "visitante");
            fila.put("nombre", StringUtils.hasText(v.getNombre()) ? v.getNombre() : "Visitante del sitio");
            fila.put("correo", v.getCorreo() == null ? "" : v.getCorreo());
            fila.put("telefono", v.getTelefono() == null ? "" : v.getTelefono());
            fila.put("idSolicitud", v.getIdSolicitud());
            fila.put("totalMensajes", mensajes.size());
            fila.put("ultimoMensaje", recortar(ultimo.getMensaje()));
            fila.put("remitenteUltimo", ultimo.getRemitente());
            fila.put("creadoEnUltimo", ultimo.getCreadoEn());
            fila.put("sinLeer", v.getSinLeer() == null ? 0 : v.getSinLeer());
            // Espera atencion humana mientras el ultimo en escribir haya sido el visitante
            // y un asesor real no haya respondido todavia (el bot no cuenta como atencion).
            boolean hayAsesor = mensajes.stream().anyMatch(m -> "asesor".equals(m.getRemitente())
                    && m.getIdUsuarioEnvia() != null);
            fila.put("solicitaAsesor", "visitante".equals(ultimo.getRemitente()) && !hayAsesor);
            filas.add(fila);
        }

        filas.sort((a, b) -> {
            int sol = Boolean.compare(Boolean.TRUE.equals(b.get("solicitaAsesor")),
                    Boolean.TRUE.equals(a.get("solicitaAsesor")));
            if (sol != 0) return sol;
            long sa = ((Number) a.get("sinLeer")).longValue();
            long sb = ((Number) b.get("sinLeer")).longValue();
            int leido = Long.compare(sb > 0 ? 1 : 0, sa > 0 ? 1 : 0);
            if (leido != 0) return leido;
            LocalDateTime fa = (LocalDateTime) a.get("creadoEnUltimo");
            LocalDateTime fb = (LocalDateTime) b.get("creadoEnUltimo");
            if (fa == null && fb == null) return 0;
            if (fa == null) return 1;
            if (fb == null) return -1;
            return fb.compareTo(fa);
        });

        return filas;
    }

    /* ----------------------------- Internos -------------------------- */

    private ConversacionVisitante buscar(String token) {
        if (!StringUtils.hasText(token)) {
            throw ApiException.notFound("Conversacion no encontrada");
        }
        return conversacionRepository.findByToken(token.trim())
                .orElseThrow(() -> ApiException.notFound("Conversacion no encontrada"));
    }

    private void avisarEquipo(ConversacionVisitante conversacion, String texto) {
        String quien = StringUtils.hasText(conversacion.getNombre())
                ? conversacion.getNombre() : "Visitante del sitio";
        String recorte = texto.length() > 120 ? texto.substring(0, 120) + "..." : texto;
        try {
            soporteService.notificarEquipo("Nuevo mensaje del chat web",
                    quien + ": " + recorte,
                    "chat.html?visita=" + conversacion.getIdConversacion());
        } catch (Exception ex) {
            log.warn("No se pudo avisar al equipo del mensaje del visitante: {}", ex.getMessage());
        }
    }

    private void publicar(String canal, Integer id, Object payload) {
        try {
            mensajeria.convertAndSend("/topic/" + canal + "/" + id, payload);
        } catch (Exception ex) {
            log.debug("Sin WebSocket disponible para {} #{}", canal, id);
        }
    }

    private String nuevoToken() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    private String recortar(String texto) {
        if (texto == null) return "";
        String limpio = texto.replace("\n", " ").trim();
        return limpio.length() > 80 ? limpio.substring(0, 77) + "..." : limpio;
    }

    private long NumberOrZero(Integer n) {
        return n == null ? 0 : n.longValue();
    }
}
