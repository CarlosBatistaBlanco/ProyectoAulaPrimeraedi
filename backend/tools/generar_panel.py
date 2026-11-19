"""
Genera el panel (administrador, empleado y cliente) como HTML estático.
Conserva el diseño original (sidebar, topbar, tarjetas y tablas) pero los
datos se cargan desde la API REST mediante panel.js / paginas-panel.js.
"""
import pathlib

DESTINO = pathlib.Path(r"C:\Users\PC\Documents\TecniServicios\frontend")


def menu_admin():
    items = [
        ("Principal", [
            ("index.html", "fa-gauge", "Dashboard"),
            ("usuarios.html", "fa-users", "Usuarios"),
            ("perfil.html", "fa-user", "Mi perfil"),
        ]),
        ("Operación", [
            ("empleados.html", "fa-id-badge", "Empleados"),
            ("clientes.html", "fa-user-tie", "Clientes"),
            ("productos.html", "fa-box", "Productos"),
            ("inventario.html", "fa-warehouse", "Inventario"),
            ("pedidos.html", "fa-cart-shopping", "Pedidos"),
            ("facturacion.html", "fa-file-invoice", "Facturación"),
        ]),
        ("Análisis", [
            ("reportes.html", "fa-chart-line", "Reportes"),
            ("soporte.html", "fa-headset", "Atención al cliente"),
            ("chat.html", "fa-comments", "Chat con clientes"),
            ("auditoria.html", "fa-clipboard-list", "Auditoría"),
            ("respaldo.html", "fa-database", "Respaldo de BD"),
        ]),
    ]
    return render_menu(items)


def menu_empleado():
    items = [
        ("Principal", [
            ("index.html", "fa-gauge", "Dashboard"),
            ("perfil.html", "fa-user", "Mi perfil"),
        ]),
        ("Operación", [
            ("ventas.html", "fa-cash-register", "Registrar venta"),
            ("../administrador/pedidos.html", "fa-cart-shopping", "Pedidos"),
            ("../administrador/inventario.html", "fa-warehouse", "Inventario"),
            ("../administrador/productos.html", "fa-box", "Productos"),
        ]),
    ]
    return render_menu(items)


def menu_cliente():
    items = [
        ("Mi cuenta", [
            ("index.html", "fa-gauge", "Dashboard"),
            ("perfil.html", "fa-user", "Mi perfil"),
            ("pedidos.html", "fa-box-open", "Mis pedidos"),
        ]),
        ("Más", [
            ("favoritos.html", "fa-heart", "Favoritos"),
            ("chat.html", "fa-comments", "Chat con un asesor"),
            ("../../pages/productos.html", "fa-shop", "Ir a la tienda"),
        ]),
    ]
    return render_menu(items)


def render_menu(grupos, actual="index.html"):
    html = ['<nav class="panel-menu">']
    for titulo, enlaces in grupos:
        html.append(f'    <span class="panel-menu__seccion">{titulo}</span>')
        for href, icono, texto in enlaces:
            activo = " activo" if href == actual else ""
            html.append(f'    <a href="{href}" class="panel-menu__item{activo}">'
                        f'<i class="fa-solid {icono}"></i> {texto}</a>')
    html.append("</nav>")
    return "\n".join(html)


def pagina_panel(rol: str, archivo: str, titulo: str, contenido: str, scripts_extra: list[str] = None) -> None:
    menus = {"administrador": menu_admin(), "empleado": menu_empleado(), "cliente": menu_cliente()}
    ruta_base = "../../"

    html = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{titulo} | TECNISECURE</title>
