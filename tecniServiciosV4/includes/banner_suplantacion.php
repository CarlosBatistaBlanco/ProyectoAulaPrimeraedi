<?php
/**
 * includes/banner_suplantacion.php
 * -----------------------------------------------------------------
 * Se incluye tanto en el sitio público (includes/header.php) como en
 * el panel (includes/panel_header.php). Solo se muestra algo si la
 * sesión actual está suplantando a otro usuario (ver php/auth.php).
 * $rutaBase debe estar definida antes de incluir este archivo.
 * -----------------------------------------------------------------
 */
if (estaSuplantando()):
    $admin = $_SESSION['suplantador'];
?>
<div class="banner-suplantacion">
  <i class="fa-solid fa-user-secret"></i>
  Estás viendo el sistema como <b><?= htmlspecialchars($_SESSION['nombre']); ?></b>
  (sesión iniciada por <?= htmlspecialchars($admin['nombre']); ?>).
  <a href="<?= $rutaBase ?>php/admin_volver.php">Volver a mi cuenta de administrador</a>
</div>
<?php endif; ?>
