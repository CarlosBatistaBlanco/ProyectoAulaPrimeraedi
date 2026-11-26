/*
 * Este archivo ejecuta todas las reglas de acceso y cuentas de la tienda: revisa quien entra,
 * bloquea la cuenta por unos minutos si alguien falla muchas veces la clave, registra clientes
 * nuevos, ayuda a recuperar contrasenas olvidadas y guarda los cambios en Mi perfil. Su trabajo
 * se nota en las pantallas de inicio de sesion, registro, recuperacion de clave y perfil.
 */
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

    // Prepara todas las herramientas para revisar cuentas, proteger contrasenas, entregar pases de sesion y guardar el registro de accesos.
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

    // Revisa la suma de verificacion, comprueba que la cuenta este activa y desbloqueada, verifica la clave y entrega el pase de sesion.
    // Cada intento fallido se guarda por separado para que, si ocurre un error de clave, el conteo no se pierda y el bloqueo funcione de verdad.
    @Override
    public TokenResponse login(LoginRequest request, String ip) {
        // Primero comprueba la suma de verificacion: si falla, ni siquiera busca el correo para no revelar si la cuenta existe.
        if (!captchaService.validar(request.captchaToken(), request.captcha())) {
            throw ApiException.badRequest("La verificación no es correcta. Escribe el resultado de la suma.");
        }

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(request.correo().trim())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(MENSAJE_GENERICO));

        // Si la cuenta estuvo bloqueada por varios errores pero el tiempo de espera ya termino, la libera automaticamente.
        desbloquearSiExpiro(usuario);

        // Si la cuenta sigue en pausa por seguridad, avisa cuantos minutos faltan para volver a intentar.
        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
            long minutos = usuario.getBloqueadoHasta() == null ? propiedades.security().bloqueoMinutos()
                    : Math.max(1, java.time.Duration.between(LocalDateTime.now(), usuario.getBloqueadoHasta()).toMinutes() + 1);
            throw new org.springframework.security.authentication.LockedException(
                    "Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " minuto(s).");
        }

        // Impide entrar si un administrador desactivo temporalmente esta cuenta.
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new org.springframework.security.authentication.DisabledException(
                    "Tu cuenta está inactiva. Contacta al administrador.");
        }

        // Si la clave escrita no coincide con la guardada de forma segura, suma un intento fallido y bloquea si llego al limite.
        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario, ip);
            throw new org.springframework.security.authentication.BadCredentialsException(MENSAJE_GENERICO);
        }

        // Si todo coincide, vuelve a cero los intentos fallidos, anota la fecha de entrada y deja registro en la bitacora.
        usuario.setIntentosFallidos(0);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        auditar(usuario.getIdUsuario(), "login_exitoso", "", ip);

        return emitir(usuario);
    }

    // Renueva automaticamente el pase de sesion del usuario siempre que su cuenta siga activa y sin bloqueos.
    @Override
    @Transactional(readOnly = true)
    public TokenResponse refrescar(String refreshToken) {
        if (!jwtService.esTokenValido(refreshToken) || !jwtService.esRefreshToken(refreshToken)) {
            throw ApiException.unauthorized("Refresh token invalido o expirado");
        }
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(jwtService.extraerUsuario(refreshToken))
                .orElseThrow(() -> ApiException.unauthorized("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw ApiException.unauthorized("Tu cuenta está inactiva");
        }
        if (Boolean.TRUE.equals(usuario.getBloqueado())
                && (usuario.getBloqueadoHasta() == null || usuario.getBloqueadoHasta().isAfter(LocalDateTime.now()))) {
            throw ApiException.unauthorized("Tu cuenta está bloqueada temporalmente");
        }
        return emitir(usuario);
    }

    // Crea la cuenta nueva de un cliente y su ficha de compras al mismo tiempo; si algo falla a mitad de camino, todo se deshace para no dejar cuentas a medias.
    @Override
    @Transactional
    public Usuario registrar(RegistroRequest request) {
        // Comprueba que ni el correo ni el documento esten ya usados por otra persona.
        if (usuarioRepository.existsByCorreoIgnoreCase(request.correo().trim())) {
            throw ApiException.conflict("Ese correo ya está registrado. Intenta iniciar sesión.");
        }
        if (StringUtils.hasText(request.documento())
                && usuarioRepository.existsByDocumento(request.documento().trim())) {
            throw ApiException.conflict("Ese documento ya está registrado.");
        }

        Rol rolCliente = rolRepository.findByNombre(Rol.CLIENTE)
                .orElseThrow(() -> ApiException.badRequest("El rol cliente no existe en la base de datos"));

        // Guarda la cuenta con la contrasena protegida de forma ilegible para mayor seguridad.
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

        // Crea tambien la ficha comercial del cliente para que pueda hacer pedidos, recibir facturas y usar el chat.
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setNombre(usuario.getNombre());
        cliente.setCorreo(usuario.getCorreo());
        cliente.setTelefono(usuario.getTelefono());
        clienteRepository.save(cliente);

        auditar(usuario.getIdUsuario(), "registro", "Nueva cuenta de cliente", null);
        return usuario;
    }

    // Crea un codigo unico valido por 1 hora para que la persona pueda recuperar su contrasena olvidada.
    @Override
    @Transactional
    public String solicitarRecuperacion(String correo) {
        // Por privacidad, si el correo no existe no avisa que falte en la tienda.
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

    // Guarda la nueva contrasena usando el enlace temporal vigente, quita cualquier bloqueo anterior y marca el enlace como usado.
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

    // Consulta la informacion personal de una cuenta para mostrarla en la pantalla de Mi perfil.
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse perfil(Integer idUsuario) {
        return conCliente(usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado")));
    }

    // Guarda cambios en nombre, telefono, documento o foto de perfil y permite cambiar la clave verificando primero la clave actual.
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
        // Si la persona cambia su numero de documento, revisa que no pertenezca ya a otra cuenta.
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

        // Si pidio cambiar la clave, valida que escribio bien la clave anterior y que la nueva sea segura (8 letras, mayuscula y numero).
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

    // Adjunta el numero de ficha de cliente (solo cuando la cuenta es de un cliente) para que su chat se conecte correctamente.
    private UsuarioResponse conCliente(Usuario usuario) {
        Integer idCliente = usuario.esCliente()
                ? clienteRepository.findByUsuarioIdUsuario(usuario.getIdUsuario())
                        .map(Cliente::getIdCliente)
                        .orElse(null)
                : null;
        return UsuarioResponse.de(usuario, idCliente);
    }

    // Genera una pregunta matematica sencilla para mostrar en la pantalla de inicio de sesion.
    @Override
    public String generarPreguntaCaptcha() {
        return captchaService.generar().pregunta();
    }

    // Se conserva por compatibilidad; la comprobacion real de la suma se hace junto con su ficha sellada al iniciar sesion.
    @Override
    public boolean validarCaptcha(String respuesta) {
        return false;
    }

    // Devuelve los datos de la persona que tiene la sesion abierta en este momento.
    @Override
    @Transactional(readOnly = true)
    public Usuario usuarioAutenticado() {
        return usuarioActual.requerir();
    }

    // Entrega los pases de acceso y renovacion junto con el resumen de la cuenta del usuario.
    private TokenResponse emitir(Usuario usuario) {
        String access = jwtService.generarToken(usuario);
        String refresh = jwtService.generarRefreshToken(usuario);
        long segundos = java.util.concurrent.TimeUnit.MINUTES.toSeconds(propiedades.jwt().expirationMinutes());
        return TokenResponse.de(access, refresh, segundos, conCliente(usuario));
    }

    // Levanta el bloqueo temporal de una cuenta cuando el tiempo de espera ya se cumplio.
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

    // Suma un error de clave, pausa temporalmente la cuenta si alcanzo el tope de intentos y anota el suceso en auditoria.
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

    // Anota en la bitacora de control cada acceso, registro o cambio de contrasena para seguimiento del administrador.
    private void auditar(Integer idUsuario, String accion, String detalle, String ip) {
        LogActividad log = new LogActividad();
        log.setIdUsuario(idUsuario);
        log.setAccion(accion);
        log.setDetalle(detalle);
        log.setIp(ip);
        logRepository.save(log);
    }

    // Obtiene los datos de la visita actual si se necesitan durante el acceso.
    private HttpServletRequest peticionActual() {
        var atributos = RequestContextHolder.getRequestAttributes();
        return atributos instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }
}
