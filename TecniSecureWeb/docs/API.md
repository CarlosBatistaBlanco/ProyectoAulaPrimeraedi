# API REST · TECNISECURE

Documentación de referencia de la API. Swagger UI interactivo:
**http://localhost:8080/swagger-ui.html**

- Base URL: `http://localhost:8080`
- Formato: JSON UTF-8
- Autenticación: `Authorization: Bearer <accessToken>`

---

## Códigos de respuesta

| Código | Significado |
|---|---|
| 200 | Operación correcta |
| 201 | Recurso creado |
| 204 | Eliminado sin contenido |
| 400 | Datos inválidos (incluye `campos` con el detalle) |
| 401 | No autenticado o token expirado |
| 403 | Sin permisos para el rol |
| 404 | Recurso no encontrado |
| 409 | Conflicto (correo, documento o SKU duplicado) |

Formato de error:

```json
{
  "timestamp": "2026-11-07T01:20:00",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Datos invalidos",
  "ruta": "/api/pedidos",
  "campos": { "correo": "El correo no es valido" }
}
```

---

## 1. Autenticación — `/api/auth`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/login` | público | Devuelve `accessToken`, `refreshToken` y `usuario` |
| POST | `/refresh` | público | Renueva el access token |
| POST | `/registro` | público | Crea una cuenta de cliente |
| GET | `/captcha` | público | Pregunta matemática de verificación |
| POST | `/recuperar` | público | Genera token de recuperación |
| POST | `/restablecer` | público | Cambia la contraseña con el token |
| GET | `/me` | autenticado | Datos del usuario |
| PUT | `/me` | autenticado | Actualiza nombre, teléfono o contraseña |

**Login**

```http
POST /api/auth/login
Content-Type: application/json

{ "correo": "admin@tecnicsecure.com", "password": "Admin123!" }
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiraEnSegundos": 7200,
  "usuario": { "idUsuario": 1, "nombre": "Administrador Principal",
               "correo": "admin@tecnicsecure.com", "rol": "administrador" }
}
```

**Registro** — la contraseña exige mínimo 8 caracteres, una mayúscula y un número.

```json
{ "nombre": "Ana Martínez", "documento": "1234567890",
  "correo": "ana@ejemplo.com", "telefono": "3001234567", "password": "Clave123" }
```

---

## 2. Catálogo — `/api`

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/productos?texto=&idCategoria=&idMarca=&pagina=0&tamanio=12&orden=idProducto` | público |
| GET | `/productos/destacados` | público |
| GET | `/productos/{id}` | público |
| POST | `/productos` | administrador, empleado |
| PUT | `/productos/{id}` | administrador, empleado |
| DELETE | `/productos/{id}` | administrador (baja lógica) |
| GET | `/categorias` · `/marcas` · `/servicios` | público |
| POST | `/categorias` · `/marcas` | administrador, empleado |

Respuesta paginada:

```json
{
  "contenido": [
    { "idProducto": 1, "nombre": "Cámara Domo 4K IP", "sku": "CAM-DOM-4K-001",
      "precio": 249900.00, "precioFinal": 212415.00, "descuentoPct": 15.00,
      "stock": 25, "stockBajo": false, "imagen": "img/productos/camara-domo.jpg",
      "destacado": true, "categoria": "Cámaras de Seguridad", "marca": "Hikvision" }
  ],
  "pagina": 0, "tamanio": 12, "totalElementos": 1, "totalPaginas": 1, "ultima": true
}
```

> `precioFinal` ya incluye el descuento vigente. Ese es el valor que debe mostrarse
> y cobrarse; el backend lo recalcula igualmente al registrar el pedido.

---

## 3. Pedidos — `/api/pedidos`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/` | público / cliente | Checkout del carrito |
| GET | `/mis-pedidos` | cliente | Pedidos propios |
| GET | `/?idCliente=&estado=&pagina=&tamanio=` | administrador, empleado | Listado |
| GET | `/{id}` | propietario o gestor | Detalle |
| PATCH | `/{id}/estado` | administrador, empleado | Cambia el estado |
| GET | `/{id}/historial` | autenticado | Historial de estados |

**Checkout**

```json
{
  "nombre": "Ana Martínez", "correo": "ana@ejemplo.com",
  "telefono": "3001234567", "direccion": "Calle 1 #2-3",
  "carrito": [ { "idProducto": 1, "cantidad": 2 } ]
}
```

El precio **no** se envía desde el navegador: el servidor lo toma de la base de
datos, valida el stock, descuenta existencias y registra el movimiento en el kardex.

**Flujo de estados**

