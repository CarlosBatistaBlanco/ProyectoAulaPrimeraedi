/**
 * Este archivo controla las pantallas de inicio de sesión, creación de cuenta, recuperación de contraseña
 * y cambio de clave. También actualiza la barra superior de la tienda para mostrar el nombre de la persona
 * conectada, el enlace directo a su panel personal y el botón para cerrar sesión.
 */

// Pantalla principal a la que entra cada persona después de iniciar sesión según su tipo de cuenta
const MENSAJES_REDIRECCION = {
  administrador: 'panel/administrador/index.html',
  empleado: 'panel/empleado/index.html',
  cliente: 'panel/cliente/index.html'
};

// Muestra un recuadro de aviso (rojo de error, verde de éxito o azul informativo) en los formularios de acceso
function mostrarAlerta(contenedor, mensaje, tipo = 'error') {
  if (!contenedor) return;
  contenedor.textContent = mensaje;
  contenedor.className = `auth-alerta auth-alerta--${tipo === 'ok' ? 'ok' : tipo === 'info' ? 'info' : 'error'}`;
  contenedor.style.display = 'block';
}

// Lee un dato enviado en la dirección de la página (por ejemplo, el código de recuperación o un aviso de éxito)
function parametroUrl(nombre) {
  return new URLSearchParams(window.location.search).get(nombre) || '';
}

/* ------------------------ Inicio de sesión ------------------------- */

// Código de seguridad de la pregunta de verificación actual enviado por el servidor
let captchaFicha = '';

// Pide al servidor una pregunta matemática sencilla para confirmar que quien entra es una persona real
async function cargarCaptcha() {
  const contenedor = document.getElementById('preguntaCaptcha');
  const campo = document.getElementById('captcha');
  if (!contenedor) return;
  try {
    const datos = await Api.get('/auth/captcha');
    contenedor.textContent = datos.pregunta;
    captchaFicha = datos.token || '';
  } catch {
    contenedor.textContent = 'No se pudo cargar la verificación. Recarga la página.';
    captchaFicha = '';
  }
  if (campo) campo.value = '';
}

// Prepara la pantalla de inicio de sesión: revisa si ya entró, carga la pregunta de verificación y envía los datos
function inicializarLogin() {
  const form = document.getElementById('formLogin');
  if (!form) return;

  // Si la persona ya tiene la sesión abierta, la lleva directo a su panel
  const usuario = Api.getUsuario();
  if (Api.estaAutenticado() && usuario) {
    window.location.href = MENSAJES_REDIRECCION[usuario.rol] || 'index.html';
    return;
  }

  cargarCaptcha();

  const alerta = document.getElementById('alertaLogin') || form.parentElement.querySelector('.auth-alerta');

  // Muestra mensajes de confirmación o error que lleguen desde otra pantalla (por ejemplo, tras registrarse)
  const exito = parametroUrl('exito');
  const errorUrl = parametroUrl('error');
  if (exito) mostrarAlerta(alerta, exito, 'ok');
  else if (errorUrl) mostrarAlerta(alerta, errorUrl, 'error');

  // Detecta cuando la persona presiona "Iniciar sesión", revisa la verificación y abre su cuenta
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    // Comprueba que haya escrito un número válido en la pregunta de verificación
    const respuestaCaptcha = form.elements.captcha ? form.elements.captcha.value.trim() : '';
    if (!respuestaCaptcha) {
      mostrarAlerta(alerta, 'Escribe el resultado de la verificación.');
      form.elements.captcha?.focus();
      return;
    }
    if (!/^\d+$/.test(respuestaCaptcha)) {
      mostrarAlerta(alerta, 'La verificación debe ser un número.');
      form.elements.captcha?.focus();
      return;
    }
    if (!captchaFicha) {
      mostrarAlerta(alerta, 'La verificación no se cargó. Recarga la página.');
      await cargarCaptcha();
      return;
    }

    const boton = form.querySelector('button[type="submit"]');
    const textoOriginal = boton.innerHTML;
    boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Entrando...';
    boton.disabled = true;

    // Envía el correo, la contraseña y la verificación al servidor para iniciar sesión
    try {
      const respuesta = await Api.post('/auth/login', {
        correo: form.elements.correo.value.trim(),
        password: form.elements.password.value,
        captcha: respuestaCaptcha,
        captchaToken: captchaFicha
      });

      Api.guardarSesion(respuesta);

      // Lleva a la persona a la pantalla que quería ver o al inicio de su panel
      const destino = parametroUrl('redir')
        || MENSAJES_REDIRECCION[respuesta.usuario.rol]
        || 'index.html';
      window.location.href = destino;
    } catch (error) {
      mostrarAlerta(alerta, error.message);
      // Cada pregunta de verificación sirve una sola vez, por eso siempre se pide una nueva si falla
      await cargarCaptcha();
      form.elements.captcha?.focus();
      boton.innerHTML = textoOriginal;
      boton.disabled = false;
    }
  });
}

