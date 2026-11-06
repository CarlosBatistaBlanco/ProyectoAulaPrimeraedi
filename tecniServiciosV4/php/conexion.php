<?php
/**
 * conexion.php
 * -----------------------------------------------------------------
 * Archivo central de conexión a la base de datos MySQL usando PDO.
 * Todos los demás archivos PHP incluyen este archivo con:
 *      require_once __DIR__ . '/conexion.php';
 *
 * Si usas XAMPP por defecto, estos datos ya funcionan sin cambiar nada:
 *   Usuario: root
 *   Contraseña: (vacía)
 *   Host: localhost
 *
 * Si en cambio usas el MySQL Server que instalaste junto con MySQL
 * Workbench (sin XAMPP), casi siempre pusiste una contraseña para
 * 'root' durante la instalación. Solo cámbiala abajo en DB_PASS.
 * -----------------------------------------------------------------
 */

// Datos de conexión (ajusta usuario/clave si tu MySQL los tiene distintos)
//
// NOTA: se usa un usuario dedicado 'TecniSecure' en lugar de 'root'.
// MySQL 8 usa por defecto caching_sha2_password, que PHP 8 no sabe leer
// (error "Plugin caching_sha2_password could not be loaded"), por eso el
// usuario fue creado con mysql_native_password.
// Si necesitas recrearlo:
//   CREATE USER 'TecniSecure'@'localhost'
//     IDENTIFIED WITH mysql_native_password BY 'TecniServ2026*';
//   GRANT ALL PRIVILEGES ON TecniSecure.* TO 'TecniSecure'@'localhost';
define('DB_HOST', 'localhost');
define('DB_NAME', 'tecniservicios');
define('DB_USER', 'tecniservicios');
define('DB_PASS', 'TecniServ2026*');

/**
 * CÓDIGO DE SUPLANTACIÓN DE CUENTA
 * -----------------------------------------------------------------
 * Código secreto que SOLO el administrador debe conocer. Se pide como
 * segunda verificación antes de permitir "iniciar sesión como" otro
 * usuario (útil para dar soporte sin pedirle la contraseña al cliente).
 * CÁMBIALO por uno propio antes de usar el sitio en producción — no
 * dejes el valor de fábrica.
 */
define('CODIGO_SUPLANTACION', 'TecniAdmin2026*');

try {
    // DSN = Data Source Name, indica motor, host, base y charset
    $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4';

    $opciones = [
        PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION, // lanza errores como excepciones
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,       // resultados como arrays asociativos
        PDO::ATTR_EMULATE_PREPARES   => false,                  // usa prepares reales (más seguro)
    ];

    $pdo = new PDO($dsn, DB_USER, DB_PASS, $opciones);
} catch (PDOException $e) {
    // Si la base de datos no existe o hay error de conexión, se detiene todo
    die('Error de conexión a la base de datos: ' . $e->getMessage() .
        '<br>Verifica que Apache y MySQL estén encendidos en XAMPP y que importaste sql/TecniSecure.sql');
}

/**
 * recortar_texto()
 * Corta un texto a $limite caracteres y agrega "..." si fue recortado.
 * No depende de la extensión mbstring (funciona en cualquier instalación
 * de PHP/XAMPP sin configuración adicional).
 */
if (!function_exists('recortar_texto')) {
    function recortar_texto(string $texto, int $limite = 90): string {
        if (strlen($texto) <= $limite) {
            return $texto;
        }
        return substr($texto, 0, $limite) . '...';
    }
}

/**
 * texto_pdf()
 * FPDF (en su versión clásica) espera texto en Latin-1, no en UTF-8.
 * La función nativa utf8_decode() quedó obsoleta desde PHP 8.2 y genera
 * avisos si el servidor tiene activado mostrar errores. Esta función
 * hace la misma conversión sin depender de utf8_decode ni de
 * extensiones opcionales (mbstring/iconv), usando iconv() solo si
 * está disponible y, si no, una tabla de reemplazo manual que cubre
 * perfectamente los acentos y la "ñ" del español.
 */
if (!function_exists('texto_pdf')) {
    function texto_pdf(?string $texto): string {
        $texto = $texto ?? '';
        if (function_exists('iconv')) {
            $convertido = @iconv('UTF-8', 'ISO-8859-1//TRANSLIT//IGNORE', $texto);
            if ($convertido !== false) return $convertido;
        }
        $mapa = [
            'á'=>'a','é'=>'e','í'=>'i','ó'=>'o','ú'=>'u','Á'=>'A','É'=>'E','Í'=>'I','Ó'=>'O','Ú'=>'U',
            'ñ'=>'n','Ñ'=>'N','ü'=>'u','Ü'=>'U','¿'=>'?','¡'=>'!','—'=>'-','“'=>'"','”'=>'"','’'=>"'",
        ];
        return strtr($texto, $mapa);
    }
}
