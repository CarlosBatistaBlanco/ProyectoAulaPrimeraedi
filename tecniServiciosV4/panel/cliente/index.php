<?php
/**
 * panel/cliente/index.php
 * -----------------------------------------------------------------
 * Dashboard del cliente: resumen de sus pedidos recientes y total comprado.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['cliente']);
$tituloPagina = 'Mi Dashboard';

// Busca el id_cliente ligado a este usuario
$stmt = $pdo->prepare('SELECT * FROM clientes WHERE id_usuario = :id LIMIT 1');
$stmt->execute([':id' => $u['id_usuario']]);
$cliente = $stmt->fetch();

$pedidos = [];
$totalGastado = 0;
if ($cliente) {
    $stmt = $pdo->prepare('SELECT * FROM pedidos WHERE id_cliente = :id ORDER BY creado_en DESC LIMIT 5');
    $stmt->execute([':id' => $cliente['id_cliente']]);
    $pedidos = $stmt->fetchAll();

    $totalGastado = $pdo->prepare('SELECT COALESCE(SUM(total),0) FROM pedidos WHERE id_cliente = :id AND estado != "cancelado"');
    $totalGastado->execute([':id' => $cliente['id_cliente']]);
    $totalGastado = $totalGastado->fetchColumn();
}

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-hand-wave"></i> Hola, <?= htmlspecialchars($u['nombre']); ?></h2>
  <p style="color:var(--texto-tenue);">Bienvenido a tu panel. Aquí puedes ver tus pedidos, tu perfil y (próximamente) tus productos favoritos.</p>
</div>

<div class="panel-stats" style="grid-template-columns: repeat(2, 1fr);">
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-box-open"></i></div>
    <div><div class="panel-stat-card__valor"><?= count($pedidos); ?></div><div class="panel-stat-card__etiqueta">Pedidos recientes</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-sack-dollar"></i></div>
    <div><div class="panel-stat-card__valor">$<?= number_format($totalGastado, 0, ',', '.'); ?></div><div class="panel-stat-card__etiqueta">Total comprado</div></div>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-box-open"></i> Mis últimos pedidos</h2>
  <?php if (empty($pedidos)): ?>
    <p style="color:var(--texto-tenue);">Aún no tienes pedidos. <a href="../../pages/productos.php" style="color:var(--cian);">Ir a la tienda</a></p>
  <?php else: ?>
    <div class="panel-tabla-wrap">
      <table class="panel-tabla">
        <thead><tr><th>Pedido</th><th>Fecha</th><th>Total</th><th>Estado</th></tr></thead>
        <tbody>
          <?php foreach ($pedidos as $p): ?>
            <tr>
              <td>#<?= $p['id_pedido']; ?></td>
              <td><?= date('d/m/Y', strtotime($p['creado_en'])); ?></td>
              <td>$<?= number_format($p['total'], 0, ',', '.'); ?></td>
              <td><span class="badge-rol"><?= ucfirst($p['estado']); ?></span></td>
            </tr>
          <?php endforeach; ?>
        </tbody>
      </table>
    </div>
    <p style="margin-top:14px;"><a href="pedidos.php" style="color:var(--cian);">Ver todos mis pedidos →</a></p>
  <?php endif; ?>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
