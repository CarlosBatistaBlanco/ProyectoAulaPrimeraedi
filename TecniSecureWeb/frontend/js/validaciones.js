/**
 * Este archivo revisa en vivo lo que la persona escribe en todos los formularios de la tienda y los paneles
 * (cédula, teléfono, correo, nombre y contraseña). Bloquea caracteres no permitidos, sugiere terminaciones
 * de correo electrónico como gmail o hotmail y muestra mensajes claros debajo de cada casilla antes de
 * permitir enviar los datos.
 */

// Bloque cerrado que protege los formularios y orienta a la persona mientras escribe
(function () {
  'use strict';

  // Reglas de formato para cada tipo de dato (cédula, teléfono, correo, nombre y contraseña)
  const REGLAS = {
    documento: {
      patron: /^[0-9]{5,15}$/,
      soloDigitos: true,
      mensaje: 'Solo números (5 a 15 dígitos).',
      ayuda: 'Ej: 1234567890'
    },
    telefono: {
      patron: /^(\+?57)?[\s-]?(\(?\d{1,3}\)?[\s-]?)?\d{7,10}$/,
      soloDigitos: true,
      mensaje: 'Solo números (7 a 10 dígitos). Puedes empezar con +57.',
      ayuda: 'Ej: 300 123 4567'
    },
    correo: {
      patron: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/,
      mensaje: 'Escribe un correo completo, por ejemplo nombre@gmail.com',
      ayuda: 'Debe terminar en .com, .co, .net…'
    },
    nombre: {
      patron: /^[A-Za-zÁÉÍÓÚÑÜáéíóúñü][A-Za-zÁÉÍÓÚÑÜáéíóúñü' .-]{2,119}$/,
      mensaje: 'Solo letras y espacios.',
      ayuda: 'Ej: Ana Martínez'
    },
    password: {
      patron: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/,
      mensaje: 'Mínimo 8 caracteres, una mayúscula, una minúscula y un número.',
      ayuda: 'Ej: Prueba123!'
    }
  };

  // Terminaciones de correo más comunes para ofrecer sugerencias rápidas al escribir "@"
  const DOMINIOS = ['gmail.com', 'hotmail.com', 'outlook.com', 'yahoo.com', 'icloud.com', 'live.com'];
  const CAMPOS = Object.keys(REGLAS);

  /* ------------------------------ Utilidades ------------------------- */

  // Identifica qué tipo de dato pide una casilla según su nombre (teléfono, correo, documento, etc.)
  function reglaDe(input) {
    const nombre = (input.getAttribute('name') || input.id || '').toLowerCase();
    if (/documento/.test(nombre)) return 'documento';
    if (/telefono|celular/.test(nombre)) return 'telefono';
    if (/correo|email/.test(nombre)) return 'correo';
    if (/password|contrasena/.test(nombre)) {
      // La casilla de confirmar contraseña solo debe coincidir con la primera
      return /password2|confirmar/.test(nombre) ? 'confirmar' : 'password';
    }
    if (/nombre|apellido/.test(nombre) && input.type !== 'hidden') return 'nombre';
    return null;
  }

  // Busca el recuadro principal de la casilla para colocar el aviso debajo, incluso si tiene botón de ojo al lado
  function contenedorDe(input) {
    const campo = input.closest('.campo');
    return campo || input.parentElement;
  }

  // Crea o reutiliza el espacio de texto donde se muestran las explicaciones debajo de cada casilla
  function asegurarAviso(input) {
    const contenedor = contenedorDe(input);
    let aviso = contenedor.querySelector(':scope > .ayuda-validacion');
    if (aviso) return aviso;

    // Limpia avisos repetidos que hayan quedado pegados dentro de la casilla
    input.parentElement
      .querySelectorAll(':scope > .ayuda-validacion')
      .forEach((viejo) => viejo.remove());

    aviso = document.createElement('small');
    aviso.className = 'ayuda-validacion';
    aviso.setAttribute('aria-live', 'polite');
    contenedor.appendChild(aviso);
    return aviso;
  }

  // Resalta el borde de la casilla en rojo si hay un error y escribe la explicación debajo
  function marcar(input, valido, mensaje) {
    const aviso = asegurarAviso(input);
    input.classList.toggle('campo-invalido', !valido && input.value.trim() !== '');
    input.setAttribute('aria-invalid', String(!valido && input.value.trim() !== ''));
    if (input.value.trim() === '') {
      aviso.textContent = '';
      aviso.className = 'ayuda-validacion';
      return;
    }
    aviso.textContent = valido ? '' : mensaje;
    aviso.className = 'ayuda-validacion ' + (valido ? 'valido' : 'invalido');
  }

  // Revisa si lo que escribió la persona cumple la regla de la casilla y actualiza el mensaje visual
  function validar(input) {
    const clave = reglaDe(input);
    if (!clave) return true;

    const valor = input.value.trim();
    if (valor === '') return true; // las casillas obligatorias vacías ya las avisa el navegador

    // Caso especial: confirmación de contraseña (debe ser idéntica a la casilla principal)
    if (clave === 'confirmar') {
      const primera = input.form ? input.form.querySelector('[name="password"], #password') : null;
      const coincide = primera ? primera.value === input.value : true;
      marcar(input, coincide, 'Las contraseñas no coinciden.');
      return coincide;
    }

    const regla = REGLAS[clave];
    const valido = regla.patron.test(valor);
    marcar(input, valido, regla.mensaje);
    return valido;
  }

  /* --------------------------- Correo: .com ------------------------- */

  // Terminaciones compuestas válidas para correos institucionales o de país (como .com.co o .edu.co)
  const TLD_COMPUESTOS = ['com', 'edu', 'gov', 'org', 'net', 'ac', 'mil'];

  // Revisa si la parte final del correo (después del @) ya tiene una terminación completa
  function terminaBien(dominio) {
    const partes = dominio.split('.').filter(Boolean);
    if (partes.length < 2) return false;
    const ultimo = partes[partes.length - 1];
    const penultimo = partes[partes.length - 2];
    if (!/^[A-Za-z]{2,}$/.test(ultimo)) return false;
    // Correos como edu.co, com.co o gov.co ya están completos
    if (ultimo.length === 2 && TLD_COMPUESTOS.includes(penultimo.toLowerCase())) return true;
    return true;
  }

  // Al salir de la casilla de correo, completa terminaciones incompletas (por ejemplo "ana@gmail" pasa a "ana@gmail.com")
  function completarCorreo(input) {
    let valor = input.value.trim().replace(/\s/g, '');
    if (!valor) return;
    if (!valor.includes('@')) {
      valor += '@gmail.com';
    } else {
      const iArroba = valor.lastIndexOf('@');
      const usuario = valor.slice(0, iArroba);
      const dominio = valor.slice(iArroba + 1);
      if (!usuario) return;
      if (!dominio) {
        valor = usuario + '@gmail.com';
      } else if (!dominio.includes('.')) {
        // Agrega .com cuando solo se escribió el proveedor (como gmail o hotmail)
        valor = usuario + '@' + dominio + '.com';
      } else if (!terminaBien(dominio)) {
        valor = usuario + '@' + dominio + '.com';
      }
    }
    input.value = valor;
  }

  // Muestra sugerencias rápidas de correos comunes (gmail, hotmail, outlook...) al escribir "@"
  function sugerirDominios(input) {
    if (input.dataset.sugerenciasListo) return;
    input.dataset.sugerenciasListo = '1';
    let lista = null;

    const ocultar = () => { if (lista) lista.style.display = 'none'; };

    // Mientras la persona escribe después del "@", despliega opciones de correo para elegir con un clic
    input.addEventListener('input', () => {
      const valor = input.value;
      const iArroba = valor.lastIndexOf('@');
      if (iArroba < 0) { ocultar(); return; }
      const escrito = valor.slice(iArroba + 1).toLowerCase();
      if (escrito.includes('.')) { ocultar(); return; }

      const opciones = DOMINIOS.filter((d) => d.startsWith(escrito)).slice(0, 4);
      if (!opciones.length) { ocultar(); return; }

      if (!lista) {
        lista = document.createElement('div');
        lista.className = 'sugerencias-correo';
        contenedorDe(input).appendChild(lista);
      }
      lista.innerHTML = opciones.map((d) =>
        `<button type="button" data-dominio="${d}">${valor.slice(0, iArroba + 1)}${d}</button>`).join('');
      lista.style.display = 'block';
    });

    // Oculta las sugerencias cuando la persona sale de la casilla
    input.addEventListener('blur', () => setTimeout(ocultar, 150));

    // Al hacer clic en una sugerencia, rellena el correo completo y lo revisa de inmediato
    contenedorDe(input).addEventListener('mousedown', (e) => {
      const boton = e.target.closest('[data-dominio]');
      if (!boton) return;
      e.preventDefault();
      input.value = boton.textContent.trim();
      ocultar();
      validar(input);
      input.dispatchEvent(new Event('input', { bubbles: true }));
    });
  }

  /* ------------------------------ Arranque -------------------------- */

  // Activa todas las ayudas automáticas (teclado numérico, sugerencias de correo y revisión en vivo) en una casilla
  function configurar(input) {
    const clave = reglaDe(input);
    if (!clave) return;

    // Permite escribir solo números y signos telefónicos en las casillas de cédula y teléfono
    if (REGLAS[clave] && REGLAS[clave].soloDigitos) {
      input.setAttribute('inputmode', 'numeric');
      input.addEventListener('input', () => {
        const limpio = input.value.replace(/[^0-9+\s()-]/g, '');
        if (limpio !== input.value) {
          const pos = input.selectionStart;
          input.value = limpio;
          input.setSelectionRange(Math.max(0, pos - 1), Math.max(0, pos - 1));
        }
      });
    }

    // En las casillas de correo activa el autocompletado y las sugerencias rápidas
    if (clave === 'correo') {
      input.setAttribute('autocomplete', input.getAttribute('autocomplete') || 'email');
      input.addEventListener('blur', () => completarCorreo(input));
      sugerirDominios(input);
    }

    // Revisa la casilla cuando la persona termina de escribir o mientras avanza
    input.addEventListener('blur', () => validar(input));
    input.addEventListener('input', () => { if (input.value.length > 2) validar(input); });

    // Coloca un texto de ejemplo dentro de la casilla si aún no tiene uno
    const ayuda = REGLAS[clave] && REGLAS[clave].ayuda;
    if (ayuda && !input.placeholder) input.placeholder = ayuda;
  }

  // Configura todas las casillas de un formulario e impide enviarlo si algún dato tiene error de formato
  function configurarFormulario(form) {
    form.querySelectorAll('input').forEach((input) => {
      if (input.type === 'hidden' || input.type === 'checkbox' || input.type === 'file') return;
      configurar(input);
    });
    // Detiene el envío si encuentra alguna casilla con error y lleva el cursor hasta ella
    form.addEventListener('submit', (e) => {
      const invalidos = [...form.querySelectorAll('input')].filter((i) => !validar(i));
      if (invalidos.length) {
        e.preventDefault();
        e.stopImmediatePropagation();
        invalidos[0].focus();
      }
    }, true);
  }

  // Busca todos los formularios visibles en la página y les activa las reglas de ayuda
  function iniciar() {
    document.querySelectorAll('form').forEach(configurarFormulario);
  }

  // Espera a que la página termine de cargar para activar las validaciones
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciar);
  } else {
    iniciar();
  }

  // Deja disponibles estas funciones por si alguna pantalla crea formularios nuevos más adelante
  window.Validaciones = { configurarFormulario, validar, REGLAS };
})();
