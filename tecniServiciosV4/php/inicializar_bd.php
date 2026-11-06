<?php
/**
 * php/inicializar_bd.php
 * Script CLI para verificar y preparar automáticamente la base de datos
 * al ejecutar el lanzador iniciar.bat / iniciar.ps1.
 */

echo "\n====================================================\n";
echo "       TECNISECURE - Verificando Base de Datos\n";
echo "====================================================\n";

$host = 'localhost';
$dbname = 'tecniservicios';
$credenciales = [
    ['user' => 'tecniservicios', 'pass' => 'TecniServ2026*'],
    ['user' => 'root', 'pass' => ''],
    ['user' => 'root', 'pass' => 'root']
];

$pdoServer = null;
$usuarioValido = null;
$claveValida = null;

foreach ($credenciales as $cred) {
    try {
        $pdoServer = new PDO("mysql:host=$host;charset=utf8mb4", $cred['user'], $cred['pass'], [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
        ]);
        $usuarioValido = $cred['user'];
        $claveValida = $cred['pass'];
        echo "[OK] Conectado a MySQL con usuario: '{$cred['user']}'\n";
        break;
    } catch (PDOException $e) {
        // Intenta el siguiente
    }
}

if (!$pdoServer) {
    echo "[AVISO] No se pudo conectar a MySQL.\n";
    echo "       Asegurate de que el modulo MySQL este iniciado en XAMPP.\n";
    exit(1);
}

// 1. Crear base de datos si no existe
try {
    $pdoServer->exec("CREATE DATABASE IF NOT EXISTS `$dbname` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    echo "[OK] Base de datos '$dbname' verificada.\n";
} catch (PDOException $e) {
    echo "[ERROR] Al crear la base de datos: " . $e->getMessage() . "\n";
    exit(1);
}

// 2. Conectar a la base de datos
try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $usuarioValido, $claveValida, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
    ]);
} catch (PDOException $e) {
    echo "[ERROR] Al conectar a '$dbname': " . $e->getMessage() . "\n";
    exit(1);
}

// 3. Crear usuario 'tecniservicios' si no existe
try {
    $pdoServer->exec("CREATE USER IF NOT EXISTS 'tecniservicios'@'localhost' IDENTIFIED BY 'TecniServ2026*'");
    $pdoServer->exec("GRANT ALL PRIVILEGES ON `$dbname`.* TO 'tecniservicios'@'localhost'");
    $pdoServer->exec("FLUSH PRIVILEGES");
    echo "[OK] Usuario 'tecniservicios' y permisos verificados.\n";
} catch (Exception $e) {
    // Si no tiene permisos de crear usuarios (ej. usuario MySQL restringido), continuar normalmente
}

// 4. Verificar tablas e importar SQL si esta vacia
$stmt = $pdo->query("SHOW TABLES");
$tablas = $stmt->fetchAll(PDO::FETCH_COLUMN);

if (count($tablas) === 0) {
    echo "[INFO] Base de datos vacia. Importando tablas y datos iniciales...\n";
    
    $sqlFile = __DIR__ . '/../sql/tecnicsecure.sql';
    if (file_exists($sqlFile)) {
        $sql = file_get_contents($sqlFile);
        $pdo->exec($sql);
        echo "[OK] Estructura inicial (tecnicsecure.sql) importada con exito.\n";
    }

    $catalogoFile = __DIR__ . '/../sql/catalogo-28productos.sql';
    if (file_exists($catalogoFile)) {
        $sqlCat = file_get_contents($catalogoFile);
        $pdo->exec($sqlCat);
        echo "[OK] Catalogo de 28 productos importado con exito.\n";
    }
} else {
    echo "[OK] Base de datos con " . count($tablas) . " tablas listas y operativas.\n";
}

echo "====================================================\n\n";
exit(0);
