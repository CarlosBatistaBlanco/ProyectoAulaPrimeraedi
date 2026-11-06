<?php
/**
 * admin_productos.php
 * -----------------------------------------------------------------
 * CRUD de productos. Valida SKU/código de barras únicos ANTES de
 * guardar (el prompt original pedía impedir automáticamente
 * "productos repetidos, IDs duplicados, códigos repetidos, SKU
 * repetidos" — la tabla ya tiene UNIQUE en sku y codigo_barras como
 * segunda capa de protección a nivel de base de datos).
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$admin = requerirRol(['administrador']);

function volver(string $mensaje, string $tipo = 'exito'): void {
    header('Location: ../panel/administrador/productos.php?' . $tipo . '=' . urlencode($mensaje));
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'POST' || !csrfValido($_POST['csrf_token'] ?? null)) {
    volver('Solicitud inválida o sesión expirada.', 'error');
}

$accion = $_POST['accion'] ?? '';

/** Sube una imagen a uploads/productos/ y devuelve la ruta relativa, o null si no se subió nada */
function subirImagen(array $archivo): ?string {
    if (empty($archivo['name']) || $archivo['error'] !== UPLOAD_ERR_OK) return null;
    $permitidas = ['jpg', 'jpeg', 'png', 'webp'];
    $ext = strtolower(pathinfo($archivo['name'], PATHINFO_EXTENSION));
    if (!in_array($ext, $permitidas, true)) return null;

    $nombreNuevo = 'prod_' . bin2hex(random_bytes(6)) . '.' . $ext;
    $destino = __DIR__ . '/../uploads/productos/' . $nombreNuevo;
    if (move_uploaded_file($archivo['tmp_name'], $destino)) {
        return 'uploads/productos/' . $nombreNuevo;
    }
    return null;
}

