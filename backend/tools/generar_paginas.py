"""
Genera las páginas públicas restantes del frontend estático:
  · solicitar-servicio.html
  · pages/lista_deseos.html
  · panel/**  (index de cada rol, pedidos, favoritos, chat, perfil)
"""
import pathlib
import re

ORIGEN = pathlib.Path(r"C:\xampp\htdocs\TecniServicios")
DESTINO = pathlib.Path(r"C:\Users\PC\Documents\TecniServicios\frontend")


def limpiar(texto: str) -> str:
    texto = re.sub(r"<\?php.*?\?>", "", texto, flags=re.S)
    texto = re.sub(r"<\?=.*?\?>", "", texto, flags=re.S)
    texto = texto.replace(' value=""', "").replace(" readonly", "")
    texto = re.sub(r"<\?= date\('Y-m-d'\); \?>", "2026-01-01", texto)
    return texto


def entre(texto: str, inicio: str, fin: str) -> str:
    i = texto.index(inicio)
    j = texto.index(fin, i)
    return texto[i:j]


def main() -> None:
    origen = limpiar((ORIGEN / "solicitar-servicio.php").read_text(encoding="utf-8-sig"))

    cabecera = """  <section class="pagina-header">
    <div class="contenedor">
      <p class="seccion__eyebrow">Servicios</p>
      <h1 class="seccion__titulo" id="tituloServicio">Solicitar servicio</h1>
      <p class="seccion__descripcion" id="descripcionServicio">Cargando información del servicio…</p>
    </div>
  </section>"""

    formulario = entre(origen, '  <section class="contacto" style="padding-top:0;">',
                       "  </section>\n</main>").rstrip()
    formulario = formulario.replace('action="php/procesar_solicitud_servicio.php"', 'action="#"')
    formulario = formulario.replace('<input type="hidden" name="id_servicio" value="">', '')
    formulario = formulario.replace('href="index.php#servicios"', 'href="../index.html#servicios"')

    html = f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Solicitar servicio | TECNISECURE</title>
<link rel="icon" href="../img/logo/logo.svg" type="image/svg+xml">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
<link rel="stylesheet" href="../css/style.css">
<link rel="stylesheet" href="../css/auth.css">
</head>
<body>

<div class="top-bar">
  <div class="contenedor top-bar__inner">
    <div class="top-bar__contacto">
      <span><i class="fa-solid fa-phone"></i> +57 300 123 4567</span>
      <span><i class="fa-solid fa-envelope"></i> contacto@tecnicsecure.com</span>
    </div>
  </div>
</div>

<header class="nav-principal" id="navPrincipal">
  <div class="contenedor nav-principal__inner">
    <a href="../index.html#inicio" class="logo">
      <img src="../img/logo/logo-claro.svg" alt="Logo TECNISECURE" class="logo__claro">
      <img src="../img/logo/logo.svg" alt="Logo TECNISECURE" class="logo__oscuro">
    </a>
    <button class="menu-toggle" id="menuToggle" aria-label="Abrir menú" aria-expanded="false">
      <span></span><span></span><span></span>
    </button>
    <nav class="menu" id="menuPrincipal">
      <ul>
        <li><a href="../index.html#inicio">Inicio</a></li>
        <li><a href="../index.html#servicios">Servicios</a></li>
        <li><a href="productos.html">Productos</a></li>
        <li><a href="lista_deseos.html">Lista de Compras</a></li>
        <li><a href="../index.html#contacto">Contacto</a></li>
      </ul>
      <button type="button" class="btn-tema" title="Cambiar tema claro/oscuro"><i class="fa-solid fa-moon"></i></button>
      <a href="../login.html" class="btn-cuenta" id="btnCuenta"><i class="fa-solid fa-user"></i> Iniciar sesión</a>
    </nav>
  </div>
</header>

<main>
{cabecera}

{formulario}
</main>

<footer class="footer">
  <div class="footer__bottom">
    <p>&copy; 2026 TECNISECURE. Todos los derechos reservados.</p>
  </div>
</footer>

<script>window.RUTA_BASE = "../";</script>
<script src="../js/api.js"></script>
<script src="../js/tema.js"></script>
<script src="../js/main.js"></script>
<script src="../js/auth-paginas.js"></script>
<script src="../js/solicitud.js"></script>
</body>
</html>
"""
    (DESTINO / "pages" / "solicitar-servicio.html").write_text(html, encoding="utf-8", newline="")
    print("  pages/solicitar-servicio.html")


if __name__ == "__main__":
    main()
