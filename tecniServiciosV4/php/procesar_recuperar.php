<?php
/**
 * procesar_recuperar.php
 * -----------------------------------------------------------------
 * Genera un token de recuperación de contraseña válido por 1 hora.
 *
 * IMPORTANTE (léelo): en un sitio en producción, este archivo enviaría
 * el enlace por CORREO REAL usando PHPMailer + un servidor SMTP (Gmail,
 * SendGrid, etc.). Configurar el envío real de correos es la Fase 5
 * del proyecto ("Correos Automáticos"), porque requiere credenciales
 * SMTP que solo tú puedes proporcionar (usuario/clave de tu correo o
 * de un servicio como SendGrid). Mientras tanto, para que puedas
 * PROBAR todo el flujo, el enlace se muestra directamente en pantalla
 * en vez de enviarse por correo.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: ../recuperar.php'); exit; }

if (!csrfValido($_POST['csrf_token'] ?? null)) {
    header('Location: ../recuperar.php?error=' . urlencode('Tu sesión expiró, intenta de nuevo.'));
    exit;
}

$correo = trim($_POST['correo'] ?? '');
$mensajeGenerico = 'Si el correo existe en nuestro sistema, se generó un enlace de recuperación.';

$stmt = $pdo->prepare('SELECT id_usuario FROM usuarios WHERE correo = :c LIMIT 1');
$stmt->execute([':c' => $correo]);
$usuario = $stmt->fetch();

// No revela si el correo existe o no (evita que alguien "descubra" correos registrados)
if (!$usuario) {
    header('Location: ../recuperar.php?exito=' . urlencode($mensajeGenerico));
    exit;
}

$token = bin2hex(random_bytes(32));
$expira = date('Y-m-d H:i:s', time() + 3600); // 1 hora

$stmt = $pdo->prepare('INSERT INTO tokens_recuperacion (id_usuario, token, expira_en) VALUES (:u, :t, :e)');
$stmt->execute([':u' => $usuario['id_usuario'], ':t' => $token, ':e' => $expira]);
registrarLog($usuario['id_usuario'], 'solicitud_recuperacion', '');

$enlace = 'restablecer.php?token=' . $token;

// --- Aquí, en producción, iría el envío real por PHPMailer/SMTP ---
header('Location: ../recuperar.php?enlace_demo=' . urlencode($enlace) .
       '&exito=' . urlencode($mensajeGenerico));
exit;
