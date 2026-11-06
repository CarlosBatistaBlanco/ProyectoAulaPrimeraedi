<?php
/**
 * php/admin_pedidos.php
 * -----------------------------------------------------------------
 * Backend de gestión de pedidos: cambia el estado de un pedido (con historial), y genera o anula su factura.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirRol(['administrador', 'empleado']);

function volver(int $idPedido, string $mensaje, string $tipo = 'exito'): void {
    header('Location: ../panel/administrador/pedido_detalle.php?id=' . $idPedido . '&' . $tipo . '=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    header('Location: ../panel/administrador/pedidos.php');
    exit;
}

$accion = $_POST['accion'] ?? '';
$idPedido = (int)($_POST['id_pedido'] ?? 0);

$stmt = $pdo->prepare('SELECT * FROM pedidos WHERE id_pedido = :id');
$stmt->execute([':id' => $idPedido]);
$pedido = $stmt->fetch();
if (!$pedido) { header('Location: ../panel/administrador/pedidos.php'); exit; }

if ($accion === 'cambiar_estado') {
    $nuevoEstado = $_POST['estado'] ?? '';
    $estadosValidos = ['pendiente','confirmado','en_preparacion','empacado','enviado','entregado','cancelado'];
    if (!in_array($nuevoEstado, $estadosValidos, true)) volver($idPedido, 'Estado no válido.', 'error');

    $pdo->prepare('UPDATE pedidos SET estado = :e WHERE id_pedido = :id')->execute([':e' => $nuevoEstado, ':id' => $idPedido]);
    $pdo->prepare('INSERT INTO pedidos_historial (id_pedido, estado_anterior, estado_nuevo, id_usuario) VALUES (:p,:ant,:nue,:u)')
        ->execute([':p' => $idPedido, ':ant' => $pedido['estado'], ':nue' => $nuevoEstado, ':u' => $u['id_usuario']]);

    registrarLog($u['id_usuario'], 'cambiar_estado_pedido', "Pedido #$idPedido: {$pedido['estado']} -> $nuevoEstado");

    // Notifica al cliente dueño del pedido, si tiene cuenta
    if ($pedido['id_cliente']) {
        $cliente = $pdo->prepare('SELECT id_usuario FROM clientes WHERE id_cliente = :id');
        $cliente->execute([':id' => $pedido['id_cliente']]);
        $idUsuarioCliente = $cliente->fetchColumn();
        if ($idUsuarioCliente) {
            crearNotificacion((int)$idUsuarioCliente, 'Tu pedido #' . $idPedido . ' cambió de estado',
                'Ahora está: ' . ucfirst(str_replace('_', ' ', $nuevoEstado)), 'panel/cliente/pedidos.php');
        }
    }

    volver($idPedido, 'Estado del pedido actualizado.');
}

if ($accion === 'generar_factura') {
    $existente = $pdo->prepare('SELECT id_factura FROM facturas WHERE id_pedido = :id');
    $existente->execute([':id' => $idPedido]);
    if ($existente->fetch()) volver($idPedido, 'Este pedido ya tiene una factura.', 'error');

    $subtotal = $pedido['total'];
    $numero = 'FAC-' . date('Y') . '-' . str_pad($idPedido, 5, '0', STR_PAD_LEFT);

    $pdo->prepare('INSERT INTO facturas (id_pedido, numero_factura, subtotal, impuestos, descuento, total)
                    VALUES (:p, :num, :sub, 0, 0, :total)')
        ->execute([':p' => $idPedido, ':num' => $numero, ':sub' => $subtotal, ':total' => $subtotal]);

    registrarLog($u['id_usuario'], 'generar_factura', "Pedido #$idPedido -> $numero");
    volver($idPedido, 'Factura generada correctamente.');
}

if ($accion === 'anular_factura') {
    $pdo->prepare("UPDATE facturas SET estado = 'anulada' WHERE id_pedido = :id")->execute([':id' => $idPedido]);
    registrarLog($u['id_usuario'], 'anular_factura', "Pedido #$idPedido");
    volver($idPedido, 'Factura anulada.');
}

volver($idPedido, 'Acción no reconocida.', 'error');
