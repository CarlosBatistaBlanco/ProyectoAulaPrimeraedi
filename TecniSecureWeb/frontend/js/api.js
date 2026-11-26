/**
 * Este archivo es el puente central que comunica todas las pantallas de la tienda pública y de los paneles
 * internos con el servidor. Se encarga de guardar y renovar automáticamente el pase de sesión de cada
 * persona, enviar y recibir los datos de productos, pedidos, chat y reportes, traducir los errores a
 * español y ofrecer la opción de instalar la tienda como aplicación en el celular o computador.
 */

// Dirección del servidor y nombres con los que se guarda la sesión del usuario en la memoria del navegador
const API_CONFIG = {
  // Apunta automáticamente al mismo servidor desde donde se abrió la tienda
  base: window.API_BASE || `${window.location.origin}/api`,
  tokenKey: 'tecnicsecure_token',
  refreshKey: 'tecnicsecure_refresh',
  usuarioKey: 'tecnicsecure_usuario'
};

// Calcula cuántos niveles de carpeta hay que subir desde la pantalla actual para llegar a la portada
function calcularRutaBase() {
  const partes = window.location.pathname.split('/').filter(Boolean);
  // Si la última parte lleva un punto, corresponde al nombre de la página actual y no a una carpeta
  const esArchivo = partes.length > 0 && partes[partes.length - 1].includes('.');
  const carpetas = esArchivo ? partes.length - 1 : partes.length;
  return carpetas === 0 ? '' : '../'.repeat(carpetas);
}

// Deja lista la ruta base para que los enlaces funcionen igual desde la portada y desde los paneles
window.RUTA_BASE = calcularRutaBase();

