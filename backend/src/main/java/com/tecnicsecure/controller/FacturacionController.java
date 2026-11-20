package com.tecnicsecure.controller;

import com.tecnicsecure.dto.pedido.FacturaDetalleResponse;
import com.tecnicsecure.dto.pedido.PedidoResponse;
import com.tecnicsecure.entity.EstadoPedido;
import com.tecnicsecure.entity.Factura;
import com.tecnicsecure.entity.LogActividad;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.FacturaRepository;
import com.tecnicsecure.repository.LogActividadRepository;
import com.tecnicsecure.service.EmailService;
import com.tecnicsecure.service.PedidoService;
import com.tecnicsecure.util.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Tag(name = "3.1 Facturacion y Pagos", description = "Factura formal DIAN, notas credito, descarga individual y envio por correo")
@RestController
@RequestMapping("/api/pedidos")
public class FacturacionController {

    private final PedidoService pedidoService;
    private final FacturaRepository facturaRepository;
    private final LogActividadRepository logRepository;
    private final UsuarioActual usuarioActual;
    private final EmailService emailService;

    public FacturacionController(PedidoService pedidoService,
                                 FacturaRepository facturaRepository,
                                 LogActividadRepository logRepository,
                                 UsuarioActual usuarioActual,
                                 EmailService emailService) {
        this.pedidoService = pedidoService;
        this.facturaRepository = facturaRepository;
        this.logRepository = logRepository;
        this.usuarioActual = usuarioActual;
        this.emailService = emailService;
    }

    @Operation(summary = "Consultar factura formal de un pedido (solo datos necesarios del comprador)")
    @GetMapping("/{id}/factura")
    @Transactional(readOnly = true)
    public FacturaDetalleResponse obtenerFactura(@PathVariable Integer id) {
        PedidoResponse pedido = pedidoService.obtener(id);
        Factura factura = facturaRepository.findByIdPedido(id).orElse(null);

        String numero = factura != null ? factura.getNumeroFactura() : String.format("FAC-%05d", id);
        BigDecimal total = pedido.total() != null ? pedido.total() : BigDecimal.ZERO;
        BigDecimal base = (factura != null && factura.getSubtotal() != null && factura.getSubtotal().compareTo(BigDecimal.ZERO) > 0)
                ? factura.getSubtotal()
                : total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
        BigDecimal iva = total.subtract(base).max(BigDecimal.ZERO);
        BigDecimal descuento = factura != null && factura.getDescuento() != null ? factura.getDescuento() : BigDecimal.ZERO;
        LocalDateTime fecha = factura != null && factura.getCreadoEn() != null ? factura.getCreadoEn() : pedido.creadoEn();
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }

        String cufe = calcularCufe(numero + "|" + id + "|" + total.toPlainString() + "|" + pedido.clienteCorreo());

