package com.tecnicsecure.service;

import com.tecnicsecure.dto.soporte.MensajeChatResponse;
import com.tecnicsecure.entity.MensajeChat;
import com.tecnicsecure.entity.Pedido;
import com.tecnicsecure.entity.Producto;
import com.tecnicsecure.entity.Servicio;
import com.tecnicsecure.repository.MensajeChatRepository;
import com.tecnicsecure.repository.PedidoRepository;
import com.tecnicsecure.repository.ProductoRepository;
import com.tecnicsecure.repository.ServicioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asistente local persistente de soporte al cliente.
 * Consulta directamente la base de datos H2/MySQL sin consumir tokens externos de IA.
 */
@Service
public class BotAsistenteService {

    private static final Pattern PATRON_PEDIDO = Pattern.compile("#(\\d+)");

    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final ServicioRepository servicioRepository;
    private final MensajeChatRepository chatRepository;
    private final SimpMessagingTemplate mensajeria;

    public BotAsistenteService(ProductoRepository productoRepository,
                               PedidoRepository pedidoRepository,
                               ServicioRepository servicioRepository,
                               MensajeChatRepository chatRepository,
                               SimpMessagingTemplate mensajeria) {
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.servicioRepository = servicioRepository;
        this.chatRepository = chatRepository;
        this.mensajeria = mensajeria;
    }

    public boolean esSolicitudAsesor(String mensajeRaw) {
        if (mensajeRaw == null || mensajeRaw.isBlank()) {
            return false;
        }
        String msg = normalizarTexto(mensajeRaw);
        return contiene(msg, "asesor", "humano", "persona", "agente", "operador", "reclamo", "queja");
    }

    /** Analiza el mensaje del cliente y genera una respuesta automatica persistida en el chat. */
    public MensajeChatResponse responderAutomatico(Integer idCliente, String mensajeCliente) {
        if (idCliente == null || mensajeCliente == null || mensajeCliente.isBlank()) {
            return null;
        }
        String respuestaTexto = generarRespuesta(idCliente, mensajeCliente);
        if (respuestaTexto == null) {
            return null;
        }

        MensajeChat bot = new MensajeChat();
        bot.setIdCliente(idCliente);
        bot.setRemitente("asesor");
        bot.setIdUsuarioEnvia(null);
        bot.setMensaje("[Asistente Virtual] " + respuestaTexto);
        bot.setLeido(false);
        chatRepository.save(bot);

        MensajeChatResponse dto = MensajeChatResponse.de(bot);
        if (mensajeria != null) {
            mensajeria.convertAndSend("/topic/chat/" + idCliente, dto);
        }
        return dto;
    }

    /** Calcula la respuesta deterministica consultando inventario, pedidos y servicios en la BD. */
    public String generarRespuesta(Integer idCliente, String mensajeRaw) {
        String msg = mensajeRaw.trim().toLowerCase(Locale.ROOT);

        if (esSolicitudAsesor(msg)) {
            return "Hemos conectado tu chat con prioridad hacia un asesor humano de TECNISECURE.\n"
                    + "• Puedes escribir aquí todos los detalles (número de pedido, producto o consulta) y el asesor los verá de inmediato.";
        }

        if (contiene(msg, "pedido", "orden", "envio", "envío", "rastreo", "guia", "guía")
                || PATRON_PEDIDO.matcher(msg).find()) {
            return consultarPedidosCliente(idCliente, msg);
        }

        if (contiene(msg, "precio", "vale", "cuesta", "stock", "disponible", "camara", "cámara",
                "disco", "ssd", "router", "ups", "producto", "catalogo", "catálogo", "comprar")) {
            return consultarProductos(msg);
        }

        if (contiene(msg, "servicio", "instalacion", "instalación", "mantenimiento", "tecnico",
                "técnico", "garantia", "garantía", "cita", "cotizacion", "cotización")) {
            return consultarServicios();
        }

        if (contiene(msg, "horario", "hora", "ubicacion", "ubicación", "telefono", "teléfono",
                "correo", "contacto", "direccion", "dirección")) {
            return "Información de atención TECNISECURE:\n"
                    + "• Horario: Lunes a Sábado de 8:00 a.m. a 6:00 p.m.\n"
                    + "• Soporte: soporte@tecnisecure.com\n"
                    + "• Garantía mínima de 12 meses y factura electrónica DIAN.";
        }

        return "¡Hola! Soy el Asistente Virtual de TECNISECURE. Puedo ayudarte con:\n"
                + "• Pedidos: escribe 'mi pedido' o '#número'\n"
                + "• Catálogo: escribe 'precio cámara' o 'stock ssd'\n"
                + "• Servicios técnicos y garantías ('servicios')\n"
                + "• Atención humana: escribe 'hablar con asesor'";
    }

