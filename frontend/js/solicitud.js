/**
 * solicitud.js — Solicitud de cotización o cita técnica
 * -----------------------------------------------------------------
 * Solicitud de cotizaci?n o cita t?cnica:
 *   · carga el servicio desde GET /api/servicios (según ?id=)
 *   · permite elegir entre cotización y cita con fecha/hora
 *   · envía todo a POST /api/solicitudes
 */

const ICONOS_SERVICIO = {
  camera: 'fa-solid fa-video',
  monitor: 'fa-solid fa-display',
  bell: 'fa-solid fa-bell',
  lock: 'fa-solid fa-fingerprint',
  wrench: 'fa-solid fa-screwdriver-wrench',
  clipboard: 'fa-solid fa-clipboard-list'
};

async function cargarServicio() {
  const id = Number(new URLSearchParams(window.location.search).get('id') || 0);
  const titulo = document.getElementById('tituloServicio');
  const descripcion = document.getElementById('descripcionServicio');
  if (!titulo || !id) return null;

  try {
    const servicios = await Api.servicios();
    const servicio = servicios.find((s) => Number(s.idServicio) === id);
    if (!servicio) {
      window.location.href = '../index.html#servicios';
      return null;
    }
    titulo.textContent = `Solicitar: ${servicio.titulo}`;
    descripcion.textContent = servicio.descripcion;
    document.title = `Solicitar: ${servicio.titulo} | TECNISECURE`;
    return servicio;
  } catch {
    descripcion.textContent = 'No se pudo cargar la información del servicio.';
    return null;
  }
}

function inicializarSolicitud() {
  const form = document.getElementById('formSolicitud');
  if (!form) return;

  // Se usa form.elements: form.nombre / form.correo / form.direccion no
  // existen porque chocan con propiedades nativas del <form>.
  const campo = (nombre) => form.elements[nombre];
  const valor = (nombre) => (campo(nombre) ? campo(nombre).value.trim() : '');

  const usuario = Api.getUsuario();
  if (usuario) {
    if (campo('nombre') && !campo('nombre').value) campo('nombre').value = usuario.nombre || '';
    if (campo('correo') && !campo('correo').value) campo('correo').value = usuario.correo || '';
    if (campo('telefono') && !campo('telefono').value) campo('telefono').value = usuario.telefono || '';
  }

  // Fecha mínima: hoy
  const fecha = document.getElementById('fecha_preferida');
  if (fecha) fecha.min = new Date().toISOString().split('T')[0];

  cargarServicio();

  const idServicio = Number(new URLSearchParams(window.location.search).get('id') || 0);

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const alertaPrevia = form.parentElement.querySelector('.auth-alerta');
    if (alertaPrevia) alertaPrevia.remove();

    const boton = form.querySelector('button[type="submit"]');
    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Enviando...';
    boton.disabled = true;

    const datos = {
      idServicio,
      nombre: valor('nombre'),
      correo: valor('correo'),
      telefono: valor('telefono'),
      direccion: valor('direccion'),
      tipo: valor('tipo'),
      descripcion: valor('descripcion')
    };

    if (datos.tipo === 'cita') {
      datos.fechaPreferida = valor('fecha_preferida') || null;
      datos.horaPreferida = valor('hora_preferida') || null;
      if (!datos.fechaPreferida) {
        mostrarMensaje(form, 'Para agendar una cita indica la fecha preferida.', 'error');
        boton.innerHTML = textoOriginal;
        boton.disabled = false;
        return;
      }
    }

    if (!idServicio) {
      mostrarMensaje(form, 'No se identificó el servicio. Vuelve a elegirlo desde la página de servicios.', 'error');
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
      return;
    }

    try {
      await Api.solicitarServicio(datos);
      form.reset();
      mostrarMensaje(form,
        '¡Solicitud recibida! Un asesor se pondrá en contacto contigo muy pronto.', 'ok');
    } catch (error) {
      mostrarMensaje(form, error.message, 'error');
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
    }
  });
}

function mostrarMensaje(form, texto, tipo) {
  const aviso = document.createElement('div');
  aviso.className = `auth-alerta auth-alerta--${tipo === 'ok' ? 'ok' : 'error'}`;
  aviso.style.marginBottom = '20px';
  aviso.textContent = texto;
  form.parentElement.insertBefore(aviso, form);
  aviso.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

document.addEventListener('DOMContentLoaded', inicializarSolicitud);
