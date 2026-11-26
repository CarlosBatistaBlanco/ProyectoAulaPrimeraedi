"""
Prueba de humo de la API REST (extremo a extremo).
Ejecutar con el backend encendido:  python backend/tools/prueba_e2e.py
"""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:8080"
fallos = []


def peticion(metodo, ruta, cuerpo=None, token=None, esperado=200):
    datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
    req = urllib.request.Request(BASE + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req) as r:
            texto = r.read().decode("utf-8")
            estado = r.status
            tipo = r.headers.get("Content-Type", "")
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8")
        estado = e.code
        tipo = e.headers.get("Content-Type", "")

    cuerpo_resp = json.loads(texto) if "json" in tipo and texto else texto
    marca = "OK " if estado == esperado else "FALLO"
    print(f"[{marca}] {metodo:6} {ruta:45} -> {estado} (esperado {esperado})")
    if estado != esperado:
        fallos.append(f"{metodo} {ruta}: {estado} -> {texto[:200]}")
    return cuerpo_resp


print("=" * 78)
print("PRUEBA END-TO-END · API TECNISECURE")
print("=" * 78)

# --- Público ---
productos = peticion("GET", "/api/productos?tamanio=3")
assert len(productos["contenido"]) == 3, "Debe devolver 3 productos"
print("   ↳ primer producto:", productos["contenido"][0]["nombre"])
print("   ↳ precio final:  ", productos["contenido"][0]["precioFinal"])

destacados = peticion("GET", "/api/productos/destacados")
print("   ↳ destacados:", len(destacados))

categorias = peticion("GET", "/api/categorias")
print("   ↳ categorías:", len(categorias))

servicios = peticion("GET", "/api/servicios")
print("   ↳ servicios:", len(servicios), "| primero:", servicios[0]["titulo"])

peticion("GET", "/api/pedidos", esperado=401)          # sin token
peticion("GET", "/api/inventario", esperado=401)       # sin token

# --- Autenticación ---
login = peticion("POST", "/api/auth/login",
                 {"correo": "admin@tecnicsecure.com", "password": "Admin123!"})
token = login["accessToken"]
print("   ↳ rol:", login["usuario"]["rol"], "| nombre:", login["usuario"]["nombre"])

peticion("POST", "/api/auth/login",
         {"correo": "admin@tecnicsecure.com", "password": "malaclave"}, esperado=401)

peticion("GET", "/api/auth/me", token=token)

refresco = peticion("POST", "/api/auth/refresh", {"refreshToken": login["refreshToken"]})
assert refresco["accessToken"]

# --- Registro de cliente ---
import random
correo = f"cliente{random.randint(1000, 9999)}@prueba.com"
peticion("POST", "/api/auth/registro", {
    "nombre": "Cliente de Prueba",
    "documento": str(random.randint(10000000, 99999999)),
    "correo": correo,
    "telefono": "3001234567",
    "password": "Clave123"
}, esperado=201)

login_cliente = peticion("POST", "/api/auth/login", {"correo": correo, "password": "Clave123"})
token_cliente = login_cliente["accessToken"]

# --- Seguridad por roles ---
peticion("GET", "/api/inventario", token=token_cliente, esperado=403)

# --- Pedido (checkout) ---
producto_id = productos["contenido"][0]["idProducto"]
stock_antes = productos["contenido"][0]["stock"]

pedido = peticion("POST", "/api/pedidos", {
    "nombre": "Cliente de Prueba",
    "correo": correo,
    "telefono": "3001234567",
    "direccion": "Calle 1 #2-3",
    "carrito": [{"idProducto": producto_id, "cantidad": 2}]
}, token=token_cliente, esperado=201)
print("   ↳ pedido:", pedido["idPedido"], "| total:", pedido["total"], "| estado:", pedido["estado"])

detalle = peticion("GET", f"/api/productos/{producto_id}")
print("   ↳ stock: ", stock_antes, "->", detalle["stock"], "(descontado)" if detalle["stock"] < stock_antes else "(SIN DESCUENTO)")

peticion("POST", "/api/pedidos", {
    "nombre": "Cliente de Prueba", "correo": correo,
    "carrito": [{"idProducto": producto_id, "cantidad": 99999}]
}, token=token_cliente, esperado=400)

