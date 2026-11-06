<?php
/**
 * panel/administrador/empleados.php
 * -----------------------------------------------------------------
 * Listado de empleados con su cargo (editable en línea) y cuántas ventas manuales ha registrado cada uno.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Empleados';

$empleados = $pdo->query("SELECT u.*, e.cargo, e.fecha_ingreso,
                           (SELECT COUNT(*) FROM pedidos WHERE id_empleado_registro = u.id_usuario) AS ventas_registradas
                           FROM usuarios u
                           JOIN empleados e ON e.id_usuario = u.id_usuario
                           ORDER BY u.creado_en DESC")->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-id-badge"></i> Empleados (<?= count($empleados); ?>)</h2>
  <p style="color:var(--texto-tenue); margin-bottom:16px;">Para crear un nuevo empleado, ve a <a href="usuarios.php" style="color:var(--cian);">Usuarios</a> y elige el rol "Empleado" al crearlo.</p>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Nombre</th><th>Correo</th><th>Cargo</th><th>Ingreso</th><th>Ventas registradas</th><th>Estado</th><th>Acción</th></tr></thead>
      <tbody>
        <?php if (empty($empleados)): ?><tr><td colspan="7" style="text-align:center; padding:24px; color:var(--texto-tenue);">No hay empleados registrados.</td></tr><?php endif; ?>
        <?php foreach ($empleados as $e): ?>
          <tr>
            <td><?= htmlspecialchars($e['nombre']); ?></td>
            <td><?= htmlspecialchars($e['correo']); ?></td>
            <td>
              <form action="../../php/admin_empleados.php" method="POST" style="display:flex; gap:6px;">
                <?= csrfCampoOculto(); ?>
                <input type="hidden" name="id_usuario" value="<?= $e['id_usuario']; ?>">
                <input type="text" name="cargo" value="<?= htmlspecialchars($e['cargo']); ?>"
                       style="background:var(--azul-oscuro); border:1px solid var(--azul-acero); color:var(--texto); border-radius:6px; padding:4px 8px; width:120px; font-size:0.82rem;">
                <button type="submit" class="panel-accion-btn" title="Guardar cargo"><i class="fa-solid fa-check"></i></button>
              </form>
            </td>
            <td><?= date('d/m/Y', strtotime($e['fecha_ingreso'])); ?></td>
            <td><?= $e['ventas_registradas']; ?></td>
            <td><?= $e['activo'] ? '<span class="badge-estado badge-estado--activo">Activo</span>' : '<span class="badge-estado badge-estado--inactivo">Inactivo</span>'; ?></td>
            <td><a href="usuarios.php?buscar=<?= urlencode($e['correo']); ?>" style="color:var(--cian);">Gestionar <i class="fa-solid fa-arrow-right"></i></a></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
