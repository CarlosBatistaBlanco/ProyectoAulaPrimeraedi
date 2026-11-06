/**
 * deseos.js
 * -----------------------------------------------------------------
 * Gestión de la "Lista de Compras" / Favoritos:
 * - Persistencia instantánea en localStorage ('tecniservicios_deseos')
 * - Sincronización en segundo plano con la base de datos si hay sesión
 * - Actualización en tiempo real del contador en el menú
 * - Notificaciones flotantes tipo toast al guardar o quitar
 * - Renderizado en la página dedicada pages/lista_deseos.php
 * -----------------------------------------------------------------
 */

const DESEOS_KEY = 'tecniservicios_deseos';

function leerDeseos() {
  try {
    const raw = localStorage.getItem(DESEOS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    return [];
  }
}

function guardarDeseos(lista) {
  try {
    localStorage.setItem(DESEOS_KEY, JSON.stringify(lista));
  } catch (e) {}
}

function estaEnDeseos(idProducto) {
  const lista = leerDeseos();
  return lista.some(p => Number(p.id_producto) === Number(idProducto));
}

function actualizarContadorDeseos() {
  const lista = leerDeseos();
  document.querySelectorAll('#deseosContador, .deseos-contador').forEach(el => {
    el.textContent = lista.length;
    if (lista.length > 0) {
      el.classList.add('activo');
    } else {
      el.classList.remove('activo');
    }
  });
  actualizarDeseosEnCarrito();
}

function mostrarToast(mensaje, icono = 'fa-solid fa-heart') {
  let toast = document.getElementById('tecnisecureToast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'tecnisecureToast';
    toast.className = 'tecnisecure-toast';
    document.body.appendChild(toast);
  }
  toast.innerHTML = `<i class="${icono}"></i> <span>${mensaje}</span>`;
  toast.classList.add('visible');

  clearTimeout(window.__toastTimeout);
  window.__toastTimeout = setTimeout(() => {
    toast.classList.remove('visible');
  }, 2600);
}

async function toggleDeseo(producto) {
  let lista = leerDeseos();
  const id = Number(producto.id_producto);
  const index = lista.findIndex(p => Number(p.id_producto) === id);
  let agregado = false;

  if (index >= 0) {
    lista.splice(index, 1);
    agregado = false;
    mostrarToast('Eliminado de tu lista de compras', 'fa-regular fa-heart');
  } else {
    lista.push({
      id_producto: id,
      nombre: producto.nombre || 'Producto',
      precio: parseFloat(producto.precio) || 0,
      precio_original: parseFloat(producto.precio_original || producto.precio) || 0,
      descuento_pct: parseFloat(producto.descuento_pct || 0),
      imagen: producto.imagen || 'img/productos/sin-imagen.jpg',
      marca: producto.marca || ''
    });
    agregado = true;
    mostrarToast('¡Guardado en tu lista de compras!', 'fa-solid fa-heart');
  }

  guardarDeseos(lista);
  actualizarContadorDeseos();

  // Sincronizar todos los botones con este id en la página
  document.querySelectorAll(`.btn-favorito[data-id="${id}"]`).forEach(btn => {
    btn.classList.toggle('activo', agregado);
    btn.setAttribute('aria-pressed', agregado ? 'true' : 'false');
  });

  // Si estamos en la página de lista de deseos, re-renderizar
  if (document.getElementById('listaDeseosGrid')) {
    renderizarListaDeseos();
  }

  // Notificar al servidor en segundo plano si está logueado
  try {
    const rutaPhp = window.location.pathname.includes('/pages/')
      ? '../php/toggle_favorito.php'
      : 'php/toggle_favorito.php';

    const formData = new FormData();
    formData.append('id_producto', id);
    fetch(rutaPhp, { method: 'POST', body: formData }).catch(() => {});
  } catch (e) {}

  return agregado;
}

function quitarDeDeseos(idProducto) {
  let lista = leerDeseos();
  lista = lista.filter(p => Number(p.id_producto) !== Number(idProducto));
  guardarDeseos(lista);
  actualizarContadorDeseos();

  document.querySelectorAll(`.btn-favorito[data-id="${idProducto}"]`).forEach(btn => {
    btn.classList.remove('activo');
  });

  mostrarToast('Producto quitado de tu lista', 'fa-regular fa-heart');
  renderizarListaDeseos();
}

function vaciarListaDeseos() {
  if (!confirm('¿Deseas vaciar todos los productos de tu lista de compras?')) return;
  guardarDeseos([]);
  actualizarContadorDeseos();
  document.querySelectorAll('.btn-favorito').forEach(btn => btn.classList.remove('activo'));
  mostrarToast('Lista de compras vaciada', 'fa-regular fa-trash-can');
  renderizarListaDeseos();
}

function moverTodoAlCarrito() {
  const lista = leerDeseos();
  if (lista.length === 0) return;

  if (typeof agregarAlCarrito === 'function') {
    lista.forEach(item => {
      let rutaImg = item.imagen;
      if (window.location.pathname.includes('/pages/') && !rutaImg.startsWith('../') && !rutaImg.startsWith('http')) {
        rutaImg = '../' + rutaImg;
      }
      agregarAlCarrito({
        id_producto: item.id_producto,
        nombre: item.nombre,
        precio: item.precio,
        imagen: rutaImg
      });
    });
    mostrarToast('¡Todos los productos se agregaron al carrito!', 'fa-solid fa-cart-plus');
    if (typeof abrirCarrito === 'function') {
      setTimeout(abrirCarrito, 400);
    }
  }
}

function renderizarListaDeseos() {
  const contenedor = document.getElementById('listaDeseosGrid');
  const emptyState = document.getElementById('listaDeseosVacia');
  const countSpan = document.getElementById('listaDeseosTotal');
  const accionesBarra = document.getElementById('listaDeseosAcciones');

  if (!contenedor) return;

  const lista = leerDeseos();
  if (countSpan) countSpan.textContent = lista.length;

  if (lista.length === 0) {
    contenedor.style.display = 'none';
    if (accionesBarra) accionesBarra.style.display = 'none';
    if (emptyState) emptyState.style.display = 'block';
    return;
  }

  contenedor.style.display = 'grid';
  if (accionesBarra) accionesBarra.style.display = 'flex';
  if (emptyState) emptyState.style.display = 'none';

  contenedor.innerHTML = lista.map(p => {
    const tieneDesc = p.descuento_pct > 0;
    const precioFmt = '$' + Number(p.precio).toLocaleString('es-CO');
    const precioOrigFmt = '$' + Number(p.precio_original).toLocaleString('es-CO');

    let imgPath = p.imagen;
    if (!imgPath.startsWith('../') && !imgPath.startsWith('http')) {
      imgPath = '../' + imgPath;
    }

    return `
      <article class="tarjeta-producto tarjeta-deseo" data-id="${p.id_producto}">
        <div class="tarjeta-producto__imagen">
          ${tieneDesc ? `<span class="badge-rebaja">-${Math.round(p.descuento_pct)}% OFF</span>` : ''}
          <button type="button" class="btn-quitar-deseo" onclick="quitarDeDeseos(${p.id_producto})" title="Quitar de mi lista">
            <i class="fa-solid fa-xmark"></i>
          </button>
          <img src="${imgPath}" alt="${p.nombre}" onerror="this.src='../img/productos/sin-imagen.jpg'">
          <span class="tarjeta-producto__zoom-hint"><i class="fa-solid fa-magnifying-glass-plus"></i> Ampliar</span>
        </div>
        <div class="tarjeta-producto__info">
          ${p.marca ? `<span class="tarjeta-producto__marca">${p.marca}</span>` : ''}
          <h3>${p.nombre}</h3>
          
          <div class="precio-contenedor">
            ${tieneDesc ? `
              <div class="precio-contenedor__fila">
                <span class="precio-original">${precioOrigFmt}</span>
                <span class="badge-descuento">-${Math.round(p.descuento_pct)}%</span>
              </div>
            ` : ''}
            <span class="precio ${tieneDesc ? 'precio--con-descuento' : ''}">${precioFmt}</span>
          </div>

          <div class="tarjeta-deseo__acciones">
            <button type="button" class="btn-agregar" 
                    onclick="agregarDesdeDeseos(${p.id_producto})" 
                    data-id="${p.id_producto}"
                    data-nombre="${p.nombre}"
                    data-precio="${p.precio}"
                    data-imagen="${imgPath}">
              <i class="fa-solid fa-cart-plus"></i> Mover al Carrito
            </button>
          </div>
        </div>
      </article>
    `;
  }).join('');
}

function agregarDesdeDeseos(idProducto) {
  const lista = leerDeseos();
  const prod = lista.find(p => Number(p.id_producto) === Number(idProducto));
  if (!prod) return;

  let imgPath = prod.imagen;
  if (!imgPath.startsWith('../') && !imgPath.startsWith('http')) {
    imgPath = '../' + imgPath;
  }

  if (typeof agregarAlCarrito === 'function') {
    agregarAlCarrito({
      id_producto: prod.id_producto,
      nombre: prod.nombre,
      precio: prod.precio,
      imagen: imgPath
    });
    mostrarToast('¡Producto movido al carrito!', 'fa-solid fa-cart-plus');
    if (typeof abrirCarrito === 'function') {
      setTimeout(abrirCarrito, 400);
    }
  }
}

// Inicialización de eventos al cargar el DOM
document.addEventListener('DOMContentLoaded', () => {
  actualizarContadorDeseos();

  // Marcar como activos todos los botones de favoritos guardados
  const lista = leerDeseos();
  const idsGuardados = new Set(lista.map(p => Number(p.id_producto)));

  document.querySelectorAll('.btn-favorito').forEach(btn => {
    const id = Number(btn.dataset.id);
    if (idsGuardados.has(id)) {
      btn.classList.add('activo');
      btn.setAttribute('aria-pressed', 'true');
    }
  });

  // Delegación de clic para botones favoritos en tarjetas o modal
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('.btn-favorito');
    if (!btn) return;
    e.preventDefault();
    e.stopPropagation();

    const tarjeta = btn.closest('.tarjeta-producto') || btn.closest('.modal-zoom__caja');
    const id = btn.dataset.id;
    const nombre = btn.dataset.nombre || tarjeta?.querySelector('h3')?.textContent.trim() || 'Producto';
    const precio = btn.dataset.precio || tarjeta?.querySelector('.precio')?.textContent.replace(/[^0-9]/g, '') || 0;
    const precioOrig = btn.dataset.precioOriginal || tarjeta?.querySelector('.precio-original')?.textContent.replace(/[^0-9]/g, '') || precio;
    const descPct = btn.dataset.descuento || 0;
    const imgEl = tarjeta?.querySelector('img');
    const imagen = btn.dataset.imagen || imgEl?.getAttribute('src') || '';
    const marca = btn.dataset.marca || tarjeta?.querySelector('.tarjeta-producto__marca')?.textContent.trim() || '';

    toggleDeseo({
      id_producto: id,
      nombre: nombre,
      precio: precio,
      precio_original: precioOrig,
      descuento_pct: descPct,
      imagen: imagen,
      marca: marca
    });
  });

  // Si estamos en la página de lista de deseos, dibujarla
  if (document.getElementById('listaDeseosGrid')) {
    renderizarListaDeseos();
  }

  // Inicializar contador y estado en el panel lateral del carrito
  actualizarDeseosEnCarrito();
});

