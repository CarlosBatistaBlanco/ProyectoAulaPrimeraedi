/**
 * Este archivo da vida a todas las pantallas internas de los paneles de cliente, empleado
 * y administrador. Según la sección que la persona abra, consulta la información actualizada
 * en el servidor y arma las tablas, tarjetas, ventanas emergentes y formularios para manejar
 * usuarios, productos, inventario, pedidos, facturas, tareas con evidencias, soporte, chat,
 * reportes, historial de actividad y copias de respaldo.
 */

// Traduce los estados internos de un pedido a etiquetas claras en español para mostrar al usuario
const NOMBRES_ESTADO = {
  pendiente: 'Pendiente',
  confirmado: 'Confirmado',
  en_preparacion: 'En preparación',
  empacado: 'Empacado',
  enviado: 'Enviado',
  entregado: 'Entregado',
  cancelado: 'Cancelado'
};

// Define cuál es el paso siguiente lógico al avanzar un pedido desde el panel
const SIGUIENTE_ESTADO = {
  pendiente: 'confirmado',
  confirmado: 'en_preparacion',
  en_preparacion: 'empacado',
  empacado: 'enviado',
  enviado: 'entregado'
};

// Devuelve el nombre legible de un estado (por ejemplo "en_preparacion" -> "En preparación")
function estadoLegible(estado) {
  return NOMBRES_ESTADO[estado] || estado || '—';
}

