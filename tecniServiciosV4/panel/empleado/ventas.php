<?php
/**
 * panel/empleado/ventas.php
 * -----------------------------------------------------------------
 * Formulario tipo 'punto de venta' simplificado: el empleado elige un cliente (o crea uno nuevo) y agrega productos para registrar una venta presencial/manual. El envío lo procesa php/empleado_ventas.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['empleado', 'administrador']);
$tituloPagina = 'Registrar Venta';

$productos = $pdo->query('SELECT * FROM productos WHERE activo = 1 AND stock > 0 ORDER BY nombre')->fetchAll();
$clientesExistentes = $pdo->query('SELECT id_cliente, nombre, correo FROM clientes ORDER BY nombre')->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>
<?php if (!empty($_GET['error'])): ?><div class="auth-alerta auth-alerta--error" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['error']); ?></div><?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-cash-register"></i> Registrar venta presencial</h2>
  <p style="color:var(--texto-tenue); margin-bottom:16px;">Úsalo para ventas hechas en el local o por teléfono, que no pasaron por el carrito de la página web.</p>

  <form action="../../php/empleado_ventas.php" method="POST" id="formVenta">
    <?= csrfCampoOculto(); ?>

    <div class="panel-form" style="margin-bottom:20px;">
      <div class="campo">
        <label>Cliente existente (opcional)</label>
        <select id="clienteExistente" name="id_cliente_existente" onchange="document.getElementById('bloqueNuevoCliente').style.display = this.value ? 'none' : 'grid';">
          <option value="">— Cliente nuevo —</option>
          <?php foreach ($clientesExistentes as $c): ?>
            <option value="<?= $c['id_cliente']; ?>"><?= htmlspecialchars($c['nombre']); ?> (<?= htmlspecialchars($c['correo']); ?>)</option>
          <?php endforeach; ?>
        </select>
      </div>
    </div>

    <div class="panel-form" id="bloqueNuevoCliente">
      <div class="campo"><label>Nombre del cliente</label><input type="text" name="nombre_cliente"></div>
      <div class="campo"><label>Correo</label><input type="email" name="correo_cliente"></div>
      <div class="campo"><label>Teléfono</label><input type="text" name="telefono_cliente"></div>
    </div>

    <h3 style="margin:22px 0 12px; font-size:0.95rem; color:var(--texto-tenue);">Productos</h3>
    <div id="lineasVenta"></div>
    <button type="button" id="btnAgregarLinea" class="btn btn--secundario" style="margin-top:10px;"><i class="fa-solid fa-plus"></i> Agregar producto</button>

    <p style="margin-top:18px;">
      <button type="submit" class="btn btn--primario"><i class="fa-solid fa-check"></i> Registrar venta</button>
    </p>
  </form>
</div>

<template id="plantillaLinea">
  <div class="linea-venta" style="display:flex; gap:10px; margin-bottom:10px; align-items:center;">
    <select name="productos[]" required style="flex:2; background:var(--azul-oscuro); border:1px solid var(--azul-acero); color:var(--texto); padding:9px; border-radius:8px;">
      <?php foreach ($productos as $p): ?>
        <option value="<?= $p['id_producto']; ?>" data-precio="<?= $p['precio']; ?>" data-stock="<?= $p['stock']; ?>">
          <?= htmlspecialchars($p['nombre']); ?> ($<?= number_format($p['precio'],0,',','.'); ?>) — <?= $p['stock']; ?> disp.
        </option>
      <?php endforeach; ?>
    </select>
    <input type="number" name="cantidades[]" value="1" min="1" required style="width:80px; background:var(--azul-oscuro); border:1px solid var(--azul-acero); color:var(--texto); padding:9px; border-radius:8px;">
    <button type="button" class="panel-accion-btn panel-accion-btn--peligro quitar-linea" title="Quitar"><i class="fa-solid fa-trash"></i></button>
  </div>
</template>

<script>
  const contenedor = document.getElementById('lineasVenta');
  const plantilla = document.getElementById('plantillaLinea');

  function agregarLinea() {
    const clon = plantilla.content.cloneNode(true);
    clon.querySelector('.quitar-linea').addEventListener('click', (e) => e.target.closest('.linea-venta').remove());
    contenedor.appendChild(clon);
  }
  document.getElementById('btnAgregarLinea').addEventListener('click', agregarLinea);
  agregarLinea(); // arranca con una línea visible
</script>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
