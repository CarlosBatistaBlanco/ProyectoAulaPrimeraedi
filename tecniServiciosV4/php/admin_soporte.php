<?php
/**
 * php/admin_soporte.php
 * -----------------------------------------------------------------
 * Backend del panel de Atención al Cliente: cambia el estado de una solicitud de servicio (cotización/cita) o marca un mensaje de contacto como leído.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$admin = requerirRol(['administrador']);

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    header('Location: ../panel/administrador/soporte.php');
    exit;
}

$idSolicitud = (int)($_POST['id_solicitud'] ?? 0);

if (($_POST['accion'] ?? '') === 'marcar_leido_contacto') {
    $idContacto = (int)($_POST['id_contacto'] ?? 0);
    $pdo->prepare('UPDATE contactos SET leido = NOT leido WHERE id_contacto = :id')->execute([':id' => $idContacto]);
    registrarLog($admin['id_usuario'], 'marcar_contacto', "Contacto #$idContacto");
    header('Location: ../panel/administrador/soporte.php?exito=' . urlencode('Actualizado.'));
    exit;
}

$estadosValidos = ['pendiente', 'contactado', 'agendado', 'completado', 'cancelado'];
$estado = in_array($_POST['estado'] ?? '', $estadosValidos, true) ? $_POST['estado'] : 'pendiente';

$stmt = $pdo->prepare('UPDATE solicitudes_servicio SET estado = :e WHERE id_solicitud = :id');
$stmt->execute([':e' => $estado, ':id' => $idSolicitud]);

registrarLog($admin['id_usuario'], 'actualizar_solicitud_servicio', "Solicitud #$idSolicitud -> $estado");

header('Location: ../panel/administrador/soporte.php?exito=' . urlencode('Estado actualizado correctamente.'));
exit;