/* ---------------------------------------------------------------------
   INTEGRACIÓN: LISTA DE DESEOS DENTRO DEL PANEL DEL CARRITO
--------------------------------------------------------------------- */

/**
 * Alterna (despliega / oculta) la lista de productos deseada dentro del carrito
 */
function toggleDeseosCarrito() {
  const panel = document.getElementById('carritoDeseosContenedor');
  const flecha = document.getElementById('flechaDeseosCarrito');
  const btn = document.getElementById('btnToggleDeseosCarrito');
  const texto = document.getElementById('textoToggleDeseos');
  if (!panel) return;

  const estaVisible = panel.style.display !== 'none' && panel.style.display !== '';

  if (estaVisible) {
    panel.style.display = 'none';
    btn?.classList.remove('abierto');
    btn?.setAttribute('aria-expanded', 'false');
    if (flecha) flecha.style.transform = 'rotate(0deg)';
    if (texto) texto.textContent = 'Mostrar lista de productos deseada';
  } else {
    renderizarDeseosEnCarrito();
    panel.style.display = 'block';
    btn?.classList.add('abierto');
    btn?.setAttribute('aria-expanded', 'true');
    if (flecha) flecha.style.transform = 'rotate(180deg)';
    if (texto) texto.textContent = 'Ocultar lista de productos deseada';
  }
}

