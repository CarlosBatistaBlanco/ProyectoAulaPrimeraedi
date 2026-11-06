<?php
/**
 * logout.php
 * -----------------------------------------------------------------
 * Cierra la sesión del usuario actual (cualquier rol) y lo regresa al login.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/auth.php';
$u = usuarioActual();
if ($u) {
    registrarLog($u['id_usuario'], 'logout', '');
}
cerrarSesionUsuario();
header('Location: login.php?exito=' . urlencode('Sesión cerrada correctamente.'));
exit;
