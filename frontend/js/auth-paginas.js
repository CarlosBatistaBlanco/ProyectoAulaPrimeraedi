/**
 * auth-paginas.js — Lógica de las pantallas de acceso
 * -----------------------------------------------------------------
 * · login.html       → POST /api/auth/login
 * · registro.html    → POST /api/auth/registro
 * · recuperar.html   → POST /api/auth/recuperar
 * · restablecer.html → POST /api/auth/restablecer
 * Mantiene las mismas validaciones y mensajes del sistema original.
 */

const MENSAJES_REDIRECCION = {
  administrador: 'panel/administrador/index.html',
  empleado: 'panel/empleado/index.html',
  cliente: 'panel/cliente/index.html'
};

function mostrarAlerta(contenedor, mensaje, tipo = 'error') {
  if (!contenedor) return;
  contenedor.textContent = mensaje;
  contenedor.className = `auth-alerta auth-alerta--${tipo === 'ok' ? 'ok' : tipo === 'info' ? 'info' : 'error'}`;
  contenedor.style.display = 'block';
}

function parametroUrl(nombre) {
  return new URLSearchParams(window.location.search).get(nombre) || '';
}

/* ------------------------------ Login ------------------------------ */

async function cargarCaptcha() {
  const contenedor = document.getElementById('preguntaCaptcha');
  if (!contenedor) return;
  try {
    const datos = await Api.get('/auth/captcha');
    contenedor.textContent = datos.pregunta;
  } catch {
    contenedor.textContent = '¿cuánto es 3 + 4?';
  }
}

function inicializarLogin() {
  const form = document.getElementById('formLogin');
  if (!form) return;

  // Si ya hay sesión, se entra directo al panel correspondiente.
  const usuario = Api.getUsuario();
  if (Api.estaAutenticado() && usuario) {
    window.location.href = MENSAJES_REDIRECCION[usuario.rol] || 'index.html';
    return;
  }

  cargarCaptcha();

  const alerta = document.getElementById('alertaLogin') || form.parentElement.querySelector('.auth-alerta');

  // Mensajes que llegan desde otra pagina (ej. registro.html?exito=...)
  const exito = parametroUrl('exito');
  const errorUrl = parametroUrl('error');
  if (exito) mostrarAlerta(alerta, exito, 'ok');
  else if (errorUrl) mostrarAlerta(alerta, errorUrl, 'error');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const boton = form.querySelector('button[type="submit"]');
    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Entrando...';
    boton.disabled = true;

    try {
      const respuesta = await Api.post('/auth/login', {
        correo: form.elements.correo.value.trim(),
        password: form.elements.password.value
      });

      Api.guardarSesion(respuesta);

      const destino = parametroUrl('redir')
        || MENSAJES_REDIRECCION[respuesta.usuario.rol]
        || 'index.html';
      window.location.href = destino;
    } catch (error) {
      mostrarAlerta(alerta, error.message);
      cargarCaptcha();
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
    }
  });
}

/* ----------------------------- Registro ---------------------------- */

function inicializarRegistro() {
  const form = document.getElementById('formRegistro');
  if (!form) return;

  const alerta = form.parentElement.querySelector('.auth-alerta');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const password = form.password.value;
    const password2 = form.password2.value;

    if (password !== password2) {
      mostrarAlerta(alerta, 'Las contraseñas no coinciden.');
      return;
    }
    if (!/^(?=.*[A-Z])(?=.*\d).{8,}$/.test(password)) {
      mostrarAlerta(alerta, 'La contraseña debe tener mínimo 8 caracteres, una mayúscula y un número.');
      return;
    }
    if (form.terminos && !form.terminos.checked) {
      mostrarAlerta(alerta, 'Debes aceptar los términos y condiciones.');
      return;
    }

    const boton = form.querySelector('button[type="submit"]');
    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Creando cuenta...';
    boton.disabled = true;

    try {
      const respuesta = await Api.post('/auth/registro', {
        nombre: form.nombre.value.trim(),
        documento: form.documento.value.trim(),
        correo: form.correo.value.trim(),
        telefono: form.telefono.value.trim(),
        password
      });

      mostrarAlerta(alerta, respuesta.mensaje, 'ok');
      form.reset();
      setTimeout(() => { window.location.href = 'login.html?exito=' + encodeURIComponent(respuesta.mensaje); }, 1200);
    } catch (error) {
      mostrarAlerta(alerta, error.message);
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
    }
  });
}