<link rel="icon" href="{ruta_base}img/logo/logo.svg" type="image/svg+xml">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
<link rel="stylesheet" href="{ruta_base}css/style.css">
<link rel="stylesheet" href="{ruta_base}css/auth.css">
<link rel="stylesheet" href="{ruta_base}css/panel.css">
</head>
<body class="panel-body" data-rol="{rol}">
<div class="panel-layout">

  <aside class="panel-sidebar" id="panelSidebar">
    <a href="{ruta_base}index.html" class="panel-sidebar__logo logo">
      <img src="{ruta_base}img/logo/logo-claro.svg" alt="TECNISECURE" class="logo__claro">
      <img src="{ruta_base}img/logo/logo.svg" alt="TECNISECURE" class="logo__oscuro">
    </a>
{menus[rol]}
  </aside>
  <div class="panel-overlay" id="panelOverlay"></div>

  <div class="panel-contenido">
    <header class="panel-topbar">
      <button class="panel-toggle" id="panelToggle" aria-label="Abrir menú"><i class="fa-solid fa-bars"></i></button>
      <h1 class="panel-topbar__titulo">{titulo}</h1>
      <div class="panel-topbar__usuario">
        <button type="button" class="btn-tema" title="Cambiar tema claro/oscuro">
          <i class="fa-solid fa-moon"></i>
        </button>

        <div class="panel-notificaciones">
          <button class="panel-noti-btn" id="botonNotificaciones" aria-label="Notificaciones">
            <i class="fa-solid fa-bell"></i>
            <span class="panel-noti-contador" id="notificacionesContador" style="display:none">0</span>
          </button>
          <div class="panel-noti-dropdown" id="dropdownNotificaciones">
            <div class="panel-noti-dropdown__header">
              <span>Notificaciones</span>
              <a href="#" id="marcarNotificaciones">Marcar todas como leídas</a>
            </div>
            <ul id="listaNotificaciones">
              <li class="vacio">Cargando notificaciones…</li>
            </ul>
          </div>
        </div>

        <span class="panel-topbar__nombre"><span data-nombre-usuario>…</span>
          <small data-rol-usuario></small>
        </span>
        <div class="panel-avatar" data-avatar-usuario>·</div>
        <a href="#" class="panel-logout" title="Cerrar sesión" onclick="cerrarSesionPanel(); return false;">
          <i class="fa-solid fa-right-from-bracket"></i>
        </a>
      </div>
    </header>

    <main class="panel-main">
{contenido}
    </main>
  </div>
</div>

<script>window.RUTA_BASE = "{ruta_base}";</script>
<script src="{ruta_base}js/api.js"></script>
<script src="{ruta_base}js/tema.js"></script>
<script src="{ruta_base}js/panel.js"></script>
{chr(10).join(f'<script src="{s}"></script>' for s in (scripts_extra or []))}
</body>
</html>
"""
    destino = DESTINO / "panel" / rol / archivo
    destino.parent.mkdir(parents=True, exist_ok=True)
    destino.write_text(html, encoding="utf-8", newline="")
    print(f"  panel/{rol}/{archivo}")


def tarjeta(valor_id: str, etiqueta: str, icono: str) -> str:
    return f"""  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid {icono}"></i></div>
    <div><div class="panel-stat-card__valor" id="{valor_id}">—</div>
      <div class="panel-stat-card__etiqueta">{etiqueta}</div></div>
  </div>"""


def main() -> None:
    print("Generando panel…")

    # ------------------------- Administrador -------------------------
    dashboard_admin = f"""<div class="panel-stats">
{tarjeta("statUsuarios", "Usuarios totales", "fa-users")}
{tarjeta("statClientes", "Clientes registrados", "fa-user-tie")}
{tarjeta("statProductos", "Productos activos", "fa-box")}
{tarjeta("statPedidos", "Pedidos totales", "fa-cart-shopping")}
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-sack-dollar"></i> Ventas acumuladas</h2>
  <p style="font-family:var(--fuente-titulo); font-size:1.8rem; color:var(--cian);" id="statVentas">—</p>
  <p style="color:#ffb199; margin-top:10px;" id="avisoStockBajo"></p>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-chart-simple"></i> Resumen del mes</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Indicador</th><th>Valor</th></tr></thead>
      <tbody id="tablaResumenMes"><tr><td colspan="2">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-cart-shopping"></i> Últimos pedidos</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Estado</th><th>Fecha</th></tr></thead>
      <tbody id="tablaUltimosPedidos"><tr><td colspan="5">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""
    pagina_panel("administrador", "index.html", "Dashboard", dashboard_admin,
                 ["../../js/paginas-panel.js"])

    # Cliente
    dashboard_cliente = """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-hand-wave"></i> Hola, <span data-nombre-usuario></span></h2>
  <p style="color:var(--texto-tenue);">Bienvenido a tu panel. Aquí puedes ver tus pedidos, tu perfil y tus productos favoritos.</p>
</div>

<div class="panel-stats" style="grid-template-columns: repeat(2, 1fr);">
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-box-open"></i></div>
    <div><div class="panel-stat-card__valor" id="statMisPedidos">—</div>
      <div class="panel-stat-card__etiqueta">Pedidos recientes</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-heart"></i></div>
    <div><div class="panel-stat-card__valor" id="statMisFavoritos">—</div>
      <div class="panel-stat-card__etiqueta">Productos favoritos</div></div>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-box-open"></i> Mis últimos pedidos</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Pedido</th><th>Fecha</th><th>Total</th><th>Estado</th></tr></thead>
      <tbody id="tablaMisPedidos"><tr><td colspan="4">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""
    pagina_panel("cliente", "index.html", "Mi Dashboard", dashboard_cliente,
                 ["../../js/paginas-panel.js"])

    # Empleado
    dashboard_empleado = """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-hand-wave"></i> Hola, <span data-nombre-usuario></span></h2>
  <p style="color:var(--texto-tenue);">Panel operativo: puedes registrar ventas, gestionar pedidos y revisar el inventario.</p>
