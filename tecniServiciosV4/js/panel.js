document.addEventListener('DOMContentLoaded', () => {
  const toggle = document.getElementById('panelToggle');
  const sidebar = document.getElementById('panelSidebar');
  const overlay = document.getElementById('panelOverlay');

  function abrir() { sidebar.classList.add('activo'); overlay.classList.add('activo'); }
  function cerrar() { sidebar.classList.remove('activo'); overlay.classList.remove('activo'); }

  if (toggle) toggle.addEventListener('click', abrir);
  if (overlay) overlay.addEventListener('click', cerrar);

  // Dropdown de notificaciones
  const botonNoti = document.getElementById('botonNotificaciones');
  const dropdownNoti = document.getElementById('dropdownNotificaciones');
  if (botonNoti && dropdownNoti) {
    botonNoti.addEventListener('click', (e) => {
      e.stopPropagation();
      dropdownNoti.classList.toggle('activo');
    });
    document.addEventListener('click', (e) => {
      if (!dropdownNoti.contains(e.target)) dropdownNoti.classList.remove('activo');
    });
  }

  // Chat: enviar mensaje y refrescar la conversación cada 4 segundos
  const formChat = document.getElementById('formChat');
  const chatMensajesDiv = document.getElementById('chatMensajes');

  function pintarMensajes(lista) {
    if (!chatMensajesDiv) return;
    chatMensajesDiv.innerHTML = lista.map(m => `
      <div class="chat-burbuja chat-burbuja--${m.remitente === 'cliente' ? 'cliente' : 'asesor'}">
        ${m.mensaje.replace(/</g, '&lt;')}
        <small>${new Date(m.creado_en).toLocaleString('es-CO', {day:'2-digit', month:'2-digit', hour:'2-digit', minute:'2-digit'})}</small>
      </div>`).join('');
    chatMensajesDiv.scrollTop = chatMensajesDiv.scrollHeight;
  }

  async function refrescarChat() {
    const idCliente = chatMensajesDiv?.dataset.cliente;
    if (!idCliente || idCliente === '0') return;
    try {
      const res = await fetch('../../php/obtener_mensajes_chat.php?id_cliente=' + idCliente);
      const datos = await res.json();
      if (Array.isArray(datos) && datos.length) pintarMensajes(datos);
    } catch (e) { /* silencioso: el chat sigue mostrando lo último cargado */ }
  }

  if (chatMensajesDiv) {
    chatMensajesDiv.scrollTop = chatMensajesDiv.scrollHeight;
    setInterval(refrescarChat, 4000);
  }

  if (formChat) {
    formChat.addEventListener('submit', async (e) => {
      e.preventDefault();
      const input = document.getElementById('mensajeChat');
      const idCliente = document.getElementById('idClienteChat').value;
      const csrf = document.getElementById('csrfChat').value;
      if (!input.value.trim()) return;

      const formData = new FormData();
      formData.append('csrf_token', csrf);
      formData.append('id_cliente', idCliente);
      formData.append('mensaje', input.value.trim());

      input.value = '';
      await fetch('../../php/enviar_mensaje_chat.php', { method: 'POST', body: formData });
      refrescarChat();
    });
  }
});
