<?php
/**
 * panel/administrador/pedido_detalle.php
 * -----------------------------------------------------------------
 * Detalle de un pedido: productos, cambio de estado (con historial), y generación/anulación de su factura en PDF.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador', 'empleado']);

$idPedido = (int)($_GET['id'] ?? 0);
$stmt = $pdo->prepare("SELECT p.*, c.nombre AS cliente, c.correo AS correo_cliente, c.telefono, c.direccion
                        FROM pedidos p LEFT JOIN clientes c ON c.id_cliente = p.id_cliente WHERE p.id_pedido = :id");
$stmt->execute([':id' => $idPedido]);
$pedido = $stmt->fetch();
if (!$pedido) { header('Location: pedidos.php'); exit; }

$tituloPagina = 'Pedido #' . $idPedido;

$detalle = $pdo->prepare('SELECT pd.*, pr.nombre, pr.imagen FROM pedido_detalle pd
                           JOIN productos pr ON pr.id_producto = pd.id_producto WHERE pd.id_pedido = :id');
$detalle->execute([':id' => $idPedido]);
$detalle = $detalle->fetchAll();

// Historial y factura están en try/catch a propósito: si la base de datos no
// tiene todavía estas tablas (por ejemplo, por no haber reimportado el script
// SQL más reciente), la página de todas formas muestra el pedido y sus
// productos, en vez de romperse por completo con un error fatal.
$historial = [];
try {
    $stmtHist = $pdo->prepare("SELECT h.*, u.nombre AS usuario FROM pedidos_historial h
                                 LEFT JOIN usuarios u ON u.id_usuario = h.id_usuario
                                 WHERE h.id_pedido = :id ORDER BY h.creado_en DESC");
    $stmtHist->execute([':id' => $idPedido]);
    $historial = $stmtHist->fetchAll();
} catch (PDOException $e) {
    $historial = [];
}

$factura = null;
try {
    $stmtFact = $pdo->prepare('SELECT * FROM facturas WHERE id_pedido = :id');
    $stmtFact->execute([':id' => $idPedido]);
    $factura = $stmtFact->fetch();
} catch (PDOException $e) {
    $factura = null;
}

$estados = ['pendiente','confirmado','en_preparacion','empacado','enviado','entregado','cancelado'];

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>
<?php if (!empty($_GET['error'])): ?><div class="auth-alerta auth-alerta--error" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['error']); ?></div><?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-receipt"></i> Pedido #<?= $idPedido; ?></h2>
  <p style="color:var(--texto-tenue);">
    Cliente: <strong style="color:var(--texto);"><?= htmlspecialchars($pedido['cliente'] ?? '—'); ?></strong> ·
    <?= htmlspecialchars($pedido['correo_cliente'] ?? ''); ?> · <?= htmlspecialchars($pedido['telefono'] ?? ''); ?><br>
    Dirección: <?= htmlspecialchars($pedido['direccion'] ?? 'No especificada'); ?><br>
    Fecha: <?= date('d/m/Y H:i', strtotime($pedido['creado_en'])); ?>
  </p>

  <div class="panel-tabla-wrap" style="margin-top:18px;">
    <table class="panel-tabla">
      <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unitario</th><th>Subtotal</th></tr></thead>
      <tbody>
        <?php foreach ($detalle as $d): ?>
          <tr>
            <td><?= htmlspecialchars($d['nombre']); ?></td>
            <td><?= $d['cantidad']; ?></td>
            <td>$<?= number_format($d['precio_unitario'], 0, ',', '.'); ?></td>
            <td>$<?= number_format($d['precio_unitario'] * $d['cantidad'], 0, ',', '.'); ?></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
  <p style="text-align:right; margin-top:12px; font-family:var(--fuente-titulo); font-size:1.3rem; color:var(--cian);">
    Total: $<?= number_format($pedido['total'], 0, ',', '.'); ?>
  </p>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-truck"></i> Cambiar estado del pedido</h2>
  <form action="../../php/admin_pedidos.php" method="POST" style="display:flex; gap:12px; flex-wrap:wrap; align-items:flex-end;">
    <?= csrfCampoOculto(); ?>
    <input type="hidden" name="accion" value="cambiar_estado">
    <input type="hidden" name="id_pedido" value="<?= $idPedido; ?>">
    <div class="campo">
      <label>Nuevo estado</label>
      <select name="estado">
        <?php foreach ($estados as $e): ?>
          <option value="<?= $e; ?>" <?= $pedido['estado'] === $e ? 'selected' : ''; ?>><?= ucfirst(str_replace('_',' ',$e)); ?></option>
        <?php endforeach; ?>
      </select>
    </div>
    <button type="submit" class="btn btn--primario"><i class="fa-solid fa-check"></i> Actualizar estado</button>
  </form>

  <h3 style="margin-top:24px; font-size:0.95rem; color:var(--texto-tenue);">Historial de cambios</h3>
  <div class="panel-tabla-wrap" style="margin-top:10px;">
    <table class="panel-tabla">
      <thead><tr><th>Fecha</th><th>De</th><th>A</th><th>Realizado por</th></tr></thead>
      <tbody>
        <?php if (empty($historial)): ?><tr><td colspan="4" style="text-align:center; color:var(--texto-tenue); padding:16px;">Sin cambios de estado todavía.</td></tr><?php endif; ?>
        <?php foreach ($historial as $h): ?>
          <tr>
            <td><?= date('d/m/Y H:i', strtotime($h['creado_en'])); ?></td>
            <td><?= $h['estado_anterior'] ? ucfirst(str_replace('_',' ',$h['estado_anterior'])) : '—'; ?></td>
            <td><?= ucfirst(str_replace('_',' ',$h['estado_nuevo'])); ?></td>
            <td><?= htmlspecialchars($h['usuario'] ?? 'Sistema'); ?></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-file-invoice"></i> Facturación</h2>
  <?php if ($factura): ?>
    <p>Factura <strong><?= htmlspecialchars($factura['numero_factura']); ?></strong> —
      <span class="badge-estado <?= $factura['estado'] === 'emitida' ? 'badge-estado--activo' : 'badge-estado--bloqueado'; ?>"><?= ucfirst($factura['estado']); ?></span>
    </p>
    <div style="display:flex; gap:10px; margin-top:12px;">
      <a href="../../php/generar_factura.php?id=<?= $idPedido; ?>" class="btn btn--secundario" target="_blank"><i class="fa-solid fa-file-pdf"></i> Descargar PDF</a>
      <?php if ($factura['estado'] === 'emitida'): ?>
        <form action="../../php/admin_pedidos.php" method="POST" onsubmit="return confirm('¿Anular esta factura?');">
          <?= csrfCampoOculto(); ?>
          <input type="hidden" name="accion" value="anular_factura">
          <input type="hidden" name="id_pedido" value="<?= $idPedido; ?>">
          <button type="submit" class="btn btn--secundario" style="border-color:var(--naranja); color:var(--naranja);"><i class="fa-solid fa-ban"></i> Anular factura</button>
        </form>
      <?php endif; ?>
    </div>
  <?php else: ?>
    <p style="color:var(--texto-tenue); margin-bottom:14px;">Este pedido aún no tiene factura generada.</p>
    <form action="../../php/admin_pedidos.php" method="POST">
      <?= csrfCampoOculto(); ?>
      <input type="hidden" name="accion" value="generar_factura">
      <input type="hidden" name="id_pedido" value="<?= $idPedido; ?>">
      <button type="submit" class="btn btn--primario"><i class="fa-solid fa-file-invoice-dollar"></i> Generar factura</button>
    </form>
  <?php endif; ?>
</div>

<p><a href="pedidos.php" style="color:var(--texto-tenue);"><i class="fa-solid fa-arrow-left"></i> Volver a pedidos</a></p>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
