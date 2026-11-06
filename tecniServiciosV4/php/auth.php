<?php
/**
 * auth.php
 * -----------------------------------------------------------------
 * Núcleo de autenticación y control de acceso (RBAC) de todo el sitio.
 * TODAS las páginas protegidas empiezan incluyendo este archivo:
 *      require_once __DIR__ . '/../php/auth.php';
 *      requerirRol(['administrador']);
 *
 * Qué resuelve este archivo:
 *   - Inicia la sesión de forma segura (cookies httponly).
 *   - Genera y valida tokens CSRF para todos los formularios.
 *   - Guarda quién inició sesión y con qué rol.
 *   - Bloquea el acceso a quien no tenga el rol correcto.
 *   - Registra auditoría (logs_actividad) de acciones importantes.
 *   - Maneja el "Recordarme" del login con cookies seguras.
 * -----------------------------------------------------------------
 */

require_once __DIR__ . '/conexion.php';

// -----------------------------------------------------------------
// Sesión segura: se configura ANTES de session_start()
// -----------------------------------------------------------------
if (session_status() === PHP_SESSION_NONE) {
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/',
        'httponly' => true,   // JavaScript no puede leer la cookie de sesión (mitiga XSS)
        'samesite' => 'Lax',  // mitiga ataques CSRF básicos
    ]);
    session_start();
}

// Expiración automática por inactividad (30 minutos)
const SESION_MAX_INACTIVIDAD = 1800;
if (isset($_SESSION['ultimo_momento']) && (time() - $_SESSION['ultimo_momento'] > SESION_MAX_INACTIVIDAD)) {
    session_unset();
    session_destroy();
    session_start();
}
$_SESSION['ultimo_momento'] = time();

/* ===================================================================
   CSRF: un "token" único por sesión que se mete escondido en cada
   formulario. Si alguien intenta enviar el formulario desde otra
   página (ataque CSRF), el token no coincide y se rechaza.
   =================================================================== */
function csrfToken(): string {
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}
function csrfCampoOculto(): string {
    return '<input type="hidden" name="csrf_token" value="' . htmlspecialchars(csrfToken()) . '">';
}
function csrfValido(?string $token): bool {
    return !empty($token) && !empty($_SESSION['csrf_token']) && hash_equals($_SESSION['csrf_token'], $token);
}

/* ===================================================================
   LOGIN / LOGOUT / SESIÓN ACTUAL
   =================================================================== */
function iniciarSesionUsuario(array $usuario): void {
    // session_regenerate_id evita "session fixation" (que alguien reutilice
    // el id de sesión de antes de iniciar sesión)
    session_regenerate_id(true);
    $_SESSION['id_usuario'] = $usuario['id_usuario'];
    $_SESSION['nombre']     = $usuario['nombre'];
    $_SESSION['correo']     = $usuario['correo'];
    $_SESSION['rol']        = $usuario['rol'];
    $_SESSION['avatar']     = $usuario['avatar'] ?? null;
}

function usuarioActual(): ?array {
    if (!empty($_SESSION['id_usuario'])) {
        return [
            'id_usuario' => $_SESSION['id_usuario'],
            'nombre'     => $_SESSION['nombre'],
            'correo'     => $_SESSION['correo'],
            'rol'        => $_SESSION['rol'],
            'avatar'     => $_SESSION['avatar'] ?? null,
        ];
    }
    return null;
}

function cerrarSesionUsuario(): void {
    // Elimina también la cookie de "recordarme" si existe
    if (!empty($_COOKIE['recordarme'])) {
        global $pdo;
        [$selector] = explode(':', $_COOKIE['recordarme']) + [null, null];
        if ($selector) {
            $stmt = $pdo->prepare('DELETE FROM recordar_tokens WHERE selector = :s');
            $stmt->execute([':s' => $selector]);
        }
        setcookie('recordarme', '', time() - 3600, '/');
    }
    session_unset();
    session_destroy();
}

/* ===================================================================
   RBAC: exigir que haya sesión, y opcionalmente un rol específico.
   Se llama al principio de cada página protegida.
   =================================================================== */
