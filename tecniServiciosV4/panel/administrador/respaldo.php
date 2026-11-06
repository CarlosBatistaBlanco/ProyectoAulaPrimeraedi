<?php
/**
 * panel/administrador/respaldo.php
 * -----------------------------------------------------------------
 * Panel para descargar un respaldo .sql completo de la base de datos, o restaurar uno. El procesamiento real lo hace php/respaldo_bd.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Respaldo de Base de Datos';

// Estadísticas de almacenamiento (tamaño de cada tabla)
$tablas = $pdo->query("SELECT table_name AS tabla, table_rows AS filas,
    ROUND((data_length + index_length) / 1024, 1) AS tamano_kb
    FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY tabla")->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-database"></i> Respaldar base de datos</h2>
  <p style="color:var(--texto-tenue); margin-bottom:16px;">
    Descarga un archivo .sql con TODA la información actual (estructura + datos). Guárdalo en un lugar seguro
    (USB, Google Drive, etc.) — es tu copia de seguridad para poder restaurar el sistema si algo falla.
  </p>
  <a href="../../php/respaldo_bd.php?accion=exportar" class="btn btn--primario"><i class="fa-solid fa-download"></i> Descargar respaldo (.sql)</a>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-rotate-left"></i> Restaurar desde un respaldo</h2>
  <p style="color:var(--texto-tenue); margin-bottom:16px;">
    <i class="fa-solid fa-triangle-exclamation" style="color:#ffb199;"></i>
    Esto reemplaza los datos actuales por los del archivo. Úsalo con cuidado, idealmente solo en caso de emergencia.
  </p>
  <form action="../../php/respaldo_bd.php" method="POST" enctype="multipart/form-data" onsubmit="return confirm('¿Seguro? Esto reemplazará los datos actuales.');">
    <?= csrfCampoOculto(); ?>
    <input type="hidden" name="accion" value="restaurar">
    <input type="file" name="archivo_sql" accept=".sql" required>
    <button type="submit" class="btn btn--secundario" style="margin-top:12px; border-color:var(--naranja); color:var(--naranja);"><i class="fa-solid fa-upload"></i> Restaurar</button>
  </form>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-chart-simple"></i> Uso de almacenamiento por tabla</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Tabla</th><th>Filas aprox.</th><th>Tamaño</th></tr></thead>
      <tbody>
        <?php foreach ($tablas as $t): ?>
          <tr><td><?= htmlspecialchars($t['tabla']); ?></td><td><?= number_format($t['filas']); ?></td><td><?= $t['tamano_kb']; ?> KB</td></tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