```
pendiente → confirmado → en_preparacion → empacado → enviado → entregado
      ↘          ↘              ↘             ↘          ↘
                        cancelado (desde cualquier estado no entregado)
```

Un salto no permitido devuelve **400**.

---

## 4. Inventario — `/api/inventario` *(administrador, empleado)*

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` | Existencias de todos los productos |
| GET | `/stock-bajo` | Productos en o por debajo del mínimo |
| GET | `/productos/{id}/kardex` | Historial del producto |
| GET | `/movimientos` | Últimos 50 movimientos |
| POST | `/productos/{id}/movimientos` | Registrar movimiento |

```json
{ "tipo": "entrada", "cantidad": 15, "motivo": "Compra a proveedor" }
```

`tipo` admite `entrada` (suma), `salida` (resta, valida stock) y `ajuste`
(fija el stock al valor indicado).

---

## 5. Soporte — `/api`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/contactos` | público | Mensaje, reclamo o sugerencia |
| GET | `/contactos?soloNoLeidos=true` | administrador | Bandeja de entrada |
| POST | `/contactos/{id}/respuesta` | administrador | Responder |
| POST | `/solicitudes` | público | Cotización o cita técnica |
| GET | `/solicitudes?estado=` | administrador | Listado |
| PATCH | `/solicitudes/{id}` | administrador | Técnico, estado y observaciones |
| GET | `/chat/mi-conversacion` | cliente | Conversación propia |
| POST | `/chat/mi-conversacion` | cliente | Enviar mensaje |
| GET | `/chat/clientes` | administrador, empleado | Clientes para abrir su chat |
| GET | `/chat/{idCliente}` | administrador, empleado | Conversación del cliente |
| POST | `/chat/{idCliente}` | administrador, empleado | Responder |
| GET | `/notificaciones` | autenticado | Notificaciones |
| GET | `/notificaciones/sin-leer` | autenticado | Contador |
| PATCH | `/notificaciones/leidas` | autenticado | Marcar como leídas |

**Solicitud de servicio**

```json
{ "idServicio": 1, "nombre": "Ana Martínez", "correo": "ana@ejemplo.com",
  "telefono": "3001234567", "direccion": "Calle 1", "tipo": "cita",
  "fechaPreferida": "2026-11-20", "horaPreferida": "10:00",
  "descripcion": "Instalación de 3 cámaras" }
```

### Tiempo real (WebSocket STOMP)

| Canal | Contenido |
|---|---|
| `/topic/chat/{idCliente}` | Mensajes del hilo de conversación |
| `/topic/notificaciones/{idUsuario}` | Notificaciones del usuario |

Endpoint de conexión: `ws://localhost:8080/ws`

El cliente del navegador está en `frontend/js/websocket.js` (STOMP sobre
WebSocket nativo, sin dependencias externas). El panel se suscribe
automáticamente y mantiene el sondeo cada 4 s como respaldo si el
WebSocket no está disponible.

Para suscribirse al chat, el cliente necesita su `idCliente`; por eso
`GET /api/auth/me` (y la respuesta del login) lo incluyen cuando el rol
es `cliente`:

```json
{ "idUsuario": 7, "nombre": "Ana Martínez", "rol": "cliente", "idCliente": 5 }
```

---

## 6. Reportes — `/api/reportes` *(administrador, empleado)*

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/resumen?desde=&hasta=` | Indicadores del dashboard |
| GET | `/productos-mas-vendidos?desde=&hasta=&limite=10` | Ranking |
| GET | `/ingresos?desde=&hasta=` | Ingresos del período |
| GET | `/ventas/export?formato=csv\|xml` | Descarga de ventas |
| GET | `/inventario/export` | Descarga de indicadores |

Las fechas usan formato ISO (`2026-11-01`). El CSV incluye BOM UTF-8 para que
Excel respete las tildes y la ñ.

---

## 7. Cliente — `/api/cliente`

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/favoritos` | Productos favoritos |
| POST | `/favoritos/{idProducto}` | Agregar o quitar (alterna) |
| GET | `/productos/{id}/resenas` | Reseñas del producto |
| POST | `/productos/{id}/resenas` | Reseñar (una vez por usuario) |

```json
{ "calificacion": 5, "comentario": "Excelente calidad de imagen" }
```

---

## 8. Ejemplo completo con curl

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"correo\":\"admin@tecnicsecure.com\",\"password\":\"Admin123!\"}"

# 2. Guardar el token
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 3. Consultar inventario
curl http://localhost:8080/api/inventario -H "Authorization: Bearer $TOKEN"

# 4. Cambiar el estado de un pedido
curl -X PATCH http://localhost:8080/api/pedidos/1/estado \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"estado\":\"confirmado\"}"
```
