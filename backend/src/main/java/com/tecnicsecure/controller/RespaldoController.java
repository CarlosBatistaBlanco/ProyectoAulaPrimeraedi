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
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    @Operation(summary = "Descargar respaldo integral en ZIP con streaming directo para bajo consumo de RAM")
    @GetMapping(value = "/completo", produces = "application/zip")
    @Transactional(readOnly = false)
    public void respaldoCompleto(HttpServletResponse response) throws IOException {
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

        // Streaming tabla por tabla directo al OutputStream sin retener todo el ZIP en RAM
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
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

        LogActividad log = new LogActividad();
        log.setIdUsuario(usuarioActual.idOpcional());
        log.setAccion("respaldo_completo");
        log.setDetalle("Descarga de respaldo ZIP completo (" + totalProductos + " productos, " + totalPedidos + " pedidos)");
        logRepository.save(log);
    }

    private void agregarEntradaJson(ZipOutputStream zos, String nombre, Object data) throws IOException {
        zos.putNextEntry(new ZipEntry(nombre));
        mapper.writeValue(zos, data);
        zos.closeEntry();
    }
}