// Convierte fechas recibidas del servidor (texto o arreglo numérico) en un objeto Date válido
function parsearFechaPanel(valor) {
  if (!valor) return null;
  if (Array.isArray(valor) && valor.length >= 3) {
    const [anio, mes, dia, hora = 0, min = 0, seg = 0] = valor;
    const d = new Date(anio, mes - 1, dia, hora, min, seg);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  const d = new Date(valor);
  return Number.isNaN(d.getTime()) ? null : d;
}

// Muestra una fecha en formato corto colombiano (dd/mm/aaaa)
function fechaCorta(valor) {
  const d = parsearFechaPanel(valor);
  return d ? d.toLocaleDateString('es-CO') : '—';
}

// Muestra fecha y hora completas en formato local (dd/mm/aaaa hh:mm)
function fechaHora(valor) {
  const d = parsearFechaPanel(valor);
  return d
    ? d.toLocaleString('es-CO', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
    : '—';
}

// Rellena una tabla con una sola fila centrada cuando todavía no hay datos que mostrar
function tablaVacia(tbody, columnas, mensaje = 'No hay información para mostrar.') {
  tbody.innerHTML = `<tr><td colspan="${columnas}" style="text-align:center; color:var(--texto-tenue);">${mensaje}</td></tr>`;
}

// Da formato de dinero en pesos colombianos sin decimales (por ejemplo $180.000)
function textoImporte(valor) {
  return `$${Number(valor || 0).toLocaleString('es-CO', { maximumFractionDigits: 0 })}`;
}

// Escribe rápidamente un número o texto en una tarjeta estadística identificada por su id
function asignar(id, valor) {
  const elemento = document.getElementById(id);
  if (elemento) elemento.textContent = valor;
}

/** Dibuja una gráfica de barras horizontal liviana con los productos más vendidos. */
function renderizarGraficaBarras(idContenedor, items, mensajeVacio = 'Aún no hay ventas registradas para graficar.') {
  const cont = document.getElementById(idContenedor);
  if (!cont) return;

  if (!items || !items.length) {
    cont.innerHTML = `<p style="color:var(--texto-tenue); font-size:.9rem; margin:0;">${escaparHtml(mensajeVacio)}</p>`;
    return;
  }

  const maxUnidades = Math.max(...items.map((x) => Number(x.unidades || 0)), 1);
  cont.innerHTML = `<div class="panel-grafica">` + items.slice(0, 6).map((item) => {
    const pct = Math.max(6, Math.round((Number(item.unidades || 0) / maxUnidades) * 100));
    return `
      <div class="panel-barra-item">
        <div title="${escaparHtml(item.nombre)}" style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escaparHtml(item.nombre)}</div>
        <div class="panel-barra-track">
          <div class="panel-barra-fill" style="width:${pct}%"></div>
        </div>
        <div style="text-align:right; color:var(--texto-tenue);">${item.unidades} u. (${textoImporte(item.ingresos)})</div>
      </div>`;
  }).join('') + `</div>`;
}

/* --------------------------- Resumen principal admin --------------------------- */

// Carga la pantalla principal del administrador: totales del negocio, alerta de stock bajo, gráfica y pedidos recientes
async function cargarDashboardAdmin() {
  try {
    const [resumen, pedidos, vendidos] = await Promise.all([
      Api.resumenReporte(),
      Api.pedidos({ tamanio: 8 }),
      Api.productosMasVendidos().catch(() => [])
    ]);

    asignar('statUsuarios', resumen.usuariosActivos ?? 0);
    asignar('statClientes', resumen.usuariosActivos ?? 0);
    asignar('statProductos', resumen.productosActivos ?? 0);
    asignar('statPedidos', resumen.cantidadPedidos ?? 0);
    asignar('statVentas', textoImporte(resumen.ingresosHistoricos));

    // Si hay productos con pocas existencias, muestra un aviso llamativo con enlace al inventario
    const aviso = document.getElementById('avisoStockBajo');
    if (aviso && Number(resumen.productosStockBajo) > 0) {
      aviso.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> ${resumen.productosStockBajo} producto(s) con stock bajo. <a href="inventario.html" style="color:var(--cian); text-decoration:underline; margin-left:6px;">Revisar inventario</a>`;
    }

    // Dibuja la tabla de indicadores clicables que llevan directo a cada módulo
    const tablaResumen = document.getElementById('tablaResumenMes');
    if (tablaResumen) {
      const filasResumen = [
        ['Pedidos del período', resumen.cantidadPedidos ?? 0, 'pedidos.html', 'Ver todos los pedidos'],
        ['Ingresos del período', textoImporte(resumen.ingresosPeriodo), 'reportes.html', 'Ver detalle de ventas'],
        ['Ingresos históricos', textoImporte(resumen.ingresosHistoricos), 'facturacion.html', 'Ir a facturación'],
        ['Productos con stock bajo', resumen.productosStockBajo ?? 0, 'inventario.html', 'Gestionar inventario'],
        ['Solicitudes pendientes', resumen.solicitudesPendientes ?? 0, 'soporte.html', 'Atender soporte'],
        ['Mensajes sin leer', resumen.mensajesContactoSinLeer ?? 0, 'soporte.html#contactos', 'Leer mensajes']
      ];
      tablaResumen.innerHTML = filasResumen.map(([k, v, href, tip]) => `
        <tr class="fila-clicable" title="${tip}" onclick="window.location.href='${href}'" style="cursor:pointer;">
          <td>${k} <i class="fa-solid fa-arrow-up-right-from-square" style="font-size:.7rem; opacity:.55; margin-left:4px;"></i></td>
          <td><strong>${v}</strong></td>
        </tr>`).join('');
    }

    renderizarGraficaBarras('graficaVentasDashboard', vendidos);
    pintarPedidos('tablaUltimosPedidos', pedidos.contenido, true);
  } catch (error) {
    console.warn('Dashboard administrador:', error.message);
  }
}

/* -------------------------- Resumen principal cliente -------------------------- */

// Carga el inicio del panel del cliente con su cantidad de pedidos, favoritos y sus últimas 5 compras
async function cargarDashboardCliente() {
  const [pedidos, favoritos] = await Promise.all([
    Api.misPedidos().catch(() => ({ contenido: [] })),
    Api.favoritos().catch(() => [])
  ]);

  asignar('statMisPedidos', pedidos.contenido ? pedidos.contenido.length : 0);
  asignar('statMisFavoritos', favoritos.length || 0);

  const tbody = document.getElementById('tablaMisPedidos');
  if (!tbody) return;

  if (!pedidos.contenido || !pedidos.contenido.length) {
    tablaVacia(tbody, 4, 'Aún no tienes pedidos.');
    return;
  }

  tbody.innerHTML = pedidos.contenido.slice(0, 5).map((p) => `
    <tr>
      <td>#${p.idPedido}</td>
      <td>${fechaCorta(p.creadoEn)}</td>
      <td>${textoImporte(p.total)}</td>
      <td><span class="badge-rol">${estadoLegible(p.estado)}</span></td>
    </tr>`).join('');
}

/* ------------------------- Resumen principal empleado -------------------------- */

// Carga el inicio del panel del empleado con métricas operativas, pedidos recientes y actividades de su área
async function cargarDashboardEmpleado() {
  try {
    const [resumen, pedidos, stock, perfil] = await Promise.all([
      Api.resumenReporte(),
      Api.pedidos({ tamanio: 8 }).catch(() => ({ contenido: [] })),
      Api.stockBajo().catch(() => []),
      Api.miPerfilEmpleado().catch(() => null)
    ]);

    asignar('statPedidosPendientes', resumen.cantidadPedidos ?? 0);
    asignar('statStockBajo', stock.length || resumen.productosStockBajo || 0);
    asignar('statVentasMes', textoImporte(resumen.ingresosPeriodo));
    pintarPedidos('tablaUltimosPedidos', pedidos.contenido, false);

    // Muestra el área asignada al empleado y las actividades sugeridas para su cargo
    if (perfil) {
      const badgeArea = document.getElementById('miAreaDashboardBadge');
      if (badgeArea && perfil.area) badgeArea.textContent = perfil.area;
      const lista = document.getElementById('listaActividadesRolDashboard');
      if (lista) {
        const acts = Array.isArray(perfil.actividadesRol) ? perfil.actividadesRol : [];
        lista.innerHTML = acts.length
          ? acts.map((a) => `
              <li style="background:var(--azul-oscuro); border:1px solid var(--azul-acero); border-radius:8px; padding:10px 12px;">
                <div style="display:flex; justify-content:space-between; gap:8px; align-items:center;">
                  <strong>${escaparHtml(a.titulo || '')}</strong>
                  <span class="badge-rol">${escaparHtml(a.prioridad || 'media')}</span>
                </div>
                ${a.descripcion ? `<div style="font-size:.82rem; color:var(--texto-tenue); margin-top:4px;">${escaparHtml(a.descripcion)}</div>` : ''}
              </li>`).join('')
          : '<li style="color:var(--texto-tenue);">No hay actividades operativas definidas para tu área aún.</li>';
      }
    }
  } catch (error) {
    console.warn('Dashboard empleado:', error.message);
  }
}

/* ------------------------------ Modales -------------------------------- */

// Abre y cierra cualquier ventana emergente (modal) del panel según su identificador
function abrirModal(id) {
  document.getElementById(id)?.classList.add('activo');
}

function cerrarModal(id) {
  document.getElementById(id)?.classList.remove('activo');
}
window.abrirModal = abrirModal;
window.cerrarModal = cerrarModal;

/* ------------------------------ Usuarios ------------------------------- */

let rolFijoUsuarios = '';

// Configura el buscador, filtros y formulario para crear o editar usuarios, clientes o empleados
function inicializarUsuarios() {
  const box = document.querySelector('[data-rol-fijo]');
  rolFijoUsuarios = box ? (box.dataset.rolFijo || '') : '';

  const buscar = document.getElementById('buscarUsuario');
  const filtroRol = document.getElementById('filtroRolUsuario');
  const btnNuevo = document.getElementById('btnNuevoUsuario');
  const form = document.getElementById('formUsuario');

  // Busca automáticamente tras una pequeña pausa mientras el administrador escribe
  let timer;
  if (buscar) {
    buscar.addEventListener('input', () => {
      clearTimeout(timer);
      timer = setTimeout(cargarUsuarios, 250);
    });
  }
  if (filtroRol) filtroRol.addEventListener('change', cargarUsuarios);

  // Botón "Nuevo usuario": limpia el formulario y abre la ventana para crear una cuenta nueva
  if (btnNuevo && form) {
    btnNuevo.addEventListener('click', () => {
      form.reset();
      form.idUsuario.value = '';
      form.activo.checked = true;
      form.bloqueado.checked = false;
      form.rol.value = rolFijoUsuarios || 'cliente';
      form.password.required = true;
      const lblPass = document.getElementById('etiquetaPasswordUsuario');
      if (lblPass) lblPass.textContent = 'Contraseña *';
      const resp = document.getElementById('respuestaUsuario');
      if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
      const titulo = document.getElementById('tituloModalUsuario');
      if (titulo) titulo.innerHTML = `<i class="fa-solid fa-user-plus"></i> Nuevo ${rolFijoUsuarios || 'usuario'}`;
      abrirModal('modalUsuario');
    });
  }

  // Guarda los cambios del formulario (crea usuario nuevo o actualiza el existente) y refresca la tabla
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const resp = document.getElementById('respuestaUsuario');
      resp.textContent = '';
      resp.className = 'form-respuesta campo--full';

      const id = form.idUsuario.value;
      const datos = {
        nombre: form.nombre.value.trim(),
        correo: form.correo.value.trim(),
        documento: form.documento.value.trim() || null,
        telefono: form.telefono.value.trim() || null,
        rol: form.rol.value,
        password: form.password.value ? form.password.value : null,
        activo: form.activo.checked,
        bloqueado: form.bloqueado.checked
      };

      try {
        if (id) {
          await Api.actualizarUsuario(id, datos);
        } else {
          await Api.crearUsuario(datos);
        }
        cerrarModal('modalUsuario');
        await cargarUsuarios();
      } catch (error) {
        resp.textContent = error.message;
        resp.className = 'form-respuesta error campo--full';
      }
    });
  }

  cargarUsuarios();
}

// Consulta la lista de usuarios filtrados y dibuja la tabla con botones de editar, soporte, activar/desactivar y borrar
async function cargarUsuarios() {
  const tbody = document.getElementById('tablaUsuarios');
  if (!tbody) return;

  const texto = (document.getElementById('buscarUsuario')?.value || '').trim();
  const rol = rolFijoUsuarios || (document.getElementById('filtroRolUsuario')?.value || '');

  try {
    const datos = await Api.usuarios({ texto, rol, tamanio: 100 });
    const lista = datos.contenido || [];
    if (!lista.length) {
      tablaVacia(tbody, 7, 'No se encontraron usuarios.');
      return;
    }

    tbody.innerHTML = lista.map((u) => {
      const estadoBadge = u.bloqueado
        ? '<span class="badge-estado badge-estado--bloqueado">Bloqueado</span>'
        : (u.activo
          ? '<span class="badge-estado badge-estado--activo">Activo</span>'
          : '<span class="badge-estado badge-estado--inactivo">Inactivo</span>');

      return `
        <tr>
          <td>${escaparHtml(u.nombre)}</td>
          <td>${escaparHtml(u.correo)}</td>
          <td>${escaparHtml(u.documento || '—')}</td>
          <td><span class="badge-rol">${escaparHtml(u.rol)}</span></td>
          <td>${estadoBadge}</td>
          <td>${fechaHora(u.creadoEn || u.ultimoAcceso)}</td>
          <td>
            <div class="panel-acciones">
              <button type="button" class="panel-accion-btn" title="Editar" onclick="editarUsuario(${u.idUsuario})">
                <i class="fa-solid fa-pen"></i>
              </button>
              <button type="button" class="panel-accion-btn" title="Inspección soporte auditada"
                      onclick="inspeccionarCuentaSoporte(${u.idUsuario})">
                <i class="fa-solid fa-key"></i>
              </button>
              ${u.activo ? `
              <button type="button" class="panel-accion-btn panel-accion-btn--peligro" title="Desactivar"
                      onclick="desactivarUsuario(${u.idUsuario}, '${escaparHtml(u.nombre)}')">
                <i class="fa-solid fa-user-slash"></i>
              </button>` : `
              <button type="button" class="panel-accion-btn panel-accion-btn--exito" title="Reactivar cuenta"
                      onclick="reactivarUsuario(${u.idUsuario}, '${escaparHtml(u.nombre)}')">
                <i class="fa-solid fa-user-check"></i>
              </button>`}
              <button type="button" class="panel-accion-btn panel-accion-btn--eliminar" title="Eliminar definitivamente"
                      onclick="eliminarUsuarioDefinitivo(${u.idUsuario}, '${escaparHtml(u.nombre)}', '${escaparHtml(u.correo)}')">
                <i class="fa-solid fa-trash"></i>
              </button>
            </div>
          </td>
        </tr>`;
    }).join('');
  } catch (error) {
    tablaVacia(tbody, 7, error.message);
  }
}

// Permite al administrador revisar el resumen técnico de una cuenta dejando constancia en el historial de auditoría
async function inspeccionarCuentaSoporte(idUsuario) {
  const ok = await confirmarAccion(
    'La inspección soporte queda registrada en auditoría con tu usuario Administrador. ¿Deseas continuar?',
    { titulo: 'Acceso soporte auditado', etiquetaOk: 'Auditar e inspeccionar' }
  );
  if (!ok) return;
  try {
    const info = await Api.accesoSoporteUsuario(idUsuario);
    const resumen = [
      `Usuario: ${info.nombre} (${info.correo})`,
      `Rol: ${info.rol} | Estado: ${info.activo ? 'Activo' : 'Inactivo'}${info.bloqueado ? ' / Bloqueado' : ''}`,
      info.areaEmpleado ? `Área: ${info.areaEmpleado} | Módulos: ${(info.modulosEmpleado || []).join(', ')}` : null,
      info.totalPedidosCliente != null ? `Pedidos cliente: ${info.totalPedidosCliente}` : null,
      `Tareas asignadas: ${info.tareasAsignadas ?? 0}`,
      `Auditado por: ${info.auditadoPor}`
    ].filter(Boolean).join(' · ');
    mostrarToast(resumen, 'info', 6500);
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

// Carga los datos actuales de un usuario en la ventana emergente para modificarlos
async function editarUsuario(id) {
  const form = document.getElementById('formUsuario');
  if (!form) return;
  try {
    const u = await Api.usuario(id);
    form.idUsuario.value = u.idUsuario;
    form.nombre.value = u.nombre || '';
    form.correo.value = u.correo || '';
    form.documento.value = u.documento || '';
    form.telefono.value = u.telefono || '';
    form.rol.value = u.rol || 'cliente';
    form.password.value = '';
    form.password.required = false;
    form.activo.checked = Boolean(u.activo);
    form.bloqueado.checked = Boolean(u.bloqueado);
    const lblPass = document.getElementById('etiquetaPasswordUsuario');
    if (lblPass) lblPass.textContent = 'Nueva contraseña (opcional)';
    const resp = document.getElementById('respuestaUsuario');
    if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
    const titulo = document.getElementById('tituloModalUsuario');
    if (titulo) titulo.innerHTML = `<i class="fa-solid fa-user-pen"></i> Editar usuario #${u.idUsuario}`;
    abrirModal('modalUsuario');
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

// Desactiva temporalmente una cuenta para impedir su ingreso sin borrar su historial
async function desactivarUsuario(id, nombre) {
  const confirmado = await confirmarAccion('Desactivar usuario', `¿Desactivar la cuenta de "${nombre}"?`);
  if (!confirmado) return;
  try {
    await Api.eliminarUsuario(id);
    mostrarToast(`Cuenta "${nombre}" desactivada.`, 'info');
    await cargarUsuarios();
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

// Vuelve a habilitar una cuenta previamente desactivada
async function reactivarUsuario(id, nombre) {
  const confirmado = await confirmarAccion('Reactivar cuenta', `¿Volver a activar la cuenta de "${nombre}"?`);
  if (!confirmado) return;
  try {
    await Api.cambiarEstadoUsuario(id, true);
    mostrarToast(`Cuenta "${nombre}" reactivada correctamente.`, 'ok');
    await cargarUsuarios();
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

/**
 * Eliminacion DEFINITIVA. Pide dos confirmaciones:
 *  1) el aviso normal, y
 *  2) la clave del administrador (ventana propia).
 */
async function eliminarUsuarioDefinitivo(id, nombre, correo) {
  const aviso = await confirmarAccion(
    'Eliminar usuario definitivamente',
    `Esta accion NO se puede deshacer.\n\nSe eliminara la cuenta de "${nombre}" (${correo || 'sin correo'}) ` +
    'junto con sus pedidos, favoritos, notificaciones y tareas asignadas.\n\n' +
    'Si solo quieres bloquearle el acceso, usa "Desactivar".'
  );
  if (!aviso) return;

  const eliminado = await pedirClaveYeliminar(id, nombre);
  if (eliminado) {
    mostrarToast(`Usuario "${nombre}" eliminado definitivamente.`, 'ok');
    await cargarUsuarios();
  }
}

/**
 * Ventana que solicita la clave del administrador y elimina el usuario.
 * Si la clave es incorrecta NO se cierra: muestra el error para reintentar.
 * Devuelve true solo si la eliminacion se completo.
 */
function pedirClaveYeliminar(idUsuario, nombre) {
  return new Promise((resolve) => {
    let modal = document.getElementById('modalClaveAdmin');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'modalClaveAdmin';
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-caja" style="max-width:420px;">
          <h3><i class="fa-solid fa-triangle-exclamation" style="color:#c83810;"></i> Confirmar eliminacion</h3>
          <p id="claveAdminMensaje" style="color:var(--texto-tenue); margin:12px 0 16px;"></p>
          <label for="claveAdminInput" style="display:block; font-weight:600; margin-bottom:6px; font-size:.9rem;">
            Escribe tu clave de administrador para continuar
          </label>
          <input type="password" id="claveAdminInput" autocomplete="current-password"
                 style="width:100%; padding:10px 12px; border:1px solid var(--azul-acero); border-radius:8px;"
                 placeholder="Tu clave actual">
          <p id="claveAdminError" style="color:#c83810; font-size:.85rem; margin:8px 0 0; min-height:18px;"></p>
          <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:16px;">
            <button type="button" class="btn btn--secundario" id="claveAdminNo">Cancelar</button>
            <button type="button" class="btn btn--peligro" id="claveAdminSi"
                    style="background:#c83810; color:#fff; border-color:#c83810;">
              <i class="fa-solid fa-trash"></i> Eliminar definitivamente
            </button>
          </div>
        </div>`;
      document.body.appendChild(modal);
    }

    const input = document.getElementById('claveAdminInput');
    const error = document.getElementById('claveAdminError');
    const btnSi = document.getElementById('claveAdminSi');
    const btnNo = document.getElementById('claveAdminNo');
    document.getElementById('claveAdminMensaje').textContent =
      `Vas a eliminar la cuenta de "${nombre}". Esta accion no se puede deshacer.`;
    input.value = '';
    error.textContent = '';
    btnSi.disabled = false;
    btnSi.innerHTML = '<i class="fa-solid fa-trash"></i> Eliminar definitivamente';
    modal.classList.add('activo');
    setTimeout(() => input.focus(), 50);

    const cerrar = (resultado) => {
      modal.classList.remove('activo');
      btnSi.onclick = null;
      btnNo.onclick = null;
      input.onkeydown = null;
      resolve(resultado);
    };
    const aceptar = async () => {
      const clave = input.value.trim();
      if (!clave) { error.textContent = 'Debes escribir tu clave para continuar.'; input.focus(); return; }
      btnSi.disabled = true;
      error.textContent = 'Verificando...';
      try {
        await Api.eliminarUsuarioDefinitivo(idUsuario, clave);
        cerrar(true);
      } catch (e) {
        error.textContent = e.message || 'No se pudo eliminar el usuario.';
        input.value = '';
        input.focus();
        btnSi.disabled = false;
      }
    };
    btnSi.onclick = aceptar;
    btnNo.onclick = () => cerrar(false);
    input.onkeydown = (e) => {
      if (e.key === 'Enter') { e.preventDefault(); aceptar(); }
      if (e.key === 'Escape') { e.preventDefault(); cerrar(false); }
    };
  });
}

window.editarUsuario = editarUsuario;
window.desactivarUsuario = desactivarUsuario;
window.reactivarUsuario = reactivarUsuario;
window.eliminarUsuarioDefinitivo = eliminarUsuarioDefinitivo;

/* ------------------- Marca nueva desde el producto ------------------- */

/** Pide el nombre de una marca nueva. Devuelve null si cancela. */
function pedirMarcaNueva() {
  return new Promise((resolve) => {
    let modal = document.getElementById('modalNuevaMarca');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'modalNuevaMarca';
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-caja" style="max-width:400px;">
          <h3><i class="fa-solid fa-tags"></i> Nueva marca</h3>
          <p style="color:var(--texto-tenue); margin:10px 0 16px; font-size:.9rem;">
            Escribe el nombre de la marca. Quedara disponible para todos los productos.
          </p>
          <label for="nuevaMarcaInput" style="display:block; font-weight:600; margin-bottom:6px; font-size:.9rem;">Nombre de la marca</label>
          <input type="text" id="nuevaMarcaInput" maxlength="80" autocomplete="off"
                 style="width:100%; padding:10px 12px; border:1px solid var(--azul-acero); border-radius:8px;"
                 placeholder="Ej: Hikvision">
          <p id="nuevaMarcaError" style="color:#c83810; font-size:.85rem; margin:8px 0 0; min-height:18px;"></p>
          <div style="display:flex; justify-content:flex-end; gap:10px; margin-top:16px;">
            <button type="button" class="btn btn--secundario" id="nuevaMarcaNo">Cancelar</button>
            <button type="button" class="btn btn--primario" id="nuevaMarcaSi">
              <i class="fa-solid fa-check"></i> Guardar marca
            </button>
          </div>
        </div>`;
      document.body.appendChild(modal);
    }

    const input = document.getElementById('nuevaMarcaInput');
    const error = document.getElementById('nuevaMarcaError');
    const btnSi = document.getElementById('nuevaMarcaSi');
    const btnNo = document.getElementById('nuevaMarcaNo');
    input.value = '';
    error.textContent = '';
    btnSi.disabled = false;
    modal.classList.add('activo');
    setTimeout(() => input.focus(), 50);

    const cerrar = (valor) => {
      modal.classList.remove('activo');
      btnSi.onclick = null;
      btnNo.onclick = null;
      input.onkeydown = null;
      resolve(valor);
    };
    const aceptar = () => {
      const nombre = input.value.trim();
      if (!nombre) { error.textContent = 'Escribe el nombre de la marca.'; input.focus(); return; }
      cerrar(nombre);
    };
    btnSi.onclick = aceptar;
    btnNo.onclick = () => cerrar(null);
    input.onkeydown = (e) => {
      if (e.key === 'Enter') { e.preventDefault(); aceptar(); }
      if (e.key === 'Escape') { e.preventDefault(); cerrar(null); }
    };
  });
}

/* ----------------------------- Productos ------------------------------- */

// Configura el catálogo interno del panel: filtros por categoría, búsqueda, subida de foto y formulario de producto
async function inicializarProductos() {
  const buscar = document.getElementById('buscarProducto');
  const filtroCat = document.getElementById('filtroCategoriaProducto');
  const btnNuevo = document.getElementById('btnNuevoProducto');
  const form = document.getElementById('formProducto');

  // Rellena los menús desplegables de categorías y marcas desde el servidor
  try {
    const [cats, marcas] = await Promise.all([Api.categorias(), Api.marcas()]);
    const optsCat = cats.map((c) => `<option value="${c.idCategoria}">${escaparHtml(c.nombre)}</option>`).join('');
    if (filtroCat) filtroCat.innerHTML = `<option value="">Todas las categorías</option>${optsCat}`;
    const selCat = document.getElementById('prodCategoria');
    if (selCat) selCat.innerHTML = `<option value="">Sin categoría</option>${optsCat}`;

    const selMarca = document.getElementById('prodMarca');
    if (selMarca) {
      selMarca.innerHTML = `<option value="">Sin marca</option>` +
        marcas.map((m) => `<option value="${m.idMarca || ''}">${escaparHtml(m.nombre)}</option>`).join('');
    }
  } catch (error) {
    console.warn('Catálogos:', error.message);
  }

  // Registrar una marca nueva sin salir del formulario del producto
  const btnNuevaMarca = document.getElementById('btnNuevaMarca');
  if (btnNuevaMarca) {
    btnNuevaMarca.addEventListener('click', async () => {
      const nombre = await pedirMarcaNueva();
      if (!nombre) return;
      try {
        const creada = await Api.crearMarca({ nombre });
        const selMarca = document.getElementById('prodMarca');
        if (selMarca && creada && creada.idMarca) {
          selMarca.insertAdjacentHTML('beforeend',
            `<option value="${creada.idMarca}">${escaparHtml(creada.nombre || nombre)}</option>`);
          selMarca.value = String(creada.idMarca);
        }
        mostrarToast(`Marca "${creada.nombre || nombre}" creada.`, 'ok');
      } catch (error) {
        mostrarToast(error.message || 'No se pudo crear la marca.', 'error');
      }
    });
  }

  // Filtra los productos mientras el usuario escribe en el buscador o cambia la categoría
  let timer;
  if (buscar) {
    buscar.addEventListener('input', () => {
      clearTimeout(timer);
      timer = setTimeout(cargarProductos, 250);
    });
  }
  if (filtroCat) filtroCat.addEventListener('change', cargarProductos);

  // Botón "Nuevo producto": abre la ventana emergente con valores iniciales recomendados
  if (btnNuevo && form) {
    btnNuevo.addEventListener('click', () => {
      form.reset();
      form.idProducto.value = '';
      form.activo.checked = true;
      form.destacado.checked = false;
      form.descuentoPct.value = 0;
      form.stockMinimo.value = 5;
      form.garantiaMeses.value = 12;
      const resp = document.getElementById('respuestaProducto');
      if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
      const titulo = document.getElementById('tituloModalProducto');
      if (titulo) titulo.innerHTML = '<i class="fa-solid fa-plus"></i> Nuevo producto';
      abrirModal('modalProducto');
    });
  }

  // Cuando el usuario selecciona una foto desde su equipo, la sube al servidor y rellena la ruta automáticamente
  const archivoImg = document.getElementById('archivoImagenProducto');
  if (archivoImg && form) {
    archivoImg.addEventListener('change', async () => {
      const archivo = archivoImg.files && archivoImg.files[0];
      if (!archivo) return;
      try {
        const res = await Api.subirImagen(archivo, 'productos');
        if (form.imagen) form.imagen.value = res.ruta || res.url;
        mostrarToast('Imagen subida correctamente al catálogo.', 'ok');
      } catch (error) {
        mostrarToast(error.message || 'No se pudo subir la imagen.', 'error');
      }
    });
  }

  // Envía el producto nuevo o editado al servidor y actualiza la tabla visible
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const resp = document.getElementById('respuestaProducto');
      resp.textContent = '';
      resp.className = 'form-respuesta campo--full';

      const id = form.idProducto.value;
      const datos = {
        nombre: form.nombre.value.trim(),
        sku: form.sku.value.trim() || null,
        codigoBarras: null,
        descripcion: form.descripcion.value.trim() || null,
        precio: Number(form.precio.value || 0),
        descuentoPct: Number(form.descuentoPct.value || 0),
        impuestoPct: 19,
        garantiaMeses: Number(form.garantiaMeses.value || 12),
        stock: Number(form.stock.value || 0),
        stockMinimo: Number(form.stockMinimo.value || 5),
        stockMaximo: 100,
        imagen: form.imagen.value.trim() || null,
        destacado: form.destacado.checked,
        activo: form.activo.checked,
        idCategoria: form.idCategoria.value ? Number(form.idCategoria.value) : null,
        idMarca: form.idMarca.value ? Number(form.idMarca.value) : null
      };

      try {
        if (id) {
          await Api.actualizarProducto(id, datos);
        } else {
          await Api.crearProducto(datos);
        }
        cerrarModal('modalProducto');
        await cargarProductos();
      } catch (error) {
        resp.textContent = error.message;
        resp.className = 'form-respuesta error campo--full';
      }
    });
  }

  await cargarProductos();
}

// Descarga la lista de productos según búsqueda y categoría y dibuja la tabla del panel
async function cargarProductos() {
  const tbody = document.getElementById('tablaProductos');
  if (!tbody) return;

  const texto = (document.getElementById('buscarProducto')?.value || '').trim();
  const idCategoria = document.getElementById('filtroCategoriaProducto')?.value || '';

  try {
    const datos = await Api.productos({ texto, idCategoria, tamanio: 100 });
    if (!datos.contenido.length) {
      tablaVacia(tbody, 8, 'No se encontraron productos.');
      return;
    }

    tbody.innerHTML = datos.contenido.map((p) => `
      <tr>
        <td>${escaparHtml(p.sku || '—')}</td>
        <td>${escaparHtml(p.nombre)}</td>
        <td>${escaparHtml(p.categoria || '—')}</td>
        <td>${escaparHtml(p.marca || '—')}</td>
        <td>${p.stock}</td>
        <td>${textoImporte(p.precioFinal)}</td>
        <td>${p.stockBajo
          ? '<span class="badge-estado badge-estado--bloqueado">Stock bajo</span>'
          : '<span class="badge-estado badge-estado--activo">Disponible</span>'}</td>
        <td>
          <div class="panel-acciones">
            <button type="button" class="panel-accion-btn" title="Editar" onclick="editarProducto(${p.idProducto})">
              <i class="fa-solid fa-pen"></i>
            </button>
            <button type="button" class="panel-accion-btn panel-accion-btn--peligro" title="Desactivar"
                    onclick="eliminarProducto(${p.idProducto}, '${escaparHtml(p.nombre)}')">
              <i class="fa-solid fa-trash"></i>
            </button>
          </div>
        </td>
      </tr>`).join('');
  } catch (error) {
    tablaVacia(tbody, 8, error.message);
  }
}

// Carga los datos de un producto existente en la ventana emergente para editar precio, stock, foto o descripción
async function editarProducto(id) {
  const form = document.getElementById('formProducto');
  if (!form) return;
  try {
    const p = await Api.producto(id);
    form.idProducto.value = p.idProducto;
    form.nombre.value = p.nombre || '';
    form.sku.value = p.sku || '';
    form.idCategoria.value = p.idCategoria || '';
    form.idMarca.value = p.idMarca || '';
    form.precio.value = p.precio ?? 0;
    form.descuentoPct.value = p.descuentoPct ?? 0;
    form.stock.value = p.stock ?? 0;
    form.stockMinimo.value = p.stockMinimo ?? 5;
    form.garantiaMeses.value = p.garantiaMeses ?? 12;
    form.imagen.value = p.imagen || '';
    form.descripcion.value = p.descripcion || '';
    form.destacado.checked = Boolean(p.destacado);
    form.activo.checked = Boolean(p.activo);
    const resp = document.getElementById('respuestaProducto');
    if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
    const titulo = document.getElementById('tituloModalProducto');
    if (titulo) titulo.innerHTML = `<i class="fa-solid fa-pen"></i> Editar producto #${p.idProducto}`;
    abrirModal('modalProducto');
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

// Oculta/desactiva un producto del catálogo tras pedir confirmación
async function eliminarProducto(id, nombre) {
  const confirmado = await confirmarAccion('Desactivar producto', `¿Desactivar el producto "${nombre}" del catálogo?`);
  if (!confirmado) return;
  try {
    await Api.eliminarProducto(id);
    mostrarToast(`Producto "${nombre}" desactivado.`, 'info');
    await cargarProductos();
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

window.editarProducto = editarProducto;
window.eliminarProducto = eliminarProducto;

/* ------------------------------ Pedidos -------------------------------- */

let cachePedidosPanel = [];

// Dibuja la tabla de pedidos y, si corresponde, agrega los botones para ver el mapa/ruta y avanzar de estado
function pintarPedidos(idTabla, pedidos, conAcciones) {
  const tbody = document.getElementById(idTabla);
  if (!tbody) return;
  if (Array.isArray(pedidos)) cachePedidosPanel = pedidos;

  if (!pedidos || !pedidos.length) {
    tablaVacia(tbody, conAcciones ? 6 : 5, 'Todavía no hay pedidos registrados.');
    return;
  }

  tbody.innerHTML = pedidos.map((p) => {
    const siguiente = SIGUIENTE_ESTADO[p.estado];
    const accion = conAcciones
      ? `<div class="panel-acciones">
          <button type="button" class="panel-accion-btn" title="Ver ruta, dirección y pago" onclick="verRutaPedido(${p.idPedido})">
            <i class="fa-solid fa-route"></i>
          </button>
          ${siguiente
            ? `<button type="button" class="btn btn--primario" style="padding:5px 10px; font-size:.78rem;"
                       onclick="avanzarPedido(${p.idPedido}, '${siguiente}')">
                 ${estadoLegible(siguiente)}</button>`
            : ''}
        </div>`
      : '';

    return `
      <tr>
        <td>#${p.idPedido}</td>
        <td>${escaparHtml(p.clienteNombre || '—')}</td>
        <td>${textoImporte(p.total)}</td>
        <td><span class="badge-rol">${estadoLegible(p.estado)}</span></td>
        <td>${fechaHora(p.creadoEn)}</td>
        ${conAcciones ? `<td>${accion}</td>` : ''}
      </tr>`;
  }).join('');
}

// Conecta el selector de estado (Pendiente, Enviado, Entregado...) y carga la tabla de pedidos
function inicializarPedidos() {
  const filtro = document.getElementById('filtroEstadoPedido');
  if (filtro) filtro.addEventListener('change', cargarPedidos);
  cargarPedidos();
}

// Trae los pedidos filtrados por estado desde el servidor
async function cargarPedidos() {
  const tbody = document.getElementById('tablaPedidos');
  if (!tbody) return;
  const estado = document.getElementById('filtroEstadoPedido')?.value || '';
  try {
    const datos = await Api.pedidos({ estado, tamanio: 50 });
    pintarPedidos('tablaPedidos', datos.contenido, true);
  } catch (error) {
    tablaVacia(tbody, 6, error.message);
  }
}

// Abre la ventana de seguimiento de un pedido mostrando dirección, enlace a Google Maps, pago y línea de tiempo
async function verRutaPedido(idPedido) {
  const cont = document.getElementById('contenidoRutaPedido');
  const titulo = document.getElementById('tituloModalRuta');
  if (!cont) return;
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-route"></i> Seguimiento del pedido #${idPedido}`;
  cont.innerHTML = '<p style="color:var(--texto-tenue);">Cargando historial…</p>';
  abrirModal('modalRutaPedido');
  const pedido = cachePedidosPanel.find((p) => Number(p.idPedido) === Number(idPedido));
  let resumenEntrega = '';
  if (pedido && (pedido.direccionEntrega || pedido.metodoPago || (pedido.latitud && pedido.longitud))) {
    const qMapa = (pedido.latitud != null && pedido.longitud != null)
      ? `${pedido.latitud},${pedido.longitud}`
      : (pedido.direccionEntrega || '');
    const urlMaps = qMapa
      ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(qMapa)}`
      : '';
    resumenEntrega = `
      <div style="background:var(--azul-oscuro); border:1px solid var(--azul-acero); border-radius:8px; padding:10px 12px; margin-bottom:12px; font-size:.86rem;">
        <div><strong>Dirección:</strong> ${escaparHtml(pedido.direccionEntrega || 'No registrada')}
          ${urlMaps ? ` <a href="${urlMaps}" target="_blank" rel="noopener" style="color:var(--cian); text-decoration:underline; margin-left:6px;"><i class="fa-solid fa-map-location-dot"></i> Ver en Google Maps</a>` : ''}
        </div>
        <div style="margin-top:4px;"><strong>Pago:</strong> ${escaparHtml(pedido.metodoPago || 'No especificado')}${pedido.detallePago ? ` — <small style="color:var(--texto-tenue);">${escaparHtml(pedido.detallePago)}</small>` : ''}</div>
      </div>`;
  }
  try {
    const lista = await Api.historialPedido(idPedido);
    if (!lista || !lista.length) {
      cont.innerHTML = resumenEntrega + '<p style="color:var(--texto-tenue);">Sin movimientos registrados aún.</p>';
      return;
    }
    cont.innerHTML = resumenEntrega + lista.map((h) => `
      <div class="panel-timeline__item">
        <strong>${escaparHtml(estadoLegible(h.estadoNuevo))}</strong>
        ${h.estadoAnterior ? `<span style="color:var(--texto-tenue); font-size:.82rem;"> (desde ${escaparHtml(estadoLegible(h.estadoAnterior))})</span>` : ''}
        <div class="panel-timeline__fecha">${fechaHora(h.creadoEn)}</div>
      </div>`).join('');
  } catch (error) {
    cont.innerHTML = resumenEntrega + `<p style="color:var(--naranja);">${escaparHtml(error.message)}</p>`;
  }
}

// Cambia un pedido a su siguiente etapa (por ejemplo, de Confirmado a En preparación) y refresca la pantalla
async function avanzarPedido(idPedido, nuevoEstado) {
  try {
    await Api.cambiarEstadoPedido(idPedido, nuevoEstado);
    mostrarToast(`Pedido #${idPedido} actualizado a "${estadoLegible(nuevoEstado)}".`, 'ok');
    if (document.getElementById('tablaPedidos')) await cargarPedidos();
    if (document.getElementById('tablaUltimosPedidos') && document.body.dataset.rol === 'administrador') {
      await cargarDashboardAdmin();
    }
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

window.verRutaPedido = verRutaPedido;
window.avanzarPedido = avanzarPedido;

/* ----------------------------- Inventario ------------------------------ */

let inventarioCache = [];

// Activa el buscador, el filtro de stock bajo y el formulario para registrar entradas, salidas o ajustes
function inicializarInventario() {
  const buscar = document.getElementById('buscarInventario');
  const filtro = document.getElementById('filtroInventarioEstado');
  const form = document.getElementById('formMovimientoInv');

  if (buscar) buscar.addEventListener('input', pintarTablaInventario);
  if (filtro) filtro.addEventListener('change', pintarTablaInventario);

  // Al enviar un movimiento (entrada, salida o ajuste), actualiza el inventario en el servidor
  if (form) {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const resp = document.getElementById('respuestaMovimientoInv');
      if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
      const idProducto = Number(form.idProducto.value);
      try {
        await Api.registrarMovimiento(idProducto, {
          tipo: form.tipo.value,
          cantidad: Math.max(0, Math.trunc(Number(form.cantidad.value))),
          motivo: form.motivo.value.trim()
        });
        mostrarToast('Movimiento aplicado al inventario.', 'ok');
        cerrarModal('modalMovimientoInv');
        await cargarInventario();
      } catch (error) {
        if (resp) { resp.textContent = error.message; resp.className = 'form-respuesta error campo--full'; }
      }
    });
  }

  cargarInventario();
}

// Filtra y dibuja la tabla general de existencias con acceso a ajustar stock y consultar el historial de entradas y salidas de bodega
function pintarTablaInventario() {
  const tbodyTodo = document.getElementById('tablaInventario');
  if (!tbodyTodo) return;

  const texto = (document.getElementById('buscarInventario')?.value || '').trim().toLowerCase();
  const estado = document.getElementById('filtroInventarioEstado')?.value || '';

  const filtrados = inventarioCache.filter((p) => {
    const coincideTexto = !texto
      || (p.nombre || '').toLowerCase().includes(texto)
      || (p.sku || '').toLowerCase().includes(texto);
    const esBajo = Number(p.stock) <= Number(p.stockMinimo || 5);
    const coincideEstado = !estado || (estado === 'bajo' ? esBajo : !esBajo);
    return coincideTexto && coincideEstado;
  });

  if (!filtrados.length) {
    tablaVacia(tbodyTodo, 5, 'No se encontraron productos con esos filtros.');
    return;
  }

  tbodyTodo.innerHTML = filtrados.map((p) => `
    <tr>
      <td>${escaparHtml(p.nombre)}</td>
      <td>${escaparHtml(p.sku || '—')}</td>
      <td><strong>${p.stock}</strong></td>
      <td>${p.stockMinimo ?? 5}</td>
      <td>
        <div class="panel-acciones">
          <button type="button" class="panel-accion-btn" title="Registrar entrada/salida/ajuste"
                  onclick="abrirMovimientoInv(${p.idProducto}, '${escaparHtml(p.nombre)}', ${p.stock})">
            <i class="fa-solid fa-sliders"></i>
          </button>
          <button type="button" class="panel-accion-btn" title="Ver Kardex"
                  onclick="verKardex(${p.idProducto}, '${escaparHtml(p.nombre)}')">
            <i class="fa-solid fa-clock-rotate-left"></i>
          </button>
        </div>
      </td>
    </tr>`).join('');
}

// Carga tanto la tabla de alertas por stock bajo como el inventario completo desde el servidor
async function cargarInventario() {
  const tbodyBajo = document.getElementById('tablaStockBajo');
  try {
    const [bajos, todos] = await Promise.all([Api.stockBajo(), Api.inventario()]);
    if (tbodyBajo) {
      tbodyBajo.innerHTML = bajos.length
        ? bajos.map((p) => `<tr>
            <td>${escaparHtml(p.nombre)}</td>
            <td>${escaparHtml(p.sku || '—')}</td>
            <td><strong style="color:#ffb199">${p.stock}</strong></td>
            <td>${p.stockMinimo}</td>
            <td>${p.stockMaximo}</td>
          </tr>`).join('')
        : '<tr><td colspan="5" style="text-align:center; color:var(--texto-tenue);">Todo el inventario está por encima del mínimo.</td></tr>';
    }
    inventarioCache = todos || [];
    pintarTablaInventario();
  } catch (error) {
    if (tbodyBajo) tablaVacia(tbodyBajo, 5, error.message);
  }
}

// Abre la ventana emergente para sumar o descontar unidades de un producto en bodega
function abrirMovimientoInv(idProducto, nombre, stockActual) {
  const form = document.getElementById('formMovimientoInv');
  if (!form) return;
  form.reset();
  form.idProducto.value = idProducto;
  const resp = document.getElementById('respuestaMovimientoInv');
  if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
  const titulo = document.getElementById('tituloModalMovimiento');
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-boxes-stacked"></i> ${escaparHtml(nombre)} (Stock: ${stockActual})`;
  abrirModal('modalMovimientoInv');
}

// Muestra el historial detallado (historial de entradas y salidas de bodega) de todas las entradas y salidas de un producto
async function verKardex(idProducto, nombre) {
  const tbody = document.getElementById('tablaKardexDetalle');
  const titulo = document.getElementById('tituloModalKardex');
  if (!tbody) return;
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-clock-rotate-left"></i> Kardex: ${escaparHtml(nombre)}`;
  tbody.innerHTML = '<tr><td colspan="5">Cargando movimientos…</td></tr>';
  abrirModal('modalKardex');
  try {
    const movs = await Api.kardex(idProducto);
    if (!movs || !movs.length) {
      tablaVacia(tbody, 5, 'Este producto aún no tiene movimientos en el Kardex.');
      return;
    }
    tbody.innerHTML = movs.map((m) => `
      <tr>
        <td>${fechaHora(m.creadoEn)}</td>
        <td><span class="badge-rol">${escaparHtml(m.tipo)}</span></td>
        <td><strong>${m.cantidad}</strong></td>
        <td>${m.stockResultante}</td>
        <td>${escaparHtml(m.motivo || '—')}</td>
      </tr>`).join('');
  } catch (error) {
    tablaVacia(tbody, 5, error.message);
  }
}

window.abrirMovimientoInv = abrirMovimientoInv;
window.verKardex = verKardex;

/* ------------------------------- Reportes ------------------------------ */

// Carga los indicadores de ventas, la gráfica de barras y el ranking de productos más vendidos
async function cargarReportes() {
  try {
    const [resumen, vendidos] = await Promise.all([
      Api.resumenReporte(),
      Api.productosMasVendidos()
    ]);

    const tablaResumen = document.getElementById('tablaResumen');
    if (tablaResumen) {
      tablaResumen.innerHTML = [
        ['Pedidos del período', resumen.cantidadPedidos ?? 0],
        ['Ingresos del período', textoImporte(resumen.ingresosPeriodo)],
        ['Ingresos históricos', textoImporte(resumen.ingresosHistoricos)],
        ['Productos activos', resumen.productosActivos ?? 0],
        ['Productos con stock bajo', resumen.productosStockBajo ?? 0],
        ['Usuarios activos', resumen.usuariosActivos ?? 0]
      ].map(([k, v]) => `<tr><td>${k}</td><td><strong>${v}</strong></td></tr>`).join('');
    }

    renderizarGraficaBarras('graficaMasVendidos', vendidos);

    const tablaVendidos = document.getElementById('tablaMasVendidos');
    if (tablaVendidos) {
      tablaVendidos.innerHTML = vendidos.length
        ? vendidos.map((p, i) => `<tr>
            <td>${i + 1}</td>
            <td>${escaparHtml(p.nombre)}</td>
            <td>${p.unidades}</td>
            <td>${textoImporte(p.ingresos)}</td>
          </tr>`).join('')
        : '<tr><td colspan="4" style="text-align:center; color:var(--texto-tenue);">Aún no hay ventas registradas.</td></tr>';
    }
  } catch (error) {
    console.warn('Reportes:', error.message);
  }
}

// Descarga el archivo de reporte de ventas en formato Excel/archivo para Excel o XML
function exportarVentas(formato) {
  const url = Api.urlExportarVentas(undefined, undefined, formato);
  Api.descargar(url, formato === 'xml' ? 'ventas.xml' : 'ventas.csv')
    .then(() => mostrarToast(`Reporte ${formato.toUpperCase()} descargado.`, 'ok'))
    .catch((error) => mostrarToast(error.message, 'error'));
}

/* ------------------------------- Soporte ------------------------------- */

let soporteSolicitudesCache = [];
let soporteContactosCache = [];

// Configura los formularios de soporte para asignar técnicos a citas/cotizaciones y responder mensajes de contacto
function inicializarSoporte() {
  const formSol = document.getElementById('formGestionSolicitud');
  const formCon = document.getElementById('formResponderContacto');

  // Guarda el técnico asignado, estado y observaciones de una solicitud de servicio
  if (formSol) {
    formSol.addEventListener('submit', async (e) => {
      e.preventDefault();
      const resp = document.getElementById('respuestaSolicitudModal');
      if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
      try {
        await Api.gestionarSolicitud(Number(formSol.idSolicitud.value), {
          tecnicoAsignado: formSol.tecnicoAsignado.value.trim() || null,
          estado: formSol.estado.value,
          observaciones: formSol.observaciones.value.trim() || null
        });
        mostrarToast('Solicitud actualizada correctamente.', 'ok');
        cerrarModal('modalGestionSolicitud');
        await cargarSoporte();
      } catch (err) {
        if (resp) { resp.textContent = err.message; resp.className = 'form-respuesta error campo--full'; }
      }
    });
  }

  // Envía y registra la respuesta escrita para un mensaje recibido desde el formulario de contacto
  if (formCon) {
    formCon.addEventListener('submit', async (e) => {
      e.preventDefault();
      const resp = document.getElementById('respuestaContactoModal');
      if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
      try {
        await Api.responderContacto(Number(formCon.idContacto.value), formCon.respuesta.value.trim());
        mostrarToast('Respuesta enviada y registrada.', 'ok');
        cerrarModal('modalResponderContacto');
        await cargarSoporte();
      } catch (err) {
        if (resp) { resp.textContent = err.message; resp.className = 'form-respuesta error campo--full'; }
      }
    });
  }

  cargarSoporte();
}

// Abre la ventana emergente con los datos de una solicitud para asignarle técnico o cambiar su estado
function gestionarSolicitudUI(id) {
  const s = soporteSolicitudesCache.find((x) => x.idSolicitud === id);
  const form = document.getElementById('formGestionSolicitud');
  if (!s || !form) return;
  form.idSolicitud.value = s.idSolicitud;
  form.tecnicoAsignado.value = s.tecnicoAsignado || '';
  form.estado.value = s.estado || 'pendiente';
  form.observaciones.value = s.observaciones || '';
  const resp = document.getElementById('respuestaSolicitudModal');
  if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
  const titulo = document.getElementById('tituloModalSolicitud');
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-wrench"></i> Solicitud #${s.idSolicitud} (${escaparHtml(s.nombre)})`;
  abrirModal('modalGestionSolicitud');
}

// Abre la ventana para leer un mensaje de contacto recibido y redactar su respuesta
function responderContactoUI(id) {
  const c = soporteContactosCache.find((x) => x.idContacto === id);
  const form = document.getElementById('formResponderContacto');
  if (!c || !form) return;
  form.idContacto.value = c.idContacto;
  form.respuesta.value = '';
  const orig = document.getElementById('textoContactoOriginal');
  if (orig) orig.textContent = `${c.asunto ? c.asunto + ': ' : ''}${c.mensaje || ''}`;
  const resp = document.getElementById('respuestaContactoModal');
  if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
  const titulo = document.getElementById('tituloModalContacto');
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-reply"></i> Responder a ${escaparHtml(c.nombre)}`;
  abrirModal('modalResponderContacto');
}

window.gestionarSolicitudUI = gestionarSolicitudUI;
window.responderContactoUI = responderContactoUI;

// Descarga y pinta las tablas de solicitudes técnicas y mensajes de contacto pendientes
async function cargarSoporte() {
  try {
    const solicitudes = await Api.solicitudes({ tamanio: 50 });
    soporteSolicitudesCache = solicitudes.contenido || [];
    const tbody = document.getElementById('tablaSolicitudes');
    if (tbody) {
      tbody.innerHTML = soporteSolicitudesCache.length
        ? soporteSolicitudesCache.map((s) => `<tr>
            <td>#${s.idSolicitud}</td>
            <td>${escaparHtml(s.servicio || '—')}</td>
            <td>${escaparHtml(s.nombre)}</td>
            <td>${s.tipo === 'cita' ? 'Cita' : 'Cotización'}</td>
            <td><span class="badge-rol">${estadoLegible(s.estado)}</span></td>
            <td>${escaparHtml(s.tecnicoAsignado || 'Sin asignar')}</td>
            <td>
              <button type="button" class="panel-accion-btn" title="Asignar técnico y estado"
                      onclick="gestionarSolicitudUI(${s.idSolicitud})">
                <i class="fa-solid fa-user-gear"></i>
              </button>
            </td>
          </tr>`).join('')
        : '<tr><td colspan="7" style="text-align:center; color:var(--texto-tenue);">No hay solicitudes.</td></tr>';
    }
  } catch (error) {
    console.warn('Solicitudes:', error.message);
  }

  try {
    const contactos = await Api.contactos({ tamanio: 30 });
    soporteContactosCache = contactos.contenido || [];
    const tbody = document.getElementById('tablaContactos');
    if (tbody) {
      tbody.innerHTML = soporteContactosCache.length
        ? soporteContactosCache.map((c) => `<tr>
            <td>#${c.idContacto}</td>
            <td>${escaparHtml(c.nombre)}</td>
            <td>${escaparHtml(c.correo)}</td>
            <td>${escaparHtml(c.asunto || '—')}</td>
            <td>${escaparHtml(c.tipo || 'mensaje')}</td>
            <td>${fechaHora(c.creadoEn)}</td>
            <td>
              <button type="button" class="panel-accion-btn" title="Responder mensaje"
                      onclick="responderContactoUI(${c.idContacto})">
                <i class="fa-solid fa-reply"></i>
              </button>
            </td>
          </tr>`).join('')
        : '<tr><td colspan="7" style="text-align:center; color:var(--texto-tenue);">No hay mensajes.</td></tr>';
    }
  } catch (error) {
    console.warn('Contactos:', error.message);
  }
}

/* --------------------------------- Chat -------------------------------- */

// Carga la bandeja de conversaciones con clientes y visitantes web para que el asesor abra y responda chats
async function cargarListaChats() {  const tbody = document.getElementById('tablaChats');
  if (!tbody) return;

  try {
    const clientes = await Api.clientesChat();
    if (!clientes.length) {
      tbody.innerHTML = '<tr><td colspan="5" style="color:var(--texto-tenue);">Todavía no hay clientes con conversaciones.</td></tr>';
      return;
    }

    const clienteActivo = document.getElementById('chatMensajes')?.dataset.cliente || '';
    tbody.innerHTML = clientes.map((c) => {
      const preview = c.ultimoMensaje
        ? String(c.ultimoMensaje).replace(/\s+/g, ' ').slice(0, 75) + (String(c.ultimoMensaje).length > 75 ? '…' : '')
        : 'Sin mensajes todavía';
      const esVisita = String(c.origen) === 'visitante';
      const origenBadge = esVisita
        ? '<span class="badge-estado" style="background:var(--azul-acero,#d5e3f0);color:#122536;" title="Chat iniciado desde el sitio web">Web</span> '
        : '';
      const estadoBadge = origenBadge + (c.solicitaAsesor
        ? '<span class="chat-lista__badge chat-lista__badge--asesor">Solicita asesor</span>'
        : (Number(c.sinLeer || 0) > 0
          ? `<span class="chat-lista__badge">${c.sinLeer} sin leer</span>`
          : (Number(c.totalMensajes || 0) > 0
            ? '<span class="badge-estado badge-estado--activo">Respondido</span>'
            : '<span class="badge-estado badge-estado--inactivo">Nuevo</span>')));
      const activo = String(clienteActivo) === String(c.idCliente) ? ' style="background:rgba(var(--cian-rgb),0.08);"' : '';
      const correo = c.correo ? escaparHtml(c.correo) : '<span style="color:var(--texto-tenue);">Sin correo</span>';
      return `
        <tr${activo}>
          <td><strong>${escaparHtml(c.nombre)}</strong></td>
          <td>${correo}</td>
          <td style="max-width:280px; color:var(--texto-tenue); font-size:.84rem;">
            ${escaparHtml(preview)}${c.creadoEnUltimo ? `<br><small style="opacity:.78;">${fechaHora(c.creadoEnUltimo)}</small>` : ''}
          </td>
          <td>${estadoBadge}</td>
          <td>
            <button type="button" class="btn btn--primario" style="padding:5px 10px; font-size:.78rem;"
                    data-cliente="${c.idCliente}" data-nombre="${escaparHtml(c.nombre)}">
              <i class="fa-solid fa-comments"></i> Abrir
            </button>
          </td>
        </tr>`;
    }).join('');

    // Al pulsar "Abrir" en una fila, muestra el hilo de conversación de esa persona debajo
    tbody.querySelectorAll('[data-cliente]').forEach((boton) => {
      boton.addEventListener('click', (e) => {
        e.preventDefault();
        abrirConversacionCliente(boton.dataset.cliente, boton.dataset.nombre);
      });
    });

    // Si el asesor entró desde una notificación (?visita= o ?cliente=), abre ese chat automáticamente
    const params = new URLSearchParams(window.location.search);
    const paramVisita = params.get('visita');
    const paramCliente = params.get('cliente');
    if (!clienteActivo) {
      if (paramVisita) {
        const id = 'cv-' + paramVisita;
        const target = clientes.find((c) => String(c.idCliente) === id);
        abrirConversacionCliente(id, target ? target.nombre : 'Visitante del sitio');
      } else if (paramCliente) {
        const target = clientes.find((c) => String(c.idCliente) === String(paramCliente));
        if (target) {
          abrirConversacionCliente(target.idCliente, target.nombre);
        }
      }
    }
  } catch (error) {
    tbody.innerHTML = `<tr><td colspan="5" style="color:var(--texto-tenue);">${escaparHtml(error.message)}</td></tr>`;
  }
}

/** Prepara el panel de chat con el hilo del cliente elegido. */
function abrirConversacionCliente(idCliente, nombre) {
  const panel = document.getElementById('panelConversacion');
  const contenedor = document.getElementById('chatMensajes');
  if (!panel || !contenedor) return;

  panel.style.display = '';
  document.getElementById('chatConNombre').textContent = nombre || 'Conversación';
  contenedor.dataset.cliente = String(idCliente);
  contenedor.innerHTML = '<p style="color:var(--texto-tenue); padding:14px;">Cargando mensajes…</p>';
  panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  inicializarChat();
}

/* ------------------------------- Favoritos ----------------------------- */

// Carga y dibuja las tarjetas de los productos marcados como favoritos dentro del panel del cliente
async function cargarFavoritos() {
  const contenedor = document.getElementById('listaFavoritos');
  if (!contenedor) return;

  try {
    const favoritos = await Api.favoritos();
    if (!favoritos.length) {
      contenedor.innerHTML = `<p class="cargando" style="color:var(--texto-tenue);">
        Todavía no tienes favoritos.
        <a href="../../pages/productos.html" style="color:var(--cian);">Explora el catálogo</a>
      </p>`;
      return;
    }

    contenedor.innerHTML = favoritos.map((p) => `
      <article class="tarjeta-producto">
        <div class="tarjeta-producto__imagen">
          <img src="../../${escaparHtml(p.imagen)}" alt="${escaparHtml(p.nombre)}">
        </div>
        <div class="tarjeta-producto__info">
          <h3>${escaparHtml(p.nombre)}</h3>
          <div class="tarjeta-producto__footer">
            <span class="precio">${textoImporte(p.precioFinal)}</span>
            <button class="btn-agregar" onclick="quitarFavorito(${p.idProducto})"
                    title="Quitar de favoritos"><i class="fa-solid fa-heart-crack"></i> Quitar</button>
          </div>
        </div>
      </article>`).join('');
  } catch (error) {
    contenedor.innerHTML = `<p class="cargando">${escaparHtml(error.message)}</p>`;
  }
}

// Quita un producto de la lista de favoritos del cliente y refresca las tarjetas
async function quitarFavorito(idProducto) {
  try {
    await Api.alternarFavorito(idProducto);
    mostrarToast('Producto quitado de favoritos.', 'info');
    await cargarFavoritos();
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}

/* -------------------------------- Perfil ------------------------------- */

// Rellena los datos personales del usuario conectado y permite cambiar su foto, nombre, teléfono o contraseña
function cargarPerfil() {
  const form = document.getElementById('formPerfil');
  if (!form) return;

  const usuario = Api.getUsuario() || {};
  form.nombre.value = usuario.nombre || '';
  form.correo.value = usuario.correo || '';
  form.telefono.value = usuario.telefono || '';

  // Agrega el selector de foto de perfil y sube la imagen al servidor apenas el usuario la elige
  if (!document.getElementById('archivoAvatarPerfil')) {
    const campoAvatar = document.createElement('div');
    campoAvatar.className = 'campo';
    campoAvatar.innerHTML = `
      <label for="archivoAvatarPerfil">Foto de perfil (JPG, PNG, WEBP)</label>
      <input type="file" id="archivoAvatarPerfil" accept="image/*">`;
    const btnGuardar = form.querySelector('button[type="submit"]');
    if (btnGuardar) form.insertBefore(campoAvatar, btnGuardar);
    else form.appendChild(campoAvatar);

    const inputAvatar = campoAvatar.querySelector('#archivoAvatarPerfil');
    inputAvatar.addEventListener('change', async () => {
      const archivo = inputAvatar.files && inputAvatar.files[0];
      if (!archivo) return;
      try {
        const res = await Api.subirImagen(archivo, 'avatars');
        const actual = Api.getUsuario() || {};
        actual.avatar = res.url || ('/' + res.ruta);
        localStorage.setItem('tecnicsecure_usuario', JSON.stringify(actual));
        if (typeof pintarUsuarioPanel === 'function') pintarUsuarioPanel();
        mostrarToast('Foto de perfil actualizada.', 'ok');
      } catch (error) {
        mostrarToast(error.message || 'No se pudo subir el avatar.', 'error');
      }
    });
  }

  const respuesta = document.getElementById('respuestaPerfil');

  // Guarda las actualizaciones del perfil en el servidor y refresca el nombre en la cabecera del panel
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    respuesta.className = 'form-respuesta';
    respuesta.textContent = '';

    try {
      const actualizado = await Api.put('/auth/me', {
        nombre: form.nombre.value.trim(),
        telefono: form.telefono.value.trim(),
        documento: usuario.documento || null,
        passwordActual: form.passwordActual.value || null,
        passwordNueva: form.passwordNueva.value || null
      });
      const previo = Api.getUsuario() || {};
      if (!actualizado.avatar && previo.avatar) {
        actualizado.avatar = previo.avatar;
      }

      localStorage.setItem('tecnicsecure_usuario', JSON.stringify(actualizado));
      respuesta.textContent = 'Perfil actualizado correctamente.';
      respuesta.className = 'form-respuesta ok';
      form.passwordActual.value = '';
      form.passwordNueva.value = '';
      document.querySelectorAll('[data-nombre-usuario]').forEach((el) => {
        el.textContent = actualizado.nombre || '';
      });
      if (typeof pintarUsuarioPanel === 'function') pintarUsuarioPanel();
    } catch (error) {
      respuesta.textContent = error.message;
      respuesta.className = 'form-respuesta error';
    }
  });
}

/* --------------------------- Mis pedidos (cliente) --------------------- */

// Muestra al cliente el historial completo de sus pedidos con botones para ver seguimiento, factura o pagar
async function cargarMisPedidos() {
  const tbody = document.getElementById('tablaMisPedidos');
  if (!tbody) return;

  try {
    const datos = await Api.misPedidos();
    if (!datos.contenido.length) {
      tablaVacia(tbody, 6, 'Aún no tienes pedidos.');
      return;
    }

    tbody.innerHTML = datos.contenido.map((p) => `
      <tr>
        <td>#${p.idPedido}</td>
        <td>${fechaHora(p.creadoEn)}</td>
        <td>${textoImporte(p.total)}</td>
        <td><span class="badge-rol">${estadoLegible(p.estado)}</span></td>
        <td>${(p.detalles || []).length} producto(s)</td>
        <td>
          <div class="panel-acciones">
            <button type="button" class="panel-accion-btn" title="Ver ruta" onclick="verRutaPedido(${p.idPedido})">
              <i class="fa-solid fa-route"></i>
            </button>
            <button type="button" class="panel-accion-btn" title="Ver factura" onclick="verFactura(${p.idPedido})">
              <i class="fa-solid fa-file-invoice"></i>
            </button>
            ${p.estado === 'pendiente' ? `
            <button type="button" class="btn btn--primario" style="padding:4px 9px; font-size:.75rem;"
                    onclick="pagarPedidoUI(${p.idPedido})">Pagar</button>` : ''}
          </div>
        </td>
      </tr>`).join('');
  } catch (error) {
    tablaVacia(tbody, 6, error.message);
  }
}

// Procesa el pago en línea de un pedido pendiente tras pedir confirmación al usuario
async function pagarPedidoUI(idPedido) {
  const ok = await confirmarAccion('Confirmar pago en línea', `¿Deseas procesar el pago del pedido #${idPedido} mediante Wompi PSE / Tarjeta?`);
  if (!ok) return;
  try {
    const r = await Api.pagarPedido(idPedido, 'Wompi PSE / Tarjeta');
    mostrarToast(`Pago aprobado (${r.referencia}). Factura ${r.numeroFactura}.`, 'ok');
    if (document.getElementById('tablaMisPedidos')) await cargarMisPedidos();
    if (document.getElementById('tablaFacturacion')) await cargarFacturacion();
  } catch (error) {
    mostrarToast(error.message, 'error');
  }
}
window.pagarPedidoUI = pagarPedidoUI;

/* ------------------------------ Auditoría ------------------------------ */

// Configura los filtros por texto/fechas, la exportación en archivo para Excel y la depuración de registros antiguos de auditoría
function inicializarAuditoria() {
  const buscar = document.getElementById('buscarAuditoria');
  const desde = document.getElementById('desdeAuditoria');
  const hasta = document.getElementById('hastaAuditoria');
  const btnCsv = document.getElementById('btnExportarAuditoria');
  const btnPurge = document.getElementById('btnDepurarAuditoria');

  let t;
  if (buscar) {
    buscar.addEventListener('input', () => { clearTimeout(t); t = setTimeout(cargarAuditoria, 250); });
  }
  if (desde) desde.addEventListener('change', cargarAuditoria);
  if (hasta) hasta.addEventListener('change', cargarAuditoria);

  // Botón para descargar el registro filtrado de auditoría en formato archivo para Excel
  if (btnCsv) {
    btnCsv.addEventListener('click', () => {
      const params = {
        texto: buscar?.value.trim() || '',
        desde: desde?.value || '',
        hasta: hasta?.value || ''
      };
      Api.descargar(Api.urlExportarAuditoria(params), 'auditoria.csv')
        .then(() => mostrarToast('Auditoría exportada en CSV.', 'ok'))
        .catch((e) => mostrarToast(e.message, 'error'));
    });
  }

  // Botón para limpiar eventos de auditoría con más de 90 días de antigüedad
  if (btnPurge) {
    btnPurge.addEventListener('click', async () => {
      const ok = await confirmarAccion('Depurar auditoría', '¿Eliminar eventos de auditoría con más de 90 días de antigüedad?');
      if (!ok) return;
      try {
        const res = await Api.depurarAuditoria(90);
        mostrarToast(`Depuración completada (${res.eliminados ?? 0} registro(s) eliminados).`, 'ok');
        await cargarAuditoria();
      } catch (e) {
        mostrarToast(e.message, 'error');
      }
    });
  }

  cargarAuditoria();
}

// Consulta y dibuja en tabla los eventos de seguridad y acciones registradas en el sistema
async function cargarAuditoria() {
  const tbody = document.getElementById('tablaAuditoria');
  if (!tbody) return;
  const texto = document.getElementById('buscarAuditoria')?.value.trim() || '';
  const desde = document.getElementById('desdeAuditoria')?.value || '';
  const hasta = document.getElementById('hastaAuditoria')?.value || '';
  try {
    const datos = await Api.auditoria({ texto, desde, hasta, tamanio: 100 });
    const lista = datos.contenido || [];
    if (!lista.length) {
      tablaVacia(tbody, 5, 'No se encontraron eventos de auditoría con esos filtros.');
      return;
    }
    tbody.innerHTML = lista.map((l) => `
      <tr>
        <td>${fechaHora(l.creadoEn)}</td>
        <td>${escaparHtml(l.nombreUsuario || 'Sistema')}</td>
        <td><span class="badge-rol">${escaparHtml(l.rolUsuario || 'sistema')}</span></td>
        <td><span class="badge-rol">${escaparHtml(l.accion)}</span></td>
        <td>${escaparHtml(l.detalle || '—')}</td>
      </tr>`).join('');
  } catch (error) {
    tablaVacia(tbody, 5, error.message);
  }
}

/* ----------------------------- Facturación ----------------------------- */

let pedidoFacturaActual = null;

// Muestra un aviso flotante indicando si el envío de la factura por correo fue exitoso o falló
function mostrarResultadoEnvioFactura(r) {
  const estado = r?.estado || '';
  const msg = r?.mensaje || `Procesado para ${r?.destinatario || 'el cliente'}.`;
  if (estado === 'ENVIADO') {
    mostrarToast(msg, 'ok');
  } else if (estado === 'ERROR_ENVIO' || estado === 'SIN_DESTINATARIO') {
    mostrarToast(msg, 'error');
  } else {
    mostrarToast(msg, 'info');
  }
}

// Prepara la página para imprimir únicamente la factura abierta y abre el diálogo de impresión del navegador
function imprimirFacturaActual() {
  document.body.classList.add('imprimiendo-factura');
  window.print();
  setTimeout(() => document.body.classList.remove('imprimiendo-factura'), 800);
}
window.addEventListener('afterprint', () => document.body.classList.remove('imprimiendo-factura'));
window.imprimirFacturaActual = imprimirFacturaActual;

// Activa el botón de descargar la factura abierta como archivo HTML en el dispositivo
function inicializarBotonesFacturaComunes() {
  const btnDescargar = document.getElementById('btnDescargarFactura');
  if (btnDescargar && !btnDescargar.dataset.bind) {
    btnDescargar.dataset.bind = '1';
    btnDescargar.addEventListener('click', async () => {
      if (!pedidoFacturaActual) return;
      const nombre = `FAC-${String(pedidoFacturaActual).padStart(5, '0')}.html`;
      try {
        await Api.descargar(Api.urlDescargarFactura(pedidoFacturaActual), nombre);
        mostrarToast(`Factura ${nombre} descargada.`, 'ok');
      } catch (e) {
        mostrarToast(e.message, 'error');
      }
    });
  }
}

// Configura el módulo de facturación: envío por correo, envío a otro correo, pago y emisión de Nota Crédito
function inicializarFacturacion() {
  inicializarBotonesFacturaComunes();
  const btnEnviar = document.getElementById('btnEnviarFactura');
  const btnPagar = document.getElementById('btnPagarFactura');
  const btnNota = document.getElementById('btnNotaCredito');

  // Pagar y emitir Nota Credito son operaciones contables del administrador.
  const esAdmin = ((Api.getUsuario()?.rol) || '').toLowerCase() === 'administrador';
  if (!esAdmin) {
    document.querySelectorAll('[data-solo-admin]').forEach((el) => { el.style.display = 'none'; });
  }

  // Envía la factura abierta al correo principal registrado del cliente
  if (btnEnviar) {
    btnEnviar.addEventListener('click', async () => {
      if (!pedidoFacturaActual) return;
      try {
        const r = await Api.enviarFactura(pedidoFacturaActual);
        mostrarResultadoEnvioFactura(r);
      } catch (e) {
        mostrarToast(e.message, 'error');
      }
    });
  }

  // Enviar la misma factura a otro correo (por si el cliente usa uno distinto).
  const btnOtro = document.getElementById('btnEnviarFacturaOtro');
  const cajaOtro = document.getElementById('facturaEnvioOtro');
  const inputOtro = document.getElementById('facturaCorreoDestino');
  const btnConfirmar = document.getElementById('facturaEnvioOtroConfirmar');
  const btnCancelarOtro = document.getElementById('facturaEnvioOtroCancelar');
  const respuestaOtro = document.getElementById('facturaEnvioOtroRespuesta');

  if (btnOtro && cajaOtro) {
    btnOtro.addEventListener('click', () => {
      cajaOtro.hidden = !cajaOtro.hidden;
      if (!cajaOtro.hidden) {
        if (respuestaOtro) respuestaOtro.textContent = '';
        inputOtro?.focus();
      }
    });
  }
  if (btnCancelarOtro) {
    btnCancelarOtro.addEventListener('click', () => {
      if (cajaOtro) cajaOtro.hidden = true;
      if (respuestaOtro) respuestaOtro.textContent = '';
    });
  }
  if (btnConfirmar) {
    btnConfirmar.addEventListener('click', async () => {
      const correo = (inputOtro?.value || '').trim();
      if (!correo || !/^[^\s@]+@[^\s@]+\.[A-Za-z]{2,}$/.test(correo)) {
        if (respuestaOtro) {
          respuestaOtro.textContent = 'Escribe un correo válido, por ejemplo cliente@gmail.com';
          respuestaOtro.className = 'form-respuesta error';
        }
        inputOtro?.focus();
        return;
      }
      const texto = btnConfirmar.innerHTML;
      btnConfirmar.disabled = true;
      btnConfirmar.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Enviando…';
      try {
        const r = await Api.enviarFacturaA(pedidoFacturaActual, correo);
        if (respuestaOtro) {
          respuestaOtro.textContent = r?.mensaje || 'Procesado.';
          respuestaOtro.className = 'form-respuesta ' + (r?.estado === 'ENVIADO' ? 'ok' : 'error');
        }
        mostrarResultadoEnvioFactura(r);
        if (r?.estado === 'ENVIADO') {
          setTimeout(() => { if (cajaOtro) cajaOtro.hidden = true; }, 2500);
        }
      } catch (e) {
        if (respuestaOtro) {
          respuestaOtro.textContent = e.message;
          respuestaOtro.className = 'form-respuesta error';
        }
      } finally {
        btnConfirmar.disabled = false;
        btnConfirmar.innerHTML = texto;
      }
    });
  }

  // Botón para registrar el pago de la factura abierta
  if (btnPagar) {
    btnPagar.addEventListener('click', async () => {
      if (!pedidoFacturaActual) return;
      await pagarPedidoUI(pedidoFacturaActual);
      await verFactura(pedidoFacturaActual);
    });
  }

  // Botón para emitir una Nota Crédito contable en lugar de borrar la factura (normativa DIAN)
  if (btnNota) {
    btnNota.addEventListener('click', async () => {
      if (!pedidoFacturaActual) return;
      const ok = await confirmarAccion('Emitir Nota Crédito', 'Por normativa DIAN las facturas no se borran; se emite una Nota Crédito auditada. ¿Continuar?');
      if (!ok) return;
      try {
        const r = await Api.notaCreditoFactura(pedidoFacturaActual, 'Anulación/ajuste mediante Nota Crédito administrativa');
        mostrarToast(`Nota crédito emitida para ${r.numeroFactura}.`, 'info');
        await verFactura(pedidoFacturaActual);
        await cargarFacturacion();
      } catch (e) {
        mostrarToast(e.message, 'error');
      }
    });
  }

  cargarFacturacion();
  mostrarEstadoCorreo();
}

/** Indica si el equipo puede enviar correos reales (sin exponer credenciales). */
async function mostrarEstadoCorreo() {
  const caja = document.getElementById('correoEstadoFactura');
  if (!caja) return;
  try {
    const estado = await Api.estadoCorreoFactura();
    caja.hidden = false;
    if (estado.configurado) {
      caja.className = 'correo-estado correo-estado--ok';
      caja.innerHTML = `<i class="fa-solid fa-circle-check"></i>
        <span>El envío de facturas por correo está <strong>activo</strong>
        (${escaparHtml(estado.remitente || 'servidor configurado')}).</span>`;
    } else {
      caja.className = 'correo-estado correo-estado--aviso';
      caja.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i>
        <span>El envío de correos aún no está configurado. Puedes descargar o imprimir las facturas.</span>`;
    }
  } catch {
    caja.hidden = true;
  }
}

// Carga la lista de facturas electrónicas generadas a partir de los pedidos del sistema
async function cargarFacturacion() {
  const tbody = document.getElementById('tablaFacturacion');
  if (!tbody) return;
  try {
    const datos = await Api.pedidos({ tamanio: 50 });
    const lista = datos.contenido || [];
    if (!lista.length) {
      tablaVacia(tbody, 6, 'No hay comprobantes generados.');
      return;
    }
    tbody.innerHTML = lista.map((p) => `
      <tr>
        <td><strong>FAC-${String(p.idPedido).padStart(5, '0')}</strong> <small style="color:var(--texto-tenue);">(Ped #${p.idPedido})</small></td>
        <td>${escaparHtml(p.clienteNombre || '—')}</td>
        <td>${fechaHora(p.creadoEn)}</td>
        <td><span class="badge-rol">${estadoLegible(p.estado)}</span></td>
        <td>${textoImporte(p.total)}</td>
        <td>
          <div class="panel-acciones">
            <button type="button" class="panel-accion-btn" title="Ver factura electrónica" onclick="verFactura(${p.idPedido})">
              <i class="fa-solid fa-file-invoice"></i>
            </button>
            <button type="button" class="panel-accion-btn" title="Enviar por correo" onclick="enviarFacturaDirecto(${p.idPedido})">
              <i class="fa-solid fa-paper-plane"></i>
            </button>
          </div>
        </td>
      </tr>`).join('');
  } catch (error) {
    tablaVacia(tbody, 6, error.message);
  }
}

// Abre la vista detallada de una factura electrónica con datos fiscales, ítems, IVA, total y código código único de factura
async function verFactura(idPedido) {
  const cont = document.getElementById('contenidoFactura');
  if (!cont) return;
  inicializarBotonesFacturaComunes();
  pedidoFacturaActual = idPedido;
  cont.innerHTML = 'Generando vista fiscal…';
  abrirModal('modalFactura');
  try {
    const f = await Api.facturaPedido(idPedido);
    const filas = (f.items || []).map((it) => `
      <tr>
        <td>${escaparHtml(it.producto)}</td>
        <td>${it.cantidad}</td>
        <td>${textoImporte(it.precioUnitario)}</td>
        <td>${textoImporte(it.subtotal)}</td>
      </tr>`).join('');

    cont.innerHTML = `
      <div style="display:flex; justify-content:space-between; flex-wrap:wrap; gap:12px; border-bottom:2px solid #0a192f; padding-bottom:10px; margin-bottom:12px;">
        <div>
          <h4>${escaparHtml(f.emisorRazonSocial)}</h4>
          <div>NIT: ${escaparHtml(f.emisorNit)} · ${escaparHtml(f.emisorRegimen)}</div>
          <div style="font-size:.76rem; color:#555;">${escaparHtml(f.resolucionDian)}</div>
        </div>
        <div style="text-align:right;">
          <h4 style="color:#0077b6;">FACTURA DE VENTA ${escaparHtml(f.numeroFactura)}</h4>
          <div>Emisión: ${fechaHora(f.fechaEmision)}</div>
          <div>Estado fiscal: <strong>${escaparHtml((f.estadoFactura || 'emitida').toUpperCase())}</strong></div>
        </div>
      </div>
      <div style="margin-bottom:10px;">
        <strong>Cliente:</strong> ${escaparHtml(f.clienteNombre || '—')} ·
        <strong>Correo:</strong> ${escaparHtml(f.clienteCorreo || '—')} ·
        <strong>Tel:</strong> ${escaparHtml(f.clienteTelefono || '—')}<br>
        <strong>Dirección:</strong> ${escaparHtml(f.clienteDireccion || '—')}
        ${f.metodoPago ? ` · <strong>Pago:</strong> ${escaparHtml(f.metodoPago)}` : ''}
      </div>
      <table>
        <thead><tr><th>Descripción</th><th>Cant.</th><th>V. Unit</th><th>Subtotal</th></tr></thead>
        <tbody>${filas || '<tr><td colspan="4">Sin ítems</td></tr>'}</tbody>
      </table>
      <div class="factura-totales">
        <div>Base gravable: <strong>${textoImporte(f.subtotalBase)}</strong></div>
        <div>IVA (${f.ivaPct}%): <strong>${textoImporte(f.ivaValor)}</strong></div>
        ${Number(f.descuento || 0) > 0 ? `<div>Descuento: -${textoImporte(f.descuento)}</div>` : ''}
        <div style="font-size:1.1rem; margin-top:4px;">TOTAL A PAGAR: <strong>${textoImporte(f.total)}</strong></div>
      </div>
      <div style="margin-top:12px; font-size:.72rem; color:#555; word-break:break-all;">
        <strong>CUFE:</strong> ${escaparHtml(f.cufe)}
      </div>`;
  } catch (error) {
    cont.innerHTML = `<p style="color:#c0392b;">${escaparHtml(error.message)}</p>`;
  }
}

// Envía directamente la factura por correo desde el botón rápido de la tabla
async function enviarFacturaDirecto(idPedido) {
  try {
    const r = await Api.enviarFactura(idPedido);
    mostrarResultadoEnvioFactura(r);
  } catch (e) {
    mostrarToast(e.message, 'error');
  }
}

window.verFactura = verFactura;
window.enviarFacturaDirecto = enviarFacturaDirecto;

/* ------------------------------ Respaldo ------------------------------- */

// Conecta los tres botones de copia de seguridad (respaldo archivo comprimido completo, ventas archivo para Excel e inventario archivo para Excel)
function configurarRespaldo() {
  const btnCompleto = document.getElementById('btnRespaldoCompleto');
  const btnVentas = document.getElementById('btnRespaldoVentas');
  const btnInv = document.getElementById('btnRespaldoInventario');
  const resp = document.getElementById('respuestaRespaldo');

  if (btnCompleto) {
    btnCompleto.addEventListener('click', async () => {
      try {
        await Api.descargar(Api.urlRespaldoCompleto(), 'tecnicsecure-respaldo.zip');
        mostrarToast('Respaldo completo en ZIP generado correctamente.', 'ok');
        if (resp) { resp.textContent = 'Respaldo ZIP completo descargado.'; resp.className = 'form-respuesta ok'; }
      } catch (e) {
        mostrarToast(e.message, 'error');
        if (resp) { resp.textContent = e.message; resp.className = 'form-respuesta error'; }
      }
    });
  }
  if (btnVentas) {
    btnVentas.addEventListener('click', async () => {
      try {
        await Api.descargar(Api.urlExportarVentas(), 'respaldo-ventas.csv');
        mostrarToast('Respaldo de ventas descargado.', 'ok');
        if (resp) { resp.textContent = 'Respaldo de ventas descargado.'; resp.className = 'form-respuesta ok'; }
      } catch (e) {
        mostrarToast(e.message, 'error');
        if (resp) { resp.textContent = e.message; resp.className = 'form-respuesta error'; }
      }
    });
  }
  if (btnInv) {
    btnInv.addEventListener('click', async () => {
      try {
        await Api.descargar(Api.urlExportarInventario(), 'respaldo-inventario.csv');
        mostrarToast('Respaldo de inventario descargado.', 'ok');
        if (resp) { resp.textContent = 'Respaldo de inventario descargado.'; resp.className = 'form-respuesta ok'; }
      } catch (e) {
        mostrarToast(e.message, 'error');
        if (resp) { resp.textContent = e.message; resp.className = 'form-respuesta error'; }
      }
    });
  }
}

/* ------------- Fase 4: Empleados (Permisos, Tareas, Desempeño) ---------- */

let cacheEmpleadosCarga = [];
let cacheTareasAdmin = [];
let cacheMisTareas = [];
let cacheAreasActividades = {};

// Si la evidencia es un enlace o archivo subido, muestra un botón clicable "Ver archivo"; si es texto, lo muestra limpio
function construirHtmlEvidencia(evidencia) {
  if (!evidencia) return '—';
  const str = String(evidencia).trim();
  if (str.startsWith('/uploads/') || str.startsWith('http://') || str.startsWith('https://')) {
    const seguro = escaparHtml(str);
    return `<a href="${seguro}" target="_blank" rel="noopener" style="color:var(--cian); text-decoration:underline;"><i class="fa-solid fa-paperclip"></i> Ver archivo</a>`;
  }
  return escaparHtml(str);
}

// Muestra la vista previa de tareas típicas del área elegida (Operativo, Ventas, Soporte, etc.)
function pintarActividadesPreviewArea(area) {
  const cont = document.getElementById('previewActividadesRol');
  if (!cont) return;
  const acts = cacheAreasActividades[area] || [];
  if (!acts.length) {
    cont.textContent = 'Sin actividades predefinidas para esta área.';
    return;
  }
  cont.innerHTML = acts.map((a) => `
    <div style="margin-bottom:6px;">
      <strong>• ${escaparHtml(a.titulo || '')}</strong>
      <span class="badge-rol" style="margin-left:6px;">${escaparHtml(a.prioridad || 'media')}</span>
      ${a.descripcion ? `<div style="color:var(--texto-tenue); font-size:.8rem; margin-left:10px;">${escaparHtml(a.descripcion)}</div>` : ''}
    </div>`).join('');
}

// Carga la tabla de empleados con sus módulos permitidos y conecta el formulario de edición de área y permisos
async function inicializarPermisosEmpleados() {
  const tbody = document.getElementById('tablaPermisosEmpleados');
  if (!tbody) return;

  cacheAreasActividades = await Api.areasActividades().catch(() => ({}));

  // Trae el listado de empleados, su carga de trabajo activa y su calificación promedio
  const cargarTabla = async () => {
    try {
      cacheEmpleadosCarga = await Api.empleadosCarga();
      if (!cacheEmpleadosCarga.length) {
        tablaVacia(tbody, 6, 'No hay empleados registrados.');
        return;
      }
      tbody.innerHTML = cacheEmpleadosCarga.map((e) => `
        <tr>
          <td><strong>${escaparHtml(e.nombre)}</strong><br><small style="color:var(--texto-tenue);">${escaparHtml(e.correo)}</small></td>
          <td><span class="badge-rol">${escaparHtml(e.area || 'OPERATIVO')}</span></td>
          <td>${(e.modulos || []).filter((m) => m !== 'dashboard' && m !== 'perfil' && m !== 'tareas').map((m) => `<span class="badge-estado badge-estado--activo" style="margin:2px;">${escaparHtml(m)}</span>`).join(' ') || 'Solo base'}</td>
          <td><strong>${e.cargaActiva ?? 0}</strong> activas</td>
          <td>${e.promedioCalificacion ? `${e.promedioCalificacion} / 5` : 'Sin calificar'}</td>
          <td>
            <button type="button" class="panel-accion-btn" title="Configurar área y permisos"
                    onclick="abrirPermisosEmpleado(${e.idUsuario})">
              <i class="fa-solid fa-sliders"></i>
            </button>
          </td>
        </tr>`).join('');
    } catch (error) {
      tablaVacia(tbody, 6, error.message);
    }
  };

  // Guarda el área y los módulos marcados para un empleado (y opcionalmente le asigna tareas iniciales del rol)
  const form = document.getElementById('formPermisosEmpleado');
  if (form) {
    form.area?.addEventListener('change', () => pintarActividadesPreviewArea(form.area.value));
    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const resp = document.getElementById('respuestaPermisosModal');
      if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
      const idUsuario = Number(form.idUsuario.value);
      const area = form.area.value;
      const modulos = Array.from(form.querySelectorAll('input[name="modulos"]:checked')).map((c) => c.value);
      const asignarActividadesRol = Boolean(form.asignarActividadesRol?.checked);
      try {
        const res = await Api.actualizarPermisosEmpleado(idUsuario, { area, modulos, asignarActividadesRol });
        cerrarModal('modalPermisosEmpleado');
        const creadas = Number(res?.tareasCreadas || 0);
        mostrarToast(
          creadas > 0
            ? `Área actualizada y ${creadas} actividad(es) del rol asignadas.`
            : 'Área y permisos actualizados correctamente.',
          'ok'
        );
        await cargarTabla();
      } catch (error) {
        if (resp) { resp.textContent = error.message; resp.className = 'form-respuesta error campo--full'; }
      }
    });
  }

  await cargarTabla();
}

// Abre la ventana emergente de permisos precargando el área y casillas actuales del empleado elegido
function abrirPermisosEmpleado(idUsuario) {
  const form = document.getElementById('formPermisosEmpleado');
  const emp = cacheEmpleadosCarga.find((e) => e.idUsuario === idUsuario);
  if (!form || !emp) return;
  form.idUsuario.value = emp.idUsuario;
  form.area.value = emp.area || 'OPERATIVO';
  pintarActividadesPreviewArea(form.area.value);
  const habilitados = new Set(emp.modulos || []);
  form.querySelectorAll('input[name="modulos"]').forEach((chk) => {
    chk.checked = habilitados.has(chk.value);
  });
  const titulo = document.getElementById('tituloModalPermisos');
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-shield-halved"></i> Permisos de ${escaparHtml(emp.nombre)}`;
  const resp = document.getElementById('respuestaPermisosModal');
  if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
  abrirModal('modalPermisosEmpleado');
}

// Actualiza el menú de tareas rápidas sugeridas según el área del empleado seleccionado al asignar una tarea
function actualizarPlantillasPorEmpleado() {
  const selectEmp = document.getElementById('selectEmpleadoTarea');
  const selectPlantilla = document.getElementById('selectPlantillaRolTarea');
  if (!selectEmp || !selectPlantilla) return;
  const emp = cacheEmpleadosCarga.find((e) => String(e.idUsuario) === String(selectEmp.value));
  const area = emp?.area || 'OPERATIVO';
  const acts = cacheAreasActividades[area] || [];
  selectPlantilla.innerHTML = `<option value="">Escribir manualmente o elegir actividad (${escaparHtml(area)})…</option>` +
    acts.map((a, idx) => `<option value="${idx}">${escaparHtml(a.titulo)}</option>`).join('');
  selectPlantilla.dataset.area = area;
}

// Configura el módulo de tareas del administrador: listado de tareas, desempeño del equipo, asignación y calificación
async function inicializarTareasAdmin() {
  const tbody = document.getElementById('tablaTareasAdmin');
  const tbodyDes = document.getElementById('tablaDesempenoAdmin');
  if (!tbody) return;

  // Trae empleados, tareas asignadas, tabla de desempeño y plantillas de actividades
  const cargarTodo = async () => {
    try {
      const [empleados, tareas, desempeno, areas] = await Promise.all([
        Api.empleadosCarga(),
        Api.todasTareas(),
        Api.desempenoEmpleados(),
        Api.areasActividades().catch(() => ({}))
      ]);
      cacheEmpleadosCarga = empleados || [];
      cacheTareasAdmin = tareas || [];
      cacheAreasActividades = areas || {};

      const selectEmp = document.getElementById('selectEmpleadoTarea');
      if (selectEmp) {
        selectEmp.innerHTML = cacheEmpleadosCarga.map((e) =>
          `<option value="${e.idUsuario}">${escaparHtml(e.nombre)} — ${escaparHtml(e.area)} (${e.cargaActiva} activas)</option>`
        ).join('') || '<option value="">Sin empleados activos</option>';
        actualizarPlantillasPorEmpleado();
      }

      if (!cacheTareasAdmin.length) {
        tablaVacia(tbody, 9, 'No hay tareas asignadas.');
      } else {
        tbody.innerHTML = cacheTareasAdmin.map((t) => {
          const prio = (t.prioridad || 'media').toLowerCase();
          const badgePrio = (prio === 'urgente' || prio === 'alta')
            ? 'badge-estado--bloqueado'
            : (prio === 'media' ? 'badge-estado--activo' : 'badge-estado--inactivo');
          return `
            <tr>
              <td>#${t.idTarea}</td>
              <td>${escaparHtml(t.empleadoNombre)}</td>
              <td><strong>${escaparHtml(t.titulo)}</strong>${t.descripcion ? `<br><small style="color:var(--texto-tenue);">${escaparHtml(t.descripcion)}</small>` : ''}</td>
              <td><span class="badge-estado ${badgePrio}">${escaparHtml(prio)}</span></td>
              <td>${t.fechaLimite ? escaparHtml(t.fechaLimite) : '—'}</td>
              <td><span class="badge-rol">${escaparHtml(t.estado)}</span>${t.totalEntregas > 0 ? `<br><small style="color:var(--texto-tenue);">${t.totalEntregas} entrega(s)</small>` : ''}</td>
              <td>${construirHtmlEvidencia(t.evidencia)}</td>
              <td>${t.calificacion ? `<strong>${t.calificacion}/5</strong>` : 'Pendiente'}</td>
              <td>
                <button type="button" class="panel-accion-btn" title="Revisar entrega y calificar" onclick="abrirCalificarTarea(${t.idTarea})">
                  <i class="fa-solid fa-clipboard-check"></i>
                </button>
              </td>
            </tr>`;
        }).join('');
      }

      if (tbodyDes) {
        if (!desempeno.length) {
          tablaVacia(tbodyDes, 6, 'Sin datos de desempeño.');
        } else {
          tbodyDes.innerHTML = desempeno.map((d) => `
            <tr>
              <td>${escaparHtml(d.nombre)}</td>
              <td><span class="badge-rol">${escaparHtml(d.area)}</span></td>
              <td>${d.cargaActiva ?? 0}</td>
              <td>${d.tareasCompletadas ?? 0}</td>
              <td><strong>${d.cumplimientoPct ?? 100}%</strong></td>
              <td>${d.promedioCalificacion ? `${d.promedioCalificacion} / 5` : '—'}</td>
            </tr>`).join('');
        }
      }
    } catch (error) {
      tablaVacia(tbody, 9, error.message);
    }
  };

  const btnNueva = document.getElementById('btnNuevaTarea');
  const formAsignar = document.getElementById('formAsignarTarea');
  const selectEmp = document.getElementById('selectEmpleadoTarea');
  const selectPlantilla = document.getElementById('selectPlantillaRolTarea');

  // Al cambiar el empleado o elegir una plantilla, autocompleta título, prioridad y descripción de la tarea
  if (selectEmp) {
    selectEmp.addEventListener('change', actualizarPlantillasPorEmpleado);
  }
  if (selectPlantilla && formAsignar) {
    selectPlantilla.addEventListener('change', () => {
      const idx = selectPlantilla.value;
      if (idx === '') return;
      const area = selectPlantilla.dataset.area || 'OPERATIVO';
      const item = (cacheAreasActividades[area] || [])[Number(idx)];
      if (!item) return;
      formAsignar.titulo.value = item.titulo || '';
      formAsignar.prioridad.value = item.prioridad || 'media';
      formAsignar.descripcion.value = item.descripcion || '';
    });
  }

  // Botón "Nueva tarea" y envío del formulario para asignar trabajo a un empleado
  if (btnNueva && formAsignar) {
    btnNueva.addEventListener('click', () => {
      formAsignar.reset();
      actualizarPlantillasPorEmpleado();
      const resp = document.getElementById('respuestaTareaAdmin');
      if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
      abrirModal('modalAsignarTarea');
    });
    formAsignar.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const resp = document.getElementById('respuestaTareaAdmin');
      try {
        await Api.asignarTarea({
          idEmpleadoUsuario: Number(formAsignar.idEmpleadoUsuario.value),
          titulo: formAsignar.titulo.value.trim(),
          prioridad: formAsignar.prioridad.value,
          fechaLimite: formAsignar.fechaLimite.value || null,
          descripcion: formAsignar.descripcion.value.trim() || null
        });
        cerrarModal('modalAsignarTarea');
        mostrarToast('Tarea asignada al empleado.', 'ok');
        await cargarTodo();
      } catch (error) {
        if (resp) { resp.textContent = error.message; resp.className = 'form-respuesta error campo--full'; }
      }
    });
  }

  // Formulario de revisión: permite aprobar y calificar la entrega (1 a 5) o devolverla para corrección
  const formCalificar = document.getElementById('formCalificarTarea');
  if (formCalificar) {
    formCalificar.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      const resp = document.getElementById('respuestaCalificarModal');
      try {
        const decision = formCalificar.estado?.value || 'revisada';
        await Api.calificarTarea(Number(formCalificar.idTarea.value), {
          estado: decision,
          calificacion: Number(formCalificar.calificacion.value),
          observacionAdmin: formCalificar.observacionAdmin.value.trim() || null
        });
        cerrarModal('modalCalificarTarea');
        mostrarToast(
          decision === 'en_progreso'
            ? 'Tarea devuelta al empleado para corrección.'
            : 'Revisión y calificación guardadas.',
          'ok'
        );
        await cargarTodo();
      } catch (error) {
        if (resp) { resp.textContent = error.message; resp.className = 'form-respuesta error campo--full'; }
      }
    });
  }

  await cargarTodo();
}

// Construye las tarjetas del historial de entregas anteriores que el empleado ha subido para una tarea
function renderizarHistorialEntregas(entregas) {
  if (!Array.isArray(entregas) || !entregas.length) {
    return '<p style="color:var(--texto-tenue); font-size:.84rem; margin:0;">Sin entregas anteriores archivadas en el historial.</p>';
  }
  return `
    <h4 style="margin:0 0 8px 0; font-size:.9rem;"><i class="fa-solid fa-clock-rotate-left"></i> Historial de entregas archivadas (${entregas.length})</h4>
    <div style="display:flex; flex-direction:column; gap:8px; max-height:220px; overflow-y:auto; padding-right:4px;">
      ${entregas.map((e) => {
        const est = (e.estadoRevision || 'en_revision').toLowerCase();
        const etiquetaEst = est === 'aprobada'
          ? 'Aprobada'
          : (est === 'devuelta' ? 'Devuelta para corrección' : 'En revisión');
        const claseEst = est === 'aprobada'
          ? 'badge-estado--activo'
          : (est === 'devuelta' ? 'badge-estado--bloqueado' : 'badge-estado--inactivo');
        return `
          <div style="background:rgba(255,255,255,0.03); border:1px solid var(--azul-acero); border-radius:8px; padding:8px 10px; font-size:.83rem; line-height:1.45;">
            <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:6px; margin-bottom:4px;">
              <strong>Entrega #${e.numeroIntento}</strong>
              <span class="badge-estado ${claseEst}">${etiquetaEst}${e.calificacion ? ` · ${e.calificacion}/5` : ''}</span>
            </div>
            <div style="color:var(--texto-tenue); font-size:.78rem;">
              Enviada: ${e.enviadoEn ? escaparHtml(fechaHora(e.enviadoEn)) : '—'}
              ${e.revisadoEn ? ` · Revisada: ${escaparHtml(fechaHora(e.revisadoEn))}` : ''}
            </div>
            <div style="margin-top:4px;"><strong>Evidencia:</strong> ${construirHtmlEvidencia(e.evidencia)}</div>
            ${e.comentarioEmpleado ? `<div style="margin-top:3px;"><strong>Comentario empleado:</strong> ${escaparHtml(e.comentarioEmpleado)}</div>` : ''}
            ${e.observacionAdmin ? `<div style="margin-top:3px; color:var(--cian);"><strong>Observación admin:</strong> ${escaparHtml(e.observacionAdmin)}</div>` : ''}
          </div>`;
      }).join('')}
    </div>`;
}

// Consulta al servidor todas las entregas pasadas de una tarea y las dibuja dentro dla ventana emergente abierto
async function cargarHistorialEnContenedor(idTarea, idContenedor) {
  const cont = document.getElementById(idContenedor);
  if (!cont) return;
  cont.innerHTML = '<p style="color:var(--texto-tenue); font-size:.84rem; margin:0;">Cargando historial de entregas…</p>';
  try {
    const entregas = await Api.historialTarea(idTarea);
    cont.innerHTML = renderizarHistorialEntregas(entregas);
  } catch (error) {
    cont.innerHTML = `<p style="color:var(--texto-tenue); font-size:.84rem; margin:0;">No se pudo cargar el historial: ${escaparHtml(error.message)}</p>`;
  }
}

// Abre el panel de revisión del administrador mostrando evidencias (con vista previa si es imagen) e historial
function abrirCalificarTarea(idTarea) {
  const form = document.getElementById('formCalificarTarea');
  const tarea = cacheTareasAdmin.find((t) => t.idTarea === idTarea);
  if (!form || !tarea) return;
  form.idTarea.value = tarea.idTarea;
  if (form.estado) form.estado.value = (tarea.estado === 'en_progreso') ? 'en_progreso' : 'revisada';
  form.calificacion.value = String(tarea.calificacion || 5);
  form.observacionAdmin.value = tarea.observacionAdmin || '';

  const detalle = document.getElementById('detalleRevisionTarea');
  if (detalle) {
    const ev = tarea.evidencia || '';
    const esImagen = /\.(png|jpe?g|webp|gif)$/i.test(ev);
    detalle.innerHTML = `
      <div><strong>Empleado:</strong> ${escaparHtml(tarea.empleadoNombre || '—')} · <strong>Estado actual:</strong> <span class="badge-rol">${escaparHtml(tarea.estado)}</span></div>
      ${tarea.descripcion ? `<div style="margin-top:6px;"><strong>Instrucciones:</strong> ${escaparHtml(tarea.descripcion)}</div>` : ''}
      <div style="margin-top:6px;"><strong>Comentario actual del empleado:</strong> ${escaparHtml(tarea.comentarioEmpleado || 'Sin comentarios registrados')}</div>
      <div style="margin-top:6px;"><strong>Evidencia actual adjunta:</strong> ${construirHtmlEvidencia(ev)}</div>
      ${esImagen && ev.startsWith('/uploads/') ? `<div style="margin-top:8px;"><img src="${escaparHtml(ev)}" alt="Evidencia" style="max-width:100%; max-height:180px; border-radius:6px; border:1px solid var(--azul-acero);"></div>` : ''}
    `;
  }

  const titulo = document.getElementById('tituloModalCalificar');
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-clipboard-check"></i> Revisar: ${escaparHtml(tarea.titulo)}`;
  const resp = document.getElementById('respuestaCalificarModal');
  if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
  abrirModal('modalCalificarTarea');
  cargarHistorialEnContenedor(tarea.idTarea, 'historialEntregasAdmin');
}

// Configura la vista "Mis tareas" del empleado: lista de asignaciones, subida de evidencia y reporte de avance
async function inicializarMisTareas() {
  const tbody = document.getElementById('tablaMisTareas');
  if (!tbody) return;

  try {
    const perfil = await Api.miPerfilEmpleado();
    const badge = document.getElementById('miAreaBadge');
    if (badge && perfil?.area) badge.textContent = perfil.area;
  } catch (_) {}

  // Carga las tareas asignadas al empleado actual e indica si están editables, en revisión o aprobadas
  const cargarMis = async () => {
    try {
      cacheMisTareas = await Api.misTareas();
      if (!cacheMisTareas.length) {
        tablaVacia(tbody, 7, 'No tienes tareas asignadas por el momento.');
        return;
      }
      tbody.innerHTML = cacheMisTareas.map((t) => {
        const est = (t.estado || 'pendiente').toLowerCase();
        const bloqueada = est === 'completada' || est === 'revisada';
        const textoEstado = est === 'completada'
          ? 'Completada (en revisión)'
          : (est === 'revisada' ? 'Revisada (aprobada)' : est);
        const claseEstado = est === 'revisada'
          ? 'badge-estado--activo'
          : (est === 'completada' ? 'badge-estado--inactivo' : 'badge-estado--activo');
        const iconoAccion = bloqueada ? 'fa-eye' : 'fa-pen-to-square';
        const tituloAccion = est === 'completada'
          ? 'Ver entrega e historial (bloqueada en espera de revisión)'
          : (est === 'revisada' ? 'Ver tarea aprobada e historial' : 'Reportar avance y subir archivo');
        return `
        <tr>
          <td>#${t.idTarea}</td>
          <td><strong>${escaparHtml(t.titulo)}</strong>${t.descripcion ? `<br><small style="color:var(--texto-tenue);">${escaparHtml(t.descripcion)}</small>` : ''}</td>
          <td><span class="badge-rol">${escaparHtml(t.prioridad || 'media')}</span></td>
          <td>${t.fechaLimite ? escaparHtml(t.fechaLimite) : '—'}</td>
          <td><span class="badge-estado ${claseEstado}">${escaparHtml(textoEstado)}</span></td>
          <td>${t.calificacion ? `<strong>${t.calificacion}/5</strong>` : 'Sin calificar'}</td>
          <td>
            <button type="button" class="panel-accion-btn" title="${tituloAccion}" onclick="abrirProgresoTarea(${t.idTarea})">
              <i class="fa-solid ${iconoAccion}"></i>
            </button>
          </td>
        </tr>`;
      }).join('');
    } catch (error) {
      tablaVacia(tbody, 7, error.message);
    }
  };

  const form = document.getElementById('formProgresoTarea');
  const inputArchivo = document.getElementById('archivoEvidenciaTarea');
  const estadoSubida = document.getElementById('estadoSubidaEvidencia');

  // Sube el archivo de evidencia (foto, PDF, etc.) al servidor apenas el empleado lo selecciona
  if (inputArchivo && form) {
    inputArchivo.addEventListener('change', async () => {
      if (form.dataset.bloqueada === 'true') return;
      const file = inputArchivo.files && inputArchivo.files[0];
      if (!file) return;
      const btnSubmit = form.querySelector('button[type="submit"]');
      if (btnSubmit) btnSubmit.disabled = true;
      if (estadoSubida) estadoSubida.textContent = 'Subiendo archivo de evidencia…';
      try {
        const res = await Api.subirEvidencia(file);
        form.evidencia.value = res.url || ('/' + res.ruta);
        if (estadoSubida) estadoSubida.innerHTML = `<span style="color:var(--verde-ok);">Subido: ${escaparHtml(res.nombreOriginal || file.name)}</span>`;
        mostrarToast('Archivo de evidencia cargado.', 'ok');
      } catch (error) {
        if (estadoSubida) estadoSubida.innerHTML = `<span style="color:var(--naranja);">${escaparHtml(error.message)}</span>`;
        mostrarToast(error.message, 'error');
      } finally {
        if (btnSubmit && form.dataset.bloqueada !== 'true') btnSubmit.disabled = false;
      }
    });
  }

  // Envía el avance o marca la tarea como completada para que el administrador la revise
  if (form) {
    form.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      if (form.dataset.bloqueada === 'true') {
        mostrarToast('Esta tarea está bloqueada hasta que el administrador la revise.', 'aviso');
        return;
      }
      const resp = document.getElementById('respuestaProgresoModal');
      try {
        await Api.progresoTarea(Number(form.idTarea.value), {
          estado: form.estado.value,
          evidencia: form.evidencia.value.trim() || null,
          comentarioEmpleado: form.comentarioEmpleado.value.trim() || null
        });
        cerrarModal('modalProgresoTarea');
        mostrarToast(
          form.estado.value === 'completada'
            ? 'Entrega enviada y archivada. Queda en espera de revisión del administrador.'
            : 'Avance reportado correctamente.',
          'ok'
        );
        await cargarMis();
      } catch (error) {
        if (resp) { resp.textContent = error.message; resp.className = 'form-respuesta error campo--full'; }
      }
    });
  }

  await cargarMis();
}

// Abre la ventana de avance de una tarea del empleado, bloqueando edición si ya está en revisión o aprobada
function abrirProgresoTarea(idTarea) {
  const form = document.getElementById('formProgresoTarea');
  const tarea = cacheMisTareas.find((t) => t.idTarea === idTarea);
  if (!form || !tarea) return;

  const est = (tarea.estado || 'pendiente').toLowerCase();
  const bloqueada = est === 'completada' || est === 'revisada';
  form.dataset.bloqueada = bloqueada ? 'true' : 'false';

  form.idTarea.value = tarea.idTarea;
  form.estado.value = (est === 'revisada') ? 'completada' : (est || 'en_progreso');
  form.evidencia.value = tarea.evidencia || '';
  form.comentarioEmpleado.value = tarea.comentarioEmpleado || '';

  const inputArchivo = document.getElementById('archivoEvidenciaTarea');
  const estadoSubida = document.getElementById('estadoSubidaEvidencia');
  const btnGuardar = document.getElementById('btnGuardarProgresoTarea') || form.querySelector('button[type="submit"]');
  const avisoBloqueo = document.getElementById('avisoBloqueoTarea');

  if (inputArchivo) inputArchivo.value = '';
  if (estadoSubida) estadoSubida.innerHTML = tarea.evidencia ? `Actual: ${construirHtmlEvidencia(tarea.evidencia)}` : '';

  // Bloqueo completo de controles cuando está completada (en espera de revisión) o revisada
  form.estado.disabled = bloqueada;
  form.evidencia.disabled = bloqueada;
  form.comentarioEmpleado.disabled = bloqueada;
  if (inputArchivo) inputArchivo.disabled = bloqueada;
  if (btnGuardar) {
    btnGuardar.disabled = bloqueada;
    btnGuardar.style.display = bloqueada ? 'none' : '';
  }

  if (avisoBloqueo) {
    if (est === 'completada') {
      avisoBloqueo.style.display = 'block';
      avisoBloqueo.innerHTML = '<i class="fa-solid fa-lock"></i> <strong>Tarea bloqueada en espera de revisión:</strong> Ya marcaste esta tarea como completada. No podrás modificarla ni subir nuevos archivos a menos que el administrador la devuelva para corrección.';
    } else if (est === 'revisada') {
      avisoBloqueo.style.display = 'block';
      avisoBloqueo.innerHTML = '<i class="fa-solid fa-circle-check"></i> <strong>Tarea aprobada y cerrada:</strong> El administrador ya revisó y calificó esta entrega.';
    } else {
      avisoBloqueo.style.display = 'none';
    }
  }

  const feedback = document.getElementById('adminFeedbackBox');
  if (feedback) {
    if (tarea.observacionAdmin || tarea.calificacion) {
      const prefijo = est === 'en_progreso'
        ? '<i class="fa-solid fa-rotate-left"></i> <strong>Devuelta para corrección por el administrador:</strong> '
        : '<strong>Revisión administrativa:</strong> ';
      feedback.style.display = 'block';
      feedback.innerHTML = `${prefijo}${tarea.calificacion ? `Calificación ${tarea.calificacion}/5. ` : ''}${escaparHtml(tarea.observacionAdmin || '')}`;
    } else {
      feedback.style.display = 'none';
    }
  }

  const titulo = document.getElementById('tituloModalProgreso');
  if (titulo) titulo.innerHTML = `<i class="fa-solid fa-check-to-slot"></i> ${escaparHtml(tarea.titulo)}`;
  const resp = document.getElementById('respuestaProgresoModal');
  if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
  abrirModal('modalProgresoTarea');
  cargarHistorialEnContenedor(tarea.idTarea, 'historialEntregasEmpleado');
}

/* ------------------------ Venta manual (empleado) ---------------------- */

// Permite a un empleado registrar una venta presencial o telefónica descontando stock directamente
async function inicializarVentaManual() {
  const form = document.getElementById('formVentaManual');
  const selectProd = document.getElementById('ventaProductoId');
  const resp = document.getElementById('respuestaVentaManual');
  if (!form || !selectProd) return;

  // Llena el selector solo con productos activos que tengan unidades disponibles en inventario
  try {
    const res = await Api.productos({ tamanio: 100 });
    const lista = (res.contenido || []).filter((p) => Number(p.stock || 0) > 0 && p.activo !== false);
    selectProd.innerHTML = lista.length
      ? lista.map((p) => `<option value="${p.idProducto}">${escaparHtml(p.nombre)} — ${textoImporte(p.precioFinal)} (Stock: ${p.stock})</option>`).join('')
      : '<option value="">Sin productos con stock disponible</option>';
  } catch (error) {
    selectProd.innerHTML = `<option value="">${escaparHtml(error.message)}</option>`;
  }

  // Envía la venta manual al servidor y muestra el número de pedido y total cobrado
  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    if (resp) { resp.textContent = ''; resp.className = 'form-respuesta campo--full'; }
    const idProducto = Number(form.idProducto.value);
    const cantidad = Number(form.cantidad.value || 1);
    if (!idProducto || cantidad < 1) {
      if (resp) { resp.textContent = 'Selecciona un producto y cantidad válidos.'; resp.className = 'form-respuesta error campo--full'; }
      return;
    }
    try {
      const pedido = await Api.registrarPedido({
        nombre: form.nombre.value.trim(),
        correo: form.correo.value.trim(),
        telefono: form.telefono.value.trim(),
        direccion: form.direccion.value.trim(),
        metodoPago: form.metodoPago.value,
        detallePago: form.detallePago.value.trim() || null,
        carrito: [{ idProducto, cantidad }]
      });
      mostrarToast(`Venta #${pedido.idPedido} registrada (${textoImporte(pedido.total)}).`, 'ok');
      if (resp) {
        resp.textContent = `Pedido #${pedido.idPedido} registrado por ${textoImporte(pedido.total)} (${form.metodoPago.value}).`;
        resp.className = 'form-respuesta ok campo--full';
      }
      form.reset();
    } catch (error) {
      if (resp) { resp.textContent = error.message; resp.className = 'form-respuesta error campo--full'; }
    }
  });
}

/* -------------------------------- Arranque ----------------------------- */

// Detecta qué pantalla del panel está abierta y qué rol tiene el usuario para encender solo el módulo necesario
document.addEventListener('DOMContentLoaded', () => {
  // Si la URL es una carpeta (/panel/administrador o /panel/administrador/)
  // el navegador muestra su index.html, asi que se usa ese nombre.
  const partes = window.location.pathname.split('/').filter(Boolean);
  const ultima = partes[partes.length - 1] || '';
  const pagina = ultima.includes('.') ? ultima : 'index.html';
  const rol = document.body.dataset.rol;

  if (pagina === 'index.html') {
    if (rol === 'administrador') cargarDashboardAdmin();
    if (rol === 'empleado') cargarDashboardEmpleado();
    if (rol === 'cliente') cargarDashboardCliente();
  }
  if (pagina === 'usuarios.html' || pagina === 'clientes.html' || pagina === 'empleados.html') {
    inicializarUsuarios();
    if (pagina === 'empleados.html') inicializarPermisosEmpleados();
  }
  if (pagina === 'tareas.html') {
    if (rol === 'administrador') inicializarTareasAdmin();
    else if (rol === 'empleado') inicializarMisTareas();
  }
  if (pagina === 'productos.html') inicializarProductos();
  if (pagina === 'pedidos.html') {
    if (rol === 'cliente') cargarMisPedidos();
    else inicializarPedidos();
  }
  if (pagina === 'ventas.html') inicializarVentaManual();
  if (pagina === 'inventario.html') inicializarInventario();
  if (pagina === 'reportes.html') cargarReportes();
  if (pagina === 'soporte.html') inicializarSoporte();
  if (pagina === 'chat.html' && rol !== 'cliente') cargarListaChats();
  if (pagina === 'favoritos.html') cargarFavoritos();
  if (pagina === 'perfil.html') cargarPerfil();
  if (pagina === 'auditoria.html') inicializarAuditoria();
  if (pagina === 'facturacion.html') inicializarFacturacion();
  if (pagina === 'respaldo.html') configurarRespaldo();
});
