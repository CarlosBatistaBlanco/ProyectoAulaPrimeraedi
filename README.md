# TECNISECURE · API REST

Sistema de seguridad electrónica (cámaras, alarmas y control de acceso) migrado de
PHP a **Java + Spring Boot** con arquitectura de tres capas y patrón MVC.

---

## 1. Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot 3.3 |
| Arquitectura | Tres capas (Presentación → Negocio → Datos) + MVC |
| API | REST (JSON) documentada con Swagger / OpenAPI 3 |
| Base de datos | H2 embebida en archivo *(sin instalar nada)* · compatible con MySQL |
| Frontend | HTML5 + CSS3 + JavaScript (vanilla) |
| Seguridad | Spring Security + JWT (access y refresh token) |
| Pruebas | JUnit 5 + Mockito |
| Tiempo real | WebSocket (STOMP) para chat y notificaciones |

> **No requiere XAMPP, Apache, MySQL ni Tomcat.** Spring Boot trae su propio servidor
> y la base de datos vive en un archivo dentro del proyecto.

---

## 2. Cómo ejecutar

### Opción A · Docker (recomendada · funciona en cualquier equipo)
Solo necesitas **Docker Desktop**: no hay que instalar Java, Maven ni nada más.

```bash
docker compose up --build -d
```

Abre **http://localhost:8080**. Para detenerlo: `docker compose down`.

- Los datos persisten en `backend/data/` y los archivos subidos en `frontend/uploads/`.
- El sitio web se sirve desde `frontend/`: los cambios en HTML/CSS/JS se ven al recargar.
- Opcional: crea un `.env` junto a `docker-compose.yml` con `APP_PORT`, `JWT_SECRET`,
  `ADMIN_CORREO`, `ADMIN_PASSWORD`, `MAIL_*`, etc.
- Estado del servicio: `docker compose ps` (debe aparecer `healthy`).

### Opción B · Script incluido (Windows · requiere JDK 21 y Maven 3.9+)
```bat
iniciar.bat
```
o en PowerShell:
```powershell
.\iniciar.ps1
```

### Opción C · Manual
```bash
cd backend
mvn spring-boot:run
```

Abre **http://localhost:8080**

| Recurso | URL |
|---|---|
| Sitio web | http://localhost:8080 |
| Swagger UI (documentación de la API) | http://localhost:8080/swagger-ui.html |
| Consola de la base de datos | http://localhost:8080/h2-console |
| Estado del servicio (health) | http://localhost:8080/actuator/health |

> El frontend se toma de la carpeta `frontend/` hermana de `backend/`. Si ejecutas el
> JAR desde otra ubicación, indica la ruta con `-Dfrontend.dir=/ruta/al/frontend`
> (y opcionalmente `UPLOADS_DIR` para los archivos subidos).

### Cuenta de administrador

