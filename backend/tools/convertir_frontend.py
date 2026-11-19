"""
Genera el frontend estático (HTML) a partir de las plantillas PHP originales.

Aplica las reglas concretas de este proyecto para que el HTML conserva el
diseño original pero queda libre de PHP, listo para consumir la API REST.
"""
import pathlib
import re

ORIGEN = pathlib.Path(r"C:\xampp\htdocs\TecniServicios")
DESTINO = pathlib.Path(r"C:\Users\PC\Documents\TecniServicios\frontend")


def quitar_bloques_php(texto: str) -> str:
    texto = re.sub(r"<\?php.*?\?>", "", texto, flags=re.S)
    texto = re.sub(r"<\?=.*?\?>", "", texto, flags=re.S)
    return texto


def limpiar_atributos_php(fragmento: str) -> str:
    """Quita bloques PHP incrustados en fragmentos de HTML (carrito, modales)."""
    fragmento = quitar_bloques_php(fragmento)
    # Atributos vaciados por el bloque PHP: value=""  readonly  se dejan limpios
    fragmento = fragmento.replace(' value=""', "")
    fragmento = fragmento.replace(" readonly", "")
    fragmento = fragmento.replace(" style=\"display: none;\"", "")
    return fragmento


def entre(texto: str, inicio: str, fin: str) -> str:
    i = texto.index(inicio)
    j = texto.index(fin, i) + len(fin)
    return texto[i:j]


# ---------------------------------------------------------------------------
# index.html  (portada pública)
# ---------------------------------------------------------------------------
def index_html() -> str:
    origen = (ORIGEN / "index.php").read_text(encoding="utf-8-sig")

    hero = entre(origen, '  <section class="hero"', "</section>")
    confianza = entre(origen, '  <section class="confianza"', "</section>")
    nosotros = entre(origen, '  <section class="nosotros"', "</section>")
    contacto = entre(origen, '  <section class="contacto"', "</section>")

    servicios = """  <section class="servicios" id="servicios">
    <div class="contenedor">
      <p class="seccion__eyebrow">Lo que hacemos</p>
      <h2 class="seccion__titulo">Nuestros Servicios</h2>
      <p class="seccion__descripcion">Soluciones completas de seguridad electrónica, diseñadas y ejecutadas por técnicos certificados.</p>

      <div class="servicios__grid" id="listaServicios">
        <p class="cargando">Cargando servicios…</p>
      </div>
    </div>
  </section>"""

    destacados = """  <section class="productos-destacados">
    <div class="contenedor">
      <p class="seccion__eyebrow">Equipos recomendados</p>
      <h2 class="seccion__titulo">Productos Destacados</h2>

      <div class="productos__grid" id="listaDestacados">
        <p class="cargando">Cargando productos…</p>
      </div>

      <div class="ver-todos">
        <a href="pages/productos.html" class="btn btn--secundario">Ver catálogo completo</a>
      </div>
    </div>
  </section>"""

    return plantilla_pagina(
        titulo="TECNISECURE | Seguridad Electrónica y Cámaras",
        ruta_base="",
        contenido="\n\n".join([hero, confianza, servicios, destacados, nosotros, contacto]),
        scripts=["js/api.js", "js/tema.js", "js/main.js", "js/carrito.js", "js/deseos.js", "js/auth.js"],
    )