/**
 * Actualiza el contador y, si el panel está desplegado, refresca los items
 */
function actualizarDeseosEnCarrito() {
  const lista = leerDeseos();
  const countEl = document.getElementById('carritoDeseosCount');
  if (countEl) countEl.textContent = lista.length;

  const panel = document.getElementById('carritoDeseosContenedor');
  if (panel && panel.style.display !== 'none' && panel.style.display !== '') {
    renderizarDeseosEnCarrito();
  }
}

/**
 * Dibuja los productos de la lista de deseos dentro del acordeón del carrito
 */
function renderizarDeseosEnCarrito() {
  const contenedor = document.getElementById('carritoDeseosLista');
  const countEl = document.getElementById('carritoDeseosCount');
  if (!contenedor) return;

  const lista = leerDeseos();
  if (countEl) countEl.textContent = lista.length;

  if (lista.length === 0) {
    contenedor.innerHTML = `
      <div class="carrito-deseos-vacio">
        <i class="fa-regular fa-heart"></i>
        <p>No tienes productos en tu lista de deseos todavía.</p>
      </div>
    `;
    return;
  }

  contenedor.innerHTML = lista.map(p => {
    let imgPath = p.imagen || 'img/productos/sin-imagen.jpg';
    if (window.location.pathname.includes('/pages/') && !imgPath.startsWith('../') && !imgPath.startsWith('http')) {
      imgPath = '../' + imgPath;
    }
    const fallbackImg = window.location.pathname.includes('/pages/') ? '../img/productos/sin-imagen.jpg' : 'img/productos/sin-imagen.jpg';
    const precioFmt = '$' + Number(p.precio).toLocaleString('es-CO');
    const tieneDesc = p.descuento_pct > 0 && p.precio_original > p.precio;
    const precioOrigFmt = tieneDesc ? '$' + Number(p.precio_original).toLocaleString('es-CO') : '';

    return `
      <div class="item-deseo-carrito" data-id="${p.id_producto}">
        <img src="${imgPath}" alt="${p.nombre}" onerror="this.src='${fallbackImg}'">
        <div class="item-deseo-carrito__info">
          ${p.marca ? `<span class="item-deseo-carrito__marca">${p.marca}</span>` : ''}
          <h5 title="${p.nombre}">${p.nombre}</h5>
          <div class="item-deseo-carrito__precio">
            ${tieneDesc ? `<span class="precio-original-mini">${precioOrigFmt}</span>` : ''}
            <span class="precio-actual-mini">${precioFmt}</span>
          </div>
          <button type="button" class="btn-agregar-desde-deseos" onclick="agregarDesdeDeseosEnCarrito(${p.id_producto}, this)" title="Agregar este producto al carrito">
            <i class="fa-solid fa-cart-plus"></i> Agregar al carrito
          </button>
        </div>
      </div>
    `;
  }).join('');
}