/* ----------------------------- Registro ---------------------------- */

// Prepara el formulario para crear una cuenta nueva de cliente
function inicializarRegistro() {
  const form = document.getElementById('formRegistro');
  if (!form) return;

  const alerta = form.parentElement.querySelector('.auth-alerta');

  // Detecta cuando se envía el registro, revisa la contraseña y los términos, y crea la cuenta
  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const password = form.password.value;
    const password2 = form.password2.value;

    // Revisa las reglas básicas antes de enviar los datos al servidor
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

    // Registra al nuevo cliente y lo lleva automáticamente a la pantalla de inicio de sesión
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

// Prepara la pantalla donde la persona pide un enlace para recuperar su contraseña olvidada
function inicializarRecuperar() {
  const form = document.getElementById('formRecuperar');
  if (!form) return;

  const alerta = form.parentElement.querySelector('.auth-alerta');

  // Al enviar su correo, pide al servidor que genere el enlace de recuperación
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

      // En pruebas locales, muestra el enlace directo en pantalla para probar el cambio sin enviar correo real
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

// Prepara la pantalla donde la persona escribe su nueva contraseña después de abrir el enlace de recuperación
function inicializarRestablecer() {
  const form = document.getElementById('formRestablecer');
  if (!form) return;

  const alerta = form.parentElement.querySelector('.auth-alerta');
  const token = parametroUrl('token');
  if (!token) {
    mostrarAlerta(alerta, 'El enlace no es válido. Solicita uno nuevo.');
  }

  // Al guardar la nueva clave, revisa que ambas casillas coincidan y actualiza la contraseña en el servidor
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

/* ------------- Barra superior (usuario conectado) ------------- */

// Arma la dirección correcta hacia el panel del usuario sin importar desde qué sección de la tienda haga clic
function rutaPanel(rol) {
  // Ajusta el prefijo de carpeta para que el enlace funcione igual desde la portada y desde las páginas internas
  const relativa = MENSAJES_REDIRECCION[rol];
  return relativa ? (window.RUTA_BASE || '') + relativa : (window.RUTA_BASE || '') + 'index.html';
}

// Actualiza el botón de cuenta en la barra superior: muestra el nombre si inició sesión y añade el botón de salir
function pintarEstadoSesion() {
  const enlaceCuenta = document.getElementById('btnCuenta');
  if (!enlaceCuenta) return;

  const usuario = Api.getUsuario();
  const autenticado = Api.estaAutenticado() && usuario;

  // Si es un visitante sin sesión iniciada, el botón lleva a la pantalla de inicio de sesión
  if (!autenticado) {
    enlaceCuenta.href = (window.RUTA_BASE || '') + 'login.html';
    enlaceCuenta.title = 'Iniciar sesión';
    return;
  }

  // Si ya inició sesión, muestra su primer nombre y enlaza directo a su panel personal
  const nombre = (usuario.nombre || usuario.correo || '').trim();
  const primerNombre = nombre.split(' ')[0] || 'Mi cuenta';
  const destino = rutaPanel(usuario.rol);

  enlaceCuenta.innerHTML =
    `<i class="fa-solid fa-user"></i> <span class="btn-cuenta__nombre">${escaparHtml(primerNombre)}</span>`;
  enlaceCuenta.href = destino;
  enlaceCuenta.title = 'Ir a mi panel';
  enlaceCuenta.classList.add('btn-cuenta--activa');

  // Agrega el botón de cerrar sesión al lado del nombre solo cuando hay una cuenta abierta
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

// Espera a que la página termine de cargar para encender la pantalla de acceso que corresponda
document.addEventListener('DOMContentLoaded', () => {
  inicializarLogin();
  inicializarRegistro();
  inicializarRecuperar();
  inicializarRestablecer();
  pintarEstadoSesion();
});
