<?php
/**
 * panel/cliente/favoritos.php
 * -----------------------------------------------------------------
 * Lista de productos que el cliente marcó como favoritos (corazón) en el catálogo.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['cliente']);
$tituloPagina = 'Mis Favoritos';

$stmt = $pdo->prepare("SELECT p.* FROM favoritos f JOIN productos p ON p.id_producto = f.id_producto
                        WHERE f.id_usuario = :u ORDER BY f.creado_en DESC");
$stmt->execute([':u' => $u['id_usuario']]);
$favoritos = $stmt->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-heart"></i> Mis productos favoritos</h2>

  <?php if (empty($favoritos)): ?>
    <p style="color:var(--texto-tenue);">Aún no tienes favoritos. <a href="../../pages/productos.php" style="color:var(--cian);">Explora el catálogo</a> y da clic en el corazón de los productos que te gusten.</p>
  <?php else: ?>
    <div class="panel-tabla-wrap">
      <table class="panel-tabla">
        <thead><tr><th>Producto</th><th>Precio</th><th>Disponibilidad</th><th>Acción</th></tr></thead>
        <tbody>
          <?php foreach ($favoritos as $p): ?>
            <tr>
              <td><?= htmlspecialchars($p['nombre']); ?></td>
              <td>$<?= number_format($p['precio'], 0, ',', '.'); ?></td>
              <td><?= $p['stock'] > 0 ? '<span class="badge-estado badge-estado--activo">Disponible</span>' : '<span class="badge-estado badge-estado--bloqueado">Agotado</span>'; ?></td>
              <td><a href="../../pages/productos.php" style="color:var(--cian);">Ver en tienda <i class="fa-solid fa-arrow-right"></i></a></td>
            </tr>
          <?php endforeach; ?>
        </tbody>
      </table>
    </div>
  <?php endif; ?>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
