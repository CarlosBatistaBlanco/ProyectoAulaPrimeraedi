/**
 * Este archivo permite a los usuarios ver, crear, editar, eliminar y marcar como principal sus direcciones
 * de entrega. Su efecto se nota en la sección "Dirección" de los paneles de cliente, empleado y administrador,
 * y las direcciones guardadas aquí aparecen listas para elegirse más rápido en la pantalla de pago de la tienda.
 */

// Bloque cerrado para manejar las direcciones sin interferir con otras funciones de la página
(function () {
  'use strict';

  // Lista de direcciones cargadas del usuario y número de la dirección que se está editando
  let direcciones = [];
  let editandoId = null;

  // Atajo rápido para buscar un elemento de la pantalla por su nombre
  const $ = (id) => document.getElementById(id);

  // Protege el texto del usuario para evitar código malicioso al mostrarlo en pantalla
  function escapar(texto) {
    return String(texto == null ? '' : texto)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // Muestra un mensaje breve de error o confirmación dentro de la ventana de dirección
  function mostrarRespuesta(texto, tipo) {
    const el = $('respuestaDireccion');
    if (!el) return;
    el.textContent = texto || '';
    el.className = 'form-respuesta' + (tipo ? ' ' + tipo : '');
  }

  /* ------------------------------ Listado --------------------------- */

  // Pide al servidor todas las direcciones guardadas por el usuario y las muestra en pantalla
  async function cargar() {
    const contenedor = $('listaDirecciones');
    if (!contenedor) return;

    try {
      direcciones = await Api.misDirecciones() || [];
      pintar();
    } catch (error) {
      contenedor.innerHTML = `<p class="cargando">${escapar(error.message)}</p>`;
    }
  }

  // Dibuja en pantalla las tarjetas de cada dirección con sus botones de Editar, Eliminar y Predeterminar
  function pintar() {
    const contenedor = $('listaDirecciones');
    if (!contenedor) return;

    // Si el usuario aún no ha registrado direcciones, muestra un mensaje amigable invitando a crear una
    if (!direcciones.length) {
      contenedor.innerHTML = `
        <div class="direccion-vacia">
          <i class="fa-solid fa-location-dot"></i>
          <p>Todavía no tienes direcciones guardadas.</p>
          <small>Agrega una para que el pago se complete más rápido.</small>
        </div>`;
      return;
    }

    contenedor.innerHTML = direcciones.map((d) => {
      const detalles = [d.ciudad, d.barrio].filter(Boolean).join(' · ');
      return `
      <article class="direccion-card ${d.predeterminada ? 'direccion-card--principal' : ''}">
        <header class="direccion-card__cabecera">
          <h3><i class="fa-solid fa-location-dot"></i> ${escapar(d.etiqueta)}</h3>
          ${d.predeterminada
            ? '<span class="direccion-badge"><i class="fa-solid fa-star"></i> Predeterminada</span>'
            : `<button type="button" class="direccion-card__pred" data-pred="${d.idDireccion}"
                     title="Usar como predeterminada">
                 <i class="fa-regular fa-star"></i> Predeterminar
               </button>`}
        </header>
        <p class="direccion-card__texto">${escapar(d.direccion)}</p>
        ${detalles ? `<p class="direccion-card__detalle">${escapar(detalles)}</p>` : ''}
        ${d.nombreRecibe ? `<p class="direccion-card__detalle"><i class="fa-solid fa-user"></i> ${escapar(d.nombreRecibe)}</p>` : ''}
        ${d.telefono ? `<p class="direccion-card__detalle"><i class="fa-solid fa-phone"></i> ${escapar(d.telefono)}</p>` : ''}
        ${d.referencia ? `<p class="direccion-card__ref"><i class="fa-solid fa-circle-info"></i> ${escapar(d.referencia)}</p>` : ''}
        <footer class="direccion-card__acciones">
          <button type="button" class="direccion-btn" data-editar="${d.idDireccion}">
            <i class="fa-solid fa-pen"></i> Editar
          </button>
          <button type="button" class="direccion-btn direccion-btn--peligro" data-eliminar="${d.idDireccion}">
            <i class="fa-solid fa-trash"></i> Eliminar
          </button>
        </footer>
      </article>`;
    }).join('');

    // Detecta cuando el usuario hace clic en editar, eliminar o marcar como principal en cada tarjeta
    contenedor.querySelectorAll('[data-editar]').forEach((b) => {
      b.addEventListener('click', () => abrirFormulario(Number(b.dataset.editar)));
    });
    contenedor.querySelectorAll('[data-eliminar]').forEach((b) => {
      b.addEventListener('click', () => eliminar(Number(b.dataset.eliminar)));
    });
    contenedor.querySelectorAll('[data-pred]').forEach((b) => {
      b.addEventListener('click', () => predeterminar(Number(b.dataset.pred)));
    });
  }

  /* ---------------------------- Formulario -------------------------- */

  // Abre la ventana emergente para crear una dirección nueva o editar los datos de una existente
  function abrirFormulario(id) {
    const modal = $('modalDireccion');
    const form = $('formDireccion');
    if (!modal || !form) return;

    form.reset();
    mostrarRespuesta('');
    editandoId = id || null;

    // Ajusta el título de la ventana según si se va a editar o crear
    const titulo = $('tituloModalDireccion');
    if (titulo) {
      titulo.innerHTML = editandoId
        ? '<i class="fa-solid fa-pen"></i> Editar dirección'
        : '<i class="fa-solid fa-location-dot"></i> Nueva dirección';
    }

    // Si es edición, rellena las casillas del formulario con los datos guardados de esa dirección
    if (editandoId) {
      const d = direcciones.find((x) => Number(x.idDireccion) === Number(editandoId));
      if (d) {
        form.elements.etiqueta.value = d.etiqueta || '';
        form.elements.direccion.value = d.direccion || '';
        form.elements.ciudad.value = d.ciudad || '';
        form.elements.barrio.value = d.barrio || '';
        form.elements.nombreRecibe.value = d.nombreRecibe || '';
        form.elements.telefono.value = d.telefono || '';
        form.elements.referencia.value = d.referencia || '';
        form.elements.predeterminada.checked = Boolean(d.predeterminada);
      }
    }

    modal.classList.add('activo');
    setTimeout(() => form.elements.etiqueta?.focus(), 120);
  }

  // Oculta la ventana emergente de direcciones y reinicia el modo de edición
  function cerrarFormulario() {
    $('modalDireccion')?.classList.remove('activo');
    editandoId = null;
  }

  // Revisa los datos escritos por el usuario y guarda la dirección (nueva o editada) en el servidor
  async function guardar(e) {
    e.preventDefault();
    const form = $('formDireccion');
    const boton = $('guardarDireccion');
    if (!form || !boton) return;

    const etiqueta = form.elements.etiqueta.value.trim();
    const direccion = form.elements.direccion.value.trim();

    // Comprueba que el nombre y la dirección tengan información suficiente antes de enviar
    if (!etiqueta) {
      mostrarRespuesta('Ponle un nombre para reconocerla (Casa, Oficina…).', 'error');
      form.elements.etiqueta.focus();
      return;
    }
    if (direccion.length < 8) {
      mostrarRespuesta('La dirección debe tener al menos 8 caracteres.', 'error');
      form.elements.direccion.focus();
      return;
    }

    // Reúne todos los datos de la dirección para enviarlos al servidor
    const datos = {
      etiqueta,
      direccion,
      ciudad: form.elements.ciudad.value.trim() || null,
      barrio: form.elements.barrio.value.trim() || null,
      nombreRecibe: form.elements.nombreRecibe.value.trim() || null,
      telefono: form.elements.telefono.value.trim() || null,
      referencia: form.elements.referencia.value.trim() || null,
      predeterminada: form.elements.predeterminada.checked
    };

    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Guardando…';
    boton.disabled = true;

    try {
      if (editandoId) {
        await Api.actualizarDireccion(editandoId, datos);
        mostrarRespuesta('Dirección actualizada.', 'ok');
      } else {
        await Api.crearDireccion(datos);
        mostrarRespuesta('Dirección guardada.', 'ok');
      }
      await cargar();
      cerrarFormulario();
      if (window.mostrarToast) window.mostrarToast('Dirección guardada correctamente.', 'ok');
    } catch (error) {
      mostrarRespuesta(error.message, 'error');
    } finally {
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
    }
  }

  /* ------------------------------ Acciones -------------------------- */

  // Pide confirmación y elimina la dirección seleccionada de la cuenta del usuario
  async function eliminar(id) {
    const d = direcciones.find((x) => Number(x.idDireccion) === Number(id));
    if (!d) return;
    if (!window.confirm(`¿Eliminar la dirección "${d.etiqueta}"?`)) return;

    try {
      await Api.eliminarDireccion(id);
      await cargar();
      if (window.mostrarToast) window.mostrarToast('Dirección eliminada.', 'ok');
    } catch (error) {
      alert(error.message);
    }
  }

  // Marca una dirección como la principal para que aparezca elegida primero al momento de pagar
  async function predeterminar(id) {
    try {
      await Api.marcarDireccionPredeterminada(id);
      await cargar();
      if (window.mostrarToast) window.mostrarToast('Dirección predeterminada actualizada.', 'ok');
    } catch (error) {
      alert(error.message);
    }
  }

  /* ------------------------------ Arranque -------------------------- */

  // Espera a que la página termine de cargar para activar los botones y mostrar las direcciones guardadas
  document.addEventListener('DOMContentLoaded', () => {
    if (!$('listaDirecciones')) return;

    $('btnNuevaDireccion')?.addEventListener('click', () => abrirFormulario(null));
    $('cerrarModalDireccion')?.addEventListener('click', cerrarFormulario);
    $('cancelarDireccion')?.addEventListener('click', cerrarFormulario);
    $('formDireccion')?.addEventListener('submit', guardar);
    // Permite cerrar la ventana emergente haciendo clic en el fondo oscuro exterior
    $('modalDireccion')?.addEventListener('click', (e) => {
      if (e.target.id === 'modalDireccion') cerrarFormulario();
    });
    // Permite cerrar la ventana emergente presionando la tecla Escape
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && $('modalDireccion')?.classList.contains('activo')) {
        cerrarFormulario();
      }
    });

    cargar();
  });
})();
