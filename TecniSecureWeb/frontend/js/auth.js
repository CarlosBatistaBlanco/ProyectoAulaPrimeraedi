/**
 * Este archivo agrega las ayudas interactivas en las pantallas de inicio de sesión, registro, recuperación
 * de contraseña y solicitud de servicio. Permite mostrar u ocultar la clave con el ícono del ojo, revisa
 * mientras se escribe si la contraseña cumple las reglas de seguridad y alterna entre pedir una cotización
 * o agendar una cita técnica con fecha y hora.
 */
// Espera a que la pantalla de acceso, registro o solicitud termine de cargar
document.addEventListener('DOMContentLoaded', () => {

  // Muestra u oculta la contraseña al hacer clic en el ícono del ojo junto a la casilla
  document.querySelectorAll('.toggle-password').forEach(boton => {
    boton.addEventListener('click', () => {
      const input = document.getElementById(boton.dataset.target);
      const icono = boton.querySelector('i');
      const mostrando = input.type === 'text';
      // Cambia la casilla entre texto visible y puntos ocultos, y alterna el ícono del ojo
      input.type = mostrando ? 'password' : 'text';
      icono.classList.toggle('fa-eye', mostrando);
      icono.classList.toggle('fa-eye-slash', !mostrando);
    });
  });

  // Revisión en vivo de contraseña segura en las pantallas de registro y cambio de clave
  const password = document.getElementById('password');
  const password2 = document.getElementById('password2');
  const ayudaPassword = document.getElementById('ayudaPassword');
  const ayudaCoinciden = document.getElementById('ayudaCoinciden');

  // Revisa mientras la persona escribe si la clave tiene al menos 8 letras, una mayúscula y un número
  function validarPassword() {
    if (!password || !ayudaPassword) return;
    const val = password.value;
    const cumple = val.length >= 8 && /[A-Z]/.test(val) && /[0-9]/.test(val);
    ayudaPassword.textContent = cumple
      ? 'Contraseña segura.'
      : 'Debe tener mínimo 8 caracteres, una mayúscula y un número.';
    ayudaPassword.className = 'ayuda-campo ' + (cumple ? 'valido' : 'invalido');
  }

  // Comprueba en vivo que la persona haya repetido exactamente la misma contraseña
  function validarCoincidencia() {
    if (!password2 || !ayudaCoinciden) return;
    if (password2.value === '') { ayudaCoinciden.textContent = ''; return; }
    const coincide = password.value === password2.value;
    ayudaCoinciden.textContent = coincide ? 'Las contraseñas coinciden.' : 'Las contraseñas no coinciden.';
    ayudaCoinciden.className = 'ayuda-campo ' + (coincide ? 'valido' : 'invalido');
  }

  // Activa las revisiones anteriores cada vez que la persona escribe en las casillas de contraseña
  if (password) password.addEventListener('input', () => { validarPassword(); validarCoincidencia(); });
  if (password2) password2.addEventListener('input', validarCoincidencia);

  // Selector de tipo de solicitud en la pantalla de solicitar servicio: cotización o cita técnica
  const opciones = document.querySelectorAll('.tipo-solicitud__opcion');
  const tipoInput = document.getElementById('tipoInput');
  const campoFechaHora = document.getElementById('campoFechaHora');
  const fechaInput = document.getElementById('fecha_preferida');
  const horaInput = document.getElementById('hora_preferida');
  const textoBoton = document.getElementById('textoBoton');

  // Al hacer clic en "Cotización" o "Agendar cita", muestra u oculta las casillas de fecha y hora
  opciones.forEach(opcion => {
    opcion.addEventListener('click', () => {
      opciones.forEach(o => o.classList.remove('activo'));
      opcion.classList.add('activo');
      const tipo = opcion.dataset.tipo;
      tipoInput.value = tipo;

      // Si el cliente eligió cita técnica, exige fecha y hora y cambia el texto del botón principal
      const esCita = tipo === 'cita';
      campoFechaHora.style.display = esCita ? 'grid' : 'none';
      if (fechaInput) fechaInput.required = esCita;
      if (horaInput) horaInput.required = esCita;
      textoBoton.textContent = esCita ? 'Agendar cita técnica' : 'Enviar solicitud de cotización';
    });
  });
});
