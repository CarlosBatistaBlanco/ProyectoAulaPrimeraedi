/*
 * Este archivo atiende el botón de Descargar Respaldo Completo dentro del panel de
 * administrador. Empaqueta toda la información de la tienda (usuarios, productos, ventas,
 * facturas, bodega, soporte y tareas) en un archivo comprimido que se envía por partes sin llenar la memoria.
 */
package com.tecnicsecure.controller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tecnicsecure.dto.auth.UsuarioResponse;
import com.tecnicsecure.dto.pedido.PedidoResponse;
import com.tecnicsecure.entity.LogActividad;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.util.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(name = "8. Respaldo", description = "Copia de seguridad integral de la base de datos en formato ZIP (streaming)")
@RestController
@RequestMapping("/api/respaldo")
// Solo el administrador tiene permiso para descargar una copia completa de toda la tienda
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RespaldoController {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final PedidoRepository pedidoRepository;
    private final FacturaRepository facturaRepository;
    private final InventarioMovimientoRepository movimientoRepository;
    private final SolicitudServicioRepository solicitudRepository;
    private final TareaEmpleadoRepository tareaRepository;
    private final TareaHistorialEntregaRepository tareaHistorialRepository;
    private final LogActividadRepository logRepository;
    private final UsuarioActual usuarioActual;
    private final ObjectMapper mapper;

    // Conecta con todas las áreas de datos de la tienda y prepara el formato legible para guardar cada archivo
    public RespaldoController(UsuarioRepository usuarioRepository,
                              ClienteRepository clienteRepository,
                              ProductoRepository productoRepository,
                              CategoriaRepository categoriaRepository,
                              MarcaRepository marcaRepository,
                              PedidoRepository pedidoRepository,
                              FacturaRepository facturaRepository,
                              InventarioMovimientoRepository movimientoRepository,
                              SolicitudServicioRepository solicitudRepository,
                              TareaEmpleadoRepository tareaRepository,
                              TareaHistorialEntregaRepository tareaHistorialRepository,
                              LogActividadRepository logRepository,
                              UsuarioActual usuarioActual) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.pedidoRepository = pedidoRepository;
        this.facturaRepository = facturaRepository;
        this.movimientoRepository = movimientoRepository;
        this.solicitudRepository = solicitudRepository;
        this.tareaRepository = tareaRepository;
        this.tareaHistorialRepository = tareaHistorialRepository;
        this.logRepository = logRepository;
        this.usuarioActual = usuarioActual;
        // Prepara el escritor de archivos con fechas fáciles de leer y manteniéndolo abierto hasta terminar el paquete
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    // Genera y descarga al instante un paquete comprimido con cada bloque de datos en un archivo ordenado y un resumen
    @Operation(summary = "Descargar respaldo integral en ZIP con streaming directo para bajo consumo de RAM")
    @GetMapping(value = "/completo", produces = "application/zip")
    @Transactional(readOnly = false)
    public void respaldoCompleto(HttpServletResponse response) throws IOException {
        // Arma el nombre del archivo comprimido usando la fecha y hora del momento
        String fechaNombre = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"tecnicsecure-respaldo-" + fechaNombre + ".zip\"");

        int totalUsuarios;
        int totalClientes;
        int totalProductos;
        int totalPedidos;
        int totalFacturas;
        int totalMovimientos;
        int totalSolicitudes;
        int totalTareas;
        int totalEntregas;
        int totalLogs;

        // Va enviando cada bloque por partes directo a la descarga para no saturar la memoria del servidor
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
            // 1. Guarda el listado de cuentas de usuarios y fichas de clientes
            var usuarios = usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
            totalUsuarios = usuarios.size();
            agregarEntradaJson(zos, "usuarios.json", usuarios);

            var clientes = clienteRepository.findAll().stream().map(c -> Map.of(
                    "idCliente", c.getIdCliente(),
                    "nombre", String.valueOf(c.getNombre()),
                    "correo", String.valueOf(c.getCorreo()),
                    "telefono", c.getTelefono() != null ? c.getTelefono() : "",
                    "direccion", c.getDireccion() != null ? c.getDireccion() : ""
            )).toList();
            totalClientes = clientes.size();
            agregarEntradaJson(zos, "clientes.json", clientes);

            // 2. Guarda el catálogo completo: categorías, marcas y productos con precios y existencias
            agregarEntradaJson(zos, "categorias.json", categoriaRepository.findAll());
            agregarEntradaJson(zos, "marcas.json", marcaRepository.findAll());

            var productos = productoRepository.findAll().stream().map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idProducto", p.getIdProducto());
                m.put("nombre", p.getNombre());
                m.put("sku", p.getSku());
                m.put("precio", p.getPrecio());
                m.put("stock", p.getStock());
                m.put("activo", Boolean.TRUE.equals(p.getActivo()));
                return m;
            }).toList();
            totalProductos = productos.size();
            agregarEntradaJson(zos, "productos.json", productos);

            // 3. Guarda todas las compras, facturas y movimientos de entradas y salidas de bodega
            var pedidos = pedidoRepository.findAll().stream().map(PedidoResponse::de).toList();
            totalPedidos = pedidos.size();
            agregarEntradaJson(zos, "pedidos.json", pedidos);

            var facturas = facturaRepository.findAll();
            totalFacturas = facturas.size();
            agregarEntradaJson(zos, "facturas.json", facturas);

            var movimientos = movimientoRepository.findAll().stream().map(mv -> Map.of(
                    "idMovimiento", mv.getIdMovimiento(),
                    "idProducto", mv.getIdProducto() != null ? mv.getIdProducto() : 0,
                    "tipo", String.valueOf(mv.getTipo()),
                    "cantidad", mv.getCantidad(),
                    "stockResultante", mv.getStockResultante(),
                    "motivo", mv.getMotivo() != null ? mv.getMotivo() : "",
                    "creadoEn", String.valueOf(mv.getCreadoEn())
            )).toList();
            totalMovimientos = movimientos.size();
            agregarEntradaJson(zos, "inventario_movimientos.json", movimientos);

            // 4. Guarda solicitudes de soporte técnico, tareas del equipo y registro histórico de actividad
            var solicitudes = solicitudRepository.findAll().stream().map(s -> Map.of(
                    "idSolicitud", s.getIdSolicitud(),
                    "servicio", s.getServicio() != null ? s.getServicio().getTitulo() : "",
                    "nombre", String.valueOf(s.getNombre()),
                    "tipo", String.valueOf(s.getTipo()),
                    "estado", String.valueOf(s.getEstado()),
                    "tecnicoAsignado", s.getTecnicoAsignado() != null ? s.getTecnicoAsignado() : "",
                    "creadoEn", String.valueOf(s.getCreadoEn())
            )).toList();
            totalSolicitudes = solicitudes.size();
            agregarEntradaJson(zos, "solicitudes_servicio.json", solicitudes);

            var tareas = tareaRepository.findAllByOrderByCreadoEnDesc();
            totalTareas = tareas.size();
            agregarEntradaJson(zos, "tareas_empleados.json", tareas);

            var entregas = tareaHistorialRepository.findAll();
            totalEntregas = entregas.size();
            agregarEntradaJson(zos, "tareas_historial_entregas.json", entregas);

            var logs = logRepository.findAll();
            totalLogs = logs.size();
            agregarEntradaJson(zos, "auditoria.json", logs);

            // 5. Añade una nota resumen (MANIFIESTO.txt) con la fecha y cuántos registros trae el respaldo
            String manifiesto = "TECNISECURE - RESPALDO INTEGRAL DE BASE DE DATOS\n"
                    + "Fecha de generacion: " + LocalDateTime.now() + "\n"
                    + "Usuarios: " + totalUsuarios + "\n"
                    + "Clientes: " + totalClientes + "\n"
                    + "Productos: " + totalProductos + "\n"
                    + "Pedidos: " + totalPedidos + "\n"
                    + "Facturas: " + totalFacturas + "\n"
                    + "Movimientos inventario: " + totalMovimientos + "\n"
                    + "Solicitudes soporte: " + totalSolicitudes + "\n"
                    + "Tareas empleados: " + totalTareas + "\n"
                    + "Historial entregas tareas: " + totalEntregas + "\n"
                    + "Eventos auditoria: " + totalLogs + "\n";
            zos.putNextEntry(new ZipEntry("MANIFIESTO.txt"));
            zos.write(manifiesto.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.finish();
        }

        // Anota en el registro de actividad quién descargó esta copia de seguridad
        LogActividad log = new LogActividad();
        log.setIdUsuario(usuarioActual.idOpcional());
        log.setAccion("respaldo_completo");
        log.setDetalle("Descarga de respaldo ZIP completo (" + totalProductos + " productos, " + totalPedidos + " pedidos)");
        logRepository.save(log);
    }

    // Escribe una lista de datos como un archivo individual dentro del paquete comprimido
    private void agregarEntradaJson(ZipOutputStream zos, String nombre, Object data) throws IOException {
        zos.putNextEntry(new ZipEntry(nombre));
        mapper.writeValue(zos, data);
        zos.closeEntry();
    }
}
