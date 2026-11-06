# TECNISECURE — Sitio web de Seguridad y Cámaras

Sitio web completo: HTML + CSS + PHP + JavaScript + MySQL.
Incluye menú interactivo, carrito de compras, formulario de contacto
conectado a base de datos, catálogo de productos, y video de encabezado.

## 1. Estructura de carpetas

```
TecniSecure/
├── index.php                 → Página principal (hero, servicios, contacto)
├── pages/
│   └── productos.php         → Catálogo completo + carrito de compras
├── includes/
│   ├── header.php            → <head>, menú de navegación
│   └── footer.php            → Footer, redes sociales, scripts
├── php/
│   ├── conexion.php          → Conexión PDO a MySQL
│   ├── procesar_contacto.php → Guarda mensajes del formulario de contacto
│   └── procesar_pedido.php   → Guarda pedidos del carrito de compras
├── css/
│   └── style.css             → Todos los estilos del sitio
├── js/
│   ├── main.js                → Menú, reloj del HUD, envío de contacto
│   └── carrito.js             → Lógica del carrito de compras
├── img/
│   ├── logo/logo.svg           → Logo para fondo oscuro
│   ├── logo/logo-claro.svg     → Logo para fondo claro
│   ├── productos/              → Fotos de productos
│   └── servicios/               → Fondo del hero:
│                                  poster-hero.jpg        (tema oscuro, foto nocturna)
│                                  poster-hero-claro.jpg  (tema claro, lente CCTV)
├── video/
│   └── encabezado.mp4          → (agrégalo tú) video del encabezado
├── audio/                      → Carpeta lista para sonidos/alertas futuras
├── uploads/productos/          → Carpeta para subir fotos nuevas de productos
└── sql/
    └── tecnicsecure.sql      → Script para crear la base de datos
```

## 2. Instalación paso a paso (XAMPP + VS Code + MySQL Workbench)

### Paso 1 — Copiar el proyecto a XAMPP
1. Abre la carpeta donde instalaste XAMPP (normalmente `C:\xampp\htdocs`).
2. Copia toda la carpeta `TecniSecure` dentro de `htdocs`.
   Debe quedar así: `C:\xampp\htdocs\TecniSecure\index.php`

### Paso 2 — Encender Apache y MySQL
1. Abre el **Panel de control de XAMPP**.
2. Da clic en **Start** en el módulo **Apache**.
3. Da clic en **Start** en el módulo **MySQL**.
4. Ambos deben quedar en verde.

### Paso 3 — Crear la base de datos
Tienes dos formas, usa la que prefieras:

**Opción A: con phpMyAdmin (más fácil, viene con XAMPP)**
1. Abre tu navegador y entra a: `http://localhost/phpmyadmin`
2. Da clic en la pestaña **Importar**.
3. Elige el archivo `sql/tecnicsecure.sql` (dentro de la carpeta del proyecto).
4. Da clic en **Continuar / Importar**. Esto crea la base `TecniSecure`
   con todas sus tablas y datos de ejemplo (productos y servicios).

**Opción B: con MySQL Workbench**
1. Abre MySQL Workbench y conéctate a tu servidor local (`127.0.0.1`, usuario `root`).
2. Ve a **File > Open SQL Script** y selecciona `sql/tecnicsecure.sql`.
3. Ejecuta todo el script con el ícono del rayo ⚡ (Execute).

> 💡 Si en vez de phpMyAdmin/Workbench prefieres importar por línea de
> comandos, usa: `mysql --default-character-set=utf8mb4 -u root < sql/tecnicsecure.sql`
> (así los acentos de nombres y descripciones no se dañan).

### Paso 4 — Verificar la conexión
Abre el archivo `php/conexion.php` en Visual Studio Code. Si tu MySQL
tiene contraseña (no es el caso por defecto en XAMPP), cambia esta línea:

```php
define('DB_PASS', ''); // coloca aquí tu contraseña si tienes una
```

#### Si usas MySQL 8 (MySQL Server + Workbench) en lugar de MariaDB de XAMPP

MySQL 8 autentica con `caching_sha2_password`, que PHP no sabe leer
(error `Plugin caching_sha2_password could not be loaded`). Por eso
`php/conexion.php` usa un **usuario dedicado**, no `root`:

