# REGLAS DE CREACIÓN DEL PROYECTO — TecniSecure

> Documento normativo. Toda persona (o IA) que agregue, modifique o
> refactorice código en este repositorio **debe** seguir estas reglas.
> Versión 1.0 — 2026-09-12

---

## 1. Stack tecnológico obligatorio

| Capa | Tecnología | Restricción |
|---|---|---|
| Servidor | Apache (XAMPP) | PHP **8.0+** obligatorio |
| Backend | PHP puro (procedural + funciones) | **Sin frameworks, sin Composer** |
| Base de datos | MySQL / MariaDB | Acceso **solo** vía **PDO** |
| Frontend | HTML5 + CSS3 + JS vanilla | **Sin jQuery, Bootstrap, React, etc.** |
| PDF | FPDF (`php/lib/fpdf/`) | Vendorizada en el repo, no instalar |
| Editor | VS Code | MySQL Workbench / phpMyAdmin para la BD |

**Regla de oro:** si una dependencia externa no está ya en el repo, no se agrega.
Este proyecto es deliberadamente *zero-dependency* para funcionar en cualquier XAMPP.

---

## 2. Estructura de carpetas (canónica)

```
TecniSecure/
├── index.php                  → Portada pública
├── login.php / registro.php   → Autenticación pública
├── recuperar.php / restablecer.php
├── solicitar-servicio.php
├── php/                       → LÓGICA: endpoints, conexión, auth
│   ├── conexion.php           → Conexión PDO + helpers globales
│   ├── auth.php               → Sesión, CSRF, RBAC, auditoría
│   ├── procesar_*.php         → Endpoints de formularios públicos
│   ├── admin_*.php            → Endpoints del panel administrador
│   ├── empleado_*.php         → Endpoints del panel empleado
│   └── lib/fpdf/              → Librería vendorizada (NO editar)
├── includes/                  → PARTIALS reutilizables (header, footer…)
├── pages/                     → Vistas públicas secundarias
├── panel/
│   ├── administrador/         → Vistas del rol administrador
│   ├── empleado/              → Vistas del rol empleado
│   └── cliente/               → Vistas del rol cliente
├── css/                       → style.css, auth.css, panel.css
├── js/                        → main.js, carrito.js, auth.js, panel.js, tema.js
├── img/                       → logo/, productos/, servicios/, iconos/
├── uploads/                   → Archivos subidos por usuarios
├── sql/tecnicsecure.sql     → Script único de creación de BD
├── video/ · audio/            → Medios del hero
└── verificar_instalacion.php  → Diagnóstico (borrar en producción)
```

### 2.1 Regla de tres capas
- **Lógica** → siempre en `php/`. Nunca SQL embebido en una vista.
- **Vista** → siempre en `panel/`, `pages/` o raíz. Nunca lógica de negocio aquí.
- **Estilos/JS** → siempre en `css/` y `js/`. **Prohibido** `<style>` o `<script>`
  con código nuevo dentro de los `.php` (salvo configuración de una variable).

### 2.2 Regla de nomenclatura
- Archivos y carpetas: **snake_case en español** → `admin_productos.php`, `pedido_detalle.php`
- Endpoints de acción: prefijo `procesar_` (público) o `admin_` / `empleado_` (panel)
- Tablas y columnas BD: **snake_case**, tablas en plural → `pedido_detalle`, `logs_actividad`
- Funciones PHP: **camelCase** → `csrfValido()`, `requerirRol()`
- Clases: **PascalCase** (solo FPDF)
- Constantes: **MAYÚSCULAS** → `DB_HOST`, `SESION_MAX_INACTIVIDAD`

---

## 3. Reglas de base de datos

### 3.1 Creación / migración
1. **Toda** modificación de esquema se agrega al final de `sql/tecnicsecure.sql`
   como una nueva "FASE" comentada. Nunca crear scripts SQL sueltos.
2. El script usa `DROP DATABASE IF EXISTS` a propósito (evita el error
   `Table already exists` que detiene el script en MySQL Workbench).
   → **Hacer respaldo antes de reimportar** si hay datos reales.
3. Charset obligatorio: `utf8mb4` / `utf8mb4_unicode_ci`.
4. Motor: InnoDB (implícito por el uso de FOREIGN KEY).
5. Valores monetarios: `DECIMAL(10,2)` o `DECIMAL(12,2)`. **Nunca FLOAT.**
6. Toda tabla nueva debe tener `PRIMARY KEY` y `creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP`.

