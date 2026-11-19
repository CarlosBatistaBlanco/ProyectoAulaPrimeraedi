package com.tecnicsecure.controller;

import com.tecnicsecure.dto.reporte.ProductoVendido;
import com.tecnicsecure.dto.reporte.ResumenReporte;
import com.tecnicsecure.entity.Pedido;
import com.tecnicsecure.service.ReporteService;
import com.tecnicsecure.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "6. Reportes", description = "Ventas, inventario y productos mas vendidos (JSON, CSV, XML)")
@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Operation(summary = "Resumen general del periodo (dashboard)")
    @GetMapping("/resumen")
    public ResumenReporte resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.resumen(desde, hasta);
    }

    @Operation(summary = "Productos mas vendidos")
    @GetMapping("/productos-mas-vendidos")
    public List<ProductoVendido> masVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return reporteService.productosMasVendidos(desde, hasta, limite);
    }

    @Operation(summary = "Ingresos del periodo")
    @GetMapping("/ingresos")
    public BigDecimal ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.resumen(desde, hasta).ingresosPeriodo();
    }

    @Operation(summary = "Exportar ventas del periodo en CSV (abre en Excel)")
    @GetMapping(value = "/ventas/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "csv") String formato) {

        List<Pedido> pedidos = reporteService.pedidosDelPeriodo(desde, hasta, null);

        if ("xml".equalsIgnoreCase(formato)) {
            return descargar("ventas.xml", "application/xml", construirXml(pedidos));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Pedido;Fecha;Cliente;Correo;Estado;Total\r\n");
        for (Pedido p : pedidos) {
            sb.append(p.getIdPedido()).append(';')
              .append(p.getCreadoEn()).append(';')
              .append(p.getCliente() != null ? p.getCliente().getNombre() : "").append(';')
              .append(p.getCliente() != null ? p.getCliente().getCorreo() : "").append(';')
              .append(p.getEstado()).append(';')
              .append(p.getTotal()).append("\r\n");
        }
        // BOM UTF-8 para que Excel respete los acentos
        byte[] contenido = ("\uFEFF" + sb).getBytes(StandardCharsets.UTF_8);
        return descargar("ventas.csv", "text/csv;charset=UTF-8", contenido);
    }

    @Operation(summary = "Exportar productos con stock bajo en CSV")
    @GetMapping(value = "/inventario/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportarInventario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        var resumen = reporteService.resumen(desde, hasta);
        StringBuilder sb = new StringBuilder();
        sb.append("Indicador;Valor\r\n");
        sb.append("Pedidos del periodo;").append(resumen.cantidadPedidos()).append("\r\n");
        sb.append("Ingresos del periodo;").append(resumen.ingresosPeriodo()).append("\r\n");
        sb.append("Ingresos historicos;").append(resumen.ingresosHistoricos()).append("\r\n");
        sb.append("Productos activos;").append(resumen.productosActivos()).append("\r\n");
        sb.append("Productos con stock bajo;").append(resumen.productosStockBajo()).append("\r\n");
        sb.append("Usuarios activos;").append(resumen.usuariosActivos()).append("\r\n");
        byte[] contenido = ("\uFEFF" + sb).getBytes(StandardCharsets.UTF_8);
        return descargar("inventario.csv", "text/csv;charset=UTF-8", contenido);
    }

    private ResponseEntity<byte[]> descargar(String nombre, String tipo, String texto) {
        return descargar(nombre, tipo, texto.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<byte[]> descargar(String nombre, String tipo, byte[] contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(tipo))
                .body(contenido);
    }

    private String construirXml(List<Pedido> pedidos) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<ventas>\n");
        for (Pedido p : pedidos) {
            xml.append("  <pedido id=\"").append(p.getIdPedido()).append("\">")
               .append("<fecha>").append(p.getCreadoEn()).append("</fecha>")
               .append("<estado>").append(p.getEstado()).append("</estado>")
               .append("<cliente>").append(escapar(p.getCliente() != null ? p.getCliente().getNombre() : ""))
               .append("</cliente>")
               .append("<total>").append(p.getTotal()).append("</total>")
               .append("</pedido>\n");
        }
        return xml.append("</ventas>").toString();
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
