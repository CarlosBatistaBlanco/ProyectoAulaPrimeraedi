package com.tecnicsecure.service.impl;

import com.tecnicsecure.config.TecniSecureProperties;
import com.tecnicsecure.dto.auth.*;
import com.tecnicsecure.entity.Cliente;
import com.tecnicsecure.entity.LogActividad;
import com.tecnicsecure.entity.Rol;
import com.tecnicsecure.entity.TokenRecuperacion;
import com.tecnicsecure.entity.Usuario;
import com.tecnicsecure.exception.ApiException;
import com.tecnicsecure.repository.ClienteRepository;
import com.tecnicsecure.repository.LogActividadRepository;
import com.tecnicsecure.repository.RolRepository;
import com.tecnicsecure.repository.TokenRecuperacionRepository;
import com.tecnicsecure.repository.UsuarioRepository;
import com.tecnicsecure.security.JwtService;
import com.tecnicsecure.service.AuthService;
import com.tecnicsecure.service.CaptchaService;
import com.tecnicsecure.util.UsuarioActual;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String MENSAJE_GENERICO = "Correo o contraseña incorrectos.";
    private static final String CAPTCHA_SESION = "captchaRespuesta";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteRepository clienteRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final LogActividadRepository logRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TecniSecureProperties propiedades;
    private final UsuarioActual usuarioActual;
    private final CaptchaService captchaService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           ClienteRepository clienteRepository,
                           TokenRecuperacionRepository tokenRepository,
                           LogActividadRepository logRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           TecniSecureProperties propiedades,
                           UsuarioActual usuarioActual,
                           CaptchaService captchaService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.clienteRepository = clienteRepository;
        this.tokenRepository = tokenRepository;
        this.logRepository = logRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.propiedades = propiedades;
        this.usuarioActual = usuarioActual;
        this.captchaService = captchaService;
    }

    /**
     * NOTA IMPORTANTE: el login NO puede ser @Transactional.
     * Al lanzar una excepcion la transaccion haria rollback y se perderian
     * los intentos fallidos, por lo que el bloqueo por 5 intentos nunca
     * se guardaria. Por eso aqui cada guardado se confirma por separado.
     */
    @Override
    public TokenResponse login(LoginRequest request, String ip) {
        // La verificacion se comprueba primero: si esta mal no se consulta la
        // base ni se descuentan intentos, y no se revela si el correo existe.
        if (!captchaService.validar(request.captchaToken(), request.captcha())) {
            throw ApiException.badRequest("La verificación no es correcta. Escribe el resultado de la suma.");
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(MENSAJE_GENERICO));

        desbloquearSiExpiro(usuario);

        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            long minutos = usuario.getBloqueadoHasta() == null ? propiedades.security().bloqueoMinutos()
                    : Math.max(1, java.time.Duration.between(LocalDateTime.now(), usuario.getBloqueadoHasta()).toMinutes() + 1);
            throw new org.springframework.security.authentication.LockedException(
                    "Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " minuto(s).");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Tu cuenta está inactiva. Contacta al administrador.");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario, ip);
            throw new org.springframework.security.authentication.BadCredentialsException(MENSAJE_GENERICO);
        }

        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        auditar(usuario.getIdUsuario(), "login_exitoso", "", ip);

        return emitir(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse refrescar(String refreshToken) {
        if (!jwtService.esTokenValido(refreshToken) || !jwtService.esRefreshToken(refreshToken)) {
            throw ApiException.unauthorized("Refresh token invalido o expirado");
        }
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(jwtService.extraerUsuario(refreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Usuario no encontrado"));
        return emitir(usuario);
    }

    @Override
    @Transactional
    public Usuario registrar(RegistroRequest request) {
        if (usuarioRepository.existsByCorreoIgnoreCase(request.correo().trim())) {
            throw ApiException.conflict("Ese correo ya está registrado. Intenta iniciar sesión.");
        }
        if (StringUtils.hasText(request.documento())
                && usuarioRepository.existsByDocumento(request.documento().trim())) {
            throw ApiException.conflict("Ese documento ya está registrado.");
        }

        Rol rolCliente = rolRepository.findByNombre(Rol.CLIENTE)
                .orElseThrow(() -> ApiException.badRequest("El rol cliente no existe en la base de datos"));

        Usuario usuario = new Usuario();
        usuario.setRol(rolCliente);
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(request.correo().trim().toLowerCase());
        usuario.setDocumento(request.documento().trim());
        usuario.setTelefono(request.telefono());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setActivo(true);
        usuario.setBloqueado(false);
        usuario.setIntentosFallidos(0);
        usuarioRepository.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setNombre(usuario.getNombre());
        cliente.setCorreo(usuario.getCorreo());
        cliente.setTelefono(usuario.getTelefono());
        clienteRepository.save(cliente);

        auditar(usuario.getIdUsuario(), "registro", "Nueva cuenta de cliente", null);
        return usuario;
    }

    @Override
    @Transactional
    public String solicitarRecuperacion(String correo) {
        // Por seguridad, si el correo no existe no se revela.
        var usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(correo.trim());
        if (usuarioOpt.isEmpty()) {
            return null;
        }
        Usuario usuario = usuarioOpt.get();
        String token = UUID.randomUUID().toString().replace("-", "");

        TokenRecuperacion registro = new TokenRecuperacion();
        registro.setIdUsuario(usuario.getIdUsuario());
        registro.setToken(token);
        registro.setExpiraEn(LocalDateTime.now().plusHours(1));
        registro.setUsado(false);
        tokenRepository.save(registro);

        auditar(usuario.getIdUsuario(), "recuperacion_solicitada", "", null);
        return token;
    }

    @Override
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        TokenRecuperacion registro = tokenRepository.findByTokenAndUsadoFalse(token)
                .orElseThrow(() -> ApiException.badRequest("El enlace no es válido o ya fue usado"));

        if (registro.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest("El enlace ha expirado. Solicita uno nuevo.");
        }

        Usuario usuario = usuarioRepository.findById(registro.getIdUsuario())
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueado(false);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        registro.setUsado(true);
        tokenRepository.save(registro);
        auditar(usuario.getIdUsuario(), "password_restablecida", "", null);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Integer idUsuario) {
        return conCliente(usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado")));
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarPerfil(Integer idUsuario, PerfilRequest request) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        if (StringUtils.hasText(request.nombre())) {
            usuario.setNombre(request.nombre().trim());
        }
        if (request.telefono() != null) {
            usuario.setTelefono(request.telefono());
        }
        if (StringUtils.hasText(request.documento())
                && !request.documento().trim().equalsIgnoreCase(usuario.getDocumento())) {
            if (usuarioRepository.existsByDocumento(request.documento().trim())) {
                throw ApiException.conflict("Ese documento ya está registrado");
            }
            usuario.setDocumento(request.documento().trim());
        }
        if (StringUtils.hasText(request.avatar())) {
            usuario.setAvatar(request.avatar());
        }

        if (StringUtils.hasText(request.passwordNueva())) {
            if (!StringUtils.hasText(request.passwordActual())
                    || !passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
                throw ApiException.badRequest("La contraseña actual no es correcta");
            }
            if (!request.passwordNueva().matches("^(?=.*[A-Z])(?=.*\\d).{8,}$")) {
                throw ApiException.badRequest(
                        "La nueva contraseña debe tener mínimo 8 caracteres, una mayúscula y un número");
            }
            usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        }

        usuarioRepository.save(usuario);
        auditar(usuario.getIdUsuario(), "perfil_actualizado", "", null);
        return conCliente(usuario);
    }

    /** Completa el idCliente (solo si el usuario es cliente) para que el frontend pueda suscribirse al chat. */
    private UsuarioResponse conCliente(Usuario usuario) {
        Integer idCliente = usuario.esCliente()
                ? clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                        .map(Cliente::getIdCliente)
                        .orElse(null)
                : null;
        return UsuarioResponse.de(usuario, idCliente);
    }

    @Override
    public String generarPreguntaCaptcha() {
        return captchaService.generar().pregunta();
    }

    @Override
    public boolean validarCaptcha(String respuesta) {
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario usuarioAutenticado() {
        return usuarioActual.requerir();
    }

    private TokenResponse emitir(Usuario usuario) {
        String access = jwtService.generarToken(usuario);
        String refresh = jwtService.generarRefreshToken(usuario);
        long segundos = java.util.concurrent.TimeUnit.MINUTES.toSeconds(propiedades.jwt().expirationMinutes());
        return TokenResponse.de(access, refresh, segundos, conCliente(usuario));
    }

    private void desbloquearSiExpiro(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getBloqueado())
                && usuario.getBloqueadoHasta() != null
                && !usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            usuario.setBloqueado(false);
            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);
            usuarioRepository.save(usuario);
        }
    }

    private void registrarIntentoFallido(Usuario usuario, String ip) {
        int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
        int maximo = propiedades.security().maxIntentosFallidos();
        usuario.setIntentosFallidos(intentos);

        if (intentos >= maximo) {
            usuario.setBloqueado(true);
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(propiedades.security().bloqueoMinutos()));
        }
        usuarioRepository.save(usuario);
        auditar(usuario.getIdUsuario(), "login_fallido", "Intento " + intentos + " de " + maximo, ip);
    }

    private void auditar(Integer idUsuario, String accion, String detalle, String ip) {
        LogActividad log = new LogActividad();
        log.setIdUsuario(idUsuario);
        log.setAccion(accion);
        log.setDetalle(detalle);
        log.setIp(ip);
        logRepository.save(log);
    }

    private HttpServletRequest peticionActual() {
        var atributos = RequestContextHolder.getRequestAttributes();
        return atributos instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }
}
