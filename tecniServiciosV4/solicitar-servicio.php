<?php
/**
 * solicitar-servicio.php
 * -----------------------------------------------------------------
 * Cuando alguien hace clic en "Solicitar este servicio" desde el
 * inicio, llega aquí. El formulario deja elegir entre dos caminos:
 *   - "cotizacion": solo quiere que lo contacten con un precio.
 *   - "cita": quiere agendar una visita técnica en fecha/hora concretas.
 * Ambos casos se guardan en la tabla solicitudes_servicio.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/conexion.php';
require_once __DIR__ . '/php/auth.php';

$idServicio = (int)($_GET['id'] ?? 0);
$stmt = $pdo->prepare('SELECT * FROM servicios WHERE id_servicio = :id LIMIT 1');
$stmt->execute([':id' => $idServicio]);
$servicio = $stmt->fetch();

if (!$servicio) {
    header('Location: index.php#servicios');
    exit;
}

$tituloPagina = 'Solicitar: ' . $servicio['titulo'] . ' | TECNISECURE';
$rutaBase = '';

// Si el visitante ya inició sesión como cliente, se precargan sus datos
$sesion = usuarioActual();
$datosCliente = null;
if ($sesion && $sesion['rol'] === 'cliente') {
    $stmtC = $pdo->prepare('SELECT * FROM clientes WHERE id_usuario = :id LIMIT 1');
    $stmtC->execute([':id' => $sesion['id_usuario']]);
    $datosCliente = $stmtC->fetch();
}

require_once __DIR__ . '/includes/header.php';
?>

<main>
  <section class="pagina-header">
    <div class="contenedor">
      <p class="seccion__eyebrow">Servicios</p>
      <h1 class="seccion__titulo">Solicitar: <?= htmlspecialchars($servicio['titulo']); ?></h1>
      <p class="seccion__descripcion"><?= htmlspecialchars($servicio['descripcion']); ?></p>
    </div>
  </section>

  <section class="contacto" style="padding-top:0;">
    <div class="contenedor" style="max-width:640px;">

      <div class="contacto__form" style="padding:32px;">
        <?php if (!empty($_GET['exito'])): ?>
          <div class="auth-alerta auth-alerta--ok" style="margin-bottom:20px;">
            <?= htmlspecialchars($_GET['exito']); ?>
          </div>
        <?php else: ?>

        <?php if (!empty($_GET['error'])): ?>
          <div class="auth-alerta auth-alerta--error" style="margin-bottom:20px;"><?= htmlspecialchars($_GET['error']); ?></div>
        <?php endif; ?>

        <!-- Selector de tipo de solicitud -->
        <div class="tipo-solicitud" id="tipoSolicitud">
          <button type="button" class="tipo-solicitud__opcion activo" data-tipo="cotizacion">
            <i class="fa-solid fa-file-invoice-dollar"></i>
            <span>Quiero una cotización</span>
            <small>Te contactamos con un precio, sin fecha fija</small>
          </button>
          <button type="button" class="tipo-solicitud__opcion" data-tipo="cita">
            <i class="fa-solid fa-calendar-check"></i>
            <span>Quiero agendar una cita</span>
            <small>Elige el día y la hora que prefieras</small>
          </button>
        </div>

        <form action="php/procesar_solicitud_servicio.php" method="POST" class="auth-form" id="formSolicitud">
          <?= csrfCampoOculto(); ?>
          <input type="hidden" name="id_servicio" value="<?= $servicio['id_servicio']; ?>">
          <input type="hidden" name="tipo" id="tipoInput" value="cotizacion">

          <div class="campo">
            <label for="nombre">Nombre completo</label>
            <input type="text" id="nombre" name="nombre" required
                   value="<?= htmlspecialchars($datosCliente['nombre'] ?? ''); ?>" placeholder="Ej: Ana Martínez">
          </div>

          <div class="campo">
            <label for="correo">Correo electrónico</label>
            <input type="email" id="correo" name="correo" required
                   value="<?= htmlspecialchars($datosCliente['correo'] ?? ''); ?>" placeholder="tucorreo@ejemplo.com">
          </div>

          <div class="campo">
            <label for="telefono">Teléfono</label>
            <input type="tel" id="telefono" name="telefono" required
                   value="<?= htmlspecialchars($datosCliente['telefono'] ?? ''); ?>" placeholder="300 123 4567">
          </div>

          <div class="campo">
            <label for="direccion">Dirección donde se necesita el servicio</label>
            <input type="text" id="direccion" name="direccion" placeholder="Barrio, calle, ciudad">
          </div>

          <div class="campo-fecha-hora" id="campoFechaHora" style="display:none;">
            <div class="campo">
              <label for="fecha_preferida">Fecha preferida</label>
              <input type="date" id="fecha_preferida" name="fecha_preferida" min="<?= date('Y-m-d'); ?>">
            </div>
            <div class="campo">
              <label for="hora_preferida">Hora preferida</label>
              <input type="time" id="hora_preferida" name="hora_preferida" min="08:00" max="18:00">
            </div>
          </div>

          <div class="campo">
            <label for="descripcion">Cuéntanos brevemente qué necesitas</label>
            <textarea id="descripcion" name="descripcion" rows="4" required
                      placeholder="Ej: Necesito 3 cámaras para el frente y patio de mi casa..."></textarea>
          </div>

          <button type="submit" class="btn btn--primario btn--full">
            <i class="fa-solid fa-paper-plane"></i> <span id="textoBoton">Enviar solicitud de cotización</span>
          </button>
        </form>
        <?php endif; ?>
      </div>

      <p style="text-align:center; margin-top:20px;">
        <a href="index.php#servicios" style="color:var(--texto-tenue);"><i class="fa-solid fa-arrow-left"></i> Volver a servicios</a>
      </p>
    </div>
  </section>
</main>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
