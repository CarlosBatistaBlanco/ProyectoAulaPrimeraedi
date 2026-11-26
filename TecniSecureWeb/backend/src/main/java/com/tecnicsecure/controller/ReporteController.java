/*
 * Este archivo alimenta las tarjetas de resumen y los botones de exportación en las pantallas
 * de Inicio (tablero principal) y Reportes del panel administrativo. Calcula ingresos, pedidos
 * y productos más vendidos por fechas, y descarga tablas listas para abrir en Excel o programas contables.
 */
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
// Solo administradores y empleados pueden ver las cifras del negocio y descargar reportes
@PreAuthorize("hasAnyRole('ADMINISTRADOR','EMPLEADO')")
public class ReporteController {

    private final ReporteService reporteService;

    // Conecta con el encargado de sumar ventas, calcular ingresos y armar estadísticas por fechas
    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    // Entrega los números principales de las tarjetas del tablero (pedidos, ingresos, productos y usuarios)
    @Operation(summary = "Resumen general del periodo (dashboard)")
    @GetMapping("/resumen")
    public ResumenReporte resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.resumen(desde, hasta);
    }

    // Muestra el listado de los productos más vendidos entre dos fechas (por defecto los 10 primeros)
    @Operation(summary = "Productos mas vendidos")
    @GetMapping("/productos-mas-vendidos")
    public List<ProductoVendido> masVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return reporteService.productosMasVendidos(desde, hasta, limite);
    }

    // Entrega el total de dinero ingresado por ventas en el rango de fechas elegido
    @Operation(summary = "Ingresos del periodo")
    @GetMapping("/ingresos")
    public BigDecimal ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.resumen(desde, hasta).ingresosPeriodo();
    }

    // Descarga la lista de ventas del periodo en hoja de cálculo para Excel o en archivo estructurado XML
    @Operation(summary = "Exportar ventas del periodo en CSV (abre en Excel)")
    @GetMapping(value = "/ventas/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "csv") String formato) {

        List<Pedido> pedidos = reporteService.pedidosDelPeriodo(desde, hasta, null);

        // Si en la pantalla se eligió descargar en formato XML, arma ese documento en lugar de la hoja de Excel
        if ("xml".equalsIgnoreCase(formato)) {
            return descargar("ventas.xml", "application/xml", construirXml(pedidos));
        }

        // Arma la tabla separada por punto y coma con una fila por cada pedido vendido
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
        // Agrega una marca invisible al inicio para que Excel muestre bien las tildes y la letra ñ
        byte[] contenido = ("\uFEFF" + sb).getBytes(StandardCharsets.UTF_8);
        return descargar("ventas.csv", "text/csv;charset=UTF-8", contenido);
    }

    // Descarga una hoja de cálculo con los indicadores resumidos de ventas, inventario y productos por agotarse
    @Operation(summary = "Exportar productos con stock bajo en CSV")
    @GetMapping(value = "/inventario/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportarInventario(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        var resumen = reporteService.resumen(desde, hasta);
        // Arma una tabla sencilla de dos columnas (Indicador y Valor) lista para abrir en Excel
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

    // Prepara un texto respetando las tildes y lo envía al navegador como archivo descargable
    private ResponseEntity<byte[]> descargar(String nombre, String tipo, String texto) {
        return descargar(nombre, tipo, texto.getBytes(StandardCharsets.UTF_8));
    }

    // Indica al navegador que guarde el archivo recibido con el nombre sugerido
    private ResponseEntity<byte[]> descargar(String nombre, String tipo, byte[] contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(tipo))
                .body(contenido);
    }

    // Arma un documento estructurado con todas las ventas del periodo para sistemas externos o contabilidad
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

    // Limpia símbolos especiales en nombres o textos para que el archivo descargado siempre abra sin errores
    private String escapar(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
