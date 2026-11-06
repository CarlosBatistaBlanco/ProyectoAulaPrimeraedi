<?php
/**
 * procesar_pedido.php
 * -----------------------------------------------------------------
 * Recibe (por POST, en JSON) los datos del cliente + el carrito
 * que armó el usuario en el front (guardado en localStorage por
 * js/carrito.js) y genera:
 *   1) un registro en `clientes` (o reutiliza uno existente por correo)
 *   2) un registro en `pedidos`
 *   3) un registro por cada producto en `pedido_detalle`
 *
 * Entrada esperada (JSON):
 * {
 *   "nombre": "...", "correo": "...", "telefono": "...", "direccion": "...",
 *   "carrito": [ {"id_producto": 1, "cantidad": 2, "precio": 249900 }, ... ]
 * }
 * -----------------------------------------------------------------
 */

header('Content-Type: application/json; charset=utf-8');
require_once __DIR__ . '/conexion.php';

$datos = json_decode(file_get_contents('php://input'), true);

if (!$datos || empty($datos['carrito']) || !is_array($datos['carrito'])) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'mensaje' => 'El carrito está vacío o los datos son inválidos.']);
    exit;
}

$nombre    = htmlspecialchars(trim($datos['nombre']    ?? ''), ENT_QUOTES, 'UTF-8');
$correo    = htmlspecialchars(trim($datos['correo']    ?? ''), ENT_QUOTES, 'UTF-8');
$telefono  = htmlspecialchars(trim($datos['telefono']  ?? ''), ENT_QUOTES, 'UTF-8');
$direccion = htmlspecialchars(trim($datos['direccion'] ?? ''), ENT_QUOTES, 'UTF-8');
$carrito   = $datos['carrito'];

if ($nombre === '' || !filter_var($correo, FILTER_VALIDATE_EMAIL)) {
    echo json_encode(['ok' => false, 'mensaje' => 'Nombre o correo inválidos.']);
    exit;
}

try {
    // Toda la operación en una transacción: o se guarda todo, o nada
    $pdo->beginTransaction();

    // 1) Buscar si el cliente ya existe por correo, si no, crearlo
    $stmt = $pdo->prepare('SELECT id_cliente FROM clientes WHERE correo = :correo LIMIT 1');
    $stmt->execute([':correo' => $correo]);
    $cliente = $stmt->fetch();

    if ($cliente) {
        $idCliente = $cliente['id_cliente'];
    } else {
        $stmt = $pdo->prepare('INSERT INTO clientes (nombre, correo, telefono, direccion)
                                VALUES (:nombre, :correo, :telefono, :direccion)');
        $stmt->execute([
            ':nombre' => $nombre, ':correo' => $correo,
            ':telefono' => $telefono, ':direccion' => $direccion
        ]);
        $idCliente = $pdo->lastInsertId();
    }

    // 2) Calcular el total validando precio real contra la BD (nunca confiar en el front)
    $total = 0;
    $lineas = [];
    $stmtProd = $pdo->prepare('SELECT id_producto, nombre, precio, stock FROM productos WHERE id_producto = :id AND activo = 1');

    foreach ($carrito as $item) {
        $idProducto = (int)($item['id_producto'] ?? 0);
        $cantidad   = max(1, (int)($item['cantidad'] ?? 1));

        $stmtProd->execute([':id' => $idProducto]);
        $producto = $stmtProd->fetch();

        if (!$producto) continue; // ignora productos inválidos
        if ($producto['stock'] < $cantidad) {
            throw new Exception('Stock insuficiente para: ' . $producto['nombre']);
        }

        $subtotal = $producto['precio'] * $cantidad;
        $total += $subtotal;
        $lineas[] = [
            'id_producto'     => $idProducto,
            'cantidad'        => $cantidad,
            'precio_unitario' => $producto['precio'],
        ];
    }

    if (empty($lineas)) {
        throw new Exception('Ningún producto del carrito es válido.');
    }

    // 3) Crear el pedido
    $stmt = $pdo->prepare('INSERT INTO pedidos (id_cliente, total, estado) VALUES (:id_cliente, :total, "pendiente")');
    $stmt->execute([':id_cliente' => $idCliente, ':total' => $total]);
    $idPedido = $pdo->lastInsertId();

    // 4) Insertar el detalle y descontar stock
    $stmtDetalle = $pdo->prepare('INSERT INTO pedido_detalle (id_pedido, id_producto, cantidad, precio_unitario)
                                   VALUES (:id_pedido, :id_producto, :cantidad, :precio_unitario)');
    $stmtStock = $pdo->prepare('UPDATE productos SET stock = stock - :cantidad WHERE id_producto = :id_producto');

    foreach ($lineas as $l) {
        $stmtDetalle->execute([
            ':id_pedido' => $idPedido,
            ':id_producto' => $l['id_producto'],
            ':cantidad' => $l['cantidad'],
            ':precio_unitario' => $l['precio_unitario'],
        ]);
        $stmtStock->execute([':cantidad' => $l['cantidad'], ':id_producto' => $l['id_producto']]);
    }

    $pdo->commit();

    echo json_encode([
        'ok' => true,
        'mensaje' => "¡Pedido #$idPedido registrado con éxito! Total: $" . number_format($total, 0, ',', '.'),
        'id_pedido' => $idPedido,
        'total' => $total
    ]);

} catch (Exception $e) {
    $pdo->rollBack();
    http_response_code(400);
    echo json_encode(['ok' => false, 'mensaje' => 'No se pudo procesar el pedido: ' . $e->getMessage()]);
}
