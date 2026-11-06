<?php
/**
 * php/admin_empleados.php
 * -----------------------------------------------------------------
 * Backend que actualiza el cargo de un empleado desde panel/administrador/empleados.php.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$admin = requerirRol(['administrador']);

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    header('Location: ../panel/administrador/empleados.php');
    exit;
}

$idUsuario = (int)($_POST['id_usuario'] ?? 0);
$cargo = trim($_POST['cargo'] ?? 'Empleado');

$pdo->prepare('UPDATE empleados SET cargo = :c WHERE id_usuario = :id')->execute([':c' => $cargo, ':id' => $idUsuario]);
registrarLog($admin['id_usuario'], 'actualizar_cargo_empleado', "Usuario #$idUsuario -> $cargo");

header('Location: ../panel/administrador/empleados.php?exito=' . urlencode('Cargo actualizado.'));
exit;
