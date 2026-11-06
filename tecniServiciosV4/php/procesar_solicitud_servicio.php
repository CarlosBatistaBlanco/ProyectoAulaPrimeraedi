<?php
/**
 * procesar_solicitud_servicio.php
 * -----------------------------------------------------------------
 * Guarda la solicitud de cotización o cita técnica que el visitante
 * llenó en solicitar-servicio.php. No requiere haber iniciado sesión
 * (cualquier visitante puede pedir que lo contacten), pero si inició
 * sesión como cliente, sus datos ya vienen precargados desde el form.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';

function volverConError(int $idServicio, string $mensaje): void {
    header('Location: ../solicitar-servicio.php?id=' . $idServicio . '&error=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST') { header('Location: ../index.php#servicios'); exit; }

$idServicio = (int)($_POST['id_servicio'] ?? 0);

if (!csrfValido($_POST['csrf_token'] ?? null)) {
    volverConError($idServicio, 'Tu sesión expiró, intenta de nuevo.');
}

// Confirma que el servicio exista de verdad
$stmt = $pdo->prepare('SELECT id_servicio FROM servicios WHERE id_servicio = :id LIMIT 1');
$stmt->execute([':id' => $idServicio]);
if (!$stmt->fetch()) {
    header('Location: ../index.php#servicios');
    exit;
}

$nombre = trim($_POST['nombre'] ?? '');
$correo = trim($_POST['correo'] ?? '');
$telefono = trim($_POST['telefono'] ?? '');
$direccion = trim($_POST['direccion'] ?? '');
$tipo = ($_POST['tipo'] ?? 'cotizacion') === 'cita' ? 'cita' : 'cotizacion';
$fecha = trim($_POST['fecha_preferida'] ?? '');
$hora = trim($_POST['hora_preferida'] ?? '');
$descripcion = trim($_POST['descripcion'] ?? '');

// --- Validaciones del servidor ---
$errores = [];
if ($nombre === '') $errores[] = 'El nombre es obligatorio.';
if (!filter_var($correo, FILTER_VALIDATE_EMAIL)) $errores[] = 'El correo no es válido.';
if ($telefono === '') $errores[] = 'El teléfono es obligatorio.';
if ($descripcion === '') $errores[] = 'Cuéntanos brevemente qué necesitas.';
if ($tipo === 'cita' && ($fecha === '' || $hora === '')) {
    $errores[] = 'Para agendar una cita debes elegir fecha y hora.';
}
if ($tipo === 'cita' && $fecha !== '' && $fecha < date('Y-m-d')) {
    $errores[] = 'La fecha de la cita no puede ser en el pasado.';
}

if (!empty($errores)) {
    volverConError($idServicio, implode(' ', $errores));
}

$stmt = $pdo->prepare('INSERT INTO solicitudes_servicio
    (id_servicio, nombre, correo, telefono, direccion, tipo, fecha_preferida, hora_preferida, descripcion)
    VALUES (:serv, :nombre, :correo, :tel, :dir, :tipo, :fecha, :hora, :desc)');
$stmt->execute([
    ':serv' => $idServicio,
    ':nombre' => $nombre,
    ':correo' => $correo,
    ':tel' => $telefono,
    ':dir' => $direccion,
    ':tipo' => $tipo,
    ':fecha' => $tipo === 'cita' ? $fecha : null,
    ':hora'  => $tipo === 'cita' ? $hora : null,
    ':desc' => $descripcion,
]);

$sesion = usuarioActual();
registrarLog($sesion['id_usuario'] ?? null, 'solicitud_servicio', "Servicio #$idServicio ($tipo)");

$mensaje = $tipo === 'cita'
    ? '¡Listo! Tu cita quedó registrada. Un asesor confirmará el horario contigo por teléfono o correo.'
    : '¡Listo! Recibimos tu solicitud de cotización. Un asesor te contactará pronto.';

header('Location: ../solicitar-servicio.php?id=' . $idServicio . '&exito=' . urlencode($mensaje));
exit;
