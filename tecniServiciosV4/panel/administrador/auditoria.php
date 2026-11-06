<?php
/**
 * panel/administrador/auditoria.php
 * -----------------------------------------------------------------
 * Visor paginado del registro de auditoría (tabla logs_actividad): quién hizo qué acción y cuándo, en todo el sistema.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Auditoría';

$pagina = max(1, (int)($_GET['pagina'] ?? 1));
$porPagina = 40;
$offset = ($pagina - 1) * $porPagina;

$total = $pdo->query('SELECT COUNT(*) FROM logs_actividad')->fetchColumn();
$stmt = $pdo->prepare("SELECT l.*, u.nombre AS usuario FROM logs_actividad l
                        LEFT JOIN usuarios u ON u.id_usuario = l.id_usuario
                        ORDER BY l.creado_en DESC LIMIT :lim OFFSET :off");
$stmt->bindValue(':lim', $porPagina, PDO::PARAM_INT);
$stmt->bindValue(':off', $offset, PDO::PARAM_INT);
$stmt->execute();
$logs = $stmt->fetchAll();

$totalPaginas = (int)ceil($total / $porPagina);

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-clipboard-list"></i> Registro de auditoría (<?= $total; ?> eventos)</h2>
  <p style="color:var(--texto-tenue); margin-bottom:16px;">Aquí queda registrada cada acción importante del sistema: inicios de sesión, creación de usuarios/productos, cambios de estado, exportaciones, etc.</p>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Detalle</th><th>IP</th></tr></thead>
      <tbody>
        <?php foreach ($logs as $l): ?>
          <tr>
            <td><?= date('d/m/Y H:i:s', strtotime($l['creado_en'])); ?></td>
            <td><?= htmlspecialchars($l['usuario'] ?? 'Anónimo/Sistema'); ?></td>
            <td><span class="badge-rol"><?= htmlspecialchars(str_replace('_', ' ', $l['accion'])); ?></span></td>
            <td style="font-size:0.85rem; color:var(--texto-tenue);"><?= htmlspecialchars($l['detalle']); ?></td>
            <td style="font-size:0.8rem; color:var(--texto-tenue);"><?= htmlspecialchars($l['ip']); ?></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>

  <div style="display:flex; gap:8px; margin-top:18px; justify-content:center;">
    <?php for ($p = 1; $p <= $totalPaginas; $p++): ?>
      <a href="?pagina=<?= $p; ?>" class="btn btn--secundario" style="padding:6px 12px; font-size:0.8rem; <?= $p === $pagina ? 'background:rgba(0,229,255,0.15);' : ''; ?>"><?= $p; ?></a>
    <?php endfor; ?>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
