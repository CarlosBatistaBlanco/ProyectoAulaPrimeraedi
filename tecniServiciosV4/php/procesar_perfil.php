<?php
/**
 * php/procesar_perfil.php
 * -----------------------------------------------------------------
 * Procesa la edición del propio perfil: nombre, teléfono, foto de
 * perfil (avatar) y cambio de contraseña. Funciona para CUALQUIER
 * rol (administrador, empleado o cliente) — cada uno edita solo su
 * propia cuenta, nunca la de otro usuario, usando siempre
 * $u['id_usuario'] tomado de la sesión actual, nunca de un campo del
 * formulario (así nadie puede editar el perfil de otra persona
 * cambiando un id en el HTML).
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirLogin();

function volver(string $mensaje, string $tipo = 'exito'): void {
    global $u;
    header('Location: ../panel/' . $u['rol'] . '/perfil.php?' . $tipo . '=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    volver('Solicitud inválida o sesión expirada.', 'error');
}

$accion = $_POST['accion'] ?? '';

if ($accion === 'actualizar_datos') {
    $nombre = trim($_POST['nombre'] ?? '');
    $telefono = trim($_POST['telefono'] ?? '');
    if ($nombre === '') volver('El nombre no puede estar vacío.', 'error');

    // --- Foto de perfil (opcional): solo se procesa si el usuario subió una ---
    $rutaAvatar = null;
    if (!empty($_FILES['avatar']['name']) && $_FILES['avatar']['error'] === UPLOAD_ERR_OK) {
        $permitidas = ['jpg', 'jpeg', 'png', 'webp'];
        $ext = strtolower(pathinfo($_FILES['avatar']['name'], PATHINFO_EXTENSION));
        if (!in_array($ext, $permitidas, true)) {
            volver('La foto debe ser JPG, PNG o WEBP.', 'error');
        }
        // Nombre nuevo y aleatorio (nunca se confía en el nombre original del archivo)
        $nombreArchivo = 'avatar_' . $u['id_usuario'] . '_' . bin2hex(random_bytes(5)) . '.' . $ext;
        $destino = __DIR__ . '/../uploads/avatares/' . $nombreArchivo;
        if (!is_dir(__DIR__ . '/../uploads/avatares')) {
            mkdir(__DIR__ . '/../uploads/avatares', 0755, true);
        }
        if (move_uploaded_file($_FILES['avatar']['tmp_name'], $destino)) {
            $rutaAvatar = 'uploads/avatares/' . $nombreArchivo;
        }
    }

    if ($rutaAvatar) {
        $pdo->prepare('UPDATE usuarios SET nombre = :n, telefono = :t, avatar = :a WHERE id_usuario = :id')
            ->execute([':n' => $nombre, ':t' => $telefono, ':a' => $rutaAvatar, ':id' => $u['id_usuario']]);
        $_SESSION['avatar'] = $rutaAvatar;
    } else {
        $pdo->prepare('UPDATE usuarios SET nombre = :n, telefono = :t WHERE id_usuario = :id')
            ->execute([':n' => $nombre, ':t' => $telefono, ':id' => $u['id_usuario']]);
    }

    // Si además es cliente, refleja el cambio en la tabla clientes (usada por pedidos/checkout)
    if ($u['rol'] === 'cliente') {
        $pdo->prepare('UPDATE clientes SET nombre = :n, telefono = :t WHERE id_usuario = :id')
            ->execute([':n' => $nombre, ':t' => $telefono, ':id' => $u['id_usuario']]);
    }

    $_SESSION['nombre'] = $nombre;
    registrarLog($u['id_usuario'], 'actualizar_perfil', '');
    volver('Tus datos se actualizaron correctamente.');
}

if ($accion === 'cambiar_password') {
    $actual = $_POST['actual'] ?? '';
    $nueva = $_POST['nueva'] ?? '';
    $nueva2 = $_POST['nueva2'] ?? '';

    $stmt = $pdo->prepare('SELECT password_hash FROM usuarios WHERE id_usuario = :id');
    $stmt->execute([':id' => $u['id_usuario']]);
    $hashActual = $stmt->fetchColumn();

    if (!password_verify($actual, $hashActual)) {
        volver('Tu contraseña actual no es correcta.', 'error');
    }
    if (strlen($nueva) < 8 || !preg_match('/[A-Z]/', $nueva) || !preg_match('/[0-9]/', $nueva)) {
        volver('La nueva contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.', 'error');
    }
    if ($nueva !== $nueva2) {
        volver('Las contraseñas nuevas no coinciden.', 'error');
    }

    $pdo->prepare('UPDATE usuarios SET password_hash = :h WHERE id_usuario = :id')
        ->execute([':h' => password_hash($nueva, PASSWORD_DEFAULT), ':id' => $u['id_usuario']]);
    registrarLog($u['id_usuario'], 'cambiar_password_propio', '');
    volver('Tu contraseña se actualizó correctamente.');
}

volver('Acción no reconocida.', 'error');
