<?php
/**
 * includes/auth_header.php
 * -----------------------------------------------------------------
 * Encabezado HTML compartido por las páginas de autenticación (login, registro, recuperar, restablecer): carga los estilos y abre la tarjeta central.
 * -----------------------------------------------------------------
 */
$rutaBase = $rutaBase ?? ''; ?>
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><?= $tituloPagina ?? 'Acceso | TECNISECURE'; ?></title>
<link rel="icon" href="<?= $rutaBase ?>img/logo/logo.svg" type="image/svg+xml">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
<link rel="stylesheet" href="<?= $rutaBase ?>css/style.css?v=<?= @filemtime(__DIR__ . '/../css/style.css') ?: '1'; ?>">
<link rel="stylesheet" href="<?= $rutaBase ?>css/auth.css?v=<?= @filemtime(__DIR__ . '/../css/auth.css') ?: '1'; ?>">
</head>
<body class="auth-body">
<div class="auth-wrapper">
  <div style="display:flex; justify-content:center; align-items:center; gap:14px; margin-bottom:22px;">
    <a href="<?= $rutaBase ?>index.php" class="auth-logo logo" style="margin-bottom:0;">
      <img src="<?= $rutaBase ?>img/logo/logo-claro.svg" alt="TECNISECURE" class="logo__claro">
      <img src="<?= $rutaBase ?>img/logo/logo.svg" alt="TECNISECURE" class="logo__oscuro">
    </a>
    <button type="button" class="btn-tema" title="Cambiar tema claro/oscuro">
      <i class="fa-solid fa-moon"></i>
    </button>
  </div>
  <div class="auth-card">