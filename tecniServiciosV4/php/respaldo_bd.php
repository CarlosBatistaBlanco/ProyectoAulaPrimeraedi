<?php
/**
 * respaldo_bd.php
 * -----------------------------------------------------------------
 * Exporta o restaura la base de datos completa usando solo PDO
 * (no depende de que el binario `mysqldump` esté en el PATH del
 * sistema, lo cual varía entre instalaciones de XAMPP).
 *
 * LIMITACIÓN CONOCIDA: el restaurador divide el archivo .sql por
 * punto y coma (;) al final de línea. Funciona perfecto con
 * respaldos generados por este mismo exportador o por phpMyAdmin/
 * MySQL Workbench en modo simple, pero no soporta procedimientos
 * almacenados o triggers con lógica interna que use ";".
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirRol(['administrador']);

$accion = $_GET['accion'] ?? $_POST['accion'] ?? '';

if ($accion === 'exportar') {
    registrarLog($u['id_usuario'], 'exportar_respaldo_bd', '');

    header('Content-Type: application/sql; charset=utf-8');
    header('Content-Disposition: attachment; filename="respaldo_TecniSecure_' . date('Y-m-d_His') . '.sql"');

    echo "-- Respaldo de TECNISECURE generado el " . date('Y-m-d H:i:s') . "\n";
    echo "SET FOREIGN_KEY_CHECKS=0;\n\n";

    $tablas = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
    foreach ($tablas as $tabla) {
        $create = $pdo->query("SHOW CREATE TABLE `$tabla`")->fetch();
        echo "DROP TABLE IF EXISTS `$tabla`;\n";
        echo $create['Create Table'] . ";\n\n";

        $filas = $pdo->query("SELECT * FROM `$tabla`")->fetchAll(PDO::FETCH_ASSOC);
        foreach ($filas as $fila) {
            $columnas = array_map(fn($c) => "`$c`", array_keys($fila));
            $valores = array_map(function ($v) use ($pdo) {
                return $v === null ? 'NULL' : $pdo->quote($v);
            }, array_values($fila));
            echo "INSERT INTO `$tabla` (" . implode(',', $columnas) . ") VALUES (" . implode(',', $valores) . ");\n";
        }
        echo "\n";
    }
    echo "SET FOREIGN_KEY_CHECKS=1;\n";
    exit;
}

if ($accion === 'restaurar') {
    if (!csrfValido($_POST['csrf_token'] ?? null)) {
        header('Location: ../panel/administrador/respaldo.php?error=' . urlencode('Sesión expirada.'));
        exit;
    }
    if (empty($_FILES['archivo_sql']) || $_FILES['archivo_sql']['error'] !== UPLOAD_ERR_OK) {
        header('Location: ../panel/administrador/respaldo.php?error=' . urlencode('No se pudo leer el archivo.'));
        exit;
    }

    $contenido = file_get_contents($_FILES['archivo_sql']['tmp_name']);
    // Divide por punto y coma al final de línea (ver limitación en el comentario superior)
    $sentencias = preg_split('/;\s*\n/', $contenido);

    try {
        $pdo->exec('SET FOREIGN_KEY_CHECKS=0');
        foreach ($sentencias as $sql) {
            $sql = trim($sql);
            if ($sql === '' || str_starts_with($sql, '--')) continue;
            $pdo->exec($sql);
        }
        $pdo->exec('SET FOREIGN_KEY_CHECKS=1');
        registrarLog($u['id_usuario'], 'restaurar_respaldo_bd', $_FILES['archivo_sql']['name']);
        header('Location: ../panel/administrador/respaldo.php?exito=' . urlencode('Base de datos restaurada correctamente.'));
    } catch (PDOException $e) {
        header('Location: ../panel/administrador/respaldo.php?error=' . urlencode('Error restaurando: ' . $e->getMessage()));
    }
    exit;
}

header('Location: ../panel/administrador/respaldo.php');
