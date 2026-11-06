<?php
/**
 * panel/empleado/index.php
 * -----------------------------------------------------------------
 * Dashboard del rol "empleado". Muestra un resumen de bienvenida y
 * accesos directos a los módulos que SÍ puede usar un empleado:
 * registrar ventas manuales, ver pedidos e inventario (comparte esas
 * páginas con el administrador, ver includes/panel_sidebar.php).
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['empleado']);
$tituloPagina = 'Dashboard';

// Estadísticas rápidas de lo que ESTE empleado ha hecho
$ventasHoy = $pdo->prepare("SELECT COUNT(*) FROM pedidos WHERE id_empleado_registro = :id AND DATE(creado_en) = CURDATE()");
$ventasHoy->execute([':id' => $u['id_usuario']]);
$ventasHoy = (int)$ventasHoy->fetchColumn();

$ventasTotales = $pdo->prepare("SELECT COUNT(*) FROM pedidos WHERE id_empleado_registro = :id");
$ventasTotales->execute([':id' => $u['id_usuario']]);
$ventasTotales = (int)$ventasTotales->fetchColumn();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-hand-wave"></i> Hola, <?= htmlspecialchars($u['nombre']); ?></h2>
  <p style="color:var(--texto-tenue);">Este es tu panel de empleado. Tu acceso está limitado a los módulos operativos (ventas, pedidos, inventario) — no puedes ver la gestión de usuarios ni la configuración general del sistema, esas son exclusivas del administrador.</p>
</div>

<div class="panel-stats" style="grid-template-columns: repeat(2, 1fr);">
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-cash-register"></i></div>
    <div><div class="panel-stat-card__valor"><?= $ventasHoy; ?></div><div class="panel-stat-card__etiqueta">Ventas registradas hoy</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-chart-simple"></i></div>
    <div><div class="panel-stat-card__valor"><?= $ventasTotales; ?></div><div class="panel-stat-card__etiqueta">Ventas totales registradas por ti</div></div>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-list-check"></i> Accesos rápidos</h2>
  <div style="display:flex; gap:12px; flex-wrap:wrap;">
    <a href="ventas.php" class="btn btn--primario"><i class="fa-solid fa-cash-register"></i> Registrar una venta</a>
    <a href="../administrador/pedidos.php" class="btn btn--secundario"><i class="fa-solid fa-cart-shopping"></i> Ver pedidos</a>
    <a href="../administrador/inventario.php" class="btn btn--secundario"><i class="fa-solid fa-warehouse"></i> Ver inventario</a>
  </div>
</div>

<div class="panel-proximamente">
  <i class="fa-solid fa-list-check"></i>
  <h2>Asignación formal de tareas: próximamente</h2>
  <p>Ventas, pedidos e inventario ya están activos y funcionando (los tienes en el menú de la izquierda). Lo único que falta por construir es un módulo para que el administrador te asigne tareas puntuales y puedas marcarlas como completadas.</p>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
