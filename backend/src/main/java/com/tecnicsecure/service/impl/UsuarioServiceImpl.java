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
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final EmpleadoPerfilRepository empleadoPerfilRepository;
    private final TareaEmpleadoRepository tareaEmpleadoRepository;
    private final LogActividadRepository logRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioActual usuarioActual;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              RolRepository rolRepository,
                              ClienteRepository clienteRepository,
                              EmpleadoRepository empleadoRepository,
                              PedidoRepository pedidoRepository,
                              FavoritoRepository favoritoRepository,
                              NotificacionRepository notificacionRepository,
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
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.empleadoPerfilRepository = empleadoPerfilRepository;
        this.tareaEmpleadoRepository = tareaEmpleadoRepository;
        this.logRepository = logRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioActual = usuarioActual;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(String texto, String rol, Pageable pageable) {
        String filtroTexto = StringUtils.hasText(texto) ? texto.trim() : null;
        String filtroRol = StringUtils.hasText(rol) ? rol.trim().toLowerCase() : null;
        return PageResponse.de(usuarioRepository.buscar(filtroTexto, filtroRol, pageable), UsuarioResponse::de);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Integer idUsuario) {
        return UsuarioResponse.de(buscarUsuario(idUsuario));
    }

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

    @Override
    @Transactional
    public UsuarioResponse actualizar(Integer idUsuario, UsuarioAdminRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);
        String correo = request.correo().trim().toLowerCase();

        if (!correo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflict("Ya existe otro usuario con ese correo");
        }
        String documento = StringUtils.hasText(request.documento()) ? request.documento().trim() : null;
        if (documento != null && !documento.equalsIgnoreCase(usuario.getDocumento())
                && usuarioRepository.existsByDocumento(documento)) {
            throw ApiException.conflict("Ya existe otro usuario con ese documento");
        }

        Rol rol = buscarRol(request.rol());
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

    @Override
    @Transactional
    public void eliminarDefinitivo(Integer idUsuario, String claveAdministrador) {
        Usuario actor = usuarioActual.requerir();

        // 1) La clave del administrador que ejecuta la accion es obligatoria
        if (!StringUtils.hasText(claveAdministrador)
                || !passwordEncoder.matches(claveAdministrador, actor.getPasswordHash())) {
            throw ApiException.badRequest("Clave incorrecta. No se elimino el usuario.");
        }
        // 2) Nunca se puede eliminar a si mismo
        if (actor.getIdUsuario().equals(idUsuario)) {
            throw ApiException.badRequest("No puedes eliminar tu propia cuenta");
        }

        Usuario usuario = buscarUsuario(idUsuario);
        String correo = usuario.getCorreo();

        // 3) Se limpian los datos propios del usuario
        favoritoRepository.deleteByIdUsuario(idUsuario);
        notificacionRepository.deleteByIdUsuario(idUsuario);
        tokenRecuperacionRepository.deleteByIdUsuario(idUsuario);
        empleadoPerfilRepository.deleteByIdUsuario(idUsuario);
        tareaEmpleadoRepository.deleteByIdEmpleadoUsuario(idUsuario);

        // 4) Si es cliente, sus pedidos dependen de la fila de cliente:
        //    se eliminan los pedidos (y sus lineas) antes del cliente.
        clienteRepository.findByUsuarioIdUsuario(idUsuario).ifPresent(cliente -> {
            pedidoRepository.deleteByClienteIdCliente(cliente.getIdCliente());
            clienteRepository.delete(cliente);
        });

        // 5) Si es empleado, se elimina su ficha; el historial de auditoria
        //    y las facturas se conservan.
        empleadoRepository.findByUsuarioIdUsuario(idUsuario).ifPresent(empleadoRepository::delete);

        usuarioRepository.delete(usuario);
        registrarLog("eliminar_usuario", "Usuario #" + idUsuario + " (" + correo + ") eliminado definitivamente");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LogActividadResponse> auditoria(Pageable pageable) {
        return auditoria(null, null, null, null, pageable);
    }

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

    @Override
    @Transactional
    public int depurarAuditoria(int diasRetencion) {
        int dias = Math.max(30, diasRetencion);
        LocalDateTime limite = LocalDateTime.now().minusDays(dias);
        int eliminados = logRepository.deleteByCreadoEnBefore(limite);
        registrarLog("depurar_auditoria", "Registros anteriores a " + dias + " dias eliminados: " + eliminados);
        return eliminados;
    }

    private Usuario buscarUsuario(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    private Rol buscarRol(String nombreRol) {
        String nombre = nombreRol.trim().toLowerCase();
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> ApiException.badRequest("Rol invalido: " + nombreRol));
    }

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

    private void registrarLog(String accion, String detalle) {
        LogActividad log = new LogActividad();
        log.setIdUsuario(usuarioActual.idOpcional());
        log.setAccion(accion);
        log.setDetalle(detalle);
        logRepository.save(log);
    }
}
