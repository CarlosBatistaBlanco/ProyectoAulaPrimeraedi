<?php
/**
 * php/admin_suplantar.php
 * -----------------------------------------------------------------
 * Recibe el formulario "Iniciar sesión como este usuario" desde
 * panel/administrador/usuarios.php. Exige: sesión de administrador,
 * CSRF válido, y el CODIGO_SUPLANTACION correcto (definido en
 * php/conexion.php) — así, aunque alguien tomara control de la
 * sesión del administrador, no podría suplantar cuentas sin conocer
 * también ese código.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$admin = requerirRol(['administrador']);

function volver(string $mensaje, string $tipo = 'error'): void {
    header('Location: ../panel/administrador/usuarios.php?' . $tipo . '=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    volver('Solicitud inválida o sesión expirada.');
}

// No se puede suplantar mientras ya se está suplantando a alguien más
if (estaSuplantando()) {
    volver('Ya estás viendo el sistema como otro usuario. Vuelve primero a tu cuenta de administrador.');
}

$codigo = $_POST['codigo_verificacion'] ?? '';
if (!hash_equals(CODIGO_SUPLANTACION, $codigo)) {
    registrarLog($admin['id_usuario'], 'suplantacion_codigo_incorrecto', '');
    volver('El código de verificación no es correcto.');
}

$idObjetivo = (int)($_POST['id_usuario'] ?? 0);
if ($idObjetivo === $admin['id_usuario']) {
    volver('Ya eres tú mismo, no puedes suplantarte.');
}

$stmt = $pdo->prepare("SELECT u.*, r.nombre AS rol FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol WHERE u.id_usuario = :id");
$stmt->execute([':id' => $idObjetivo]);
$objetivo = $stmt->fetch();

if (!$objetivo || !$objetivo['activo']) {
    volver('Ese usuario no existe o está inactivo.');
}

iniciarSuplantacion($objetivo);
registrarLog($admin['id_usuario'], 'suplantacion_iniciada', "Ahora viendo como usuario #{$objetivo['id_usuario']} ({$objetivo['correo']})");

header('Location: ../panel/' . $objetivo['rol'] . '/index.php');
exit;
