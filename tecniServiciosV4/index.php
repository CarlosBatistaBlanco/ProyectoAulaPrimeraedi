<?php
/**
 * index.php - Página principal de TECNISECURE
 * -----------------------------------------------------------------
 * Trae desde MySQL (usando PDO):
 *   - Lista de servicios          -> tabla `servicios`
 *   - Productos destacados        -> tabla `productos` (destacado = 1)
 * y arma la página con includes/header.php + includes/footer.php
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/conexion.php';

$tituloPagina = 'TECNISECURE | Seguridad Electrónica y Cámaras';
$rutaBase = ''; // estamos en la raíz

// Traer servicios ordenados
$servicios = $pdo->query('SELECT * FROM servicios ORDER BY orden ASC')->fetchAll();

// Traer productos destacados (máximo 3 para la vista previa)
$destacados = $pdo->query('SELECT * FROM productos WHERE destacado = 1 AND activo = 1 LIMIT 3')->fetchAll();

// Mapa de íconos de Font Awesome según el campo `icono` guardado en BD
$mapaIconos = [
    'camera'    => 'fa-solid fa-video',
    'monitor'   => 'fa-solid fa-display',
    'bell'      => 'fa-solid fa-bell',
    'lock'      => 'fa-solid fa-fingerprint',
    'wrench'    => 'fa-solid fa-screwdriver-wrench',
    'clipboard' => 'fa-solid fa-clipboard-list',
];

require_once __DIR__ . '/includes/header.php';
?>

<main>
  <!-- ======================= HERO / ENCABEZADO CON VIDEO ======================= -->
  <section class="hero" id="inicio">
    <video class="hero__video" autoplay muted loop playsinline poster="img/servicios/poster-hero.jpg">
      <!-- Video del encabezado: se toma de video/video.mp4 -->
      <source src="video/video.mp4" type="video/mp4">
    </video>

    <!-- Animación de centro de monitoreo (CSS puro, sin archivos externos).
         Se oculta sola cuando el video carga correctamente. -->
    <div class="hero__monitor" aria-hidden="true">
      <div class="monitor-grid">
        <div class="monitor-cam monitor-cam--1"><span class="monitor-scan"></span></div>
        <div class="monitor-cam monitor-cam--2"><span class="monitor-scan"></span></div>
        <div class="monitor-cam monitor-cam--3"><span class="monitor-scan"></span></div>
        <div class="monitor-cam monitor-cam--4"><span class="monitor-scan"></span></div>
      </div>
    </div>

    <div class="hero__overlay"></div>

    <!-- Marco estilo cámara de vigilancia (elemento visual distintivo) -->
    <div class="hero__hud" aria-hidden="true">
      <span class="hud-corner hud-corner--tl"></span>
      <span class="hud-corner hud-corner--tr"></span>
      <span class="hud-corner hud-corner--bl"></span>
      <span class="hud-corner hud-corner--br"></span>
      <div class="hud-rec"><span class="hud-rec__dot"></span> REC</div>
      <div class="hud-clock" id="hudClock">00:00:00</div>
      <div class="hud-tag">CAM 01 · ENTRADA PRINCIPAL</div>
    </div>

    <div class="contenedor hero__contenido">
      <p class="hero__eyebrow">Vigilancia · Alarmas · Control de acceso</p>
      <h1 class="hero__titulo">TECNI<span>SECURE</span></h1>
      <p class="hero__subtitulo">Protegemos lo que más te importa con tecnología de video vigilancia, monitoreo 24/7 y soporte experto en toda la ciudad.</p>
      <div class="hero__botones">
        <a href="#contacto" class="btn btn--primario">Solicitar cotización</a>
        <a href="pages/productos.php" class="btn btn--secundario">Ver productos</a>
      </div>
    </div>
  </section>

  <!-- ======================= BARRA DE CONFIANZA ======================= -->
  <section class="confianza">
    <div class="contenedor confianza__grid">
      <div><i class="fa-solid fa-shield-halved"></i><span>+500 instalaciones</span></div>
      <div><i class="fa-solid fa-headset"></i><span>Soporte 24/7</span></div>
      <div><i class="fa-solid fa-medal"></i><span>Garantía en equipos</span></div>
      <div><i class="fa-solid fa-truck-fast"></i><span>Instalación rápida</span></div>
    </div>
  </section>

  <!-- ======================= SERVICIOS ======================= -->
  <section class="servicios" id="servicios">
    <div class="contenedor">
      <p class="seccion__eyebrow">Lo que hacemos</p>
      <h2 class="seccion__titulo">Nuestros Servicios</h2>
      <p class="seccion__descripcion">Soluciones completas de seguridad electrónica, diseñadas y ejecutadas por técnicos certificados.</p>

      <div class="servicios__grid">
        <?php foreach ($servicios as $s): ?>
          <article class="tarjeta-servicio">
            <div class="tarjeta-servicio__icono">
              <i class="<?= $mapaIconos[$s['icono']] ?? 'fa-solid fa-shield'; ?>"></i>
            </div>
            <h3><?= htmlspecialchars($s['titulo']); ?></h3>
            <p><?= htmlspecialchars($s['descripcion']); ?></p>
            <a href="solicitar-servicio.php?id=<?= $s['id_servicio']; ?>" class="tarjeta-servicio__boton">
              Solicitar este servicio <i class="fa-solid fa-arrow-right"></i>
            </a>
          </article>
        <?php endforeach; ?>
      </div>
    </div>
  </section>

  <!-- ======================= PRODUCTOS DESTACADOS ======================= -->
  <section class="productos-destacados">
    <div class="contenedor">
      <p class="seccion__eyebrow">Equipos recomendados</p>
      <h2 class="seccion__titulo">Productos Destacados</h2>

      <div class="productos__grid">
        <?php foreach ($destacados as $p): 
          $tieneDesc = !empty($p['descuento_pct']) && (float)$p['descuento_pct'] > 0;
          $precioOrig = (float)$p['precio'];
          $descPct = (float)$p['descuento_pct'];
          $precioFinal = $tieneDesc ? round($precioOrig * (1 - ($descPct / 100))) : $precioOrig;
        ?>
          <article class="tarjeta-producto">
            <div class="tarjeta-producto__imagen">
              <?php if ($tieneDesc): ?>
                <span class="badge-rebaja">-<?= (int)$descPct; ?>% OFF</span>
              <?php endif; ?>

              <button type="button" class="btn-favorito"
                      data-id="<?= $p['id_producto']; ?>"
                      data-nombre="<?= htmlspecialchars($p['nombre']); ?>"
                      data-precio="<?= $precioFinal; ?>"
                      data-precio-original="<?= $precioOrig; ?>"
                      data-descuento="<?= (int)$descPct; ?>"
                      data-imagen="<?= htmlspecialchars($p['imagen']); ?>"
                      title="Guardar en mi lista de compras">
                <i class="fa-solid fa-heart"></i>
              </button>

              <img src="<?= htmlspecialchars($p['imagen']); ?>" alt="<?= htmlspecialchars($p['nombre']); ?>"
                   onerror="this.src='img/productos/sin-imagen.jpg'">
              <span class="tarjeta-producto__zoom-hint"><i class="fa-solid fa-magnifying-glass-plus"></i> Ampliar</span>
            </div>
            <div class="tarjeta-producto__info">
              <h3><?= htmlspecialchars($p['nombre']); ?></h3>
              <p class="tarjeta-producto__desc"><?= htmlspecialchars($p['descripcion']); ?></p>
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
                        data-precio="<?= $precioFinal; ?>" data-imagen="<?= htmlspecialchars($p['imagen']); ?>">
                  <i class="fa-solid fa-cart-plus"></i> Agregar
                </button>
              </div>
            </div>
          </article>
        <?php endforeach; ?>
      </div>

      <div class="ver-todos">
        <a href="pages/productos.php" class="btn btn--secundario">Ver catálogo completo</a>
      </div>
    </div>
  </section>

  <!-- ======================= NOSOTROS ======================= -->
  <section class="nosotros" id="nosotros">
    <div class="contenedor nosotros__grid">
      <div class="nosotros__texto">
        <p class="seccion__eyebrow">Quiénes somos</p>
        <h2 class="seccion__titulo">Expertos en Seguridad Electrónica</h2>
        <p>En TECNISECURE diseñamos e instalamos sistemas de seguridad a la medida de cada cliente: hogares, oficinas, locales comerciales e industrias. Nuestro equipo técnico está certificado y disponible para brindar soporte cuando lo necesites.</p>
        <ul class="nosotros__lista">
          <li><i class="fa-solid fa-circle-check"></i> Técnicos certificados</li>
          <li><i class="fa-solid fa-circle-check"></i> Equipos de marcas reconocidas</li>
          <li><i class="fa-solid fa-circle-check"></i> Monitoreo remoto vía app</li>
          <li><i class="fa-solid fa-circle-check"></i> Garantía y mantenimiento</li>
        </ul>
      </div>
      <div class="nosotros__stats">
        <div class="stat"><span class="stat__numero">10+</span><span>Años de experiencia</span></div>
        <div class="stat"><span class="stat__numero">500+</span><span>Clientes satisfechos</span></div>
        <div class="stat"><span class="stat__numero">24/7</span><span>Monitoreo activo</span></div>
        <div class="stat"><span class="stat__numero">100%</span><span>Garantía en instalación</span></div>
      </div>
    </div>
  </section>

  <!-- ======================= CONTACTO ======================= -->
  <section class="contacto" id="contacto">
    <div class="contenedor contacto__grid">
      <div class="contacto__info">
        <p class="seccion__eyebrow">Hablemos</p>
        <h2 class="seccion__titulo">Solicita tu Cotización</h2>
        <p>Cuéntanos qué necesitas y un asesor te contactará el mismo día.</p>

        <ul class="contacto__datos">
          <li><i class="fa-solid fa-location-dot"></i> Cra. 10 #20-30, Cartagena, Colombia</li>
          <li><i class="fa-solid fa-phone"></i> +57 300 123 4567</li>
          <li><i class="fa-solid fa-envelope"></i> contacto@tecnicsecure.com</li>
        </ul>

        <iframe class="contacto__mapa" loading="lazy"
          src="https://www.google.com/maps?q=Cartagena,Colombia&output=embed">
        </iframe>
      </div>

      <form class="contacto__form" id="formContacto" autocomplete="off">
        <h3>Envíanos un mensaje</h3>

        <div class="campo">
          <label for="nombre">Nombre completo</label>
          <input type="text" id="nombre" name="nombre" required placeholder="Ej: Ana Martínez">
        </div>

        <div class="campo">
          <label for="correo">Correo electrónico</label>
          <input type="email" id="correo" name="correo" required placeholder="tucorreo@ejemplo.com">
        </div>

        <div class="campo">
          <label for="telefono">Teléfono</label>
          <input type="tel" id="telefono" name="telefono" placeholder="300 123 4567">
        </div>

        <div class="campo">
          <label for="asunto">Asunto</label>
          <select id="asunto" name="asunto">
            <option value="Cotización de cámaras">Cotización de cámaras</option>
            <option value="Cotización de alarmas">Cotización de alarmas</option>
            <option value="Soporte técnico">Soporte técnico</option>
            <option value="Otro">Otro</option>
          </select>
        </div>

        <div class="campo">
          <label for="tipo">Tipo de mensaje</label>
          <select id="tipo" name="tipo">
            <option value="mensaje">Mensaje general</option>
            <option value="reclamo">Reclamo</option>
            <option value="sugerencia">Sugerencia</option>
          </select>
        </div>

        <div class="campo">
          <label for="mensaje">Mensaje</label>
          <textarea id="mensaje" name="mensaje" rows="4" required placeholder="Cuéntanos qué necesitas..."></textarea>
        </div>

        <button type="submit" class="btn btn--primario btn--full">
          <i class="fa-solid fa-paper-plane"></i> Enviar mensaje
        </button>

        <p class="form-respuesta" id="respuestaContacto"></p>
      </form>
    </div>
  </section>
</main>

<?php require_once __DIR__ . '/includes/footer.php'; ?>
