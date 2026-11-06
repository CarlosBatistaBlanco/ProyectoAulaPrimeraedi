<?php
/**
 * reparar_admin.php
 * -----------------------------------------------------------------
 * HERRAMIENTA DE EMERGENCIA — úsala solo si no puedes iniciar sesión
 * con admin@tecnicsecure.com / Admin123! después de reimportar
 * sql/TecniSecure.sql correctamente.
 *
 * Qué hace: genera un hash de contraseña NUEVO directamente en tu
 * propio servidor (con tu propia versión de PHP) y actualiza (o crea,
 * si no existiera) el usuario administrador con ese hash. Esto
 * elimina cualquier problema de que el hash guardado en el archivo
 * .sql se haya dañado al copiarlo/pegarlo o importarlo.
 *
 * CÓMO USARLA:
 *   1. Con Apache y MySQL encendidos, abre en tu navegador:
 *      http://localhost/TecniSecure/reparar_admin.php
 *   2. Lee el mensaje de resultado.
 *   3. Intenta iniciar sesión de nuevo con admin@tecnicsecure.com / Admin123!
 *   4. IMPORTANTE: borra este archivo después de usarlo (no debe
 *      quedar accesible públicamente una vez resuelto el problema).
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/php/conexion.php';

$correo = 'admin@tecnicsecure.com';
$passwordNueva = 'Admin123!';
$hash = password_hash($passwordNueva, PASSWORD_DEFAULT);

echo "<html><body style='font-family:sans-serif; max-width:640px; margin:60px auto; line-height:1.6;'>";
echo "<h2>Reparación del usuario administrador</h2>";

try {
    // Verifica que exista la tabla roles con el rol administrador
    $idRol = $pdo->query("SELECT id_rol FROM roles WHERE nombre = 'administrador'")->fetchColumn();
    if (!$idRol) {
        throw new Exception("La tabla 'roles' no tiene el rol 'administrador'. Vuelve a importar sql/TecniSecure.sql completo (te recomiendo hacerlo desde phpMyAdmin, pestaña Importar).");
    }

    $stmt = $pdo->prepare('SELECT id_usuario FROM usuarios WHERE correo = :c');
    $stmt->execute([':c' => $correo]);
    $existe = $stmt->fetch();

    if ($existe) {
        $pdo->prepare('UPDATE usuarios SET password_hash = :h, activo = 1, bloqueado = 0, bloqueado_hasta = NULL, intentos_fallidos = 0 WHERE correo = :c')
            ->execute([':h' => $hash, ':c' => $correo]);
        echo "<p style='color:green;'>✔ El usuario ya existía. Se actualizó su contraseña, se desbloqueó y se activó la cuenta.</p>";
    } else {
        $pdo->prepare('INSERT INTO usuarios (id_rol, nombre, correo, password_hash, activo) VALUES (:rol, :nombre, :correo, :hash, 1)')
            ->execute([':rol' => $idRol, ':nombre' => 'Administrador Principal', ':correo' => $correo, ':hash' => $hash]);
        echo "<p style='color:green;'>✔ El usuario no existía. Se creó de nuevo.</p>";
    }

    echo "<p><b>Ya puedes iniciar sesión con:</b><br>Correo: <code>$correo</code><br>Contraseña: <code>$passwordNueva</code></p>";
    echo "<p style='color:#b45309;'>⚠ Por seguridad, borra este archivo (reparar_admin.php) de tu servidor ahora que ya funcionó.</p>";

} catch (Exception $e) {
    echo "<p style='color:red;'>✘ " . htmlspecialchars($e->getMessage()) . "</p>";
}

echo "<p><a href='login.php'>Ir a iniciar sesión</a></p>";
echo "</body></html>";