```php
define('DB_USER', 'TecniSecure');
define('DB_PASS', 'TecniServ2026*');
```

Para crearlo (una sola vez, desde MySQL Workbench o la consola):

```sql
CREATE USER 'TecniSecure'@'localhost'
  IDENTIFIED WITH mysql_native_password BY 'TecniServ2026*';
GRANT ALL PRIVILEGES ON TecniSecure.* TO 'TecniSecure'@'localhost';
FLUSH PRIVILEGES;
```

Así no hay que cambiar la contraseña de `root` ni el plugin de
autenticación del servidor.

### Paso 5 — Abrir el sitio en el navegador
Con Apache y MySQL encendidos, ve a:

```
http://localhost/TecniSecure/
```

¡Listo! Ya deberías ver la página principal funcionando, con productos y
servicios cargados directamente desde la base de datos.

> ⚠️ Importante: este proyecto usa PHP, así que **no funciona** si solo
> abres el archivo `index.php` haciendo doble clic (eso lo abriría como
> texto). Siempre debes acceder por `http://localhost/...` con Apache
> encendido, para que el servidor procese el PHP correctamente.

## 3. Tema claro y oscuro

El sitio abre en **tema claro** (paleta "Cielo Nieve"). El botón de
sol/luna del menú cambia a **tema oscuro**; la preferencia se guarda en
el navegador de cada visitante.

Todo el color se controla con variables CSS en `:root` dentro de
`css/style.css`:

```css
:root { /* tema CLARO (por defecto) */ }
html[data-tema="oscuro"] { /* tema OSCURO */ }
```

### Regla importante al agregar estilos nuevos

Usa **siempre** las variables, nunca colores fijos:

| Correcto | Incorrecto |
|---|---|
| `color: var(--cian)` | `color: #00e5ff` |
| `background: rgba(var(--cian-rgb), .1)` | `background: rgba(0,229,255,.1)` |
| `color: var(--sobre-acento)` | `color: #06131f` |

¿Por qué? El cian brillante `#00e5ff` sobre fondo claro da un contraste
de **1.4:1** (prácticamente invisible). En tema claro `--cian` se
redefine a `#006b85`, que mantiene la identidad de marca y cumple
WCAG AA (**5.8:1**). Si escribes el color fijo, el elemento se rompe en
uno de los dos temas.

`--sobre-acento` es el color del texto que va **encima** del cian
(oscuro en tema oscuro, blanco en tema claro).

### Tipografía

Todo el sitio usa **Arial** (`--fuente-titulo` y `--fuente-texto`). No
carga fuentes externas, así que funciona sin conexión.

### Imágenes que cambian con el tema

Dos recursos tienen versión para cada tema y se alternan por CSS:

| Recurso | Tema claro | Tema oscuro |
|---|---|---|
| Logo | `logo-claro.svg` (texto oscuro) | `logo.svg` (texto claro) |
| Fondo del hero | `poster-hero-claro.jpg` | `poster-hero.jpg` |

Si cambias de tema y ves el logo duplicado o una imagen vieja, es caché
del navegador. El proyecto ya agrega `?v=<fecha>` a los CSS para
invalidarla automáticamente; si persiste, usa **Ctrl + F5**.

> Si algún día agregas `video/encabezado.mp4`, el video real se verá
> igual en ambos temas. Solo el *póster* (la imagen de espera) cambia.

## 4. Cómo agregar tu propio video de encabezado
1. Consigue un video corto en formato `.mp4` (10-20 segundos, en bucle).
2. Renómbralo a `encabezado.mp4`.
3. Colócalo dentro de la carpeta `video/`.
4. Recarga la página — el video se reproducirá automáticamente detrás
   del texto "TECNISECURE".

## 5. Cómo agregar o editar productos
Puedes hacerlo directo en phpMyAdmin (tabla `productos`) o con una consulta:

```sql
INSERT INTO productos (id_categoria, nombre, descripcion, precio, stock, imagen, destacado)
VALUES (1, 'Cámara PTZ 360°', 'Cámara motorizada con seguimiento automático.', 599900, 8, 'img/productos/ptz.jpg', 1);
```

