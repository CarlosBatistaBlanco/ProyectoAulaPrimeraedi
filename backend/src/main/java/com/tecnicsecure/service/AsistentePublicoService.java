package com.tecnicsecure.service;

import com.tecnicsecure.dto.soporte.AsistenteRequest;
import com.tecnicsecure.dto.soporte.AsistenteResponse;
import com.tecnicsecure.dto.soporte.SolicitudRequest;
import com.tecnicsecure.dto.soporte.SolicitudResponse;
import com.tecnicsecure.entity.Notificacion;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.entity.Servicio;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.repository.NotificacionRepository;
import com.tecnicsecure.repository.ProductoRepository;
import com.tecnicsecure.repository.ServicioRepository;
import com.tecnicsecure.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Asistente virtual del sitio publico (visitantes y clientes sin iniciar sesion).
 *
 * Flujo:
 *  1. Preguntas frecuentes y consultas de catalogo se resuelven localmente.
 *  2. Las preguntas abiertas se envian a DeepSeek (si esta configurado).
 *  3. Si no hay solucion (o el cliente lo pide) se ESCALA: se registra una
 *     solicitud de soporte y se notifica a administradores y empleados.
 */
@Service
public class AsistentePublicoService {

    private static final Logger log = LoggerFactory.getLogger(AsistentePublicoService.class);

    private static final List<String> SUGERENCIAS = List.of(
            "¿Que camaras tienen disponibles?",
            "¿Cuanto cuesta instalar un sistema?",
            "¿Que garantia tienen los equipos?",
            "Hablar con un asesor"
    );

    private static final String INSTRUCCIONES = """
            Eres el Asistente Virtual de TECNISECURE, empresa colombiana de seguridad electronica
            (camaras CCTV, alarmas y control de acceso) que instala y da soporte tecnico.
            Reglas:
            - Responde en espanol, claro, breve (maximo 90 palabras) y amable.
            - Solo habla de TECNISECURE: productos, precios, servicios, garantias, envios,
              instalacion, soporte y horarios (lunes a sabado, 8:00 a.m. a 6:00 p.m.).
            - No inventes precios ni datos que no aparezcan en el contexto.
            - Si no sabes algo, o el cliente pide hablar con una persona, dilo y sugiere
              escribir la palabra ASESOR para que un tecnico lo contacte.
            """;

    private final DeepSeekService deepSeek;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final SoporteService soporteService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final SimpMessagingTemplate mensajeria;
    private final com.tecnicsecure.service.ConversacionVisitanteService conversacionVisitanteService;