# --- Flujo de estados ---
peticion("PATCH", f"/api/pedidos/{pedido['idPedido']}/estado", {"estado": "enviado"}, token=token, esperado=400)
peticion("PATCH", f"/api/pedidos/{pedido['idPedido']}/estado", {"estado": "confirmado"}, token=token)
historial = peticion("GET", f"/api/pedidos/{pedido['idPedido']}/historial", token=token)
print("   ↳ historial:", [(h.get("estadoAnterior", "nuevo"), h["estadoNuevo"]) for h in historial])
peticion("GET", "/api/pedidos/mis-pedidos", token=token_cliente)

# --- Inventario ---
bajo = peticion("GET", "/api/inventario/stock-bajo", token=token)
print("   ↳ productos con stock bajo:", len(bajo))
peticion("POST", f"/api/inventario/productos/{producto_id}/movimientos",
         {"tipo": "entrada", "cantidad": 5, "motivo": "Reposición"}, token=token, esperado=201)
kardex = peticion("GET", f"/api/inventario/productos/{producto_id}/kardex", token=token)
print("   ↳ movimientos del kardex:", len(kardex), "| último:", kardex[0]["tipo"], kardex[0]["motivo"])

# --- Soporte ---
peticion("POST", "/api/contactos", {
    "nombre": "Visitante", "correo": "visitante@prueba.com", "telefono": "3000000000",
    "asunto": "Cotización de cámaras", "tipo": "mensaje", "mensaje": "Necesito información."
}, esperado=201)

solicitud = peticion("POST", "/api/solicitudes", {
    "idServicio": servicios[0]["idServicio"],
    "nombre": "Visitante", "correo": "visitante@prueba.com", "telefono": "3000000000",
    "tipo": "cotizacion", "descripcion": "Quiero 3 cámaras."
}, esperado=201)
print("   ↳ solicitud:", solicitud["idSolicitud"], "| servicio:", solicitud["servicio"])

peticion("GET", "/api/solicitudes", token=token)
peticion("PATCH", f"/api/solicitudes/{solicitud['idSolicitud']}",
         {"estado": "agendado", "tecnicoAsignado": "Carlos Pérez"}, token=token)

# --- Notificaciones ---
notis = peticion("GET", "/api/notificaciones", token=token)
sin_leer = peticion("GET", "/api/notificaciones/sin-leer", token=token)
print("   ↳ notificaciones:", len(notis), "| sin leer:", sin_leer["total"])
peticion("PATCH", "/api/notificaciones/leidas", {}, token=token)

# --- Chat ---
peticion("POST", "/api/chat/mi-conversacion", {"mensaje": "Hola, necesito ayuda"}, token=token_cliente)
mi_chat = peticion("GET", "/api/chat/mi-conversacion", token=token_cliente)
print("   ↳ mensajes del chat:", len(mi_chat))

# --- Favoritos ---
peticion("POST", f"/api/cliente/favoritos/{producto_id}", {}, token=token_cliente)
favoritos = peticion("GET", "/api/cliente/favoritos", token=token_cliente)
print("   ↳ favoritos:", len(favoritos))

# --- Reportes ---
resumen = peticion("GET", "/api/reportes/resumen", token=token)
print("   ↳ pedidos:", resumen["cantidadPedidos"], "| ingresos:", resumen["ingresosPeriodo"])
mas = peticion("GET", "/api/reportes/productos-mas-vendidos", token=token)
print("   ↳ más vendidos:", [(p["nombre"], p["unidades"]) for p in mas])

# --- Frontend estático servido por Spring Boot ---
for ruta in ["/", "/index.html", "/pages/productos.html", "/login.html", "/css/style.css", "/js/api.js"]:
    try:
        with urllib.request.urlopen(BASE + ruta) as r:
            print(f"[OK ] GET    {ruta:45} -> {r.status} ({len(r.read())} bytes)")
    except Exception as e:  # noqa: BLE001
        fallos.append(f"frontend {ruta}: {e}")
        print(f"[FALLO] GET  {ruta:45} -> {e}")

# --- Swagger ---
peticion("GET", "/v3/api-docs")

print("=" * 78)
if fallos:
    print(f"RESULTADO: {len(fallos)} FALLO(S)")
    for f in fallos:
        print("  -", f)
    raise SystemExit(1)
print("RESULTADO: TODAS LAS PRUEBAS PASARON ✔")
