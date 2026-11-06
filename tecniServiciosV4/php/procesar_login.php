<?php
/**
 * procesar_login.php
 * -----------------------------------------------------------------
 * Valida el formulario de login paso a paso: CSRF → captcha →
 * credenciales → bloqueo por intentos fallidos. Nunca revela al
 * usuario CUÁL de los datos está mal (por seguridad, se muestra
 * siempre el mismo mensaje genérico "correo o contraseña incorrectos").
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';

function volverConError(string $mensaje, string $redir = ''): void {
    $qs = 'error=' . urlencode($mensaje) . ($redir ? '&redir=' . urlencode($redir) : '');
    header('Location: ../login.php?' . $qs);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Location: ../login.php');
    exit;
}

$redir = $_POST['redir'] ?? '';

if (!csrfValido($_POST['csrf_token'] ?? null)) {
    volverConError('Tu sesión expiró, intenta de nuevo.', $redir);
}

if (!captchaValido($_POST['captcha'] ?? null)) {
    volverConError('El código de verificación no es correcto.', $redir);
}

$correo = trim($_POST['correo'] ?? '');
$password = $_POST['password'] ?? '';

if (!filter_var($correo, FILTER_VALIDATE_EMAIL) || $password === '') {
    volverConError('Correo o contraseña incorrectos.', $redir);
}

$stmt = $pdo->prepare('SELECT u.*, r.nombre AS rol FROM usuarios u
                        JOIN roles r ON r.id_rol = u.id_rol
                        WHERE u.correo = :correo LIMIT 1');
$stmt->execute([':correo' => $correo]);
$usuario = $stmt->fetch();

// Mensaje genérico siempre igual: no delatar si el correo existe o no
$mensajeGenerico = 'Correo o contraseña incorrectos.';

if (!$usuario) {
    volverConError($mensajeGenerico, $redir);
}

if ($usuario['bloqueado']) {
    // El bloqueo es temporal (15 minutos), no permanente. Si ya pasó ese
    // tiempo, se desbloquea solo y el login continúa normalmente.
    $yaExpiro = !empty($usuario['bloqueado_hasta']) && strtotime($usuario['bloqueado_hasta']) <= time();
    if ($yaExpiro) {
        $pdo->prepare('UPDATE usuarios SET bloqueado = 0, bloqueado_hasta = NULL, intentos_fallidos = 0 WHERE id_usuario = :id')
            ->execute([':id' => $usuario['id_usuario']]);
        $usuario['bloqueado'] = 0;
    } else {
        $minutosRestantes = !empty($usuario['bloqueado_hasta'])
            ? max(1, (int)ceil((strtotime($usuario['bloqueado_hasta']) - time()) / 60))
            : 15;
        volverConError("Demasiados intentos fallidos. Intenta de nuevo en $minutosRestantes minuto(s), o contacta al administrador.", $redir);
    }
}

if (!$usuario['activo']) {
    volverConError('Tu cuenta está inactiva. Contacta al administrador.', $redir);
}

if (!password_verify($password, $usuario['password_hash'])) {
    // Suma un intento fallido; bloquea automáticamente (15 min) al llegar a 5
    $intentos = (int)$usuario['intentos_fallidos'] + 1;
    $bloquear = $intentos >= 5 ? 1 : 0;
    $bloqueadoHasta = $bloquear ? date('Y-m-d H:i:s', time() + 15 * 60) : null;
    $stmt = $pdo->prepare('UPDATE usuarios SET intentos_fallidos = :i, bloqueado = :b, bloqueado_hasta = :bh WHERE id_usuario = :id');
    $stmt->execute([':i' => $intentos, ':b' => $bloquear, ':bh' => $bloqueadoHasta, ':id' => $usuario['id_usuario']]);
    registrarLog($usuario['id_usuario'], 'login_fallido', "Intento $intentos de 5");

    if ($bloquear) {
        volverConError('Demasiados intentos fallidos. Tu cuenta se bloqueó por 15 minutos.', $redir);
    }
    volverConError($mensajeGenerico, $redir);
}

// --- Login correcto ---
$stmt = $pdo->prepare('UPDATE usuarios SET intentos_fallidos = 0, ultimo_acceso = NOW() WHERE id_usuario = :id');
$stmt->execute([':id' => $usuario['id_usuario']]);

iniciarSesionUsuario($usuario);
registrarLog($usuario['id_usuario'], 'login_exitoso', '');

if (!empty($_POST['recordarme'])) {
    crearCookieRecordarme($usuario['id_usuario']);
}

if ($redir) {
    header('Location: ..' . $redir);
} else {
    header('Location: ../panel/' . $usuario['rol'] . '/index.php');
}
exit;
