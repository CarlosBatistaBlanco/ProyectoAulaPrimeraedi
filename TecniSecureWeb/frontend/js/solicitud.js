/**
 * Este archivo maneja el formulario donde los clientes piden una cotización o agendan una cita técnica.
 * Su efecto se nota en la pantalla de solicitud de servicio: muestra el título y la descripción del servicio
 * elegido, rellena automáticamente los datos del cliente si ya inició sesión, valida la fecha de la cita
 * y envía la solicitud al equipo de asesores.
 */

// Relación entre el nombre clave del servicio y el ícono visual que se muestra en pantalla
const ICONOS_SERVICIO = {
  camera: 'fa-solid fa-video',
  monitor: 'fa-solid fa-display',
  bell: 'fa-solid fa-bell',
  lock: 'fa-solid fa-fingerprint',
  wrench: 'fa-solid fa-screwdriver-wrench',
  clipboard: 'fa-solid fa-clipboard-list'
};

// Busca en el servidor qué servicio eligió el cliente (según el número en la dirección web) y muestra su título y descripción
async function cargarServicio() {
  const id = Number(new URLSearchParams(window.location.search).get('id') || 0);
  const titulo = document.getElementById('tituloServicio');
  const descripcion = document.getElementById('descripcionServicio');
  if (!titulo || !id) return null;

  try {
    const servicios = await Api.servicios();
    const servicio = servicios.find((s) => Number(s.idServicio) === id);
    // Si el servicio no existe, devuelve al cliente a la lista general de servicios
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

// Prepara el formulario de cotización o cita técnica y controla el envío al servidor
function inicializarSolicitud() {
  const form = document.getElementById('formSolicitud');
  if (!form) return;

  // Atajos seguros para leer y limpiar el texto escrito en cada casilla del formulario
  const campo = (nombre) => form.elements[nombre];
  const valor = (nombre) => (campo(nombre) ? campo(nombre).value.trim() : '');

  // Si el cliente ya inició sesión, rellena automáticamente su nombre, correo y teléfono para ahorrarle tiempo
  const usuario = Api.getUsuario();
  if (usuario) {
    if (campo('nombre') && !campo('nombre').value) campo('nombre').value = usuario.nombre || '';
    if (campo('correo') && !campo('correo').value) campo('correo').value = usuario.correo || '';
    if (campo('telefono') && !campo('telefono').value) campo('telefono').value = usuario.telefono || '';
  }

  // Impide elegir fechas pasadas en el calendario de la cita técnica
  const fecha = document.getElementById('fecha_preferida');
  if (fecha) fecha.min = new Date().toISOString().split('T')[0];

  cargarServicio();

  const idServicio = Number(new URLSearchParams(window.location.search).get('id') || 0);

  // Detecta cuando el cliente presiona el botón de enviar solicitud
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Limpia cualquier aviso anterior antes de volver a revisar los datos
    const alertaPrevia = form.parentElement.querySelector('.auth-alerta');
    if (alertaPrevia) alertaPrevia.remove();

    // Desactiva el botón y muestra un ícono girando mientras se envía la solicitud
    const boton = form.querySelector('button[type="submit"]');
    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Enviando...';
    boton.disabled = true;

    // Reúne todos los datos escritos por el cliente en el formulario
    const datos = {
      idServicio,
      nombre: valor('nombre'),
      correo: valor('correo'),
      telefono: valor('telefono'),
      direccion: valor('direccion'),
      tipo: valor('tipo'),
      descripcion: valor('descripcion')
    };

    // Si el cliente pide cita técnica, revisa que haya elegido una fecha
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

    // Revisa que la página sepa exactamente qué servicio se está solicitando
    if (!idServicio) {
      mostrarMensaje(form, 'No se identificó el servicio. Vuelve a elegirlo desde la página de servicios.', 'error');
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
      return;
    }

    // Envía la solicitud al servidor y muestra la confirmación o un mensaje si hubo algún problema
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

// Muestra un recuadro verde (éxito) o rojo (error) encima del formulario y mueve la vista hacia él
function mostrarMensaje(form, texto, tipo) {
  const aviso = document.createElement('div');
  aviso.className = `auth-alerta auth-alerta--${tipo === 'ok' ? 'ok' : 'error'}`;
  aviso.style.marginBottom = '20px';
  aviso.textContent = texto;
  form.parentElement.insertBefore(aviso, form);
  aviso.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

// Espera a que la página termine de cargar para preparar el formulario
document.addEventListener('DOMContentLoaded', inicializarSolicitud);
