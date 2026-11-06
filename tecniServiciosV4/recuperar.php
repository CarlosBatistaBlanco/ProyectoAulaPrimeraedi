<?php
/**
 * recuperar.php
 * -----------------------------------------------------------------
 * Formulario para solicitar el enlace de restablecimiento de contraseña. El envío lo procesa php/procesar_recuperar.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/auth.php';
$tituloPagina = 'Recuperar contraseña | TECNISECURE';
$rutaBase = '';
$error = $_GET['error'] ?? '';
$exito = $_GET['exito'] ?? '';
$enlaceDemo = $_GET['enlace_demo'] ?? '';
require_once __DIR__ . '/includes/auth_header.php';
?>
<h1 class="auth-titulo"><i class="fa-solid fa-key"></i> Recuperar contraseña</h1>
<p class="auth-subtitulo">Escribe tu correo y te enviaremos un enlace para restablecerla.</p>

<?php if ($error): ?><div class="auth-alerta auth-alerta--error"><?= htmlspecialchars($error); ?></div><?php endif; ?>
<?php if ($exito): ?><div class="auth-alerta auth-alerta--ok"><?= htmlspecialchars($exito); ?></div><?php endif; ?>

<?php if ($enlaceDemo): ?>
  <div class="auth-alerta auth-alerta--info">
    <b>Modo de prueba (sin servidor de correo configurado):</b><br>
    En un sitio en producción este enlace llegaría a tu correo. Por ahora, haz clic aquí para continuar:<br>
    <a href="<?= htmlspecialchars($enlaceDemo); ?>"><?= htmlspecialchars($enlaceDemo); ?></a>
  </div>
<?php endif; ?>

<form action="php/procesar_recuperar.php" method="POST" class="auth-form">
  <?= csrfCampoOculto(); ?>
  <div class="campo">
    <label for="correo">Correo electrónico</label>
    <input type="email" id="correo" name="correo" required placeholder="tucorreo@ejemplo.com" autofocus>
  </div>
  <button type="submit" class="btn btn--primario btn--full">
    <i class="fa-solid fa-paper-plane"></i> Enviar enlace de recuperación
  </button>
  <p class="auth-pie"><a href="login.php">Volver a iniciar sesión</a></p>
</form>

<?php require_once __DIR__ . '/includes/auth_footer.php'; ?>
