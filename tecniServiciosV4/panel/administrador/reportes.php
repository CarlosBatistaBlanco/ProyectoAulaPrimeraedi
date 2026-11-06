<?php
/**
 * panel/administrador/reportes.php
 * -----------------------------------------------------------------
 * Reportes de ventas por día/mes, productos más y menos vendidos, clientes frecuentes y empleados con más ventas. Exportable a PDF/Excel desde php/exportar_reporte.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Reportes';

// Ventas por día (últimos 14 días con actividad)
$ventasPorDia = $pdo->query("SELECT DATE(creado_en) AS fecha, COUNT(*) AS pedidos, SUM(total) AS total
    FROM pedidos WHERE estado != 'cancelado' GROUP BY DATE(creado_en) ORDER BY fecha DESC LIMIT 14")->fetchAll();

// Ventas por mes
$ventasPorMes = $pdo->query("SELECT DATE_FORMAT(creado_en, '%Y-%m') AS mes, COUNT(*) AS pedidos, SUM(total) AS total
    FROM pedidos WHERE estado != 'cancelado' GROUP BY mes ORDER BY mes DESC LIMIT 12")->fetchAll();

// Productos más vendidos
$masVendidos = $pdo->query("SELECT p.nombre, SUM(pd.cantidad) AS unidades, SUM(pd.cantidad*pd.precio_unitario) AS total
    FROM pedido_detalle pd JOIN productos p ON p.id_producto = pd.id_producto
    GROUP BY pd.id_producto ORDER BY unidades DESC LIMIT 10")->fetchAll();

// Productos menos vendidos (incluye productos con 0 ventas)
$menosVendidos = $pdo->query("SELECT p.nombre, COALESCE(SUM(pd.cantidad),0) AS unidades
    FROM productos p LEFT JOIN pedido_detalle pd ON pd.id_producto = p.id_producto
    WHERE p.activo = 1 GROUP BY p.id_producto ORDER BY unidades ASC LIMIT 10")->fetchAll();

// Clientes frecuentes
$clientesFrecuentes = $pdo->query("SELECT c.nombre, COUNT(*) AS pedidos, SUM(p.total) AS total
    FROM pedidos p JOIN clientes c ON c.id_cliente = p.id_cliente
    WHERE p.estado != 'cancelado' GROUP BY p.id_cliente ORDER BY pedidos DESC LIMIT 10")->fetchAll();

// Clientes nuevos (últimos 30 días)
$clientesNuevos = $pdo->query("SELECT COUNT(*) FROM clientes c JOIN usuarios u ON u.id_usuario = c.id_usuario
    WHERE u.creado_en >= DATE_SUB(NOW(), INTERVAL 30 DAY)")->fetchColumn();

// Empleados con más ventas registradas manualmente
$empleadosTop = $pdo->query("SELECT u.nombre, COUNT(*) AS ventas, SUM(p.total) AS total
    FROM pedidos p JOIN usuarios u ON u.id_usuario = p.id_empleado_registro
    GROUP BY p.id_empleado_registro ORDER BY ventas DESC LIMIT 10")->fetchAll();

// Ganancias/costos aproximados (usa el precio como venta; no hay costo de compra registrado, se documenta la limitación)
$gananciasBrutas = $pdo->query("SELECT COALESCE(SUM(total),0) FROM pedidos WHERE estado != 'cancelado'")->fetchColumn();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-file-export"></i> Exportar reportes</h2>
  <div style="display:flex; gap:10px; flex-wrap:wrap;">
    <a href="../../php/exportar_reporte.php?formato=pdf" class="btn btn--secundario"><i class="fa-solid fa-file-pdf"></i> Exportar todo a PDF</a>
    <a href="../../php/exportar_reporte.php?formato=excel" class="btn btn--secundario"><i class="fa-solid fa-file-excel"></i> Exportar todo a Excel</a>
  </div>
</div>

<div class="panel-stats">
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-sack-dollar"></i></div>
    <div><div class="panel-stat-card__valor">$<?= number_format($gananciasBrutas, 0, ',', '.'); ?></div><div class="panel-stat-card__etiqueta">Ventas totales</div></div></div>
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-user-plus"></i></div>
    <div><div class="panel-stat-card__valor"><?= $clientesNuevos; ?></div><div class="panel-stat-card__etiqueta">Clientes nuevos (30 días)</div></div></div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-calendar-day"></i> Ventas por día (últimos registros)</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Fecha</th><th>Pedidos</th><th>Total</th></tr></thead>
      <tbody>
        <?php foreach ($ventasPorDia as $v): ?>
          <tr><td><?= date('d/m/Y', strtotime($v['fecha'])); ?></td><td><?= $v['pedidos']; ?></td><td>$<?= number_format($v['total'], 0, ',', '.'); ?></td></tr>
        <?php endforeach; ?>
        <?php if (empty($ventasPorDia)): ?><tr><td colspan="3" style="text-align:center; color:var(--texto-tenue); padding:16px;">Sin ventas registradas todavía.</td></tr><?php endif; ?>
      </tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-calendar"></i> Ventas por mes</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Mes</th><th>Pedidos</th><th>Total</th></tr></thead>
      <tbody>
        <?php foreach ($ventasPorMes as $v): ?>
          <tr><td><?= $v['mes']; ?></td><td><?= $v['pedidos']; ?></td><td>$<?= number_format($v['total'], 0, ',', '.'); ?></td></tr>
        <?php endforeach; ?>
        <?php if (empty($ventasPorMes)): ?><tr><td colspan="3" style="text-align:center; color:var(--texto-tenue); padding:16px;">Sin ventas registradas todavía.</td></tr><?php endif; ?>
      </tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-arrow-trend-up"></i> Productos más vendidos</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Producto</th><th>Unidades vendidas</th><th>Total generado</th></tr></thead>
      <tbody>
        <?php foreach ($masVendidos as $m): ?>
          <tr><td><?= htmlspecialchars($m['nombre']); ?></td><td><?= $m['unidades']; ?></td><td>$<?= number_format($m['total'], 0, ',', '.'); ?></td></tr>
        <?php endforeach; ?>
        <?php if (empty($masVendidos)): ?><tr><td colspan="3" style="text-align:center; color:var(--texto-tenue); padding:16px;">Sin ventas registradas todavía.</td></tr><?php endif; ?>
      </tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-arrow-trend-down"></i> Productos menos vendidos</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Producto</th><th>Unidades vendidas</th></tr></thead>
      <tbody>
        <?php foreach ($menosVendidos as $m): ?>
          <tr><td><?= htmlspecialchars($m['nombre']); ?></td><td><?= $m['unidades']; ?></td></tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-user-group"></i> Clientes frecuentes</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Cliente</th><th>Pedidos</th><th>Total comprado</th></tr></thead>
      <tbody>
        <?php foreach ($clientesFrecuentes as $c): ?>
          <tr><td><?= htmlspecialchars($c['nombre']); ?></td><td><?= $c['pedidos']; ?></td><td>$<?= number_format($c['total'], 0, ',', '.'); ?></td></tr>
        <?php endforeach; ?>
        <?php if (empty($clientesFrecuentes)): ?><tr><td colspan="3" style="text-align:center; color:var(--texto-tenue); padding:16px;">Sin pedidos registrados todavía.</td></tr><?php endif; ?>
      </tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-ranking-star"></i> Empleados con más ventas registradas</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Empleado</th><th>Ventas registradas</th><th>Total generado</th></tr></thead>
      <tbody>
        <?php foreach ($empleadosTop as $e): ?>
          <tr><td><?= htmlspecialchars($e['nombre']); ?></td><td><?= $e['ventas']; ?></td><td>$<?= number_format($e['total'], 0, ',', '.'); ?></td></tr>
        <?php endforeach; ?>
        <?php if (empty($empleadosTop)): ?><tr><td colspan="3" style="text-align:center; color:var(--texto-tenue); padding:16px;">Ningún empleado ha registrado ventas manuales todavía (ver módulo "Registrar venta").</td></tr><?php endif; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
