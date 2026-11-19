/**
 * api.js — Cliente de la API REST de TECNISECURE
 * -----------------------------------------------------------------
 * Centraliza TODAS las llamadas HTTP al backend Spring Boot:
 *   · Guarda el token JWT en localStorage.
 *   · Añade la cabecera Authorization automáticamente.
 *   · Renueva el token cuando expira (refresh token).
 *   · Traduce los errores de la API a mensajes en español.
 * Inclúyelo ANTES que los demás scripts.
 */

const API_CONFIG = {
  // Mismo origen que sirve el frontend (Spring Boot en :8080)
  base: window.API_BASE || `${window.location.origin}/api`,
  tokenKey: 'tecnicsecure_token',
  refreshKey: 'tecnicsecure_refresh',
  usuarioKey: 'tecnicsecure_usuario'
};

/**
 * Ruta relativa desde la pagina actual hasta la raiz del frontend.
 * Se calcula con la URL real, asi funciona tanto en
 *   /panel/administrador/index.html   (espera "../../")
 * como en
 *   /panel/administrador              (espera "../../" tambien)
 * o en paginas de la raiz             ("").
 */
function calcularRutaBase() {
  const partes = window.location.pathname.split('/').filter(Boolean);
  // Si la ultima parte es un archivo (tiene punto) es la propia pagina
  const esArchivo = partes.length > 0 && partes[partes.length - 1].includes('.');
  const carpetas = esArchivo ? partes.length - 1 : partes.length;
  return carpetas === 0 ? '' : '../'.repeat(carpetas);
}

// Si la pagina no definio RUTA_BASE (o lo hizo de forma fija), se calcula sola.
window.RUTA_BASE = calcularRutaBase();

