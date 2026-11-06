<?php
/**
 * panel/administrador/inventario.php
 * -----------------------------------------------------------------
 * Panel de inventario: existencias actuales, alertas de stock bajo/agotado, y formulario para registrar entradas/salidas/ajustes. Accesible también por empleados.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador', 'empleado']);
$tituloPagina = 'Inventario';

$productos = $pdo->query('SELECT * FROM productos WHERE activo = 1 ORDER BY nombre')->fetchAll();
$agotados = $pdo->query('SELECT COUNT(*) FROM productos WHERE stock = 0 AND activo = 1')->fetchColumn();
$stockBajo = $pdo->query('SELECT COUNT(*) FROM productos WHERE stock > 0 AND stock <= stock_minimo AND activo = 1')->fetchColumn();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>
<?php if (!empty($_GET['error'])): ?><div class="auth-alerta auth-alerta--error" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['error']); ?></div><?php endif; ?>

<div class="panel-stats" style="grid-template-columns: repeat(2,1fr);">
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-triangle-exclamation"></i></div>
    <div><div class="panel-stat-card__valor"><?= $stockBajo; ?></div><div class="panel-stat-card__etiqueta">Productos con stock bajo</div></div></div>
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-ban"></i></div>
    <div><div class="panel-stat-card__valor"><?= $agotados; ?></div><div class="panel-stat-card__etiqueta">Productos agotados</div></div></div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-right-left"></i> Registrar movimiento de inventario</h2>
  <form action="../../php/admin_inventario.php" method="POST" class="panel-form">
    <?= csrfCampoOculto(); ?>
    <div class="campo">
      <label>Producto</label>
      <select name="id_producto" required>
        <?php foreach ($productos as $p): ?>
          <option value="<?= $p['id_producto']; ?>">(<?= $p['stock']; ?> en stock) <?= htmlspecialchars($p['nombre']); ?></option>
        <?php endforeach; ?>
      </select>
    </div>
    <div class="campo">
      <label>Tipo de movimiento</label>
      <select name="tipo" required>
        <option value="entrada">Entrada (compra / reposición)</option>
        <option value="salida">Salida (venta manual / daño / pérdida)</option>
        <option value="ajuste">Ajuste (corrección de conteo físico)</option>
      </select>
    </div>
    <div class="campo">
      <label>Cantidad</label>
      <input type="number" name="cantidad" min="1" required>
      <small style="color:var(--texto-tenue);">En "ajuste", esta cantidad reemplaza el stock actual (no se suma).</small>
    </div>
    <div class="campo campo--full"><label>Motivo</label><input type="text" name="motivo" required placeholder="Ej: Compra a proveedor Hikvision, factura #4521"></div>
    <div class="campo campo--full">
      <button type="submit" class="btn btn--primario"><i class="fa-solid fa-check"></i> Registrar movimiento</button>
    </div>
  </form>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-warehouse"></i> Existencias actuales</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Producto</th><th>Stock actual</th><th>Mínimo</th><th>Máximo</th><th>Estado</th><th>Kardex</th></tr></thead>
      <tbody>
        <?php foreach ($productos as $p): ?>
          <tr>
            <td><?= htmlspecialchars($p['nombre']); ?></td>
            <td><?= $p['stock']; ?></td>
            <td><?= $p['stock_minimo']; ?></td>
            <td><?= $p['stock_maximo']; ?></td>
            <td>
              <?php if ($p['stock'] == 0): ?><span class="badge-estado badge-estado--bloqueado">Agotado</span>
              <?php elseif ($p['stock'] <= $p['stock_minimo']): ?><span class="badge-estado badge-estado--bloqueado">Stock bajo</span>
              <?php else: ?><span class="badge-estado badge-estado--activo">Normal</span><?php endif; ?>
            </td>
            <td><a href="kardex.php?id=<?= $p['id_producto']; ?>" style="color:var(--cian);">Ver historial <i class="fa-solid fa-arrow-right"></i></a></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
