<?php
/**
 * login.php
 * -----------------------------------------------------------------
 * Formulario público de inicio de sesión. Muestra el captcha matemático, el campo de 'recordarme' y enlaces a registro/recuperar contraseña. El envío del formulario lo procesa php/procesar_login.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/auth.php';

// Si ya hay sesión activa, redirige directo a su panel según el rol
$u = usuarioActual();
if ($u) {
    header('Location: panel/' . $u['rol'] . '/index.php');
    exit;
}

$tituloPagina = 'Iniciar sesión | TECNISECURE';
$rutaBase = '';
$redir = $_GET['redir'] ?? '';
$error = $_GET['error'] ?? '';
$exito = $_GET['exito'] ?? '';

require_once __DIR__ . '/includes/auth_header.php';
?>
<h1 class="auth-titulo"><i class="fa-solid fa-shield-halved"></i> Iniciar sesión</h1>
<p class="auth-subtitulo">Accede a tu panel de TECNISECURE.</p>

<?php if ($error): ?>
  <div class="auth-alerta auth-alerta--error"><?= htmlspecialchars($error); ?></div>
<?php endif; ?>
<?php if ($exito): ?>
  <div class="auth-alerta auth-alerta--ok"><?= htmlspecialchars($exito); ?></div>
<?php endif; ?>

<form action="php/procesar_login.php" method="POST" class="auth-form" id="formLogin">
  <?= csrfCampoOculto(); ?>
  <input type="hidden" name="redir" value="<?= htmlspecialchars($redir); ?>">

  <div class="campo">
    <label for="correo">Correo electrónico</label>
    <input type="email" id="correo" name="correo" required placeholder="tucorreo@ejemplo.com" autofocus>
  </div>

  <div class="campo">
    <label for="password">Contraseña</label>
    <div class="campo-password">
      <input type="password" id="password" name="password" required placeholder="Tu contraseña">
      <button type="button" class="toggle-password" data-target="password" aria-label="Mostrar u ocultar contraseña">
        <i class="fa-solid fa-eye"></i>
      </button>
    </div>
  </div>

  <div class="campo campo-captcha">
    <label for="captcha">Verificación: ¿cuánto es <?= generarCaptchaPregunta(); ?>?</label>
    <input type="number" id="captcha" name="captcha" required placeholder="Escribe el resultado" autocomplete="off">
  </div>

  <div class="auth-fila-extra">
    <label class="auth-checkbox">
      <input type="checkbox" name="recordarme" value="1"> Recordarme
    </label>
    <a href="recuperar.php">¿Olvidaste tu contraseña?</a>
  </div>

  <button type="submit" class="btn btn--primario btn--full">
    <i class="fa-solid fa-right-to-bracket"></i> Entrar
  </button>

  <p class="auth-pie">¿No tienes cuenta? <a href="registro.php">Regístrate aquí</a></p>
</form>

<p class="auth-nota">
  <i class="fa-solid fa-circle-info"></i>
  Cuenta de prueba (administrador): <b>admin@tecnicsecure.com</b> / <b>Admin123!</b>
</p>

<?php require_once __DIR__ . '/includes/auth_footer.php'; ?>
