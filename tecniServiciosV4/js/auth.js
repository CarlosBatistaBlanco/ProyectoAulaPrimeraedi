/**
 * auth.js
 * -----------------------------------------------------------------
 * Comportamiento de las páginas de login/registro/recuperar:
 *   - Botón de mostrar/ocultar contraseña.
 *   - Recargar la imagen del captcha sin recargar la página.
 *   - Validación en tiempo real de contraseña segura y coincidencia
 *     (esto es solo una AYUDA visual para el usuario — la validación
 *     real y obligatoria siempre ocurre también en el servidor).
 * -----------------------------------------------------------------
 */
document.addEventListener('DOMContentLoaded', () => {

  // Mostrar / ocultar contraseña
  document.querySelectorAll('.toggle-password').forEach(boton => {
    boton.addEventListener('click', () => {
      const input = document.getElementById(boton.dataset.target);
      const icono = boton.querySelector('i');
      const mostrando = input.type === 'text';
      input.type = mostrando ? 'password' : 'text';
      icono.classList.toggle('fa-eye', mostrando);
      icono.classList.toggle('fa-eye-slash', !mostrando);
    });
  });

  // Validación en tiempo real de contraseña segura (registro y restablecer)
  const password = document.getElementById('password');
  const password2 = document.getElementById('password2');
  const ayudaPassword = document.getElementById('ayudaPassword');
  const ayudaCoinciden = document.getElementById('ayudaCoinciden');

  function validarPassword() {
    if (!password || !ayudaPassword) return;
    const val = password.value;
    const cumple = val.length >= 8 && /[A-Z]/.test(val) && /[0-9]/.test(val);
    ayudaPassword.textContent = cumple
      ? '✓ Contraseña segura.'
      : 'Debe tener mínimo 8 caracteres, una mayúscula y un número.';
    ayudaPassword.className = 'ayuda-campo ' + (cumple ? 'valido' : 'invalido');
  }

  function validarCoincidencia() {
    if (!password2 || !ayudaCoinciden) return;
    if (password2.value === '') { ayudaCoinciden.textContent = ''; return; }
    const coincide = password.value === password2.value;
    ayudaCoinciden.textContent = coincide ? '✓ Las contraseñas coinciden.' : 'Las contraseñas no coinciden.';
    ayudaCoinciden.className = 'ayuda-campo ' + (coincide ? 'valido' : 'invalido');
  }

  if (password) password.addEventListener('input', () => { validarPassword(); validarCoincidencia(); });
  if (password2) password2.addEventListener('input', validarCoincidencia);

  // Selector de tipo de solicitud: cotización vs. cita (solicitar-servicio.php)
  const opciones = document.querySelectorAll('.tipo-solicitud__opcion');
  const tipoInput = document.getElementById('tipoInput');
  const campoFechaHora = document.getElementById('campoFechaHora');
  const fechaInput = document.getElementById('fecha_preferida');
  const horaInput = document.getElementById('hora_preferida');
  const textoBoton = document.getElementById('textoBoton');

  opciones.forEach(opcion => {
    opcion.addEventListener('click', () => {
      opciones.forEach(o => o.classList.remove('activo'));
      opcion.classList.add('activo');
      const tipo = opcion.dataset.tipo;
      tipoInput.value = tipo;

      const esCita = tipo === 'cita';
      campoFechaHora.style.display = esCita ? 'grid' : 'none';
      if (fechaInput) fechaInput.required = esCita;
      if (horaInput) horaInput.required = esCita;
      textoBoton.textContent = esCita ? 'Agendar cita técnica' : 'Enviar solicitud de cotización';
    });
  });
});
