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

$buscar = trim($_GET['buscar'] ?? '');

$where = ['activo = 1'];
$params = [];

if ($idCategoria > 0) {
    $where[] = 'id_categoria = :cat';
    $params[':cat'] = $idCategoria;
}

if ($buscar !== '') {
    $where[] = '(nombre LIKE :b1 OR descripcion LIKE :b2 OR sku LIKE :b3)';
    $params[':b1'] = '%' . $buscar . '%';
    $params[':b2'] = '%' . $buscar . '%';
    $params[':b3'] = '%' . $buscar . '%';
}

$whereSql = implode(' AND ', $where);
$stmt = $pdo->prepare("SELECT * FROM productos WHERE $whereSql ORDER BY $ordenSql");
$stmt->execute($params);
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

// Función auxiliar para no perder la categoría ni la búsqueda al cambiar el orden
function urlCatalogo(int $idCategoria, string $orden, string $buscar = ''): string {
    $params = [];
    if ($idCategoria > 0) { $params['categoria'] = $idCategoria; }
    if ($orden !== 'relevancia') { $params['orden'] = $orden; }
    if ($buscar !== '') { $params['buscar'] = $buscar; }
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
            <a href="productos.php<?= $orden !== 'relevancia' ? '?orden=' . $orden : ''; ?><?= $buscar !== '' ? ($orden !== 'relevancia' ? '&' : '?') . 'buscar=' . urlencode($buscar) : ''; ?>"
               class="<?= $idCategoria === 0 ? 'activo' : ''; ?>">
              Todas
              <span class="catalogo__conteo"><?= array_sum($conteoPorCategoria); ?></span>
            </a>
          </li>
          <?php foreach ($categorias as $c): ?>
            <li>
              <a href="productos.php?categoria=<?= $c['id_categoria']; ?><?= $orden !== 'relevancia' ? '&orden=' . $orden : ''; ?><?= $buscar !== '' ? '&buscar=' . urlencode($buscar) : ''; ?>"
                 class="<?= $idCategoria === (int)$c['id_categoria'] ? 'activo' : ''; ?>">
                <?= htmlspecialchars($c['nombre']); ?>
                <span class="catalogo__conteo"><?= $conteoPorCategoria[$c['id_categoria']] ?? 0; ?></span>
              </a>
            </li>
          <?php endforeach; ?>
        </ul>

        <button type="button" class="catalogo__ayuda" onclick="abrirModalAsesoria()" title="Solicitar asesoría para encontrar un equipo">
          <i class="fa-solid fa-headset"></i>
          <span>¿No encuentras lo que buscas? <strong>Te asesoramos</strong></span>
        </button>
      </aside>

      <!-- ============ GRID DE PRODUCTOS ============ -->
      <div class="catalogo__productos">
        <div class="catalogo__barra">
          <!-- Buscador en tiempo real / por término -->
          <form method="GET" action="productos.php" class="catalogo__buscador">
            <?php if ($idCategoria > 0): ?><input type="hidden" name="categoria" value="<?= $idCategoria; ?>"><?php endif; ?>
            <?php if ($orden !== 'relevancia'): ?><input type="hidden" name="orden" value="<?= htmlspecialchars($orden); ?>"><?php endif; ?>
            <i class="fa-solid fa-magnifying-glass"></i>
            <input type="text" name="buscar" value="<?= htmlspecialchars($buscar); ?>" placeholder="Buscar cámaras, alarmas, sensores, accesorios...">
            <?php if ($buscar !== ''): ?>
              <a href="productos.php<?= $idCategoria > 0 ? '?categoria=' . $idCategoria : ''; ?>" class="catalogo__limpiar-busqueda" title="Quitar búsqueda">&times;</a>
            <?php endif; ?>
            <button type="submit">Buscar</button>
          </form>

          <div class="catalogo__barra-derecha">
            <p class="catalogo__resultado">
              <strong><?= count($productos); ?></strong>
              <?= count($productos) === 1 ? 'producto' : 'productos'; ?>
              <?php if ($buscar !== ''): ?>
                para "<strong><?= htmlspecialchars($buscar); ?></strong>"
              <?php endif; ?>
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
                  <option value="<?= urlCatalogo($idCategoria, $clave, $buscar); ?>" <?= $orden === $clave ? 'selected' : ''; ?>>
                    <?= $etiqueta; ?>
                  </option>
                <?php endforeach; ?>
              </select>
            </label>
          </div>
        </div>

        <div class="productos__grid">
          <?php if (empty($productos)): ?>
            <div class="catalogo__vacio">
              <i class="fa-solid fa-box-open"></i>
              <h3><?= $buscar !== '' ? 'No encontramos productos que coincidan con "' . htmlspecialchars($buscar) . '"' : 'No hay productos en esta categoría'; ?></h3>
              <p>¿Buscas una referencia especial, otra marca o repuesto técnico? Nuestro equipo te lo cotiza e instala.</p>
              <div class="catalogo__vacio-acciones">
                <button type="button" class="btn btn--primario" onclick="abrirModalAsesoria('<?= !empty($buscar) ? 'Hola, estaba buscando en el catálogo: ' . addslashes(htmlspecialchars($buscar)) : ''; ?>')">
                  <i class="fa-solid fa-headset"></i> Solicitar este producto a un asesor
                </button>
                <a href="productos.php" class="btn btn--secundario">Ver todo el catálogo</a>
              </div>
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

<!-- ============ MODAL ASESORÍA / NO ENCUENTRAS LO QUE BUSCAS ============ -->
<div class="modal-asesoria" id="modalAsesoria" role="dialog" aria-modal="true" aria-labelledby="tituloModalAsesoria">
  <div class="modal-asesoria__caja">
    <button type="button" class="modal-asesoria__cerrar" id="cerrarModalAsesoria" onclick="cerrarModalAsesoria()" aria-label="Cerrar ventana">&times;</button>
    
    <div class="modal-asesoria__header">
      <div class="modal-asesoria__icono">
        <i class="fa-solid fa-headset"></i>
      </div>
      <div>
        <h3 id="tituloModalAsesoria">¿No encuentras lo que buscas?</h3>
        <p>Nuestro equipo técnico te ayuda a conseguir cualquier equipo o repuesto con cotización inmediata y garantía oficial.</p>
      </div>
    </div>

    <form id="formAsesoriaProducto" class="modal-asesoria__form">
      <input type="hidden" name="tipo" value="mensaje">
      <input type="hidden" name="asunto" value="Consulta de producto especial (No encontrado en catálogo)">

      <div class="campo">
        <label for="asesoriaNombre">Nombre completo *</label>
        <input type="text" id="asesoriaNombre" name="nombre" placeholder="Tu nombre" required 
               value="<?= htmlspecialchars($datosCliente['nombre'] ?? ''); ?>">
      </div>

      <div class="modal-asesoria__fila">
        <div class="campo">
          <label for="asesoriaTelefono">Teléfono o WhatsApp *</label>
          <input type="tel" id="asesoriaTelefono" name="telefono" placeholder="Ej. 300 123 4567" required
                 value="<?= htmlspecialchars($datosCliente['telefono'] ?? ''); ?>">
        </div>
        <div class="campo">
          <label for="asesoriaCorreo">Correo electrónico *</label>
          <input type="email" id="asesoriaCorreo" name="correo" placeholder="correo@ejemplo.com" required
                 value="<?= htmlspecialchars($datosCliente['correo'] ?? ''); ?>">
        </div>
      </div>

      <div class="campo">
        <label for="asesoriaProducto">¿Qué producto, referencia o repuesto necesitas? *</label>
        <textarea id="asesoriaProducto" name="mensaje" rows="3" required
                  placeholder="Cuéntanos el modelo, marca, características técnicas o el tipo de solución que necesitas (ej. Cámara domo térmica para bodega exterior, sensor de barrera fotoeléctrica, NVR 32 canales)..."><?= !empty($buscar) ? 'Hola, estaba buscando en el catálogo el equipo "' . htmlspecialchars($buscar) . '" y no lo encontré. ¿Me pueden ayudar a cotizarlo?' : ''; ?></textarea>
      </div>

      <div class="modal-asesoria__acciones">
        <button type="submit" class="btn btn--primario">
          <i class="fa-solid fa-paper-plane"></i> Enviar solicitud
        </button>
        <a href="https://wa.me/573001234567?text=<?= urlencode('Hola TECNISECURE, estoy buscando un producto que no veo en el catálogo y necesito asesoría técnica.'); ?>"
           target="_blank" class="btn-whatsapp-asesoria" title="Chatear directamente por WhatsApp">
          <i class="fa-brands fa-whatsapp"></i> Hablar por WhatsApp
        </a>
      </div>

      <p class="form-respuesta" id="respuestaAsesoria"></p>
    </form>
  </div>
</div>

<?php require_once __DIR__ . '/../includes/footer.php'; ?>
