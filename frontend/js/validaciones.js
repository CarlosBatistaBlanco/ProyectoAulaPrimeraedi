/**
 * validaciones.js — Reglas de formato compartidas por todos los formularios
 * -----------------------------------------------------------------
 * Deja escribir solo lo permitido y avisa con un mensaje claro debajo del
 * campo. El backend valida lo mismo (PatronesValidacion.java), asi que no se
 * puede saltar la regla desde el navegador.
 *
 * Uso: incluir despues de api.js en cualquier pagina con <form>.
 *   document.querySelectorAll('input[name="telefono"]') -> se configura solo.
 */

(function () {
  'use strict';

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

  const DOMINIOS = ['gmail.com', 'hotmail.com', 'outlook.com', 'yahoo.com', 'icloud.com', 'live.com'];
  const CAMPOS = Object.keys(REGLAS);

  /* ------------------------------ Utilidades ------------------------- */

  function reglaDe(input) {
    const nombre = (input.getAttribute('name') || input.id || '').toLowerCase();
    if (/documento/.test(nombre)) return 'documento';
    if (/telefono|celular/.test(nombre)) return 'telefono';
    if (/correo|email/.test(nombre)) return 'correo';
    if (/password|contrasena/.test(nombre)) {
      // "password2" solo debe coincidir con la primera
      return /password2|confirmar/.test(nombre) ? 'confirmar' : 'password';
    }
    if (/nombre|apellido/.test(nombre) && input.type !== 'hidden') return 'nombre';
    return null;
  }

  /**
   * Contenedor donde va el mensaje: el `.campo` mas cercano. Asi el aviso
   * queda debajo del input aunque este este dentro de un envoltorio flex
   * (por ejemplo `.campo-password`, que lleva el boton de ver la clave).
   */
  function contenedorDe(input) {
    const campo = input.closest('.campo');
    return campo || input.parentElement;
  }

  function asegurarAviso(input) {
    const contenedor = contenedorDe(input);
    let aviso = contenedor.querySelector(':scope > .ayuda-validacion');
    if (aviso) return aviso;

    // Restos de versiones previas dentro del envoltorio del input
    input.parentElement
      .querySelectorAll(':scope > .ayuda-validacion')
      .forEach((viejo) => viejo.remove());

    aviso = document.createElement('small');
    aviso.className = 'ayuda-validacion';
    aviso.setAttribute('aria-live', 'polite');
    contenedor.appendChild(aviso);
    return aviso;
  }

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

  function validar(input) {
    const clave = reglaDe(input);
    if (!clave) return true;

    const valor = input.value.trim();
    if (valor === '') return true; // los "required" los controla el navegador

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

  /** Dominios de segundo nivel que ya cierran un correo valido (.com.co, .edu.co...). */
  const TLD_COMPUESTOS = ['com', 'edu', 'gov', 'org', 'net', 'ac', 'mil'];

  function terminaBien(dominio) {
    const partes = dominio.split('.').filter(Boolean);
    if (partes.length < 2) return false;
    const ultimo = partes[partes.length - 1];
    const penultimo = partes[partes.length - 2];
    if (!/^[A-Za-z]{2,}$/.test(ultimo)) return false;
    // edu.co, com.co, gov.co: el dominio ya esta completo
    if (ultimo.length === 2 && TLD_COMPUESTOS.includes(penultimo.toLowerCase())) return true;
    return true;
  }

  /**
   * Al salir del campo de correo, si escribio "ana@gmail" o "ana" se completa
   * el dominio: "ana@gmail" -> "ana@gmail.com" y "ana" -> "ana@gmail.com".
   * Un dominio ya completo no se toca nunca (leonardo@unocolombo.edu.co).
   */
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
        // "gmail" -> "gmail.com"; "hotmail" -> "hotmail.com"
        valor = usuario + '@' + dominio + '.com';
      } else if (!terminaBien(dominio)) {
        valor = usuario + '@' + dominio + '.com';
      }
    }
    input.value = valor;
  }

  /** Sugerencias rapidas de dominio para el @ (gmail, hotmail...). */
  function sugerirDominios(input) {
    if (input.dataset.sugerenciasListo) return;
    input.dataset.sugerenciasListo = '1';
    let lista = null;

    const ocultar = () => { if (lista) lista.style.display = 'none'; };

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

    input.addEventListener('blur', () => setTimeout(ocultar, 150));

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

  function configurar(input) {
    const clave = reglaDe(input);
    if (!clave) return;

    // Solo numeros mientras escribe (documento y telefono)
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

    if (clave === 'correo') {
      input.setAttribute('autocomplete', input.getAttribute('autocomplete') || 'email');
      input.addEventListener('blur', () => completarCorreo(input));
      sugerirDominios(input);
    }

    input.addEventListener('blur', () => validar(input));
    input.addEventListener('input', () => { if (input.value.length > 2) validar(input); });

    // Mensajes de ayuda iniciales
    const ayuda = REGLAS[clave] && REGLAS[clave].ayuda;
    if (ayuda && !input.placeholder) input.placeholder = ayuda;
  }

  function configurarFormulario(form) {
    form.querySelectorAll('input').forEach((input) => {
      if (input.type === 'hidden' || input.type === 'checkbox' || input.type === 'file') return;
      configurar(input);
    });
    // Evita enviar si hay un campo invalido (ademas del required nativo)
    form.addEventListener('submit', (e) => {
      const invalidos = [...form.querySelectorAll('input')].filter((i) => !validar(i));
      if (invalidos.length) {
        e.preventDefault();
        e.stopImmediatePropagation();
        invalidos[0].focus();
      }
    }, true);
  }

  function iniciar() {
    document.querySelectorAll('form').forEach(configurarFormulario);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciar);
  } else {
    iniciar();
  }

  // API publica por si otra pagina crea formularios dinamicamente
  window.Validaciones = { configurarFormulario, validar, REGLAS };
})();
