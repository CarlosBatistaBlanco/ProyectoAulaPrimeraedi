<?php
/**
 * panel/administrador/facturacion.php
 * -----------------------------------------------------------------
 * Listado de todas las facturas emitidas, con el total facturado y enlace de descarga en PDF de cada una.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Facturación';

$facturas = $pdo->query("SELECT f.*, p.id_cliente, c.nombre AS cliente FROM facturas f
                          JOIN pedidos p ON p.id_pedido = f.id_pedido
                          LEFT JOIN clientes c ON c.id_cliente = p.id_cliente
                          ORDER BY f.creado_en DESC")->fetchAll();

$totalFacturado = $pdo->query("SELECT COALESCE(SUM(total),0) FROM facturas WHERE estado = 'emitida'")->fetchColumn();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-sack-dollar"></i> Total facturado</h2>
  <p style="font-family:var(--fuente-titulo); font-size:1.8rem; color:var(--cian);">$<?= number_format($totalFacturado, 0, ',', '.'); ?></p>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-file-invoice"></i> Facturas emitidas (<?= count($facturas); ?>)</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>N° Factura</th><th>Cliente</th><th>Total</th><th>Estado</th><th>Fecha</th><th>Acción</th></tr></thead>
      <tbody>
        <?php if (empty($facturas)): ?><tr><td colspan="6" style="text-align:center; padding:24px; color:var(--texto-tenue);">Aún no hay facturas generadas.</td></tr><?php endif; ?>
        <?php foreach ($facturas as $f): ?>
          <tr>
            <td><?= htmlspecialchars($f['numero_factura']); ?></td>
            <td><?= htmlspecialchars($f['cliente'] ?? '—'); ?></td>
            <td>$<?= number_format($f['total'], 0, ',', '.'); ?></td>
            <td><span class="badge-estado <?= $f['estado'] === 'emitida' ? 'badge-estado--activo' : 'badge-estado--bloqueado'; ?>"><?= ucfirst($f['estado']); ?></span></td>
            <td><?= date('d/m/Y', strtotime($f['creado_en'])); ?></td>
            <td><a href="../../php/generar_factura.php?id=<?= $f['id_pedido']; ?>" target="_blank" style="color:var(--cian);"><i class="fa-solid fa-file-pdf"></i> Ver PDF</a></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