/* ---------------------------- Recuperar ---------------------------- */

function inicializarRecuperar() {
  const form = document.getElementById('formRecuperar');
  if (!form) return;

  const alerta = form.parentElement.querySelector('.auth-alerta');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const correo = form.elements.correo.value.trim();
    const boton = form.querySelector('button[type="submit"]');
    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Enviando...';
    boton.disabled = true;

    try {
      const respuesta = await Api.post('/auth/recuperar', { correo });
      mostrarAlerta(alerta, respuesta.mensaje, 'ok');

      if (respuesta.tokenDesarrollo) {
        const caja = document.getElementById('enlaceRecuperacion');
        const enlace = document.getElementById('enlaceRestablecer');
        if (caja && enlace) {
          enlace.href = `restablecer.html?token=${encodeURIComponent(respuesta.tokenDesarrollo)}`;
          caja.style.display = 'block';
        }
      }
    } catch (error) {
      mostrarAlerta(alerta, error.message);
    } finally {
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
    }
  });
}

/* --------------------------- Restablecer --------------------------- */

function inicializarRestablecer() {
  const form = document.getElementById('formRestablecer');
  if (!form) return;

  const alerta = form.parentElement.querySelector('.auth-alerta');
  const token = parametroUrl('token');
  if (!token) {
    mostrarAlerta(alerta, 'El enlace no es válido. Solicita uno nuevo.');
  }

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (form.password.value !== form.password2.value) {
      mostrarAlerta(alerta, 'Las contraseñas no coinciden.');
      return;
    }

    const boton = form.querySelector('button[type="submit"]');
    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Guardando...';
    boton.disabled = true;

    try {
      await Api.post('/auth/restablecer', { token, password: form.password.value });
      mostrarAlerta(alerta, 'Contraseña actualizada. Ya puedes iniciar sesión.', 'ok');
      setTimeout(() => { window.location.href = 'login.html'; }, 1500);
    } catch (error) {
      mostrarAlerta(alerta, error.message);
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
    }
  });
}

/* ------------- Encabezado dinámico (usuario conectado) ------------- */

function rutaPanel(rol) {
  // MENSAJES_REDIRECCION apunta a la raiz del sitio; se antepone RUTA_BASE
  // para que funcione igual desde index.html y desde pages/.
  const relativa = MENSAJES_REDIRECCION[rol];
  return relativa ? (window.RUTA_BASE || '') + relativa : (window.RUTA_BASE || '') + 'index.html';
}

function pintarEstadoSesion() {
  const enlaceCuenta = document.getElementById('btnCuenta');
  if (!enlaceCuenta) return;

  const usuario = Api.getUsuario();
  const autenticado = Api.estaAutenticado() && usuario;

  if (!autenticado) {
    enlaceCuenta.href = (window.RUTA_BASE || '') + 'login.html';
    enlaceCuenta.title = 'Iniciar sesión';
    return;
  }

  const nombre = (usuario.nombre || usuario.correo || '').trim();
  const primerNombre = nombre.split(' ')[0] || 'Mi cuenta';
  const destino = rutaPanel(usuario.rol);

  enlaceCuenta.innerHTML =
    `<i class="fa-solid fa-user"></i> <span class="btn-cuenta__nombre">${escaparHtml(primerNombre)}</span>`;
  enlaceCuenta.href = destino;
  enlaceCuenta.title = 'Ir a mi panel';
  enlaceCuenta.classList.add('btn-cuenta--activa');

  // Boton de cerrar sesion junto al de la cuenta (solo si hay sesion).
  let salir = document.getElementById('btnSalirSesion');
  if (!salir) {
    salir = document.createElement('button');
    salir.type = 'button';
    salir.id = 'btnSalirSesion';
    salir.className = 'btn-salir-sesion';
    salir.title = 'Cerrar sesión';
    salir.setAttribute('aria-label', 'Cerrar sesión');
    salir.innerHTML = '<i class="fa-solid fa-right-from-bracket"></i>';
    salir.addEventListener('click', () => {
      Api.cerrarSesion();
      window.location.href = (window.RUTA_BASE || '') + 'index.html';
    });
    enlaceCuenta.insertAdjacentElement('afterend', salir);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  inicializarLogin();
  inicializarRegistro();
  inicializarRecuperar();
  inicializarRestablecer();
  pintarEstadoSesion();
});