### 3.2 Integridad referencial
- Toda FK debe declarar `ON DELETE` explícitamente:
  - Datos dependientes → `ON DELETE CASCADE` (ej. `pedido_detalle` → `pedidos`)
  - Referencias opcionales → `ON DELETE SET NULL` (ej. `productos` → `categorias`)
- Estados: usar `ENUM` en la BD, no `VARCHAR`.

### 3.3 Convenciones de conexión
- **Siempre** incluir `require_once __DIR__ . '/conexion.php';` (o `../php/conexion.php`)
- **Siempre** usar la variable global `$pdo`
- **Nunca** crear una nueva conexión `new PDO(...)` fuera de `conexion.php`

---

## 4. Reglas de seguridad (NO negociables)

### 4.1 Toda página protegida DEBE empezar así
```php
require_once __DIR__ . '/../php/auth.php';
$usuario = requerirRol(['administrador']);   // o los roles permitidos
```

### 4.2 Checklist obligatorio de cada endpoint POST
```
[ ] 1. require_once de auth.php
[ ] 2. requiereRol([...]) con los roles exactos
[ ] 3. Verificar $_SERVER['REQUEST_METHOD'] === 'POST'
[ ] 4. Validar CSRF: csrfValido($_POST['csrf_token'] ?? null)
[ ] 5. Sanitizar/convertir TODA entrada con (int), trim(), (float)
[ ] 6. Usar SOLO prepared statements con $pdo->prepare()
[ ] 7. Registrar auditoría con registrarLog() si la acción es sensible
[ ] 8. Redirigir (PRG) o devolver JSON. Nunca imprimir datos crudos.
```

### 4.3 Prohibiciones absolutas
- ❌ Concatenar variables en SQL → `"WHERE id = $id"` **PROHIBIDO**
- ❌ `md5()` / `sha1()` para contraseñas → usar `password_hash()` / `password_verify()`
- ❌ Imprimir input del usuario sin `htmlspecialchars()`
- ❌ Guardar credenciales nuevas en código (usar las constantes de `conexion.php`)
- ❌ Confiar en `$_GET` para acciones que modifican datos

### 4.4 Sesión y tokens
- La sesión se configura **solo** en `auth.php` (antes de `session_start()`)
- Cookies: `httponly = true`, `samesite = 'Lax'`
- Timeout por inactividad: `SESION_MAX_INACTIVIDAD = 1800` (30 min)
- CSRF: token por sesión vía `csrfCampoOculto()` en cada `<form>`
- Todo formulario público incluye **CAPTCHA** matemático (`generarCaptchaPregunta()`)

### 4.5 Mensajes de error de login
Nunca revelar si el correo existe. Usar siempre el mensaje genérico
`'Correo o contraseña incorrectos.'`

---

## 5. Reglas de RBAC (roles)

| Rol | Acceso |
|---|---|
| `administrador` | Todo. `panel/administrador/*` + `admin_*.php` |
| `empleado` | `panel/empleado/*`, inventario, pedidos, ventas |
| `cliente` | `panel/cliente/*`, pedidos propios, favoritos, reseñas, chat |
| (sin sesión) | Solo páginas públicas + `pages/productos.php` |

- `requerirLogin()` → exige sesión, redirige a `login.php?redir=...`
- `requerirRol([...])` → exige rol, responde **403** si no coincide
- Un `cliente` **solo** puede ver sus propios datos: filtrar siempre por
  `id_usuario` o `id_cliente` de la sesión, **nunca** por un ID del formulario

---

## 6. Reglas de presentación (UI)

1. Toda vista del panel incluye el layout estándar:
   ```php
   require_once __DIR__ . '/../../includes/panel_header.php';
   require_once __DIR__ . '/../../includes/panel_sidebar.php';
   // ... contenido ...
   require_once __DIR__ . '/../../includes/panel_footer.php';
   ```
2. Páginas públicas → `includes/header.php` + `includes/footer.php`
3. Páginas de autenticación → `includes/auth_header.php` + `includes/auth_footer.php`
4. Todo `<form>` **debe** incluir `<?= csrfCampoOculto() ?>`
5. Los precios se formatean con separador de miles y sin decimales visibles
   cuando son enteros; los documentos usan `DECIMAL` en la BD, no en el HTML.
6. Idioma: **español**. Comentarios y nombres de archivos en español.
7. Accesibilidad mínima: `alt` en imágenes, `label` en inputs.

---

## 7. Reglas de estilo de código PHP