if ($accion === 'crear' || $accion === 'editar') {
    $nombre = trim($_POST['nombre'] ?? '');
    $sku = trim($_POST['sku'] ?? '');
    $codigoBarras = trim($_POST['codigo_barras'] ?? '') ?: null;
    $idCategoria = (int)($_POST['id_categoria'] ?? 0);
    $idMarca = (int)($_POST['id_marca'] ?? 0) ?: null;
    $descripcion = trim($_POST['descripcion'] ?? '');
    $precio = (float)($_POST['precio'] ?? 0);
    $descuento = (float)($_POST['descuento_pct'] ?? 0);
    $impuesto = (float)($_POST['impuesto_pct'] ?? 19);
    $garantia = (int)($_POST['garantia_meses'] ?? 12);
    $stockMin = (int)($_POST['stock_minimo'] ?? 5);
    $stockMax = (int)($_POST['stock_maximo'] ?? 100);
    $destacado = isset($_POST['destacado']) ? 1 : 0;
    $activo = isset($_POST['activo']) ? 1 : 0;

    if ($nombre === '' || $sku === '' || $idCategoria <= 0 || $precio <= 0) {
        volver('Nombre, SKU, categoría y precio son obligatorios.', 'error');
    }

    // Verifica SKU/código de barras único (excluyendo el propio producto si se está editando)
    $idProducto = (int)($_POST['id_producto'] ?? 0);
    $sqlDup = 'SELECT id_producto FROM productos WHERE (sku = :sku OR (codigo_barras IS NOT NULL AND codigo_barras = :cb)) AND id_producto != :id LIMIT 1';
    $stmt = $pdo->prepare($sqlDup);
    $stmt->execute([':sku' => $sku, ':cb' => $codigoBarras, ':id' => $idProducto]);
    if ($stmt->fetch()) {
        volver('Ya existe otro producto con ese SKU o código de barras.', 'error');
    }

    $rutaImagen = subirImagen($_FILES['imagen'] ?? []);

    if ($accion === 'crear') {
        $rutaImagen = $rutaImagen ?? 'img/productos/sin-imagen.jpg';
        $stmt = $pdo->prepare('INSERT INTO productos
            (id_categoria, id_marca, nombre, sku, codigo_barras, descripcion, precio, descuento_pct, impuesto_pct,
             garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
            VALUES (:cat,:marca,:nombre,:sku,:cb,:desc,:precio,:desc_pct,:imp,:gar,:stock,:smin,:smax,:img,:dest,:act)');
        $stock = (int)($_POST['stock'] ?? 0);
        $stmt->execute([
            ':cat' => $idCategoria, ':marca' => $idMarca, ':nombre' => $nombre, ':sku' => $sku, ':cb' => $codigoBarras,
            ':desc' => $descripcion, ':precio' => $precio, ':desc_pct' => $descuento, ':imp' => $impuesto,
            ':gar' => $garantia, ':stock' => $stock, ':smin' => $stockMin, ':smax' => $stockMax,
            ':img' => $rutaImagen, ':dest' => $destacado, ':act' => $activo,
        ]);
        $idProducto = $pdo->lastInsertId();

        if ($stock > 0) {
            $pdo->prepare('INSERT INTO inventario_movimientos (id_producto, id_usuario, tipo, cantidad, stock_resultante, motivo)
                            VALUES (:p,:u,"entrada",:c1,:c2,"Stock inicial al crear el producto")')
                ->execute([':p' => $idProducto, ':u' => $admin['id_usuario'], ':c1' => $stock, ':c2' => $stock]);
        }
        registrarLog($admin['id_usuario'], 'crear_producto', "Producto #$idProducto ($sku)");
        $mensaje = 'Producto creado correctamente.';
    } else {
        $campos = 'id_categoria=:cat, id_marca=:marca, nombre=:nombre, sku=:sku, codigo_barras=:cb, descripcion=:desc,
                    precio=:precio, descuento_pct=:desc_pct, impuesto_pct=:imp, garantia_meses=:gar,
                    stock_minimo=:smin, stock_maximo=:smax, destacado=:dest, activo=:act';
        $params = [
            ':cat' => $idCategoria, ':marca' => $idMarca, ':nombre' => $nombre, ':sku' => $sku, ':cb' => $codigoBarras,
            ':desc' => $descripcion, ':precio' => $precio, ':desc_pct' => $descuento, ':imp' => $impuesto,
            ':gar' => $garantia, ':smin' => $stockMin, ':smax' => $stockMax, ':dest' => $destacado, ':act' => $activo,
            ':id' => $idProducto,
        ];
        if ($rutaImagen) { $campos .= ', imagen=:img'; $params[':img'] = $rutaImagen; }

        $stmt = $pdo->prepare("UPDATE productos SET $campos WHERE id_producto = :id");
        $stmt->execute($params);
        registrarLog($admin['id_usuario'], 'editar_producto', "Producto #$idProducto ($sku)");
        $mensaje = 'Producto actualizado correctamente.';
    }

    // Imágenes adicionales (galería)
    if (!empty($_FILES['imagenes_extra']) && is_array($_FILES['imagenes_extra']['name'])) {
        foreach ($_FILES['imagenes_extra']['name'] as $i => $nombreArchivo) {
            if ($nombreArchivo === '') continue;
            $archivo = [
                'name' => $_FILES['imagenes_extra']['name'][$i],
                'tmp_name' => $_FILES['imagenes_extra']['tmp_name'][$i],
                'error' => $_FILES['imagenes_extra']['error'][$i],
            ];
            $ruta = subirImagen($archivo);
            if ($ruta) {
                $pdo->prepare('INSERT INTO producto_imagenes (id_producto, ruta) VALUES (:p, :r)')
                    ->execute([':p' => $idProducto, ':r' => $ruta]);
            }
        }
    }

    // Si el stock quedó por debajo del mínimo, notifica a los administradores
    $stockActual = $pdo->prepare('SELECT stock, stock_minimo, nombre FROM productos WHERE id_producto = :id');
    $stockActual->execute([':id' => $idProducto]);
    $prod = $stockActual->fetch();
    if ($prod && $prod['stock'] <= $prod['stock_minimo']) {
        notificarAdministradores('Stock bajo', $prod['nombre'] . ' tiene solo ' . $prod['stock'] . ' unidades.', 'panel/administrador/inventario.php');
    }

    volver($mensaje);
}

if ($accion === 'toggle_activo') {
    $idProducto = (int)($_POST['id_producto'] ?? 0);
    $pdo->prepare('UPDATE productos SET activo = NOT activo WHERE id_producto = :id')->execute([':id' => $idProducto]);
    registrarLog($admin['id_usuario'], 'toggle_activo_producto', "Producto #$idProducto");
    volver('Estado del producto actualizado.');
}

volver('Acción no reconocida.', 'error');
