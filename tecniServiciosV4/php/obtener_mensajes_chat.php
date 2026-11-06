<?php
/**
 * php/obtener_mensajes_chat.php
 * -----------------------------------------------------------------
 * Endpoint AJAX (responde JSON) que devuelve los mensajes de un chat. Lo consulta js/panel.js cada 4 segundos para refrescar la conversación sin recargar la página.
 * -----------------------------------------------------------------
 */
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/auth.php';
$u = requerirLogin();

$idCliente = (int)($_GET['id_cliente'] ?? 0);

if ($u['rol'] === 'cliente') {
    $stmt = $pdo->prepare('SELECT id_cliente FROM clientes WHERE id_usuario = :u AND id_cliente = :c');
    $stmt->execute([':u' => $u['id_usuario'], ':c' => $idCliente]);
    if (!$stmt->fetch()) { echo json_encode([]); exit; }
}

$stmt = $pdo->prepare('SELECT remitente, mensaje, creado_en FROM mensajes_chat WHERE id_cliente = :id ORDER BY creado_en ASC');
$stmt->execute([':id' => $idCliente]);
echo json_encode($stmt->fetchAll());
