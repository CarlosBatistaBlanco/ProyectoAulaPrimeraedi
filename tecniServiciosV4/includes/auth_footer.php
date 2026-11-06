<?php
/**
 * includes/auth_footer.php
 * -----------------------------------------------------------------
 * Pie HTML compartido por las páginas de autenticación: cierra la tarjeta y carga js/auth.js.
 * -----------------------------------------------------------------
 */
$rutaBase = $rutaBase ?? ''; ?>
  </div>
  <p class="auth-volver"><a href="<?= $rutaBase ?>index.php"><i class="fa-solid fa-arrow-left"></i> Volver al sitio</a></p>
</div>
<script src="<?= $rutaBase ?>js/auth.js"></script>
<script src="<?= $rutaBase ?>js/tema.js"></script>
</body>
</html>
