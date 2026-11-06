<?php
/**
 * php/enviar_mensaje_chat.php
 * -----------------------------------------------------------------
 * Endpoint AJAX (responde JSON) que guarda un mensaje nuevo de chat, ya sea escrito por un cliente o por un asesor (administrador/empleado).
 * -----------------------------------------------------------------
 */
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/auth.php';
$u = requerirLogin();

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    echo json_encode(['ok' => false, 'mensaje' => 'Sesión expirada.']); exit;
}

$idCliente = (int)($_POST['id_cliente'] ?? 0);
$mensaje = trim($_POST['mensaje'] ?? '');
if ($idCliente <= 0 || $mensaje === '') { echo json_encode(['ok' => false]); exit; }

// Determina el remitente y valida permiso: el cliente solo puede escribir en SU propio hilo
if ($u['rol'] === 'cliente') {
    $stmt = $pdo->prepare('SELECT id_cliente FROM clientes WHERE id_usuario = :u AND id_cliente = :c');
    $stmt->execute([':u' => $u['id_usuario'], ':c' => $idCliente]);
    if (!$stmt->fetch()) { echo json_encode(['ok' => false, 'mensaje' => 'No autorizado.']); exit; }
    $remitente = 'cliente';
} elseif (in_array($u['rol'], ['administrador', 'empleado'], true)) {
    $remitente = 'asesor';
} else {
    echo json_encode(['ok' => false]); exit;
}

$stmt = $pdo->prepare('INSERT INTO mensajes_chat (id_cliente, remitente, id_usuario_envia, mensaje) VALUES (:c,:r,:u,:m)');
$stmt->execute([':c' => $idCliente, ':r' => $remitente, ':u' => $u['id_usuario'], ':m' => $mensaje]);

// Notifica a la otra parte
if ($remitente === 'cliente') {
    notificarAdministradores('Nuevo mensaje de chat', $u['nombre'] . ' te escribió.', 'panel/administrador/chat.php?cliente=' . $idCliente);
} else {
    $clienteRow = $pdo->prepare('SELECT id_usuario FROM clientes WHERE id_cliente = :id');
    $clienteRow->execute([':id' => $idCliente]);
    $idUsuarioCliente = $clienteRow->fetchColumn();
    if ($idUsuarioCliente) {
        crearNotificacion((int)$idUsuarioCliente, 'Nueva respuesta de un asesor', '', 'panel/cliente/chat.php');
    }
}

echo json_encode(['ok' => true]);
