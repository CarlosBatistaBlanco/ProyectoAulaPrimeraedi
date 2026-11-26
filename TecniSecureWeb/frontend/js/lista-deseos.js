/**
 * Este archivo administra la lista de productos favoritos (el botón de corazón) de cada persona,
 * manteniéndola separada por cuenta para que nadie vea los productos guardados de otro usuario.
 * Su efecto se nota en el contador del menú superior, en los corazones del catálogo, en la página
 * de lista de deseos y en el desplegable rápido dentro del carrito de compras.
 */

// Prefijo usado en la memoria del navegador para guardar los favoritos separados por usuario
const DESEOS_PREFIJO = 'tecniservicios_deseos';

// Obtiene el espacio privado en la memoria del navegador donde se guardan los favoritos del usuario actual (o visitante sin sesión)
function claveDeseosActual() {
  if (typeof Api !== 'undefined' && Api.estaAutenticado()) {
    const u = Api.getUsuario();
    if (u && u.idUsuario) return `${DESEOS_PREFIJO}_u_${u.idUsuario}`;
  }
  return `${DESEOS_PREFIJO}_anon`;
}

// Comprueba si quien navega tiene sesión abierta con cuenta de cliente
function esClienteAutenticado() {
  return typeof Api !== 'undefined' && Api.estaAutenticado() && Api.getUsuario()?.rol === 'cliente';
}

// Borra cualquier lista antigua compartida para que los favoritos de una persona nunca se mezclen con otra
function limpiarClaveGlobalLegacyDeseos() {
  try {
    localStorage.removeItem(DESEOS_PREFIJO);
  } catch { /* el navegador no permitió modificar la memoria */ }
}

// Lee los productos favoritos guardados en la memoria del navegador para el usuario actual
function leerDeseos() {
  limpiarClaveGlobalLegacyDeseos();
  try {
    const datos = localStorage.getItem(claveDeseosActual());
    return datos ? JSON.parse(datos) : [];
  } catch {
    return [];
  }
}

// Guarda la lista actualizada de favoritos y refresca los contadores y vistas en toda la pantalla
function guardarDeseos(lista) {
  limpiarClaveGlobalLegacyDeseos();
  try {
    localStorage.setItem(claveDeseosActual(), JSON.stringify(lista || []));
  } catch { /* el navegador no permitió guardar en la memoria */ }
  actualizarContadorDeseos();
  if (typeof actualizarDeseosEnCarrito === 'function') actualizarDeseosEnCarrito();
  document.dispatchEvent(new CustomEvent('deseos:actualizada', { detail: lista || [] }));
}

// Acomoda los datos de un producto traído del servidor al formato que usan las tarjetas en pantalla
function normalizarFavoritoApi(p) {
  return {
    id_producto: Number(p.idProducto ?? p.id_producto),
    nombre: p.nombre || '',
    precio: Number(p.precioFinal ?? p.precio ?? 0),
    imagen: p.imagen || 'img/productos/sin-imagen.jpg',
    marca: p.marca || ''
  };
}

// Trae los favoritos guardados en la cuenta del cliente desde el servidor para mantenerlos al día
async function sincronizarDeseosDesdeApi() {
  if (!esClienteAutenticado()) {
    actualizarContadorDeseos();
    marcarFavoritosGuardados();
    renderizarListaDeseos();
    return;
  }
  try {
    const favs = await Api.favoritos();
    const lista = Array.isArray(favs) ? favs.map(normalizarFavoritoApi) : [];
    guardarDeseos(lista);
    renderizarListaDeseos();
    marcarFavoritosGuardados();
  } catch {
    actualizarContadorDeseos();
  }
}

// Actualiza el numerito junto al ícono de corazón en la barra superior
function actualizarContadorDeseos() {
  const contador = document.getElementById('deseosContador');
  if (contador) contador.textContent = leerDeseos().length;
}

// Quita un producto de la lista de deseos (en la cuenta del servidor si inició sesión, y en la vista actual)
async function quitarDeDeseos(idProducto) {
  const id = Number(idProducto);
  if (esClienteAutenticado()) {
    try {
      await Api.alternarFavorito(id);
    } catch { /* si falla la conexión, igual lo quita de la vista actual */ }
  }
  guardarDeseos(leerDeseos().filter((p) => Number(p.id_producto) !== id));
  renderizarListaDeseos();
  marcarFavoritosGuardados();
}