Guarda la foto del producto dentro de `img/productos/` (o `uploads/productos/`)
con el mismo nombre que pusiste en el campo `imagen`.

## 6. Cómo funciona el carrito de compras
- Cada botón **"Agregar"** guarda el producto en el navegador (localStorage),
  así que el carrito no se pierde aunque recargues la página.
- El panel lateral del carrito se abre desde el ícono 🛒 del menú, dentro
  de la página `pages/productos.php`.
- Al dar clic en **"Confirmar pedido"**, los datos del cliente y el carrito
  se envían a `php/procesar_pedido.php`, que:
  1. Crea (o reutiliza) el cliente en la tabla `clientes`.
  2. Crea el pedido en `pedidos`.
  3. Guarda cada producto comprado en `pedido_detalle`.
  4. Descuenta el stock disponible.

## 7. Formulario de contacto
Los mensajes enviados desde la sección **Contacto** del `index.php` se
guardan en la tabla `contactos`, y puedes revisarlos en phpMyAdmin o con:

```sql
SELECT * FROM contactos ORDER BY creado_en DESC;
```

## 8. Personalizar colores y textos
- Colores: edita las variables de `css/style.css`. Hay dos bloques: `:root` (tema claro) y `html[data-tema="oscuro"]` (tema oscuro). Ver la sección 3.
- Textos de servicios: se cargan desde la tabla `servicios` en la base de datos.
- Redes sociales: cambia los enlaces `https://facebook.com`, etc., en
  `includes/header.php` y `includes/footer.php` por tus páginas reales.
- Teléfono/WhatsApp: busca `+57 300 123 4567` y `573001234567` en
  `includes/header.php`, `includes/footer.php` e `index.php`, y reemplázalos.

## 9. Fase 1: Autenticación, roles y paneles

El sitio ahora tiene login, registro y tres paneles según el rol del usuario.

**Cuenta de administrador de prueba** (ya viene creada al importar el SQL):
- Correo: `admin@TecniSecure.com`
- Contraseña: `Admin123!`

**Páginas nuevas:**
- `login.php` / `registro.php` / `recuperar.php` / `restablecer.php` / `logout.php`
- `panel/administrador/` — dashboard con estadísticas + gestión de usuarios (crear, activar/desactivar, bloquear, cambiar rol, resetear contraseña) + atención al cliente (solicitudes de servicio)
- `panel/empleado/` — dashboard (los módulos operativos llegan en las próximas fases)
- `panel/cliente/` — dashboard, mi perfil, mis pedidos

**Servicios ahora son funcionales:** cada tarjeta de servicio en el inicio tiene un botón "Solicitar este servicio" que lleva a un formulario real donde el visitante elige entre pedir una **cotización** o **agendar una cita técnica** (con fecha y hora). Todo queda guardado en la tabla `solicitudes_servicio` y es visible/gestionable desde `panel/administrador/soporte.php`.

**Sobre el envío de correos:** el sistema de recuperación de contraseña genera el enlace correctamente, pero en vez de enviarlo por correo real (que requiere credenciales SMTP que no tengo cómo generar por ti), lo muestra directamente en pantalla para que puedas probar el flujo completo. Configurar el envío real de correos es la Fase 5 del proyecto.

**Sobre el CAPTCHA:** es una pregunta matemática simple ("¿Cuánto es 4 + 7?") generada en el servidor. Se cambió a propósito de una imagen generada con GD a esto, porque la imagen dependía de que el servidor tuviera la extensión GD con soporte de fuentes TTF bien configurado — eso variaba entre instalaciones de XAMPP y causaba errores al iniciar sesión. La pregunta matemática funciona en el 100% de los casos, sin ninguna extensión especial.

## 9.1 Registro de correcciones

