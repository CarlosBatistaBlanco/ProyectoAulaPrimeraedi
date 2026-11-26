/*
 * Este archivo guarda y actualiza los chats de las personas que escriben desde el sitio web
 * sin iniciar sesion. Tambien mezcla esos mensajes con los chats de los clientes registrados
 * en una sola bandeja ordenada por urgencia, para que administradores y empleados puedan
 * responder al instante desde el panel de Chat.
 */
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

    // Prepara el acceso a las conversaciones de visitantes, chats de clientes, avisos y mensajeria en vivo.
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

    // Busca los datos de una conversacion usando el codigo secreto guardado en el navegador del visitante.
    @Override
    @Transactional(readOnly = true)
    public ConversacionVisitanteResponse obtener(String token) {
        if (!StringUtils.hasText(token)) return null;
        return conversacionRepository.findByToken(token.trim())
                .map(ConversacionVisitanteResponse::de)
                .orElse(null);
    }

    // Trae el historial de mensajes del visitante y marca como leidas las respuestas que envio el asesor.
    @Override
    @Transactional
    public List<MensajeVisitanteResponse> mensajes(String token) {
        ConversacionVisitante conversacion = buscar(token);
        List<MensajeVisitante> todos =
                mensajeRepository.findByIdConversacionOrderByCreadoEnAsc(conversacion.getIdConversacion());

        // Marca como leidos los mensajes del asesor ahora que el visitante abrio su ventana de chat.
        List<MensajeVisitante> pendientes = todos.stream()
                .filter(m -> "asesor".equals(m.getRemitente()) && !Boolean.TRUE.equals(m.getLeido()))
                .toList();
        if (!pendientes.isEmpty()) {
            pendientes.forEach(m -> m.setLeido(true));
            mensajeRepository.saveAll(pendientes);
        }
        return todos.stream().map(MensajeVisitanteResponse::de).toList();
    }

    // Guarda el mensaje del visitante, actualiza el contador de pendientes y avisa en vivo al equipo; si algo falla, todo se deshace para no perder el orden.
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

        // Envia una alerta a la campanita del equipo y muestra el mensaje al instante en la pantalla del chat.
        avisarEquipo(conversacion, texto);
        publicar("visita", conversacion.getIdConversacion(), MensajeVisitanteResponse.de(mensaje));

        return ConversacionVisitanteResponse.de(conversacion);
    }

    // Abre una conversacion nueva la primera vez que escribe el visitante, o actualiza sus datos si luego deja nombre, correo o telefono.
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

    // Guarda la respuesta escrita por un asesor humano, marca la charla como atendida y la muestra en vivo al visitante.
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

        // Cuando una persona del equipo contesta, desaparecen los pendientes y la conversacion queda marcada como atendida.
        conversacion.setSinLeer(0);
        conversacion.setEstado("atendida");
        conversacion.setActualizadoEn(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        publicar("visita", conversacion.getIdConversacion(), MensajeVisitanteResponse.de(respuesta));
        return MensajeVisitanteResponse.de(respuesta);
    }

    // Deja guardada la respuesta automatica del asistente virtual en el chat, sin marcarla como si ya hubiera respondido un humano.
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

    // Trae todos los mensajes de un visitante para que el administrador o empleado los lea desde su panel.
    @Override
    @Transactional(readOnly = true)
    public List<MensajeVisitanteResponse> conversacionVisita(Integer idConversacion) {
        if (idConversacion == null) {
            throw ApiException.badRequest("Conversacion no indicada");
        }
        ConversacionVisitante conversacion = conversacionRepository.findById(idConversacion)
                .orElseThrow(() -> ApiException.notFound("Conversacion no encontrada"));
        return mensajeRepository.findByIdConversacionOrderByCreadoEnAsc(conversacion.getIdConversacion())
                .stream().map(MensajeVisitanteResponse::de).toList();
    }

    // Recupera el codigo secreto de un chat de visitante para poder entregarle la respuesta escrita desde el panel.
    @Override
    @Transactional(readOnly = true)
    public String tokenDeVisita(Integer idConversacion) {
        return conversacionRepository.findById(idConversacion).map(ConversacionVisitante::getToken).orElse(null);
    }

    // Arma la bandeja unificada del equipo mezclando clientes con cuenta y visitantes del sitio web (estos llevan el prefijo "cv-" para distinguirlos).
    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> bandeja() {
        List<Map<String, Object>> filas = new ArrayList<>();

        // 1) Agrega a la bandeja todos los clientes registrados con su ultimo mensaje y cuantos faltan por leer.
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

        // 2) Agrega a la misma bandeja los visitantes publicos que ya enviaron al menos un mensaje.
        for (ConversacionVisitante v : conversacionRepository.findAllByOrderByActualizadoEnDesc()) {
            List<MensajeVisitante> mensajes =
                    mensajeRepository.findByIdConversacionOrderByCreadoEnAsc(v.getIdConversacion());
            MensajeVisitante ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
            if (ultimo == null) {
                continue; // Ignora chats vacios donde nadie alcanzo a escribir.
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
            // Marca que espera atencion humana si el visitante fue el ultimo en escribir y ningun asesor real le ha respondido aun.
            boolean hayAsesor = mensajes.stream().anyMatch(m -> "asesor".equals(m.getRemitente())
                    && m.getIdUsuarioEnvia() != null);
            fila.put("solicitaAsesor", "visitante".equals(ultimo.getRemitente()) && !hayAsesor);
            filas.add(fila);
        }

        // 3) Ordena la bandeja poniendo arriba a quienes esperan asesor, luego chats con mensajes sin leer y despues los mas recientes.
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

    // Busca la conversacion del visitante por su codigo y avisa con error si no existe.
    private ConversacionVisitante buscar(String token) {
        if (!StringUtils.hasText(token)) {
            throw ApiException.notFound("Conversacion no encontrada");
        }
        return conversacionRepository.findByToken(token.trim())
                .orElseThrow(() -> ApiException.notFound("Conversacion no encontrada"));
    }

    // Envia un aviso a la campanita de administradores y empleados cuando llega un mensaje nuevo desde la web.
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

    // Muestra el mensaje nuevo en vivo en la pantalla del chat sin que la persona tenga que recargar.
    private void publicar(String canal, Integer id, Object payload) {
        try {
            mensajeria.convertAndSend("/topic/" + canal + "/" + id, payload);
        } catch (Exception ex) {
            log.debug("Sin WebSocket disponible para {} #{}", canal, id);
        }
    }

    // Genera un codigo secreto al azar de 32 letras y numeros para identificar cada conversacion de visitante.
    private String nuevoToken() {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(ALFABETO.charAt(AZAR.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    // Acorta el ultimo mensaje a una sola linea breve para que quepa ordenado en la lista de la bandeja.
    private String recortar(String texto) {
        if (texto == null) return "";
        String limpio = texto.replace("\n", " ").trim();
        return limpio.length() > 80 ? limpio.substring(0, 77) + "..." : limpio;
    }

    // Convierte un numero vacio en cero para evitar errores al contar mensajes pendientes.
    private long NumberOrZero(Integer n) {
        return n == null ? 0 : n.longValue();
    }
}