// Revisa que la foto del producto tenga una dirección válida o coloca una imagen por defecto
function resolverRutaImagenDeseo(img) {
  if (!img) return '/img/productos/sin-imagen.jpg';
  const limpia = String(img).trim();
  if (!limpia) return '/img/productos/sin-imagen.jpg';
  if (/^(https?:|data:|blob:)/i.test(limpia)) return limpia;
  return '/' + limpia.replace(/^(\.\.\/|\.\/|\/)+/, '');
}

// Muestra la cuadrícula de productos guardados en la página "Lista de deseos"
function renderizarListaDeseos() {
  const contenedor = document.getElementById('listaDeseosGrid');
  if (!contenedor) return;

  const lista = leerDeseos();
  // Si no hay productos favoritos, muestra un mensaje amigable con botón para ir al catálogo
  if (!lista.length) {
    contenedor.innerHTML = `<div class="catalogo__vacio">
      <i class="fa-solid fa-heart-crack"></i>
      <h3>Tu lista de compras está vacía</h3>
      <p>Marca el corazón de los productos que te interesen y los encontrarás aquí.</p>
      <a href="productos.html" class="btn btn--primario">Ver catálogo</a>
    </div>`;
    return;
  }

  contenedor.innerHTML = lista.map((p) => `
    <article class="tarjeta-producto">
      <div class="tarjeta-producto__imagen">
        <button type="button" class="btn-favorito activo" data-id="${p.id_producto}"
                title="Quitar de mi lista"><i class="fa-solid fa-heart"></i></button>
        <img src="${escaparHtml(resolverRutaImagenDeseo(p.imagen))}" alt="${escaparHtml(p.nombre)}"
             onerror="this.onerror=null;this.src='/img/productos/sin-imagen.jpg'">
      </div>
      <div class="tarjeta-producto__info">
        ${p.marca ? `<span class="tarjeta-producto__marca">${escaparHtml(p.marca)}</span>` : ''}
        <h3>${escaparHtml(p.nombre)}</h3>
        <div class="tarjeta-producto__footer">
          <span class="precio">${formatearPesos(p.precio)}</span>
          <button class="btn-agregar" data-id="${p.id_producto}"
                  data-nombre="${escaparHtml(p.nombre)}" data-precio="${p.precio}"
                  data-imagen="${escaparHtml(p.imagen)}">
            <i class="fa-solid fa-cart-plus"></i> Agregar
          </button>
        </div>
      </div>
    </article>`).join('');

  // Activa el botón de corazón para quitar el producto de la lista
  contenedor.querySelectorAll('.btn-favorito').forEach((boton) => {
    boton.addEventListener('click', () => quitarDeDeseos(boton.dataset.id));
  });

  // Activa el botón "Agregar" para pasar ese favorito directamente al carrito de compras
  contenedor.querySelectorAll('.btn-agregar').forEach((boton) => {
    boton.addEventListener('click', () => {
      const producto = {
        id_producto: Number(boton.dataset.id),
        nombre: boton.dataset.nombre,
        precio: Number(boton.dataset.precio),
        imagen: boton.dataset.imagen
      };
      if (typeof agregarAlCarrito === 'function') agregarAlCarrito(producto, 'deseos');
    });
  });
}

// Actualiza la mini lista desplegable de favoritos que aparece dentro del panel lateral del carrito
function actualizarDeseosEnCarrito() {
  const countEl = document.getElementById('carritoDeseosCount');
  const listaEl = document.getElementById('carritoDeseosLista');
  const lista = leerDeseos();
  if (countEl) countEl.textContent = lista.length;
  if (!listaEl) return;
  if (!lista.length) {
    listaEl.innerHTML = '<p style="color:var(--texto-tenue); font-size:0.82rem; padding:6px 0;">Aún no tienes productos guardados.</p>';
    return;
  }
  listaEl.innerHTML = lista.map((p) => `
    <div style="display:flex; justify-content:space-between; align-items:center; gap:8px; padding:6px 0; border-bottom:1px solid var(--azul-acero); font-size:0.84rem;">
      <span style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escaparHtml(p.nombre)}</span>
      <button type="button" class="btn-gps" onclick="agregarAlCarrito({id_producto:${Number(p.id_producto)}, nombre:'${escaparHtml(p.nombre).replace(/'/g, '&#39;')}', precio:${Number(p.precio)}, imagen:'${escaparHtml(p.imagen).replace(/'/g, '&#39;')}'})">+ Carrito</button>
    </div>`).join('');
}

