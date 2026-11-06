<?php
/**
 * admin_usuarios.php
 * -----------------------------------------------------------------
 * Recibe TODAS las acciones del CRUD de usuarios (panel/administrador/usuarios.php).
 * Solo el rol "administrador" puede llegar aquí (requerirRol se encarga).
 * Cada acción queda registrada en logs_actividad para auditoría.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$admin = requerirRol(['administrador']);

function volver(string $mensaje = '', string $tipo = 'exito'): void {
    $qs = $mensaje ? ('?' . $tipo . '=' . urlencode($mensaje)) : '';
    header('Location: ../panel/administrador/usuarios.php' . $qs);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    volver('Solicitud inválida o sesión expirada.', 'error');
}

$accion = $_POST['accion'] ?? '';
$idUsuario = (int)($_POST['id_usuario'] ?? 0);

switch ($accion) {

    case 'crear':
        $nombre = trim($_POST['nombre'] ?? '');
        $correo = trim($_POST['correo'] ?? '');
        $documento = trim($_POST['documento'] ?? '');
        $telefono = trim($_POST['telefono'] ?? '');
        $idRol = (int)($_POST['id_rol'] ?? 0);
        $password = $_POST['password'] ?? '';

        if ($nombre === '' || !filter_var($correo, FILTER_VALIDATE_EMAIL) || $documento === '' || $idRol <= 0) {
            volver('Todos los campos obligatorios deben estar completos.', 'error');
        }
        if (strlen($password) < 8 || !preg_match('/[A-Z]/', $password) || !preg_match('/[0-9]/', $password)) {
            volver('La contraseña temporal debe tener mínimo 8 caracteres, una mayúscula y un número.', 'error');
        }

        $stmt = $pdo->prepare('SELECT id_usuario FROM usuarios WHERE correo = :c OR documento = :d LIMIT 1');
        $stmt->execute([':c' => $correo, ':d' => $documento]);
        if ($stmt->fetch()) {
            volver('Ya existe un usuario con ese correo o documento.', 'error');
        }

        $stmt = $pdo->prepare('INSERT INTO usuarios (id_rol, nombre, correo, documento, telefono, password_hash)
                                VALUES (:rol, :nombre, :correo, :doc, :tel, :hash)');
        $stmt->execute([
            ':rol' => $idRol, ':nombre' => $nombre, ':correo' => $correo, ':doc' => $documento,
            ':tel' => $telefono, ':hash' => password_hash($password, PASSWORD_DEFAULT),
        ]);
        $nuevoId = $pdo->lastInsertId();

        // Si el rol creado es "empleado", crea también su ficha en la tabla empleados
        $nombreRol = $pdo->query("SELECT nombre FROM roles WHERE id_rol = $idRol")->fetchColumn();
        if ($nombreRol === 'empleado') {
            $pdo->prepare('INSERT INTO empleados (id_usuario) VALUES (:id)')->execute([':id' => $nuevoId]);
        }
        if ($nombreRol === 'cliente') {
            $pdo->prepare('INSERT INTO clientes (id_usuario, nombre, correo, telefono) VALUES (:u,:n,:c,:t)')
                ->execute([':u' => $nuevoId, ':n' => $nombre, ':c' => $correo, ':t' => $telefono]);
        }

        registrarLog($admin['id_usuario'], 'crear_usuario', "Creó al usuario #$nuevoId ($correo)");
        volver('Usuario creado correctamente.');
        break;

    case 'toggle_activo':
        $stmt = $pdo->prepare('UPDATE usuarios SET activo = NOT activo WHERE id_usuario = :id AND id_usuario != :admin');
        $stmt->execute([':id' => $idUsuario, ':admin' => $admin['id_usuario']]);
        registrarLog($admin['id_usuario'], 'toggle_activo', "Usuario #$idUsuario");
        volver('Estado actualizado.');
        break;

    case 'toggle_bloqueo':
        $stmt = $pdo->prepare('UPDATE usuarios SET bloqueado = NOT bloqueado, bloqueado_hasta = NULL, intentos_fallidos = 0 WHERE id_usuario = :id AND id_usuario != :admin');
        $stmt->execute([':id' => $idUsuario, ':admin' => $admin['id_usuario']]);
        registrarLog($admin['id_usuario'], 'toggle_bloqueo', "Usuario #$idUsuario");
        volver('Estado de bloqueo actualizado.');
        break;

    case 'cambiar_rol':
        $idRol = (int)($_POST['id_rol'] ?? 0);
        if ($idUsuario === $admin['id_usuario']) {
            volver('No puedes cambiar tu propio rol.', 'error');
        }
        $stmt = $pdo->prepare('UPDATE usuarios SET id_rol = :rol WHERE id_usuario = :id');
        $stmt->execute([':rol' => $idRol, ':id' => $idUsuario]);
        registrarLog($admin['id_usuario'], 'cambiar_rol', "Usuario #$idUsuario -> rol #$idRol");
        volver('Rol actualizado.');
        break;

    case 'resetear_password':
        // Genera una contraseña temporal simple que el admin puede comunicar al usuario
        $temporal = 'Temporal' . random_int(100, 999) . '!';
        $stmt = $pdo->prepare('UPDATE usuarios SET password_hash = :h, intentos_fallidos = 0, bloqueado = 0 WHERE id_usuario = :id');
        $stmt->execute([':h' => password_hash($temporal, PASSWORD_DEFAULT), ':id' => $idUsuario]);
        registrarLog($admin['id_usuario'], 'resetear_password', "Usuario #$idUsuario");
        volver("Contraseña restablecida a: $temporal (comunícasela al usuario)");
        break;

    default:
        volver('Acción no reconocida.', 'error');
}
