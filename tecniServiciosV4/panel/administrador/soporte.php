<?php
/**
 * panel/administrador/soporte.php
 * -----------------------------------------------------------------
 * Panel de Atención al Cliente: solicitudes de servicio (cotizaciones/citas) y mensajes de contacto (mensaje/reclamo/sugerencia), ambos con cambio de estado.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Atención al Cliente';

$filtro = $_GET['estado'] ?? '';
$sql = "SELECT s.*, sv.titulo AS servicio FROM solicitudes_servicio s
        JOIN servicios sv ON sv.id_servicio = s.id_servicio";
if ($filtro) {
    $sql .= " WHERE s.estado = :estado";
}
$sql .= " ORDER BY s.creado_en DESC";
$stmt = $pdo->prepare($sql);
$stmt->execute($filtro ? [':estado' => $filtro] : []);
$solicitudes = $stmt->fetchAll();

$conteos = $pdo->query("SELECT estado, COUNT(*) c FROM solicitudes_servicio GROUP BY estado")->fetchAll(PDO::FETCH_KEY_PAIR);

$mensajes = $pdo->query("SELECT * FROM contactos ORDER BY creado_en DESC LIMIT 40")->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>

<div class="panel-stats">
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-inbox"></i></div>
    <div><div class="panel-stat-card__valor"><?= $conteos['pendiente'] ?? 0; ?></div><div class="panel-stat-card__etiqueta">Pendientes</div></div></div>
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-phone"></i></div>
    <div><div class="panel-stat-card__valor"><?= $conteos['contactado'] ?? 0; ?></div><div class="panel-stat-card__etiqueta">Contactados</div></div></div>
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-calendar-check"></i></div>
    <div><div class="panel-stat-card__valor"><?= $conteos['agendado'] ?? 0; ?></div><div class="panel-stat-card__etiqueta">Agendados</div></div></div>
  <div class="panel-stat-card"><div class="panel-stat-card__icono"><i class="fa-solid fa-circle-check"></i></div>
    <div><div class="panel-stat-card__valor"><?= $conteos['completado'] ?? 0; ?></div><div class="panel-stat-card__etiqueta">Completados</div></div></div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-headset"></i> Solicitudes de servicio (cotizaciones y citas)</h2>

  <div style="display:flex; gap:8px; margin-bottom:18px; flex-wrap:wrap;">
    <?php foreach (['' => 'Todas', 'pendiente' => 'Pendientes', 'contactado' => 'Contactados', 'agendado' => 'Agendados', 'completado' => 'Completados', 'cancelado' => 'Cancelados'] as $val => $label): ?>
      <a href="?estado=<?= $val; ?>" class="btn btn--secundario" style="padding:7px 14px; font-size:0.82rem; <?= $filtro === $val ? 'background:rgba(0,229,255,0.15);' : ''; ?>"><?= $label; ?></a>
    <?php endforeach; ?>
  </div>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Cliente</th><th>Servicio</th><th>Tipo</th><th>Fecha/Hora</th><th>Descripción</th><th>Estado</th><th>Acción</th></tr></thead>
      <tbody>
        <?php if (empty($solicitudes)): ?>
          <tr><td colspan="7" style="text-align:center; color:var(--texto-tenue); padding:30px;">No hay solicitudes en esta categoría.</td></tr>
        <?php endif; ?>
        <?php foreach ($solicitudes as $s): ?>
          <tr>
            <td>
              <strong><?= htmlspecialchars($s['nombre']); ?></strong><br>
              <small style="color:var(--texto-tenue);"><?= htmlspecialchars($s['correo']); ?> · <?= htmlspecialchars($s['telefono']); ?></small>
            </td>
            <td><?= htmlspecialchars($s['servicio']); ?></td>
            <td><span class="badge-rol"><?= $s['tipo'] === 'cita' ? 'Cita técnica' : 'Cotización'; ?></span></td>
            <td><?= $s['fecha_preferida'] ? date('d/m/Y', strtotime($s['fecha_preferida'])) . ' ' . substr($s['hora_preferida'], 0, 5) : '—'; ?></td>
            <td style="max-width:220px; font-size:0.85rem; color:var(--texto-tenue);"><?= htmlspecialchars(recortar_texto($s['descripcion'], 80)); ?></td>
            <td>
              <form action="../../php/admin_soporte.php" method="POST">
                <?= csrfCampoOculto(); ?>
                <input type="hidden" name="id_solicitud" value="<?= $s['id_solicitud']; ?>">
                <select name="estado" onchange="this.form.submit()" style="background:var(--azul-oscuro); color:var(--texto); border:1px solid var(--azul-acero); border-radius:6px; padding:4px 6px; font-size:0.8rem;">
                  <?php foreach (['pendiente','contactado','agendado','completado','cancelado'] as $estado): ?>
                    <option value="<?= $estado; ?>" <?= $s['estado'] === $estado ? 'selected' : ''; ?>><?= ucfirst($estado); ?></option>
                  <?php endforeach; ?>
                </select>
              </form>
            </td>
            <td><?= date('d/m/Y', strtotime($s['creado_en'])); ?></td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-envelope-open-text"></i> Mensajes, reclamos y sugerencias</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Nombre</th><th>Tipo</th><th>Mensaje</th><th>Fecha</th><th>Estado</th></tr></thead>
      <tbody>
        <?php if (empty($mensajes)): ?><tr><td colspan="5" style="text-align:center; color:var(--texto-tenue); padding:24px;">No hay mensajes todavía.</td></tr><?php endif; ?>
        <?php foreach ($mensajes as $m): ?>
          <tr>
            <td><?= htmlspecialchars($m['nombre']); ?><br><small style="color:var(--texto-tenue);"><?= htmlspecialchars($m['correo']); ?></small></td>
            <td><span class="badge-rol"><?= ucfirst($m['tipo'] ?? 'mensaje'); ?></span></td>
            <td style="max-width:280px; font-size:0.85rem; color:var(--texto-tenue);"><?= htmlspecialchars(recortar_texto($m['mensaje'], 100)); ?></td>
            <td><?= date('d/m/Y', strtotime($m['creado_en'])); ?></td>
            <td>
              <form action="../../php/admin_soporte.php" method="POST">
                <?= csrfCampoOculto(); ?>
                <input type="hidden" name="accion" value="marcar_leido_contacto">
                <input type="hidden" name="id_contacto" value="<?= $m['id_contacto']; ?>">
                <button type="submit" class="panel-accion-btn" title="<?= $m['leido'] ? 'Leído' : 'Marcar como leído'; ?>">
                  <i class="fa-solid <?= $m['leido'] ? 'fa-envelope-open' : 'fa-envelope'; ?>"></i>
                </button>
              </form>
            </td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