    private String consultarPedidosCliente(Integer idCliente, String msg) {
        List<Pedido> pedidos = pedidoRepository.findByClienteIdCliente(
                idCliente, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "creadoEn"))).getContent();

        Matcher m = PATRON_PEDIDO.matcher(msg);
        if (m.find()) {
            Integer idBuscado = Integer.valueOf(m.group(1));
            var hallado = pedidos.stream().filter(p -> idBuscado.equals(p.getIdPedido())).findFirst();
            if (hallado.isPresent()) {
                Pedido p = hallado.get();
                return String.format("Resumen de tu pedido #%d:\n• Estado actual: %s\n• Total: %s\n• Puedes descargar tu factura o ver la ruta en 'Mis pedidos'.",
                        p.getIdPedido(), p.getEstado(), moneda(p.getTotal()));
            }
            return "No encontré el pedido #" + idBuscado + " asociado a tu cuenta. Verifica el número en 'Mis pedidos'.";
        }

        if (pedidos.isEmpty()) {
            return "Aún no tienes pedidos registrados en tu cuenta. Explora nuestro catálogo para hacer tu primera compra.";
        }
        Pedido ultimo = pedidos.get(0);
        return String.format("Tienes %d pedido(s) registrado(s).\n• Más reciente: #%d\n• Estado: %s\n• Total: %s",
                pedidos.size(), ultimo.getIdPedido(), ultimo.getEstado(), moneda(ultimo.getTotal()));
    }

    private String consultarProductos(String msg) {
        String msgNormalizado = normalizarTexto(msg);
        String[] palabras = msgNormalizado.split("[^a-z0-9]+");
        List<Producto> encontrados = productoRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> {
                    String nom = p.getNombre() != null ? normalizarTexto(p.getNombre()) : "";
                    for (String palabra : palabras) {
                        if (palabra.length() >= 4 && nom.contains(palabra)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(3)
                .toList();

        if (encontrados.isEmpty()) {
            encontrados = productoRepository.findByDestacadoTrueAndActivoTrueOrderByIdProductoAsc()
                    .stream().limit(3).toList();
        }
        if (encontrados.isEmpty()) {
            return "Actualmente estamos actualizando el catálogo. Un asesor te ayudará en breve.";
        }

        StringBuilder sb = new StringBuilder("Disponibilidad en inventario en tiempo real:\n");
        for (Producto p : encontrados) {
            sb.append("• ").append(p.getNombre()).append(": ").append(moneda(p.getPrecioFinal()))
              .append(" (").append(p.getStock()).append(" disp.)\n");
        }
        return sb.toString().trim();
    }

    private String consultarServicios() {
        List<Servicio> servicios = servicioRepository.findAllByOrderByOrdenAsc();
        if (servicios.isEmpty()) {
            return "Ofrecemos instalación de CCTV, soporte técnico de equipos y redes. Agenda tu cita desde la sección Servicios.";
        }
        StringBuilder sb = new StringBuilder("Nuestros servicios técnicos disponibles:\n");
        servicios.stream().limit(4).forEach(s -> sb.append("• ").append(s.getTitulo()).append("\n"));
        sb.append("Puedes solicitar cita o cotización directa en el menú principal.");
        return sb.toString().trim();
    }

    private boolean contiene(String texto, String... claves) {
        for (String c : claves) {
            if (texto.contains(c)) return true;
        }
        return false;
    }

    private String moneda(Number n) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(n != null ? n.doubleValue() : 0);
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