```php
<?php
/**
 * nombre_archivo.php
 * -----------------------------------------------------------------
 * Qué hace este archivo, en 2-3 líneas.
 * Si es un endpoint: qué recibe por POST y a dónde redirige.
 * -----------------------------------------------------------------
 */
```
- Indentación: **4 espacios** (no tabs)
- Llaves de apertura en la misma línea
- Comillas simples para SQL y HTML; dobles cuando hay interpolación
- `declare(strict_types=1)` → **no usado** (el proyecto es tolerante por diseño)
- Envolver funciones globales en `if (!function_exists('...'))` si se redefinen
- Comentar en español el **por qué**, no el **qué**

---

## 8. Reglas de CSS y JS

- `css/style.css` → sitio público
- `css/auth.css` → login/registro/recuperación
- `css/panel.css` → paneles internos
- `js/main.js` → menú, reloj HUD, contacto
- `js/carrito.js` → lógica del carrito
- `js/tema.js` → tema claro/oscuro
- Variables CSS (`--color-primario`, etc.) en `:root` de `style.css`
- **No** usar `!important` salvo para overrides de librerías externas
- Nombres de clase: kebab-case semántico (`.card-producto`, `.hud-reloj`)

---

## 9. Reglas de archivos de diagnóstico

- `verificar_instalacion.php`, `_diag_bd.php`, `_test_e2e.php`, `reparar_admin.php`
  son **herramientas de desarrollo**, no parte del producto.
- **Obligatorio** eliminarlas o protegerlas con `.htaccess` antes de desplegar.
- Cualquier archivo nuevo de prueba debe empezar con `_` y documentarse como temporal.

---

## 10. Flujo de trabajo obligatorio

```
1. Respaldar       → panel/administrador/respaldo.php  (si hay datos reales)
2. Leer reglas     → este archivo
3. Cambiar BD      → agregar FASE al final de sql/tecnicsecure.sql
4. Lógica          → crear/editar php/*.php (checklist de la sección 4.2)
5. Vista           → crear/editar panel/**, pages/** o raíz
6. Verificar       → http://localhost/TecniSecure/verificar_instalacion.php
7. Probar          → login admin@TecniSecure.com / Admin123!
8. Registrar log   → registrarLog() si la acción es sensible
```

### 10.1 Checklist antes de dar una tarea por terminada
```
[ ] ¿La consulta SQL usa prepared statement?
[ ] ¿El rol correcto está exigido con requerirRol()?
[ ] ¿El formulario envía csrf_token y se valida?
[ ] ¿La tabla nueva está en sql/tecnicsecure.sql en una FASE?
[ ] ¿El CSS/JS quedó en css/ o js/, no inline?
[ ] ¿Funciona con Apache + MySQL de XAMPP encendidos?
[ ] ¿Se probó con los 3 roles (admin, empleado, cliente)?
```

---

## 11. Entorno de desarrollo

```
XAMPP  → C:\xampp\htdocs\TecniSecure\
URL    → http://localhost/TecniSecure/
MySQL  → localhost:3306, usuario root, sin contraseña (XAMPP por defecto)
BD     → TecniSecure  (23 tablas)

Probar la BD desde PowerShell:
& "C:\xampp\mysql\bin\mysql.exe" -u root --default-auth=mysql_native_password -e "SHOW TABLES FROM TecniSecure;"
```
> El flag `--default-auth=mysql_native_password` es obligatorio: el `mysql.exe`
> de XAMPP no encuentra `caching_sha2_password.dll`.

**Credenciales semilla:**
```
admin@TecniSecure.com  /  Admin123!
```

---

## 12. Deuda técnica conocida (pendiente de resolver)

| # | Problema | Prioridad |
|---|---|---|
| 1 | `CODIGO_SUPLANTACION` hardcodeado en `conexion.php` | 🔴 Alta |
| 2 | Archivos de diagnóstico accesibles por URL | 🔴 Alta |
| 3 | `DB_PASS` vacío (solo válido en local) | 🟡 Media |
| 4 | Sin `.htaccess` que proteja `sql/`, `php/lib/`, `uploads/` | 🟡 Media |
| 5 | Usuario MySQL `root` usado por la app (crear usuario dedicado) | 🟡 Media |
| 6 | `recortar_texto()` no es multibyte (corta acentos) | 🟢 Baja |
| 7 | Importar el `.sql` borra la BD completa | 🟢 Baja (documentado) |

---

*Fin de las reglas. Cualquier excepción debe justificarse en un comentario del código.*
