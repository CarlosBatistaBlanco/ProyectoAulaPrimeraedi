<?php
/**
 * restablecer.php
 * -----------------------------------------------------------------
 * Formulario para definir una contraseña nueva, usando el token recibido en el enlace de recuperación. El envío lo procesa php/procesar_restablecer.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/auth.php';
$tituloPagina = 'Restablecer contraseña | TECNISECURE';
$rutaBase = '';
$token = $_GET['token'] ?? '';
$error = $_GET['error'] ?? '';

// Verifica que el token exista, no esté usado y no haya expirado
$stmt = $pdo->prepare('SELECT * FROM tokens_recuperacion WHERE token = :t AND usado = 0 AND expira_en > NOW() LIMIT 1');
$stmt->execute([':t' => $token]);
$tokenValido = $stmt->fetch();

require_once __DIR__ . '/includes/auth_header.php';
?>
<h1 class="auth-titulo"><i class="fa-solid fa-lock-open"></i> Nueva contraseña</h1>

<?php if (!$tokenValido): ?>
  <div class="auth-alerta auth-alerta--error">
    Este enlace no es válido o ya expiró. <a href="recuperar.php">Solicita uno nuevo</a>.
  </div>
<?php else: ?>
  <p class="auth-subtitulo">Escribe tu nueva contraseña.</p>
  <?php if ($error): ?><div class="auth-alerta auth-alerta--error"><?= htmlspecialchars($error); ?></div><?php endif; ?>

  <form action="php/procesar_restablecer.php" method="POST" class="auth-form" id="formRestablecer">
    <?= csrfCampoOculto(); ?>
    <input type="hidden" name="token" value="<?= htmlspecialchars($token); ?>">

    <div class="campo">
      <label for="password">Nueva contraseña</label>
      <div class="campo-password">
        <input type="password" id="password" name="password" required minlength="8" placeholder="Mínimo 8 caracteres, 1 mayúscula y 1 número">
        <button type="button" class="toggle-password" data-target="password" aria-label="Mostrar u ocultar contraseña"><i class="fa-solid fa-eye"></i></button>
      </div>
    </div>
    <div class="campo">
      <label for="password2">Confirmar contraseña</label>
      <div class="campo-password">
        <input type="password" id="password2" name="password2" required placeholder="Repite tu contraseña">
        <button type="button" class="toggle-password" data-target="password2" aria-label="Mostrar u ocultar contraseña"><i class="fa-solid fa-eye"></i></button>
      </div>
    </div>
    <button type="submit" class="btn btn--primario btn--full"><i class="fa-solid fa-check"></i> Guardar nueva contraseña</button>
  </form>
<?php endif; ?>

<?php require_once __DIR__ . '/includes/auth_footer.php'; ?>
