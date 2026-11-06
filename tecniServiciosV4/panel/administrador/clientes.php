<?php
/**
 * panel/administrador/clientes.php
 * -----------------------------------------------------------------
 * Listado de clientes registrados, con su historial de compras y estado de cuenta. Solo lectura (la edición de datos del cliente la hace el propio cliente en su perfil).
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Clientes';

$busqueda = trim($_GET['buscar'] ?? '');
$sql = "SELECT c.*, u.activo, u.bloqueado, u.creado_en,
        (SELECT COUNT(*) FROM pedidos WHERE id_cliente = c.id_cliente) AS total_pedidos,
        (SELECT COALESCE(SUM(total),0) FROM pedidos WHERE id_cliente = c.id_cliente AND estado != 'cancelado') AS total_comprado
        FROM clientes c JOIN usuarios u ON u.id_usuario = c.id_usuario";
if ($busqueda !== '') $sql .= " WHERE c.nombre LIKE :b1 OR c.correo LIKE :b2";
$sql .= " ORDER BY u.creado_en DESC";
$stmt = $pdo->prepare($sql);
$stmt->execute($busqueda !== '' ? [':b1' => "%$busqueda%", ':b2' => "%$busqueda%"] : []);
$clientes = $stmt->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-user-tie"></i> Clientes (<?= count($clientes); ?>)</h2>
  <form method="GET" style="margin-bottom:16px; display:flex; gap:10px; max-width:360px;">
    <input type="text" name="buscar" placeholder="Buscar por nombre o correo..." value="<?= htmlspecialchars($busqueda); ?>"
           style="flex:1; background:var(--azul-oscuro); border:1px solid var(--azul-acero); color:var(--texto); padding:10px 12px; border-radius:8px;">
    <button type="submit" class="btn btn--secundario"><i class="fa-solid fa-magnifying-glass"></i></button>
  </form>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Nombre</th><th>Correo</th><th>Teléfono</th><th>Pedidos</th><th>Total comprado</th><th>Registrado</th><th>Estado</th></tr></thead>
      <tbody>
        <?php if (empty($clientes)): ?><tr><td colspan="7" style="text-align:center; padding:24px; color:var(--texto-tenue);">No hay clientes registrados todavía.</td></tr><?php endif; ?>
        <?php foreach ($clientes as $c): ?>
          <tr>
            <td><?= htmlspecialchars($c['nombre']); ?></td>
            <td><?= htmlspecialchars($c['correo']); ?></td>
            <td><?= htmlspecialchars($c['telefono'] ?? '—'); ?></td>
            <td><?= $c['total_pedidos']; ?></td>
            <td>$<?= number_format($c['total_comprado'], 0, ',', '.'); ?></td>
            <td><?= date('d/m/Y', strtotime($c['creado_en'])); ?></td>
            <td><?= $c['bloqueado'] ? '<span class="badge-estado badge-estado--bloqueado">Bloqueado</span>' : ($c['activo'] ? '<span class="badge-estado badge-estado--activo">Activo</span>' : '<span class="badge-estado badge-estado--inactivo">Inactivo</span>'); ?></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
