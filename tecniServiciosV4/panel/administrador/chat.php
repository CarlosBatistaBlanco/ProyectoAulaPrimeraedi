<?php
/**
 * panel/administrador/chat.php
 * -----------------------------------------------------------------
 * Bandeja de chats: lista todos los clientes con conversación activa y permite responderles. Comparte la lógica AJAX con panel/cliente/chat.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['administrador']);
$tituloPagina = 'Chat con Clientes';

$idCliente = (int)($_GET['cliente'] ?? 0);

// Lista de clientes con mensajes, y cuántos no leídos tiene cada uno
$clientes = $pdo->query("SELECT c.id_cliente, c.nombre,
    (SELECT COUNT(*) FROM mensajes_chat WHERE id_cliente = c.id_cliente AND remitente='cliente' AND leido = 0) AS no_leidos,
    (SELECT MAX(creado_en) FROM mensajes_chat WHERE id_cliente = c.id_cliente) AS ultimo_mensaje
    FROM clientes c
    WHERE EXISTS (SELECT 1 FROM mensajes_chat WHERE id_cliente = c.id_cliente)
    ORDER BY ultimo_mensaje DESC")->fetchAll();

$clienteActual = null;
$mensajes = [];
if ($idCliente) {
    $stmt = $pdo->prepare('SELECT * FROM clientes WHERE id_cliente = :id');
    $stmt->execute([':id' => $idCliente]);
    $clienteActual = $stmt->fetch();

    if ($clienteActual) {
        $pdo->prepare("UPDATE mensajes_chat SET leido = 1 WHERE id_cliente = :id AND remitente = 'cliente'")->execute([':id' => $idCliente]);
        $stmt = $pdo->prepare('SELECT * FROM mensajes_chat WHERE id_cliente = :id ORDER BY creado_en ASC');
        $stmt->execute([':id' => $idCliente]);
        $mensajes = $stmt->fetchAll();
    }
}

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box chat-layout">
  <div class="chat-lista">
    <h3 style="font-size:0.9rem; color:var(--texto-tenue); margin-bottom:12px;">Conversaciones</h3>
    <?php if (empty($clientes)): ?>
      <p style="color:var(--texto-tenue); font-size:0.85rem;">Ningún cliente ha escrito todavía.</p>
    <?php endif; ?>
    <?php foreach ($clientes as $c): ?>
      <a href="?cliente=<?= $c['id_cliente']; ?>" class="chat-lista__item <?= $c['id_cliente'] == $idCliente ? 'activo' : ''; ?>">
        <span><?= htmlspecialchars($c['nombre']); ?></span>
        <?php if ($c['no_leidos'] > 0): ?><span class="carrito-contador" style="position:static;"><?= $c['no_leidos']; ?></span><?php endif; ?>
      </a>
    <?php endforeach; ?>
  </div>

  <div class="chat-conversacion">
    <?php if (!$clienteActual): ?>
      <p style="color:var(--texto-tenue); text-align:center; padding:60px 20px;">Selecciona una conversación de la izquierda.</p>
    <?php else: ?>
      <h3 style="margin-bottom:14px;"><?= htmlspecialchars($clienteActual['nombre']); ?></h3>
      <div class="chat-mensajes" id="chatMensajes" data-cliente="<?= $idCliente; ?>">
        <?php foreach ($mensajes as $m): ?>
          <div class="chat-burbuja chat-burbuja--<?= $m['remitente']; ?>">
            <?= htmlspecialchars($m['mensaje']); ?>
            <small><?= date('d/m H:i', strtotime($m['creado_en'])); ?></small>
          </div>
        <?php endforeach; ?>
      </div>
      <form id="formChat" class="chat-form">
        <input type="hidden" id="csrfChat" value="<?= csrfToken(); ?>">
        <input type="hidden" id="idClienteChat" value="<?= $idCliente; ?>">
        <input type="text" id="mensajeChat" placeholder="Escribe una respuesta..." required autocomplete="off">
        <button type="submit"><i class="fa-solid fa-paper-plane"></i></button>
      </form>
    <?php endif; ?>
  </div>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
