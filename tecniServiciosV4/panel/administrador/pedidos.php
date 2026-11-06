<?php
/**
 * panel/administrador/pedidos.php
 * -----------------------------------------------------------------
 * Listado de todos los pedidos, filtrable por estado, con enlace al detalle de cada uno.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador', 'empleado']);
$tituloPagina = 'Pedidos';

$filtro = $_GET['estado'] ?? '';
$sql = "SELECT p.*, c.nombre AS cliente, c.correo AS correo_cliente FROM pedidos p
        LEFT JOIN clientes c ON c.id_cliente = p.id_cliente";
if ($filtro) $sql .= " WHERE p.estado = :estado";
$sql .= " ORDER BY p.creado_en DESC";
$stmt = $pdo->prepare($sql);
$stmt->execute($filtro ? [':estado' => $filtro] : []);
$pedidos = $stmt->fetchAll();

$estados = ['pendiente','confirmado','en_preparacion','empacado','enviado','entregado','cancelado'];

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-cart-shopping"></i> Pedidos (<?= count($pedidos); ?>)</h2>

  <div style="display:flex; gap:8px; margin-bottom:18px; flex-wrap:wrap;">
    <a href="?estado=" class="btn btn--secundario" style="padding:7px 14px; font-size:0.8rem; <?= $filtro==='' ? 'background:rgba(0,229,255,0.15);' : ''; ?>">Todos</a>
    <?php foreach ($estados as $e): ?>
      <a href="?estado=<?= $e; ?>" class="btn btn--secundario" style="padding:7px 14px; font-size:0.8rem; <?= $filtro===$e ? 'background:rgba(0,229,255,0.15);' : ''; ?>"><?= ucfirst(str_replace('_',' ',$e)); ?></a>
    <?php endforeach; ?>
  </div>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Estado</th><th>Fecha</th><th>Acciones</th></tr></thead>
      <tbody>
        <?php if (empty($pedidos)): ?><tr><td colspan="6" style="text-align:center; padding:24px; color:var(--texto-tenue);">No hay pedidos en esta categoría.</td></tr><?php endif; ?>
        <?php foreach ($pedidos as $p): ?>
          <tr>
            <td>#<?= $p['id_pedido']; ?></td>
            <td><?= htmlspecialchars($p['cliente'] ?? '—'); ?><br><small style="color:var(--texto-tenue);"><?= htmlspecialchars($p['correo_cliente'] ?? ''); ?></small></td>
            <td>$<?= number_format($p['total'], 0, ',', '.'); ?></td>
            <td><span class="badge-rol"><?= ucfirst(str_replace('_',' ',$p['estado'])); ?></span></td>
            <td><?= date('d/m/Y', strtotime($p['creado_en'])); ?></td>
            <td><a href="pedido_detalle.php?id=<?= $p['id_pedido']; ?>" style="color:var(--cian);">Ver detalle <i class="fa-solid fa-arrow-right"></i></a></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
