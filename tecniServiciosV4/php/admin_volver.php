<?php
/**
 * php/admin_volver.php
 * -----------------------------------------------------------------
 * Termina la suplantación de cuenta y devuelve la sesión al
 * administrador original, sin necesidad de volver a iniciar sesión.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
requerirLogin();

if (estaSuplantando()) {
    $idAdmin = $_SESSION['suplantador']['id_usuario'];
    volverDeSuplantacion();
    registrarLog($idAdmin, 'suplantacion_finalizada', '');
}

header('Location: ../panel/administrador/usuarios.php');
exit;
