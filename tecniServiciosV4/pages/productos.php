<?php
/**
 * pages/productos.php
 * -----------------------------------------------------------------
 * Catálogo completo de productos (leído desde MySQL) + carrito
 * de compras lateral. El carrito se maneja en el navegador con
 * localStorage (js/carrito.js) y al finalizar la compra se envía
 * por fetch() a php/procesar_pedido.php para guardarlo en la BD.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/../php/conexion.php';
require_once __DIR__ . '/../php/auth.php';

$tituloPagina = 'Productos | TECNISECURE';
$rutaBase = '../'; // estamos dentro de /pages, hay que subir un nivel

// Filtro por categoría (?categoria=2) y orden del listado (?orden=precio_asc)
$idCategoria = isset($_GET['categoria']) ? (int)$_GET['categoria'] : 0;

$categorias = $pdo->query('SELECT * FROM categorias ORDER BY nombre')->fetchAll();

// Órdenes permitidos. La clave llega por la URL; el valor es el SQL seguro
// (lista blanca: nunca se concatena texto del usuario en la consulta).
$ordenes = [
    'relevancia' => 'destacado DESC, nombre ASC',
    'precio_asc' => 'precio ASC',
    'precio_desc' => 'precio DESC',
    'nombre' => 'nombre ASC',
    'nuevos' => 'creado_en DESC',
];
$orden = isset($_GET['orden']) && isset($ordenes[$_GET['orden']]) ? $_GET['orden'] : 'relevancia';
$ordenSql = $ordenes[$orden];

if ($idCategoria > 0) {
    $stmt = $pdo->prepare("SELECT * FROM productos WHERE activo = 1 AND id_categoria = :cat ORDER BY $ordenSql");
    $stmt->execute([':cat' => $idCategoria]);
} else {
    $stmt = $pdo->query("SELECT * FROM productos WHERE activo = 1 ORDER BY $ordenSql");
}
$productos = $stmt->fetchAll();

// Marca de cada producto (para mostrarla en la tarjeta)
$marcasPorId = [];
try {
    foreach ($pdo->query('SELECT id_marca, nombre FROM marcas') as $m) {
        $marcasPorId[$m['id_marca']] = $m['nombre'];
    }
} catch (PDOException $e) {
    $marcasPorId = [];
}

// Cantidad de productos por categoría (para el contador del filtro)
$conteoPorCategoria = [];
try {
    foreach ($pdo->query('SELECT id_categoria, COUNT(*) AS total FROM productos WHERE activo = 1 GROUP BY id_categoria') as $fila) {
        $conteoPorCategoria[$fila['id_categoria']] = (int)$fila['total'];
    }
} catch (PDOException $e) {
    $conteoPorCategoria = [];
}

// Función auxiliar para no perder la categoría al cambiar el orden
function urlCatalogo(int $idCategoria, string $orden): string {
    $params = [];
    if ($idCategoria > 0) { $params['categoria'] = $idCategoria; }
    if ($orden !== 'relevancia') { $params['orden'] = $orden; }
    return 'productos.php' . ($params ? '?' . http_build_query($params) : '');
}

// Favoritos del cliente actual (si inició sesión) y calificación promedio de cada producto
// Envueltas en try/catch a propósito: si por alguna razón la base de datos no está
// 100% actualizada (por ejemplo, una importación anterior incompleta), estas dos
// funciones "extra" simplemente se desactivan solas en vez de tumbar toda la página.
$clienteSesion = usuarioActual();
$favoritosCliente = [];
if ($clienteSesion && $clienteSesion['rol'] === 'cliente') {
    try {
        $stmtFav = $pdo->prepare('SELECT id_producto FROM favoritos WHERE id_usuario = :u');
        $stmtFav->execute([':u' => $clienteSesion['id_usuario']]);
        $favoritosCliente = $stmtFav->fetchAll(PDO::FETCH_COLUMN);
    } catch (PDOException $e) {
        $favoritosCliente = [];
    }
}
$calificacionesPorProducto = [];
try {
    $calificaciones = $pdo->query('SELECT id_producto, ROUND(AVG(calificacion),1) AS promedio, COUNT(*) AS total
                                    FROM resenas GROUP BY id_producto')->fetchAll(PDO::FETCH_ASSOC);
    foreach ($calificaciones as $c) { $calificacionesPorProducto[$c['id_producto']] = $c; }
} catch (PDOException $e) {
    $calificacionesPorProducto = [];
}
$datosCliente = null;
if ($clienteSesion && $clienteSesion['rol'] === 'cliente') {
    $stmtC = $pdo->prepare('SELECT * FROM clientes WHERE id_usuario = :id LIMIT 1');
    $stmtC->execute([':id' => $clienteSesion['id_usuario']]);
    $datosCliente = $stmtC->fetch();
}

require_once __DIR__ . '/../includes/header.php';
?>

<main>
  <section class="pagina-header">
    <div class="contenedor">
      <p class="seccion__eyebrow">Catálogo</p>
      <h1 class="seccion__titulo">Nuestros Productos</h1>
      <p class="seccion__descripcion">Equipos de seguridad de alta calidad, listos para instalar. Agrega al carrito y solicita tu pedido en línea.</p>
    </div>
  </section>

  <section class="catalogo">
    <div class="contenedor catalogo__layout">

      <!-- ============ FILTROS ============ -->
      <aside class="catalogo__filtros">
        <h3>Categorías</h3>
        <ul>
          <li>
            <a href="productos.php<?= $orden !== 'relevancia' ? '?orden=' . $orden : ''; ?>"
               class="<?= $idCategoria === 0 ? 'activo' : ''; ?>">
              Todas
              <span class="catalogo__conteo"><?= array_sum($conteoPorCategoria); ?></span>
            </a>
          </li>
          <?php foreach ($categorias as $c): ?>
            <li>
              <a href="productos.php?categoria=<?= $c['id_categoria']; ?><?= $orden !== 'relevancia' ? '&orden=' . $orden : ''; ?>"
                 class="<?= $idCategoria === (int)$c['id_categoria'] ? 'activo' : ''; ?>">
                <?= htmlspecialchars($c['nombre']); ?>
                <span class="catalogo__conteo"><?= $conteoPorCategoria[$c['id_categoria']] ?? 0; ?></span>
              </a>
            </li>
          <?php endforeach; ?>
        </ul>

        <a href="index.php#contacto" class="catalogo__ayuda">
          <i class="fa-solid fa-headset"></i>
          ¿No encuentras lo que buscas? Te asesoramos
        </a>
      </aside>

      <!-- ============ GRID DE PRODUCTOS ============ -->
      <div class="catalogo__productos">
        <div class="catalogo__barra">
          <p class="catalogo__resultado">
            <strong><?= count($productos); ?></strong>
            <?= count($productos) === 1 ? 'producto' : 'productos'; ?>
            <?php if ($idCategoria > 0): $catActual = array_values(array_filter($categorias, fn($c) => (int)$c['id_categoria'] === $idCategoria))[0] ?? null; ?>
              en <strong><?= htmlspecialchars($catActual['nombre'] ?? ''); ?></strong>
            <?php endif; ?>
          </p>

          <label class="catalogo__orden">
            <span>Ordenar por</span>
            <select onchange="location.href=this.value">
              <?php foreach ([
                  'relevancia'  => 'Destacados primero',
                  'precio_asc'  => 'Precio: menor a mayor',
                  'precio_desc' => 'Precio: mayor a menor',
                  'nombre'      => 'Nombre (A-Z)',
                  'nuevos'      => 'Más recientes',
              ] as $clave => $etiqueta): ?>
                <option value="<?= urlCatalogo($idCategoria, $clave); ?>" <?= $orden === $clave ? 'selected' : ''; ?>>
                  <?= $etiqueta; ?>
                </option>
              <?php endforeach; ?>
            </select>
          </label>
        </div>

        <div class="productos__grid">
          <?php if (empty($productos)): ?>
            <div class="catalogo__vacio">
              <i class="fa-solid fa-box-open"></i>
              <h3>No hay productos en esta categoría</h3>
              <p>Estamos ampliando nuestro catálogo. Cuéntanos qué necesitas y te ayudamos a conseguirlo.</p>
              <a href="productos.php" class="btn btn--secundario">Ver todos los productos</a>
            </div>
          <?php endif; ?>

          <?php foreach ($productos as $p): 
            $tieneDesc = !empty($p['descuento_pct']) && (float)$p['descuento_pct'] > 0;
            $precioOrig = (float)$p['precio'];
            $descPct = (float)$p['descuento_pct'];
            $precioFinal = $tieneDesc ? round($precioOrig * (1 - ($descPct / 100))) : $precioOrig;
            $marcaNombre = $marcasPorId[$p['id_marca']] ?? '';
          ?>
            <article class="tarjeta-producto">
              <div class="tarjeta-producto__imagen">
                <?php if ($tieneDesc): ?>
                  <span class="badge-rebaja">-<?= (int)$descPct; ?>% OFF</span>
                <?php elseif ($p['destacado']): ?>
                  <span class="badge">Destacado</span>
                <?php endif; ?>

                <button type="button" class="btn-favorito <?= in_array($p['id_producto'], $favoritosCliente) ? 'activo' : ''; ?>"
                        data-id="<?= $p['id_producto']; ?>"
                        data-nombre="<?= htmlspecialchars($p['nombre']); ?>"
                        data-precio="<?= $precioFinal; ?>"
                        data-precio-original="<?= $precioOrig; ?>"
                        data-descuento="<?= (int)$descPct; ?>"
                        data-imagen="../<?= htmlspecialchars($p['imagen']); ?>"
                        data-marca="<?= htmlspecialchars($marcaNombre); ?>"
                        title="Guardar en mi lista de compras">
                  <i class="fa-solid fa-heart"></i>
                </button>

                <img src="../<?= htmlspecialchars($p['imagen']); ?>" alt="<?= htmlspecialchars($p['nombre']); ?>"
                     loading="lazy" onerror="this.src='../img/productos/sin-imagen.jpg'">
                <span class="tarjeta-producto__zoom-hint"><i class="fa-solid fa-magnifying-glass-plus"></i> Ampliar</span>
              </div>
              <div class="tarjeta-producto__info">
                <?php if (!empty($marcaNombre)): ?>
                  <span class="tarjeta-producto__marca"><?= htmlspecialchars($marcaNombre); ?></span>
                <?php endif; ?>
                <h3><?= htmlspecialchars($p['nombre']); ?></h3>
                <?php if (isset($calificacionesPorProducto[$p['id_producto']])): $cal = $calificacionesPorProducto[$p['id_producto']]; ?>
                  <p class="tarjeta-producto__rating">
                    <?php for ($i = 1; $i <= 5; $i++): ?>
                      <i class="fa-solid fa-star" style="color:<?= $i <= round($cal['promedio']) ? '#ffb300' : '#3a4a63'; ?>;"></i>
                    <?php endfor; ?>
                    <span><?= $cal['promedio']; ?> (<?= $cal['total']; ?>)</span>
                  </p>
                <?php endif; ?>
                <p class="tarjeta-producto__desc"><?= htmlspecialchars($p['descripcion']); ?></p>

                <?php if ($p['garantia_meses'] > 0): ?>
                  <p class="tarjeta-producto__garantia">
                    <i class="fa-solid fa-shield-halved"></i> Garantía <?= (int)$p['garantia_meses']; ?> meses
                  </p>
                <?php endif; ?>

                <div class="tarjeta-producto__footer">
                  <div class="precio-contenedor">
                    <?php if ($tieneDesc): ?>
                      <div class="precio-contenedor__fila">
                        <span class="precio-original">$<?= number_format($precioOrig, 0, ',', '.'); ?></span>
                        <span class="badge-descuento">-<?= (int)$descPct; ?>%</span>
                      </div>
                    <?php endif; ?>
                    <span class="precio <?= $tieneDesc ? 'precio--con-descuento' : ''; ?>">
                      $<?= number_format($precioFinal, 0, ',', '.'); ?>
                    </span>
                  </div>
                  <button class="btn-agregar" data-id="<?= $p['id_producto']; ?>" data-nombre="<?= htmlspecialchars($p['nombre']); ?>"
                          data-precio="<?= $precioFinal; ?>" data-imagen="../<?= htmlspecialchars($p['imagen']); ?>">
                    <i class="fa-solid fa-cart-plus"></i> Agregar
                  </button>
                </div>
                <?php if ($p['stock'] > 0 && $p['stock'] <= $p['stock_minimo']): ?>
                  <p class="stock-bajo"><i class="fa-solid fa-triangle-exclamation"></i> ¡Últimas <?= $p['stock']; ?> unidades!</p>
                <?php elseif ($p['stock'] <= 0): ?>
                  <p class="stock-agotado"><i class="fa-solid fa-ban"></i> Agotado temporalmente</p>
                <?php endif; ?>
              </div>
            </article>
          <?php endforeach; ?>
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
