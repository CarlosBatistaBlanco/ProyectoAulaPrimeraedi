/**
 * asistente.js — Chat flotante del sitio publico
 * -----------------------------------------------------------------
 * Un mini recuadro (arriba del boton de WhatsApp) que:
 *   · responde automaticamente (preguntas frecuentes + catalogo local
 *     + IA de DeepSeek cuando esta configurada en el servidor);
 *   · si no hay solucion, o el visitante escribe "asesor", registra un
 *     caso y avisa al equipo (administradores y empleados);
 *   · guarda la conversacion en el navegador para no perderla al recargar.
 * Incluyelo al final del <body>; no usa dependencias externas.
 */

(function () {
  'use strict';

  const CLAVE_HISTORIAL = 'tecnicsecure_asistente_historial';
  const CLAVE_CONTACTO = 'tecnicsecure_asistente_contacto';
  const CLAVE_TOKEN = 'tecnicsecure_asistente_token';
  const MAX_MENSAJES = 60;
  const INTERVALO_HILO = 6000;

  const FRECUENTES = [
    '¿Que camaras tienen disponibles?',
    '¿Cuanto cuesta la instalacion?',
    '¿Que garantia tienen?',
    'Hablar con un asesor'
  ];

  let historial = cargarHistorial();
  let abierto = false;
  let enviando = false;
  let ultimoIdMensaje = 0;
  let temporizadorHilo = null;

  /* ------------------------------ Utilidades ------------------------- */

  function escapar(texto) {
    return String(texto == null ? '' : texto)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function cargarHistorial() {
    try {
      const guardado = JSON.parse(localStorage.getItem(CLAVE_HISTORIAL) || '[]');
      return Array.isArray(guardado) ? guardado : [];
    } catch {
      return [];
    }
  }

  function guardarHistorial() {
    try {
      localStorage.setItem(CLAVE_HISTORIAL, JSON.stringify(historial.slice(-MAX_MENSAJES)));
    } catch {
      /* almacenamiento lleno o bloqueado: la conversacion sigue en memoria */
    }
  }

  function cargarContacto() {
    try {
      return JSON.parse(localStorage.getItem(CLAVE_CONTACTO) || '{}') || {};
    } catch {
      return {};
    }
  }

  function guardarContacto(datos) {
    try {
      localStorage.setItem(CLAVE_CONTACTO, JSON.stringify(datos));
    } catch {
      /* sin persistencia */
    }
  }

  function cargarToken() {
    try {
      return localStorage.getItem(CLAVE_TOKEN) || '';
    } catch {
      return '';
    }
  }

  function guardarToken(token) {
    try {
      if (token) localStorage.setItem(CLAVE_TOKEN, token);
    } catch {
      /* sin persistencia */
    }
  }

  /* ---------------------- Hilo con el asesor humano ------------------ */

  /**
   * Revisa si el equipo respondio desde el panel. Los mensajes del asesor se
   * agregan al recuadro en vivo mientras el visitante lo tiene abierto.
   */
  async function sincronizarHilo() {
    const token = cargarToken();
    if (!token) return;

    try {
      const respuesta = await fetch(`${API_CONFIG.base}/asistente/conversacion/${encodeURIComponent(token)}/mensajes`);
      if (!respuesta.ok) return;
      const mensajes = await respuesta.json();
      if (!Array.isArray(mensajes) || !mensajes.length) return;

      const nuevos = mensajes.filter((m) => m.remitente !== 'visitante' && Number(m.idMensaje) > ultimoIdMensaje);
      if (!nuevos.length) return;

      ultimoIdMensaje = Math.max(ultimoIdMensaje, ...nuevos.map((m) => Number(m.idMensaje) || 0));
      let huboAsesor = false;
      nuevos.forEach((m) => {
        if (m.remitente === 'asesor') {
          huboAsesor = true;
          agregarMensaje('asesor', m.mensaje, 'Asesor TECNISECURE');
          historial.push({ rol: 'asesor', texto: m.mensaje });
        } else if (m.remitente === 'bot') {
          agregarMensaje('bot', m.mensaje);
          historial.push({ rol: 'assistant', texto: m.mensaje });
        }
      });
      guardarHistorial();

      const estado = document.getElementById('asistenteEstado');
      if (estado && huboAsesor) estado.textContent = 'Un asesor te respondio';
      if (!abierto) marcarNoLeido();
    } catch {
      /* sin conexion: se reintenta en el siguiente ciclo */
    }
  }

  function marcarNoLeido() {
    const boton = document.getElementById('asistenteBoton');
    if (!boton || abierto) return;
    boton.classList.add('asistente-boton--nuevo');
  }

  function quitarNoLeido() {
    document.getElementById('asistenteBoton')?.classList.remove('asistente-boton--nuevo');
  }

  function iniciarSondeo() {
    if (temporizadorHilo) return;
    temporizadorHilo = setInterval(sincronizarHilo, INTERVALO_HILO);
    sincronizarHilo();
  }

  /* ------------------------------- Estructura ------------------------ */

  function construir() {
    if (document.getElementById('asistenteChat')) return;

    const envoltorio = document.createElement('div');
    envoltorio.innerHTML = `
      <button type="button" class="asistente-boton" id="asistenteBoton"
              aria-label="Abrir el asistente virtual" aria-expanded="false">
        <i class="fa-solid fa-robot"></i>
        <span class="asistente-boton__punto" aria-hidden="true"></span>
      </button>

      <section class="asistente-panel" id="asistenteChat" role="dialog"
               aria-label="Asistente virtual de TECNISECURE" aria-modal="false">
        <header class="asistente-cabecera">
          <div class="asistente-cabecera__info">
            <span class="asistente-avatar"><i class="fa-solid fa-robot"></i></span>
            <div>
              <strong>Asistente TECNISECURE</strong>
              <small id="asistenteEstado">En linea · responde al instante</small>
            </div>
          </div>
          <button type="button" class="asistente-cerrar" id="asistenteCerrar" aria-label="Cerrar chat">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </header>

        <div class="asistente-mensajes" id="asistenteMensajes" aria-live="polite"></div>

        <div class="asistente-sugerencias" id="asistenteSugerencias"></div>

        <form class="asistente-form" id="asistenteForm">
          <input type="text" id="asistenteInput" maxlength="1000" autocomplete="off"
                 placeholder="Escribe tu pregunta..." aria-label="Tu mensaje">
          <button type="submit" id="asistenteEnviar" aria-label="Enviar mensaje">
            <i class="fa-solid fa-paper-plane"></i>
          </button>
        </form>
        <p class="asistente-pie">
          <i class="fa-solid fa-lock"></i> Tus datos solo se usan para atenderte.
        </p>
      </section>`;
    while (envoltorio.firstElementChild) {
      document.body.appendChild(envoltorio.firstElementChild);
    }
  }

  /* ------------------------------- Pintado --------------------------- */

  function agregarMensaje(rol, texto, extra) {
    const cont = document.getElementById('asistenteMensajes');
    if (!cont) return;
    const burbuja = document.createElement('div');
    const tipo = rol === 'user' ? 'usuario' : (rol === 'asesor' ? 'asesor' : 'bot');
    burbuja.className = 'asistente-msg asistente-msg--' + tipo;
    burbuja.innerHTML = `<div class="asistente-msg__texto">${escapar(texto).replace(/\n/g, '<br>')}</div>`;
    if (extra) {
      const nota = document.createElement('small');
      nota.className = 'asistente-msg__nota';
      nota.textContent = extra;
      burbuja.appendChild(nota);
    }
    cont.appendChild(burbuja);
    cont.scrollTop = cont.scrollHeight;
  }

  function pintarHistorial() {
    const cont = document.getElementById('asistenteMensajes');
    if (!cont) return;
    cont.innerHTML = '';
    if (historial.length === 0) {
      agregarMensaje('bot',
        '¡Hola! Soy el asistente de TECNISECURE.\n' +
        'Puedo ayudarte con productos, precios, servicios, garantias o ponerte en contacto con un asesor.');
    } else {
      historial.forEach((m) => agregarMensaje(m.rol, m.texto, m.rol === 'asesor' ? 'Asesor TECNISECURE' : null));
    }
  }

  function pintarSugerencias(lista) {
    const cont = document.getElementById('asistenteSugerencias');
    if (!cont) return;
    const opciones = Array.isArray(lista) && lista.length ? lista : FRECUENTES;
    cont.innerHTML = opciones.map((s) =>
      `<button type="button" class="asistente-chip">${escapar(s)}</button>`).join('');
  }

  function mostrarEscribiendo() {
    const cont = document.getElementById('asistenteMensajes');
    if (!cont || document.getElementById('asistenteEscribiendo')) return;
    const el = document.createElement('div');
    el.id = 'asistenteEscribiendo';
    el.className = 'asistente-msg asistente-msg--bot';
    el.innerHTML = '<div class="asistente-msg__texto asistente-puntos">' +
      '<span></span><span></span><span></span></div>';
    cont.appendChild(el);
    cont.scrollTop = cont.scrollHeight;
  }

  function quitarEscribiendo() {
    const el = document.getElementById('asistenteEscribiendo');
    if (el) el.remove();
  }

  /* ------------------------------- Acciones -------------------------- */

  function abrir() {
    const panel = document.getElementById('asistenteChat');
    const boton = document.getElementById('asistenteBoton');
    if (!panel) return;
    panel.classList.add('activo');
    boton?.setAttribute('aria-expanded', 'true');
    abierto = true;
    pintarHistorial();
    pintarSugerencias();
    quitarNoLeido();
    iniciarSondeo();
    setTimeout(() => document.getElementById('asistenteInput')?.focus(), 120);
  }

  function cerrar() {
    const panel = document.getElementById('asistenteChat');
    const boton = document.getElementById('asistenteBoton');
    if (!panel) return;
    panel.classList.remove('activo');
    boton?.setAttribute('aria-expanded', 'false');
    abierto = false;
  }

  async function enviar(textoForzado) {
    if (enviando) return;
    const input = document.getElementById('asistenteInput');
    const texto = (textoForzado || (input ? input.value : '')).trim();
    if (!texto) return;

    if (input) input.value = '';
    historial.push({ rol: 'user', texto });
    agregarMensaje('user', texto);
    guardarHistorial();
    pintarSugerencias([]);

    enviando = true;
    mostrarEscribiendo();
    const botonEnviar = document.getElementById('asistenteEnviar');
    if (botonEnviar) botonEnviar.disabled = true;

    try {
      const contacto = cargarContacto();
      const cuerpo = {
        mensaje: texto,
        historial: historial.slice(-10, -1).map((m) => ({ rol: m.rol, texto: m.texto })),
        nombre: contacto.nombre || null,
        correo: contacto.correo || null,
        telefono: contacto.telefono || null,
        token: cargarToken() || null
      };

      const respuesta = await fetch(`${API_CONFIG.base}/asistente`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(cuerpo)
      });

      if (!respuesta.ok) {
        throw new Error('El asistente no esta disponible en este momento.');
      }
      const datos = await respuesta.json();
      quitarEscribiendo();

      if (datos.token) {
        guardarToken(datos.token);
        iniciarSondeo();
      }

      const nota = datos.escalado
        ? 'Un asesor del equipo revisara tu caso.'
        : null;
      agregarMensaje('bot', datos.respuesta, nota);
      historial.push({ rol: 'assistant', texto: datos.respuesta });
      guardarHistorial();

      if (datos.escalado) {
        const estado = document.getElementById('asistenteEstado');
        if (estado) estado.textContent = 'Solicitud enviada a un asesor';
      }
      pintarSugerencias(datos.sugerencias);
    } catch (error) {
      quitarEscribiendo();
      const textoError = 'No pude conectarme ahora mismo. ' +
        'Puedes escribirnos por WhatsApp o al correo soporte@tecnisecure.com.';
      agregarMensaje('bot', textoError);
      historial.push({ rol: 'assistant', texto: textoError });
      guardarHistorial();
      pintarSugerencias(FRECUENTES);
    } finally {
      enviando = false;
      if (botonEnviar) botonEnviar.disabled = false;
      document.getElementById('asistenteInput')?.focus();
    }
  }

  /* ------------------------------- Arranque -------------------------- */

  function iniciar() {
    construir();

    document.getElementById('asistenteBoton')?.addEventListener('click', () => {
      abierto ? cerrar() : abrir();
    });
    document.getElementById('asistenteCerrar')?.addEventListener('click', cerrar);

    document.getElementById('asistenteForm')?.addEventListener('submit', (e) => {
      e.preventDefault();
      enviar();
    });

    document.getElementById('asistenteSugerencias')?.addEventListener('click', (e) => {
      const chip = e.target.closest('.asistente-chip');
      if (chip) enviar(chip.textContent);
    });

    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && abierto) cerrar();
    });

    pintarHistorial();
    pintarSugerencias();
    if (cargarToken()) iniciarSondeo();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciar);
  } else {
    iniciar();
  }
})();
