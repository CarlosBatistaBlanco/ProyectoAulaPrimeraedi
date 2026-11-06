<?php
/**
 * panel_sidebar.php
 * -----------------------------------------------------------------
 * Menú lateral que cambia según $u['rol']. Los módulos que todavía
 * no existen se muestran deshabilitados con una etiqueta "Próximamente".
 * -----------------------------------------------------------------
 */
function itemMenu(string $rutaBase, string $href, string $icono, string $texto, bool $activo, bool $disponible = true): string {
    if (!$disponible) {
        return '<span class="panel-menu__item panel-menu__item--proximo"><i class="fa-solid ' . $icono . '"></i> ' . $texto . ' <em>Próximamente</em></span>';
    }
    $clase = $activo ? 'panel-menu__item activo' : 'panel-menu__item';
    return '<a href="' . $rutaBase . $href . '" class="' . $clase . '"><i class="fa-solid ' . $icono . '"></i> ' . $texto . '</a>';
}
$actual = basename($_SERVER['PHP_SELF']);
?>
<nav class="panel-menu">
<?php if ($u['rol'] === 'administrador'): ?>
    <span class="panel-menu__seccion">Principal</span>
    <?= itemMenu('', 'index.php', 'fa-gauge', 'Dashboard', $actual === 'index.php'); ?>
    <?= itemMenu('', 'usuarios.php', 'fa-users', 'Usuarios', $actual === 'usuarios.php'); ?>
    <?= itemMenu('', 'perfil.php', 'fa-user', 'Mi perfil', $actual === 'perfil.php'); ?>

    <span class="panel-menu__seccion">Operación</span>
    <?= itemMenu('', 'empleados.php', 'fa-id-badge', 'Empleados', $actual === 'empleados.php', true); ?>
    <?= itemMenu('', 'clientes.php', 'fa-user-tie', 'Clientes', $actual === 'clientes.php', true); ?>
    <?= itemMenu('', 'productos.php', 'fa-box', 'Productos', in_array($actual, ['productos.php','kardex.php']), true); ?>
    <?= itemMenu('', 'inventario.php', 'fa-warehouse', 'Inventario', in_array($actual, ['inventario.php','kardex.php']), true); ?>
    <?= itemMenu('', 'pedidos.php', 'fa-cart-shopping', 'Pedidos', in_array($actual, ['pedidos.php','pedido_detalle.php']), true); ?>
    <?= itemMenu('', 'facturacion.php', 'fa-file-invoice', 'Facturación', $actual === 'facturacion.php', true); ?>

    <span class="panel-menu__seccion">Análisis</span>
    <?= itemMenu('', 'reportes.php', 'fa-chart-line', 'Reportes', $actual === 'reportes.php', true); ?>
    <?= itemMenu('', 'soporte.php', 'fa-headset', 'Atención al cliente', $actual === 'soporte.php', true); ?>
    <?= itemMenu('', 'chat.php', 'fa-comments', 'Chat con clientes', $actual === 'chat.php', true); ?>
    <?= itemMenu('', 'auditoria.php', 'fa-clipboard-list', 'Auditoría', $actual === 'auditoria.php', true); ?>
    <?= itemMenu('', 'respaldo.php', 'fa-database', 'Respaldo de BD', $actual === 'respaldo.php', true); ?>

<?php elseif ($u['rol'] === 'empleado'): ?>
    <span class="panel-menu__seccion">Principal</span>
    <?= itemMenu('', 'index.php', 'fa-gauge', 'Dashboard', $actual === 'index.php'); ?>
    <?= itemMenu('', 'perfil.php', 'fa-user', 'Mi perfil', $actual === 'perfil.php'); ?>

    <span class="panel-menu__seccion">Operación</span>
    <?= itemMenu('', 'ventas.php', 'fa-cash-register', 'Registrar venta', $actual === 'ventas.php', true); ?>
    <?= itemMenu('../administrador/', 'pedidos.php', 'fa-cart-shopping', 'Pedidos', false, true); ?>
    <?= itemMenu('../administrador/', 'inventario.php', 'fa-warehouse', 'Inventario', false, true); ?>
    <?= itemMenu('', 'tareas.php', 'fa-list-check', 'Mis tareas', false, false); ?>

<?php elseif ($u['rol'] === 'cliente'): ?>
    <span class="panel-menu__seccion">Mi cuenta</span>
    <?= itemMenu('', 'index.php', 'fa-gauge', 'Dashboard', $actual === 'index.php'); ?>
    <?= itemMenu('', 'perfil.php', 'fa-user', 'Mi perfil', $actual === 'perfil.php'); ?>
    <?= itemMenu('', 'pedidos.php', 'fa-box-open', 'Mis pedidos', $actual === 'pedidos.php'); ?>

    <span class="panel-menu__seccion">Más</span>
    <?= itemMenu('', 'favoritos.php', 'fa-heart', 'Favoritos', $actual === 'favoritos.php', true); ?>
    <?= itemMenu('', 'chat.php', 'fa-comments', 'Chat con un asesor', $actual === 'chat.php', true); ?>
    <?= itemMenu('', '../../pages/productos.php', 'fa-shop', 'Ir a la tienda', false, true); ?>
<?php endif; ?>
</nav>