const Api = {
  /* ---------------------------- Sesión ---------------------------- */

  getToken() {
    return localStorage.getItem(API_CONFIG.tokenKey);
  },

  getRefreshToken() {
    return localStorage.getItem(API_CONFIG.refreshKey);
  },

  getUsuario() {
    const datos = localStorage.getItem(API_CONFIG.usuarioKey);
    return datos ? JSON.parse(datos) : null;
  },

  estaAutenticado() {
    return Boolean(this.getToken());
  },

  guardarSesion(respuesta) {
    localStorage.setItem(API_CONFIG.tokenKey, respuesta.accessToken);
    if (respuesta.refreshToken) {
      localStorage.setItem(API_CONFIG.refreshKey, respuesta.refreshToken);
    }
    if (respuesta.usuario) {
      localStorage.setItem(API_CONFIG.usuarioKey, JSON.stringify(respuesta.usuario));
      document.dispatchEvent(new CustomEvent('sesion:cambiada', { detail: respuesta.usuario }));
    }
  },

  cerrarSesion() {
    localStorage.removeItem(API_CONFIG.tokenKey);
    localStorage.removeItem(API_CONFIG.refreshKey);
    localStorage.removeItem(API_CONFIG.usuarioKey);
    document.dispatchEvent(new CustomEvent('sesion:cerrada'));
  },

  /* --------------------------- Peticiones -------------------------- */

  async _peticion(ruta, opciones = {}, reintentar = true) {
    const cabeceras = { 'Accept': 'application/json', ...(opciones.headers || {}) };

    if (opciones.body !== undefined && !(opciones.body instanceof FormData)) {
      cabeceras['Content-Type'] = 'application/json';
      if (typeof opciones.body !== 'string') {
        opciones.body = JSON.stringify(opciones.body);
      }
    }

    const token = this.getToken();
    if (token) {
      cabeceras['Authorization'] = `Bearer ${token}`;
    }

    let respuesta;
    try {
      respuesta = await fetch(`${API_CONFIG.base}${ruta}`, { ...opciones, headers: cabeceras });
    } catch (errorRed) {
      throw new Error('No se pudo conectar con el servidor. Verifica que el backend esté encendido.');
    }

    // Token expirado → intentar renovarlo una sola vez
    if (respuesta.status === 401 && reintentar && this.getRefreshToken()) {
      if (await this._renovarToken()) {
        return this._peticion(ruta, opciones, false);
      }
      this.cerrarSesion();
    }

    if (respuesta.status === 204) {
      return null;
    }

    const tipo = respuesta.headers.get('content-type') || '';
    const cuerpo = tipo.includes('application/json') ? await respuesta.json() : await respuesta.text();

    if (!respuesta.ok) {
      const mensaje = (cuerpo && (cuerpo.mensaje || cuerpo.message)) || `Error ${respuesta.status}`;
      const error = new Error(mensaje);
      error.status = respuesta.status;
      error.campos = cuerpo && cuerpo.campos;
      throw error;
    }

    return cuerpo;
  },

  async _renovarToken() {
    try {
      const respuesta = await fetch(`${API_CONFIG.base}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: this.getRefreshToken() })
      });
      if (!respuesta.ok) return false;
      this.guardarSesion(await respuesta.json());
      return true;
    } catch {
      return false;
    }
  },

  get: (ruta) => Api._peticion(ruta, { method: 'GET' }),
  post: (ruta, body) => Api._peticion(ruta, { method: 'POST', body }),
  put: (ruta, body) => Api._peticion(ruta, { method: 'PUT', body }),
  patch: (ruta, body) => Api._peticion(ruta, { method: 'PATCH', body }),
  delete: (ruta, cuerpo) => Api._peticion(ruta, { method: 'DELETE', body: cuerpo }),

  /* --------------------------- Catálogo ---------------------------- */

  productos(params = {}) {
    const query = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString();
    return this.get(`/productos${query ? `?${query}` : ''}`);
  },

  destacados: () => Api.get('/productos/destacados'),
  producto: (id) => Api.get(`/productos/${id}`),
  crearProducto: (datos) => Api.post('/productos', datos),
  actualizarProducto: (id, datos) => Api.put(`/productos/${id}`, datos),
  eliminarProducto: (id) => Api.delete(`/productos/${id}`),
  categorias: () => Api.get('/categorias'),
  marcas: () => Api.get('/marcas'),
  crearMarca: (datos) => Api.post('/marcas', datos),
  crearCategoria: (datos) => Api.post('/categorias', datos),
  servicios: () => Api.get('/servicios'),

  /* --------------------- Usuarios y Auditoría ---------------------- */

  usuarios(params = {}) {
    const query = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString();
    return Api.get(`/usuarios${query ? `?${query}` : ''}`);
  },
  usuario: (id) => Api.get(`/usuarios/${id}`),
  crearUsuario: (datos) => Api.post('/usuarios', datos),
  actualizarUsuario: (id, datos) => Api.put(`/usuarios/${id}`, datos),
  eliminarUsuario: (id) => Api.delete(`/usuarios/${id}`),
  eliminarUsuarioDefinitivo: (id, clave) => Api.delete(`/usuarios/${id}/definitivo`, { clave }),
  cambiarEstadoUsuario: (id, activo) => Api.patch(`/usuarios/${id}/estado`, { activo }),
  auditoria: (params = {}) => {
    const query = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString();
    return Api.get(`/auditoria${query ? `?${query}` : ''}`);
  },
  urlExportarAuditoria: (params = {}) => {
    const query = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString();
    return `${API_CONFIG.base}/auditoria/export${query ? `?${query}` : ''}`;
  },
  depurarAuditoria: (dias = 90) => Api.delete(`/auditoria/depurar?dias=${dias}`),
  urlRespaldoCompleto: () => `${API_CONFIG.base}/respaldo/completo`,

  /* ----------------------------- Pedidos --------------------------- */

  registrarPedido: (datos) => Api.post('/pedidos', datos),
  misPedidos: () => Api.get('/pedidos/mis-pedidos'),
  pedidos: (params = {}) => {
    const query = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString();
    return Api.get(`/pedidos${query ? `?${query}` : ''}`);
  },
  pedido: (id) => Api.get(`/pedidos/${id}`),
  historialPedido: (id) => Api.get(`/pedidos/${id}/historial`),
  facturaPedido: (id) => Api.get(`/pedidos/${id}/factura`),
  enviarFactura: (id) => Api.post(`/pedidos/${id}/enviar-factura`, {}),
  pagarPedido: (id, metodo) => Api.post(`/pedidos/${id}/pagar`, { metodo }),
  notaCreditoFactura: (id, motivo) => Api.post(`/pedidos/${id}/nota-credito`, { motivo }),
  cambiarEstadoPedido: (id, estado, observacion) =>
    Api.patch(`/pedidos/${id}/estado`, { estado, observacion }),

  /* --------------------------- Inventario -------------------------- */

  stockBajo: () => Api.get('/inventario/stock-bajo'),
  inventario: () => Api.get('/inventario'),
  kardex: (idProducto) => Api.get(`/inventario/productos/${idProducto}/kardex`),
  movimientos: () => Api.get('/inventario/movimientos'),
  registrarMovimiento: (idProducto, datos) =>
    Api.post(`/inventario/productos/${idProducto}/movimientos`, datos),

  /* ----------------------------- Soporte --------------------------- */

  enviarContacto: (datos) => Api.post('/contactos', datos),
  contactos: (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return Api.get(`/contactos${query ? `?${query}` : ''}`);
  },
  responderContacto: (id, respuesta) => Api.post(`/contactos/${id}/respuesta`, { respuesta }),
  solicitarServicio: (datos) => Api.post('/solicitudes', datos),
  solicitudes: (params = {}) => {
    const query = new URLSearchParams(params).toString();
    return Api.get(`/solicitudes${query ? `?${query}` : ''}`);
  },
  gestionarSolicitud: (id, datos) => Api.patch(`/solicitudes/${id}`, datos),

  miConversacion: () => Api.get('/chat/mi-conversacion'),
  enviarMensajePropio: (mensaje) => Api.post('/chat/mi-conversacion', { mensaje }),
  clientesChat: () => Api.get('/chat/clientes'),
  conversacion: (idCliente) => Api.get(`/chat/${idCliente}`),
  responderChat: (idCliente, mensaje) => Api.post(`/chat/${idCliente}`, { mensaje }),

  // Visitantes del sitio web (chat iniciado desde el asistente publico).
  // El id que devuelve la bandeja es "cv-<idConversacion>".
  conversacionVisita: (idConversacion) => Api.get(`/chat/visita/${idConversacion}`),
  responderVisita: (idConversacion, mensaje) => Api.post(`/chat/visita/${idConversacion}`, { mensaje }),

  notificaciones: () => Api.get('/notificaciones'),
  notificacionesSinLeer: () => Api.get('/notificaciones/sin-leer'),
  marcarNotificacionesLeidas: () => Api.patch('/notificaciones/leidas', {}),

  /* ----------------------------- Reportes -------------------------- */

  resumenReporte: (desde, hasta) =>
    Api.get(`/reportes/resumen${Api._rango(desde, hasta)}`),
  productosMasVendidos: (desde, hasta) =>
    Api.get(`/reportes/productos-mas-vendidos${Api._rango(desde, hasta)}`),
  urlExportarVentas: (desde, hasta, formato = 'csv') => {
    const p = new URLSearchParams({ formato });
    if (desde) p.set('desde', desde);
    if (hasta) p.set('hasta', hasta);
    return `${API_CONFIG.base}/reportes/ventas/export?${p.toString()}`;
  },
  urlExportarInventario: (desde, hasta) =>
    `${API_CONFIG.base}/reportes/inventario/export${Api._rango(desde, hasta)}`,

  _rango(desde, hasta) {
    const parametros = new URLSearchParams();
    if (desde) parametros.set('desde', desde);
    if (hasta) parametros.set('hasta', hasta);
    const consulta = parametros.toString();
    return consulta ? `?${consulta}` : '';
  },

  /* ----------------------------- Cliente --------------------------- */

  favoritos: () => Api.get('/cliente/favoritos'),
  alternarFavorito: (idProducto) => Api.post(`/cliente/favoritos/${idProducto}`, {}),
  resenas: (idProducto) => Api.get(`/cliente/productos/${idProducto}/resenas`),
  resenar: (idProducto, datos) => Api.post(`/cliente/productos/${idProducto}/resenas`, datos),

  /* --------------------- Empleados y Tareas ------------------------ */

  miPerfilEmpleado: () => Api.get('/gestion-empleados/mi-perfil'),
  areasActividades: () => Api.get('/gestion-empleados/areas-actividades'),
  misTareas: () => Api.get('/gestion-empleados/mis-tareas'),
  progresoTarea: (id, datos) => Api.patch(`/gestion-empleados/tareas/${id}/progreso`, datos),
  empleadosCarga: () => Api.get('/gestion-empleados/empleados'),
  actualizarPermisosEmpleado: (idUsuario, datos) =>
    Api.put(`/gestion-empleados/empleados/${idUsuario}/permisos`, datos),
  todasTareas: () => Api.get('/gestion-empleados/tareas'),
  asignarTarea: (datos) => Api.post('/gestion-empleados/tareas', datos),
  calificarTarea: (id, datos) => Api.patch(`/gestion-empleados/tareas/${id}/calificar`, datos),
  desempenoEmpleados: () => Api.get('/gestion-empleados/desempeno'),
  accesoSoporteUsuario: (idUsuario) => Api.get(`/gestion-empleados/acceso-soporte/${idUsuario}`),
  urlDescargarFactura: (id) => `${API_CONFIG.base}/pedidos/${id}/factura/archivo`,

  /* ----------------------------- Uploads --------------------------- */

  subirImagen(archivo, tipo = 'productos') {
    const fd = new FormData();
    fd.append('archivo', archivo);
    fd.append('tipo', tipo);
    return Api._peticion('/uploads/imagen', { method: 'POST', body: fd });
  },

  subirEvidencia(archivo) {
    const fd = new FormData();
    fd.append('archivo', archivo);
    return Api._peticion('/uploads/evidencia', { method: 'POST', body: fd });
  },

  /* ---------------------------- Utilidades ------------------------- */

  /** Descarga un archivo protegido enviando el token JWT. */
  async descargar(url, nombreArchivo) {
    const respuesta = await fetch(url, {
      headers: { Authorization: `Bearer ${this.getToken()}` }
    });
    if (!respuesta.ok) {
      throw new Error('No se pudo generar el archivo');
    }
    const blob = await respuesta.blob();
    const enlace = document.createElement('a');
    enlace.href = URL.createObjectURL(blob);
    enlace.download = nombreArchivo;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(enlace.href);
  }
};

/** Formatea un valor como pesos colombianos: 249900 → $249.900 */
function formatearPesos(valor) {
  const numero = Number(valor || 0);
  return '$' + numero.toLocaleString('es-CO', { maximumFractionDigits: 0 });
}

/** Escapa texto antes de insertarlo con innerHTML. */
function escaparHtml(texto) {
  return String(texto ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

window.Api = Api;
