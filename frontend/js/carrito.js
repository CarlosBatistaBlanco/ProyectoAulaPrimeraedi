/**
 * carrito.js — Carrito aislado por usuario, checkout con Google Maps y método de pago
 */

const CARRITO_PREFIJO = 'tecniservicios_carrito';

function claveCarritoActual() {
  if (typeof Api !== 'undefined' && Api.estaAutenticado()) {
    const u = Api.getUsuario();
    if (u && u.idUsuario) return `${CARRITO_PREFIJO}_u_${u.idUsuario}`;
  }
  return `${CARRITO_PREFIJO}_anon`;
}

function limpiarClaveGlobalLegacyCarrito() {
  try {
    localStorage.removeItem(CARRITO_PREFIJO);
  } catch { /* sin acceso */ }
}

/** Lee el carrito privado del usuario actual */
function leerCarrito() {
  limpiarClaveGlobalLegacyCarrito();
  try {
    const datos = localStorage.getItem(claveCarritoActual());
    return datos ? JSON.parse(datos) : [];
  } catch {
    return [];
  }
}

/** Guarda el carrito privado del usuario actual */
function guardarCarrito(carrito) {
  limpiarClaveGlobalLegacyCarrito();
  try {
    localStorage.setItem(claveCarritoActual(), JSON.stringify(carrito || []));
  } catch { /* sin acceso */ }
}

function formatearPrecio(numero) {
  return '$' + Number(numero || 0).toLocaleString('es-CO');
}

function actualizarContador() {
  const carrito = leerCarrito();
  const totalItems = carrito.reduce((suma, item) => suma + item.cantidad, 0);
  const contador = document.getElementById('carritoContador');
  if (contador) contador.textContent = totalItems;
}

function agregarAlCarrito(producto) {
  const carrito = leerCarrito();
  const existente = carrito.find((item) => Number(item.id_producto) === Number(producto.id_producto));

  if (existente) {
    existente.cantidad += 1;
  } else {
    carrito.push({ ...producto, id_producto: Number(producto.id_producto), cantidad: 1 });
  }

  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

/* ------------------- Animacion de "agregar al carrito" ------------------- */

/** Hace saltar el icono del carrito del encabezado y dibuja una onda. */
function animarIconoCarrito() {
  const boton = document.getElementById('btnAbrirCarrito');
  if (!boton) return;

  boton.classList.remove('sacudida');
  // Forzar reflow para poder repetir la animacion sin recargar la pagina.
  void boton.offsetWidth;
  boton.classList.add('sacudida');
  setTimeout(() => boton.classList.remove('sacudida'), 700);

  const caja = boton.getBoundingClientRect();
  const onda = document.createElement('span');
  onda.className = 'carrito-onda';
  onda.style.left = `${caja.left + caja.width / 2}px`;
  onda.style.top = `${caja.top + caja.height / 2}px`;
  document.body.appendChild(onda);
  setTimeout(() => onda.remove(), 700);
}

/** Aviso flotante que desaparece solo. */
function mostrarToastCarrito(texto) {
  const anterior = document.querySelector('.toast-carrito');
  if (anterior) anterior.remove();

  const toast = document.createElement('div');
  toast.className = 'toast-carrito';
  toast.setAttribute('role', 'status');
  toast.innerHTML = `<i class="fa-solid fa-circle-check"></i> <span>${escaparHtml(texto)}</span>`;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2600);
}

/**
 * Anima el boton que se pulso y su tarjeta, y avisa al icono del carrito.
 * Se usa tanto en las tarjetas del catalogo como en la lista de deseos.
 */
function animarAgregado(boton, etiqueta) {
  if (boton && !boton.disabled) {
    const textoOriginal = boton.innerHTML;
    if (boton.dataset.animando === '1') return;
    boton.dataset.animando = '1';
    boton.classList.add('agregando');
    boton.innerHTML = '<i class="fa-solid fa-check"></i> ¡Agregado!';

    const tarjeta = boton.closest('.tarjeta-producto');
    if (tarjeta) {
      tarjeta.classList.add('recien-agregado');
      setTimeout(() => tarjeta.classList.remove('recien-agregado'), 700);
    }

    setTimeout(() => {
      boton.innerHTML = textoOriginal;
      boton.classList.remove('agregando');
      delete boton.dataset.animando;
    }, 1100);
  }
  animarIconoCarrito();
  mostrarToastCarrito(`${etiqueta || 'Producto'} agregado al carrito`);
}

