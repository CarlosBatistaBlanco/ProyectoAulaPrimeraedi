<?php
/**
 * php/procesar_resena.php
 * -----------------------------------------------------------------
 * Backend que guarda (o actualiza, si ya existía) la calificación con estrellas y el comentario que un cliente deja sobre un producto que compró.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirRol(['cliente']);

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    header('Location: ../panel/cliente/pedidos.php');
    exit;
}

$idProducto = (int)($_POST['id_producto'] ?? 0);
$calificacion = max(1, min(5, (int)($_POST['calificacion'] ?? 5)));
$comentario = trim($_POST['comentario'] ?? '');

// Verifica de nuevo que haya comprado el producto (nunca confiar solo en el formulario)
$stmt = $pdo->prepare("SELECT COUNT(*) FROM pedido_detalle pd
    JOIN pedidos p ON p.id_pedido = pd.id_pedido
    JOIN clientes c ON c.id_cliente = p.id_cliente
    WHERE c.id_usuario = :u AND pd.id_producto = :prod");
$stmt->execute([':u' => $u['id_usuario'], ':prod' => $idProducto]);
if ($stmt->fetchColumn() == 0) {
    header('Location: ../panel/cliente/pedidos.php?error=' . urlencode('Solo puedes calificar productos comprados.'));
    exit;
}

// INSERT ... ON DUPLICATE KEY UPDATE: si ya existía la reseña (UNIQUE en id_producto+id_usuario), la actualiza
$stmt = $pdo->prepare('INSERT INTO resenas (id_producto, id_usuario, calificacion, comentario)
                        VALUES (:p, :u, :c, :com)
                        ON DUPLICATE KEY UPDATE calificacion = :c2, comentario = :com2');
$stmt->execute([
    ':p' => $idProducto, ':u' => $u['id_usuario'], ':c' => $calificacion, ':com' => $comentario,
    ':c2' => $calificacion, ':com2' => $comentario,
]);

registrarLog($u['id_usuario'], 'crear_resena', "Producto #$idProducto: $calificacion estrellas");

header('Location: ../panel/cliente/resenar.php?producto=' . $idProducto . '&exito=' . urlencode('¡Gracias por tu reseña!'));
exit;