Las credenciales no se muestran en la pantalla de login. Están en el archivo
**`CREDENCIALES.txt`** en la raíz del proyecto (no se sube al repositorio).

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@tecnicsecure.com` | `Admin123!` |

> Se definen con `ADMIN_CORREO` y `ADMIN_PASSWORD` en el archivo `.env`.
> Cámbialas antes de publicar el sitio.

---

## 3. Estructura del proyecto

```
TecniServicios/
├── backend/                          ← API REST (Spring Boot)
│   ├── pom.xml
│   ├── data/                         ← base de datos H2 (se crea sola)
│   ├── src/main/java/com/tecnicsecure/
│   │   ├── TecniSecureApplication.java
│   │   ├── controller/               ← CAPA DE PRESENTACIÓN (la "C" de MVC)
│   │   │   ├── AuthController.java
│   │   │   ├── CatalogoController.java
│   │   │   ├── PedidoController.java
│   │   │   ├── InventarioController.java
│   │   │   ├── SoporteController.java
│   │   │   ├── ReporteController.java
│   │   │   └── ClienteController.java
│   │   ├── service/                  ← CAPA DE NEGOCIO (interfaces)
│   │   │   └── impl/                 ← implementaciones con las reglas
│   │   ├── repository/               ← CAPA DE DATOS (Spring Data JPA)
│   │   ├── entity/                   ← modelo del dominio
│   │   ├── dto/                      ← contratos de entrada/salida
│   │   ├── security/                 ← Spring Security + JWT
│   │   ├── config/                   ← OpenAPI, WebSocket, MVC, datos
│   │   ├── exception/                ← manejo centralizado de errores
│   │   └── util/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── data.sql                  ← datos iniciales (catálogo y servicios)
│   ├── src/test/java/                ← pruebas JUnit + Mockito
│   └── tools/                        ← utilidades de desarrollo
│
├── frontend/                         ← HTML, CSS y JavaScript
│   ├── index.html
│   ├── login.html · registro.html
│   ├── recuperar.html · restablecer.html
│   ├── pages/  (productos, lista de deseos, solicitar servicio)
│   ├── panel/  (administrador, empleado, cliente)
│   ├── css/ · js/ · img/ · video/ · audio/ · uploads/
│   │   └── js/websocket.js            ← tiempo real (STOMP): chat y notificaciones
│
├── docs/
│   ├── Funcionalidades.txt           ← funcionalidades a implementar (equipo)
│   ├── API.md                        ← referencia de endpoints
│   └── openapi.json                  ← especificación exportada
│
├── iniciar.bat / iniciar.ps1         ← arrancan todo el sistema
└── README.md
```

### Las tres capas en la práctica

```
Navegador
   │  fetch() con token JWT
   ▼
[1] PRESENTACIÓN  ·  controller/ + security/
   │  valida DTO, aplica @PreAuthorize, responde JSON
   ▼
[2] NEGOCIO       ·  service/impl/
   │  stock, precios, estados, permisos, auditoría
   ▼
[3] DATOS         ·  repository/ + entity/
   │  Spring Data JPA
   ▼
H2 (backend/data/tecnicsecure.mv.db)
```

El controlador **nunca** contiene reglas de negocio y el servicio **nunca** conoce HTTP:
cada capa solo depende de la siguiente hacia abajo.

---

## 4. Endpoints principales

La referencia completa está en [docs/API.md](docs/API.md) y en Swagger UI.

### Autenticación
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Inicia sesión y devuelve `accessToken` + `refreshToken` |
| POST | `/api/auth/refresh` | Renueva el access token |
| POST | `/api/auth/registro` | Crea una cuenta de cliente |
| POST | `/api/auth/recuperar` | Solicita enlace de recuperación |
| POST | `/api/auth/restablecer` | Cambia la contraseña con el token |
| GET | `/api/auth/me` | Datos del usuario autenticado |
| PUT | `/api/auth/me` | Actualiza el perfil |

### Catálogo (público)
| Método | Ruta |
|---|---|
| GET | `/api/productos` · `/api/productos/destacados` · `/api/productos/{id}` |
| GET | `/api/categorias` · `/api/marcas` · `/api/servicios` |
| POST / PUT / DELETE | `/api/productos` (administrador o empleado) |

### Pedidos, inventario y soporte
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/pedidos` | Checkout del carrito |
| GET | `/api/pedidos/mis-pedidos` | Pedidos del cliente |
| PATCH | `/api/pedidos/{id}/estado` | Cambio de estado con validación de flujo |
| GET | `/api/inventario/stock-bajo` | Alertas de stock mínimo |
| POST | `/api/inventario/productos/{id}/movimientos` | Entrada / salida / ajuste |
| POST | `/api/contactos` · `/api/solicitudes` | Formularios públicos |
| GET | `/api/notificaciones` | Notificaciones del usuario |
| GET | `/api/reportes/resumen` | Dashboard y reportes |
| GET | `/api/reportes/ventas/export` | Exporta CSV / XML |

