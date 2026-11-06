<?php
/**
 * php/marcar_notificaciones.php
 * -----------------------------------------------------------------
 * Marca todas las notificaciones del usuario actual como leídas y regresa a la página anterior. Se llama desde el enlace 'Marcar todas como leídas' de la campana de notificaciones.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirLogin();

$pdo->prepare('UPDATE notificaciones SET leida = 1 WHERE id_usuario = :id')->execute([':id' => $u['id_usuario']]);

$referer = $_SERVER['HTTP_REFERER'] ?? '../panel/' . $u['rol'] . '/index.php';
header('Location: ' . $referer);
exit;