/**
 * Agrega un producto desde la lista de deseos directamente al carrito
 */
function agregarDesdeDeseosEnCarrito(idProducto, btnElement) {
  const lista = leerDeseos();
  const prod = lista.find(p => Number(p.id_producto) === Number(idProducto));
  if (!prod) return;

  let imgPath = prod.imagen || 'img/productos/sin-imagen.jpg';
  if (window.location.pathname.includes('/pages/') && !imgPath.startsWith('../') && !imgPath.startsWith('http')) {
    imgPath = '../' + imgPath;
  }

  if (typeof agregarAlCarrito === 'function') {
    agregarAlCarrito({
      id_producto: Number(prod.id_producto),
      nombre: prod.nombre,
      precio: parseFloat(prod.precio),
      imagen: imgPath
    });
  }

  if (btnElement) {
    const textoOriginal = btnElement.innerHTML;
    btnElement.innerHTML = '<i class="fa-solid fa-check"></i> ¡Agregado!';
    btnElement.classList.add('agregado');
    btnElement.disabled = true;
    setTimeout(() => {
      btnElement.innerHTML = textoOriginal;
      btnElement.classList.remove('agregado');
      btnElement.disabled = false;
    }, 1400);
  }

  if (typeof mostrarToast === 'function') {
    mostrarToast(`"${prod.nombre}" agregado al carrito`, 'fa-solid fa-cart-plus');
  }
}
