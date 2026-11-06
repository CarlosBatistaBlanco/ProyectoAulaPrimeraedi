<?php
/**
 * _diag_bd.php — DIAGNÓSTICO TEMPORAL (borrar tras usar)
 * Muestra qué servidor MySQL ve PHP y qué usuarios existen, SIN contraseñas.
 */
require_once __DIR__ . '/php/conexion.php';

echo "<pre style='font-family:monospace'>";
echo "PHP:        " . PHP_VERSION . "\n";
echo "Servidor:   " . $pdo->getAttribute(PDO::ATTR_SERVER_VERSION) . "\n";
echo "Conexión:   " . $pdo->getAttribute(PDO::ATTR_CONNECTION_STATUS) . "\n";
echo "BD actual:  " . $pdo->query('SELECT DATABASE()')->fetchColumn() . "\n";
echo str_repeat('=', 70) . "\n\n";

echo "TABLAS (" . $pdo->query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='TecniSecure'")->fetchColumn() . "):\n";
foreach ($pdo->query('SHOW TABLES') as $t) {
    echo "  - " . array_values($t)[0] . "\n";
}

echo "\n" . str_repeat('=', 70) . "\n\n";
echo "USUARIOS Y ROLES:\n";
$sql = 'SELECT u.id_usuario, u.nombre, u.correo, r.nombre AS rol, u.activo, u.bloqueado, u.ultimo_acceso
        FROM usuarios u JOIN roles r ON r.id_rol = u.id_rol ORDER BY u.id_usuario';
printf("  %-3s %-28s %-32s %-14s %-4s %-4s\n", 'ID', 'NOMBRE', 'CORREO', 'ROL', 'ACT', 'BLO');
foreach ($pdo->query($sql) as $u) {
    printf("  %-3s %-28s %-32s %-14s %-4s %-4s\n",
        $u['id_usuario'], substr($u['nombre'],0,27), $u['correo'], $u['rol'], $u['activo'], $u['bloqueado']);
}

echo "\n" . str_repeat('=', 70) . "\n\n";
echo "RESUMEN POR ROL:\n";
foreach ($pdo->query('SELECT r.nombre, COUNT(u.id_usuario) n FROM roles r LEFT JOIN usuarios u ON u.id_rol=r.id_rol GROUP BY r.nombre') as $x) {
    echo "  {$x['nombre']}: {$x['n']}\n";
}

echo "\nCONTEOS CLAVE:\n";
foreach (['productos','servicios','pedidos','clientes','facturas','mensajes_chat'] as $tb) {
    try { echo "  $tb: " . $pdo->query("SELECT COUNT(*) FROM $tb")->fetchColumn() . "\n"; }
    catch (Exception $e) { echo "  $tb: (no existe)\n"; }
}
echo "</pre>";
