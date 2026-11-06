<?php
/**
 * procesar_registro.php
 * -----------------------------------------------------------------
 * Crea una cuenta nueva con rol "cliente". Valida todo del lado del
 * servidor (nunca confía solo en la validación de registro.js):
 * correo único, documento único, contraseña segura y coincidente,
 * aceptación de términos y captcha.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';

function volverConError(string $mensaje): void {
    header('Location: ../registro.php?error=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: ../registro.php'); exit; }

if (!csrfValido($_POST['csrf_token'] ?? null)) {
    volverConError('Tu sesión expiró, vuelve a intentarlo.');
}
if (!captchaValido($_POST['captcha'] ?? null)) {
    volverConError('El código de verificación no es correcto.');
}

$nombre    = trim($_POST['nombre'] ?? '');
$documento = trim($_POST['documento'] ?? '');
$correo    = trim($_POST['correo'] ?? '');
$telefono  = trim($_POST['telefono'] ?? '');
$password  = $_POST['password'] ?? '';
$password2 = $_POST['password2'] ?? '';
$terminos  = isset($_POST['terminos']);

// --- Validaciones del servidor ---
if ($nombre === '' || $documento === '') {
    volverConError('Nombre y documento son obligatorios.');
}
if (!filter_var($correo, FILTER_VALIDATE_EMAIL)) {
    volverConError('El correo no es válido.');
}
if (!$terminos) {
    volverConError('Debes aceptar los términos y condiciones.');
}
if (strlen($password) < 8 || !preg_match('/[A-Z]/', $password) || !preg_match('/[0-9]/', $password)) {
    volverConError('La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.');
}
if ($password !== $password2) {
    volverConError('Las contraseñas no coinciden.');
}

// Correo único
$stmt = $pdo->prepare('SELECT id_usuario FROM usuarios WHERE correo = :c LIMIT 1');
$stmt->execute([':c' => $correo]);
if ($stmt->fetch()) {
    volverConError('Ese correo ya está registrado. Intenta iniciar sesión.');
}

// Documento único
$stmt = $pdo->prepare('SELECT id_usuario FROM usuarios WHERE documento = :d LIMIT 1');
$stmt->execute([':d' => $documento]);
if ($stmt->fetch()) {
    volverConError('Ese documento ya está registrado.');
}

try {
    $pdo->beginTransaction();

    $idRolCliente = $pdo->query("SELECT id_rol FROM roles WHERE nombre = 'cliente'")->fetchColumn();

    $stmt = $pdo->prepare('INSERT INTO usuarios (id_rol, nombre, correo, documento, telefono, password_hash)
                            VALUES (:rol, :nombre, :correo, :doc, :tel, :hash)');
    $stmt->execute([
        ':rol' => $idRolCliente, ':nombre' => $nombre, ':correo' => $correo,
        ':doc' => $documento, ':tel' => $telefono,
        ':hash' => password_hash($password, PASSWORD_DEFAULT),
    ]);
    $idUsuario = $pdo->lastInsertId();

    // Crea también la fila en `clientes` (usada por el carrito/pedidos existente)
    $stmt = $pdo->prepare('INSERT INTO clientes (id_usuario, nombre, correo, telefono) VALUES (:u, :n, :c, :t)');
    $stmt->execute([':u' => $idUsuario, ':n' => $nombre, ':c' => $correo, ':t' => $telefono]);

    $pdo->commit();
    registrarLog($idUsuario, 'registro', 'Nueva cuenta de cliente');

} catch (Exception $e) {
    $pdo->rollBack();
    volverConError('No se pudo crear la cuenta. Intenta de nuevo.');
}

header('Location: ../login.php?exito=' . urlencode('Cuenta creada con éxito. Ya puedes iniciar sesión.'));
exit;