    public AsistentePublicoService(DeepSeekService deepSeek,
                                   ProductoRepository productoRepository,
                                   ServicioRepository servicioRepository,
                                   SoporteService soporteService,
                                   UsuarioRepository usuarioRepository,
                                   NotificacionRepository notificacionRepository,
                                   SimpMessagingTemplate mensajeria,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false)
                                   com.tecnicsecure.service.ConversacionVisitanteService conversacionVisitanteService) {
        this.deepSeek = deepSeek;
        this.productoRepository = productoRepository;
        this.servicioRepository = servicioRepository;
        this.soporteService = soporteService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
        this.mensajeria = mensajeria;
        this.conversacionVisitanteService = conversacionVisitanteService;
    }

    public AsistenteResponse conversar(AsistenteRequest peticion) {
        String mensaje = peticion.mensaje() == null ? "" : peticion.mensaje().trim();
        String texto = normalizar(mensaje);

        // 0) El hilo queda registrado para que el equipo pueda seguir la conversacion
        String token = registrarEnHilo(peticion, mensaje);

        // 1) El cliente pide explicitamente un humano
        if (esEscalado(texto)) {
            AsistenteResponse r = escalar(peticion, mensaje,
                    "Listo, registre tu solicitud de soporte y ya avise al equipo tecnico.");
            guardarRespuestaBot(token, r.respuesta());
            return conToken(r, token);
        }

        // 2) Catalogo / servicios: respondemos con datos reales de la base
        String local = respuestaLocal(texto);
        if (local != null) {
            guardarRespuestaBot(token, local);
            return conToken(new AsistenteResponse(local, "local", false, null, null, SUGERENCIAS), token);
        }

        // 3) Pregunta abierta: se intenta con DeepSeek
        if (deepSeek.estaDisponible()) {
            String ia = deepSeek.responder(INSTRUCCIONES, historial(peticion, mensaje));
            if (ia != null && !ia.isBlank()) {
                log.debug("Asistente respondio con IA ({} caracteres)", ia.length());
                guardarRespuestaBot(token, ia);
                return conToken(new AsistenteResponse(ia, "deepseek", false, null, null, SUGERENCIAS), token);
            }
            log.warn("La IA no respondio; se escala la consulta: {}", mensaje);
        } else {
            log.debug("IA no configurada (DEEPSEEK_API_KEY vacia); se escala la consulta");
        }

        // 4) Sin respuesta automatica: se escala a un asesor humano
        AsistenteResponse escalado = escalar(peticion, mensaje,
                "Para darte una respuesta precisa prefiero que te hable un tecnico.");
        guardarRespuestaBot(token, escalado.respuesta());
        return conToken(escalado, token);
    }

    /** Guarda el mensaje del visitante y devuelve el token del hilo (o null si no se pudo). */
    private String registrarEnHilo(AsistenteRequest peticion, String mensaje) {
        if (conversacionVisitanteService == null || !tieneTexto(mensaje)) {
            return peticion.token();
        }
        try {
            var guardada = conversacionVisitanteService.enviarVisitante(
                    new com.tecnicsecure.dto.soporte.MensajeVisitanteRequest(
                            mensaje, peticion.token(), peticion.nombre(),
                            peticion.correo(), peticion.telefono()));
            return guardada != null ? guardada.token() : peticion.token();
        } catch (Exception ex) {
            log.warn("No se pudo guardar el hilo del visitante: {}", ex.getMessage());
            return peticion.token();
        }
    }

    private void guardarRespuestaBot(String token, String respuesta) {
        if (conversacionVisitanteService == null || !tieneTexto(token) || !tieneTexto(respuesta)) {
            return;
        }
        try {
            conversacionVisitanteService.enviarBot(token, respuesta);
        } catch (Exception ex) {
            log.debug("No se pudo guardar la respuesta del bot: {}", ex.getMessage());
        }
    }

    private AsistenteResponse conToken(AsistenteResponse r, String token) {
        return new AsistenteResponse(r.respuesta(), r.fuente(), r.escalado(), r.idSolicitud(),
                r.mensajeEscalado(), r.sugerencias(), token);
    }

    /** Devuelve la solicitud escalada y las notificaciones al equipo. */
    private AsistenteResponse escalar(AsistenteRequest peticion, String mensaje, String prefijo) {
        String nombre = tieneTexto(peticion.nombre()) ? peticion.nombre().trim() : "Visitante del sitio";
        String correo = tieneTexto(peticion.correo()) ? peticion.correo().trim() : "visitante@tecnicsecure.local";
        String telefono = tieneTexto(peticion.telefono()) ? peticion.telefono().trim() : "No indicado";

        String descripcion = "Chat del sitio web.\n" + mensaje;
        if (descripcion.length() > 1900) {
            descripcion = descripcion.substring(0, 1900);
        }

        Integer idSolicitud = null;
        try {
            // El tipo solo admite 'cotizacion' o 'cita'; el origen queda en la descripcion.
            // El servicio se deduce del mensaje para no etiquetar todo como "Instalacion de camaras".
            Servicio servicio = servicioSegunMensaje(mensaje);
            SolicitudRequest solicitud = new SolicitudRequest(
                    servicio.getIdServicio(), nombre, correo, telefono, null,
                    "cotizacion", null, null, descripcion);
            SolicitudResponse creada = soporteService.crearSolicitud(solicitud);
            idSolicitud = creada != null ? creada.idSolicitud() : null;
        } catch (Exception ex) {
            log.warn("No se pudo registrar la solicitud del asistente: {}", ex.getMessage());
        }

        notificarEquipo(nombre, mensaje);

        String texto = prefijo + "\n"
                + "Un asesor te contactara pronto"
                + (tieneTexto(peticion.correo()) ? " en " + peticion.correo().trim() : "")
                + ". Si prefieres, deja tu correo y telefono y te llamamos.\n"
                + (idSolicitud != null ? "Tu numero de caso es #" + idSolicitud + "." : "");

        return new AsistenteResponse(texto.trim(), "escalado", true, idSolicitud, texto.trim(),
                List.of("Ver mis pedidos", "Hablar con un asesor"));
    }

    private void notificarEquipo(String nombre, String mensaje) {
        List<Usuario> destino = new ArrayList<>();
        destino.addAll(usuarioRepository.findByRolNombre("administrador"));
        destino.addAll(usuarioRepository.findByRolNombre("empleado"));

        String recorte = mensaje.length() > 120 ? mensaje.substring(0, 120) + "..." : mensaje;
        for (Usuario u : destino) {
            if (!Boolean.TRUE.equals(u.getActivo())) {
                continue;
            }
            try {
                Notificacion n = new Notificacion();
                n.setIdUsuario(u.getIdUsuario());
                n.setTitulo("Nuevo chat del sitio web");
                n.setMensaje(nombre + ": " + recorte);
                n.setEnlace("soporte.html");
                n.setLeida(false);
                notificacionRepository.save(n);
                if (mensajeria != null) {
                    mensajeria.convertAndSend("/topic/notificaciones/" + u.getIdUsuario(), n);
                }
            } catch (Exception ex) {
                log.warn("No se pudo notificar a {}: {}", u.getCorreo(), ex.getMessage());
            }
        }
    }

    /** Respuestas que no necesitan IA (datos reales y preguntas frecuentes). */
    private String respuestaLocal(String texto) {
        if (contiene(texto, "horario", "hora de atencion", "abierto", "domingo")) {
            return "Nuestro horario de atencion es de lunes a sabado, de 8:00 a.m. a 6:00 p.m.\n"
                    + "Soporte: soporte@tecnisecure.com";
        }
        if (contiene(texto, "garantia", "garantias")) {
            return "Todos nuestros equipos tienen garantia minima de 12 meses y factura electronica DIAN.\n"
                    + "La instalacion tambien queda cubierta.";
        }
        if (contiene(texto, "envio", "domicilio", "entrega", "demora")) {
            return "Hacemos entregas en la ciudad y envios nacionales.\n"
                    + "Los pedidos se despachan entre 24 y 48 horas habiles despues del pago.";
        }
        if (contiene(texto, "pago", "pagar", "tarjeta", "efectivo", "transferencia")) {
            return "Puedes pagar en efectivo contra entrega, por transferencia o con tarjeta.\n"
                    + "Al confirmar el pedido recibes la factura electronica.";
        }
        if (contiene(texto, "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches")) {
            return "¡Hola! Soy el asistente virtual de TECNISECURE.\n"
                    + "Puedo ayudarte con precios, productos, servicios, garantias o ponerte en contacto con un asesor.";
        }

        // Solo se consulta el catalogo si de verdad pregunta por precio o
        // existencia. Si no, la pregunta pasa a la IA (o a un asesor).
        boolean preguntaPorCatalogo = contiene(texto, "precio", "cuanto vale", "cuanto cuesta",
                "cuanto sale", "stock", "disponible", "disponibles", "tienen", "venden", "catalogo",
                "productos", "referencia", "modelo");
        boolean mencionaProducto = contiene(texto, "camara", "alarma", "sensor", "disco", "ssd",
                "router", "ups", "cerradura", "control de acceso", "videoportero");
        if (preguntaPorCatalogo && (mencionaProducto || contiene(texto, "producto", "catalogo"))) {
            return consultarProductos(texto);
        }
        if (contiene(texto, "agendar", "agenda", "instalacion", "mantenimiento", "tecnico",
                "cita", "cotizacion", "servicios")) {
            return consultarServicios();
        }
        return null;
    }

    private String consultarProductos(String texto) {
        List<Producto> activos = productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();

        String[] palabras = texto.split("[^a-z0-9]+");
        List<Producto> hallados = activos.stream()
                .filter(p -> {
                    String nombre = normalizar(p.getNombre() != null ? p.getNombre() : "");
                    for (String palabra : palabras) {
                        if (palabra.length() >= 4 && nombre.contains(palabra)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(3)
                .toList();

        if (hallados.isEmpty()) {
            hallados = productoRepository.findByDestacadoTrueAndActivoTrueOrderByIdProductoAsc()
                    .stream().limit(3).toList();
        }
        if (hallados.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Estos equipos tenemos disponibles:\n");
        for (Producto p : hallados) {
            sb.append("• ").append(p.getNombre()).append(": ")
              .append(moneda(p.getPrecioFinal()))
              .append(" (").append(p.getStock()).append(" disponibles)\n");
        }
        sb.append("¿Quieres que un asesor te ayude a elegir?");
        return sb.toString().trim();
    }

    private String consultarServicios() {
        List<Servicio> servicios = servicioRepository.findAllByOrderByOrdenAsc();
        if (servicios.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Estos son nuestros servicios tecnicos:\n");
        servicios.stream().limit(4).forEach(s -> sb.append("• ").append(s.getTitulo()).append("\n"));
        sb.append("Puedes solicitar una cotizacion o cita desde la seccion Servicios.");
        return sb.toString().trim();
    }

    private List<Map<String, String>> historial(AsistenteRequest peticion, String mensajeActual) {
        List<Map<String, String>> lista = new ArrayList<>();
        if (peticion.historial() != null) {
            int desde = Math.max(0, peticion.historial().size() - 10);
            for (AsistenteRequest.Mensaje m : peticion.historial().subList(desde, peticion.historial().size())) {
                if (m != null && tieneTexto(m.texto())) {
                    lista.add(Map.of(
                            "rol", "assistant".equals(m.rol()) ? "assistant" : "user",
                            "texto", m.texto().trim()));
                }
            }
        }
        lista.add(Map.of("rol", "user", "texto", mensajeActual));
        return lista;
    }

    /**
     * Elige el servicio que mejor coincide con lo que escribio el visitante.
     * Antes toda solicitud quedaba como "Instalacion de Camaras", lo que hacia
     * imposible clasificar los casos de soporte. Se puntua por palabras clave
     * del titulo y la descripcion; si nada coincide se usa "Asesoria y Diseno"
     * (o el primero del catalogo) como caso generico.
     */
    private Servicio servicioSegunMensaje(String mensaje) {
        List<Servicio> servicios = servicioRepository.findAllByOrderByOrdenAsc();
        if (servicios.isEmpty()) {
            throw new IllegalStateException("No hay servicios configurados");
        }

        String texto = normalizar(mensaje);
        Servicio mejor = null;
        int mejorPuntaje = 0;

        for (Servicio s : servicios) {
            int puntaje = puntajeServicio(s, texto);
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = s;
            }
        }
        if (mejor != null) {
            return mejor;
        }

        // Caso generico: asesoria si existe, si no el primero (que hoy es "Instalacion de Camaras").
        return servicios.stream()
                .filter(s -> normalizar(s.getTitulo()).contains("asesoria")
                        || normalizar(s.getTitulo()).contains("consulta"))
                .findFirst()
                .orElse(servicios.get(0));
    }

    private int puntajeServicio(Servicio servicio, String texto) {
        int puntaje = 0;
        String titulo = normalizar(servicio.getTitulo() != null ? servicio.getTitulo() : "");
        String descripcion = normalizar(servicio.getDescripcion() != null ? servicio.getDescripcion() : "");

        // Cada palabra significativa del titulo que aparezca en el mensaje suma.
        for (String palabra : titulo.split("[^a-z0-9]+")) {
            if (palabra.length() >= 4 && texto.contains(palabra)) {
                puntaje += 3;
            }
        }
        for (String palabra : descripcion.split("[^a-z0-9]+")) {
            if (palabra.length() >= 5 && texto.contains(palabra)) {
                puntaje += 1;
            }
        }
        return puntaje;
    }

    private boolean esEscalado(String texto) {
        return contiene(texto, "asesor", "humano", "persona real", "agente", "operador",
                "hablar con alguien", "que me llamen", "reclamo", "queja", "soporte tecnico");
    }

    private boolean esDespedida(String texto) {
        return contiene(texto, "gracias", "adios", "chao", "listo");
    }

    private boolean contiene(String texto, String... claves) {
        for (String c : claves) {
            if (texto.contains(c)) return true;
        }
        return false;
    }

    private boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("\\s+", " ").trim();
    }

    private String moneda(Number n) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"))
                .format(n != null ? n.doubleValue() : 0);
    }
}
