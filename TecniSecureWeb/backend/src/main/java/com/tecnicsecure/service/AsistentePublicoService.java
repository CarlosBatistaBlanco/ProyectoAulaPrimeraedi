/*
 * Este archivo atiende el asistente virtual de TECNISECURE (tanto en la web como en la app movil).
 * Prioriza la inteligencia artificial DeepSeek alimentada en tiempo real con el catalogo completo,
 * precios, stock y servicios reales de la tienda. Si DeepSeek no tiene llave configurada, responde
 * con un motor inteligente local que entiende tildes, plurales, categorias y especificaciones.
 */
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AsistentePublicoService {

    private static final Logger log = LoggerFactory.getLogger(AsistentePublicoService.class);

    // Botones rapidos que aparecen en el chat publico para ayudar al visitante a iniciar la charla.
    private static final List<String> SUGERENCIAS = List.of(
            "¿Que camaras tienen disponibles?",
            "¿Cuanto cuesta instalar un sistema?",
            "¿Que garantia tienen los equipos?",
            "Hablar con un asesor"
    );

    private static final Set<String> PALABRAS_IGNORADAS = Set.of(
            "hola", "buenas", "como", "esta", "estas", "que", "cual", "cuales", "cuanto", "cuantos",
            "vale", "valen", "cuesta", "cuestan", "precio", "precios", "tienen", "tiene", "hay",
            "disponible", "disponibles", "venden", "vende", "para", "una", "unas", "uno", "unos",
            "los", "las", "del", "con", "sin", "por", "favor", "quiero", "necesito", "busco",
            "recomienda", "recomiendas", "recomendacion", "mejor", "mejores", "equipo", "equipos",
            "producto", "productos", "catalogo", "tienda", "sistema", "sistemas"
    );

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

    // Recibe lo que escribio el visitante y prioriza: 1) escalado a humano si lo pide, 2) IA DeepSeek con catalogo real, 3) asesor local inteligente.
    public AsistenteResponse conversar(AsistenteRequest peticion) {
        String mensaje = peticion.mensaje() == null ? "" : peticion.mensaje().trim();
        String texto = normalizar(mensaje);

        String token = registrarEnHilo(peticion, mensaje);

        // Paso 1: si la persona pide hablar explicitamente con un asesor humano, crea el caso de soporte de inmediato.
        if (esEscalado(texto)) {
            AsistenteResponse r = escalar(peticion, mensaje,
                    "Listo, registre tu solicitud de soporte y ya avise al equipo tecnico.");
            guardarRespuestaBot(token, r.respuesta());
            return conToken(r, token);
        }

        // Paso 2: si DeepSeek esta configurado, responde como asesor experto independiente con todo el catalogo e inventario en vivo.
        if (deepSeek.estaDisponible()) {
            String promptConCatalogo = construirInstruccionesConCatalogo();
            String ia = deepSeek.responder(promptConCatalogo, historial(peticion, mensaje));
            if (ia != null && !ia.isBlank()) {
                ia = limpiarEmojis(ia);
                guardarRespuestaBot(token, ia);
                return conToken(new AsistenteResponse(ia, "deepseek", false, null, null, SUGERENCIAS), token);
            }
            log.warn("DeepSeek no respondio a tiempo; usando motor asesor local para: {}", mensaje);
        }

        // Paso 3: motor asesor local inteligente (entiende plurales, sin tildes, categorias y recomendaciones).
        String local = respuestaLocal(texto);
        if (local != null) {
            guardarRespuestaBot(token, local);
            return conToken(new AsistenteResponse(local, "local", false, null, null, SUGERENCIAS), token);
        }

        // Paso 4: si es una consulta muy particular fuera de catalogo y no hay IA externa, escala a un asesor humano.
        AsistenteResponse escalado = escalar(peticion, mensaje,
                "Para darte una respuesta precisa prefiero que te hable un tecnico.");
        guardarRespuestaBot(token, escalado.respuesta());
        return conToken(escalado, token);
    }

    // Construye las instrucciones para DeepSeek incluyendo el inventario real en tiempo real (productos, precios, descuentos y servicios).
    private String construirInstruccionesConCatalogo() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                Eres el Asistente Oficial y Exclusivo de TECNISECURE (TECNISERVICIOS), plataforma de comercio electronico, facturacion electronica DIAN y servicios tecnicos de seguridad electronica en Cartagena, Colombia.
                RESTRICCION ABSOLUTA DE DOMINIO:
                - SOLO puedes responder consultas sobre TECNISECURE: productos del catalogo (camaras CCTV, alarmas, control de acceso y accesorios), cotizaciones de esos equipos, servicios tecnicos, garantias, envios, pagos, pedidos, facturacion DIAN y funcionamiento del proyecto.
                - PROHIBIDO responder temas ajenos: matematicas sueltas (ej. cuanto es 2*3), historia (ej. fundador de una ciudad), geografia, cultura general, chistes, recetas, tareas academicas o programacion general.
                - Ante cualquier pregunta ajena, JAMAS des ni insinues la respuesta del tema externo; responde UNICAMENTE que eres el asistente exclusivo de TECNISECURE y solo atiendes consultas sobre sus equipos, servicios tecnicos y pedidos.
                - Usa UNICAMENTE los productos, precios en pesos colombianos (COP), descuentos, stock y servicios reales listados abajo.
                - Informacion oficial: Garantia minima de 12 meses, factura electronica DIAN con IVA 19% incluido, envios en 24-48h habiles, pagos por PSE/transferencia, tarjeta o contraentrega. Horario: lunes a sabado 8:00 a.m. a 6:00 p.m.
                - Responde en maximo 130 palabras bien estructuradas con vinetas usando guiones (-). NO uses emojis bajo ninguna circunstancia.
                - Solo si el cliente pide hablar con una persona real o agendar visita inmediata con un humano, indicale que toque "Hablar con un asesor".

                CATALOGO EN TIEMPO REAL DE TECNISECURE:
                """);

        List<Producto> activos = productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();
        for (Producto p : activos) {
            String cat = p.getCategoria() != null ? p.getCategoria().getNombre() : "General";
            String marca = p.getMarca() != null ? p.getMarca().getNombre() : "";
            String desc = p.getDescripcion() != null ? p.getDescripcion() : "";
            boolean rebaja = p.isTieneDescuento();
            sb.append("- [").append(cat).append("] ").append(p.getNombre());
            if (!marca.isBlank()) sb.append(" (").append(marca).append(")");
            sb.append(": ").append(moneda(p.getPrecioFinal()));
            if (rebaja) sb.append(" [antes ").append(moneda(p.getPrecio())).append(", -").append(p.getDescuentoPct().intValue()).append("%]");
            sb.append(" | Stock: ").append(p.getStock());
            if (!desc.isBlank()) sb.append(" | ").append(desc);
            sb.append("\n");
        }

        List<Servicio> servicios = servicioRepository.findAllByOrderByOrdenAsc();
        if (!servicios.isEmpty()) {
            sb.append("\nSERVICIOS TECNICOS DISPONIBLES:\n");
            for (Servicio s : servicios) {
                sb.append("- ").append(s.getTitulo()).append(": ")
                  .append(s.getDescripcion() != null ? s.getDescripcion() : "").append("\n");
            }
        }
        return sb.toString();
    }

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

    // Asesor local inteligente que responde sobre equipos, categorias, servicios, precios, garantias, pagos y horarios.
    private String respuestaLocal(String texto) {
        if (esDespedida(texto)) {
            return "¡Con mucho gusto! Estamos atentos si necesitas cotizar otro equipo o programar una instalacion.";
        }
        if (contiene(texto, "horario", "hora de atencion", "abierto", "domingo", "ubicacion")) {
            return "Nuestro horario de atencion es de lunes a sabado, de 8:00 a.m. a 6:00 p.m.\n"
                    + "Atendemos en Cartagena y envios a nivel nacional · Soporte: soporte@tecnisecure.com";
        }
        if (contiene(texto, "garantia", "garantias", "factura", "dian")) {
            return "Todos nuestros equipos cuentan con garantia minima de 12 meses y factura electronica DIAN con CUFE verificable.\n"
                    + "La instalacion realizada por nuestros tecnicos tambien queda respaldada.";
        }
        if (contiene(texto, "envio", "domicilio", "entrega", "demora")) {
            return "Realizamos entregas en Cartagena y envios asegurados a todo el pais.\n"
                    + "Los pedidos se despachan entre 24 y 48 horas habiles tras confirmar tu orden.";
        }
        if (contiene(texto, "pago", "pagar", "tarjeta", "efectivo", "transferencia", "pse", "contraentrega")) {
            return "Aceptamos PSE / transferencia bancaria, tarjeta de credito o debito y pago contraentrega.\n"
                    + "Al confirmar tu pedido se emite automaticamente tu factura electronica.";
        }

        // Si pregunta especificamente por instalacion, mantenimiento, citas o servicios tecnicos
        boolean pideServicio = contiene(texto, "instalar", "instalacion", "mantenimiento", "cita", "visita tecnica", "servicios");
        boolean mencionaEquipo = contiene(texto, "camara", "cctv", "domo", "bullet", "ptz", "wifi", "solar",
                "alarma", "sensor", "sirena", "humo", "movimiento", "acceso", "biometric", "huella",
                "cerradura", "rfid", "videoportero", "torniquete", "disco", "ssd", "ups", "cable",
                "balun", "fuente", "gabinete", "nvr", "dvr", "hikvision", "dahua", "reolink", "ezviz", "zkteco");

        if (pideServicio && !mencionaEquipo) {
            return consultarServicios();
        }

        if (mencionaEquipo || contiene(texto, "precio", "cuanto vale", "cuanto cuesta", "stock",
                "disponible", "tienen", "venden", "catalogo", "productos", "recomienda", "barat", "econom")) {
            String hallazgo = consultarProductosInteligente(texto);
            if (hallazgo != null) {
                return hallazgo;
            }
        }

        if (pideServicio) {
            return consultarServicios();
        }

        if (contiene(texto, "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches", "ayuda", "informacion")) {
            return "¡Hola! Soy el asesor virtual de TECNISECURE.\n"
                    + "Preguntame por camaras (4K, WiFi, PTZ, solares), alarmas, control biometrico, accesorios o instalacion tecnica.";
        }

        return null;
    }

    // Puntua cada producto segun categoria, nombre, marca y especificaciones para recomendar exactamente lo que pidio el cliente.
    private String consultarProductosInteligente(String texto) {
        List<Producto> activos = productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .toList();
        if (activos.isEmpty()) {
            return null;
        }

        boolean buscaCamara = contiene(texto, "camara", "cctv", "domo", "bullet", "ptz", "videovigilancia");
        boolean buscaAlarma = contiene(texto, "alarma", "sirena", "sensor", "humo", "intrusion");
        boolean buscaAcceso = contiene(texto, "acceso", "biometric", "huella", "cerradura", "rfid", "videoportero");
        boolean buscaAccesorio = contiene(texto, "accesorio", "balun", "cable", "utp", "disco", "ups", "fuente", "bateria");

        List<String> raices = Arrays.stream(texto.split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 2 && !PALABRAS_IGNORADAS.contains(w))
                .map(this::raizSingular)
                .distinct()
                .toList();

        record Candidato(Producto p, int puntaje) {}

        List<Producto> mejores = activos.stream()
                .map(p -> {
                    int puntos = 0;
                    String nombre = normalizar(p.getNombre());
                    String cat = normalizar(p.getCategoria() != null ? p.getCategoria().getNombre() : "");
                    String marca = normalizar(p.getMarca() != null ? p.getMarca().getNombre() : "");
                    String desc = normalizar(p.getDescripcion());

                    // Priorizar la categoria principal solicitada (evita mostrar un Balun cuando piden Camaras)
                    if (buscaCamara && !buscaAccesorio) {
                        if (cat.contains("camara")) puntos += 12;
                        if (cat.contains("accesorio")) puntos -= 8;
                    }
                    if (buscaAlarma && cat.contains("alarma")) puntos += 12;
                    if (buscaAcceso && cat.contains("acceso")) puntos += 12;
                    if (buscaAccesorio && cat.contains("accesorio")) puntos += 12;

                    for (String r : raices) {
                        if (r.length() < 2) continue;
                        if (nombre.contains(r)) puntos += 8;
                        if (marca.contains(r)) puntos += 6;
                        if (desc.contains(r)) puntos += 4;
                        if (cat.contains(r)) puntos += 3;
                    }

                    if (contiene(texto, "exterior", "intemperie", "lluvia") && contiene(desc, "ip66", "ip67", "exterior", "antivandalic")) {
                        puntos += 10;
                    }
                    if (contiene(texto, "interior", "casa", "apartamento", "oficina") && contiene(nombre + " " + desc, "interior", "wifi", "domo")) {
                        puntos += 6;
                    }
                    if (Boolean.TRUE.equals(p.getDestacado())) {
                        puntos += 1;
                    }
                    return new Candidato(p, puntos);
                })
                .filter(c -> c.puntaje() > 1)
                .sorted(Comparator.comparingInt(Candidato::puntaje).reversed())
                .limit(4)
                .map(Candidato::p)
                .toList();

        if (mejores.isEmpty()) {
            mejores = productoRepository.findByDestacadoTrueAndActivoTrueOrderByIdProductoAsc()
                    .stream().limit(4).toList();
        }
        if (mejores.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("Te recomiendo estas opciones disponibles en inventario:\n");
        for (Producto p : mejores) {
            sb.append("• ").append(p.getNombre());
            if (p.getMarca() != null && p.getMarca().getNombre() != null) {
                sb.append(" (").append(p.getMarca().getNombre()).append(")");
            }
            sb.append(": ").append(moneda(p.getPrecioFinal()));
            if (p.isTieneDescuento()) {
                sb.append(" [-").append(p.getDescuentoPct().intValue()).append("%]");
            }
            sb.append(" · Stock: ").append(p.getStock()).append("\n");
        }
        sb.append("¿Deseas conocer los detalles tecnicos de alguno o cotizar su instalacion?");
        return sb.toString().trim();
    }

    private String consultarServicios() {
        List<Servicio> servicios = servicioRepository.findAllByOrderByOrdenAsc();
        if (servicios.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Nuestros servicios tecnicos especializados incluyen:\n");
        servicios.stream().limit(4).forEach(s ->
                sb.append("• ").append(s.getTitulo())
                  .append(s.getDescripcion() != null ? ": " + s.getDescripcion() : "")
                  .append("\n")
        );
        sb.append("La cotizacion se ajusta segun cantidad de puntos y metraje. ¿Te gustaria hablar con un asesor para agendar visita?");
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

        for (String palabra : titulo.split("[^a-z0-9]+")) {
            if (palabra.length() >= 4 && texto.contains(raizSingular(palabra))) {
                puntaje += 3;
            }
        }
        for (String palabra : descripcion.split("[^a-z0-9]+")) {
            if (palabra.length() >= 5 && texto.contains(raizSingular(palabra))) {
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
        return contiene(texto, "gracias", "adios", "chao", "hasta luego");
    }

    private boolean contiene(String texto, String... claves) {
        if (texto == null || texto.isEmpty()) return false;
        for (String c : claves) {
            if (texto.contains(c)) return true;
        }
        return false;
    }

    private boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }

    private String raizSingular(String palabra) {
        if (palabra == null) return "";
        if (palabra.length() > 5 && palabra.endsWith("ores")) {
            return palabra.substring(0, palabra.length() - 2);
        }
        if (palabra.length() > 4 && palabra.endsWith("s") && !palabra.endsWith("ups")) {
            return palabra.substring(0, palabra.length() - 1);
        }
        return palabra;
    }

    private String limpiarEmojis(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\x{1F300}-\\x{1FAFF}\\u2600-\\u27BF\\uFE0F]", "").trim();
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.replaceAll("\\s+", " ").trim();
    }

    private String moneda(Number n) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"))
                .format(n != null ? n.doubleValue() : 0);
    }
}
