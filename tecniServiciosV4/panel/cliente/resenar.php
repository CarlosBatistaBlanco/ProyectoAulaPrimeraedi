<?php
/**
 * panel/cliente/resenar.php
 * -----------------------------------------------------------------
 * Formulario para que el cliente califique (1 a 5 estrellas) y comente un producto que ya compró. Verifica la compra antes de aceptar la reseña.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../../php/auth.php';
$u = requerirRol(['cliente']);
$tituloPagina = 'Calificar Producto';

$idProducto = (int)($_GET['producto'] ?? 0);
$stmt = $pdo->prepare('SELECT * FROM productos WHERE id_producto = :id');
$stmt->execute([':id' => $idProducto]);
$producto = $stmt->fetch();
if (!$producto) { header('Location: pedidos.php'); exit; }

// Verifica que el cliente realmente haya comprado este producto
$stmt = $pdo->prepare("SELECT COUNT(*) FROM pedido_detalle pd
    JOIN pedidos p ON p.id_pedido = pd.id_pedido
    JOIN clientes c ON c.id_cliente = p.id_cliente
    WHERE c.id_usuario = :u AND pd.id_producto = :prod");
$stmt->execute([':u' => $u['id_usuario'], ':prod' => $idProducto]);
$compro = $stmt->fetchColumn() > 0;

$miResena = $pdo->prepare('SELECT * FROM resenas WHERE id_producto = :p AND id_usuario = :u');
$miResena->execute([':p' => $idProducto, ':u' => $u['id_usuario']]);
$miResena = $miResena->fetch();

require_once __DIR__ . '/../../includes/panel_header.php';
?>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-star"></i> Calificar: <?= htmlspecialchars($producto['nombre']); ?></h2>

  <?php if (!$compro): ?>
    <p style="color:var(--texto-tenue);">Solo puedes calificar productos que hayas comprado.</p>
  <?php else: ?>
    <?php if (!empty($_GET['exito'])): ?><div class="auth-alerta auth-alerta--ok"><?= htmlspecialchars($_GET['exito']); ?></div><?php endif; ?>

    <form action="../../php/procesar_resena.php" method="POST" style="max-width:480px;">
      <?= csrfCampoOculto(); ?>
      <input type="hidden" name="id_producto" value="<?= $idProducto; ?>">

      <div class="campo" style="margin-bottom:16px;">
        <label>Tu calificación</label>
        <div class="estrellas-selector" id="estrellasSelector">
          <?php for ($i = 1; $i <= 5; $i++): ?>
            <i class="fa-solid fa-star" data-valor="<?= $i; ?>" style="color:<?= ($miResena && $i <= $miResena['calificacion']) ? '#ffb300' : '#3a4a63'; ?>;"></i>
          <?php endfor; ?>
        </div>
        <input type="hidden" name="calificacion" id="calificacionInput" value="<?= $miResena['calificacion'] ?? 5; ?>" required>
      </div>

      <div class="campo" style="margin-bottom:16px;">
        <label>Comentario (opcional)</label>
        <textarea name="comentario" rows="4" style="width:100%; background:var(--azul-oscuro); border:1px solid var(--azul-acero); color:var(--texto); padding:10px; border-radius:8px;"><?= htmlspecialchars($miResena['comentario'] ?? ''); ?></textarea>
      </div>

      <button type="submit" class="btn btn--primario"><i class="fa-solid fa-check"></i> <?= $miResena ? 'Actualizar reseña' : 'Enviar reseña'; ?></button>
    </form>

    <script>
      document.querySelectorAll('#estrellasSelector i').forEach(estrella => {
        estrella.addEventListener('click', () => {
          const valor = parseInt(estrella.dataset.valor);
          document.getElementById('calificacionInput').value = valor;
          document.querySelectorAll('#estrellasSelector i').forEach(e => {
            e.style.color = parseInt(e.dataset.valor) <= valor ? '#ffb300' : '#3a4a63';
          });
        });
      });
    </script>
  <?php endif; ?>
  <p style="margin-top:18px;"><a href="pedidos.php" style="color:var(--texto-tenue);"><i class="fa-solid fa-arrow-left"></i> Volver a mis pedidos</a></p>
</div>

<?php require_once __DIR__ . '/../../includes/panel_footer.php'; ?>
