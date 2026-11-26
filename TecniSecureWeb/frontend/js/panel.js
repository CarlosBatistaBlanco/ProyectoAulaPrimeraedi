/**
 * Este archivo concentra las funciones compartidas por los paneles internos de clientes, empleados y
 * administradores. Controla el menú lateral en celulares, revisa que cada persona entre solo a las
 * pantallas permitidas para su tipo de cuenta, muestra el reloj en vivo, maneja la campana de alertas
 * y mantiene el chat de soporte actualizado al instante sin recargar la página.
 */

// Espera a que termine de cargar cualquier pantalla del panel interno para activar sus funciones comunes
document.addEventListener('DOMContentLoaded', () => {
  normalizarEnlacesDeCarpeta();
  marcarMenuActivo();
  inicializarMenuLateral();
  inicializarRelojEnVivo();
  inicializarNotificaciones();
  inicializarChat();
  pintarUsuarioPanel();
  protegerPanel();
  aplicarPermisosEmpleado();
});

// Resalta en el menú lateral la sección en la que se encuentra la persona actualmente
function marcarMenuActivo() {
  const partes = window.location.pathname.split('/').filter(Boolean);
  const ultima = partes[partes.length - 1] || '';
  // Si la dirección apunta a la carpeta del panel sin nombre de archivo, corresponde a la portada del panel
  const paginaActual = ultima.includes('.') ? ultima : 'index.html';

  document.querySelectorAll('.panel-menu__item').forEach((enlace) => {
    const href = enlace.getAttribute('href') || '';
    const archivo = href.split(/[?#]/)[0].split('/').pop() || 'index.html';
    enlace.classList.toggle('activo', archivo === paginaActual);
    if (archivo === paginaActual) {
      enlace.setAttribute('aria-current', 'page');
    } else {
      enlace.removeAttribute('aria-current');
    }
  });
}

// Ajusta los enlaces internos si la persona abrió la carpeta del panel sin escribir la barra final
function normalizarEnlacesDeCarpeta() {
  const ruta = window.location.pathname;
  if (!ruta.endsWith('/') && !ruta.split('/').pop().includes('.')) {
    const base = document.querySelector('base') || document.createElement('base');
    base.href = ruta + '/';
    if (!base.parentNode) document.head.prepend(base);
  }
}

// Revisa que la persona haya iniciado sesión y que entre únicamente al panel de su rol (cliente, empleado o admin)
function protegerPanel() {
  if (!Api.estaAutenticado()) {
    window.location.href = '../../login.html?redir=' + encodeURIComponent(window.location.pathname);
    return;
  }
  const rolRequerido = document.body.dataset.rol;
  const usuario = Api.getUsuario();
  const rolActual = (usuario && usuario.rol || '').toLowerCase();
  if (rolRequerido && rolActual && rolRequerido !== rolActual) {
    // Si alguien intenta entrar al panel de otro rol, lo devuelve al inicio de su propio panel
    window.location.href = `../${rolActual}/index.html`;
  }
}

// Oculta del menú del empleado las secciones que el administrador no le autorizó y bloquea el acceso directo
async function aplicarPermisosEmpleado() {
  const usuario = Api.getUsuario();
  if (!usuario || (usuario.rol || '').toLowerCase() !== 'empleado') return;
  try {
    const perfil = await Api.miPerfilEmpleado();
    const permitidos = new Set(perfil.modulos || []);
    document.querySelectorAll('[data-rol-usuario]').forEach((el) => {
      el.textContent = `empleado · ${perfil.area || 'OPERATIVO'}`;
    });
    const badgeArea = document.getElementById('miAreaBadge');
    if (badgeArea) badgeArea.textContent = perfil.area || 'OPERATIVO';

    // Esconde los botones del menú lateral que no pertenecen a los permisos del empleado
    document.querySelectorAll('[data-modulo]').forEach((el) => {
      const mod = el.dataset.modulo;
      if (mod && !permitidos.has(mod) && !['perfil', 'direccion'].includes(mod)) {
        el.style.display = 'none';
      }
    });

    const archivo = (window.location.pathname.split('/').pop() || 'index.html').replace('.html', '');
    const moduloActual = archivo === 'index' ? 'dashboard' : archivo;
    // El perfil y las direcciones son datos personales, así que siempre se pueden abrir
    const siemprePermitidos = ['dashboard', 'tareas', 'perfil', 'direccion', 'direcciones'];
    if (moduloActual && !permitidos.has(moduloActual) && !siemprePermitidos.includes(moduloActual)) {
      window.location.href = 'index.html';
    }
  } catch {
    /* si falla la consulta puntual, conserva el menú visible por defecto */
  }
}

/* --------------------- Avisos flotantes ---------------------------- */

// Muestra un aviso temporal en una esquina de la pantalla (éxito, error, advertencia o información) y desaparece solo
function mostrarToast(mensaje, tipo = 'info') {
  let wrap = document.getElementById('panelToastWrap');
  if (!wrap) {
    wrap = document.createElement('div');
    wrap.id = 'panelToastWrap';
    wrap.className = 'panel-toast-wrap';
    document.body.appendChild(wrap);
  }
  const iconos = {
    ok: 'fa-circle-check',
    error: 'fa-circle-exclamation',
    aviso: 'fa-triangle-exclamation',
    info: 'fa-circle-info'
  };
  const toast = document.createElement('div');
  toast.className = `panel-toast panel-toast--${tipo}`;
  toast.innerHTML = `<i class="fa-solid ${iconos[tipo] || iconos.info}"></i><span>${escaparHtml(mensaje)}</span>`;
  wrap.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transition = 'opacity .25s ease';
    setTimeout(() => toast.remove(), 260);
  }, 3600);
}

