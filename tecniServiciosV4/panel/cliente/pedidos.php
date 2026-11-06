<?php
/**
 * panel/cliente/pedidos.php
 * -----------------------------------------------------------------
 * Historial completo de pedidos del cliente, con el detalle de productos de cada uno y enlace para calificarlos.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['cliente']);
$tituloPagina = 'Mis Pedidos';

$stmt = $pdo->prepare('SELECT * FROM clientes WHERE id_usuario = :id LIMIT 1');
$stmt->execute([':id' => $u['id_usuario']]);
$cliente = $stmt->fetch();

$pedidos = [];
if ($cliente) {
    $stmt = $pdo->prepare('SELECT * FROM pedidos WHERE id_cliente = :id ORDER BY creado_en DESC');
    $stmt->execute([':id' => $cliente['id_cliente']]);
    $pedidos = $stmt->fetchAll();
}

function detallePedido(PDO $pdo, int $idPedido): array {
    $stmt = $pdo->prepare('SELECT pd.*, p.nombre, p.imagen FROM pedido_detalle pd
                            JOIN productos p ON p.id_producto = pd.id_producto
                            WHERE pd.id_pedido = :id');
    $stmt->execute([':id' => $idPedido]);
    return $stmt->fetchAll();
}

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-box-open"></i> Historial de pedidos</h2>

  <?php if (empty($pedidos)): ?>
    <p style="color:var(--texto-tenue);">Aún no has realizado ningún pedido. <a href="../../pages/productos.php" style="color:var(--cian);">Ir a la tienda</a></p>
  <?php else: ?>
    <?php foreach ($pedidos as $p): ?>
      <div style="border:1px solid var(--azul-acero); border-radius:10px; padding:16px 18px; margin-bottom:14px;">
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
          <div>
            <strong>Pedido #<?= $p['id_pedido']; ?></strong>
            <span style="color:var(--texto-tenue); font-size:0.85rem; margin-left:10px;"><?= date('d/m/Y H:i', strtotime($p['creado_en'])); ?></span>
          </div>
          <span class="badge-rol"><?= ucfirst($p['estado']); ?></span>
        </div>
        <div class="panel-tabla-wrap" style="margin-top:12px;">
          <table class="panel-tabla">
            <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th><th>Reseña</th></tr></thead>
            <tbody>
              <?php foreach (detallePedido($pdo, $p['id_pedido']) as $linea): ?>
                <tr>
                  <td><?= htmlspecialchars($linea['nombre']); ?></td>
                  <td><?= $linea['cantidad']; ?></td>
                  <td>$<?= number_format($linea['precio_unitario'], 0, ',', '.'); ?></td>
                  <td>$<?= number_format($linea['precio_unitario'] * $linea['cantidad'], 0, ',', '.'); ?></td>
                  <td><a href="resenar.php?producto=<?= $linea['id_producto']; ?>" style="color:var(--cian); font-size:0.82rem;"><i class="fa-solid fa-star"></i> Calificar</a></td>
                </tr>
              <?php endforeach; ?>
            </tbody>
          </table>
        </div>
        <p style="text-align:right; margin-top:10px; font-family:var(--fuente-titulo); color:var(--cian);">
          Total: $<?= number_format($p['total'], 0, ',', '.'); ?>
        </p>
      </div>
    <?php endforeach; ?>
  <?php endif; ?>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
