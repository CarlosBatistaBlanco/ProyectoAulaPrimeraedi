# TECNISECURE Mobile — Aplicación Android Nativa

**Institución:** Fundación Universitaria Colombo Internacional (UNICOLOMBO)  
**Facultad:** Facultad de Ingeniería y Ciencias Aplicadas  
**Autor:** Leonardo Martínez Santander (Sexto Semestre, Noviembre de 2026)  
**Plataforma Backend:** Spring Boot 3.3.5 / Java 21 (API RESTful)  

---

## 1. Descripción del Proyecto Móvil

Esta aplicación móvil fue desarrollada de forma nativa en **Android Studio** utilizando **Kotlin**, **Retrofit 2**, **Corrutinas**, **OkHttp** y **Material Design 3**. 

Se comunica directamente con el backend existente de **TECNISERVICIOS (TECNISECURE v2.0)** sin modificar ni alterar ningún archivo de la aplicación web original, permitiendo presentar en simultáneo la solución web y la solución móvil consumiendo la misma base de datos y lógica de negocio.

---

## 2. Cómo Abrir y Ejecutar en Android Studio

1. Abre **Android Studio** (Hedgehog, Iguana, Jellyfish o posterior).
2. Haz clic en **Open** (o *File -> Open...*).
3. Navega y selecciona la carpeta `TecniSecureMobile`.
4. Espera a que Gradle sincronice las dependencias (`Sync Project with Gradle Files`).
5. Inicia un emulador Android (API 30+) o conecta un teléfono físico por USB.
6. Presiona **Run 'app'** en la barra superior.

---

## 3. Conexión de Red con el Backend Spring Boot

La aplicación incluye un selector inteligente de URL base en la pantalla de inicio de sesión (*"Configurar servidor API"*):

* **Emulador de Android Studio:** Por defecto apunta a `http://10.0.2.2:8080/` (que es el alias nativo del emulador para conectarse al `localhost:8080` de tu computadora donde corre Spring Boot).
* **Producción en la Nube (Render):** `https://tecniservicios.onrender.com/` (disponible 24/7).
* **Teléfono físico conectado a la misma red WiFi:** `http://192.168.x.x:8080/` (reemplazando por tu IP local de Windows).

---

## 4. Módulos y Funcionalidades Integradas (Esencia Completa)

1. **Autenticación y Seguridad:**
   * Desafío matemático CAPTCHA stateless (`GET /api/auth/captcha`) con firma HMAC-SHA256.
   * Login con JWT (`POST /api/auth/login`), guardando `accessToken` y `refreshToken` en `SessionManager`.
   * Bloqueo de cuentas en backend tras 5 intentos fallidos (15 min).
   * Auto-registro de clientes con validación de contraseña.

2. **Catálogo y Tienda de Seguridad:**
   * Cuadrícula de 2 columnas con fotos de productos, badges de descuento (`-15%`) y stock disponible en tiempo real.
   * Filtros por categoría (CCTV, Alarmas, Control de Acceso) y barra de búsqueda reactiva.
   * Pantalla de detalle de producto con especificaciones técnicas completas y botón para añadir al carrito.

3. **Carrito de Compras y Checkout:**
   * Carrito reactivo local (`CarritoManager`).
   * Precarga automática de la dirección predeterminada del cliente desde el backend.
   * Checkout con cálculo de IVA (19%) y emisión de orden con factura fiscal y **CUFE SHA-384**.

4. **Direcciones de Entrega (Regla de Máximo 15):**
   * CRUD de direcciones con etiqueta (Casa, Oficina, Bodega), barrio y teléfono.
   * Contador visual y validación estricta de tope de **15 direcciones por cliente**.

5. **Tareas Operativas v2.0 (Perfil Técnico / Empleado):**
   * Visible automáticamente si el usuario autenticado tiene rol `EMPLEADO` o `ADMIN`.
   * Consulta de tareas asignadas con semáforo de prioridad (Alta, Media, Baja).
   * **Bloqueo estricto v2.0:** Al entregar la tarea con evidencia, el sistema la bloquea en solo lectura para impedir modificaciones mientras el administrador revisa.
   * Consulta del historial inmutable de intentos y observaciones de revisión.

6. **Asistente Virtual DeepSeek AI:**
   * Interfaz de chat integrada para soporte técnico 24/7 de cámaras y cotizaciones.

---

## 5. Estructura de Archivos del Proyecto Móvil

```text
TecniSecureMobile/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/tecniservicios/tecnisecure/
│   │   │   ├── data/
│   │   │   │   ├── api/          # RetrofitClient, ApiService, AuthInterceptor
│   │   │   │   ├── local/        # SessionManager (JWT), CarritoManager
│   │   │   │   └── model/        # DTOs: Producto, Pedido, Tarea, Direccion, Captcha
│   │   │   └── ui/
│   │   │       ├── auth/         # LoginActivity, RegisterActivity
│   │   │       ├── main/         # MainActivity (Catálogo y navegación)
│   │   │       ├── catalogo/     # ProductoAdapter, ProductoDetalleActivity
│   │   │       ├── carrito/      # CarritoActivity, CarritoAdapter
│   │   │       ├── pedidos/      # MisPedidosActivity, PedidoAdapter
│   │   │       ├── tareas/       # TareasEmpleadoActivity, TareaAdapter (v2.0)
│   │   │       ├── direcciones/  # DireccionesActivity, DireccionAdapter
│   │   │       └── asistente/    # AsistenteIAActivity, ChatAdapter
│   │   └── res/
│   │       ├── layout/           # 14 layouts XML con Material 3
│   │       ├── values/           # colors.xml (#070F1E, #006B85, #00E5FF, #FF5A36), strings, themes
│   │       └── menu/             # bottom_nav_menu.xml
│   └── build.gradle.kts          # SDK 34, ViewBinding, Retrofit, Glide, Coroutines
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```
