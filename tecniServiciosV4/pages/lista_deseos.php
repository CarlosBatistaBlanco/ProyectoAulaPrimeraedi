<?php
/**
 * pages/lista_deseos.php
 * -----------------------------------------------------------------
 * Apartado dedicado para la "Lista de Compras" / Favoritos.
 * Permite al usuario guardar productos que le gusten con el icono
 * de corazón, ver sus fotos ampliadas, comparar precios y
 * descuentos, y transferirlos al carrito de compras con un solo clic.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../php/conexion.php';
require_once __DIR__ . '/../php/auth.php';

$tituloPagina = 'Mi Lista de Compras | TECNISECURE';
$rutaBase = '../';

$clienteSesion = usuarioActual();
$datosCliente = null;
if ($clienteSesion && $clienteSesion['rol'] === 'cliente') {
    $stmtC = $pdo->prepare('SELECT * FROM clientes WHERE id_usuario = :id LIMIT 1');
    $stmtC->execute([':id' => $clienteSesion['id_usuario']]);
    $datosCliente = $stmtC->fetch();
}

require_once __DIR__ . '/../includes/header.php';
?>

<main>
  <!-- Cabecera de la sección -->
  <section class="pagina-header">
    <div class="contenedor">
      <p class="seccion__eyebrow"><i class="fa-solid fa-heart" style="color: #ff3366;"></i> Selección Personal</p>
      <h1 class="seccion__titulo">Mi Lista de Compras</h1>
      <p class="seccion__descripcion">
        Tus productos favoritos guardados. Revisa especificaciones, aprovecha descuentos activos y pásalos a tu carrito cuando estés listo para comprar.
      </p>
    </div>
  </section>

  <section class="deseos-seccion">
    <div class="contenedor">

      <!-- Barra de acciones superiores -->
      <div class="deseos-barra-acciones" id="listaDeseosAcciones">
        <div class="deseos-conteo">
          <i class="fa-solid fa-bookmark"></i>
          <span>Tienes <strong id="listaDeseosTotal">0</strong> producto(s) en tu lista</span>
        </div>
        <div class="deseos-botones">
          <button type="button" class="btn btn--primario" onclick="moverTodoAlCarrito()">
            <i class="fa-solid fa-cart-arrow-down"></i> Mover todo al carrito
          </button>
          <button type="button" class="btn btn--secundario" onclick="vaciarListaDeseos()">
            <i class="fa-solid fa-trash-can"></i> Vaciar lista
          </button>
        </div>
      </div>

      <!-- Cuadrícula donde se muestran los productos guardados -->
      <div class="productos__grid deseos__grid" id="listaDeseosGrid">
        <!-- Renderizado dinámico vía js/deseos.js -->
      </div>

      <!-- Estado vacío (cuando no hay productos en la lista) -->
      <div class="deseos-vacio" id="listaDeseosVacia" style="display: none;">
        <div class="deseos-vacio__icono">
          <i class="fa-regular fa-heart"></i>
        </div>
        <h3>Tu lista de compras está vacía</h3>
        <p>Aún no has agregado ningún producto. Haz clic en el ícono de corazón <i class="fa-solid fa-heart" style="color:#ff3366;"></i> en cualquier producto del catálogo para guardarlo aquí.</p>
        <div class="deseos-vacio__acciones">
          <a href="productos.php" class="btn btn--primario">
            <i class="fa-solid fa-box-open"></i> Explorar Catálogo de Productos
          </a>
        </div>
      </div>

    </div>
  </section>
</main>

<!-- ============ PANEL LATERAL DEL CARRITO ============ -->
<div class="carrito-overlay" id="carritoOverlay"></div>
<aside class="carrito-panel" id="carritoPanel">
  <div class="carrito-panel__header">
    <h3><i class="fa-solid fa-cart-shopping"></i> Tu Carrito</h3>
    <button id="cerrarCarrito" aria-label="Cerrar carrito">&times;</button>
  </div>

  <div class="carrito-panel__items" id="carritoItems">
    <!-- Los productos se insertan aquí dinámicamente con JS -->
  </div>

  <!-- SECCIÓN: LISTA DE PRODUCTOS DESEADA -->
  <div class="carrito-deseos-seccion" id="carritoDeseosSeccion">
    <button type="button" class="btn-toggle-deseos-carrito" id="btnToggleDeseosCarrito" onclick="toggleDeseosCarrito()" aria-expanded="false">
      <span class="btn-toggle-deseos-carrito__texto">
        <i class="fa-solid fa-heart"></i>
        <span id="textoToggleDeseos">Mostrar lista de productos deseada</span>
        <span class="deseos-chip-contador" id="carritoDeseosCount">0</span>
      </span>
      <i class="fa-solid fa-chevron-down flecha-deseos" id="flechaDeseosCarrito"></i>
    </button>
    <div class="carrito-deseos-contenedor" id="carritoDeseosContenedor" style="display: none;">
      <div class="carrito-deseos-lista" id="carritoDeseosLista">
        <!-- Renderizado dinámico desde JS con los productos deseados -->
      </div>
    </div>
  </div>

  <div class="carrito-panel__resumen">
    <div class="carrito-total">
      <span>Total:</span>
      <span id="carritoTotal">$0</span>
    </div>

    <form id="formCheckout" class="checkout-form">
      <input type="text" name="nombre" placeholder="Nombre completo" required
             value="<?= htmlspecialchars($datosCliente['nombre'] ?? ''); ?>" <?= $datosCliente ? 'readonly' : ''; ?>>
      <input type="email" name="correo" placeholder="Correo electrónico" required
             value="<?= htmlspecialchars($datosCliente['correo'] ?? ''); ?>" <?= $datosCliente ? 'readonly' : ''; ?>>
      <input type="tel" name="telefono" placeholder="Teléfono" value="<?= htmlspecialchars($datosCliente['telefono'] ?? ''); ?>">
      <input type="text" name="direccion" placeholder="Dirección de instalación" value="<?= htmlspecialchars($datosCliente['direccion'] ?? ''); ?>">
      <button type="submit" class="btn btn--primario btn--full">
        <i class="fa-solid fa-lock"></i> Confirmar pedido
      </button>
    </form>
    <p class="form-respuesta" id="respuestaPedido"></p>
  </div>
</aside>

<?php require_once __DIR__ . '/../includes/footer.php'; ?>