</div>

<div class="panel-stats" style="grid-template-columns: repeat(3, 1fr);">
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-cart-shopping"></i></div>
    <div><div class="panel-stat-card__valor" id="statPedidosPendientes">—</div>
      <div class="panel-stat-card__etiqueta">Pedidos pendientes</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-warehouse"></i></div>
    <div><div class="panel-stat-card__valor" id="statStockBajo">—</div>
      <div class="panel-stat-card__etiqueta">Productos con stock bajo</div></div>
  </div>
  <div class="panel-stat-card">
    <div class="panel-stat-card__icono"><i class="fa-solid fa-sack-dollar"></i></div>
    <div><div class="panel-stat-card__valor" id="statVentasMes">—</div>
      <div class="panel-stat-card__etiqueta">Ventas del mes</div></div>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-cart-shopping"></i> Pedidos recientes</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Estado</th><th>Fecha</th></tr></thead>
      <tbody id="tablaUltimosPedidos"><tr><td colspan="5">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""
    pagina_panel("empleado", "index.html", "Dashboard", dashboard_empleado,
                 ["../../js/paginas-panel.js"])

    # Páginas compartidas de gestión (administrador y empleado)
    gestion = {
        "usuarios.html": ("Usuarios", "G",
                          """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-users"></i> Usuarios del sistema</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Nombre</th><th>Correo</th><th>Documento</th><th>Rol</th><th>Estado</th><th>Registrado</th></tr></thead>
      <tbody id="tablaUsuarios"><tr><td colspan="6">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""),
        "productos.html": ("Productos", "G",
                           """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-box"></i> Catálogo de productos</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>SKU</th><th>Producto</th><th>Categoría</th><th>Marca</th><th>Stock</th><th>Precio</th><th>Estado</th></tr></thead>
      <tbody id="tablaProductos"><tr><td colspan="7">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""),
        "pedidos.html": ("Pedidos", "G",
                         """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-cart-shopping"></i> Pedidos</h2>
  <p style="color:var(--texto-tenue); margin-bottom:14px;">Cambia el estado de un pedido según el flujo: pendiente → confirmado → en preparación → empacado → enviado → entregado.</p>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Estado</th><th>Fecha</th><th>Acciones</th></tr></thead>
      <tbody id="tablaPedidos"><tr><td colspan="6">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""),
        "inventario.html": ("Inventario", "G",
                            """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-triangle-exclamation"></i> Alertas de stock mínimo</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Producto</th><th>SKU</th><th>Stock</th><th>Mínimo</th><th>Máximo</th></tr></thead>
      <tbody id="tablaStockBajo"><tr><td colspan="5">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-warehouse"></i> Existencias</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Producto</th><th>SKU</th><th>Stock</th><th>Ajuste</th></tr></thead>
      <tbody id="tablaInventario"><tr><td colspan="4">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""),
        "reportes.html": ("Reportes", "G",
                          """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-chart-line"></i> Resumen del período</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Indicador</th><th>Valor</th></tr></thead>
      <tbody id="tablaResumen"><tr><td colspan="2">Cargando…</td></tr></tbody>
    </table>
  </div>
  <p style="margin-top:16px; display:flex; gap:12px; flex-wrap:wrap;">
    <button class="btn btn--primario" onclick="exportarVentas('csv')">
      <i class="fa-solid fa-file-csv"></i> Exportar ventas (CSV/Excel)
    </button>
    <button class="btn btn--secundario" onclick="exportarVentas('xml')">
      <i class="fa-solid fa-file-code"></i> Exportar ventas (XML)
    </button>
  </p>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-ranking-star"></i> Productos más vendidos</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>#</th><th>Producto</th><th>Unidades</th><th>Ingresos</th></tr></thead>
      <tbody id="tablaMasVendidos"><tr><td colspan="4">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""),
        "soporte.html": ("Atención al cliente", "G",
                         """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-headset"></i> Solicitudes de servicio</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>#</th><th>Servicio</th><th>Cliente</th><th>Tipo</th><th>Estado</th><th>Técnico</th></tr></thead>
      <tbody id="tablaSolicitudes"><tr><td colspan="6">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>

<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-envelope"></i> Mensajes de contacto</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>#</th><th>Nombre</th><th>Correo</th><th>Asunto</th><th>Tipo</th><th>Fecha</th></tr></thead>
      <tbody id="tablaContactos"><tr><td colspan="6">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""),
        "auditoria.html": ("Auditoría", "G",
                           """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-clipboard-list"></i> Registro de actividad</h2>
  <p style="color:var(--texto-tenue);">Ejecuta <code>GET /api/auditoria</code> desde Swagger para consultar el historial completo de acciones registradas.</p>
</div>"""),
        "respaldo.html": ("Respaldo de BD", "G",
                          """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-database"></i> Respaldo de la base de datos</h2>
  <p style="color:var(--texto-tenue);">La base de datos es un archivo H2 ubicado en
    <code>backend/data/tecnicsecure.mv.db</code>. Para respaldarla basta con copiar ese archivo
    (con el servidor detenido) o descargar los reportes en CSV desde el módulo de Reportes.</p>
</div>"""),
        "facturacion.html": ("Facturación", "G",
                             """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-file-invoice"></i> Facturación</h2>
  <p style="color:var(--texto-tenue);">Las facturas se generan a partir de los pedidos confirmados. Consulta
    <code>GET /api/pedidos</code> para el detalle y usa Reportes para exportar.</p>
</div>"""),
        "empleados.html": ("Empleados", "G",
                           """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-id-badge"></i> Empleados</h2>
  <p style="color:var(--texto-tenue);">Consulta los usuarios con rol <strong>empleado</strong> desde el módulo de Usuarios.</p>
</div>"""),
        "clientes.html": ("Clientes", "G",
                          """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-user-tie"></i> Clientes</h2>
  <p style="color:var(--texto-tenue);">Consulta los usuarios con rol <strong>cliente</strong> desde el módulo de Usuarios.</p>
</div>"""),
        "chat.html": ("Chat con clientes", "G",
                      """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-comments"></i> Conversaciones</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Cliente</th><th>Correo</th><th>Abrir</th></tr></thead>
      <tbody id="tablaChats"><tr><td colspan="3">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>"""),
    }

    for archivo, (titulo, _, contenido) in gestion.items():
        pagina_panel("administrador", archivo, titulo, contenido,
                     ["../../js/paginas-panel.js"])

    # Empleado: registrar venta y perfil
    pagina_panel("empleado", "ventas.html", "Registrar venta", """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-cash-register"></i> Registrar una venta manual</h2>
  <p style="color:var(--texto-tenue);">Usa el catálogo para armar el pedido; el stock se descuenta automáticamente
    al confirmar y queda registrado en el kardex.</p>
  <p><a class="btn btn--primario" href="../../pages/productos.html"><i class="fa-solid fa-shop"></i> Ir al catálogo</a></p>
</div>""", ["../../js/paginas-panel.js"])

    # Perfil (los tres roles)
    perfil = """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-user"></i> Mi perfil</h2>
  <form id="formPerfil" class="auth-form" style="max-width:520px;">
    <div class="campo">
      <label for="nombre">Nombre completo</label>
      <input type="text" id="nombre" name="nombre" required>
    </div>
    <div class="campo">
      <label for="correo">Correo electrónico</label>
      <input type="email" id="correo" name="correo" disabled>
    </div>
    <div class="campo">
      <label for="telefono">Teléfono</label>
      <input type="tel" id="telefono" name="telefono">
    </div>
    <div class="campo">
      <label for="passwordActual">Contraseña actual (solo si vas a cambiarla)</label>
      <input type="password" id="passwordActual" name="passwordActual" placeholder="••••••••">
    </div>
    <div class="campo">
      <label for="passwordNueva">Nueva contraseña</label>
      <input type="password" id="passwordNueva" name="passwordNueva" placeholder="Mínimo 8 caracteres, 1 mayúscula y 1 número">
    </div>
    <button type="submit" class="btn btn--primario btn--full">
      <i class="fa-solid fa-floppy-disk"></i> Guardar cambios
    </button>
    <p class="form-respuesta" id="respuestaPerfil"></p>
  </form>
</div>"""
    for rol in ("administrador", "empleado", "cliente"):
        pagina_panel(rol, "perfil.html", "Mi perfil", perfil, ["../../js/paginas-panel.js"])

    # Cliente: pedidos, favoritos, chat
    pagina_panel("cliente", "pedidos.html", "Mis pedidos", """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-box-open"></i> Historial de pedidos</h2>
  <div class="panel-tabla-wrap">
    <table class="panel-tabla">
      <thead><tr><th>Pedido</th><th>Fecha</th><th>Total</th><th>Estado</th><th>Detalle</th></tr></thead>
      <tbody id="tablaMisPedidos"><tr><td colspan="5">Cargando…</td></tr></tbody>
    </table>
  </div>
</div>""", ["../../js/paginas-panel.js"])

    pagina_panel("cliente", "favoritos.html", "Favoritos", """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-heart"></i> Mis productos favoritos</h2>
  <div class="productos__grid" id="listaFavoritos">
    <p class="cargando">Cargando favoritos…</p>
  </div>
</div>""", ["../../js/paginas-panel.js"])

    pagina_panel("cliente", "chat.html", "Chat con un asesor", """<div class="panel-box">
  <h2 class="panel-box__titulo"><i class="fa-solid fa-comments"></i> Conversación con soporte</h2>
  <div class="chat-mensajes" id="chatMensajes" data-rol="cliente" style="max-height:420px; overflow-y:auto;">
    <p style="color:var(--texto-tenue);">Cargando conversación…</p>
  </div>
  <form id="formChat" class="chat-form" style="display:flex; gap:10px; margin-top:14px;">
    <input type="text" id="mensajeChat" placeholder="Escribe tu mensaje…" autocomplete="off"
           style="flex:1; padding:12px; border-radius:10px; border:1px solid var(--borde); background:var(--fondo-input); color:var(--texto);">
    <button type="submit" class="btn btn--primario"><i class="fa-solid fa-paper-plane"></i> Enviar</button>
  </form>
</div>""", ["../../js/paginas-panel.js"])


if __name__ == "__main__":
    main()