// Abre una ventana de confirmación ("Cancelar" / "Confirmar") antes de realizar acciones delicadas
function confirmarAccion(titulo, mensaje) {
  return new Promise((resolve) => {
    let modal = document.getElementById('modalConfirmarGlobal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'modalConfirmarGlobal';
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-caja">
          <h3 id="confirmTitulo"><i class="fa-solid fa-circle-question"></i> Confirmar</h3>
          <p id="confirmMensaje" style="color:var(--texto-tenue); margin: 10px 0 20px;"></p>
          <div style="display:flex; justify-content:flex-end; gap:10px;">
            <button type="button" class="btn btn--secundario" id="confirmBtnNo">Cancelar</button>
            <button type="button" class="btn btn--primario" id="confirmBtnSi">Confirmar</button>
          </div>
        </div>`;
      document.body.appendChild(modal);
    }
    document.getElementById('confirmTitulo').innerHTML = `<i class="fa-solid fa-circle-question"></i> ${escaparHtml(titulo)}`;
    document.getElementById('confirmMensaje').textContent = mensaje;
    modal.classList.add('activo');

    const cerrar = (valor) => {
      modal.classList.remove('activo');
      resolve(valor);
    };
    document.getElementById('confirmBtnNo').onclick = () => cerrar(false);
    document.getElementById('confirmBtnSi').onclick = () => cerrar(true);
  });
}

// Comparte los avisos flotantes y ventanas de confirmación con las demás pantallas del panel
window.mostrarToast = mostrarToast;
window.confirmarAccion = confirmarAccion;

/* --------------------------- Menú lateral -------------------------- */

// Permite desplegar y cerrar el menú lateral del panel en pantallas de celular o tablet
function inicializarMenuLateral() {
  const toggle = document.getElementById('panelToggle');
  const sidebar = document.getElementById('panelSidebar');
  const overlay = document.getElementById('panelOverlay');

  const abrir = () => {
    sidebar?.classList.add('activo');
    overlay?.classList.add('activo');
  };
  const cerrar = () => {
    sidebar?.classList.remove('activo');
    overlay?.classList.remove('activo');
  };

  if (toggle) toggle.addEventListener('click', abrir);
  if (overlay) overlay.addEventListener('click', cerrar);
}

/* ------------------------ Datos del usuario ------------------------ */

// Muestra el nombre, correo, rol y foto (o inicial) de la persona conectada en la parte superior del panel
function pintarUsuarioPanel() {
  const usuario = Api.getUsuario();
  if (!usuario) return;

  document.querySelectorAll('[data-nombre-usuario]').forEach((el) => { el.textContent = usuario.nombre || ''; });
  document.querySelectorAll('[data-correo-usuario]').forEach((el) => { el.textContent = usuario.correo || ''; });
  document.querySelectorAll('[data-rol-usuario]').forEach((el) => { el.textContent = usuario.rol || ''; });
  document.querySelectorAll('[data-avatar-usuario]').forEach((el) => {
    if (usuario.avatar) {
      el.innerHTML = `<img src="${escaparHtml(usuario.avatar)}" alt="Foto de perfil">`;
    } else {
      el.textContent = (usuario.nombre || '?').charAt(0).toUpperCase();
    }
  });
}

/* -------------------------- Reloj y Fechas ------------------------- */

// Interpreta una fecha enviada por el servidor (como texto o lista de números) para poder mostrarla bien
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

// Convierte una fecha en texto fácil de leer al estilo colombiano (dd/mm/aaaa, hh:mm)
function formatearFechaPanel(valor) {
  const d = parsearFechaPanel(valor);
  if (!d) return 'Reciente';
  return d.toLocaleString('es-CO', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

// Coloca un reloj con fecha y hora actualizadas segundo a segundo en la barra superior del panel
function inicializarRelojEnVivo() {
  const contUsuario = document.querySelector('.panel-topbar__usuario');
  if (!contUsuario || document.getElementById('panelRelojVivo')) return;

  const reloj = document.createElement('div');
  reloj.id = 'panelRelojVivo';
  reloj.className = 'panel-reloj';
  reloj.title = 'Fecha y hora en tiempo real';
  reloj.innerHTML = '<i class="fa-regular fa-clock"></i><span></span>';
  contUsuario.insertBefore(reloj, contUsuario.firstChild);

  const texto = reloj.querySelector('span');
  const actualizar = () => {
    const ahora = new Date();
    const fecha = ahora.toLocaleDateString('es-CO', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
    const hora = ahora.toLocaleTimeString('es-CO', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    texto.textContent = `${fecha} · ${hora}`;
  };

  actualizar();
  setInterval(actualizar, 1000);
}

/* -------------------------- Notificaciones ------------------------- */

// Decide a qué pantalla del panel llevar a la persona cuando hace clic sobre una notificación de la campana
function destinoNotificacion(n) {
  if (n && n.enlace) {
    const limpio = String(n.enlace).trim();
    if (limpio) {
      if (limpio.startsWith('/panel/')) {
        const archivo = limpio.split('/').pop();
        if (archivo) return archivo.includes('.html') ? archivo : `${archivo}.html`;
      }
      if (/^[\w-]+\.html([?#].*)?$/i.test(limpio)) return limpio;
    }
  }
  const texto = `${n.titulo || ''} ${n.mensaje || ''} ${n.tipo || ''}`.toLowerCase();
  if (texto.includes('chat') || texto.includes('mensaje')) return 'chat.html';
  if (texto.includes('solicitud') || texto.includes('soporte') || texto.includes('contacto')) return 'soporte.html';
  if (texto.includes('stock') || texto.includes('inventario')) return 'inventario.html';
  if (texto.includes('pedido') || texto.includes('compra')) return 'pedidos.html';
  if (texto.includes('tarea')) return 'tareas.html';
  return null;
}

// Configura la campana de alertas en la barra superior: cuenta avisos sin leer, abre el listado y escucha avisos en vivo
async function inicializarNotificaciones() {
  const boton = document.getElementById('botonNotificaciones');
  const dropdown = document.getElementById('dropdownNotificaciones');
  const contador = document.getElementById('notificacionesContador');
  const lista = document.getElementById('listaNotificaciones');

  if (!Api.estaAutenticado() || !boton) return;

  // Trae las notificaciones del servidor, actualiza el número de la campana y muestra cada aviso
  async function refrescar() {
    try {
      const [sinLeer, notificaciones] = await Promise.all([
        Api.notificacionesSinLeer(),
        Api.notificaciones()
      ]);

      if (contador) {
        contador.textContent = sinLeer.total || 0;
        contador.style.display = sinLeer.total > 0 ? 'flex' : 'none';
      }

      if (lista) {
        lista.innerHTML = notificaciones.length
          ? notificaciones.map((n, idx) => `
              <li class="panel-noti-item ${n.leida ? '' : 'no-leida'}" data-noti-idx="${idx}" style="cursor:pointer;">
                <strong>${escaparHtml(n.titulo)}</strong>
                <span>${escaparHtml(n.mensaje || '')}</span>
                <small>${formatearFechaPanel(n.creadoEn)}</small>
              </li>`).join('')
          : '<li class="vacio">No tienes notificaciones.</li>';

        // Al hacer clic en un aviso, lo marca como leído y abre la pantalla correspondiente (pedido, chat, tarea, etc.)
        lista.querySelectorAll('[data-noti-idx]').forEach((li) => {
          li.addEventListener('click', async () => {
            const n = notificaciones[Number(li.dataset.notiIdx)];
            try { await Api.marcarNotificacionesLeidas(); } catch {}
            const dest = destinoNotificacion(n);
            if (dest) {
              window.location.href = dest;
            } else {
              mostrarToast(`${n.titulo}: ${n.mensaje || ''}`, 'info');
              dropdown?.classList.remove('activo');
            }
          });
        });
      }
    } catch {
      /* la campana es informativa: si falla no interrumpe el uso del panel */
    }
  }

  // Abre o cierra la lista desplegable al hacer clic en la campana
  boton.addEventListener('click', async (e) => {
    e.stopPropagation();
    dropdown?.classList.toggle('activo');
    if (dropdown?.classList.contains('activo')) await refrescar();
  });

  // Cierra la lista desplegable al hacer clic fuera de ella
  document.addEventListener('click', (e) => {
    if (dropdown && !dropdown.contains(e.target) && !boton.contains(e.target)) {
      dropdown.classList.remove('activo');
    }
  });

  // Botón "Marcar todas como leídas" dentro del menú de notificaciones
  const marcar = document.getElementById('marcarNotificaciones');
  if (marcar) {
    marcar.addEventListener('click', async () => {
      await Api.marcarNotificacionesLeidas();
      refrescar();
    });
  }

  // Consulta al iniciar y refresca de respaldo cada minuto
  refrescar();
  setInterval(refrescar, 60000);

  // Escucha avisos instantáneos del servidor para actualizar la campana sin recargar la página
  const idUsuario = Api.getUsuario()?.idUsuario;
  if (idUsuario) {
    let pendiente = null;
    Ws.suscribir(`/topic/notificaciones/${idUsuario}`, () => {
      clearTimeout(pendiente);
      pendiente = setTimeout(refrescar, 400);
    });
  }
}

/* ------------------------------- Chat ------------------------------ */

// Muestra en pantalla las burbujas de mensajes diferenciando si escribió el cliente, el asistente virtual o un asesor
function renderizarBurbujasChat(contenedor, mensajes) {
  if (!mensajes || !mensajes.length) {
    contenedor.innerHTML = '<p style="color:var(--texto-tenue); padding:16px;">Aún no hay mensajes en esta conversación.</p>';
    return;
  }

  // Baja automáticamente al último mensaje solo si la persona ya estaba abajo o llegó un mensaje nuevo
  // Si subió a leer mensajes anteriores, respeta su posición
  const estabaAbajo =
    contenedor.scrollHeight - contenedor.scrollTop - contenedor.clientHeight < 60;
  const firma = mensajes.map((m) => `${m.idMensaje ?? ''}:${m.remitente ?? ''}`).join('|');
  const hayNuevos = firma !== contenedor.dataset.ultimaFirma;

  contenedor.dataset.ultimaFirma = firma;

  contenedor.innerHTML = mensajes.map((m) => {
    const esVisitante = m.remitente === 'visitante';
    const esCliente = m.remitente === 'cliente' || esVisitante;
    const esBot = m.remitente === 'bot'
      || (!esCliente && typeof m.mensaje === 'string' && m.mensaje.trim().startsWith('[Asistente Virtual]'));
    // Distingue entre una respuesta automática del asistente y un mensaje escrito por un asesor humano
    const esBotSistema = esBot && m.remitente !== 'asesor';
    const clase = esCliente ? 'chat-burbuja--cliente' : (esBotSistema ? 'chat-burbuja--bot' : 'chat-burbuja--asesor');
    const etiqueta = esVisitante ? 'Visitante' : (esCliente ? 'Cliente' : (esBotSistema ? 'Asistente Virtual' : 'Asesor Técnico'));
    const fecha = m.creadoEn
      ? new Date(m.creadoEn).toLocaleString('es-CO', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
      : '';
    return `
      <div class="chat-burbuja ${clase}">
        <span class="chat-burbuja__meta">${etiqueta}</span>
        <div class="chat-burbuja__texto">${escaparHtml(m.mensaje || '')}</div>
        <small>${fecha}</small>
      </div>`;
  }).join('');

  if (estabaAbajo || hayNuevos) {
    contenedor.scrollTop = contenedor.scrollHeight;
  }
}

// Activa el chat instantáneo dentro del panel (tanto para clientes como para asesores atendiendo conversaciones)
function inicializarChat() {
  const form = document.getElementById('formChat');
  const contenedor = document.getElementById('chatMensajes');
  if (!contenedor) return;

  const esPanelCliente = contenedor.dataset.rol === 'cliente';
  const idCliente = contenedor.dataset.cliente;
  const idHilo = esPanelCliente ? Api.getUsuario()?.idCliente : idCliente;
  // Un código que empieza por "cv-" identifica a un visitante de la tienda; un número normal es un cliente registrado
  const esVisita = !esPanelCliente && /^cv-\d+$/.test(String(idHilo || ''));
  const idVisita = esVisita ? String(idHilo).slice(3) : null;

  // Detiene cualquier actualización de una conversación anterior antes de abrir la nueva
  if (window.__chatTimer) {
    clearInterval(window.__chatTimer);
    window.__chatTimer = null;
  }

  if (!idHilo) {
    if (!esPanelCliente) {
      contenedor.innerHTML = '<p style="color:var(--texto-tenue); padding:16px;">Selecciona un cliente de la lista para ver y responder la conversación.</p>';
    }
    return;
  }

  contenedor.dataset.hiloActivo = String(idHilo);

  // Trae los mensajes de la conversación activa desde el servidor y actualiza las burbujas en pantalla
  const cargar = async () => {
    const clienteObjetivo = esPanelCliente ? Api.getUsuario()?.idCliente : contenedor.dataset.cliente;
    if (!clienteObjetivo || String(clienteObjetivo) !== String(idHilo)) return;
    try {
      let mensajes;
      if (esPanelCliente) {
        mensajes = await Api.miConversacion();
      } else if (esVisita) {
        mensajes = await Api.conversacionVisita(idVisita);
      } else {
        mensajes = await Api.conversacion(clienteObjetivo);
      }
      if (String(contenedor.dataset.hiloActivo) === String(idHilo)) {
        renderizarBurbujasChat(contenedor, mensajes || []);
      }
    } catch (error) {
      contenedor.innerHTML = `<p style="color:var(--naranja); padding:12px;">${escaparHtml(error.message)}</p>`;
    }
  };

  // Carga los mensajes al abrir y revisa cada 4 segundos como respaldo
  cargar();
  window.__chatTimer = setInterval(cargar, 4000);

  // Se conecta al canal instantáneo para mostrar mensajes nuevos al momento sin esperar la revisión
  if (typeof Ws !== 'undefined' && idHilo) {
    const canal = esVisita ? `/topic/visita/${idVisita}` : `/topic/chat/${idHilo}`;
    Ws.suscribir(canal, () => {
      if (String(contenedor.dataset.hiloActivo) === String(idHilo)) cargar();
    });
  }

  // Detecta cuando la persona envía un mensaje presionando Enter o haciendo clic en Enviar
  if (form && !form.dataset.submitBind) {
    form.dataset.submitBind = '1';
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const input = document.getElementById('mensajeChat');
      const texto = (input?.value || '').trim();
      if (!texto) return;

      const clienteActual = contenedor.dataset.cliente;
      if (!esPanelCliente && !clienteActual) {
        mostrarToast('Selecciona primero un cliente para responder.', 'error');
        return;
      }

      input.value = '';
      try {
        if (esPanelCliente) {
          await Api.enviarMensajePropio(texto);
        } else if (esVisita) {
          await Api.responderVisita(idVisita, texto);
        } else {
          await Api.responderChat(clienteActual, texto);
        }
        let mensajes;
        if (esPanelCliente) {
          mensajes = await Api.miConversacion();
        } else if (esVisita) {
          mensajes = await Api.conversacionVisita(idVisita);
        } else {
          mensajes = await Api.conversacion(clienteActual);
        }
        renderizarBurbujasChat(contenedor, mensajes || []);
        if (typeof cargarListaChats === 'function' && !esPanelCliente) {
          cargarListaChats();
        }
      } catch (error) {
        input.value = texto;
        mostrarToast(error.message, 'error');
      }
    });
  }
}

// Permite enviar una respuesta o pregunta rápida con un solo clic en el chat del panel
window.enviarChipChat = function (texto) {
  const input = document.getElementById('mensajeChat');
  const form = document.getElementById('formChat');
  if (!input || !form) return;
  input.value = texto;
  form.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
};

/* ---------------------------- Cerrar sesión ------------------------ */

// Cierra la sesión activa desde el panel y devuelve a la persona a la pantalla de inicio de sesión
function cerrarSesionPanel() {
  Api.cerrarSesion();
  window.location.href = '../../login.html';
}
