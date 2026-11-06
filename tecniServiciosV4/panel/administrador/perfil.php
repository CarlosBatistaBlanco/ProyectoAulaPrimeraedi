<?php
/**
 * panel/administrador/perfil.php
 * -----------------------------------------------------------------
 * Página para editar los propios datos (nombre, teléfono, foto de
 * perfil) y cambiar la propia contraseña. El envío lo procesa
 * php/procesar_perfil.php, que funciona igual para cualquier rol.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Mi Perfil';
$exito = $_GET['exito'] ?? '';
$error = $_GET['error'] ?? '';

$stmt = $pdo->prepare('SELECT * FROM usuarios WHERE id_usuario = :id');
$stmt->execute([':id' => $u['id_usuario']]);
$usuario = $stmt->fetch();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if ($exito): ?><div class="auth-alerta auth-alerta--ok" style="margin-bottom:18px;"><?= htmlspecialchars($exito); ?></div><?php endif; ?>
<?php if ($error): ?><div class="auth-alerta auth-alerta--error" style="margin-bottom:18px;"><?= htmlspecialchars($error); ?></div><?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-user"></i> Mis datos</h2>

  <div style="display:flex; align-items:center; gap:18px; margin-bottom:22px;">
    <?php if (!empty($usuario['avatar']) && file_exists(__DIR__ . '/../../' . $usuario['avatar'])): ?>
      <div style="width:72px; height:72px; border-radius:50%; background-image:url('../../<?= htmlspecialchars($usuario['avatar']); ?>'); background-size:cover; background-position:center; border:3px solid var(--cian);"></div>
    <?php else: ?>
      <div style="width:72px; height:72px; border-radius:50%; background:linear-gradient(135deg, var(--cian), #0077b6); color:#06131f; font-weight:700; font-size:1.8rem; display:flex; align-items:center; justify-content:center;">
        <?= strtoupper(substr($usuario['nombre'], 0, 1)); ?>
      </div>
    <?php endif; ?>
    <div>
      <strong style="font-size:1.05rem;"><?= htmlspecialchars($usuario['nombre']); ?></strong><br>
      <span style="color:var(--texto-tenue); font-size:0.85rem;"><?= htmlspecialchars($usuario['correo']); ?></span>
    </div>
  </div>

  <form action="../../php/procesar_perfil.php" method="POST" class="panel-form" enctype="multipart/form-data">
    <?= csrfCampoOculto(); ?>
    <input type="hidden" name="accion" value="actualizar_datos">
    <div class="campo"><label>Nombre completo</label><input type="text" name="nombre" value="<?= htmlspecialchars($usuario['nombre']); ?>" required></div>
    <div class="campo"><label>Correo electrónico</label><input type="email" value="<?= htmlspecialchars($usuario['correo']); ?>" disabled title="El correo no se puede cambiar por seguridad"></div>
    <div class="campo"><label>Teléfono</label><input type="text" name="telefono" value="<?= htmlspecialchars($usuario['telefono'] ?? ''); ?>"></div>
    <div class="campo"><label>Foto de perfil</label><input type="file" name="avatar" accept="image/*"></div>
    <div class="campo campo--full">
      <button type="submit" class="btn btn--primario"><i class="fa-solid fa-floppy-disk"></i> Guardar cambios</button>
    </div>
  </form>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-key"></i> Cambiar contraseña</h2>
  <form action="../../php/procesar_perfil.php" method="POST" class="panel-form">
    <?= csrfCampoOculto(); ?>
    <input type="hidden" name="accion" value="cambiar_password">
    <div class="campo"><label>Contraseña actual</label><input type="password" name="actual" required></div>
    <div class="campo"></div>
    <div class="campo"><label>Nueva contraseña</label><input type="password" name="nueva" required minlength="8" placeholder="Mínimo 8 caracteres, 1 mayúscula y 1 número"></div>
    <div class="campo"><label>Confirmar nueva contraseña</label><input type="password" name="nueva2" required></div>
    <div class="campo campo--full">
      <button type="submit" class="btn btn--secundario"><i class="fa-solid fa-key"></i> Cambiar contraseña</button>
    </div>
  </form>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
