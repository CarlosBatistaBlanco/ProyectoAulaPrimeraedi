<?php
/**
 * panel/administrador/kardex.php
 * -----------------------------------------------------------------
 * Historial completo de movimientos de inventario (entradas/salidas/ajustes) de UN producto específico.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador', 'empleado']);

$idProducto = (int)($_GET['id'] ?? 0);
$stmt = $pdo->prepare('SELECT * FROM productos WHERE id_producto = :id');
$stmt->execute([':id' => $idProducto]);
$producto = $stmt->fetch();
if (!$producto) { header('Location: inventario.php'); exit; }

$tituloPagina = 'Kardex: ' . $producto['nombre'];

$stmt = $pdo->prepare("SELECT im.*, u.nombre AS usuario FROM inventario_movimientos im
                        LEFT JOIN usuarios u ON u.id_usuario = im.id_usuario
                        WHERE im.id_producto = :id ORDER BY im.creado_en DESC");
$stmt->execute([':id' => $idProducto]);
$movimientos = $stmt->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-clock-rotate-left"></i> Kardex de <?= htmlspecialchars($producto['nombre']); ?></h2>
  <p style="color:var(--texto-tenue); margin-bottom:16px;">SKU: <?= htmlspecialchars($producto['sku'] ?? '—'); ?> · Stock actual: <strong style="color:var(--cian);"><?= $producto['stock']; ?></strong></p>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Fecha</th><th>Tipo</th><th>Cantidad</th><th>Stock resultante</th><th>Motivo</th><th>Registrado por</th></tr></thead>
      <tbody>
        <?php if (empty($movimientos)): ?>
          <tr><td colspan="6" style="text-align:center; color:var(--texto-tenue); padding:24px;">Sin movimientos registrados.</td></tr>
        <?php endif; ?>
        <?php foreach ($movimientos as $m): ?>
          <tr>
            <td><?= date('d/m/Y H:i', strtotime($m['creado_en'])); ?></td>
            <td>
              <?php $colores = ['entrada' => 'badge-estado--activo', 'salida' => 'badge-estado--bloqueado', 'ajuste' => 'badge-estado--inactivo']; ?>
              <span class="badge-estado <?= $colores[$m['tipo']]; ?>"><?= ucfirst($m['tipo']); ?></span>
            </td>
            <td><?= $m['tipo'] === 'salida' ? '-' : ($m['tipo'] === 'ajuste' ? '=' : '+'); ?><?= $m['cantidad']; ?></td>
            <td><?= $m['stock_resultante']; ?></td>
            <td><?= htmlspecialchars($m['motivo']); ?></td>
            <td><?= htmlspecialchars($m['usuario'] ?? 'Sistema'); ?></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
  <p style="margin-top:16px;"><a href="inventario.php" style="color:var(--cian);"><i class="fa-solid fa-arrow-left"></i> Volver a inventario</a></p>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
