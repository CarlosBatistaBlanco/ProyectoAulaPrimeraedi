<?php
/**
 * panel/cliente/chat.php
 * -----------------------------------------------------------------
 * Vista del cliente para chatear con un asesor de la empresa. Usa AJAX (js/panel.js) para enviar y recibir mensajes sin recargar la página.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['cliente']);
$tituloPagina = 'Chat con un Asesor';

$stmt = $pdo->prepare('SELECT * FROM clientes WHERE id_usuario = :id');
$stmt->execute([':id' => $u['id_usuario']]);
$cliente = $stmt->fetch();

$mensajes = [];
if ($cliente) {
    $pdo->prepare("UPDATE mensajes_chat SET leido = 1 WHERE id_cliente = :id AND remitente = 'asesor'")->execute([':id' => $cliente['id_cliente']]);
    $stmt = $pdo->prepare('SELECT * FROM mensajes_chat WHERE id_cliente = :id ORDER BY creado_en ASC');
    $stmt->execute([':id' => $cliente['id_cliente']]);
    $mensajes = $stmt->fetchAll();
}

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box chat-layout chat-layout--solo">
  <div class="chat-conversacion">
    <h3 style="margin-bottom:6px;"><i class="fa-solid fa-headset"></i> Habla con un asesor</h3>
    <p style="color:var(--texto-tenue); font-size:0.85rem; margin-bottom:14px;">Un asesor de TECNISECURE responderá tu mensaje en horario laboral (lunes a sábado, 8am - 6pm).</p>
    <div class="chat-mensajes" id="chatMensajes" data-cliente="<?= $cliente['id_cliente'] ?? 0; ?>">
      <?php if (empty($mensajes)): ?>
        <p style="text-align:center; color:var(--texto-tenue); padding:30px;">Escribe tu primer mensaje para iniciar la conversación.</p>
      <?php endif; ?>
      <?php foreach ($mensajes as $m): ?>
        <div class="chat-burbuja chat-burbuja--<?= $m['remitente'] === 'cliente' ? 'cliente' : 'asesor'; ?>">
          <?= htmlspecialchars($m['mensaje']); ?>
          <small><?= date('d/m H:i', strtotime($m['creado_en'])); ?></small>
        </div>
      <?php endforeach; ?>
    </div>
    <form id="formChat" class="chat-form">
      <input type="hidden" id="csrfChat" value="<?= csrfToken(); ?>">
      <input type="hidden" id="idClienteChat" value="<?= $cliente['id_cliente'] ?? 0; ?>">
      <input type="text" id="mensajeChat" placeholder="Escribe tu mensaje..." required autocomplete="off">
      <button type="submit"><i class="fa-solid fa-paper-plane"></i></button>
    </form>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