---

## 5. Seguridad

- **JWT** con expiración configurable y refresh token.
- Contraseñas con **BCrypt** (compatible con los hashes `password_hash()` de PHP).
- **Control por roles**: `administrador`, `empleado`, `cliente` mediante `@PreAuthorize`.
- **Bloqueo automático** tras 5 intentos fallidos (15 minutos).
- Registro de accesos y acciones en `logs_actividad`.
- Un cliente solo puede ver **sus propios** pedidos y conversaciones.

### Autenticarse en Swagger
1. `POST /api/auth/login` con `admin@tecnicsecure.com` / `Admin123!`
2. Copia el `accessToken`
3. Pulsa **Authorize** y pega el token (sin la palabra `Bearer`)

---

## 6. Pruebas

```bash
cd backend
mvn test
```

| Prueba | Qué verifica |
|---|---|
| `PedidoServiceImplTest` | Total con precio real, stock, flujo de estados |
| `InventarioServiceImplTest` | Entradas, salidas, ajustes y alertas |
| `CatalogoServiceImplTest` | Precio con descuento, SKU único, baja lógica |
| `JwtServiceTest` | Generación, expiración y firma de tokens |
| `CatalogoControllerTest` | JSON de los endpoints con MockMvc |
| `AuthServiceImplPerfilTest` | `idCliente` expuesto para el chat en tiempo real |
| `SoporteServiceImplChatTest` | Listado de clientes y publicación en `/topic/chat/{id}` |

**32 pruebas · todas en verde.**

---

## 7. Configuración

Variables de entorno (todas opcionales):

| Variable | Por defecto | Descripción |
|---|---|---|
| `APP_PORT` | `8080` | Puerto del servidor |
| `JWT_SECRET` | *(valor de desarrollo)* | Clave de firma (mínimo 32 caracteres) |
| `JWT_EXPIRATION_MINUTES` | `120` | Duración del access token |
| `ADMIN_CORREO` | `admin@tecnicsecure.com` | Correo del administrador |
| `ADMIN_PASSWORD` | `Admin123!` | Contraseña del administrador |
| `MAIL_ENABLED` | `false` | Activa el envío real de correos |
| `MAIL_USERNAME` | *(vacío)* | Cuenta desde la que se envían las facturas |
| `MAIL_PASSWORD` | *(vacío)* | Contraseña de aplicación (no la del correo) |
| `MAIL_FROM` | *(= MAIL_USERNAME)* | Remitente que ve el cliente |
| `DEEPSEEK_API_KEY` | *(vacío)* | Clave de la IA del asistente virtual |
| `CAPTCHA_SECRET` | *(aleatoria)* | Clave para firmar la verificación del login |

> **En producción cambia siempre `JWT_SECRET` y `ADMIN_PASSWORD`.**

### Enviar facturas por correo

Las facturas se envían realmente al correo del cliente desde el panel de
**Facturación** (administrador y empleado). Para activarlo:

1. Usa una cuenta de Gmail con **verificación en 2 pasos**.
2. Genera una **Contraseña de aplicación** en
   <https://myaccount.google.com/apppasswords> (16 caracteres).
3. Escríbela en `.env`:
   ```ini
   MAIL_ENABLED=true
   MAIL_USERNAME=tucuenta@gmail.com
   MAIL_PASSWORD=xxxxxxxxxxxxxxxx
   MAIL_FROM=tucuenta@gmail.com
   ```
4. Reinicia: `docker compose up -d --force-recreate`

La página de facturación muestra un aviso verde cuando el envío está activo.
Si no hay credenciales, el comprobante se puede descargar o imprimir y el
aviso indica que el correo aún no está configurado (sin tecnicismos).

Desde el modal de la factura puedes enviarla **al cliente** o **a otro correo**
con el botón "Enviar a otro correo".

