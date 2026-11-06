<?php
/**
 * panel_header.php
 * -----------------------------------------------------------------
 * Se incluye al inicio de cada página dentro de panel/<rol>/.
 * Espera que la página ya haya definido:
 *   $u            -> array devuelto por requerirRol([...])
 *   $tituloPagina -> título que se muestra en la pestaña y en el topbar
 *   $rutaBase     -> '../../' (dos niveles hacia la raíz del sitio)
 * -----------------------------------------------------------------
 */
$rutaBase = $rutaBase ?? '../../';
?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><?= htmlspecialchars($tituloPagina ?? 'Panel'); ?> | TECNISECURE</title>
<link rel="icon" href="<?= $rutaBase ?>img/logo/logo.svg" type="image/svg+xml">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
<link rel="stylesheet" href="<?= $rutaBase ?>css/style.css?v=<?= @filemtime(__DIR__ . '/../css/style.css') ?: '1'; ?>">
<link rel="stylesheet" href="<?= $rutaBase ?>css/auth.css?v=<?= @filemtime(__DIR__ . '/../css/auth.css') ?: '1'; ?>">
<link rel="stylesheet" href="<?= $rutaBase ?>css/panel.css?v=<?= @filemtime(__DIR__ . '/../css/panel.css') ?: '1'; ?>">
</head>
<body class="panel-body">
<?php require __DIR__ . '/banner_suplantacion.php'; ?>
<div class="panel-layout">

  <!-- ============ SIDEBAR ============ -->
  <aside class="panel-sidebar" id="panelSidebar">
    <a href="<?= $rutaBase ?>index.php" class="panel-sidebar__logo logo">
      <img src="<?= $rutaBase ?>img/logo/logo-claro.svg" alt="TECNISECURE" class="logo__claro">
      <img src="<?= $rutaBase ?>img/logo/logo.svg" alt="TECNISECURE" class="logo__oscuro">
    </a>
    <?php require __DIR__ . '/panel_sidebar.php'; ?>
  </aside>
  <div class="panel-overlay" id="panelOverlay"></div>

  <!-- ============ CONTENIDO ============ -->
  <div class="panel-contenido">
    <header class="panel-topbar">
      <button class="panel-toggle" id="panelToggle" aria-label="Abrir menú"><i class="fa-solid fa-bars"></i></button>
      <h1 class="panel-topbar__titulo"><?= htmlspecialchars($tituloPagina ?? 'Panel'); ?></h1>
      <div class="panel-topbar__usuario">
        <button type="button" class="btn-tema" title="Cambiar tema claro/oscuro">
          <i class="fa-solid fa-moon"></i>
        </button>
        <?php
          $totalNoLeidas = 0;
          $notiRecientes = [];
          try {
              $notiNoLeidas = $pdo->prepare('SELECT COUNT(*) FROM notificaciones WHERE id_usuario = :id AND leida = 0');
              $notiNoLeidas->execute([':id' => $u['id_usuario']]);
              $totalNoLeidas = (int)$notiNoLeidas->fetchColumn();

              $notiRecientes = $pdo->prepare('SELECT * FROM notificaciones WHERE id_usuario = :id ORDER BY creado_en DESC LIMIT 8');
              $notiRecientes->execute([':id' => $u['id_usuario']]);
              $notiRecientes = $notiRecientes->fetchAll();
          } catch (PDOException $e) {
              // Si la tabla no existe todavía (base de datos desactualizada), simplemente no se muestran notificaciones
          }
        ?>
        <div class="panel-notificaciones">
          <button class="panel-noti-btn" id="botonNotificaciones">
            <i class="fa-solid fa-bell"></i>
            <?php if ($totalNoLeidas > 0): ?><span class="panel-noti-contador"><?= $totalNoLeidas; ?></span><?php endif; ?>
          </button>
          <div class="panel-noti-dropdown" id="dropdownNotificaciones">
            <div class="panel-noti-dropdown__header">
              <span>Notificaciones</span>
              <?php if ($totalNoLeidas > 0): ?>
                <a href="<?= $rutaBase ?>php/marcar_notificaciones.php">Marcar todas como leídas</a>
              <?php endif; ?>
            </div>
            <?php if (empty($notiRecientes)): ?>
              <p class="panel-noti-vacio">No tienes notificaciones todavía.</p>
            <?php else: ?>
              <?php foreach ($notiRecientes as $n): ?>
                <a href="<?= $n['enlace'] ? $rutaBase . ltrim($n['enlace'], '/') : '#'; ?>" class="panel-noti-item <?= $n['leida'] ? '' : 'no-leida'; ?>">
                  <strong><?= htmlspecialchars($n['titulo']); ?></strong>
                  <?php if ($n['mensaje']): ?><span><?= htmlspecialchars($n['mensaje']); ?></span><?php endif; ?>
                  <small><?= date('d/m/Y H:i', strtotime($n['creado_en'])); ?></small>
                </a>
              <?php endforeach; ?>
            <?php endif; ?>
          </div>
        </div>

        <span class="panel-topbar__nombre"><?= htmlspecialchars($u['nombre']); ?>
          <small><?= ucfirst(htmlspecialchars($u['rol'])); ?></small>
        </span>
        <?php if (!empty($u['avatar']) && file_exists(__DIR__ . '/../' . $u['avatar'])): ?>
          <div class="panel-avatar panel-avatar--foto" style="background-image:url('<?= $rutaBase . htmlspecialchars($u['avatar']); ?>');"></div>
        <?php else: ?>
          <div class="panel-avatar"><?= strtoupper(substr($u['nombre'], 0, 1)); ?></div>
        <?php endif; ?>
        <a href="<?= $rutaBase ?>logout.php" class="panel-logout" title="Cerrar sesión"><i class="fa-solid fa-right-from-bracket"></i></a>
      </div>
    </header>
    <main class="panel-main">