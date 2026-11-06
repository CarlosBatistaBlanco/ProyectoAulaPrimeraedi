<?php
/**
 * php/admin_inventario.php
 * -----------------------------------------------------------------
 * Backend del módulo de Inventario: procesa un movimiento (entrada, salida o ajuste) y actualiza el stock del producto, dejando registro en el Kardex.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirRol(['administrador', 'empleado']);

function volver(string $mensaje, string $tipo = 'exito'): void {
    header('Location: ../panel/administrador/inventario.php?' . $tipo . '=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    volver('Solicitud inválida o sesión expirada.', 'error');
}

$idProducto = (int)($_POST['id_producto'] ?? 0);
$tipo = $_POST['tipo'] ?? '';
$cantidad = (int)($_POST['cantidad'] ?? 0);
$motivo = trim($_POST['motivo'] ?? '');

if (!in_array($tipo, ['entrada', 'salida', 'ajuste'], true) || $cantidad <= 0 || $motivo === '') {
    volver('Todos los campos son obligatorios y la cantidad debe ser mayor a 0.', 'error');
}

$stmt = $pdo->prepare('SELECT * FROM productos WHERE id_producto = :id');
$stmt->execute([':id' => $idProducto]);
$producto = $stmt->fetch();
if (!$producto) volver('Producto no encontrado.', 'error');

try {
    $pdo->beginTransaction();

    if ($tipo === 'entrada') {
        $nuevoStock = $producto['stock'] + $cantidad;
    } elseif ($tipo === 'salida') {
        if ($cantidad > $producto['stock']) {
            throw new Exception('No puedes sacar más unidades de las que hay en stock (' . $producto['stock'] . ').');
        }
        $nuevoStock = $producto['stock'] - $cantidad;
    } else { // ajuste: la cantidad ingresada ES el nuevo stock total
        $nuevoStock = $cantidad;
    }

    $pdo->prepare('UPDATE productos SET stock = :s WHERE id_producto = :id')->execute([':s' => $nuevoStock, ':id' => $idProducto]);

    $pdo->prepare('INSERT INTO inventario_movimientos (id_producto, id_usuario, tipo, cantidad, stock_resultante, motivo)
                    VALUES (:p, :u, :t, :c, :sr, :m)')
        ->execute([':p' => $idProducto, ':u' => $u['id_usuario'], ':t' => $tipo, ':c' => $cantidad, ':sr' => $nuevoStock, ':m' => $motivo]);

    $pdo->commit();
    registrarLog($u['id_usuario'], 'movimiento_inventario', "Producto #$idProducto: $tipo de $cantidad");

    if ($nuevoStock <= $producto['stock_minimo']) {
        notificarAdministradores('Stock bajo', $producto['nombre'] . ' quedó con ' . $nuevoStock . ' unidades.', 'panel/administrador/inventario.php');
    }

    volver('Movimiento registrado. Nuevo stock: ' . $nuevoStock);
} catch (Exception $e) {
    $pdo->rollBack();
    volver($e->getMessage(), 'error');
}