// Abre o cierra la sección desplegable de favoritos dentro del carrito lateral
function toggleDeseosCarrito() {
  const cont = document.getElementById('carritoDeseosContenedor');
  const btn = document.getElementById('btnToggleDeseosCarrito');
  const txt = document.getElementById('textoToggleDeseos');
  const flecha = document.getElementById('flechaDeseosCarrito');
  if (!cont || !btn) return;
  const abierto = cont.style.display === 'block';
  cont.style.display = abierto ? 'none' : 'block';
  btn.setAttribute('aria-expanded', String(!abierto));
  if (txt) txt.textContent = abierto ? 'Mostrar lista de productos deseada' : 'Ocultar lista de productos deseada';
  if (flecha) flecha.style.transform = abierto ? '' : 'rotate(180deg)';
  if (!abierto) actualizarDeseosEnCarrito();
}

// Pinta encendidos los corazones de los productos que el usuario actual ya tiene guardados
function marcarFavoritosGuardados() {
  const ids = leerDeseos().map((p) => Number(p.id_producto));
  document.querySelectorAll('.btn-favorito[data-id]').forEach((boton) => {
    boton.classList.toggle('activo', ids.includes(Number(boton.dataset.id)));
  });
}

// Activa el botón de corazón en cada tarjeta de producto (el botón "Agregar" ya lo atiende el carrito para no duplicar clics)
function inicializarBotonesTarjetas() {
  document.querySelectorAll('.btn-favorito').forEach((boton) => {
    if (boton.dataset.activo) return;
    boton.dataset.activo = 'true';
    // Detecta cuando el usuario hace clic en el corazón para agregar o quitar el producto de favoritos
    boton.addEventListener('click', async () => {
      const id = Number(boton.dataset.id);
      const lista = leerDeseos();
      const existe = lista.find((p) => Number(p.id_producto) === id);

      // Si es un cliente con sesión activa, guarda el cambio directamente en su cuenta del servidor
      if (esClienteAutenticado()) {
        try {
          const resp = await Api.alternarFavorito(id);
          const agregado = Boolean(resp?.esFavorito);
          boton.classList.toggle('activo', agregado);
          await sincronizarDeseosDesdeApi();
          return;
        } catch {
          /* si falla la conexión, guarda el cambio en la memoria del navegador */
        }
      }

      // Si navega sin sesión (o falla la señal), actualiza los favoritos en la memoria del navegador
      const nueva = existe
        ? lista.filter((p) => Number(p.id_producto) !== id)
        : [...lista, {
            id_producto: id,
            nombre: boton.dataset.nombre,
            precio: Number(boton.dataset.precio),
            imagen: boton.dataset.imagen,
            marca: boton.dataset.marca || ''
          }];
      guardarDeseos(nueva);
      boton.classList.toggle('activo', !existe);
    });
  });

  marcarFavoritosGuardados();
}

// Espera a que la página termine de cargar para preparar el contador y traer los favoritos del usuario
document.addEventListener('DOMContentLoaded', () => {
  const contCarritoDeseos = document.getElementById('carritoDeseosContenedor');
  if (contCarritoDeseos) contCarritoDeseos.style.display = 'none';
  limpiarClaveGlobalLegacyDeseos();
  actualizarContadorDeseos();
  renderizarListaDeseos();
  sincronizarDeseosDesdeApi();
});

// Si la persona inicia sesión o cambia de cuenta, recarga los favoritos que pertenecen a esa cuenta
document.addEventListener('sesion:cambiada', () => {
  sincronizarDeseosDesdeApi();
});

// Si la persona cierra sesión, limpia la vista para mostrar solo la lista de visitante sin cuenta
document.addEventListener('sesion:cerrada', () => {
  actualizarContadorDeseos();
  marcarFavoritosGuardados();
  renderizarListaDeseos();
  actualizarDeseosEnCarrito();
});

// Deja disponibles estas funciones para que los botones del carrito puedan abrir y actualizar los favoritos
window.toggleDeseosCarrito = toggleDeseosCarrito;
window.actualizarDeseosEnCarrito = actualizarDeseosEnCarrito;
