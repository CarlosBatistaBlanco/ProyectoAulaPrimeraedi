<?php
/**
 * php/empleado_ventas.php
 * -----------------------------------------------------------------
 * Backend que registra una venta manual/presencial hecha por un empleado: crea (o reutiliza) el cliente, descuenta stock y deja registro en el Kardex.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirRol(['empleado', 'administrador']);

function volver(string $mensaje, string $tipo = 'exito'): void {
    header('Location: ../panel/empleado/ventas.php?' . $tipo . '=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    volver('Solicitud inválida o sesión expirada.', 'error');
}

$idClienteExistente = (int)($_POST['id_cliente_existente'] ?? 0);
$productos = $_POST['productos'] ?? [];
$cantidades = $_POST['cantidades'] ?? [];

if (empty($productos)) volver('Debes agregar al menos un producto.', 'error');

try {
    $pdo->beginTransaction();

    // Cliente: usa uno existente o crea uno nuevo con los datos del formulario
    if ($idClienteExistente > 0) {
        $idCliente = $idClienteExistente;
    } else {
        $nombre = trim($_POST['nombre_cliente'] ?? '');
        $correo = trim($_POST['correo_cliente'] ?? '');
        $telefono = trim($_POST['telefono_cliente'] ?? '');
        if ($nombre === '') throw new Exception('Escribe el nombre del cliente nuevo o elige uno existente.');

        $stmt = $pdo->prepare('INSERT INTO clientes (nombre, correo, telefono) VALUES (:n, :c, :t)');
        $stmt->execute([':n' => $nombre, ':c' => $correo ?: 'sin-correo+' . time() . '@tecnicsecure.com', ':t' => $telefono]);
        $idCliente = $pdo->lastInsertId();
    }

    // Calcula el total validando precio y stock real de cada producto
    $total = 0;
    $lineas = [];
    $stmtProd = $pdo->prepare('SELECT * FROM productos WHERE id_producto = :id AND activo = 1');
    foreach ($productos as $i => $idProducto) {
        $cantidad = max(1, (int)($cantidades[$i] ?? 1));
        $stmtProd->execute([':id' => (int)$idProducto]);
        $producto = $stmtProd->fetch();
        if (!$producto) continue;
        if ($producto['stock'] < $cantidad) {
            throw new Exception('Stock insuficiente para: ' . $producto['nombre']);
        }
        $total += $producto['precio'] * $cantidad;
        $lineas[] = ['id_producto' => $producto['id_producto'], 'cantidad' => $cantidad, 'precio' => $producto['precio'], 'nombre' => $producto['nombre']];
    }
    if (empty($lineas)) throw new Exception('Ningún producto válido fue seleccionado.');

    $stmt = $pdo->prepare('INSERT INTO pedidos (id_cliente, id_empleado_registro, total, estado) VALUES (:c, :e, :t, "confirmado")');
    $stmt->execute([':c' => $idCliente, ':e' => $u['id_usuario'], ':t' => $total]);
    $idPedido = $pdo->lastInsertId();

    $stmtDetalle = $pdo->prepare('INSERT INTO pedido_detalle (id_pedido, id_producto, cantidad, precio_unitario) VALUES (:p,:prod,:c,:pu)');
    $stmtStock = $pdo->prepare('UPDATE productos SET stock = stock - :c WHERE id_producto = :id');
    $stmtMov = $pdo->prepare('INSERT INTO inventario_movimientos (id_producto, id_usuario, tipo, cantidad, stock_resultante, motivo)
                               VALUES (:p,:u,"salida",:c,:sr,:m)');

    foreach ($lineas as $l) {
        $stmtDetalle->execute([':p' => $idPedido, ':prod' => $l['id_producto'], ':c' => $l['cantidad'], ':pu' => $l['precio']]);
        $stmtStock->execute([':c' => $l['cantidad'], ':id' => $l['id_producto']]);

        $stockRow = $pdo->prepare('SELECT stock FROM productos WHERE id_producto = :id');
        $stockRow->execute([':id' => $l['id_producto']]);
        $stmtMov->execute([':p' => $l['id_producto'], ':u' => $u['id_usuario'], ':c' => $l['cantidad'],
                            ':sr' => $stockRow->fetchColumn(), ':m' => "Venta manual #$idPedido registrada por " . $u['nombre']]);
    }

    $pdo->prepare('INSERT INTO pedidos_historial (id_pedido, estado_anterior, estado_nuevo, id_usuario) VALUES (:p, NULL, "confirmado", :u)')
        ->execute([':p' => $idPedido, ':u' => $u['id_usuario']]);

    $pdo->commit();
    registrarLog($u['id_usuario'], 'venta_manual', "Pedido #$idPedido por $" . number_format($total, 0, ',', '.'));
    volver('Venta registrada correctamente. Pedido #' . $idPedido . ' por $' . number_format($total, 0, ',', '.'));

} catch (Exception $e) {
    $pdo->rollBack();
    volver($e->getMessage(), 'error');
}
