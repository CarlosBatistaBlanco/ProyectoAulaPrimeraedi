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
  if (!contenedor || !totalEl) return;

  const carrito = leerCarrito();
  if (carrito.length === 0) {
    contenedor.innerHTML = '<p style="color:#8ea3bd; text-align:center; margin-top:20px;">Tu carrito está vacío.</p>';
    totalEl.textContent = formatearPrecio(0);
    return;
  }

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
            <button type="button" onclick="cambiarCantidad(${Number(item.id_producto)}, -1)">-</button>
            <span>${item.cantidad}</span>
            <button type="button" onclick="cambiarCantidad(${Number(item.id_producto)}, 1)">+</button>
          </div>
          <button type="button" class="item-carrito__quitar" onclick="quitarDelCarrito(${Number(item.id_producto)})">
            <i class="fa-solid fa-trash"></i> Quitar
          </button>
        </div>
      </div>`;
  }).join('');

  totalEl.textContent = formatearPrecio(total);
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

  document.querySelectorAll('.btn-agregar').forEach((boton) => {
    if (boton.dataset.activo) return;
    boton.dataset.activo = 'true';
    boton.addEventListener('click', () => {
      agregarAlCarrito({
        id_producto: parseInt(boton.dataset.id, 10),
        nombre: boton.dataset.nombre,
        precio: parseFloat(boton.dataset.precio),
        imagen: boton.dataset.imagen
      });
      const textoOriginal = boton.innerHTML;
      boton.innerHTML = '<i class="fa-solid fa-check"></i> Agregado';
      setTimeout(() => { boton.innerHTML = textoOriginal; }, 1200);
      abrirCarrito();
    });
  });

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