# ---------------------------------------------------------------------------
# pages/productos.html  (catálogo)
# ---------------------------------------------------------------------------
def productos_html() -> str:
    origen = (ORIGEN / "pages/productos.php").read_text(encoding="utf-8-sig")

    filtros = """  <section class="catalogo">
    <div class="contenedor catalogo__layout">

      <aside class="catalogo__filtros">
        <h3>Categorías</h3>
        <ul id="listaCategorias">
          <li><a href="#" class="activo">Todas <span class="catalogo__conteo">—</span></a></li>
        </ul>

        <button type="button" class="catalogo__ayuda" onclick="abrirModalAsesoria()" title="Solicitar asesoría para encontrar un equipo">
          <i class="fa-solid fa-headset"></i>
          <span>¿No encuentras lo que buscas? <strong>Te asesoramos</strong></span>
        </button>
      </aside>

      <div class="catalogo__productos">
        <div class="catalogo__barra">
          <form method="GET" action="productos.html" class="catalogo__buscador" id="formBuscarCatalogo">
            <i class="fa-solid fa-magnifying-glass"></i>
            <input type="text" name="buscar" id="buscarCatalogo" placeholder="Buscar cámaras, alarmas, sensores, accesorios...">
            <button type="submit">Buscar</button>
          </form>

          <div class="catalogo__barra-derecha">
            <p class="catalogo__resultado" id="resultadoCatalogo"><strong>0</strong> productos</p>

            <label class="catalogo__orden">
              <span>Ordenar por</span>
              <select id="ordenCatalogo">
                <option value="relevancia">Destacados primero</option>
                <option value="precio_asc">Precio: menor a mayor</option>
                <option value="precio_desc">Precio: mayor a menor</option>
                <option value="nombre">Nombre (A-Z)</option>
                <option value="nuevos">Más recientes</option>
              </select>
            </label>
          </div>
        </div>

        <div class="productos__grid" id="listaProductos">
          <p class="cargando">Cargando productos…</p>
        </div>
      </div>
    </div>
  </section>"""

    carrito = limpiar_atributos_php(entre(origen, "<!-- ============ PANEL LATERAL DEL CARRITO ============ -->", "</aside>"))
    asesoria = limpiar_atributos_php(entre(origen, "<!-- ============ MODAL ASESORÍA", "</div>\n</div>"))

    return plantilla_pagina(
        titulo="Productos | TECNISECURE",
        ruta_base="../",
        encabezado_seccion="""  <section class="pagina-header">
    <div class="contenedor">
      <p class="seccion__eyebrow">Catálogo</p>
      <h1 class="seccion__titulo">Nuestros Productos</h1>
      <p class="seccion__descripcion">Equipos de seguridad de alta calidad, listos para instalar. Agrega al carrito y solicita tu pedido en línea.</p>
    </div>
  </section>""",
        contenido="\n\n".join([filtros, carrito, asesoria]),
        scripts=["../js/api.js", "../js/tema.js", "../js/main.js", "../js/carrito.js",
                 "../js/deseos.js", "../js/auth.js", "../js/catalogo.js"],
    )


# ---------------------------------------------------------------------------
# Plantilla base (sustituye includes/header.php + includes/footer.php)
# ---------------------------------------------------------------------------
def plantilla_pagina(titulo: str, ruta_base: str, contenido: str,
                     scripts: list[str], encabezado_seccion: str = "") -> str:
    cuerpo = f"""{encabezado_seccion}

<main>
{contenido}
</main>"""

    return f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{titulo}</title>
<meta name="description" content="TECNISECURE: instalación de cámaras de seguridad, alarmas, control de acceso y monitoreo 24/7.">
<link rel="icon" href="{ruta_base}img/logo/logo.svg" type="image/svg+xml">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
<link rel="stylesheet" href="{ruta_base}css/style.css">
<link rel="stylesheet" href="{ruta_base}css/auth.css">
</head>
<body>
{barra_superior(ruta_base)}

{encabezado(ruta_base)}

{cuerpo}

{pie(ruta_base)}

<script>window.RUTA_BASE = "{ruta_base}";</script>
{chr(10).join(f'<script src="{s}"></script>' for s in scripts)}
</body>
</html>
"""


def barra_superior(ruta_base: str) -> str:
    return f"""<div class="top-bar">
  <div class="contenedor top-bar__inner">
    <div class="top-bar__contacto">
      <span><i class="fa-solid fa-phone"></i> +57 300 123 4567</span>
      <span><i class="fa-solid fa-envelope"></i> contacto@tecnicsecure.com</span>
    </div>
    <div class="top-bar__redes">
      <a href="https://facebook.com" target="_blank" aria-label="Facebook"><i class="fa-brands fa-facebook"></i></a>
      <a href="https://instagram.com" target="_blank" aria-label="Instagram"><i class="fa-brands fa-instagram"></i></a>
      <a href="https://wa.me/573001234567" target="_blank" aria-label="WhatsApp"><i class="fa-brands fa-whatsapp"></i></a>
      <a href="https://tiktok.com" target="_blank" aria-label="TikTok"><i class="fa-brands fa-tiktok"></i></a>
    </div>
  </div>
