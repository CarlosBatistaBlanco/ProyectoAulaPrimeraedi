<?php
/**
 * panel/administrador/index.php
 * -----------------------------------------------------------------
 * Dashboard del administrador: estadísticas generales (usuarios, clientes, productos, pedidos, ventas) y últimos usuarios registrados.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Dashboard';

// Estadísticas reales tomadas de la base de datos
$totalUsuarios  = $pdo->query("SELECT COUNT(*) FROM usuarios")->fetchColumn();
$totalClientes  = $pdo->query("SELECT COUNT(*) FROM usuarios WHERE id_rol = (SELECT id_rol FROM roles WHERE nombre='cliente')")->fetchColumn();
$totalProductos = $pdo->query("SELECT COUNT(*) FROM productos WHERE activo = 1")->fetchColumn();
$totalPedidos   = $pdo->query("SELECT COUNT(*) FROM pedidos")->fetchColumn();
$ventasTotales  = $pdo->query("SELECT COALESCE(SUM(total),0) FROM pedidos WHERE estado != 'cancelado'")->fetchColumn();
$stockBajo      = $pdo->query("SELECT COUNT(*) FROM productos WHERE stock <= 5 AND activo = 1")->fetchColumn();
$ultimosUsuarios = $pdo->query("SELECT u.nombre, u.correo, r.nombre AS rol, u.creado_en, u.activo, u.bloqueado
                                 FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol
                                 ORDER BY u.creado_en DESC LIMIT 5")->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-stats">
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-users"></i></div>
    <div><div class="panel-stat-card__valor"><?= $totalUsuarios; ?></div><div class="panel-stat-card__etiqueta">Usuarios totales</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-user-tie"></i></div>
    <div><div class="panel-stat-card__valor"><?= $totalClientes; ?></div><div class="panel-stat-card__etiqueta">Clientes registrados</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-box"></i></div>
    <div><div class="panel-stat-card__valor"><?= $totalProductos; ?></div><div class="panel-stat-card__etiqueta">Productos activos</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-cart-shopping"></i></div>
    <div><div class="panel-stat-card__valor"><?= $totalPedidos; ?></div><div class="panel-stat-card__etiqueta">Pedidos totales</div></div>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-sack-dollar"></i> Ventas acumuladas</h2>
  <p style="font-family:var(--fuente-titulo); font-size:1.8rem; color:var(--cian);">
    $<?= number_format($ventasTotales, 0, ',', '.'); ?>
  </p>
  <?php if ($stockBajo > 0): ?>
    <p style="color:#ffb199; margin-top:10px;"><i class="fa-solid fa-triangle-exclamation"></i>
      <?= $stockBajo; ?> producto(s) con stock bajo (5 unidades o menos).</p>
  <?php endif; ?>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-user-clock"></i> Últimos usuarios registrados</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Registrado</th></tr></thead>
      <tbody>
        <?php foreach ($ultimosUsuarios as $us): ?>
          <tr>
            <td><?= htmlspecialchars($us['nombre']); ?></td>
            <td><?= htmlspecialchars($us['correo']); ?></td>
            <td><span class="badge-rol"><?= htmlspecialchars($us['rol']); ?></span></td>
            <td>
              <?php if ($us['bloqueado']): ?><span class="badge-estado badge-estado--bloqueado">Bloqueado</span>
              <?php elseif ($us['activo']): ?><span class="badge-estado badge-estado--activo">Activo</span>
              <?php else: ?><span class="badge-estado badge-estado--inactivo">Inactivo</span><?php endif; ?>
            </td>
            <td><?= date('d/m/Y', strtotime($us['creado_en'])); ?></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
