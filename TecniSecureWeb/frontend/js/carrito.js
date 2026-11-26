/**
 * Este archivo controla el carrito de compras que se desliza desde el lado derecho de la tienda.
 * Se encarga de guardar los productos elegidos por cada persona de forma separada en la memoria del
 * navegador, mostrar sus fotos, cantidades y totales, ayudar a elegir o ubicar la dirección de entrega
 * en el mapa y enviar el pedido final al servidor con el método de pago escogido.
 */

// Prefijo usado en la memoria del navegador para guardar el carrito separado por cada cuenta o visitante
const CARRITO_PREFIJO = 'tecniservicios_carrito';

// Calcula el espacio privado en la memoria del navegador donde se guardan los productos del usuario actual
function claveCarritoActual() {
  if (typeof Api !== 'undefined' && Api.estaAutenticado()) {
    const u = Api.getUsuario();
    if (u && u.idUsuario) return `${CARRITO_PREFIJO}_u_${u.idUsuario}`;
  }
  return `${CARRITO_PREFIJO}_anon`;
}

// Elimina cualquier carrito antiguo compartido para que las compras de una persona nunca aparezcan en la cuenta de otra
function limpiarClaveGlobalLegacyCarrito() {
  try {
    localStorage.removeItem(CARRITO_PREFIJO);
  } catch { /* el navegador no permitió modificar la memoria */ }
}

// Lee los productos guardados en el carrito privado del usuario actual
function leerCarrito() {
  limpiarClaveGlobalLegacyCarrito();
  try {
    const datos = localStorage.getItem(claveCarritoActual());
    return datos ? JSON.parse(datos) : [];
  } catch {
    return [];
  }
}

// Guarda la lista actualizada de productos en el carrito privado del usuario actual
function guardarCarrito(carrito) {
  limpiarClaveGlobalLegacyCarrito();
  try {
    localStorage.setItem(claveCarritoActual(), JSON.stringify(carrito || []));
  } catch { /* el navegador no permitió guardar en la memoria */ }
}

// Da formato de pesos colombianos a un precio (por ejemplo 150000 se muestra como $150.000)
function formatearPrecio(numero) {
  return '$' + Number(numero || 0).toLocaleString('es-CO');
}

// Asegura que la foto de cada producto se vea bien desde cualquier página o muestra una imagen por defecto
function normalizarImagenCarrito(img) {
  if (!img) return '/img/productos/sin-imagen.jpg';
  const limpia = String(img).trim();
  if (!limpia) return '/img/productos/sin-imagen.jpg';
  if (/^(https?:|data:|blob:)/i.test(limpia)) return limpia;
  const sinRelativos = limpia.replace(/^(\.\.\/|\.\/|\/)+/, '');
  return '/' + sinRelativos;
}

// Actualiza el numerito de cantidad total sobre el ícono del carrito en la barra superior
function actualizarContador() {
  const carrito = leerCarrito();
  const totalItems = carrito.reduce((suma, item) => suma + item.cantidad, 0);
  const contador = document.getElementById('carritoContador');
  if (contador) contador.textContent = totalItems;
}

// Evita que un mismo clic accidental sume dos unidades seguidas en menos de un tercio de segundo
let ultimoAgregado = { clave: '', momento: 0 };