function cambiarCantidad(idProducto, delta) {
  const carrito = leerCarrito().map((item) => {
    if (Number(item.id_producto) === Number(idProducto)) {
      item.cantidad = Math.max(1, item.cantidad + delta);
    }
    return item;
  });
  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

function quitarDelCarrito(idProducto) {
  const carrito = leerCarrito().filter((item) => Number(item.id_producto) !== Number(idProducto));
  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

function renderizarCarrito() {
  const contenedor = document.getElementById('carritoItems');
  const totalEl = document.getElementById('carritoTotal');
  const totalPagoEl = document.getElementById('carritoTotalPago');
  const resumenPago = document.getElementById('carritoResumenPago');
  const btnPagar = document.getElementById('btnIrAPagar');
  if (!contenedor) return;

  const carrito = leerCarrito();
  if (carrito.length === 0) {
    contenedor.innerHTML = `
      <div class="carrito-vacio">
        <i class="fa-solid fa-cart-shopping"></i>
        <p>Tu carrito está vacío.</p>
        <small>Agrega productos desde el catálogo para continuar.</small>
      </div>`;
    if (totalEl) totalEl.textContent = formatearPrecio(0);
    if (totalPagoEl) totalPagoEl.textContent = formatearPrecio(0);
    if (resumenPago) resumenPago.innerHTML = '';
    if (btnPagar) btnPagar.disabled = true;
    return;
  }
  if (btnPagar) btnPagar.disabled = false;

  let total = 0;
  contenedor.innerHTML = carrito.map((item) => {
    const subtotal = item.precio * item.cantidad;
    total += subtotal;
    return `
      <div class="item-carrito">
        <img src="${escaparHtml(item.imagen)}" alt="${escaparHtml(item.nombre)}" onerror="this.src='../img/productos/sin-imagen.jpg'">
        <div class="item-carrito__info">
          <h4>${escaparHtml(item.nombre)}</h4>
          <span class="item-carrito__precio">${formatearPrecio(subtotal)}</span>
          <div class="item-carrito__controles">
            <button type="button" onclick="cambiarCantidad(${Number(item.id_producto)}, -1)" aria-label="Quitar una unidad">−</button>
            <span>${item.cantidad}</span>
            <button type="button" onclick="cambiarCantidad(${Number(item.id_producto)}, 1)" aria-label="Agregar una unidad">+</button>
          </div>
          <br>
          <button type="button" class="item-carrito__quitar" onclick="quitarDelCarrito(${Number(item.id_producto)})">
            <i class="fa-solid fa-trash"></i> Quitar
          </button>
        </div>
      </div>`;
  }).join('');

  const totalTexto = formatearPrecio(total);
  if (totalEl) totalEl.textContent = totalTexto;
  if (totalPagoEl) totalPagoEl.textContent = totalTexto;

  // Resumen corto que se ve al entrar a la vista de pago.
  if (resumenPago) {
    const unidades = carrito.reduce((n, i) => n + i.cantidad, 0);
    resumenPago.innerHTML = `
      <span class="carrito-resumen-pago__titulo">
        <i class="fa-solid fa-box-open"></i> ${carrito.length} producto(s) · ${unidades} unidad(es)
      </span>
      ${carrito.map((i) => `
        <div class="carrito-resumen-pago__fila">
          <span>${escaparHtml(i.nombre)} × ${i.cantidad}</span>
          <strong>${formatearPrecio(i.precio * i.cantidad)}</strong>
        </div>`).join('')}`;
  }
}

/* ------------------- Cambio entre lista de carrito y pago ------------------- */

function irAPagar() {
  if (leerCarrito().length === 0) {
    mostrarToastCarrito('Agrega al menos un producto');
    return;
  }
  const lista = document.getElementById('carritoVistaLista');
  const pago = document.getElementById('carritoVistaPago');
  if (!lista || !pago) return;

  lista.hidden = true;
  pago.hidden = false;
  rellenarDatosCheckout();
  pago.querySelector('.checkout-form input, .checkout-form select')?.focus();
}

function volverAlCarrito() {
  const lista = document.getElementById('carritoVistaLista');
  const pago = document.getElementById('carritoVistaPago');
  if (!lista || !pago) return;
  pago.hidden = true;
  lista.hidden = false;
}

/** Precarga los datos del usuario y su direccion predeterminada. */
function rellenarDatosCheckout() {
  const form = document.getElementById('formCheckout');
  if (!form) return;

  const usuario = (typeof Api !== 'undefined' && Api.getUsuario) ? Api.getUsuario() : null;
  const campos = form.elements;

  if (usuario) {
    if (campos.nombre && !campos.nombre.value.trim()) campos.nombre.value = usuario.nombre || '';
    if (campos.correo && !campos.correo.value.trim()) campos.correo.value = usuario.correo || '';
    if (campos.telefono && !campos.telefono.value.trim()) campos.telefono.value = usuario.telefono || '';
  }

  if (!usuario || typeof Api === 'undefined') return;

  // Direcciones guardadas del usuario (menu Direccion).
  Api.misDirecciones()
    .then((direcciones) => {
      const lista = Array.isArray(direcciones) ? direcciones : (direcciones?.contenido || []);
      const selector = document.getElementById('chkDireccionGuardada');
      const inputDir = document.getElementById('chkDireccion');
      const inputLat = document.getElementById('chkLatitud');
      const inputLon = document.getElementById('chkLongitud');

      if (!selector || !inputDir || !lista.length) return;

      selector.innerHTML = lista.map((d) => `
        <option value="${d.idDireccion}" ${d.predeterminada ? 'selected' : ''}>
          ${escaparHtml(d.etiqueta)} — ${escaparHtml(d.direccion)}
        </option>`).join('');
      selector.hidden = false;

      const aplicar = () => {
        const elegida = lista.find((d) => String(d.idDireccion) === String(selector.value))
          || lista.find((d) => d.predeterminada) || lista[0];
        if (!elegida) return;
        inputDir.value = elegida.direccion || '';
        if (inputLat) inputLat.value = elegida.latitud != null ? String(elegida.latitud) : '';
        if (inputLon) inputLon.value = elegida.longitud != null ? String(elegida.longitud) : '';
        // La direccion puede traer telefono y nombre de quien recibe: si el
        // perfil no los tiene, se completan para no bloquear el pedido.
        const tel = document.getElementById('chkTelefono');
        if (tel && !tel.value.trim() && elegida.telefono) tel.value = elegida.telefono;
        const nom = document.getElementById('chkNombre');
        if (nom && !nom.value.trim() && elegida.nombreRecibe) nom.value = elegida.nombreRecibe;
        actualizarVistaMapaCheckout(elegida.direccion, elegida.latitud, elegida.longitud);
      };

      aplicar();
      selector.addEventListener('change', aplicar);
    })
    .catch(() => { /* sin direcciones guardadas: se escribe a mano */ });
}

/* -------------------- Mapa Google Maps y pago en checkout -------------------- */

function actualizarVistaMapaCheckout(queryTexto, lat, lon) {
  const box = document.getElementById('checkoutMapaBox');
  const iframe = document.getElementById('mapaCheckoutIframe');
  const texto = document.getElementById('checkoutCoordsTexto');
  const enlace = document.getElementById('checkoutEnlaceMaps');
  if (!box || !iframe) return;

  const tieneCoords = Number.isFinite(lat) && Number.isFinite(lon);
  const q = tieneCoords
    ? `${lat.toFixed(6)},${lon.toFixed(6)}`
    : (queryTexto || '').trim();

  if (!q) {
    box.style.display = 'none';
    return;
  }

  const encoded = encodeURIComponent(q);
  iframe.src = `https://maps.google.com/maps?q=${encoded}&z=15&output=embed`;
  if (enlace) enlace.href = `https://www.google.com/maps/search/?api=1&query=${encoded}`;
  if (texto) {
    texto.textContent = tieneCoords ? `GPS: ${lat.toFixed(5)}, ${lon.toFixed(5)}` : q;
  }
  box.style.display = 'block';
}

function inicializarMapaYPagoCheckout() {
  const inputDir = document.getElementById('chkDireccion');
  const inputLat = document.getElementById('chkLatitud');
  const inputLon = document.getElementById('chkLongitud');
  const btnBuscar = document.getElementById('btnBuscarMapaCheckout');
  const btnGps = document.getElementById('btnGpsCheckout');
  const selMetodo = document.getElementById('chkMetodoPago');
  const guia = document.getElementById('checkoutPagoGuia');
  const detalle = document.getElementById('chkDetallePago');

  if (btnBuscar && inputDir) {
    btnBuscar.addEventListener('click', () => {
      if (inputLat) inputLat.value = '';
      if (inputLon) inputLon.value = '';
      actualizarVistaMapaCheckout(inputDir.value);
    });
    inputDir.addEventListener('change', () => {
      if (inputDir.value.trim().length >= 5) {
        actualizarVistaMapaCheckout(inputDir.value);
      }
    });
  }

  if (btnGps) {
    btnGps.addEventListener('click', () => {
      if (!navigator.geolocation) {
        alert('Tu navegador no soporta geolocalización.');
        return;
      }
      const prev = btnGps.innerHTML;
      btnGps.disabled = true;
      btnGps.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const lat = pos.coords.latitude;
          const lon = pos.coords.longitude;
          if (inputLat) inputLat.value = String(lat);
          if (inputLon) inputLon.value = String(lon);
          if (inputDir && !inputDir.value.trim()) {
            inputDir.value = `Ubicación GPS (${lat.toFixed(5)}, ${lon.toFixed(5)})`;
          }
          actualizarVistaMapaCheckout(inputDir?.value, lat, lon);
          btnGps.disabled = false;
          btnGps.innerHTML = prev;
        },
        () => {
          btnGps.disabled = false;
          btnGps.innerHTML = prev;
          const resp = document.getElementById('respuestaPedido');
          if (resp) {
            resp.textContent = 'No pudimos obtener tu GPS; escribe tu dirección y pulsa buscar.';
            resp.className = 'form-respuesta error';
          }
        },
        { enableHighAccuracy: true, timeout: 8000 }
      );
    });
  }

  if (selMetodo && guia && detalle) {
    const textosPago = {
      'Transferencia / Nequi': 'Cuenta ahorros Bancolombia / Nequi 300 123 4567. Puedes anotar tu comprobante abajo.',
      'Efectivo contraentrega': 'Pagas en efectivo al recibir o instalar tu pedido. Indica si necesitas devuelta.',
      'Tarjeta': 'Pago con tarjeta débito o crédito (datáfono al entregar o enlace seguro desde Mis pedidos).'
    };
    selMetodo.addEventListener('change', () => {
      const m = selMetodo.value;
      if (!m) {
        guia.style.display = 'none';
        detalle.style.display = 'none';
        return;
      }
      guia.textContent = textosPago[m] || '';
      guia.style.display = 'block';
      detalle.style.display = 'block';
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  limpiarClaveGlobalLegacyCarrito();
  actualizarContador();
  renderizarCarrito();
  inicializarMapaYPagoCheckout();

  // Delegacion de eventos: las tarjetas del catalogo se crean despues de
  // cargar la API, asi que no se pueden enganchar una por una al arrancar.
  if (!document.body.dataset.carritoDelegado) {
    document.body.dataset.carritoDelegado = '1';
    document.addEventListener('click', (e) => {
      const boton = e.target.closest('.btn-agregar');
      if (!boton || boton.disabled) return;
      agregarAlCarrito({
        id_producto: parseInt(boton.dataset.id, 10),
        nombre: boton.dataset.nombre,
        precio: parseFloat(boton.dataset.precio),
        imagen: boton.dataset.imagen
      });
      animarAgregado(boton, boton.dataset.nombre);
    });
  }

  const btnAbrir = document.getElementById('btnAbrirCarrito');
  const btnCerrar = document.getElementById('cerrarCarrito');
  const overlay = document.getElementById('carritoOverlay');

  if (btnAbrir) {
    btnAbrir.addEventListener('click', (e) => {
      if (document.getElementById('carritoPanel')) {
        e.preventDefault();
        abrirCarrito();
      }
    });
  }
  if (btnCerrar) btnCerrar.addEventListener('click', cerrarCarrito);
  if (overlay) overlay.addEventListener('click', cerrarCarrito);

  document.getElementById('cerrarCarritoPago')?.addEventListener('click', cerrarCarrito);
  document.getElementById('btnIrAPagar')?.addEventListener('click', irAPagar);
  document.getElementById('btnVolverAlCarrito')?.addEventListener('click', volverAlCarrito);
  document.getElementById('btnGestionarDirecciones')?.addEventListener('click', (e) => {
    e.preventDefault();
    const base = (typeof RUTA_BASE !== 'undefined' ? RUTA_BASE : window.RUTA_BASE) || '';
    const panel = (typeof Api !== 'undefined' && Api.getUsuario && Api.getUsuario())
      ? Api.getUsuario().rol
      : 'cliente';
    const destino = ['administrador', 'empleado', 'cliente'].includes(panel) ? panel : 'cliente';
    window.location.href = `${base}panel/${destino}/direcciones.html`;
  });

  const formCheckout = document.getElementById('formCheckout');
  const respuestaPedido = document.getElementById('respuestaPedido');

  if (formCheckout) {
    formCheckout.addEventListener('submit', async (e) => {
      e.preventDefault();
      const carrito = leerCarrito();

      if (carrito.length === 0) {
        respuestaPedido.textContent = 'Agrega al menos un producto antes de confirmar.';
        respuestaPedido.className = 'form-respuesta error';
        return;
      }

      const formData = new FormData(formCheckout);
      const direccion = String(formData.get('direccion') || '').trim();
      const metodoPago = String(formData.get('metodoPago') || '').trim();

      if (!direccion) {
        respuestaPedido.textContent = 'Ingresa tu dirección de entrega o selecciona tu ubicación en el mapa.';
        respuestaPedido.className = 'form-respuesta error';
        return;
      }
      if (!metodoPago) {
        respuestaPedido.textContent = 'Selecciona un método de pago (Transferencia, Efectivo o Tarjeta).';
        respuestaPedido.className = 'form-respuesta error';
        return;
      }

      const latRaw = formData.get('latitud');
      const lonRaw = formData.get('longitud');
      const latitud = latRaw ? parseFloat(latRaw) : null;
      const longitud = lonRaw ? parseFloat(lonRaw) : null;

      const boton = formCheckout.querySelector('button[type="submit"]');
      const textoOriginal = boton.innerHTML;
      boton.innerHTML = 'Procesando...';
      boton.disabled = true;

      try {
        const datos = await Api.registrarPedido({
          nombre: String(formData.get('nombre') || '').trim(),
          correo: String(formData.get('correo') || '').trim(),
          telefono: String(formData.get('telefono') || '').trim(),
          direccion,
          latitud: Number.isFinite(latitud) ? latitud : null,
          longitud: Number.isFinite(longitud) ? longitud : null,
          metodoPago,
          detallePago: String(formData.get('detallePago') || '').trim() || null,
          carrito: carrito.map((item) => ({
            idProducto: item.id_producto,
            cantidad: item.cantidad
          }))
        });

        respuestaPedido.textContent = `¡Pedido #${datos.idPedido} registrado (${metodoPago})! Total: ${formatearPesos(datos.total)}`;
        respuestaPedido.className = 'form-respuesta ok';

        localStorage.removeItem(claveCarritoActual());
        actualizarContador();
        renderizarCarrito();
        formCheckout.reset();
        const boxMapa = document.getElementById('checkoutMapaBox');
        if (boxMapa) boxMapa.style.display = 'none';
        const guia = document.getElementById('checkoutPagoGuia');
        if (guia) guia.style.display = 'none';
      } catch (error) {
        respuestaPedido.textContent = error.message || 'Error de conexión con el servidor.';
        respuestaPedido.className = 'form-respuesta error';
      } finally {
        boton.innerHTML = textoOriginal;
        boton.disabled = false;
      }
    });
  }
});

document.addEventListener('sesion:cambiada', () => {
  actualizarContador();
  renderizarCarrito();
});

document.addEventListener('sesion:cerrada', () => {
  actualizarContador();
  renderizarCarrito();
  const form = document.getElementById('formCheckout');
  if (form) form.reset();
});

function abrirCarrito() {
  volverAlCarrito();
  document.getElementById('carritoPanel')?.classList.add('activo');
  document.getElementById('carritoOverlay')?.classList.add('activo');
  if (typeof actualizarDeseosEnCarrito === 'function') {
    actualizarDeseosEnCarrito();
  }
}

function cerrarCarrito() {
  document.getElementById('carritoPanel')?.classList.remove('activo');
  document.getElementById('carritoOverlay')?.classList.remove('activo');
}

window.agregarAlCarrito = agregarAlCarrito;
window.cambiarCantidad = cambiarCantidad;
window.quitarDelCarrito = quitarDelCarrito;
window.actualizarContador = actualizarContador;
window.abrirCarrito = abrirCarrito;
window.cerrarCarrito = cerrarCarrito;
window.animarAgregado = animarAgregado;
window.animarIconoCarrito = animarIconoCarrito;
window.mostrarToastCarrito = mostrarToastCarrito;
window.irAPagar = irAPagar;
window.volverAlCarrito = volverAlCarrito;
