<?php
/**
 * php/toggle_favorito.php
 * -----------------------------------------------------------------
 * Endpoint AJAX (responde JSON) que agrega o quita un producto
 * de los favoritos del cliente.
 * Si no ha iniciado sesión, responde ok para que el front-end
 * lo gestione en localStorage sin interrumpir la experiencia.
 * -----------------------------------------------------------------
 */
header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/auth.php';

$idProducto = (int)($_POST['id_producto'] ?? 0);
if ($idProducto <= 0) {
    echo json_encode(['ok' => false, 'mensaje' => 'ID de producto inválido.']);
    exit;
}

$u = usuarioActual();
if (!$u || $u['rol'] !== 'cliente') {
    // Usuario invitado: se almacena en el cliente vía localStorage
    echo json_encode([
        'ok' => true,
        'sincronizado_bd' => false,
        'mensaje' => 'Guardado en tu lista de compras local.'
    ]);
    exit;
}

// Para clientes registrados, sincronizamos con la base de datos MySQL
$stmt = $pdo->prepare('SELECT 1 FROM favoritos WHERE id_usuario = :u AND id_producto = :p');
$stmt->execute([':u' => $u['id_usuario'], ':p' => $idProducto]);

if ($stmt->fetch()) {
    $pdo->prepare('DELETE FROM favoritos WHERE id_usuario = :u AND id_producto = :p')
        ->execute([':u' => $u['id_usuario'], ':p' => $idProducto]);
    echo json_encode(['ok' => true, 'favorito' => false, 'sincronizado_bd' => true]);
} else {
    $pdo->prepare('INSERT INTO favoritos (id_usuario, id_producto) VALUES (:u, :p)')
        ->execute([':u' => $u['id_usuario'], ':p' => $idProducto]);
    echo json_encode(['ok' => true, 'favorito' => true, 'sincronizado_bd' => true]);
}