</div>"""


def encabezado(ruta_base: str) -> str:
    return f"""<header class="nav-principal" id="navPrincipal">
  <div class="contenedor nav-principal__inner">
    <a href="{ruta_base}index.html#inicio" class="logo">
      <img src="{ruta_base}img/logo/logo-claro.svg" alt="Logo TECNISECURE" class="logo__claro">
      <img src="{ruta_base}img/logo/logo.svg" alt="Logo TECNISECURE" class="logo__oscuro">
    </a>

    <button class="menu-toggle" id="menuToggle" aria-label="Abrir menú" aria-expanded="false">
      <span></span><span></span><span></span>
    </button>

    <nav class="menu" id="menuPrincipal">
      <ul>
        <li><a href="{ruta_base}index.html#inicio">Inicio</a></li>
        <li><a href="{ruta_base}index.html#servicios">Servicios</a></li>
        <li><a href="{ruta_base}pages/productos.html">Productos</a></li>
        <li><a href="{ruta_base}pages/lista_deseos.html">Lista de Compras</a></li>
        <li><a href="{ruta_base}index.html#contacto">Contacto</a></li>
      </ul>
      <a href="{ruta_base}pages/lista_deseos.html" class="btn-deseos" id="btnAbrirDeseos" title="Mi Lista de Compras">
        <i class="fa-solid fa-heart"></i>
        <span class="deseos-contador" id="deseosContador">0</span>
      </a>
      <a href="{ruta_base}pages/productos.html" class="btn-carrito" id="btnAbrirCarrito" title="Ver carrito">
        <i class="fa-solid fa-cart-shopping"></i>
        <span class="carrito-contador" id="carritoContador">0</span>
      </a>

      <button type="button" class="btn-tema" title="Cambiar tema claro/oscuro">
        <i class="fa-solid fa-moon"></i>
      </button>

      <a href="{ruta_base}login.html" class="btn-cuenta" id="btnCuenta">
        <i class="fa-solid fa-user"></i> Iniciar sesión
      </a>
    </nav>
  </div>
</header>"""


def pie(ruta_base: str) -> str:
    return f"""<footer class="footer" id="contacto-footer">
  <div class="contenedor footer__grid">
    <div class="footer__col">
      <span class="logo">
        <img src="{ruta_base}img/logo/logo-claro.svg" alt="TECNISECURE" class="footer__logo logo__claro">
        <img src="{ruta_base}img/logo/logo.svg" alt="TECNISECURE" class="footer__logo logo__oscuro">
      </span>
      <p>Más de 10 años protegiendo hogares y negocios con tecnología de vigilancia confiable, monitoreo 24/7 y soporte técnico especializado.</p>
      <div class="footer__redes">
        <a href="https://facebook.com" target="_blank" aria-label="Facebook"><i class="fa-brands fa-facebook"></i></a>
        <a href="https://instagram.com" target="_blank" aria-label="Instagram"><i class="fa-brands fa-instagram"></i></a>
        <a href="https://wa.me/573001234567" target="_blank" aria-label="WhatsApp"><i class="fa-brands fa-whatsapp"></i></a>
        <a href="https://tiktok.com" target="_blank" aria-label="TikTok"><i class="fa-brands fa-tiktok"></i></a>
        <a href="https://youtube.com" target="_blank" aria-label="YouTube"><i class="fa-brands fa-youtube"></i></a>
      </div>
    </div>

    <div class="footer__col">
      <h4>Enlaces</h4>
      <ul>
        <li><a href="{ruta_base}index.html#inicio">Inicio</a></li>
        <li><a href="{ruta_base}index.html#servicios">Servicios</a></li>
        <li><a href="{ruta_base}pages/productos.html">Productos</a></li>
        <li><a href="{ruta_base}pages/lista_deseos.html">Lista de Compras</a></li>
        <li><a href="{ruta_base}index.html#nosotros">Nosotros</a></li>
        <li><a href="{ruta_base}index.html#contacto">Contacto</a></li>
      </ul>
    </div>

    <div class="footer__col">
      <h4>Servicios</h4>
      <ul>
        <li>Instalación de cámaras CCTV</li>
        <li>Monitoreo remoto 24/7</li>
        <li>Alarmas y sensores</li>
        <li>Control de acceso biométrico</li>
        <li>Mantenimiento preventivo</li>
      </ul>
    </div>

    <div class="footer__col">
      <h4>Contáctanos</h4>
      <ul class="footer__contacto">
        <li><i class="fa-solid fa-location-dot"></i> Cra. 10 #20-30, Cartagena, Colombia</li>
        <li><i class="fa-solid fa-phone"></i> +57 300 123 4567</li>
        <li><i class="fa-solid fa-envelope"></i> contacto@tecnicsecure.com</li>
        <li><i class="fa-solid fa-clock"></i> Lun - Sáb: 8:00 am - 6:00 pm</li>
      </ul>
    </div>
  </div>

  <div class="footer__bottom">
    <p>&copy; 2026 TECNISECURE. Todos los derechos reservados.</p>
  </div>
</footer>

<a href="https://wa.me/573001234567" target="_blank" class="whatsapp-flotante" aria-label="Escríbenos por WhatsApp">
  <i class="fa-brands fa-whatsapp"></i>
</a>"""


def main() -> None:
    print("Generando frontend estático…")
    (DESTINO / "index.html").write_text(index_html(), encoding="utf-8", newline="")
    print("  index.html")
    (DESTINO / "pages").mkdir(parents=True, exist_ok=True)
    (DESTINO / "pages" / "productos.html").write_text(productos_html(), encoding="utf-8", newline="")
    print("  pages/productos.html")


if __name__ == "__main__":
    main()

