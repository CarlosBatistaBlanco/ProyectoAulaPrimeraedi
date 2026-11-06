<?php
/**
 * verificar_instalacion.php
 * -----------------------------------------------------------------
 * Herramienta de diagnóstico. Ábrela en el navegador para revisar,
 * de un solo vistazo, si tu instalación está completa: archivos que
 * deberían existir y tablas que debería tener tu base de datos.
 *
 * Úsala cada vez que algo dé un error de "tabla no existe" o
 * "página no encontrada" — normalmente significa que faltó copiar
 * algún archivo nuevo, o que la base de datos no se reimportó
 * completa después de una actualización del proyecto.
 *
 *   http://localhost/TecniSecure/verificar_instalacion.php
 * -----------------------------------------------------------------
 */

echo "<html><head><meta charset='utf-8'><title>Diagnóstico - TECNISECURE</title></head>";
echo "<body style='font-family:sans-serif; max-width:800px; margin:40px auto; line-height:1.5;'>";
echo "<h1>Diagnóstico de instalación — TECNISECURE</h1>";

function fila($ok, $texto) {
    $icono = $ok ? "✅" : "❌";
    $color = $ok ? "#1a7a3c" : "#c0392b";
    echo "<div style='padding:6px 0; color:$color;'>$icono $texto</div>";
}

// -----------------------------------------------------------------
// 1. Versión de PHP y extensiones
// -----------------------------------------------------------------
echo "<h2>1. PHP</h2>";
fila(version_compare(PHP_VERSION, '8.0.0', '>='), "Versión de PHP: " . PHP_VERSION . " (se requiere 8.0 o superior)");
fila(extension_loaded('pdo_mysql'), "Extensión pdo_mysql " . (extension_loaded('pdo_mysql') ? "activada" : "NO activada — actívala en php.ini"));

// -----------------------------------------------------------------
// 2. Archivos clave que deben existir
// -----------------------------------------------------------------
echo "<h2>2. Archivos del proyecto</h2>";
$archivosClave = [
    'php/conexion.php', 'php/auth.php',
    'login.php', 'registro.php', 'index.php',
    'pages/productos.php',
    'panel/administrador/index.php', 'panel/administrador/usuarios.php',
    'panel/administrador/productos.php', 'panel/administrador/inventario.php',
    'panel/administrador/pedidos.php', 'panel/administrador/pedido_detalle.php',
    'panel/administrador/facturacion.php', 'panel/administrador/reportes.php',
    'panel/administrador/soporte.php', 'panel/administrador/chat.php',
    'panel/administrador/auditoria.php', 'panel/administrador/respaldo.php',
    'panel/empleado/index.php', 'panel/empleado/ventas.php',
    'panel/cliente/index.php', 'panel/cliente/pedidos.php',
    'php/lib/fpdf/fpdf.php',
];
$faltantes = [];
foreach ($archivosClave as $rel) {
    $existe = file_exists(__DIR__ . '/' . $rel);
    if (!$existe) $faltantes[] = $rel;
    fila($existe, $rel);
}

// -----------------------------------------------------------------
// 3. Base de datos: conexión y tablas esperadas
// -----------------------------------------------------------------
echo "<h2>3. Base de datos</h2>";
try {
    require_once __DIR__ . '/php/conexion.php';
    fila(true, "Conexión a MySQL exitosa (base: " . DB_NAME . ")");

    $tablasEsperadas = [
        'roles', 'usuarios', 'empleados', 'clientes', 'tokens_recuperacion', 'recordar_tokens',
        'categorias', 'marcas', 'productos', 'producto_imagenes', 'inventario_movimientos',
        'servicios', 'solicitudes_servicio',
        'pedidos', 'pedido_detalle', 'pedidos_historial', 'facturas',
        'contactos', 'mensajes_chat', 'notificaciones',
        'favoritos', 'resenas', 'logs_actividad',
    ];
    $tablasReales = $pdo->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
    $tablasFaltantes = array_diff($tablasEsperadas, $tablasReales);

    fila(true, count($tablasReales) . " tablas encontradas en la base de datos (se esperan " . count($tablasEsperadas) . ")");
    foreach ($tablasEsperadas as $t) {
        fila(in_array($t, $tablasReales, true), "Tabla: $t");
    }

    // Columna nueva bloqueado_hasta (agregada en la última corrección)
    $columnas = $pdo->query("SHOW COLUMNS FROM usuarios LIKE 'bloqueado_hasta'")->fetchAll();
    fila(count($columnas) > 0, "Columna usuarios.bloqueado_hasta (bloqueo temporal de login)");

    // Usuario administrador semilla
    $admin = $pdo->query("SELECT correo FROM usuarios WHERE correo = 'admin@tecnicsecure.com'")->fetch();
    fila((bool)$admin, "Usuario administrador de prueba (admin@tecnicsecure.com) existe");

} catch (Throwable $e) {
    fila(false, "No se pudo conectar a la base de datos: " . htmlspecialchars($e->getMessage()));
    $tablasFaltantes = ['(no se pudo verificar, revisa la conexión primero)'];
}

// -----------------------------------------------------------------
// 4. Resumen y siguiente paso recomendado
// -----------------------------------------------------------------
echo "<h2>4. Resumen</h2>";
if (empty($faltantes) && empty($tablasFaltantes)) {
    echo "<p style='color:#1a7a3c; font-weight:bold;'>Todo está completo. Si sigues viendo un error, cuéntame exactamente en qué página ocurre.</p>";
} else {
    echo "<p style='color:#c0392b; font-weight:bold;'>Se encontraron faltantes. Recomendación:</p><ul>";
    if (!empty($faltantes)) {
        echo "<li><b>Faltan archivos.</b> Borra por completo tu carpeta actual del proyecto y vuelve a extraer el .zip más reciente desde cero (no copies archivo por archivo encima de una copia vieja).</li>";
    }
    if (!empty($tablasFaltantes)) {
        echo "<li><b>Faltan tablas en la base de datos.</b> Ve a phpMyAdmin > pestaña Importar > selecciona <code>sql/TecniSecure.sql</code> completo. El script empieza borrando la base de datos anterior, así que siempre queda limpio.</li>";
    }
    echo "</ul>";
}

echo "<p style='margin-top:30px; color:#888;'>⚠ Por seguridad, borra este archivo cuando termines de diagnosticar.</p>";
echo "</body></html>";
