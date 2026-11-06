<?php
/**
 * php/procesar_restablecer.php
 * -----------------------------------------------------------------
 * Backend que valida el token de recuperación y guarda la nueva contraseña del usuario.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';

function volverConError(string $token, string $mensaje): void {
    header('Location: ../restablecer.php?token=' . urlencode($token) . '&error=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: ../login.php'); exit; }

$token = $_POST['token'] ?? '';
$password = $_POST['password'] ?? '';
$password2 = $_POST['password2'] ?? '';

if (!csrfValido($_POST['csrf_token'] ?? null)) {
    volverConError($token, 'Tu sesión expiró, intenta de nuevo.');
}

$stmt = $pdo->prepare('SELECT * FROM tokens_recuperacion WHERE token = :t AND usado = 0 AND expira_en > NOW() LIMIT 1');
$stmt->execute([':t' => $token]);
$fila = $stmt->fetch();

if (!$fila) {
    header('Location: ../recuperar.php?error=' . urlencode('El enlace ya no es válido, solicita uno nuevo.'));
    exit;
}

if (strlen($password) < 8 || !preg_match('/[A-Z]/', $password) || !preg_match('/[0-9]/', $password)) {
    volverConError($token, 'La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.');
}
if ($password !== $password2) {
    volverConError($token, 'Las contraseñas no coinciden.');
}

$pdo->beginTransaction();
$stmt = $pdo->prepare('UPDATE usuarios SET password_hash = :h, intentos_fallidos = 0, bloqueado = 0 WHERE id_usuario = :id');
$stmt->execute([':h' => password_hash($password, PASSWORD_DEFAULT), ':id' => $fila['id_usuario']]);

$stmt = $pdo->prepare('UPDATE tokens_recuperacion SET usado = 1 WHERE id_token = :id');
$stmt->execute([':id' => $fila['id_token']]);
$pdo->commit();

registrarLog($fila['id_usuario'], 'restablecimiento_password', '');

header('Location: ../login.php?exito=' . urlencode('Contraseña actualizada. Ya puedes iniciar sesión.'));
exit;