- **Se corrigió el script SQL para que SIEMPRE borre e importe la base de datos desde cero** (`DROP DATABASE IF EXISTS` al inicio). Antes, si ya tenías una base de datos parcial de un intento anterior, el script se detenía a mitad de camino con un error de "la tabla ya existe" (en MySQL Workbench esto detiene TODO el resto del script), dejando tablas nuevas como `resenas` o `favoritos` sin crear — eso causaba el error "Table 'TecniSecure.resenas' doesn't exist".
- **Vuelve a importar `sql/tecnicsecure.sql` completo** (phpMyAdmin > Importar, o Workbench > Open SQL Script > Execute) para aplicar esta corrección. Si ya tenías datos reales, respáldalos antes desde `panel/administrador/respaldo.php`.
- Se agregó `reparar_admin.php` en la raíz del proyecto: una herramienta de emergencia que restablece el usuario administrador directamente si el login sigue sin funcionar después de reimportar. Ábrelo en el navegador (`http://localhost/TecniSecure/reparar_admin.php`), lee el mensaje, e **bórralo después de usarlo**.
- Se corrigió un error fatal (`mb_substr` no disponible) que rompía el formulario de contacto al enviar un reclamo o sugerencia.
- Se reemplazó el captcha de imagen (GD/TTF) por una pregunta matemática, ya que el captcha de imagen impedía iniciar sesión en instalaciones de XAMPP sin soporte de fuentes TTF en GD.
- Se reemplazó `utf8_decode()` (obsoleta desde PHP 8.2) por una función propia (`texto_pdf()`) en la generación de facturas y reportes en PDF.
- Se corrigió el orden de inclusión de `php/auth.php` en `pages/productos.php`, que causaba una página en blanco.
- Las consultas a tablas nuevas (`resenas`, `favoritos`, `notificaciones`, `pedidos_historial`, `facturas`) ahora están protegidas con `try/catch`: si por cualquier motivo una de esas tablas faltara, la página sigue funcionando (esa función puntual simplemente se desactiva sola) en vez de romperse por completo.
- **Se corrigió un bug real de PDO en varios formularios de búsqueda y en la creación de productos**: se usaba el mismo parámetro con nombre (ej. `:b`) dos veces en una misma consulta, lo cual causa `PDOException: SQLSTATE[HY093] Invalid parameter number` con consultas preparadas reales (`PDO::ATTR_EMULATE_PREPARES => false`). Afectaba a la búsqueda de usuarios, clientes, productos, y a la creación de productos. Corregido usando un nombre de parámetro distinto por cada aparición.
- Se agregó `verificar_instalacion.php` en la raíz del proyecto: una herramienta de diagnóstico que revisa de un vistazo si faltan archivos o tablas en tu instalación.
- Se agregó un comentario de encabezado a los 43 archivos PHP que no lo tenían, explicando qué hace cada uno.

## 9.2 Funciones nuevas: cambio de cuenta, foto de perfil y modo claro/oscuro

**Suplantación de cuenta (solo administrador):** desde `panel/administrador/usuarios.php`, el ícono 🕵 junto a cada usuario permite "iniciar sesión como" esa persona SIN cerrar tu propia sesión de administrador — útil para dar soporte viendo exactamente lo que ve el usuario. Pide un código de verificación secreto (definido en `php/conexion.php`, constante `CODIGO_SUPLANTACION` — **cámbialo antes de usar el sitio en producción**). Mientras estás "viendo como" otro usuario, aparece un aviso naranja en la parte superior de toda página con un enlace para volver a tu cuenta de administrador en un clic.

**Editar perfil con foto (los 3 roles):** cada rol tiene ahora su propia página "Mi perfil" (visible en el menú lateral) donde puede cambiar su nombre, teléfono, contraseña, y subir una foto de perfil que reemplaza el círculo con la inicial en toda la interfaz.

**Modo claro/oscuro:** el ícono de sol/luna (en el menú del sitio público, en el panel, y en las páginas de login/registro) alterna entre el tema oscuro de siempre y uno claro. Es completamente opcional: por defecto todo sigue oscuro para cualquier visitante nuevo, y solo cambia si la persona presiona el botón — la elección se guarda en su navegador (localStorage), sin afectar a nadie más.

## 10. Fases 2 a 6: Sistema completo tipo ERP

Sobre la Fase 1 (login, roles, paneles) se construyó todo el resto del sistema:

