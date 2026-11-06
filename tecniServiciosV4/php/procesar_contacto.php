<?php
/**
 * procesar_contacto.php
 * -----------------------------------------------------------------
 * Recibe los datos del formulario de contacto (método POST),
 * los valida/limpia y los guarda en la tabla `contactos`.
 * Responde en JSON para que main.js pueda mostrar un mensaje
 * sin recargar la página (fetch / AJAX).
 * -----------------------------------------------------------------
 */

header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/auth.php';

// Solo aceptar peticiones POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'mensaje' => 'Método no permitido']);
    exit;
}

// Función corta para limpiar texto de entrada
function limpiar($valor) {
    return htmlspecialchars(trim($valor ?? ''), ENT_QUOTES, 'UTF-8');
}

$nombre   = limpiar($_POST['nombre']   ?? '');
$correo   = limpiar($_POST['correo']   ?? '');
$telefono = limpiar($_POST['telefono'] ?? '');
$asunto   = limpiar($_POST['asunto']   ?? '');
$tipo     = in_array($_POST['tipo'] ?? '', ['mensaje','reclamo','sugerencia'], true) ? $_POST['tipo'] : 'mensaje';
$mensaje  = limpiar($_POST['mensaje']  ?? '');

// Validación básica en el servidor
$errores = [];
if ($nombre === '')  $errores[] = 'El nombre es obligatorio.';
if (!filter_var($correo, FILTER_VALIDATE_EMAIL)) $errores[] = 'El correo no es válido.';
if ($mensaje === '') $errores[] = 'El mensaje no puede estar vacío.';

if (!empty($errores)) {
    echo json_encode(['ok' => false, 'mensaje' => implode(' ', $errores)]);
    exit;
}

try {
    $sql = 'INSERT INTO contactos (nombre, correo, telefono, asunto, tipo, mensaje)
            VALUES (:nombre, :correo, :telefono, :asunto, :tipo, :mensaje)';
    $stmt = $pdo->prepare($sql);
    $stmt->execute([
        ':nombre'   => $nombre,
        ':correo'   => $correo,
        ':telefono' => $telefono,
        ':asunto'   => $asunto,
        ':tipo'     => $tipo,
        ':mensaje'  => $mensaje,
    ]);

    notificarAdministradores('Nuevo ' . $tipo, $nombre . ': ' . recortar_texto($mensaje, 60), 'panel/administrador/soporte.php');

    echo json_encode([
        'ok' => true,
        'mensaje' => '¡Gracias ' . $nombre . '! Tu mensaje fue enviado, te contactaremos pronto.'
    ]);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'mensaje' => 'Error guardando el mensaje: ' . $e->getMessage()]);
}
