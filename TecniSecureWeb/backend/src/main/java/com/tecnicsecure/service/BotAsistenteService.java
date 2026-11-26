/*
 * Este archivo da respuestas automaticas inmediatas dentro del chat privado de los clientes
 * que ya iniciaron sesion. Sin depender de servicios externos ni costos extra, revisa los
 * propios pedidos del cliente, los precios reales de la tienda y los servicios tecnicos, y
 * muestra la respuesta al instante en su pantalla de soporte.
 */
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

@Service
public class BotAsistenteService {

    // Reconoce cuando el cliente escribe un numero de pedido como #123 dentro del mensaje.
    private static final Pattern PATRON_PEDIDO = Pattern.compile("#(\\d+)");

    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final ServicioRepository servicioRepository;
    private final MensajeChatRepository chatRepository;
    private final SimpMessagingTemplate mensajeria;

    // Conecta el asistente automatico con el catalogo, los pedidos, los servicios y el chat en vivo.
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

    // Detecta si el cliente pidio hablar con una persona real o quiere presentar un reclamo.
    public boolean esSolicitudAsesor(String mensajeRaw) {
        if (mensajeRaw == null || mensajeRaw.isBlank()) {
            return false;
        }
        String msg = normalizarTexto(mensajeRaw);
        return contiene(msg, "asesor", "humano", "persona", "agente", "operador", "reclamo", "queja");
    }

    // Lee el mensaje del cliente, prepara una respuesta automatica, la guarda en el chat y la muestra al instante.
    public MensajeChatResponse responderAutomatico(Integer idCliente, String mensajeCliente) {
        if (idCliente == null || mensajeCliente == null || mensajeCliente.isBlank()) {
            return null;
        }
        String respuestaTexto = generarRespuesta(idCliente, mensajeCliente);
        if (respuestaTexto == null) {
            return null;
        }

        // Guarda el mensaje automatico en el historial de la conversacion marcandolo como asistente virtual.
        MensajeChat bot = new MensajeChat();
        bot.setIdCliente(idCliente);
        bot.setRemitente("asesor");
        bot.setIdUsuarioEnvia(null);
        bot.setMensaje("[Asistente Virtual] " + respuestaTexto);
        bot.setLeido(false);
        chatRepository.save(bot);

        // Envia la respuesta en vivo a la ventana de chat del cliente para que la vea sin recargar.
        MensajeChatResponse dto = MensajeChatResponse.de(bot);
        if (mensajeria != null) {
            mensajeria.convertAndSend("/topic/chat/" + idCliente, dto);
        }
        return dto;
    }

    // Elige la respuesta adecuada segun el tema de la pregunta: asesor humano, pedidos, productos, servicios u horarios.
    public String generarRespuesta(Integer idCliente, String mensajeRaw) {
        String msg = mensajeRaw.trim().toLowerCase(Locale.ROOT);

        // 1) Si el cliente pide un humano, avisa que su chat queda con prioridad para un asesor.
        if (esSolicitudAsesor(msg)) {
            return "Hemos conectado tu chat con prioridad hacia un asesor humano de TECNISECURE.\n"
                    + "• Puedes escribir aquí todos los detalles (número de pedido, producto o consulta) y el asesor los verá de inmediato.";
        }

        // 2) Si pregunta por un pedido, envio o numero de orden (#123), consulta sus compras reales.
        if (contiene(msg, "pedido", "orden", "envio", "envío", "rastreo", "guia", "guía")
                || PATRON_PEDIDO.matcher(msg).find()) {
            return consultarPedidosCliente(idCliente, msg);
        }

        // 3) Si pregunta por precios, disponibilidad o equipos, busca directamente en la bodega.
        if (contiene(msg, "precio", "vale", "cuesta", "stock", "disponible", "camara", "cámara",
                "disco", "ssd", "router", "ups", "producto", "catalogo", "catálogo", "comprar")) {
            return consultarProductos(msg);
        }

        // 4) Si pregunta por instalaciones, mantenimiento, garantias o citas, muestra los servicios de la empresa.
        if (contiene(msg, "servicio", "instalacion", "instalación", "mantenimiento", "tecnico",
                "técnico", "garantia", "garantía", "cita", "cotizacion", "cotización")) {
            return consultarServicios();
        }

        // 5) Si pregunta por horarios o datos de contacto, entrega la informacion oficial de atencion.
        if (contiene(msg, "horario", "hora", "ubicacion", "ubicación", "telefono", "teléfono",
                "correo", "contacto", "direccion", "dirección")) {
            return "Información de atención TECNISECURE:\n"
                    + "• Horario: Lunes a Sábado de 8:00 a.m. a 6:00 p.m.\n"
                    + "• Soporte: soporte@tecnisecure.com\n"
                    + "• Garantía mínima de 12 meses y factura electrónica DIAN.";
        }

        // 6) Si el mensaje es general o un saludo, muestra un menu rapido con ejemplos de lo que puede preguntar.
        return "¡Hola! Soy el Asistente Virtual de TECNISECURE. Puedo ayudarte con:\n"
                + "• Pedidos: escribe 'mi pedido' o '#número'\n"
                + "• Catálogo: escribe 'precio cámara' o 'stock ssd'\n"
                + "• Servicios técnicos y garantías ('servicios')\n"
                + "• Atención humana: escribe 'hablar con asesor'";
    }

