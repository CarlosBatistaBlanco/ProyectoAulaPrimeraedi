<?php
/**
 * panel/administrador/usuarios.php
 * -----------------------------------------------------------------
 * CRUD de usuarios: crear, buscar, cambiar de rol, activar/desactivar, bloquear/desbloquear y resetear contraseña. El envío lo procesa php/admin_usuarios.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Gestión de Usuarios';

$roles = $pdo->query('SELECT * FROM roles ORDER BY id_rol')->fetchAll();

// Filtro de búsqueda simple por nombre o correo
$busqueda = trim($_GET['buscar'] ?? '');
if ($busqueda !== '') {
    $stmt = $pdo->prepare("SELECT u.*, r.nombre AS rol FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol
                            WHERE u.nombre LIKE :b1 OR u.correo LIKE :b2 ORDER BY u.creado_en DESC");
    $stmt->execute([':b1' => "%$busqueda%", ':b2' => "%$busqueda%"]);
} else {
    $stmt = $pdo->query("SELECT u.*, r.nombre AS rol FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol ORDER BY u.creado_en DESC");
}
$usuarios = $stmt->fetchAll();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<?php if (!empty($_GET['exito'])): ?>
  <div class="auth-alerta auth-alerta--ok" style="margin-bottom:20px;"><?= htmlspecialchars($_GET['exito']); ?></div>
<?php endif; ?>
<?php if (!empty($_GET['error'])): ?>
  <div class="auth-alerta auth-alerta--error" style="margin-bottom:20px;"><?= htmlspecialchars($_GET['error']); ?></div>
<?php endif; ?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-user-plus"></i> Crear nuevo usuario</h2>
  <form action="../../php/admin_usuarios.php" method="POST" class="panel-form">
    <?= csrfCampoOculto(); ?>
    <input type="hidden" name="accion" value="crear">

    <div class="campo"><label>Nombre completo</label><input type="text" name="nombre" required></div>
    <div class="campo"><label>Correo electrónico</label><input type="email" name="correo" required></div>
    <div class="campo"><label>Documento</label><input type="text" name="documento" required></div>
    <div class="campo"><label>Teléfono</label><input type="text" name="telefono"></div>
    <div class="campo">
      <label>Rol</label>
      <select name="id_rol" required>
        <?php foreach ($roles as $r): ?>
          <option value="<?= $r['id_rol']; ?>"><?= ucfirst($r['nombre']); ?></option>
        <?php endforeach; ?>
      </select>
    </div>
    <div class="campo"><label>Contraseña temporal</label><input type="text" name="password" required placeholder="Mínimo 8 caracteres, 1 mayúscula y 1 número"></div>

    <div class="campo campo--full">
      <button type="submit" class="btn btn--primario"><i class="fa-solid fa-plus"></i> Crear usuario</button>
    </div>
  </form>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-users"></i> Usuarios registrados (<?= count($usuarios); ?>)</h2>

  <form method="GET" style="margin-bottom:16px; display:flex; gap:10px; max-width:360px;">
    <input type="text" name="buscar" placeholder="Buscar por nombre o correo..." value="<?= htmlspecialchars($busqueda); ?>"
           style="flex:1; background:var(--azul-oscuro); border:1px solid var(--azul-acero); color:var(--texto); padding:10px 12px; border-radius:8px;">
    <button type="submit" class="btn btn--secundario"><i class="fa-solid fa-magnifying-glass"></i></button>
  </form>

  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Último acceso</th><th>Acciones</th></tr></thead>
      <tbody>
        <?php foreach ($usuarios as $us): ?>
          <tr>
            <td><?= htmlspecialchars($us['nombre']); ?></td>
            <td><?= htmlspecialchars($us['correo']); ?></td>
            <td>
              <form action="../../php/admin_usuarios.php" method="POST" style="display:inline;">
                <?= csrfCampoOculto(); ?>
                <input type="hidden" name="accion" value="cambiar_rol">
                <input type="hidden" name="id_usuario" value="<?= $us['id_usuario']; ?>">
                <select name="id_rol" onchange="this.form.submit()" style="background:var(--azul-oscuro); color:var(--texto); border:1px solid var(--azul-acero); border-radius:6px; padding:4px 6px; font-size:0.82rem;"
                  <?= $us['id_usuario'] == $u['id_usuario'] ? 'disabled title="No puedes cambiar tu propio rol"' : ''; ?>>
                  <?php foreach ($roles as $r): ?>
                    <option value="<?= $r['id_rol']; ?>" <?= $r['nombre'] === $us['rol'] ? 'selected' : ''; ?>><?= ucfirst($r['nombre']); ?></option>
                  <?php endforeach; ?>
                </select>
              </form>
            </td>
            <td>
              <?php if ($us['bloqueado']): ?><span class="badge-estado badge-estado--bloqueado">Bloqueado</span>
              <?php elseif ($us['activo']): ?><span class="badge-estado badge-estado--activo">Activo</span>
              <?php else: ?><span class="badge-estado badge-estado--inactivo">Inactivo</span><?php endif; ?>
            </td>
            <td><?= $us['ultimo_acceso'] ? date('d/m/Y H:i', strtotime($us['ultimo_acceso'])) : '—'; ?></td>
            <td>
              <div class="panel-acciones">
                <?php if ($us['id_usuario'] != $u['id_usuario']): ?>
                  <form action="../../php/admin_usuarios.php" method="POST">
                    <?= csrfCampoOculto(); ?>
                    <input type="hidden" name="accion" value="toggle_activo">
                    <input type="hidden" name="id_usuario" value="<?= $us['id_usuario']; ?>">
                    <button type="submit" class="panel-accion-btn" title="<?= $us['activo'] ? 'Desactivar' : 'Activar'; ?>">
                      <i class="fa-solid <?= $us['activo'] ? 'fa-user-slash' : 'fa-user-check'; ?>"></i>
                    </button>
                  </form>
                  <form action="../../php/admin_usuarios.php" method="POST">
                    <?= csrfCampoOculto(); ?>
                    <input type="hidden" name="accion" value="toggle_bloqueo">
                    <input type="hidden" name="id_usuario" value="<?= $us['id_usuario']; ?>">
                    <button type="submit" class="panel-accion-btn panel-accion-btn--peligro" title="<?= $us['bloqueado'] ? 'Desbloquear' : 'Bloquear'; ?>">
                      <i class="fa-solid <?= $us['bloqueado'] ? 'fa-lock-open' : 'fa-lock'; ?>"></i>
                    </button>
                  </form>
                  <form action="../../php/admin_usuarios.php" method="POST" onsubmit="return confirm('¿Restablecer la contraseña de este usuario a Temporal123?');">
                    <?= csrfCampoOculto(); ?>
                    <input type="hidden" name="accion" value="resetear_password">
                    <input type="hidden" name="id_usuario" value="<?= $us['id_usuario']; ?>">
                    <button type="submit" class="panel-accion-btn" title="Restablecer contraseña">
                      <i class="fa-solid fa-key"></i>
                    </button>
                  </form>
                  <button type="button" class="panel-accion-btn" title="Iniciar sesión como este usuario"
                          onclick="abrirModalSuplantar(<?= $us['id_usuario']; ?>, '<?= htmlspecialchars($us['nombre'], ENT_QUOTES); ?>')">
                    <i class="fa-solid fa-user-secret"></i>
                  </button>
                <?php else: ?>
                  <span style="color:var(--texto-tenue); font-size:0.8rem;">(tú)</span>
                <?php endif; ?>
              </div>
            </td>
          </tr>
        <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</div>

<!-- ============ MODAL: código de verificación para "iniciar sesión como" ============ -->
<div class="modal-overlay" id="modalSuplantarOverlay">
  <div class="modal-caja">
    <h3><i class="fa-solid fa-user-secret"></i> Iniciar sesión como <span id="modalSuplantarNombre"></span></h3>
    <p style="color:var(--texto-tenue); font-size:0.88rem;">Por seguridad, escribe el código de verificación de administrador para continuar. Vas a ver el sistema exactamente como lo ve este usuario, sin cerrar tu propia sesión.</p>
    <form action="../../php/admin_suplantar.php" method="POST">
      <?= csrfCampoOculto(); ?>
      <input type="hidden" name="id_usuario" id="modalSuplantarId">
      <div class="campo">
        <label>Código de verificación</label>
        <input type="password" name="codigo_verificacion" required autocomplete="off">
      </div>
      <div style="display:flex; gap:10px; margin-top:16px;">
        <button type="submit" class="btn btn--primario"><i class="fa-solid fa-right-to-bracket"></i> Continuar</button>
        <button type="button" class="btn btn--secundario" onclick="cerrarModalSuplantar()">Cancelar</button>
      </div>
    </form>
  </div>
</div>

<script>
  function abrirModalSuplantar(id, nombre) {
    document.getElementById('modalSuplantarId').value = id;
    document.getElementById('modalSuplantarNombre').textContent = nombre;
    document.getElementById('modalSuplantarOverlay').classList.add('activo');
  }
  function cerrarModalSuplantar() {
    document.getElementById('modalSuplantarOverlay').classList.remove('activo');
  }
</script>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