### Cambiar a MySQL (opcional)

La aplicación está preparada para ambos motores. Para usar MySQL:

1. Añade al `backend/pom.xml`:
   ```xml
   <dependency>
       <groupId>com.mysql</groupId>
       <artifactId>mysql-connector-j</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```
2. Define las variables de entorno:
   ```bat
   set DB_URL=jdbc:mysql://localhost:3306/tecnicsecure
   set DB_USER=root
   set DB_PASSWORD=tuclave
   ```
3. Ajusta `spring.datasource` en `application.yml` para leer esas variables.

---

## 8. Respaldo de la base de datos

Toda la información está en **`backend/data/tecnicsecure.mv.db`**.
Para respaldar: detén la aplicación y copia ese archivo.
Para restaurar: devuélvelo a su sitio.

---

## 9. Siguientes pasos

El archivo [docs/Funcionalidades.txt](docs/Funcionalidades.txt) contiene las
funcionalidades REST que el equipo añadirá sobre esta base. Las seis están
implementadas (inventario, solicitudes técnicas, reportes, JWT, seguimiento de
pedidos y comunicación con chat en tiempo real).

### Tiempo real

El chat y las notificaciones se actualizan al instante:

| Pieza | Dónde |
|---|---|
| Endpoint STOMP | `ws://localhost:8080/ws` |
| Publicación en el backend | `SoporteServiceImpl` → `/topic/chat/{idCliente}` y `/topic/notificaciones/{idUsuario}` |
| Suscripción en el navegador | `frontend/js/websocket.js` + `frontend/js/panel.js` |

Si el WebSocket no puede conectarse, el panel sigue funcionando con sondeo
cada 4 segundos, así que el chat nunca se queda bloqueado.

### Chat del sitio web (visitantes sin cuenta)

El asistente flotante de la página pública permite que un **visitante anónimo**
converse con el equipo sin registrarse.

| Pieza | Dónde |
|---|---|
| Widget público | `frontend/js/asistente.js` + `frontend/css/asistente.css` |
| Hilo del visitante | Tablas `conversaciones_visitante` y `mensajes_visitante` |
| Token del hilo | Aleatorio (32 caracteres), lo guarda el navegador en `localStorage` |
| Endpoints públicos | `POST /api/asistente`, `POST /api/asistente/mensajes`, `GET /api/asistente/conversacion/{token}` |
| Bandeja del equipo | `GET /api/chat/clientes` (mezcla clientes registrados y visitantes) |
| Responder al visitante | `GET` y `POST /api/chat/visita/{idConversacion}` (solo admin y empleado) |

**Flujo completo:**

1. El visitante escribe en el recuadro del sitio. El servidor crea el hilo y
   le devuelve un token que su navegador guarda.
2. Cada mensaje avisa al equipo con una notificación y aparece en la bandeja
   del chat con la etiqueta **Web**.
3. El administrador o un empleado abre el hilo y responde desde el panel.
4. El widget del visitante consulta el hilo cada 6 segundos, muestra la
   respuesta en verde como **Asesor TECNISECURE** y avisa con un punto rojo si
   el recuadro está cerrado.
5. El visitante puede seguir escribiendo en el mismo hilo cuando quiera.

**Clasificación de la solicitud:** cuando el chat se escala, el servicio se
deduce del texto del visitante (alarmas, monitoreo, control de acceso,
mantenimiento…). Si no coincide con ninguno se registra como *Asesoría y
Diseño*, que es el caso genérico.

| Prueba | Qué cubre |
|---|---|
| `ConversacionVisitanteTest` | Creación del hilo, token estable, bandeja y respuesta del equipo |
| `AsistenteServicioEscaladoTest` | Cada consulta se clasifica en el servicio correcto |
| `SoporteServiceImplChatTest` | Publicación en `/topic/chat/{id}` |

---

Hecho con Java, Spring Boot, HTML, CSS y JavaScript.
