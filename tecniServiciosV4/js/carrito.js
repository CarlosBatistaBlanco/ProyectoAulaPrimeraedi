/**
 * carrito.js
 * -----------------------------------------------------------------
 * Carrito de compras del lado del cliente.
 * - El carrito se guarda en localStorage (persiste al recargar).
 * - Cada botón ".btn-agregar" en las tarjetas de producto agrega
 *   el producto al carrito usando sus atributos data-*.
 * - El panel lateral (#carritoPanel) muestra los productos.
 * - Al confirmar el pedido, se envía por fetch() en JSON hacia
 *   php/procesar_pedido.php, que guarda todo en MySQL.
 * -----------------------------------------------------------------
 */

const CARRITO_KEY = 'tecniservicios_carrito';

/** Lee el carrito guardado en localStorage (o arreglo vacío) */
function leerCarrito() {
  const datos = localStorage.getItem(CARRITO_KEY);
  return datos ? JSON.parse(datos) : [];
}

/** Guarda el carrito en localStorage */
function guardarCarrito(carrito) {
  localStorage.setItem(CARRITO_KEY, JSON.stringify(carrito));
}

/** Formatea un número como precio en pesos: 249900 -> $249.900 */
function formatearPrecio(numero) {
  return '$' + Number(numero).toLocaleString('es-CO');
}

/** Actualiza el contador rojo del ícono del carrito en el menú */
function actualizarContador() {
  const carrito = leerCarrito();
  const totalItems = carrito.reduce((suma, item) => suma + item.cantidad, 0);
  const contador = document.getElementById('carritoContador');
  if (contador) contador.textContent = totalItems;
}

/** Agrega un producto al carrito (o incrementa su cantidad si ya existe) */
function agregarAlCarrito(producto) {
  const carrito = leerCarrito();
  const existente = carrito.find(item => item.id_producto === producto.id_producto);

  if (existente) {
    existente.cantidad += 1;
  } else {
    carrito.push({ ...producto, cantidad: 1 });
  }

  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

/** Cambia la cantidad de un producto ya agregado (+1 / -1) */
function cambiarCantidad(idProducto, delta) {
  let carrito = leerCarrito();
  carrito = carrito.map(item => {
    if (item.id_producto === idProducto) {
      item.cantidad = Math.max(1, item.cantidad + delta);
    }
    return item;
  });
  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

/** Elimina un producto del carrito por completo */
function quitarDelCarrito(idProducto) {
  let carrito = leerCarrito();
  carrito = carrito.filter(item => item.id_producto !== idProducto);
  guardarCarrito(carrito);
  actualizarContador();
  renderizarCarrito();
}

/** Dibuja los productos del carrito dentro del panel lateral */
function renderizarCarrito() {
  const contenedor = document.getElementById('carritoItems');
  const totalEl = document.getElementById('carritoTotal');
  if (!contenedor || !totalEl) return; // el panel solo existe en pages/productos.php

  const carrito = leerCarrito();

  if (carrito.length === 0) {
    contenedor.innerHTML = '<p style="color:#8ea3bd; text-align:center; margin-top:30px;">Tu carrito está vacío.</p>';
    totalEl.textContent = formatearPrecio(0);
    return;
  }

  let total = 0;
  contenedor.innerHTML = carrito.map(item => {
    const subtotal = item.precio * item.cantidad;
    total += subtotal;
    return `
      <div class="item-carrito">
        <img src="${item.imagen}" alt="${item.nombre}" onerror="this.src='../img/productos/sin-imagen.jpg'">
        <div class="item-carrito__info">
          <h4>${item.nombre}</h4>
          <span class="item-carrito__precio">${formatearPrecio(subtotal)}</span>
          <div class="item-carrito__controles">
            <button onclick="cambiarCantidad(${item.id_producto}, -1)">-</button>
            <span>${item.cantidad}</span>
            <button onclick="cambiarCantidad(${item.id_producto}, 1)">+</button>
          </div>
          <button class="item-carrito__quitar" onclick="quitarDelCarrito(${item.id_producto})">
            <i class="fa-solid fa-trash"></i> Quitar
          </button>
        </div>
      </div>
    `;
  }).join('');

  totalEl.textContent = formatearPrecio(total);
}

/* ---------------------------------------------------------------------
   EVENTOS
--------------------------------------------------------------------- */
document.addEventListener('DOMContentLoaded', () => {
  actualizarContador();
  renderizarCarrito();

  // Botones "Agregar" en cualquier tarjeta de producto (index o catálogo)
  document.querySelectorAll('.btn-agregar').forEach(boton => {
    boton.addEventListener('click', () => {
      agregarAlCarrito({
        id_producto: parseInt(boton.dataset.id, 10),
        nombre: boton.dataset.nombre,
        precio: parseFloat(boton.dataset.precio),
        imagen: boton.dataset.imagen,
      });

      // Pequeña confirmación visual en el botón
      const textoOriginal = boton.innerHTML;
      boton.innerHTML = '<i class="fa-solid fa-check"></i> Agregado';
      setTimeout(() => (boton.innerHTML = textoOriginal), 1200);

      abrirCarrito();
    });
  });

  // Abrir / cerrar panel del carrito
  const btnAbrir = document.getElementById('btnAbrirCarrito');
  const btnCerrar = document.getElementById('cerrarCarrito');
  const overlay = document.getElementById('carritoOverlay');

  if (btnAbrir) {
    btnAbrir.addEventListener('click', (e) => {
      // Si el botón apunta a pages/productos.php y ya estamos ahí, solo abrimos el panel
      if (document.getElementById('carritoPanel')) {
        e.preventDefault();
        abrirCarrito();
      }
      // Si estamos en index.php, se deja el enlace normal para ir al catálogo
    });
  }
  if (btnCerrar) btnCerrar.addEventListener('click', cerrarCarrito);
  if (overlay) overlay.addEventListener('click', cerrarCarrito);

  // Envío del formulario de checkout (confirmar pedido)
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
      const payload = {
        nombre: formData.get('nombre'),
        correo: formData.get('correo'),
        telefono: formData.get('telefono'),
        direccion: formData.get('direccion'),
        carrito: carrito.map(item => ({
          id_producto: item.id_producto,
          cantidad: item.cantidad,
          precio: item.precio,
        })),
      };

      const boton = formCheckout.querySelector('button[type="submit"]');
      const textoOriginal = boton.innerHTML;
      boton.innerHTML = 'Procesando...';
      boton.disabled = true;

      try {
        const respuesta = await fetch('../php/procesar_pedido.php', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });
        const datos = await respuesta.json();

        respuestaPedido.textContent = datos.mensaje;
        respuestaPedido.className = 'form-respuesta ' + (datos.ok ? 'ok' : 'error');

        if (datos.ok) {
          localStorage.removeItem(CARRITO_KEY);
          actualizarContador();
          renderizarCarrito();
          formCheckout.reset();
        }
      } catch (error) {
        respuestaPedido.textContent = 'Error de conexión con el servidor.';
        respuestaPedido.className = 'form-respuesta error';
      } finally {
        boton.innerHTML = textoOriginal;
        boton.disabled = false;
      }
    });
  }
});

function abrirCarrito() {
  document.getElementById('carritoPanel')?.classList.add('activo');
  document.getElementById('carritoOverlay')?.classList.add('activo');
}
function cerrarCarrito() {
  document.getElementById('carritoPanel')?.classList.remove('activo');
  document.getElementById('carritoOverlay')?.classList.remove('activo');
}
