package com.tecnicsecure.service.impl;

import com.tecnicsecure.entity.*;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.*;
import com.tecnicsecure.service.EmpleadoGestionService;
import com.tecnicsecure.service.SoporteService;
import com.tecnicsecure.util.UsuarioActual;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmpleadoGestionServiceImpl implements EmpleadoGestionService {

    public static final List<String> AREAS_VALIDAS = List.of(
            "ATENCION_CLIENTE", "CONTABLE", "OPERATIVO", "SISTEMAS",
            "ASESOR", "VENTAS", "MARKETING", "MENSAJERO", "GERENTE"
    );

    public static final List<String> MODULOS_VALIDOS = List.of(
            "dashboard", "tareas", "perfil", "ventas", "pedidos",
            "inventario", "productos", "soporte", "chat"
    );

    private static final List<String> ESTADOS_ACTIVOS = List.of("pendiente", "en_progreso");

    private static final Map<String, List<Map<String, String>>> ACTIVIDADES_AREA = crearMapaActividades();

    private static Map<String, List<Map<String, String>>> crearMapaActividades() {
        Map<String, List<Map<String, String>>> m = new LinkedHashMap<>();
        m.put("ATENCION_CLIENTE", List.of(
                Map.of("titulo", "Atender solicitudes de asesor humano en chat", "prioridad", "alta", "descripcion", "Revisar bandeja de chat en vivo, responder consultas y confirmar datos del cliente."),
                Map.of("titulo", "Gestionar mensajes de contacto y garantías", "prioridad", "media", "descripcion", "Responder formulario de contacto y dejar registro de atención en soporte."),
                Map.of("titulo", "Confirmar citas y cotizaciones pendientes", "prioridad", "media", "descripcion", "Validar disponibilidad técnica y asignar fecha de seguimiento.")
        ));
        m.put("CONTABLE", List.of(
                Map.of("titulo", "Conciliar facturas DIAN y pedidos confirmados", "prioridad", "alta", "descripcion", "Verificar numeración FAC, base gravable, IVA 19% y soporte de pago."),
                Map.of("titulo", "Auditar notas crédito y ajustes del día", "prioridad", "media", "descripcion", "Validar justificación y soporte contable de cada ajuste."),
                Map.of("titulo", "Generar cierre de ventas CSV/XML diario", "prioridad", "media", "descripcion", "Exportar reporte financiero del día y adjuntar archivo de comprobación.")
        ));
        m.put("OPERATIVO", List.of(
                Map.of("titulo", "Alistamiento y verificación física de equipos", "prioridad", "alta", "descripcion", "Probar cámaras/equipos antes de despacho y registrar número de serie."),
                Map.of("titulo", "Control de stock mínimo y movimientos Kardex", "prioridad", "media", "descripcion", "Validar existencias en bodega y reportar entradas o ajustes."),
                Map.of("titulo", "Ejecutar orden de instalación técnica asignada", "prioridad", "alta", "descripcion", "Adjuntar acta firmada o fotografía del montaje realizado.")
        ));
        m.put("SISTEMAS", List.of(
                Map.of("titulo", "Verificar integridad de copias de seguridad ZIP", "prioridad", "alta", "descripcion", "Generar respaldo completo de BD y confirmar lectura correcta de tablas."),
                Map.of("titulo", "Revisar logs de auditoría y accesos sospechosos", "prioridad", "alta", "descripcion", "Inspeccionar eventos de bloqueo o soporte maestro y reportar novedades."),
                Map.of("titulo", "Monitorear catálogo e imágenes de productos", "prioridad", "baja", "descripcion", "Validar enlaces y fotografías cargadas en la plataforma.")
        ));
        m.put("ASESOR", List.of(
                Map.of("titulo", "Asesorar cotizaciones técnicas de CCTV y redes", "prioridad", "alta", "descripcion", "Elaborar recomendación según metraje y necesidades del cliente."),
                Map.of("titulo", "Seguimiento postventa de pedidos entregados", "prioridad", "media", "descripcion", "Confirmar satisfacción y ofrecer mantenimiento preventivo.")
        ));
        m.put("VENTAS", List.of(
                Map.of("titulo", "Registrar ventas presenciales y telefónicas", "prioridad", "alta", "descripcion", "Verificar stock, método de pago y dirección de entrega."),
                Map.of("titulo", "Convertir solicitudes de cotización en pedidos", "prioridad", "alta", "descripcion", "Contactar prospectos del módulo Soporte y cerrar venta.")
        ));
        m.put("MARKETING", List.of(
                Map.of("titulo", "Actualizar productos destacados y descuentos", "prioridad", "media", "descripcion", "Revisar fichas comerciales, imágenes y promociones vigentes."),
                Map.of("titulo", "Analizar productos más vendidos del mes", "prioridad", "baja", "descripcion", "Subir informe de rotación y propuestas de campaña.")
        ));
        m.put("MENSAJERO", List.of(
                Map.of("titulo", "Recoger y verificar paquetes empacados", "prioridad", "alta", "descripcion", "Validar guía, dirección Google Maps y método de pago antes de salir."),
                Map.of("titulo", "Confirmar entregas y recaudo contraentrega", "prioridad", "urgente", "descripcion", "Actualizar estado a entregado y adjuntar foto/comprobante de entrega.")
        ));
        m.put("GERENTE", List.of(
                Map.of("titulo", "Supervisar KPIs diarios y desempeño por área", "prioridad", "alta", "descripcion", "Revisar cumplimiento semanal y equilibrar cargas operativas."),
                Map.of("titulo", "Validar autorizaciones de soporte e inventario", "prioridad", "media", "descripcion", "Auditar ajustes excepcionales y tiempos de respuesta al cliente.")
        ));
        return Collections.unmodifiableMap(m);
    }

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoPerfilRepository perfilRepository;
    private final TareaEmpleadoRepository tareaRepository;
    private final LogActividadRepository logRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final SoporteService soporteService;
    private final UsuarioActual usuarioActual;

    public EmpleadoGestionServiceImpl(UsuarioRepository usuarioRepository,
                                      EmpleadoPerfilRepository perfilRepository,
                                      TareaEmpleadoRepository tareaRepository,
                                      LogActividadRepository logRepository,
                                      PedidoRepository pedidoRepository,
                                      ClienteRepository clienteRepository,
                                      SoporteService soporteService,
                                      UsuarioActual usuarioActual) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.tareaRepository = tareaRepository;
        this.logRepository = logRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.soporteService = soporteService;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> miPerfil() {
        Usuario u = usuarioActual.requerir();
        EmpleadoPerfil perfil = perfilRepository.findByIdUsuario(u.getIdUsuario()).orElse(null);
        String area = perfil != null ? perfil.getArea() : "OPERATIVO";
        List<String> modulos = perfil != null ? parseModulos(perfil.getModulosPermitidos()) : MODULOS_VALIDOS;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("idUsuario", u.getIdUsuario());
        res.put("nombre", u.getNombre());
        res.put("area", area);
        res.put("modulos", modulos);
        res.put("actividadesRol", ACTIVIDADES_AREA.getOrDefault(area, List.of()));
        res.put("areasDisponibles", AREAS_VALIDAS);
        res.put("modulosDisponibles", MODULOS_VALIDOS);
        return res;
    }

    @Override
    public Map<String, List<Map<String, String>>> actividadesPorArea() {
        return ACTIVIDADES_AREA;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarEmpleadosConCarga() {
        List<Usuario> empleados = usuarioRepository.buscar(null, Rol.EMPLEADO,
                PageRequest.of(0, 100)).getContent();
        Map<Integer, EmpleadoPerfil> perfiles = perfilRepository.findAll().stream()
                .collect(Collectors.toMap(EmpleadoPerfil::getIdUsuario, p -> p, (a, b) -> a));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Usuario emp : empleados) {
            EmpleadoPerfil p = perfiles.get(emp.getIdUsuario());
            List<TareaEmpleado> tareas = tareaRepository.findByIdEmpleadoUsuarioOrderByCreadoEnDesc(emp.getIdUsuario());
            long activas = tareas.stream().filter(t -> ESTADOS_ACTIVOS.contains(t.getEstado())).count();
            long completadas = tareas.stream().filter(t -> "completada".equals(t.getEstado()) || "revisada".equals(t.getEstado())).count();
            double promedio = tareas.stream()
                    .filter(t -> t.getCalificacion() != null)
                    .mapToInt(TareaEmpleado::getCalificacion)
                    .average().orElse(0.0);

            String area = p != null ? p.getArea() : "OPERATIVO";
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("idUsuario", emp.getIdUsuario());
            fila.put("nombre", emp.getNombre());
            fila.put("correo", emp.getCorreo());
            fila.put("activo", Boolean.TRUE.equals(emp.getActivo()));
            fila.put("area", area);
            fila.put("modulos", p != null ? parseModulos(p.getModulosPermitidos()) : MODULOS_VALIDOS);
            fila.put("actividadesRol", ACTIVIDADES_AREA.getOrDefault(area, List.of()));
            fila.put("cargaActiva", activas);
            fila.put("tareasCompletadas", completadas);
            fila.put("promedioCalificacion", Math.round(promedio * 10.0) / 10.0);
            resultado.add(fila);
        }
        return resultado;
    }

    @Override
    @Transactional
    public Map<String, Object> actualizarPermisos(Integer idUsuario, String area, List<String> modulos) {
        return actualizarPermisos(idUsuario, area, modulos, false);
    }

    @Override
    @Transactional
    public Map<String, Object> actualizarPermisos(Integer idUsuario, String area, List<String> modulos, boolean asignarActividadesRol) {
        Usuario emp = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Empleado no encontrado"));

        String areaLimpia = StringUtils.hasText(area) ? area.trim().toUpperCase(Locale.ROOT) : "OPERATIVO";
        if (!AREAS_VALIDAS.contains(areaLimpia)) {
            throw ApiException.badRequest("Area no valida: " + areaLimpia);
        }

        List<String> limpios = (modulos == null ? MODULOS_VALIDOS : modulos).stream()
                .map(m -> m.trim().toLowerCase(Locale.ROOT))
                .filter(MODULOS_VALIDOS::contains)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        if (!limpios.contains("dashboard")) limpios.add(0, "dashboard");
        if (!limpios.contains("perfil")) limpios.add("perfil");
        if (!limpios.contains("tareas")) limpios.add("tareas");

        EmpleadoPerfil perfil = perfilRepository.findByIdUsuario(idUsuario).orElseGet(() -> {
            EmpleadoPerfil np = new EmpleadoPerfil();
            np.setIdUsuario(idUsuario);
            return np;
        });
        perfil.setArea(areaLimpia);
        perfil.setModulosPermitidos(String.join(",", limpios));
        perfil.setActualizadoEn(LocalDateTime.now());
        perfilRepository.save(perfil);

        int tareasCreadas = 0;
        if (asignarActividadesRol) {
            List<TareaEmpleado> existentes = tareaRepository.findByIdEmpleadoUsuarioOrderByCreadoEnDesc(idUsuario);
            Set<String> titulosActivos = existentes.stream()
                    .filter(t -> ESTADOS_ACTIVOS.contains(t.getEstado()))
                    .map(t -> t.getTitulo().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            for (var act : ACTIVIDADES_AREA.getOrDefault(areaLimpia, List.of())) {
                String tituloAct = act.get("titulo");
                if (!titulosActivos.contains(tituloAct.trim().toLowerCase(Locale.ROOT))) {
                    TareaEmpleado t = new TareaEmpleado();
                    t.setIdEmpleadoUsuario(idUsuario);
                    t.setIdAsignadorUsuario(usuarioActual.idOpcional());
                    t.setTitulo(tituloAct);
                    t.setDescripcion("[" + areaLimpia + "] " + act.get("descripcion"));
                    t.setPrioridad(act.get("prioridad"));
                    t.setFechaLimite(LocalDate.now());
                    t.setEstado("pendiente");
                    tareaRepository.save(t);
                    tareasCreadas++;
                }
            }
            if (tareasCreadas > 0) {
                soporteService.notificar(idUsuario,
                        "Actividades asignadas para tu rol (" + areaLimpia + ")",
                        "Se cargaron " + tareasCreadas + " actividades operativas en tu lista.",
                        "/panel/empleado/tareas.html");
            }
        }

        registrarLog("permisos_empleado",
                "Empleado #" + idUsuario + " (" + emp.getNombre() + ") Area=" + areaLimpia
                        + " Modulos=" + perfil.getModulosPermitidos()
                        + (tareasCreadas > 0 ? " | TareasRolCreadas=" + tareasCreadas : ""));

        return Map.of(
                "idUsuario", idUsuario,
                "area", areaLimpia,
                "modulos", limpios,
                "tareasCreadas", tareasCreadas
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarTodasTareas() {
        List<TareaEmpleado> todas = tareaRepository.findAllByOrderByCreadoEnDesc();
        Set<Integer> ids = todas.stream().map(TareaEmpleado::getIdEmpleadoUsuario).collect(Collectors.toSet());
        Map<Integer, String> nombres = usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, Usuario::getNombre));

        return todas.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idTarea", t.getIdTarea());
            m.put("idEmpleadoUsuario", t.getIdEmpleadoUsuario());
            m.put("empleadoNombre", nombres.getOrDefault(t.getIdEmpleadoUsuario(), "Empleado #" + t.getIdEmpleadoUsuario()));
            m.put("titulo", t.getTitulo());
            m.put("descripcion", t.getDescripcion());
            m.put("prioridad", t.getPrioridad());
            m.put("fechaLimite", t.getFechaLimite());
            m.put("estado", t.getEstado());
            m.put("evidencia", t.getEvidencia());
            m.put("comentarioEmpleado", t.getComentarioEmpleado());
            m.put("calificacion", t.getCalificacion());
            m.put("observacionAdmin", t.getObservacionAdmin());
            m.put("creadoEn", t.getCreadoEn());
            m.put("completadaEn", t.getCompletadaEn());
            return m;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TareaEmpleado> misTareas() {
        Usuario u = usuarioActual.requerir();
        return tareaRepository.findByIdEmpleadoUsuarioOrderByCreadoEnDesc(u.getIdUsuario());
    }

    @Override
    @Transactional
    public TareaEmpleado asignarTarea(Integer idEmpleadoUsuario, String titulo, String descripcion,
                                      String prioridad, LocalDate fechaLimite) {
        if (!StringUtils.hasText(titulo)) {
            throw ApiException.badRequest("El titulo de la tarea es obligatorio");
        }
        Usuario emp = usuarioRepository.findById(idEmpleadoUsuario)
                .orElseThrow(() -> ApiException.notFound("Empleado destino no encontrado"));

        TareaEmpleado tarea = new TareaEmpleado();
        tarea.setIdEmpleadoUsuario(idEmpleadoUsuario);
        tarea.setIdAsignadorUsuario(usuarioActual.idOpcional());
        tarea.setTitulo(titulo.trim());
        tarea.setDescripcion(StringUtils.hasText(descripcion) ? descripcion.trim() : null);
        tarea.setPrioridad(StringUtils.hasText(prioridad) ? prioridad.trim().toLowerCase(Locale.ROOT) : "media");
        tarea.setFechaLimite(fechaLimite);
        tarea.setEstado("pendiente");
        tareaRepository.save(tarea);

        soporteService.notificar(idEmpleadoUsuario,
                "Nueva tarea asignada: " + tarea.getTitulo(),
                "Prioridad " + tarea.getPrioridad() + (fechaLimite != null ? " · Límite " + fechaLimite : ""),
                "/panel/empleado/tareas.html");
        registrarLog("asignar_tarea", "Tarea #" + tarea.getIdTarea() + " a " + emp.getNombre());
        return tarea;
    }

    @Override
    @Transactional
    public TareaEmpleado actualizarProgreso(Integer idTarea, String estado, String evidencia, String comentarioEmpleado) {
        Usuario actor = usuarioActual.requerir();
        TareaEmpleado tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> ApiException.notFound("Tarea no encontrada"));

        if (!usuarioActual.esAdministrador() && !actor.getIdUsuario().equals(tarea.getIdEmpleadoUsuario())) {
            throw ApiException.forbidden("Solo puedes actualizar tus propias tareas asignadas");
        }

        String nuevoEstado = StringUtils.hasText(estado) ? estado.trim().toLowerCase(Locale.ROOT) : "en_progreso";
        if (!List.of("pendiente", "en_progreso", "completada").contains(nuevoEstado)) {
            throw ApiException.badRequest("Estado de avance no valido");
        }
        tarea.setEstado(nuevoEstado);
        if (StringUtils.hasText(evidencia)) tarea.setEvidencia(evidencia.trim());
        if (StringUtils.hasText(comentarioEmpleado)) tarea.setComentarioEmpleado(comentarioEmpleado.trim());
        if ("completada".equals(nuevoEstado) && tarea.getCompletadaEn() == null) {
            tarea.setCompletadaEn(LocalDateTime.now());
        }
        tareaRepository.save(tarea);

        if ("completada".equals(nuevoEstado)) {
            usuarioRepository.findByRolNombre(Rol.ADMINISTRADOR).forEach(adm ->
                    soporteService.notificar(adm.getIdUsuario(),
                            "Tarea lista para revisar: " + tarea.getTitulo(),
                            actor.getNombre() + " reportó entrega"
                                    + (StringUtils.hasText(tarea.getEvidencia()) ? " con evidencia adjunta." : "."),
                            "/panel/administrador/tareas.html"));
        }

        registrarLog("progreso_tarea", "Tarea #" + idTarea + " -> " + nuevoEstado);
        return tarea;
    }

    @Override
    @Transactional
    public TareaEmpleado calificarTarea(Integer idTarea, Integer calificacion, String observacionAdmin) {
        return revisarYCalificarTarea(idTarea, "revisada", calificacion, observacionAdmin);
    }

    @Override
    @Transactional
    public TareaEmpleado revisarYCalificarTarea(Integer idTarea, String estado, Integer calificacion, String observacionAdmin) {
        String estadoDestino = StringUtils.hasText(estado) ? estado.trim().toLowerCase(Locale.ROOT) : "revisada";
        if (!List.of("revisada", "en_progreso").contains(estadoDestino)) {
            throw ApiException.badRequest("Estado de revision no valido");
        }

        TareaEmpleado tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> ApiException.notFound("Tarea no encontrada"));

        if ("revisada".equals(estadoDestino)) {
            if (calificacion == null || calificacion < 1 || calificacion > 5) {
                throw ApiException.badRequest("La calificacion debe estar entre 1 y 5");
            }
            tarea.setCalificacion(calificacion);
            if (tarea.getCompletadaEn() == null) {
                tarea.setCompletadaEn(LocalDateTime.now());
            }
        } else {
            // Devuelta para correccion
            tarea.setCalificacion(calificacion != null && calificacion >= 1 && calificacion <= 5 ? calificacion : null);
        }

        tarea.setObservacionAdmin(StringUtils.hasText(observacionAdmin) ? observacionAdmin.trim() : null);
        tarea.setEstado(estadoDestino);
        tareaRepository.save(tarea);

        String tituloNoti = "revisada".equals(estadoDestino)
                ? "Tarea aprobada y calificada (" + tarea.getCalificacion() + "/5)"
                : "Tarea devuelta para corrección";
        soporteService.notificar(tarea.getIdEmpleadoUsuario(),
                tituloNoti,
                tarea.getTitulo() + (StringUtils.hasText(observacionAdmin) ? " · Obs: " + observacionAdmin.trim() : ""),
                "/panel/empleado/tareas.html");

        registrarLog("revisar_tarea", "Tarea #" + idTarea + " estado=" + estadoDestino
                + (tarea.getCalificacion() != null ? " calificacion=" + tarea.getCalificacion() + "/5" : ""));
        return tarea;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> reporteDesempeno() {
        List<Map<String, Object>> empleados = listarEmpleadosConCarga();
        for (Map<String, Object> fila : empleados) {
            long activas = ((Number) fila.get("cargaActiva")).longValue();
            long comp = ((Number) fila.get("tareasCompletadas")).longValue();
            long total = activas + comp;
            int cumplimiento = total == 0 ? 100 : (int) Math.round((comp * 100.0) / total);
            fila.put("totalTareas", total);
            fila.put("cumplimientoPct", cumplimiento);
        }
        return empleados;
    }

    @Override
    @Transactional
    public Map<String, Object> accesoSoporteAuditoria(Integer idUsuario) {
        Usuario objetivo = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        registrarLog("acceso_soporte_maestro",
                "Inspeccion administrativa autorizada sobre usuario #" + idUsuario + " (" + objetivo.getCorreo() + ")");

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("idUsuario", objetivo.getIdUsuario());
        datos.put("nombre", objetivo.getNombre());
        datos.put("correo", objetivo.getCorreo());
        datos.put("rol", objetivo.getRol() != null ? objetivo.getRol().getNombre() : "");
        datos.put("activo", objetivo.getActivo());
        datos.put("bloqueado", objetivo.getBloqueado());

        clienteRepository.findByUsuarioIdUsuario(idUsuario).ifPresent(c -> {
            long totalPedidos = pedidoRepository.findByClienteIdCliente(c.getIdCliente(), PageRequest.of(0, 1)).getTotalElements();
            datos.put("totalPedidosCliente", totalPedidos);
        });
        perfilRepository.findByIdUsuario(idUsuario).ifPresent(p -> {
            datos.put("areaEmpleado", p.getArea());
            datos.put("modulosEmpleado", parseModulos(p.getModulosPermitidos()));
        });
        datos.put("tareasAsignadas", tareaRepository.findByIdEmpleadoUsuarioOrderByCreadoEnDesc(idUsuario).size());
        datos.put("auditadoPor", usuarioActual.requerir().getNombre());
        return datos;
    }

    private List<String> parseModulos(String csv) {
        if (!StringUtils.hasText(csv)) return MODULOS_VALIDOS;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private void registrarLog(String accion, String detalle) {
        LogActividad log = new LogActividad();
        log.setIdUsuario(usuarioActual.idOpcional());
        log.setAccion(accion);
        log.setDetalle(detalle);
        logRepository.save(log);
    }
}
