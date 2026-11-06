<?php
/**
 * panel/administrador/productos.php
 * -----------------------------------------------------------------
 * CRUD completo de productos: crear/editar con SKU único, categoría, marca, descuento, impuesto, garantía e imágenes. El envío lo procesa php/admin_productos.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Gestión de Productos';

$categorias = $pdo->query('SELECT * FROM categorias ORDER BY nombre')->fetchAll();
$marcas = $pdo->query('SELECT * FROM marcas ORDER BY nombre')->fetchAll();

$editando = null;
if (!empty($_GET['editar'])) {
    $stmt = $pdo->prepare('SELECT * FROM productos WHERE id_producto = :id');
    $stmt->execute([':id' => (int)$_GET['editar']]);
    $editando = $stmt->fetch();
}

$busqueda = trim($_GET['buscar'] ?? '');
if ($busqueda !== '') {
    $stmt = $pdo->prepare("SELECT p.*, c.nombre AS categoria, m.nombre AS marca FROM productos p
                            LEFT JOIN categorias c ON c.id_categoria = p.id_categoria
                            LEFT JOIN marcas m ON m.id_marca = p.id_marca
                            WHERE p.nombre LIKE :b1 OR p.sku LIKE :b2 ORDER BY p.nombre");
    $stmt->execute([':b1' => "%$busqueda%", ':b2' => "%$busqueda%"]);
} else {
    $stmt = $pdo->query("SELECT p.*, c.nombre AS categoria, m.nombre AS marca FROM productos p
                          LEFT JOIN categorias c ON c.id_categoria = p.id_categoria
                          LEFT JOIN marcas m ON m.id_marca = p.id_marca
                          ORDER BY p.creado_en DESC");
}
$productos = $stmt->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>
<?php if (!empty($_GET['error'])): ?><div class="auth-alerta auth-alerta--error" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['error']); ?></div><?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid <?= $editando ? 'fa-pen' : 'fa-plus'; ?>"></i> <?= $editando ? 'Editar producto' : 'Crear nuevo producto'; ?></h2>
  <form action="../../php/admin_productos.php" method="POST" class="panel-form" enctype="multipart/form-data">
    <?= csrfCampoOculto(); ?>
    <input type="hidden" name="accion" value="<?= $editando ? 'editar' : 'crear'; ?>">
    <?php if ($editando): ?><input type="hidden" name="id_producto" value="<?= $editando['id_producto']; ?>"><?php endif; ?>

    <div class="campo campo--full"><label>Nombre del producto</label><input type="text" name="nombre" required value="<?= htmlspecialchars($editando['nombre'] ?? ''); ?>"></div>

    <div class="campo"><label>SKU (código único)</label><input type="text" name="sku" required value="<?= htmlspecialchars($editando['sku'] ?? ''); ?>" placeholder="Ej: CAM-DOM-002"></div>
    <div class="campo"><label>Código de barras (opcional)</label><input type="text" name="codigo_barras" value="<?= htmlspecialchars($editando['codigo_barras'] ?? ''); ?>"></div>

    <div class="campo">
      <label>Categoría</label>
      <select name="id_categoria" required>
        <?php foreach ($categorias as $c): ?>
          <option value="<?= $c['id_categoria']; ?>" <?= ($editando['id_categoria'] ?? 0) == $c['id_categoria'] ? 'selected' : ''; ?>><?= htmlspecialchars($c['nombre']); ?></option>
        <?php endforeach; ?>
      </select>
    </div>
    <div class="campo">
      <label>Marca</label>
      <select name="id_marca">
        <?php foreach ($marcas as $m): ?>
          <option value="<?= $m['id_marca']; ?>" <?= ($editando['id_marca'] ?? 0) == $m['id_marca'] ? 'selected' : ''; ?>><?= htmlspecialchars($m['nombre']); ?></option>
        <?php endforeach; ?>
      </select>
    </div>

    <div class="campo campo--full"><label>Descripción</label><input type="text" name="descripcion" required value="<?= htmlspecialchars($editando['descripcion'] ?? ''); ?>"></div>

    <div class="campo"><label>Precio</label><input type="number" step="0.01" name="precio" required value="<?= $editando['precio'] ?? ''; ?>"></div>
    <div class="campo"><label>Descuento (%)</label><input type="number" step="0.01" name="descuento_pct" value="<?= $editando['descuento_pct'] ?? 0; ?>"></div>
    <div class="campo"><label>Impuesto / IVA (%)</label><input type="number" step="0.01" name="impuesto_pct" value="<?= $editando['impuesto_pct'] ?? 19; ?>"></div>
    <div class="campo"><label>Garantía (meses)</label><input type="number" name="garantia_meses" value="<?= $editando['garantia_meses'] ?? 12; ?>"></div>

    <div class="campo"><label>Stock actual</label><input type="number" name="stock" required value="<?= $editando['stock'] ?? 0; ?>" <?= $editando ? 'readonly title="El stock se cambia desde Inventario"' : ''; ?>></div>
    <div class="campo"><label>Stock mínimo (alerta)</label><input type="number" name="stock_minimo" value="<?= $editando['stock_minimo'] ?? 5; ?>"></div>
    <div class="campo"><label>Stock máximo</label><input type="number" name="stock_maximo" value="<?= $editando['stock_maximo'] ?? 100; ?>"></div>

    <div class="campo">
      <label>Imagen principal (opcional)</label>
      <input type="file" name="imagen" accept="image/*">
      <?php if ($editando): ?><small style="color:var(--texto-tenue);">Actual: <?= htmlspecialchars($editando['imagen']); ?></small><?php endif; ?>
    </div>
    <div class="campo">
      <label>Imágenes adicionales (puedes elegir varias)</label>
      <input type="file" name="imagenes_extra[]" accept="image/*" multiple>
    </div>

    <div class="campo">
      <label><input type="checkbox" name="destacado" value="1" <?= !empty($editando['destacado']) ? 'checked' : ''; ?>> Producto destacado</label>
    </div>
    <div class="campo">
      <label><input type="checkbox" name="activo" value="1" <?= ($editando === null || !empty($editando['activo'])) ? 'checked' : ''; ?>> Producto activo (visible en la tienda)</label>
    </div>

    <div class="campo campo--full">
      <button type="submit" class="btn btn--primario"><i class="fa-solid fa-floppy-disk"></i> <?= $editando ? 'Guardar cambios' : 'Crear producto'; ?></button>
      <?php if ($editando): ?><a href="productos.php" class="btn btn--secundario">Cancelar</a><?php endif; ?>
    </div>
  </form>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-box"></i> Catálogo (<?= count($productos); ?>)</h2>
  <form method="GET" style="margin-bottom:16px; display:flex; gap:10px; max-width:360px;">
    <input type="text" name="buscar" placeholder="Buscar por nombre o SKU..." value="<?= htmlspecialchars($busqueda); ?>"
           style="flex:1; background:var(--azul-oscuro); border:1px solid var(--azul-acero); color:var(--texto); padding:10px 12px; border-radius:8px;">
    <button type="submit" class="btn btn--secundario"><i class="fa-solid fa-magnifying-glass"></i></button>
  </form>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>SKU</th><th>Nombre</th><th>Categoría</th><th>Marca</th><th>Precio</th><th>Stock</th><th>Estado</th><th>Acciones</th></tr></thead>
      <tbody>
        <?php foreach ($productos as $p): ?>
          <tr>
            <td><?= htmlspecialchars($p['sku'] ?? '—'); ?></td>
            <td><?= htmlspecialchars($p['nombre']); ?></td>
            <td><?= htmlspecialchars($p['categoria'] ?? '—'); ?></td>
            <td><?= htmlspecialchars($p['marca'] ?? '—'); ?></td>
            <td>$<?= number_format($p['precio'], 0, ',', '.'); ?></td>
            <td><?= $p['stock']; ?> <?php if ($p['stock'] <= $p['stock_minimo']): ?><i class="fa-solid fa-triangle-exclamation" style="color:#ffb199;" title="Stock bajo"></i><?php endif; ?></td>
            <td><?= $p['activo'] ? '<span class="badge-estado badge-estado--activo">Activo</span>' : '<span class="badge-estado badge-estado--inactivo">Inactivo</span>'; ?></td>
            <td>
              <div class="panel-acciones">
                <a href="productos.php?editar=<?= $p['id_producto']; ?>" class="panel-accion-btn" title="Editar"><i class="fa-solid fa-pen"></i></a>
                <a href="kardex.php?id=<?= $p['id_producto']; ?>" class="panel-accion-btn" title="Ver Kardex"><i class="fa-solid fa-clock-rotate-left"></i></a>
                <form action="../../php/admin_productos.php" method="POST" onsubmit="return confirm('¿Cambiar el estado activo/inactivo de este producto?');">
                  <?= csrfCampoOculto(); ?>
                  <input type="hidden" name="accion" value="toggle_activo">
                  <input type="hidden" name="id_producto" value="<?= $p['id_producto']; ?>">
                  <button type="submit" class="panel-accion-btn panel-accion-btn--peligro" title="Activar/Desactivar"><i class="fa-solid fa-power-off"></i></button>
                </form>
              </div>
            </td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
