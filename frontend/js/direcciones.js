/**
 * direcciones.js — CRUD de direcciones de entrega del usuario
 * -----------------------------------------------------------------
 * Se usa en el menu "Direccion" de los tres roles (administrador,
 * empleado y cliente). Las direcciones guardadas aqui rellenan el
 * checkout de la tienda cuando el usuario va a pagar.
 */

(function () {
  'use strict';

  let direcciones = [];
  let editandoId = null;

  const $ = (id) => document.getElementById(id);

  function escapar(texto) {
    return String(texto == null ? '' : texto)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function mostrarRespuesta(texto, tipo) {
    const el = $('respuestaDireccion');
    if (!el) return;
    el.textContent = texto || '';
    el.className = 'form-respuesta' + (tipo ? ' ' + tipo : '');
  }

  /* ------------------------------ Listado --------------------------- */

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

  function pintar() {
    const contenedor = $('listaDirecciones');
    if (!contenedor) return;

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

  function abrirFormulario(id) {
    const modal = $('modalDireccion');
    const form = $('formDireccion');
    if (!modal || !form) return;

    form.reset();
    mostrarRespuesta('');
    editandoId = id || null;

    const titulo = $('tituloModalDireccion');
    if (titulo) {
      titulo.innerHTML = editandoId
        ? '<i class="fa-solid fa-pen"></i> Editar dirección'
        : '<i class="fa-solid fa-location-dot"></i> Nueva dirección';
    }

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

  function cerrarFormulario() {
    $('modalDireccion')?.classList.remove('activo');
    editandoId = null;
  }

  async function guardar(e) {
    e.preventDefault();
    const form = $('formDireccion');
    const boton = $('guardarDireccion');
    if (!form || !boton) return;

    const etiqueta = form.elements.etiqueta.value.trim();
    const direccion = form.elements.direccion.value.trim();

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

  document.addEventListener('DOMContentLoaded', () => {
    if (!$('listaDirecciones')) return;

    $('btnNuevaDireccion')?.addEventListener('click', () => abrirFormulario(null));
    $('cerrarModalDireccion')?.addEventListener('click', cerrarFormulario);
    $('cancelarDireccion')?.addEventListener('click', cerrarFormulario);
    $('formDireccion')?.addEventListener('submit', guardar);
    $('modalDireccion')?.addEventListener('click', (e) => {
      if (e.target.id === 'modalDireccion') cerrarFormulario();
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && $('modalDireccion')?.classList.contains('activo')) {
        cerrarFormulario();
      }
    });

    cargar();
  });
})();
