/*
 * Este archivo ejecuta todas las reglas del panel administrativo para manejar las cuentas de
 * la tienda (crear, editar, pausar, desbloquear o eliminar usuarios) y consultar o limpiar el
 * registro de actividad del negocio. Su trabajo se nota en las secciones de Usuarios y Auditoria.
 */
package com.tecnicsecure.service.impl;

import com.tecnicsecure.dto.auth.LogActividadResponse;
import com.tecnicsecure.dto.auth.UsuarioAdminRequest;
import com.tecnicsecure.dto.auth.UsuarioResponse;
import com.tecnicsecure.dto.comun.PageResponse;
import com.tecnicsecure.entity.Cliente;
import com.tecnicsecure.entity.LogActividad;
import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.ClienteRepository;
import com.tecnicsecure.repository.DireccionUsuarioRepository;
import com.tecnicsecure.repository.EmpleadoPerfilRepository;
import com.tecnicsecure.repository.EmpleadoRepository;
import com.tecnicsecure.repository.FavoritoRepository;
import com.tecnicsecure.repository.LogActividadRepository;
import com.tecnicsecure.repository.NotificacionRepository;
import com.tecnicsecure.repository.PedidoRepository;
import com.tecnicsecure.repository.RolRepository;
import com.tecnicsecure.repository.TareaEmpleadoRepository;
import com.tecnicsecure.repository.TokenRecuperacionRepository;
import com.tecnicsecure.repository.UsuarioRepository;
import com.tecnicsecure.service.UsuarioService;
import com.tecnicsecure.util.UsuarioActual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PedidoRepository pedidoRepository;
    private final FavoritoRepository favoritoRepository;
    private final NotificacionRepository notificacionRepository;
    private final DireccionUsuarioRepository direccionUsuarioRepository;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final EmpleadoPerfilRepository empleadoPerfilRepository;
    private final TareaEmpleadoRepository tareaEmpleadoRepository;
    private final LogActividadRepository logRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;

    // Prepara el acceso a cuentas, roles, datos asociados de clientes y empleados, proteccion de claves y bitacora.
    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              ClienteRepository clienteRepository,
                              EmpleadoRepository empleadoRepository,
                              PedidoRepository pedidoRepository,
                              FavoritoRepository favoritoRepository,
                              NotificacionRepository notificacionRepository,
                              DireccionUsuarioRepository direccionUsuarioRepository,
                              TokenRecuperacionRepository tokenRecuperacionRepository,
                              EmpleadoPerfilRepository empleadoPerfilRepository,
                              TareaEmpleadoRepository tareaEmpleadoRepository,
                              LogActividadRepository logRepository,
                              PasswordEncoder passwordEncoder,
                              UsuarioActual usuarioActual) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.clienteRepository = clienteRepository;
        this.empleadoRepository = empleadoRepository;
        this.pedidoRepository = pedidoRepository;
        this.favoritoRepository = favoritoRepository;
        this.notificacionRepository = notificacionRepository;
        this.direccionUsuarioRepository = direccionUsuarioRepository;
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.empleadoPerfilRepository = empleadoPerfilRepository;
        this.tareaEmpleadoRepository = tareaEmpleadoRepository;
        this.logRepository = logRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActual = usuarioActual;
    }

    // Busca cuentas por nombre, correo o rol y las entrega divididas en paginas para la tabla de usuarios.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String texto, String rol, Pageable pageable) {
        String filtroTexto = StringUtils.hasText(texto) ? texto.trim() : null;
        String filtroRol = StringUtils.hasText(rol) ? rol.trim().toLowerCase() : null;
        return PageResponse.de(usuarioRepository.buscar(filtroTexto, filtroRol, pageable), UsuarioResponse::de);
    }

    // Consulta la ficha completa de una cuenta usando su numero identificador.
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Integer idUsuario) {
        return UsuarioResponse.de(buscarUsuario(idUsuario));
    }

    // Crea una cuenta nueva desde administracion comprobando correo y documento unicos; si algo falla, todo se deshace para no dejar datos a medias.
    @Override
    @Transactional
    public UsuarioResponse crear(UsuarioAdminRequest request) {
        String correo = request.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflict("Ya existe un usuario con ese correo");
        }
        String documento = StringUtils.hasText(request.documento()) ? request.documento().trim() : null;
        if (documento != null && usuarioRepository.existsByDocumento(documento)) {
            throw ApiException.conflict("Ya existe un usuario con ese documento");
        }
        if (!StringUtils.hasText(request.password())) {
            throw ApiException.badRequest("La contraseña es obligatoria al crear un usuario");
        }

        Rol rol = buscarRol(request.rol());

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setDocumento(documento);
        usuario.setTelefono(StringUtils.hasText(request.telefono()) ? request.telefono().trim() : null);
        usuario.setRol(rol);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setActivo(request.activo() == null || request.activo());
        usuario.setBloqueado(Boolean.TRUE.equals(request.bloqueado()));
        usuarioRepository.save(usuario);

        sincronizarFichaCliente(usuario);
        registrarLog("crear_usuario", "Usuario #" + usuario.getIdUsuario() + " (" + correo + ")");
        return UsuarioResponse.de(usuario);
    }

    // Guarda cambios en los datos, rol, clave o desbloqueo de una cuenta impidiendo que el administrador se desactive a si mismo.
    @Override
    @Transactional
    public UsuarioResponse actualizar(Integer idUsuario, UsuarioAdminRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);
        String correo = request.correo().trim().toLowerCase();

        // Comprueba que el correo o el documento nuevos no pertenezcan ya a otra persona.
        if (!correo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflict("Ya existe otro usuario con ese correo");
        }
        String documento = StringUtils.hasText(request.documento()) ? request.documento().trim() : null;
        if (documento != null && !documento.equalsIgnoreCase(usuario.getDocumento())
                && usuarioRepository.existsByDocumento(documento)) {
            throw ApiException.conflict("Ya existe otro usuario con ese documento");
        }

        Rol rol = buscarRol(request.rol());
        // Proteccion de seguridad: un administrador no puede dejarse sin acceso a si mismo por accidente.
        Usuario actor = usuarioActual.requerir();
        if (actor.getIdUsuario().equals(idUsuario) && Boolean.FALSE.equals(request.activo())) {
            throw ApiException.badRequest("No puedes desactivar tu propia cuenta de administrador");
        }

        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setDocumento(documento);
        usuario.setTelefono(StringUtils.hasText(request.telefono()) ? request.telefono().trim() : null);
        usuario.setRol(rol);
        if (StringUtils.hasText(request.password())) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }
        // Si el administrador quita el bloqueo, tambien vuelve a cero los intentos fallidos y la hora de espera.
        if (request.bloqueado() != null) {
            usuario.setBloqueado(request.bloqueado());
            if (!request.bloqueado()) {
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(null);
            }
        }

        usuarioRepository.save(usuario);
        sincronizarFichaCliente(usuario);
        registrarLog("actualizar_usuario", "Usuario #" + idUsuario + " (" + correo + ")");
        return UsuarioResponse.de(usuario);
    }

    // Activa o pausa temporalmente el acceso de una cuenta evitando que el administrador pause su propia cuenta.
    @Override
    @Transactional
    public void cambiarEstado(Integer idUsuario, boolean activo) {
        Usuario actor = usuarioActual.requerir();
        if (actor.getIdUsuario().equals(idUsuario) && !activo) {
            throw ApiException.badRequest("No puedes desactivar tu propia cuenta");
        }
        Usuario usuario = buscarUsuario(idUsuario);
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
        registrarLog(activo ? "activar_usuario" : "desactivar_usuario", "Usuario #" + idUsuario);
    }

    // Borra definitivamente una cuenta pidiendo la clave del administrador y limpiando en orden sus datos asociados; si algo falla, todo se deshace.
    @Override
    @Transactional
    public void eliminarDefinitivo(Integer idUsuario, String claveAdministrador) {
        Usuario actor = usuarioActual.requerir();

        // Paso 1: exige escribir la clave del administrador para confirmar que no es un borrado accidental.
        if (!StringUtils.hasText(claveAdministrador)
                || !passwordEncoder.matches(claveAdministrador, actor.getPasswordHash())) {
            throw ApiException.badRequest("Clave incorrecta. No se elimino el usuario.");
        }
        // Paso 2: impide que el administrador borre su propia cuenta.
        if (actor.getIdUsuario().equals(idUsuario)) {
            throw ApiException.badRequest("No puedes eliminar tu propia cuenta");
        }

        Usuario usuario = buscarUsuario(idUsuario);
        String correo = usuario.getCorreo();

        // Paso 3: limpia favoritos, avisos, enlaces de recuperacion, perfil, tareas y direcciones de esa cuenta.
        favoritoRepository.deleteByIdUsuario(idUsuario);
        notificacionRepository.deleteByIdUsuario(idUsuario);
        tokenRecuperacionRepository.deleteByIdUsuario(idUsuario);
        empleadoPerfilRepository.deleteByIdUsuario(idUsuario);
        tareaEmpleadoRepository.deleteByIdEmpleadoUsuario(idUsuario);
        direccionUsuarioRepository.deleteByIdUsuario(idUsuario);

        // Paso 4: si era cliente, retira sus pedidos ligados antes de eliminar su ficha de cliente.
        clienteRepository.findByUsuarioIdUsuario(idUsuario).ifPresent(cliente -> {
            pedidoRepository.deleteByClienteIdCliente(cliente.getIdCliente());
            clienteRepository.delete(cliente);
        });

        // Paso 5: si era empleado, borra su ficha laboral pero conserva el registro de auditoria y facturas del negocio.
        empleadoRepository.findByUsuarioIdUsuario(idUsuario).ifPresent(empleadoRepository::delete);

        usuarioRepository.delete(usuario);
        registrarLog("eliminar_usuario", "Usuario #" + idUsuario + " (" + correo + ") eliminado definitivamente");
    }

    // Muestra el historial completo de acciones de la plataforma sin filtros adicionales.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LogActividadResponse> auditoria(Pageable pageable) {
        return auditoria(null, null, null, null, pageable);
    }

    // Consulta la bitacora filtrando por palabra, tipo de accion y fechas, agregando el nombre y rol de quien hizo cada movimiento.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LogActividadResponse> auditoria(String texto, String accion,
                                                        LocalDate desde, LocalDate hasta,
                                                        Pageable pageable) {
        String filtroTexto = StringUtils.hasText(texto) ? texto.trim() : "";
        String filtroAccion = StringUtils.hasText(accion) ? accion.trim() : "";
        LocalDateTime fechaDesde = desde != null ? desde.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime fechaHasta = hasta != null ? hasta.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);

        Page<LogActividad> logs = logRepository.buscarFiltrado(
                filtroTexto, filtroAccion, fechaDesde, fechaHasta, pageable);
        // Busca de una sola vez los nombres y roles de todas las personas que aparecen en esta pagina del historial.
        Set<Integer> ids = logs.getContent().stream()
                .map(LogActividad::getIdUsuario)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Usuario> usuarios = usuarioRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Usuario::getIdUsuario, u -> u));

        return PageResponse.de(logs, l -> {
            Usuario actor = l.getIdUsuario() != null ? usuarios.get(l.getIdUsuario()) : null;
            String nombre = actor != null ? actor.getNombre()
                    : (l.getIdUsuario() != null ? "Usuario #" + l.getIdUsuario() : "Sistema");
            String rol = (actor != null && actor.getRol() != null) ? actor.getRol().getNombre() : "sistema";
            return LogActividadResponse.de(l, nombre, rol);
        });
    }

    // Limpia registros muy antiguos de la bitacora conservando siempre como minimo los ultimos 30 dias.
    @Override
    @Transactional
    public int depurarAuditoria(int diasRetencion) {
        int dias = Math.max(30, diasRetencion);
        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        int eliminados = logRepository.deleteByCreadoEnBefore(limite);
        registrarLog("depurar_auditoria", "Registros anteriores a " + dias + " dias eliminados: " + eliminados);
        return eliminados;
    }

    // Busca una cuenta por su numero y avisa con error si no existe.
    private Usuario buscarUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    // Confirma que el rol indicado (cliente, empleado o administrador) exista antes de asignarlo.
    private Rol buscarRol(String nombreRol) {
        String nombre = nombreRol.trim().toLowerCase();
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> ApiException.badRequest("Rol invalido: " + nombreRol));
    }

    // Mantiene al dia la ficha comercial de cliente (nombre, correo y telefono) cuando la cuenta tiene rol cliente.
    private void sincronizarFichaCliente(Usuario usuario) {
        if (!usuario.esCliente()) {
            return;
        }
        Cliente cliente = clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                .orElseGet(Cliente::new);
        cliente.setUsuario(usuario);
        cliente.setNombre(usuario.getNombre());
        cliente.setCorreo(usuario.getCorreo());
        cliente.setTelefono(usuario.getTelefono());
        clienteRepository.save(cliente);
    }

    // Anota en la bitacora quien creo, edito, pauso o elimino una cuenta para control administrativo.
    private void registrarLog(String accion, String detalle) {
        LogActividad log = new LogActividad();
        log.setIdUsuario(usuarioActual.idOpcional());
        log.setAccion(accion);
        log.setDetalle(detalle);
        logRepository.save(log);
    }
}
