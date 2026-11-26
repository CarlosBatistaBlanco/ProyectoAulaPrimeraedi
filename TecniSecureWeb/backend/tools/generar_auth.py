"""
Genera las páginas de autenticación estáticas (login, registro, recuperar,
restablecer) a partir de las plantillas PHP originales.
"""
import pathlib
import re

ORIGEN = pathlib.Path(r"C:\xampp\htdocs\TecniServicios")
DESTINO = pathlib.Path(r"C:\Users\PC\Documents\TecniServicios\frontend")


def sin_php(texto: str) -> str:
    texto = re.sub(r"<\?php.*?\?>", "", texto, flags=re.S)
    texto = re.sub(r"<\?=.*?\?>", "", texto, flags=re.S)
    return texto


def cuerpo_entre(html: str, inicio: str, fin: str) -> str:
    i = html.index(inicio)
    j = html.index(fin, i)
    return html[i:j]


def auth_layout(titulo: str, contenido: str, ruta_base: str = "") -> str:
    return f"""<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{titulo}</title>
<link rel="icon" href="{ruta_base}img/logo/logo.svg" type="image/svg+xml">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
<link rel="stylesheet" href="{ruta_base}css/style.css">
<link rel="stylesheet" href="{ruta_base}css/auth.css">
</head>
<body class="auth-body">
<div class="auth-wrapper">
  <div style="display:flex; justify-content:center; align-items:center; gap:14px; margin-bottom:22px;">
    <a href="{ruta_base}index.html" class="auth-logo logo" style="margin-bottom:0;">
      <img src="{ruta_base}img/logo/logo-claro.svg" alt="TECNISECURE" class="logo__claro">
      <img src="{ruta_base}img/logo/logo.svg" alt="TECNISECURE" class="logo__oscuro">
    </a>
    <button type="button" class="btn-tema" title="Cambiar tema claro/oscuro">
      <i class="fa-solid fa-moon"></i>
    </button>
  </div>
  <div class="auth-card">
{contenido}
  </div>
  <p class="auth-volver"><a href="{ruta_base}index.html"><i class="fa-solid fa-arrow-left"></i> Volver al sitio</a></p>
</div>
<script src="{ruta_base}js/api.js"></script>
<script src="{ruta_base}js/auth.js"></script>
<script src="{ruta_base}js/tema.js"></script>
</body>
</html>
"""


def convertir_login() -> str:
    html = self_origen = (ORIGEN / "login.php").read_text(encoding="utf-8-sig")
    contenido = cuerpo_entre(html, '<h1 class="auth-titulo">', "<?php require_once __DIR__ . '/includes/auth_footer.php'; ?>")
    contenido = sin_php(contenido)

    # El captcha ahora lo genera la API: se pide al cargar la página
    contenido = contenido.replace(
        '<label for="captcha">Verificación: ¿cuánto es ?</label>',
        '<label for="captcha">Verificación: <span id="preguntaCaptcha">¿cuánto es 3 + 4?</span></label>')
    contenido = contenido.replace(
        'label for="captcha">Verificación: ¿cuánto es ?</label>',
        'label for="captcha">Verificación: <span id="preguntaCaptcha">¿cuánto es 3 + 4?</span></label>')
    contenido = re.sub(r'(<label for="captcha">).*?(</label>)',
                       r'\1Verificación: <span id="preguntaCaptcha">¿cuánto es 3 + 4?</span>\2',
                       contenido, flags=re.S)

    contenido = contenido.replace('<form action="php/procesar_login.php" method="POST" class="auth-form" id="formLogin">',
                                  '<form action="#" method="POST" class="auth-form" id="formLogin">')
    contenido = contenido.replace('<a href="recuperar.php">', '<a href="recuperar.html">')
    contenido = contenido.replace('<a href="registro.php">Regístrate aquí</a>',
                                  '<a href="registro.html">Regístrate aquí</a>')
    contenido = contenido.replace('<input type="hidden" name="redir" value="">', '')

    return auth_layout("Iniciar sesión | TECNISECURE", contenido)


def convertir_registro() -> str:
    html = sin_php((ORIGEN / "registro.php").read_text(encoding="utf-8-sig"))
    contenido = cuerpo_entre(html, '<h1 class="auth-titulo">', '</form>') + "</form>"
    contenido = contenido.replace('action="php/procesar_registro.php"', 'action="#"')
    contenido = re.sub(r'<label for="captcha">.*?</label>', '', contenido, flags=re.S)
    contenido = re.sub(r'<div class="campo campo-captcha">.*?</div>\s*</div>', '', contenido, flags=re.S, count=1)
    contenido = contenido.replace('<a href="login.php">', '<a href="login.html">')
    return auth_layout("Crear cuenta | TECNISECURE", contenido)


def convertir_recuperar() -> str:
    html = sin_php((ORIGEN / "recuperar.php").read_text(encoding="utf-8-sig"))
    contenido = cuerpo_entre(html, '<h1 class="auth-titulo">', "<?php require_once")
    contenido = contenido.replace('action="php/procesar_recuperar.php"', 'action="#"')
    contenido = contenido.replace('<a href="login.php">', '<a href="login.html">')

    aviso = """  <div class="auth-alerta auth-alerta--info" id="enlaceRecuperacion" style="display:none">
    <b>Modo de prueba (sin servidor de correo configurado):</b><br>
    En un sitio en producción este enlace llegaría a tu correo. Por ahora, haz clic aquí para continuar:<br>
    <a href="#" id="enlaceRestablecer">Restablecer mi contraseña</a>
  </div>
"""
    return auth_layout("Recuperar contraseña | TECNISECURE", aviso + contenido)


def convertir_restablecer() -> str:
    html = sin_php((ORIGEN / "restablecer.php").read_text(encoding="utf-8-sig"))

    # El formulario vive dentro de un bloque "if ($tokenValido)"; se extrae directamente.
    i = html.index('<p class="auth-subtitulo">Escribe tu nueva contraseña.</p>')
    j = html.index("</form>", i) + len("</form>")
    contenido = html[i:j]
    contenido = contenido.replace('action="php/procesar_restablecer.php"', 'action="#"')
    contenido = contenido.replace('<input type="hidden" name="token" value="">', '')
    contenido = contenido.replace('<a href="recuperar.php">', '<a href="recuperar.html">')

    cabecera = '<h1 class="auth-titulo"><i class="fa-solid fa-lock-open"></i> Nueva contraseña</h1>\n'
    return auth_layout("Restablecer contraseña | TECNISECURE", cabecera + contenido)


def main() -> None:
    print("Generando páginas de autenticación…")
    for nombre, funcion in [
        ("login.html", convertir_login),
        ("registro.html", convertir_registro),
        ("recuperar.html", convertir_recuperar),
        ("restablecer.html", convertir_restablecer),
    ]:
        try:
            (DESTINO / nombre).write_text(funcion(), encoding="utf-8", newline="")
            print(f"  {nombre}")
        except Exception as ex:  # noqa: BLE001
            print(f"  ERROR en {nombre}: {ex}")


if __name__ == "__main__":
    main()