        return new FacturaDetalleResponse(
                factura != null ? factura.getIdFactura() : id,
                id,
                numero,
                cufe,
                factura != null ? factura.getEstado() : "emitida",
                pedido.estado(),
                fecha,
                "TECNISERVICIOS & TECNOLOGIA TECNISECURE S.A.S.",
                "901.456.789-2",
                "Responsable de IVA - Regimen Comun",
                "Resolucion DIAN No. 18764000001234 (Prefijo FAC del 1 al 100000)",
                pedido.clienteNombre(),
                pedido.clienteCorreo(),
                pedido.clienteTelefono(),
                pedido.clienteDireccion(),
                pedido.metodoPago(),
                base,
                new BigDecimal("19.00"),
                iva,
                descuento,
                total,
                pedido.detalles()
        );
    }

    @Operation(summary = "Descargar comprobante individual de factura en HTML imprimible")
    @GetMapping("/{id}/factura/archivo")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarFactura(@PathVariable Integer id) {
        FacturaDetalleResponse fac = obtenerFactura(id);
        byte[] bytes = construirDocumentoHtmlFactura(fac).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fac.numeroFactura() + ".html\"")
                .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
                .body(bytes);
    }

    @Operation(summary = "Estado del servicio de correo (si esta listo para enviar)")
    @GetMapping("/factura/correo-estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    public Map<String, Object> estadoCorreo() {
        return emailService.estado();
    }

    @Operation(summary = "Enviar la factura al correo que se indique")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/{id}/enviar-factura-a")
    public Map<String, String> enviarFacturaA(@PathVariable Integer id,
                                             @jakarta.validation.Valid @RequestBody
                                             com.tecnicsecure.dto.soporte.EnvioFacturaRequest peticion) {
        FacturaDetalleResponse fac = obtenerFactura(id);
        // La factura va en el cuerpo del correo; no se adjunta ningun archivo.
        String htmlDoc = construirDocumentoHtmlFactura(fac);
        return emailService.enviarCorreoConAdjunto(
                peticion.correo(),
                "Factura electronica " + fac.numeroFactura() + " - TECNISECURE",
                htmlDoc,
                null,
                null,
                fac.numeroFactura() + " | Pedido #" + id + " | Envio manual a " + peticion.correo());
    }

    @Operation(summary = "Enviar unicamente esta factura individual por correo al cliente comprador")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
    @PostMapping("/{id}/enviar-factura")
    public Map<String, String> enviarFactura(@PathVariable Integer id) {
        FacturaDetalleResponse fac = obtenerFactura(id);
        if (!StringUtils.hasText(fac.clienteCorreo())) {
            throw ApiException.badRequest("El cliente no tiene correo registrado");
        }
        // La factura se muestra dentro del correo (mensaje HTML), sin adjuntos.
        String htmlDoc = construirDocumentoHtmlFactura(fac);
        return emailService.enviarCorreoConAdjunto(
                fac.clienteCorreo(),
                "Factura electronica " + fac.numeroFactura() + " - TECNISECURE",
                htmlDoc,
                null,
                null,
                fac.numeroFactura() + " | Pedido #" + id + " | Total " + moneda(fac.total()));
    }

    @Operation(summary = "Procesar pago del pedido mediante pasarela")
    @PostMapping("/{id}/pagar")
    @Transactional
    public Map<String, Object> pagarPedido(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body) {
        PedidoResponse pedido = pedidoService.obtener(id);
        if (EstadoPedido.CANCELADO.equals(pedido.estado())) {
            throw ApiException.badRequest("No se puede pagar un pedido cancelado");
        }

        String metodo = (body != null && StringUtils.hasText(body.get("metodo")))
                ? body.get("metodo").trim()
                : (StringUtils.hasText(pedido.metodoPago()) ? pedido.metodoPago() : "Transferencia / Tarjeta");
        String referencia = "PAY-" + id + "-" + (System.currentTimeMillis() % 1000000);

        Factura factura = facturaRepository.findByIdPedido(id).orElseGet(() -> {
            Factura f = new Factura();
            f.setIdPedido(id);
            f.setNumeroFactura(String.format("FAC-%05d", id));
            BigDecimal total = pedido.total() != null ? pedido.total() : BigDecimal.ZERO;
            BigDecimal base = total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
            f.setSubtotal(base);
            f.setImpuestos(total.subtract(base));
            f.setTotal(total);
            return f;
        });
        factura.setEstado("pagada");
        facturaRepository.save(factura);

        String nuevoEstado = pedido.estado();
        if (EstadoPedido.PENDIENTE.equals(pedido.estado())) {
            var actualizado = pedidoService.cambiarEstado(
                    id,
                    EstadoPedido.CONFIRMADO,
                    "Pago confirmado por " + metodo + " (Ref: " + referencia + ")",
                    usuarioActual.idOpcional());
            nuevoEstado = actualizado.estado();
        }

        registrarLog("pago_pedido", "Pedido #" + id + " pagado via " + metodo + " Ref=" + referencia);

        return Map.of(
                "idPedido", id,
                "referencia", referencia,
                "metodo", metodo,
                "estadoPago", "APROBADO",
                "estadoPedido", nuevoEstado,
                "numeroFactura", factura.getNumeroFactura()
        );
    }

    @Operation(summary = "Emitir nota credito o ajuste administrativo de factura (auditable, sin borrar historial)")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/{id}/nota-credito")
    @Transactional
    public Map<String, String> emitirNotaCredito(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String motivo = body != null && StringUtils.hasText(body.get("motivo"))
                ? body.get("motivo").trim()
                : "Ajuste administrativo solicitado por gerencia";

        Factura factura = facturaRepository.findByIdPedido(id)
                .orElseThrow(() -> ApiException.notFound("No existe factura generada para este pedido"));
        factura.setEstado("nota_credito");
        facturaRepository.save(factura);

        registrarLog("nota_credito_factura", "Factura " + factura.getNumeroFactura() + " | Motivo: " + motivo);
        return Map.of("estado", "nota_credito", "numeroFactura", factura.getNumeroFactura(), "motivo", motivo);
    }

    /** Documento autocontenido exclusivo de esta factura (sin datos de otros clientes ni costos internos). */
    private String construirDocumentoHtmlFactura(FacturaDetalleResponse f) {
        StringBuilder filas = new StringBuilder();
        if (f.items() != null) {
            for (var it : f.items()) {
                filas.append("<tr><td>").append(esc(it.producto())).append("</td>")
                     .append("<td style='text-align:center'>").append(it.cantidad()).append("</td>")
                     .append("<td style='text-align:right'>").append(moneda(it.precioUnitario())).append("</td>")
                     .append("<td style='text-align:right'>").append(moneda(it.subtotal())).append("</td></tr>");
            }
        }
        String fecha = f.fechaEmision() != null
                ? f.fechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "";
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>%s - TECNISECURE</title>
              <style>
                body{font-family:Arial,Helvetica,sans-serif;color:#111;margin:0;padding:24px;background:#f5f7fa}
                .marco{max-width:720px;margin:0 auto;background:#ffffff;border:1px solid #dde3ea;border-radius:10px;padding:26px}
                .cab{border-bottom:2px solid #0a192f;padding-bottom:12px;margin-bottom:16px}
                .cab-derecha{margin-top:10px}
                table{width:100%%;border-collapse:collapse;margin:14px 0}
                th,td{border-bottom:1px solid #ddd;padding:9px 8px;text-align:left;font-size:14px}
                th{background:#f3f6fa}
                .tot{text-align:right;line-height:1.8;margin-top:8px}
                .cufe{margin-top:18px;font-size:11px;color:#555;word-break:break-all;border-top:1px dashed #ccc;padding-top:12px}
                @media (max-width:520px){
                  body{padding:10px}
                  .marco{padding:16px}
                  th,td{font-size:12px;padding:6px 4px}
                }
              </style>
            </head>
            <body>
              <div class="marco">
              <div class="cab">
                <div>
                  <h2 style="margin:0 0 4px 0">%s</h2>
                  <div>NIT: %s · %s</div>
                  <div style="font-size:12px;color:#555">%s</div>
                </div>
                <div class="cab-derecha">
                  <h3 style="margin:0 0 4px 0;color:#0077b6">FACTURA %s</h3>
                  <div>Fecha: %s</div>
                  <div>Estado: <strong>%s</strong></div>
                </div>
              </div>
              <div style="margin-bottom:12px;font-size:14px;line-height:1.7">
                <strong>Comprador:</strong> %s<br>
                <strong>Correo:</strong> %s<br>
                <strong>Dirección entrega:</strong> %s<br>
                <strong>Pago:</strong> %s
              </div>
              <table>
                <thead><tr><th>Producto</th><th style="text-align:center">Cant.</th><th style="text-align:right">V. Unitario</th><th style="text-align:right">Subtotal</th></tr></thead>
                <tbody>%s</tbody>
              </table>
              <div class="tot">
                <div>Base gravable: <strong>%s</strong></div>
                <div>IVA (%s%%): <strong>%s</strong></div>
                <div style="font-size:18px;margin-top:6px">TOTAL: <strong>%s</strong></div>
              </div>
              <div class="cufe"><strong>CUFE:</strong> %s</div>
              <p style="margin-top:20px;font-size:12px;color:#777;text-align:center">
                Gracias por confiar en TECNISECURE. Este mensaje fue generado automáticamente.
              </p>
              </div>
            </body>
            </html>
            """.formatted(
                esc(f.numeroFactura()),
                esc(f.emisorRazonSocial()), esc(f.emisorNit()), esc(f.emisorRegimen()), esc(f.resolucionDian()),
                esc(f.numeroFactura()), esc(fecha), esc((f.estadoFactura() != null ? f.estadoFactura() : "emitida").toUpperCase()),
                esc(f.clienteNombre()), esc(f.clienteCorreo()),
                esc(StringUtils.hasText(f.clienteDireccion()) ? f.clienteDireccion() : "Recoge en tienda"),
                esc(StringUtils.hasText(f.metodoPago()) ? f.metodoPago() : "Por confirmar"),
                filas.toString(),
                moneda(f.subtotalBase()),
                f.ivaPct() != null ? f.ivaPct().toPlainString() : "19",
                moneda(f.ivaValor()),
                moneda(f.total()),
                esc(f.cufe()));
    }

    private String moneda(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(v != null ? v.doubleValue() : 0);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void registrarLog(String accion, String detalle) {
        LogActividad log = new LogActividad();
        log.setIdUsuario(usuarioActual.idOpcional());
        log.setAccion(accion);
        log.setDetalle(detalle);
        logRepository.save(log);
    }

    private String calcularCufe(String semilla) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(semilla.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return Long.toHexString(semilla.hashCode()).toUpperCase();
        }
    }
}