**Fase 2 — Productos avanzados e Inventario:**
`panel/administrador/productos.php` (SKU único, código de barras, marca, categoría, descuento, impuesto,
garantía, múltiples imágenes) y `panel/administrador/inventario.php` + `kardex.php` (entradas, salidas,
ajustes, alertas de stock mínimo, historial completo por producto).

**Fase 3 — Pedidos y Facturación:**
`panel/administrador/pedidos.php` + `pedido_detalle.php` (flujo completo: pendiente → confirmado →
en preparación → empacado → enviado → entregado, con historial de cada cambio) y generación de
facturas en PDF real (`php/generar_factura.php`, usando la librería FPDF incluida en `php/lib/fpdf/`).

**Fase 4 — Reportes:**
`panel/administrador/reportes.php`: ventas por día/mes, productos más y menos vendidos, clientes
frecuentes, empleados con más ventas. Exportable a PDF y Excel desde `php/exportar_reporte.php`.

**Fase 5 — Notificaciones, Chat y Atención al Cliente:**
Campana de notificaciones en el panel (nuevo pedido, stock bajo, respuesta de chat, cambio de estado).
Chat simple cliente↔asesor (`panel/cliente/chat.php` y `panel/administrador/chat.php`, con actualización
automática cada 4 segundos). Mensajes de contacto clasificados en mensaje/reclamo/sugerencia, gestionables
desde `panel/administrador/soporte.php`.

**Fase 6 — Favoritos, Reseñas, Auditoría y Respaldo:**
Los clientes pueden marcar productos favoritos (corazón en el catálogo) y calificar con estrellas +
comentario los productos que compraron. `panel/administrador/auditoria.php` muestra el registro completo
de acciones del sistema. `panel/administrador/respaldo.php` permite descargar y restaurar un backup
completo de la base de datos en `.sql`, sin depender del binario `mysqldump`.

**Cuenta semilla del administrador:** `admin@TecniSecure.com` / `Admin123!`

**Limitaciones documentadas honestamente:**
- El envío de correos reales (confirmaciones, promociones) requiere que configures tus propias
  credenciales SMTP — no es algo que se pueda generar automáticamente. El sistema de notificaciones
  internas (campana) cumple esa función mientras tanto.
- El chat es por "actualización automática" (polling) cada 4 segundos, no por WebSockets en tiempo real
  — para un sitio de este tamaño es indistinguible en uso normal, pero técnicamente no es "instantáneo".
- La exportación "Excel" genera un archivo `.xls` con una tabla HTML (técnica estándar que Excel abre
  perfectamente) en vez de usar una librería pesada como PhpSpreadsheet, que no viene con XAMPP.
- El restaurador de respaldos divide el archivo `.sql` por punto y coma; funciona perfecto con backups
  de este mismo sistema o de phpMyAdmin/Workbench en modo simple.

## 11. Cómo ejecutar el proyecto (resumen)

### Camino A: con XAMPP
1. Copia la carpeta `TecniSecure/` dentro de `C:\xampp\htdocs\`.
2. Enciende **Apache** y **MySQL** desde el Panel de Control de XAMPP.
3. Si Apache no inicia por el puerto 80 ocupado: libera el puerto (cierra Skype/IIS) o cámbialo a 8080 en `httpd.conf`.
4. Entra a `http://localhost/phpmyadmin`, pestaña **Importar**, selecciona `sql/tecnicsecure.sql`.
5. Abre tu navegador en `http://localhost/TecniSecure/`.

### Camino B: con MySQL Workbench + servidor PHP integrado (sin XAMPP)
1. Instala PHP para Windows si no lo tienes (windows.php.net/download) y agrégalo al PATH del sistema.
2. En MySQL Workbench: **File > Open SQL Script** > selecciona `sql/tecnicsecure.sql` > clic en el rayo ⚡ (Execute).
3. En `php/conexion.php`, escribe tu contraseña real de MySQL en `DB_PASS`.
4. En VS Code, abre una terminal dentro de la carpeta del proyecto y ejecuta: `php -S localhost:8000`
5. Abre tu navegador en `http://localhost:8000/index.php`

> En ambos caminos, la base de datos y el servidor deben estar corriendo **al mismo tiempo** para que el sitio funcione.

---
Hecho con HTML, CSS, PHP, JavaScript y MySQL — listo para XAMPP.
