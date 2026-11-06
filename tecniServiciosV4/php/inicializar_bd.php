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
    echo "[OK] Base de datos con " . count($tablas) . " tablas verificadas.\n";
}

// 5. Garantizar catalogo completo de 28 productos y descuentos activos
try {
    // Asegurar columna de descuento si faltaba
    $col = $pdo->query("SHOW COLUMNS FROM productos LIKE 'descuento_pct'")->fetch();
    if (!$col) {
        $pdo->exec("ALTER TABLE productos ADD COLUMN descuento_pct DECIMAL(5,2) DEFAULT 0");
    }

    $numProds = (int)$pdo->query("SELECT COUNT(*) FROM productos")->fetchColumn();
    $conDesc = (int)$pdo->query("SELECT COUNT(*) FROM productos WHERE descuento_pct > 0")->fetchColumn();

    if ($numProds < 28) {
        echo "[INFO] Actualizando catalogo a los 28 productos completos...\n";
        $catalogoFile = __DIR__ . '/../sql/catalogo-28productos.sql';
        if (file_exists($catalogoFile)) {
            $sqlCat = file_get_contents($catalogoFile);
            $pdo->exec($sqlCat);
            echo "[OK] Catalogo de 28 productos sincronizado.\n";
        }
    }

    if ($conDesc === 0) {
        echo "[INFO] Activando descuentos en productos en oferta...\n";
        $pdo->exec("
            UPDATE productos SET descuento_pct = 15 WHERE id_producto = 1 OR nombre LIKE '%Domo 4K%';
            UPDATE productos SET descuento_pct = 20 WHERE id_producto = 3 OR nombre LIKE '%PTZ 360%';
            UPDATE productos SET descuento_pct = 10 WHERE id_producto = 5 OR nombre LIKE '%ColorVu%';
            UPDATE productos SET descuento_pct = 25 WHERE id_producto = 8 OR nombre LIKE '%Perimetral%';
            UPDATE productos SET descuento_pct = 15 WHERE id_producto = 10 OR nombre LIKE '%Sensor%';
            UPDATE productos SET descuento_pct = 18 WHERE id_producto = 15 OR nombre LIKE '%Biometrico%';
            UPDATE productos SET descuento_pct = 10 WHERE id_producto = 17 OR nombre LIKE '%Videoportero%';
            UPDATE productos SET descuento_pct = 12 WHERE id_producto IN (22, 23) OR nombre LIKE '%Disco Duro%';
            UPDATE productos SET descuento_pct = 20 WHERE id_producto IN (26, 27) OR nombre LIKE '%Soporte%';
        ");
        echo "[OK] Descuentos aplicados exitosamente.\n";
    } else {
        echo "[OK] Descuentos activos en productos verificados ($conDesc productos en oferta).\n";
    }
} catch (Exception $e) {
    // Continuar si la tabla productos no está lista
}

echo "====================================================\n\n";
exit(0);