function requerirLogin(): array {
    intentarRecordarme(); // si no hay sesión activa, intenta restaurarla con la cookie "recordarme"
    $u = usuarioActual();
    if (!$u) {
        $destino = urlencode($_SERVER['REQUEST_URI'] ?? '');
        header('Location: /TecniSecure/login.php?redir=' . $destino);
        exit;
    }
    return $u;
}

function requerirRol(array $rolesPermitidos): array {
    $u = requerirLogin();
    if (!in_array($u['rol'], $rolesPermitidos, true)) {
        http_response_code(403);
        die('<div style="font-family:sans-serif;padding:40px;text-align:center;">
                <h1>403 — Acceso denegado</h1>
                <p>Tu cuenta no tiene permiso para ver esta sección.</p>
                <a href="/TecniSecure/index.php">Volver al inicio</a>
             </div>');
    }
    return $u;
}

/* ===================================================================
   AUDITORÍA: deja constancia de acciones importantes
   =================================================================== */
function registrarLog(?int $idUsuario, string $accion, string $detalle = ''): void {
    global $pdo;
    $ip = $_SERVER['REMOTE_ADDR'] ?? '0.0.0.0';
    $stmt = $pdo->prepare('INSERT INTO logs_actividad (id_usuario, accion, detalle, ip) VALUES (:u, :a, :d, :ip)');
    $stmt->execute([':u' => $idUsuario, ':a' => $accion, ':d' => $detalle, ':ip' => $ip]);
}

/* ===================================================================
   "RECORDARME": patrón selector+validador (más seguro que guardar
   un solo token largo). El selector identifica la fila en la BD,
   el validador se compara con un hash (como una contraseña).
   =================================================================== */
function crearCookieRecordarme(int $idUsuario): void {
    global $pdo;
    $selector = bin2hex(random_bytes(9));
    $validador = bin2hex(random_bytes(32));
    $hash = password_hash($validador, PASSWORD_DEFAULT);
    $expira = date('Y-m-d H:i:s', time() + 60 * 60 * 24 * 30); // 30 días

    $stmt = $pdo->prepare('INSERT INTO recordar_tokens (id_usuario, selector, validador_hash, expira_en) VALUES (:u,:s,:v,:e)');
    $stmt->execute([':u' => $idUsuario, ':s' => $selector, ':v' => $hash, ':e' => $expira]);

    setcookie('recordarme', $selector . ':' . $validador, time() + 60 * 60 * 24 * 30, '/', '', false, true);
}

function intentarRecordarme(): void {
    global $pdo;
    if (usuarioActual() || empty($_COOKIE['recordarme'])) return;

    $partes = explode(':', $_COOKIE['recordarme']);
    if (count($partes) !== 2) return;
    [$selector, $validador] = $partes;

    $stmt = $pdo->prepare('SELECT rt.*, u.id_usuario, u.nombre, u.correo, r.nombre AS rol
                            FROM recordar_tokens rt
                            JOIN usuarios u ON u.id_usuario = rt.id_usuario
                            JOIN roles r ON r.id_rol = u.id_rol
                            WHERE rt.selector = :s AND rt.expira_en > NOW() LIMIT 1');
    $stmt->execute([':s' => $selector]);
    $fila = $stmt->fetch();

    if ($fila && password_verify($validador, $fila['validador_hash'])) {
        iniciarSesionUsuario($fila);
    } else {
        setcookie('recordarme', '', time() - 3600, '/');
    }
}

/* ===================================================================
   CAPTCHA propio: pregunta matemática simple ("¿Cuánto es 4 + 7?").
   A propósito NO se usa una imagen generada con GD/TTF: esa librería
   depende de que el servidor tenga la extensión GD con soporte de
   FreeType bien configurada, algo que varía entre instalaciones de
   XAMPP y fue causa de errores. Una pregunta matemática funciona en
   el 100% de los casos, sin depender de ninguna extensión de PHP.
   =================================================================== */
function generarCaptchaPregunta(): string {
    $a = random_int(1, 9);
    $b = random_int(1, 9);
    $_SESSION['captcha_resultado'] = $a + $b;
    return "$a + $b";
}

function captchaValido(?string $intento): bool {
    $valido = isset($_SESSION['captcha_resultado']) && $intento !== null && trim($intento) !== ''
        && (int)trim($intento) === (int)$_SESSION['captcha_resultado'];
    unset($_SESSION['captcha_resultado']); // un captcha solo se puede usar una vez
    return $valido;
}

/* ===================================================================
   NOTIFICACIONES INTERNAS (campana del panel)
   =================================================================== */
function crearNotificacion(int $idUsuario, string $titulo, string $mensaje = '', string $enlace = ''): void {
    global $pdo;
    $stmt = $pdo->prepare('INSERT INTO notificaciones (id_usuario, titulo, mensaje, enlace) VALUES (:u,:t,:m,:e)');
    $stmt->execute([':u' => $idUsuario, ':t' => $titulo, ':m' => $mensaje, ':e' => $enlace]);
}

/** Notifica a TODOS los administradores a la vez (ej: nuevo pedido, stock bajo) */
function notificarAdministradores(string $titulo, string $mensaje = '', string $enlace = ''): void {
    global $pdo;
    $admins = $pdo->query("SELECT id_usuario FROM usuarios WHERE id_rol = (SELECT id_rol FROM roles WHERE nombre='administrador') AND activo = 1")->fetchAll();
    foreach ($admins as $a) {
        crearNotificacion((int)$a['id_usuario'], $titulo, $mensaje, $enlace);
    }
}

/* ===================================================================
   SUPLANTACIÓN DE CUENTA ("iniciar sesión como" otro usuario)
   -------------------------------------------------------------------
   Le permite a un administrador entrar a la cuenta de otro usuario
   SIN cerrar su propia sesión, para poder ayudarlo o revisar algo
   desde su punto de vista. Solo el administrador puede hacerlo, y
   solo si escribe el CODIGO_SUPLANTACION correcto (ver conexion.php).
   =================================================================== */

/** Guarda la sesión actual del administrador y "se convierte" en el usuario objetivo */
function iniciarSuplantacion(array $usuarioObjetivo): void {
    // Guarda quién es el administrador real, para poder volver después
    $_SESSION['suplantador'] = [
        'id_usuario' => $_SESSION['id_usuario'],
        'nombre'     => $_SESSION['nombre'],
        'correo'     => $_SESSION['correo'],
        'rol'        => $_SESSION['rol'],
        'avatar'     => $_SESSION['avatar'] ?? null,
    ];

    // Cambia la sesión activa al usuario objetivo (sin regenerar el id de
    // sesión aquí: si lo hiciéramos, perderíamos la variable 'suplantador'
    // que acabamos de guardar en la sesión actual)
    $_SESSION['id_usuario'] = $usuarioObjetivo['id_usuario'];
    $_SESSION['nombre']     = $usuarioObjetivo['nombre'];
    $_SESSION['correo']     = $usuarioObjetivo['correo'];
    $_SESSION['rol']        = $usuarioObjetivo['rol'];
    $_SESSION['avatar']     = $usuarioObjetivo['avatar'] ?? null;
}

/** ¿La sesión actual está suplantando a alguien? */
function estaSuplantando(): bool {
    return !empty($_SESSION['suplantador']);
}

/** Restaura la sesión del administrador original y termina la suplantación */
function volverDeSuplantacion(): void {
    if (!empty($_SESSION['suplantador'])) {
        $_SESSION['id_usuario'] = $_SESSION['suplantador']['id_usuario'];
        $_SESSION['nombre']     = $_SESSION['suplantador']['nombre'];
        $_SESSION['correo']     = $_SESSION['suplantador']['correo'];
        $_SESSION['rol']        = $_SESSION['suplantador']['rol'];
        $_SESSION['avatar']     = $_SESSION['suplantador']['avatar'] ?? null;
        unset($_SESSION['suplantador']);
    }
}