    // Revisa los ultimos 5 pedidos propios del cliente (o el numero # que escribio) para darle su estado y valor total.
    private String consultarPedidosCliente(Integer idCliente, String msg) {
        List<Pedido> pedidos = pedidoRepository.findByClienteIdCliente(
                idCliente, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "creadoEn"))).getContent();

        // Si el cliente indico un numero especifico (#123), verifica que realmente le pertenezca antes de mostrarlo.
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

    // Palabras comunes que se ignoran al buscar productos para enfocarse solo en el nombre del equipo.
    private static final java.util.Set<String> STOP_WORDS = java.util.Set.of(
            "hola", "cuanto", "vale", "cuesta", "precio", "stock", "disponible",
            "producto", "productos", "catalogo", "comprar", "tienen", "para", "como", "quiero"
    );

    // Toma las palabras clave del mensaje y busca hasta 3 productos reales con su precio y unidades disponibles.
    private String consultarProductos(String msg) {
        String msgNormalizado = normalizarTexto(msg);
        String[] palabras = msgNormalizado.split("[^a-z0-9]+");
        List<String> terminos = java.util.Arrays.stream(palabras)
                .filter(p -> p.length() >= 4 && !STOP_WORDS.contains(p))
                .distinct()
                .limit(4)
                .toList();

        java.util.LinkedHashMap<Integer, Producto> mapa = new java.util.LinkedHashMap<>();
        // Paso 1: busca directamente los primeros equipos que coincidan para responder rapido sin recorrer toda la tienda.
        for (String termino : terminos) {
            var pagina = productoRepository.buscar(termino, null, null, PageRequest.of(0, 3));
            if (pagina != null && pagina.hasContent()) {
                for (Producto p : pagina.getContent()) {
                    mapa.putIfAbsent(p.getIdProducto() != null ? p.getIdProducto() : System.identityHashCode(p), p);
                    if (mapa.size() >= 3) break;
                }
            }
            if (mapa.size() >= 3) break;
        }

        // Paso 2: si no encontro nada (por ejemplo, si el cliente escribio "camara" sin tilde), revisa hasta 50 equipos ignorando tildes.
        if (mapa.isEmpty() && !terminos.isEmpty()) {
            var muestra = productoRepository.buscar(null, null, null, PageRequest.of(0, 50));
            if (muestra != null && muestra.hasContent()) {
                for (Producto p : muestra.getContent()) {
                    String nom = p.getNombre() != null ? normalizarTexto(p.getNombre()) : "";
                    for (String termino : terminos) {
                        if (nom.contains(termino)) {
                            mapa.putIfAbsent(p.getIdProducto() != null ? p.getIdProducto() : System.identityHashCode(p), p);
                            break;
                        }
                    }
                    if (mapa.size() >= 3) break;
                }
            }
        }

        List<Producto> encontrados = new java.util.ArrayList<>(mapa.values());

        // Si tampoco hubo coincidencias, muestra hasta 3 equipos destacados como recomendacion.
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

    // Resume hasta 4 servicios tecnicos disponibles para que el cliente sepa que puede agendar.
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

    // Revisa si el mensaje contiene alguna de las palabras buscadas.
    private boolean contiene(String texto, String... claves) {
        for (String c : claves) {
            if (texto.contains(c)) return true;
        }
        return false;
    }

    // Da formato de pesos colombianos a los precios y totales de pedido.
    private String moneda(Number n) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(n != null ? n.doubleValue() : 0);
    }

    // Quita tildes y pasa todo a minusculas para reconocer palabras aunque esten escritas sin acentos.
    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }
}
