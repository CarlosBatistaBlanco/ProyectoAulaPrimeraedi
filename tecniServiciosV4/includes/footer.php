<?php
/**
 * includes/footer.php
 * -----------------------------------------------------------------
 * Pie de página del sitio público (no del panel): redes sociales, datos de contacto, y carga de los scripts main.js/carrito.js/auth.js.
 * -----------------------------------------------------------------
 */
$rutaBase = $rutaBase ?? ''; ?>
<!-- ============== FOOTER ============== -->
<footer class="footer" id="contacto-footer">
  <div class="contenedor footer__grid">

    <div class="footer__col">
      <span class="logo">
        <img src="<?= $rutaBase ?>img/logo/logo-claro.svg" alt="TECNISECURE" class="footer__logo logo__claro">
        <img src="<?= $rutaBase ?>img/logo/logo.svg" alt="TECNISECURE" class="footer__logo logo__oscuro">
      </span>
      <p>Más de 10 años protegiendo hogares y negocios con tecnología de vigilancia confiable, monitoreo 24/7 y soporte técnico especializado.</p>
      <div class="footer__redes">
        <a href="https://facebook.com" target="_blank" aria-label="Facebook"><i class="fa-brands fa-facebook"></i></a>
        <a href="https://instagram.com" target="_blank" aria-label="Instagram"><i class="fa-brands fa-instagram"></i></a>
        <a href="https://wa.me/573001234567" target="_blank" aria-label="WhatsApp"><i class="fa-brands fa-whatsapp"></i></a>
        <a href="https://tiktok.com" target="_blank" aria-label="TikTok"><i class="fa-brands fa-tiktok"></i></a>
        <a href="https://youtube.com" target="_blank" aria-label="YouTube"><i class="fa-brands fa-youtube"></i></a>
      </div>
    </div>

    <div class="footer__col">
      <h4>Enlaces</h4>
      <ul>
        <li><a href="<?= $rutaBase ?>index.php#inicio">Inicio</a></li>
        <li><a href="<?= $rutaBase ?>index.php#servicios">Servicios</a></li>
        <li><a href="<?= $rutaBase ?>pages/productos.php">Productos</a></li>
        <li><a href="<?= $rutaBase ?>pages/lista_deseos.php">Lista de Compras</a></li>
        <li><a href="<?= $rutaBase ?>index.php#nosotros">Nosotros</a></li>
        <li><a href="<?= $rutaBase ?>index.php#contacto">Contacto</a></li>
      </ul>
    </div>

    <div class="footer__col">
      <h4>Servicios</h4>
      <ul>
        <li>Instalación de cámaras CCTV</li>
        <li>Monitoreo remoto 24/7</li>
        <li>Alarmas y sensores</li>
        <li>Control de acceso biométrico</li>
        <li>Mantenimiento preventivo</li>
      </ul>
    </div>

    <div class="footer__col">
      <h4>Contáctanos</h4>
      <ul class="footer__contacto">
        <li><i class="fa-solid fa-location-dot"></i> Cra. 10 #20-30, Cartagena, Colombia</li>
        <li><i class="fa-solid fa-phone"></i> +57 300 123 4567</li>
        <li><i class="fa-solid fa-envelope"></i> contacto@tecnicsecure.com</li>
        <li><i class="fa-solid fa-clock"></i> Lun - Sáb: 8:00 am - 6:00 pm</li>
      </ul>
    </div>
  </div>

  <div class="footer__bottom">
    <p>&copy; <?= date('Y'); ?> TECNISECURE. Todos los derechos reservados.</p>
  </div>
</footer>

<!-- Botón flotante de WhatsApp -->
<a href="https://wa.me/573001234567" target="_blank" class="whatsapp-flotante" aria-label="Escríbenos por WhatsApp">
  <i class="fa-brands fa-whatsapp"></i>
</a>

<script src="<?= $rutaBase ?>js/main.js?v=<?= @filemtime(__DIR__ . '/../js/main.js') ?: '1'; ?>"></script>
<script src="<?= $rutaBase ?>js/carrito.js?v=<?= @filemtime(__DIR__ . '/../js/carrito.js') ?: '1'; ?>"></script>
<script src="<?= $rutaBase ?>js/deseos.js?v=<?= @filemtime(__DIR__ . '/../js/deseos.js') ?: '1'; ?>"></script>
<script src="<?= $rutaBase ?>js/auth.js?v=<?= @filemtime(__DIR__ . '/../js/auth.js') ?: '1'; ?>"></script>
<script src="<?= $rutaBase ?>js/tema.js?v=<?= @filemtime(__DIR__ . '/../js/tema.js') ?: '1'; ?>"></script>
</body>
</html>