// Centro de comunicación que usan todas las pantallas de la tienda y los paneles para hablar con el servidor
const Api = {
  /* ---------------------------- Sesión ---------------------------- */

  // Lee el pase de sesión actual del usuario desde la memoria del navegador
  getToken() {
    return localStorage.getItem(API_CONFIG.tokenKey);
  },

  // Lee la llave de respaldo que permite renovar la sesión sin obligar a la persona a escribir su clave otra vez
  getRefreshToken() {
    return localStorage.getItem(API_CONFIG.refreshKey);
  },

  // Lee los datos básicos del usuario conectado (nombre, correo, rol) guardados en la memoria del navegador
  getUsuario() {
    const datos = localStorage.getItem(API_CONFIG.usuarioKey);
    return datos ? JSON.parse(datos) : null;
  },

  // Indica si hay una sesión abierta actualmente en el navegador
  estaAutenticado() {
    return Boolean(this.getToken());
  },

  // Guarda el pase de sesión y los datos del usuario tras entrar a su cuenta, y avisa al resto de la pantalla
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

  // Borra la sesión guardada al salir de la cuenta y avisa al carrito, favoritos y barra superior para actualizarse
  cerrarSesion() {
    localStorage.removeItem(API_CONFIG.tokenKey);
    localStorage.removeItem(API_CONFIG.refreshKey);
    localStorage.removeItem(API_CONFIG.usuarioKey);
    document.dispatchEvent(new CustomEvent('sesion:cerrada'));
  },

  /* --------------------------- Comunicaciones -------------------------- */

  // Motor interno que envía cualquier solicitud al servidor, adjunta el pase de sesión y devuelve mensajes claros
  async _peticion(ruta, opciones = {}, reintentar = true) {
    const cabeceras = { 'Accept': 'application/json', ...(opciones.headers || {}) };

    // Si enviamos texto o datos comunes (no archivos), los prepara en el formato estructurado que espera el servidor
    if (opciones.body !== undefined && !(opciones.body instanceof FormData)) {
      cabeceras['Content-Type'] = 'application/json';
      if (typeof opciones.body !== 'string') {
        opciones.body = JSON.stringify(opciones.body);
      }
    }

    // Adjunta automáticamente el pase de sesión del usuario si tiene su cuenta abierta
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

    // Si el pase de sesión venció, intenta renovarlo una vez en segundo plano antes de cerrar la sesión
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

    // Si el servidor responde con un problema, extrae el mensaje claro en español para mostrarlo en pantalla
    if (!respuesta.ok) {
      const mensaje = (cuerpo && (cuerpo.mensaje || cuerpo.message)) || `Error ${respuesta.status}`;
      const error = new Error(mensaje);
      error.status = respuesta.status;
      error.campos = cuerpo && cuerpo.campos;
      throw error;
    }

    return cuerpo;
  },

  // Pide al servidor un nuevo pase de sesión cuando el actual venció para no interrumpir a la persona
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

  // Atajos para consultar, crear, reemplazar, actualizar parcialmente y eliminar datos en el servidor
  get: (ruta) => Api._peticion(ruta, { method: 'GET' }),
  post: (ruta, body) => Api._peticion(ruta, { method: 'POST', body }),
  put: (ruta, body) => Api._peticion(ruta, { method: 'PUT', body }),
  patch: (ruta, body) => Api._peticion(ruta, { method: 'PATCH', body }),
  delete: (ruta, cuerpo) => Api._peticion(ruta, { method: 'DELETE', body: cuerpo }),

  /* --------------------------- Catálogo ---------------------------- */
  // Consultas y administración de productos, categorías, marcas y servicios técnicos

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
  // Gestión de cuentas (clientes, empleados, administradores), historial de seguridad y respaldo del sistema

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
  // Compras de clientes, seguimiento de estados, pagos, facturas electrónicas y notas de crédito

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
  enviarFacturaA: (id, correo) => Api.post(`/pedidos/${id}/enviar-factura-a`, { correo }),
  estadoCorreoFactura: () => Api.get('/pedidos/factura/correo-estado'),
  pagarPedido: (id, metodo) => Api.post(`/pedidos/${id}/pagar`, { metodo }),
  notaCreditoFactura: (id, motivo) => Api.post(`/pedidos/${id}/nota-credito`, { motivo }),
  cambiarEstadoPedido: (id, estado, observacion) =>
    Api.patch(`/pedidos/${id}/estado`, { estado, observacion }),

  /* --------------------------- Inventario -------------------------- */
  // Alertas de existencias bajas, lista de inventario y registro de entradas y salidas de mercancía

  stockBajo: () => Api.get('/inventario/stock-bajo'),
  inventario: () => Api.get('/inventario'),
  kardex: (idProducto) => Api.get(`/inventario/productos/${idProducto}/kardex`),
  movimientos: () => Api.get('/inventario/movimientos'),
  registrarMovimiento: (idProducto, datos) =>
    Api.post(`/inventario/productos/${idProducto}/movimientos`, datos),

  /* ----------------------------- Soporte --------------------------- */
  // Mensajes de contacto, solicitudes de cotización o cita, chat en vivo y campana de notificaciones

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

  // Visitantes de la tienda pública que iniciaron conversación desde el asistente flotante
  conversacionVisita: (idConversacion) => Api.get(`/chat/visita/${idConversacion}`),
  responderVisita: (idConversacion, mensaje) => Api.post(`/chat/visita/${idConversacion}`, { mensaje }),

  notificaciones: () => Api.get('/notificaciones'),
  notificacionesSinLeer: () => Api.get('/notificaciones/sin-leer'),
  marcarNotificacionesLeidas: () => Api.patch('/notificaciones/leidas', {}),

  /* ----------------------------- Reportes -------------------------- */
  // Estadísticas de ventas, productos más vendidos y descarga de informes

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

  // Arma el filtro de fechas desde y hasta para consultar los reportes
  _rango(desde, hasta) {
    const parametros = new URLSearchParams();
    if (desde) parametros.set('desde', desde);
    if (hasta) parametros.set('hasta', hasta);
    const consulta = parametros.toString();
    return consulta ? `?${consulta}` : '';
  },

  /* --------------------------- Direcciones ------------------------- */
  // Libreta de direcciones de entrega guardadas por cada usuario

  misDirecciones: () => Api.get('/direcciones'),
  direccionPrincipal: () => Api.get('/direcciones/principal'),
  crearDireccion: (datos) => Api.post('/direcciones', datos),
  actualizarDireccion: (id, datos) => Api.put(`/direcciones/${id}`, datos),
  marcarDireccionPredeterminada: (id) => Api.patch(`/direcciones/${id}/predeterminada`, {}),
  eliminarDireccion: (id) => Api.delete(`/direcciones/${id}`),

  /* ----------------------------- Cliente --------------------------- */
  // Lista de productos favoritos y reseñas con calificación de productos

  favoritos: () => Api.get('/cliente/favoritos'),
  alternarFavorito: (idProducto) => Api.post(`/cliente/favoritos/${idProducto}`, {}),
  resenas: (idProducto) => Api.get(`/cliente/productos/${idProducto}/resenas`),
  resenar: (idProducto, datos) => Api.post(`/cliente/productos/${idProducto}/resenas`, datos),

  /* --------------------- Empleados y Tareas ------------------------ */
  // Permisos internos de empleados, asignación de tareas técnicas, avances y calificación de desempeño

  miPerfilEmpleado: () => Api.get('/gestion-empleados/mi-perfil'),
  areasActividades: () => Api.get('/gestion-empleados/areas-actividades'),
  misTareas: () => Api.get('/gestion-empleados/mis-tareas'),
  progresoTarea: (id, datos) => Api.patch(`/gestion-empleados/tareas/${id}/progreso`, datos),
  historialTarea: (id) => Api.get(`/gestion-empleados/tareas/${id}/historial`),
  empleadosCarga: () => Api.get('/gestion-empleados/empleados'),
  actualizarPermisosEmpleado: (idUsuario, datos) =>
    Api.put(`/gestion-empleados/empleados/${idUsuario}/permisos`, datos),
  todasTareas: () => Api.get('/gestion-empleados/tareas'),
  asignarTarea: (datos) => Api.post('/gestion-empleados/tareas', datos),
  calificarTarea: (id, datos) => Api.patch(`/gestion-empleados/tareas/${id}/calificar`, datos),
  desempenoEmpleados: () => Api.get('/gestion-empleados/desempeno'),
  accesoSoporteUsuario: (idUsuario) => Api.get(`/gestion-empleados/acceso-soporte/${idUsuario}`),
  urlDescargarFactura: (id) => `${API_CONFIG.base}/pedidos/${id}/factura/archivo`,

  /* ------------------------- Envío de archivos --------------------- */
  // Subida de fotos de productos y archivos de evidencia de tareas al servidor

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

  // Descarga un archivo protegido (factura, reporte o respaldo) enviando el pase de sesión del usuario
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

// Formatea un valor como pesos colombianos (por ejemplo 249900 se ve como $249.900)
function formatearPesos(valor) {
  const numero = Number(valor || 0);
  return '$' + numero.toLocaleString('es-CO', { maximumFractionDigits: 0 });
}

// Protege el texto para evitar código malicioso antes de mostrarlo en pantalla
function escaparHtml(texto) {
  return String(texto ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

// Deja este centro de comunicación disponible para todas las pantallas de la tienda y los paneles
window.Api = Api;

/* --------------------- Instalación de aplicación móvil --------------------- */
// Suprime el aviso PWA de escritorio. La descarga directa del APK nativo queda preparada
// pero desactivada hasta que se habilite explícitamente.
(function controlarInstalacionWeb() {
  if (typeof window === 'undefined') return;

  // Desactiva cualquier Service Worker previo para que el navegador no ofrezca instalar la PWA en PC
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.getRegistrations()
      .then((regs) => regs.forEach((r) => r.unregister()))
      .catch(() => {});
  }

  // Bloquea el cuadro emergente de instalación PWA del navegador en computador y celular
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
  });

  // Elimina cualquier banner previo si existiera en pantalla
  document.getElementById('pwaInstallBanner')?.remove();
})();
