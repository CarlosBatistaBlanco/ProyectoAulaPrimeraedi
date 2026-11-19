package com.tecnicsecure.controller;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Tag(name = "8. Respaldo", description = "Copia de seguridad integral de la base de datos en formato ZIP")
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
        this.logRepository = logRepository;
        this.usuarioActual = usuarioActual;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Operation(summary = "Descargar respaldo integral en ZIP con las tablas principales en JSON")
    @GetMapping(value = "/completo", produces = "application/zip")
    @Transactional(readOnly = false)
    public ResponseEntity<byte[]> respaldoCompleto() throws IOException {
        var usuarios = usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
        var clientes = clienteRepository.findAll().stream().map(c -> Map.of(
                "idCliente", c.getIdCliente(),
                "nombre", String.valueOf(c.getNombre()),
                "correo", String.valueOf(c.getCorreo()),
                "telefono", c.getTelefono() != null ? c.getTelefono() : "",
                "direccion", c.getDireccion() != null ? c.getDireccion() : ""
        )).toList();
        var categorias = categoriaRepository.findAll();
        var marcas = marcaRepository.findAll();
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
        var pedidos = pedidoRepository.findAll().stream().map(PedidoResponse::de).toList();
        var facturas = facturaRepository.findAll();
        var movimientos = movimientoRepository.findAll().stream().map(mv -> Map.of(
                "idMovimiento", mv.getIdMovimiento(),
                "idProducto", mv.getIdProducto() != null ? mv.getIdProducto() : 0,
                "tipo", String.valueOf(mv.getTipo()),
                "cantidad", mv.getCantidad(),
                "stockResultante", mv.getStockResultante(),
                "motivo", mv.getMotivo() != null ? mv.getMotivo() : "",
                "creadoEn", String.valueOf(mv.getCreadoEn())
        )).toList();
        var solicitudes = solicitudRepository.findAll().stream().map(s -> Map.of(
                "idSolicitud", s.getIdSolicitud(),
                "servicio", s.getServicio() != null ? s.getServicio().getTitulo() : "",
                "nombre", String.valueOf(s.getNombre()),
                "tipo", String.valueOf(s.getTipo()),
                "estado", String.valueOf(s.getEstado()),
                "tecnicoAsignado", s.getTecnicoAsignado() != null ? s.getTecnicoAsignado() : "",
                "creadoEn", String.valueOf(s.getCreadoEn())
        )).toList();
        var logs = logRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            agregarEntradaJson(zos, "usuarios.json", usuarios);
            agregarEntradaJson(zos, "clientes.json", clientes);
            agregarEntradaJson(zos, "categorias.json", categorias);
            agregarEntradaJson(zos, "marcas.json", marcas);
            agregarEntradaJson(zos, "productos.json", productos);
            agregarEntradaJson(zos, "pedidos.json", pedidos);
            agregarEntradaJson(zos, "facturas.json", facturas);
            agregarEntradaJson(zos, "inventario_movimientos.json", movimientos);
            agregarEntradaJson(zos, "solicitudes_servicio.json", solicitudes);
            agregarEntradaJson(zos, "auditoria.json", logs);

            String manifiesto = "TECNISECURE - RESPALDO INTEGRAL DE BASE DE DATOS\n"
                    + "Fecha de generacion: " + LocalDateTime.now() + "\n"
                    + "Usuarios: " + usuarios.size() + "\n"
                    + "Clientes: " + clientes.size() + "\n"
                    + "Productos: " + productos.size() + "\n"
                    + "Pedidos: " + pedidos.size() + "\n"
                    + "Facturas: " + facturas.size() + "\n"
                    + "Movimientos inventario: " + movimientos.size() + "\n"
                    + "Solicitudes soporte: " + solicitudes.size() + "\n"
                    + "Eventos auditoria: " + logs.size() + "\n";
            zos.putNextEntry(new ZipEntry("MANIFIESTO.txt"));
            zos.write(manifiesto.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        LogActividad log = new LogActividad();
        log.setIdUsuario(usuarioActual.idOpcional());
        log.setAccion("respaldo_completo");
        log.setDetalle("Descarga de respaldo ZIP completo (" + productos.size() + " productos, " + pedidos.size() + " pedidos)");
        logRepository.save(log);

        String fechaNombre = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"tecnicsecure-respaldo-" + fechaNombre + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(baos.toByteArray());
    }

    private void agregarEntradaJson(ZipOutputStream zos, String nombre, Object data) throws IOException {
        zos.putNextEntry(new ZipEntry(nombre));
        zos.write(mapper.writeValueAsBytes(data));
        zos.closeEntry();
    }
}
