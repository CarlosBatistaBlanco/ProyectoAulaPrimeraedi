# Manual Técnico del Sistema TECNISECURE

**Proyecto:** TECNISECURE (API REST en Spring Boot + Frontend web estático)  
**Versión del software:** 2.0.0  
**Fecha de actualización:** 11 de noviembre de 2026  

---

## Tabla de contenido

1. [Introducción](#1-introducción)
2. [Objetivos](#2-objetivos)
   - 2.1 [Objetivo general](#21-objetivo-general)
   - 2.2 [Objetivos específicos](#22-objetivos-específicos)
3. [Alcance y requisitos de infraestructura](#3-alcance-y-requisitos-de-infraestructura)
4. [Desarrollo técnico del sistema](#4-desarrollo-técnico-del-sistema)
   - 4.1 [Arquitectura y pila tecnológica](#41-arquitectura-y-pila-tecnológica)
   - 4.2 [Estructura real de directorios y archivos](#42-estructura-real-de-directorios-y-archivos)
   - 4.3 [Modelo de datos, carga inicial y saneamiento UTF-8](#43-modelo-de-datos-carga-inicial-y-saneamiento-utf-8)
   - 4.4 [Seguridad, autenticación JWT y control de acceso](#44-seguridad-autenticación-jwt-y-control-de-acceso)
   - 4.5 [Controladores REST y comunicación en tiempo real](#45-controladores-rest-y-comunicación-en-tiempo-real)
   - 4.6 [Variables de configuración, despliegue local y pruebas](#46-variables-de-configuración-despliegue-local-y-pruebas)
   - 4.7 [Aspectos pendientes para entorno de producción](#47-aspectos-pendientes-para-entorno-de-producción)
5. [Conclusiones](#5-conclusiones)
6. [Referencias](#6-referencias)

---

## 1. Introducción

Este documento detalla la arquitectura de software, el esquema de persistencia, los contratos de servicio y los procedimientos de mantenimiento de TECNISECURE (`tecnicsecure-api`, versión `2.0.0`). El sistema nació como una migración desde PHP hacia Java y Spring Boot con el propósito de separar la lógica de negocio en una API REST y conservar un cliente web ligero servido desde el mismo puerto HTTP (TECNISECURE, 2026).

Toda la información consignada corresponde al estado verificable en los archivos del repositorio (`backend/pom.xml`, `backend/src/main/resources/application.yml`, controladores, entidades JPA y vistas de `frontend/`).

---

## 2. Objetivos

### 2.1 Objetivo general
Documentar la arquitectura técnica, configuración, modelo de datos y puntos de extensión de TECNISECURE para facilitar su mantenimiento, auditoría y futuro paso a producción.

### 2.2 Objetivos específicos
- Describir el patrón de tres capas + MVC adoptado en el backend Java junto con sus dependencias reales en Maven.
- Inventariar las 26 entidades JPA, los 12 controladores REST, los canales WebSocket y las rutinas automáticas de arranque.
- Especificar las variables de entorno soportadas, el comando de pruebas automatizadas y los requisitos técnicos pendientes antes de publicar el proyecto para clientes reales.

---

## 3. Alcance y requisitos de infraestructura

El manual abarca el servidor backend ubicado en `backend/` y el cliente estático de `frontend/`. Para compilar y ejecutar el sistema en desarrollo se requiere:

- **JDK 21 o superior** (`<java.version>21</java.version>` en `backend/pom.xml`, compilado con `--release 21`; en pruebas locales se validó con JDK 22) (Oracle Corporation, 2023).
- **Apache Maven 3.9+** para resolver dependencias y ejecutar pruebas.
- **No necesita servidor web ni motor de base de datos externo en desarrollo:** Spring Boot incluye Tomcat embebido y H2 trabaja sobre archivo local en `backend/data/tecnicsecure.mv.db` (Broadcom, 2024; H2 Database Engine, 2023).

---

## 4. Desarrollo técnico del sistema

### 4.1 Arquitectura y pila tecnológica

La aplicación sigue una división en tres capas lógicas combinada con el patrón Modelo-Vista-Controlador (MVC) en la capa web:

```text
Navegador (HTML5 / CSS3 / JS Vanilla + WebSocket STOMP)
   │  HTTP/JSON (Authorization: Bearer <JWT>) + /ws
   ▼
[1] Capa de Presentación · controller/ + security/
   │  Validación Jakarta (@Valid), @PreAuthorize, DTOs y respuestas JSON
   ▼
[2] Capa de Negocio · service/ + service/impl/
   │  Reglas de negocio, transacciones, stock, correo SMTP y bot
   ▼
[3] Capa de Datos · repository/ + entity/
   │  Spring Data JPA / Hibernate ORM 6.5.3.Final
   ▼
Base de datos H2 embebida (backend/data/tecnicsecure.mv.db)
```

Dependencias principales declaradas en `backend/pom.xml` (TECNISECURE, 2026):

| Pieza | Artefacto / Versión | Propósito |
|---|---|---|
| Framework principal | `spring-boot-starter-parent` `3.3.5` | Contenedor IoC, MVC, seguridad, JPA, WebSocket y correo |
| Persistencia | `spring-boot-starter-data-jpa` + `h2` (`2.2.224`) | Mapeo objeto-relacional y almacenamiento en archivo |
| Seguridad | `spring-boot-starter-security` + `jjwt` `0.12.6` | Filtros HTTP, BCrypt y firma/validación JWT |
| Documentación API | `springdoc-openapi-starter-webmvc-ui` `2.6.0` | Interfaz Swagger UI en `/swagger-ui.html` (Springdoc, 2024) |
| Mensajería correo | `spring-boot-starter-mail` | Envío SMTP de facturas y recuperación de clave |
| Pruebas | `spring-boot-starter-test` + `spring-security-test` | JUnit 5, Mockito y pruebas de controladores con MockMvc |

### 4.2 Estructura real de directorios y archivos

```text
TecniServicios/
├── iniciar.bat / iniciar.ps1
├── README.md
├── docs/
│   ├── API.md
│   ├── Funcionalidades.txt
│   ├── Manual_Usuario.md
│   └── Manual_Tecnico.md
├── frontend/
│   ├── index.html, login.html, registro.html, recuperar.html, restablecer.html
│   ├── pages/                 (productos.html, lista_deseos.html, solicitar-servicio.html)
│   ├── panel/                 (administrador/, cliente/, empleado/)
│   ├── css/ · js/ · img/ · uploads/ · audio/ · video/
└── backend/
    ├── pom.xml
    ├── data/                  (tecnicsecure.mv.db)
    └── src/
        ├── main/
        │   ├── resources/
        │   │   ├── application.yml
        │   │   └── data.sql
        │   └── java/com/tecnicsecure/
        │       ├── TecniSecureApplication.java
        │       ├── config/        (CargaDatosIniciales, DatosInicialesConfig, OpenApiConfig, WebSocketConfig, etc.)
        │       ├── controller/    (12 controladores REST)
        │       ├── dto/           (records y DTOs de entrada/salida)
        │       ├── entity/        (26 entidades JPA)
        │       ├── exception/     (ApiException y manejador global)
        │       ├── repository/    (24 interfaces JpaRepository)
        │       ├── security/      (JwtService, JwtAuthenticationFilter, SecurityConfig)
        │       ├── service/ & service/impl/
        │       └── util/          (UsuarioActual)
        └── test/java/com/tecnicsecure/
```

El frontend estático se publica directamente desde el servidor backend mediante la propiedad `spring.web.resources.static-locations: file:../frontend/,classpath:/static/` definida en `application.yml`.

### 4.3 Modelo de datos, carga inicial y saneamiento UTF-8

El paquete `com.tecnicsecure.entity` agrupa 26 clases mapeadas sobre H2 en modo de compatibilidad MySQL (`jdbc:h2:file:./data/tecnicsecure;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_ON_EXIT=FALSE`) (H2 Database Engine, 2023):

- **Seguridad y estructura operativa:** `Rol`, `Usuario`, `TokenRecuperacion`, `Cliente`, `Empleado`, `EmpleadoPerfil`, `TareaEmpleado`, `LogActividad`.
- **Catálogo y almacén:** `Categoria`, `Marca`, `Producto`, `ProductoImagen`, `InventarioMovimiento`, `Favorito`, `FavoritoId`, `Resena`.
- **Órdenes y facturación:** `Pedido`, `PedidoDetalle`, `PedidoHistorial`, `EstadoPedido`, `Factura`.
- **Atención al cliente y notificaciones:** `Servicio`, `SolicitudServicio`, `Contacto`, `MensajeChat`, `Notificacion`.

Todas las entidades con auditoría temporal inicializan `LocalDateTime creadoEn = LocalDateTime.now()` para impedir valores nulos al insertar con `ddl-auto: update`. Al iniciar Spring Boot se ejecutan dos procesos automáticos:

1. `DatosInicialesConfig` (mediante `ApplicationRunner`) rellena con `CURRENT_TIMESTAMP` cualquier columna `creado_en` que siga en `NULL`, inspecciona las columnas de texto del esquema activo con `DatabaseMetaData` para corregir caracteres dañados por doble codificación UTF-8 (`repararCodificacionMojibake`) y crea el usuario administrador si no existe.
2. `CargaDatosIniciales` (al recibir `ApplicationReadyEvent`) lee `backend/src/main/resources/data.sql` en UTF-8 usando `^^^` como separador de sentencias para sembrar roles, categorías, marcas, 6 servicios y 28 productos de forma idempotente, además de deduplicar registros antiguos con codificación alterada.

### 4.4 Seguridad, autenticación JWT y control de acceso

La seguridad HTTP está implementada con Spring Security bajo política sin sesiones en servidor (`STATELESS`), donde cada solicitud autenticada adjunta la cabecera `Authorization: Bearer <accessToken>` firmada mediante HMAC-SHA con `jjwt` conforme al estándar RFC 7519 (Jones et al., 2015).

- Las contraseñas se almacenan con hash BCrypt.
- Tras cinco intentos fallidos consecutivos de ingreso, `AuthServiceImpl` bloquea la cuenta durante 15 minutos (`bloqueo-minutos: 15` en `application.yml`).
- Los permisos se validan por rol (`administrador`, `empleado`, `cliente`) mediante anotaciones `@PreAuthorize` en los controladores y por módulo habilitado para cada empleado en `EmpleadoGestionServiceImpl`.
- El detalle de los permisos se audita en `logs_actividad`.

### 4.5 Controladores REST y comunicación en tiempo real

Los 12 controladores REST en `com.tecnicsecure.controller` se documentan interactivamente en `/swagger-ui.html` gracias a `OpenApiConfig` (Springdoc, 2024) y de forma estática en `docs/API.md`:

| Controlador | Ruta base | Responsabilidad principal |
|---|---|---|
| `AuthController` | `/api/auth` | Login, refresh token, registro, captcha matemático, recuperación y `/me` |
| `CatalogoController` | `/api` | Productos, categorías, marcas, servicios y reseñas |
| `PedidoController` | `/api/pedidos` | Checkout, consulta de órdenes propias o generales, cambio de estado e historial |
| `FacturacionController` | `/api/pedidos` | Consulta de factura formal, nota crédito, descarga HTML y envío SMTP |
| `InventarioController` | `/api/inventario` | Movimientos de entrada/salida/ajuste y alertas de stock mínimo |
| `SoporteController` | `/api` | Solicitudes técnicas, contactos, notificaciones y chat |
| `EmpleadoGestionController` | `/api/gestion-empleados` | Áreas, módulos habilitados, tareas, evidencias y calificación |
| `UsuarioController` | `/api/usuarios` | Gestión administrativa de cuentas, roles, desbloqueo y CSV |
| `ClienteController` | `/api/clientes` | Consulta de clientes registrados |
| `ReporteController` | `/api/reportes` | Métricas de período, productos más vendidos y exportación CSV (con BOM) / XML |
| `RespaldoController` | `/api/respaldo` | Respaldo general de registros en JSON |
| `UploadController` | `/api/uploads` | Carga de archivos multipart (máximo 5 MB por archivo, 10 MB por solicitud) |

La mensajería instantánea utiliza WebSocket con protocolo STOMP sobre el punto `/ws` (Fette & Melnikov, 2011). En el backend, `SoporteServiceImpl` publica hacia `/topic/chat/{idCliente}` y `/topic/notificaciones/{idUsuario}`. En el frontend, `frontend/js/websocket.js` abre la suscripción y `frontend/js/panel.js` conserva un respaldo por sondeo cada 4 segundos.

### 4.6 Variables de configuración, despliegue local y pruebas

Variables configurables en `backend/src/main/resources/application.yml`:

| Variable | Por defecto | Función |
|---|---|---|
| `APP_PORT` | `8080` | Puerto de escucha HTTP |
| `DB_USER` / `DB_PASSWORD` | `sa` / *(vacío)* | Acceso a H2 |
| `JWT_SECRET` | valor de desarrollo | Secreto HMAC-SHA (mínimo 32 caracteres) |
| `JWT_EXPIRATION_MINUTES` | `120` | Minutos de vigencia del access token |
| `JWT_REFRESH_MINUTES` | `10080` | Minutos de vigencia del refresh token |
| `MAIL_ENABLED` | `false` | Habilita (`true`) o simula (`false`) envíos SMTP |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` | Servidor SMTP y puerto STARTTLS |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | *(vacío)* | Cuenta y contraseña de aplicación SMTP |
| `MAIL_FROM` | `no-responder@tecnisecure.com` | Dirección remitente |
| `CORS_ORIGINS` | `localhost:8080`, `5500` | Orígenes permitidos |
| `UPLOADS_DIR` | `../frontend/uploads` | Carpeta física para cargas multipart |
| `ADMIN_CORREO` / `ADMIN_PASSWORD` | `admin@tecnicsecure.com` / `Admin123!` | Cuenta administradora inicial |

Comandos de ejecución y pruebas en Windows (PowerShell):

```powershell
# Arrancar servidor (también disponible mediante iniciar.bat o .\iniciar.ps1)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-22'
mvn -f backend\pom.xml spring-boot:run

# Ejecutar pruebas automatizadas
mvn -f backend\pom.xml test
```

La última ejecución de `mvn -f backend\pom.xml test` sobre las 11 clases existentes (`CatalogoControllerTest`, `DescargasIntegrationTest`, `JwtServiceTest` y 8 clases de prueba de servicios) completó **43 pruebas con 0 fallos, 0 errores y 0 omitidas**.

### 4.7 Aspectos pendientes para entorno de producción

Al inspeccionar el código actual se identifican cinco requisitos pendientes para pasar de demostración local a operación comercial real:

1. **Pasarela de pagos real:** el proceso de checkout almacena el método de pago escogido sin comunicarse con entidades financieras. Se requiere integrar un proveedor (Wompi, MercadoPago, PayU o Stripe) y validar transacciones mediante webhook antes de confirmar pedidos.
2. **Base de datos cliente-servidor:** H2 en archivo admite un solo proceso de escritura y resulta adecuado para desarrollo o academia; en producción debe sustituirse por MySQL o PostgreSQL agregando el conector JDBC al `pom.xml`, cambiando `ddl-auto` de `update` a `validate` y versionando el esquema con Flyway o Liquibase.
3. **Endurecimiento de credenciales y red (`application.yml`):** asignar `JWT_SECRET`, `ADMIN_PASSWORD` y `DB_PASSWORD` por variables de entorno externas, activar `MAIL_ENABLED=true` con credenciales SMTP reales, desactivar `/h2-console` (`spring.h2.console.enabled=false`), restringir `/swagger-ui.html` si aplica y servir tráfico bajo HTTPS mediante proxy inverso.
4. **Generación nativa de PDF o facturación electrónica DIAN:** `FacturacionController` entrega la factura en HTML lista para imprimir o guardar como PDF desde el navegador; si la operación exige un `.pdf` binario directo o radicación electrónica DIAN, será necesario incorporar una biblioteca como OpenPDF o un proveedor tecnológico habilitado.
5. **Ampliación de pruebas y multimedia:** las carpetas `frontend/audio/` y `frontend/video/` conservan únicamente archivos instructivos (`LEEME.txt`) sin recursos multimedia finales; en `backend/src/test/java` conviene añadir pruebas MockMvc para `AuthController`, `PedidoController`, `FacturacionController` y `EmpleadoGestionController`.

---

## 5. Conclusiones

La separación del backend de TECNISECURE en controladores, servicios y repositorios JPA permite mantener aisladas las reglas de negocio respecto al transporte HTTP y facilita reemplazar la base embebida H2 por un motor cliente-servidor sin reescribir la lógica de dominio. El uso de JWT sin estado, WebSocket con respaldo por sondeo, saneamiento automático de codificación al inicio y una suite verificada de 43 pruebas unitarias y de integración entrega una base técnica funcional y ordenada sobre la cual solo resta incorporar integraciones externas de pago, correo productivo y configuración de despliegue seguro para operar en un entorno comercial.

---

## 6. Referencias

Broadcom. (2024). *Spring Boot Reference Documentation (Version 3.3)*. https://docs.spring.io/spring-boot/3.3/reference/index.html

Fette, I., & Melnikov, A. (2011). *RFC 6455: The WebSocket Protocol*. Internet Engineering Task Force (IETF). https://www.rfc-editor.org/info/rfc6455

H2 Database Engine. (2023). *H2 Database Engine Documentation (Version 2.2.224)*. https://www.h2database.com/html/main.html

Jones, M., Bradley, J., & Sakimura, N. (2015). *RFC 7519: JSON Web Token (JWT)*. Internet Engineering Task Force (IETF). https://www.rfc-editor.org/info/rfc7519

Oracle Corporation. (2023). *Java Platform, Standard Edition (Java SE 21) Documentation*. https://docs.oracle.com/en/java/javase/21/

Springdoc. (2024). *springdoc-openapi v2: OpenAPI 3 & Spring Boot Integration*. https://springdoc.org/

TECNISECURE. (2026). *Código fuente y especificación de API REST de TECNISECURE (Versión 2.0.0)* [Software]. Repositorio `TecniServicios` (`README.md`, `docs/API.md`, `backend/pom.xml`, `backend/src/main/resources/application.yml`).
