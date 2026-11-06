<?php
/**
 * registro.php
 * -----------------------------------------------------------------
 * Formulario público para que un visitante cree su cuenta de cliente. El envío lo procesa php/procesar_registro.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/auth.php';
if (usuarioActual()) { header('Location: index.php'); exit; }

$tituloPagina = 'Crear cuenta | TECNISECURE';
$rutaBase = '';
$error = $_GET['error'] ?? '';
require_once __DIR__ . '/includes/auth_header.php';
?>
<h1 class="auth-titulo"><i class="fa-solid fa-user-plus"></i> Crear cuenta</h1>
<p class="auth-subtitulo">Regístrate para comprar, ver tus pedidos y recibir promociones.</p>

<?php if ($error): ?>
  <div class="auth-alerta auth-alerta--error"><?= htmlspecialchars($error); ?></div>
<?php endif; ?>

<form action="php/procesar_registro.php" method="POST" class="auth-form" id="formRegistro">
  <?= csrfCampoOculto(); ?>

  <div class="campo">
    <label for="nombre">Nombre completo</label>
    <input type="text" id="nombre" name="nombre" required placeholder="Ej: Ana Martínez" autofocus>
  </div>

  <div class="campo">
    <label for="documento">Documento de identidad</label>
    <input type="text" id="documento" name="documento" required placeholder="Ej: 1234567890">
  </div>

  <div class="campo">
    <label for="correo">Correo electrónico</label>
    <input type="email" id="correo" name="correo" required placeholder="tucorreo@ejemplo.com">
  </div>

  <div class="campo">
    <label for="telefono">Teléfono</label>
    <input type="tel" id="telefono" name="telefono" placeholder="300 123 4567">
  </div>

  <div class="campo">
    <label for="password">Contraseña</label>
    <div class="campo-password">
      <input type="password" id="password" name="password" required minlength="8" placeholder="Mínimo 8 caracteres, 1 mayúscula y 1 número">
      <button type="button" class="toggle-password" data-target="password" aria-label="Mostrar u ocultar contraseña"><i class="fa-solid fa-eye"></i></button>
    </div>
    <span class="ayuda-campo" id="ayudaPassword">Debe tener mínimo 8 caracteres, una mayúscula y un número.</span>
  </div>

  <div class="campo">
    <label for="password2">Confirmar contraseña</label>
    <div class="campo-password">
      <input type="password" id="password2" name="password2" required placeholder="Repite tu contraseña">
      <button type="button" class="toggle-password" data-target="password2" aria-label="Mostrar u ocultar contraseña"><i class="fa-solid fa-eye"></i></button>
    </div>
    <span class="ayuda-campo" id="ayudaCoinciden"></span>
  </div>

  <div class="campo campo-captcha">
    <label for="captcha">Verificación: ¿cuánto es <?= generarCaptchaPregunta(); ?>?</label>
    <input type="number" id="captcha" name="captcha" required placeholder="Escribe el resultado" autocomplete="off">
  </div>

  <label class="auth-checkbox auth-checkbox--terminos">
    <input type="checkbox" name="terminos" required>
    Acepto los <a href="#" target="_blank">términos y condiciones</a> y el tratamiento de mis datos.
  </label>

  <button type="submit" class="btn btn--primario btn--full">
    <i class="fa-solid fa-user-plus"></i> Crear mi cuenta
  </button>

  <p class="auth-pie">¿Ya tienes cuenta? <a href="login.php">Inicia sesión</a></p>
</form>

<?php require_once __DIR__ . '/includes/auth_footer.php'; ?>
