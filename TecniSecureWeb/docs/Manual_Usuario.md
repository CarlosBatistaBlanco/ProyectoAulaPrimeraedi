# Manual de Usuario del Sistema Web TECNISECURE

**Proyecto:** TECNISECURE (Tienda en línea, servicios técnicos y paneles operativos)  
**Versión del software:** 2.0.0  
**Fecha de actualización:** 11 de noviembre de 2026  

---

## Tabla de contenido

1. [Introducción](#1-introducción)
2. [Objetivos](#2-objetivos)
   - 2.1 [Objetivo general](#21-objetivo-general)
   - 2.2 [Objetivos específicos](#22-objetivos-específicos)
3. [Alcance, requisitos y glosario básico](#3-alcance-requisitos-y-glosario-básico)
4. [Desarrollo operativo por perfil de usuario](#4-desarrollo-operativo-por-perfil-de-usuario)
   - 4.1 [Ingreso, registro y recuperación de contraseña](#41-ingreso-registro-y-recuperación-de-contraseña)
   - 4.2 [Navegación pública para visitantes](#42-navegación-pública-para-visitantes)
   - 4.3 [Panel del cliente](#43-panel-del-cliente)
   - 4.4 [Panel del empleado](#44-panel-del-empleado)
   - 4.5 [Panel del administrador](#45-panel-del-administrador)
   - 4.6 [Solución a problemas frecuentes de acceso y visualización](#46-solución-a-problemas-frecuentes-de-acceso-y-visualización)
5. [Conclusiones](#5-conclusiones)
6. [Referencias](#6-referencias)

---

## 1. Introducción

Este manual describe el manejo de la aplicación web TECNISECURE desde el navegador. La plataforma atiende la venta de equipos de seguridad electrónica, la solicitud de instalaciones o soporte técnico y la administración interna del negocio. Todo el sitio funciona de forma unificada en `http://localhost:8080`, donde el servidor embebido entrega tanto las páginas visuales como los servicios de datos (Broadcom, 2024; TECNISECURE, 2026).

Cada módulo visible depende del perfil con el que ingresa la persona. Un visitante sin sesión recorre el catálogo y pide cotizaciones; un cliente registrado realiza pedidos, descarga facturas y conversa por chat; un empleado atiende únicamente las áreas habilitadas para su cargo; y el administrador controla usuarios, inventario, reportes y auditoría.

---

## 2. Objetivos

### 2.1 Objetivo general
Guiar paso a paso a visitantes, clientes, empleados y administradores en el uso correcto de las pantallas, formularios y reportes disponibles en TECNISECURE.

### 2.2 Objetivos específicos
- Explicar el proceso de registro, autenticación, bloqueo preventivo por intentos fallidos y recuperación de contraseña.
- Detallar el recorrido de compra del cliente, incluyendo mapa de entrega, cambio de estados del pedido, consulta de factura y chat en vivo.
- Documentar el flujo de trabajo del empleado para gestionar tareas asignadas, subir evidencias, actualizar pedidos y mover inventario.
- Indicar al administrador cómo configurar permisos por área, revisar reportes exportables, vigilar el registro de auditoría y generar copias de respaldo.

---

## 3. Alcance, requisitos y glosario básico

Este documento cubre la versión 2.0.0 del sistema desplegada en entorno local o servidor HTTP/HTTPS. No describe programación interna ni instalación de herramientas de desarrollo, temas tratados en `docs/Manual_Tecnico.md`.

Para operar la interfaz el usuario necesita un navegador web actualizado (Google Chrome, Microsoft Edge o Mozilla Firefox) con JavaScript habilitado y conexión hacia el servidor de la aplicación.

Terminología usada en las pantallas:
- **Token de sesión (JWT):** credencial digital emitida al iniciar sesión que identifica al usuario ante el sistema sin guardar estado en la memoria del servidor (Jones et al., 2015).
- **WebSocket:** canal de comunicación bidireccional que permite ver mensajes de chat y alertas de campana sin recargar manualmente la página (Fette & Melnikov, 2011).
- **Stock mínimo:** límite configurado por producto que activa avisos cuando las unidades disponibles bajan de ese umbral.

---

## 4. Desarrollo operativo por perfil de usuario

### 4.1 Ingreso, registro y recuperación de contraseña

El punto de entrada al sitio público es `index.html`. Desde la barra superior se accede a los formularios de cuenta (TECNISECURE, 2026):

1. **Inicio de sesión (`login.html`):** solicita correo electrónico, contraseña y el resultado de una suma matemática de verificación (por ejemplo, `¿cuánto es 3 + 4?`). El campo de contraseña incluye botón de ojo para mostrar u ocultar el texto, casilla `Recordarme` y el enlace `¿Olvidaste tu contraseña?`. Cuando una cuenta acumula cinco intentos erróneos seguidos, el acceso queda bloqueado durante 15 minutos.
2. **Registro de cuenta (`registro.html`):** pide nombre completo, documento de identidad, correo electrónico, teléfono y una contraseña de mínimo ocho caracteres con al menos una letra mayúscula y un número. Hay que repetir la contraseña, marcar la casilla de aceptación de términos y pulsar `Crear mi cuenta`. Todo usuario registrado por esta vía recibe rol `cliente`.
3. **Recuperación de contraseña (`recuperar.html` y `restablecer.html`):** solicita el correo registrado para generar un enlace o token temporal y permite definir una nueva clave válida.

Para pruebas iniciales en entorno local existe la cuenta administradora `admin@tecnicsecure.com` con clave `Admin123!`, definida en `backend/src/main/resources/application.yml` (TECNISECURE, 2026).

Dentro de todos los paneles internos, la franja superior presenta selector de tema claro o oscuro, reloj en tiempo real con la fecha y hora (`America/Bogota`) actualizado cada segundo, campana de notificaciones y botón de cierre de sesión.

### 4.2 Navegación pública para visitantes

Sin haber iniciado sesión, cualquier persona puede consultar la tienda y enviar solicitudes:

1. **Catálogo (`pages/productos.html`):** muestra los equipos filtrables por palabra clave, categoría (`Cámaras de Seguridad`, `Alarmas`, `Control de Acceso`, `Accesorios`) o marca (`Hikvision`, `Dahua`, `TP-Link`, `Genérica`). Al abrir un producto se revisa su precio, descuento, existencias y reseñas.
2. **Lista de deseos (`pages/lista_deseos.html`) y carrito:** permite guardar artículos de interés y calcular subtotales con IVA en el carrito lateral. Al intentar confirmar la compra, el sitio solicita iniciar sesión.
3. **Solicitar servicio técnico (`pages/solicitar-servicio.html`):** ofrece un formulario para elegir entre los seis servicios activos (`Instalación de Cámaras`, `Monitoreo 24/7`, `Alarmas y Sensores`, `Control de Acceso`, `Mantenimiento Preventivo`, `Asesoría y Diseño`), describir la necesidad y enviar el caso.
4. **Contacto general (`index.html`):** envía mensajes hacia la bandeja de atención al cliente.

### 4.3 Panel del cliente

Al autenticar una cuenta con rol `cliente`, el navegador abre `panel/cliente/index.html` con las opciones `Dashboard`, `Mi perfil`, `Mis pedidos`, `Favoritos`, `Chat con un asesor` e `Ir a la tienda`.

- **Compra y checkout:** con artículos en el carrito, el cliente llena los datos de entrega (pudiendo ubicar su dirección en el mapa mediante coordenadas GPS) y selecciona un método de pago. Confirmada la orden, el sistema descuenta el inventario y genera la factura correspondiente.
- **Mis pedidos y facturas (`panel/cliente/pedidos.html`):** presenta cada compra con fecha, hora, valor total y estado (`pendiente`, `confirmado`, `en_preparacion`, `empacado`, `enviado`, `entregado` o `cancelado`). Desde cada fila es posible revisar el historial de cambios y abrir la factura formal para verla en pantalla, descargarla como archivo HTML imprimible (o guardarla en PDF desde el diálogo de impresión del navegador) o solicitar su envío por correo electrónico cuando el servicio SMTP está habilitado (TECNISECURE, 2026).
- **Favoritos (`panel/cliente/favoritos.html`) y Mi perfil (`panel/cliente/perfil.html`):** la primera pantalla agrupa los productos guardados exclusivamente por la cuenta activa; la segunda permite cambiar nombre, teléfono o contraseña.
- **Chat con un asesor (`panel/cliente/chat.html`):** atiende dudas iniciales mediante un asistente automático (`BotAsistenteService`) y ofrece el botón de solicitud de asesor para transferir el hilo hacia personal conectado mediante WebSocket o sondeo periódico (Fette & Melnikov, 2011; TECNISECURE, 2026).

### 4.4 Panel del empleado

Quienes ingresan con rol `empleado` trabajan en `panel/empleado/index.html`. El menú lateral incluye `Dashboard`, `Mis tareas`, `Mi perfil`, `Registrar venta`, `Pedidos`, `Inventario`, `Productos`, `Soporte` y `Chat clientes`, aunque solo aparecen visibles los módulos asignados por el administrador según el área del trabajador (`OPERATIVO`, `VENTAS`, `SOPORTE`, `INVENTARIO` o `TECNICO`).

1. **Mis tareas (`tareas.html`):** lista las actividades asignadas con su fecha límite. El empleado actualiza el progreso (`pendiente`, `en_progreso`, `completada`), redacta observaciones y adjunta archivos de evidencia.
2. **Pedidos (`pedidos.html`):** muestra las órdenes de clientes y deja avanzar el estado operativo escribiendo una nota para la bitácora del pedido.
3. **Inventario (`inventario.html`) y Productos (`productos.html`):** permite revisar existencias, identificar artículos con stock bajo y registrar entradas, salidas o ajustes indicando el motivo.
4. **Registrar venta (`ventas.html`):** módulo dedicado para asentar ventas directas o asistidas.
5. **Soporte (`soporte.html`) y Chat clientes (`chat.html`):** concentra solicitudes técnicas, mensajes del formulario web y conversaciones de clientes ordenadas con vista previa y fecha del último mensaje; los hilos donde se pidió atención humana se marcan con `Solicita asesor`.

### 4.5 Panel del administrador

El rol `administrador` entra a `panel/administrador/index.html` con control sobre toda la operación:

| Módulo del menú | Archivo | Operaciones principales |
|---|---|---|
| `Dashboard` | `index.html` | Conteo de usuarios, clientes, productos, pedidos, ingresos y alertas con enlaces rápidos |
| `Usuarios` | `usuarios.html` | Alta, edición, cambio de rol, activación, desbloqueo de cuentas y descarga CSV |
| `Mi perfil` | `perfil.html` | Edición de datos propios y cambio de clave del administrador |
| `Empleados` | `empleados.html` | Asignación de área, permisos por módulo y generación de actividades según rol |
| `Tareas y Desempeño` | `tareas.html` | Creación de tareas, revisión de evidencias, calificación y porcentaje de cumplimiento |
| `Clientes` | `clientes.html` | Consulta del directorio de compradores y sus datos de contacto |
| `Productos` | `productos.html` | Gestión de catálogo, categorías, marcas, precios, descuentos, IVA e imágenes |
| `Inventario` | `inventario.html` | Trazabilidad de movimientos de bodega y control de stock mínimo |
| `Pedidos` | `pedidos.html` | Seguimiento de órdenes, cambios de estado e historial |
| `Facturación` | `facturacion.html` | Consulta de facturas, emisión de notas crédito, descarga individual y envío por correo |
| `Reportes` | `reportes.html` | Análisis por fechas, productos más vendidos y exportación en CSV y XML |
| `Atención al cliente` | `soporte.html` | Asignación de técnicos a solicitudes y lectura de mensajes de contacto |
| `Chat con clientes` | `chat.html` | Atención en tiempo real de conversaciones activas |
| `Auditoría` | `auditoria.html` | Revisión de registros de `logs_actividad` y depuración de eventos mayores a 90 días |
| `Respaldo de BD` | `respaldo.html` | Descarga de respaldo general del sistema en formato JSON |

### 4.6 Solución a problemas frecuentes de acceso y visualización

- **La pantalla no muestra cambios recientes o datos nuevos:** pulsar `Ctrl + F5` en el navegador para vaciar caché de scripts y hojas de estilo.
- **El botón de envío de factura por correo no entrega mensajes reales:** confirmar con el administrador técnico que `MAIL_ENABLED=true` y las credenciales SMTP estén configuradas en el servidor.
- **Cuenta bloqueada al iniciar sesión:** esperar 15 minutos desde el último intento o pedir al administrador que desbloquee el usuario desde `Usuarios`.

---

## 5. Conclusiones

La interfaz de TECNISECURE organiza sus pantallas de acuerdo con las responsabilidades reales de cada usuario, evitando que visitantes o empleados accedan a opciones administrativas ajenas a su área. Separar los flujos públicos de la tienda respecto a los paneles autenticados simplifica el recorrido de compra para el cliente y mantiene ordenadas las tareas operativas de inventario, facturación y soporte técnico. Contar con seguimiento en vivo de chat, alertas de stock mínimo, descarga individual de facturas y exportación directa hacia CSV o XML cubre las actividades cotidianas de operación sin exigir conocimientos técnicos avanzados al usuario final.

---

## 6. Referencias

Broadcom. (2024). *Spring Boot Reference Documentation (Version 3.3)*. https://docs.spring.io/spring-boot/3.3/reference/index.html

Fette, I., & Melnikov, A. (2011). *RFC 6455: The WebSocket Protocol*. Internet Engineering Task Force (IETF). https://www.rfc-editor.org/info/rfc6455

Jones, M., Bradley, J., & Sakimura, N. (2015). *RFC 7519: JSON Web Token (JWT)*. Internet Engineering Task Force (IETF). https://www.rfc-editor.org/info/rfc7519

TECNISECURE. (2026). *Código fuente, vistas HTML y documentación interna de TECNISECURE (Versión 2.0.0)* [Software]. Repositorio `TecniServicios` (`README.md`, `frontend/`, `backend/src/main/resources/application.yml`).