// Agrega una unidad del producto elegido al carrito y refresca el contador y el listado
function agregarAlCarrito(producto, origen) {
  const ahora = Date.now();
  const clave = `${origen || 'directo'}:${producto.id_producto}`;
  if (clave === ultimoAgregado.clave && ahora - ultimoAgregado.momento < 300) {
    return;
  }
  ultimoAgregado = { clave, momento: ahora };

  const carrito = leerCarrito();
  const imagenNormalizada = normalizarImagenCarrito(producto.imagen);
  const existente = carrito.find((item) => Number(item.id_producto) === Number(producto.id_producto));

  // Si el producto ya estaba en el carrito suma 1 unidad; si es nuevo lo agrega al final
  if (existente) {
    existente.cantidad += 1;
    existente.imagen = imagenNormalizada;
  } else {
    carrito.push({
      ...producto,
      id_producto: Number(producto.id_producto),
      imagen: imagenNormalizada,
      cantidad: 1
    });
  }

  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

/* ------------------- Animación al agregar al carrito ------------------- */

// Hace saltar el ícono del carrito en la barra superior y dibuja una onda luminosa alrededor
function animarIconoCarrito() {
  const boton = document.getElementById('btnAbrirCarrito');
  if (!boton) return;

  boton.classList.remove('sacudida');
  // Reinicia el movimiento visual para poder repetir el salto cada vez que se agrega un producto
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

// Muestra un aviso flotante breve confirmando que el producto entró al carrito y desaparece solo
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

// Cambia brevemente el botón pulsado a "¡Agregado!", resalta la tarjeta y dispara la animación del carrito
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

// Aumenta o disminuye la cantidad de un producto dentro del carrito (mínimo 1 unidad)
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

// Quita por completo un producto del carrito y recalcula el total
function quitarDelCarrito(idProducto) {
  const carrito = leerCarrito().filter((item) => Number(item.id_producto) !== Number(idProducto));
  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

// Muestra la lista de productos dentro del panel lateral del carrito, el total y el resumen de compra
function renderizarCarrito() {
  const contenedor = document.getElementById('carritoItems');
  const totalEl = document.getElementById('carritoTotal');
  const totalPagoEl = document.getElementById('carritoTotalPago');
  const resumenPago = document.getElementById('carritoResumenPago');
  const btnPagar = document.getElementById('btnIrAPagar');
  if (!contenedor) return;

  const carrito = leerCarrito();
  // Si no hay productos, muestra un aviso de carrito vacío y desactiva el botón para continuar al pago
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
    const urlImagen = normalizarImagenCarrito(item.imagen);
    return `
      <div class="item-carrito">
        <img src="${escaparHtml(urlImagen)}" alt="${escaparHtml(item.nombre)}" onerror="this.onerror=null;this.src='/img/productos/sin-imagen.jpg'">
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

  // Resumen corto de los productos que se ve al entrar a la pantalla de pago
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

/* ------------------- Cambio entre carrito y pantalla de pago ------------------- */

// Cambia el panel lateral del carrito al paso de datos de entrega y método de pago
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

// Regresa desde la pantalla de pago a la lista de productos del carrito
function volverAlCarrito() {
  const lista = document.getElementById('carritoVistaLista');
  const pago = document.getElementById('carritoVistaPago');
  if (!lista || !pago) return;
  pago.hidden = true;
  lista.hidden = false;
}

// Rellena automáticamente los datos de la persona conectada y sus direcciones guardadas para agilizar la compra
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

  // Trae las direcciones guardadas de la persona (desde la sección Dirección de su panel)
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

      // Copia la dirección elegida del menú desplegable a las casillas del pedido y actualiza el mapa
      const aplicar = () => {
        const elegida = lista.find((d) => String(d.idDireccion) === String(selector.value))
          || lista.find((d) => d.predeterminada) || lista[0];
        if (!elegida) return;
        inputDir.value = elegida.direccion || '';
        if (inputLat) inputLat.value = elegida.latitud != null ? String(elegida.latitud) : '';
        if (inputLon) inputLon.value = elegida.longitud != null ? String(elegida.longitud) : '';
        // Si la dirección tiene teléfono o nombre de quien recibe, los completa si esas casillas estaban vacías
        const tel = document.getElementById('chkTelefono');
        if (tel && !tel.value.trim() && elegida.telefono) tel.value = elegida.telefono;
        const nom = document.getElementById('chkNombre');
        if (nom && !nom.value.trim() && elegida.nombreRecibe) nom.value = elegida.nombreRecibe;
        actualizarVistaMapaCheckout(elegida.direccion, elegida.latitud, elegida.longitud);
      };

      aplicar();
      selector.addEventListener('change', aplicar);
    })
    .catch(() => { /* si no tiene direcciones guardadas, puede escribirla manualmente */ });
}

/* -------------------- Mapa Google Maps y método de pago -------------------- */

// Muestra una vista previa en Google Maps de la dirección escrita o de la ubicación por GPS del cliente
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

// Activa el botón de buscar en el mapa, la ubicación por GPS y las instrucciones de cada forma de pago
function inicializarMapaYPagoCheckout() {
  const inputDir = document.getElementById('chkDireccion');
  const inputLat = document.getElementById('chkLatitud');
  const inputLon = document.getElementById('chkLongitud');
  const btnBuscar = document.getElementById('btnBuscarMapaCheckout');
  const btnGps = document.getElementById('btnGpsCheckout');
  const selMetodo = document.getElementById('chkMetodoPago');
  const guia = document.getElementById('checkoutPagoGuia');
  const detalle = document.getElementById('chkDetallePago');

  // Busca la dirección escrita en el mapa cuando el cliente hace clic en la lupa o termina de escribir
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

  // Botón de GPS: pide la ubicación actual al navegador para marcarla directamente en el mapa
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

  // Muestra instrucciones claras (Nequi, contraentrega o tarjeta) cuando el cliente elige cómo pagar
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

// Espera a que la página termine de cargar para conectar el carrito, los botones "Agregar" y el envío del pedido
document.addEventListener('DOMContentLoaded', () => {
  limpiarClaveGlobalLegacyCarrito();
  actualizarContador();
  renderizarCarrito();
  inicializarMapaYPagoCheckout();

  // Detecta clics en cualquier botón "Agregar" de la tienda, incluso en productos que aparecen después de buscar
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
      }, 'tarjeta');
      animarAgregado(boton, boton.dataset.nombre);
    });
  }

  const btnAbrir = document.getElementById('btnAbrirCarrito');
  const btnCerrar = document.getElementById('cerrarCarrito');
  const overlay = document.getElementById('carritoOverlay');

  // Abre el panel lateral del carrito al hacer clic en el ícono de la barra superior
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

  // Botones internos del carrito: cerrar pago, ir a pagar, volver a la lista y abrir libreta de direcciones
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

  // Formulario final de confirmación de pedido: revisa las casillas y registra la compra en el servidor
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

      // Exige dirección de entrega y método de pago antes de registrar el pedido
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

      // Envía el pedido completo al servidor; si se confirma, vacía el carrito y muestra el número de orden
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

// Si la persona inicia sesión o cambia de cuenta, refresca el carrito para mostrar solo el suyo
document.addEventListener('sesion:cambiada', () => {
  actualizarContador();
  renderizarCarrito();
});

// Si cierra sesión, vuelve al carrito de visitante y limpia lo que estaba escrito en la pantalla de pago
document.addEventListener('sesion:cerrada', () => {
  actualizarContador();
  renderizarCarrito();
  const form = document.getElementById('formCheckout');
  if (form) form.reset();
});

// Despliega el panel lateral del carrito y actualiza la sección desplegable de favoritos
function abrirCarrito() {
  volverAlCarrito();
  document.getElementById('carritoPanel')?.classList.add('activo');
  document.getElementById('carritoOverlay')?.classList.add('activo');
  if (typeof actualizarDeseosEnCarrito === 'function') {
    actualizarDeseosEnCarrito();
  }
}

// Oculta el panel lateral del carrito y su fondo oscurecido
function cerrarCarrito() {
  document.getElementById('carritoPanel')?.classList.remove('activo');
  document.getElementById('carritoOverlay')?.classList.remove('activo');
}

// Deja disponibles las acciones del carrito para que los botones de la tienda puedan usarlas
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
