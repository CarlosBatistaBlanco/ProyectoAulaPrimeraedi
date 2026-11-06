<?php
/**
 * header.php
 * -----------------------------------------------------------------
 * Encabezado común para todo el sitio: <head>, menú de navegación
 * interactivo (hamburguesa en móvil) y logo.
 * Se incluye en cada página con:
 *      require_once __DIR__ . '/includes/header.php';
 *
 * La variable $rutaBase permite que los enlaces funcionen tanto desde
 * index.php (raíz) como desde archivos dentro de /pages.
 * -----------------------------------------------------------------
 */
$rutaBase = $rutaBase ?? ''; // definir $rutaBase = '../' antes de incluir si estás en /pages
require_once __DIR__ . '/../php/auth.php';
$usuarioSesion = usuarioActual();
?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><?= $tituloPagina ?? 'TECNISECURE | Seguridad Electrónica y Cámaras'; ?></title>
<meta name="description" content="TECNISECURE: instalación de cámaras de seguridad, alarmas, control de acceso y monitoreo 24/7.">
<meta name="csrf-token" content="<?= htmlspecialchars(csrfToken()); ?>">
<link rel="icon" href="<?= $rutaBase ?>img/logo/logo.svg" type="image/svg+xml">

<!-- Tipografía: Arial (definida en css/style.css, no requiere carga externa) -->

<!-- Iconos -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">

<!-- ?v=filemtime fuerza a recargar el CSS cuando cambia, evitando que
     el navegador use una versión vieja en caché (logos duplicados, etc.) -->
<link rel="stylesheet" href="<?= $rutaBase ?>css/style.css?v=<?= @filemtime(__DIR__ . '/../css/style.css') ?: '1'; ?>">
<link rel="stylesheet" href="<?= $rutaBase ?>css/auth.css?v=<?= @filemtime(__DIR__ . '/../css/auth.css') ?: '1'; ?>">
</head>
<body>
<?php require __DIR__ . '/banner_suplantacion.php'; ?>

<!-- ============== BARRA DE TOP (contacto rápido) ============== -->
<div class="top-bar">
  <div class="contenedor top-bar__inner">
    <div class="top-bar__contacto">
      <span><i class="fa-solid fa-phone"></i> +57 300 123 4567</span>
      <span><i class="fa-solid fa-envelope"></i> contacto@tecnicsecure.com</span>
    </div>
    <div class="top-bar__redes">
      <a href="https://facebook.com" target="_blank" aria-label="Facebook"><i class="fa-brands fa-facebook"></i></a>
      <a href="https://instagram.com" target="_blank" aria-label="Instagram"><i class="fa-brands fa-instagram"></i></a>
      <a href="https://wa.me/573001234567" target="_blank" aria-label="WhatsApp"><i class="fa-brands fa-whatsapp"></i></a>
      <a href="https://tiktok.com" target="_blank" aria-label="TikTok"><i class="fa-brands fa-tiktok"></i></a>
    </div>
  </div>
</div>

<!-- ============== NAV PRINCIPAL ============== -->
<header class="nav-principal" id="navPrincipal">
  <div class="contenedor nav-principal__inner">
    <a href="<?= $rutaBase ?>index.php#inicio" class="logo">
      <img src="<?= $rutaBase ?>img/logo/logo-claro.svg" alt="Logo TECNISECURE" class="logo__claro">
      <img src="<?= $rutaBase ?>img/logo/logo.svg" alt="Logo TECNISECURE" class="logo__oscuro">
    </a>

    <!-- Botón hamburguesa (solo visible en móvil) -->
    <button class="menu-toggle" id="menuToggle" aria-label="Abrir menú" aria-expanded="false">
      <span></span><span></span><span></span>
    </button>

    <nav class="menu" id="menuPrincipal">
      <ul>
        <li><a href="<?= $rutaBase ?>index.php#inicio">Inicio</a></li>
        <li><a href="<?= $rutaBase ?>index.php#servicios">Servicios</a></li>
        <li><a href="<?= $rutaBase ?>pages/productos.php">Productos</a></li>
        <li><a href="<?= $rutaBase ?>pages/lista_deseos.php">Lista de Compras</a></li>
        <li><a href="<?= $rutaBase ?>index.php#nosotros">Nosotros</a></li>
        <li><a href="<?= $rutaBase ?>index.php#contacto">Contacto</a></li>
      </ul>
      <a href="<?= $rutaBase ?>pages/lista_deseos.php" class="btn-deseos" id="btnAbrirDeseos" title="Mi Lista de Compras">
        <i class="fa-solid fa-heart"></i>
        <span class="deseos-contador" id="deseosContador">0</span>
      </a>
      <a href="<?= $rutaBase ?>pages/productos.php" class="btn-carrito" id="btnAbrirCarrito" title="Ver carrito">
        <i class="fa-solid fa-cart-shopping"></i>
        <span class="carrito-contador" id="carritoContador">0</span>
      </a>

      <button type="button" class="btn-tema" title="Cambiar tema claro/oscuro">
        <i class="fa-solid fa-moon"></i>
      </button>

      <?php if ($usuarioSesion): ?>
        <a href="<?= $rutaBase ?>panel/<?= $usuarioSesion['rol']; ?>/index.php" class="btn-cuenta">
          <i class="fa-solid fa-user"></i> <?= htmlspecialchars(explode(' ', $usuarioSesion['nombre'])[0]); ?>
        </a>
      <?php else: ?>
        <a href="<?= $rutaBase ?>login.php" class="btn-cuenta">
          <i class="fa-solid fa-user"></i> Iniciar sesión
        </a>
      <?php endif; ?>
    </nav>
  </div>
</header>
