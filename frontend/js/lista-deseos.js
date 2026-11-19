/**
 * lista-deseos.js — Lista de deseos privada por usuario
 * -----------------------------------------------------------------
 * - Aísla la lista en localStorage según el usuario autenticado
 *   (tecniservicios_deseos_u_<id> o tecniservicios_deseos_anon).
 * - Si el usuario es un cliente autenticado, sincroniza exclusivamente
 *   sus favoritos desde /api/cliente/favoritos para garantizar privacidad.
 */

const DESEOS_PREFIJO = 'tecniservicios_deseos';

function claveDeseosActual() {
  if (typeof Api !== 'undefined' && Api.estaAutenticado()) {
    const u = Api.getUsuario();
    if (u && u.idUsuario) return `${DESEOS_PREFIJO}_u_${u.idUsuario}`;
  }
  return `${DESEOS_PREFIJO}_anon`;
}

function esClienteAutenticado() {
  return typeof Api !== 'undefined' && Api.estaAutenticado() && Api.getUsuario()?.rol === 'cliente';
}

function limpiarClaveGlobalLegacyDeseos() {
  try {
    localStorage.removeItem(DESEOS_PREFIJO);
  } catch { /* sin acceso a almacenamiento */ }
}

function leerDeseos() {
  limpiarClaveGlobalLegacyDeseos();
  try {
    const datos = localStorage.getItem(claveDeseosActual());
    return datos ? JSON.parse(datos) : [];
  } catch {
    return [];
  }
}

function guardarDeseos(lista) {
  limpiarClaveGlobalLegacyDeseos();
  try {
    localStorage.setItem(claveDeseosActual(), JSON.stringify(lista || []));
  } catch { /* sin acceso a almacenamiento */ }
  actualizarContadorDeseos();
  if (typeof actualizarDeseosEnCarrito === 'function') actualizarDeseosEnCarrito();
  document.dispatchEvent(new CustomEvent('deseos:actualizada', { detail: lista || [] }));
}

function normalizarFavoritoApi(p) {
  return {
    id_producto: Number(p.idProducto ?? p.id_producto),
    nombre: p.nombre || '',
    precio: Number(p.precioFinal ?? p.precio ?? 0),
    imagen: p.imagen || 'img/productos/sin-imagen.jpg',
    marca: p.marca || ''
  };
}

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

function actualizarContadorDeseos() {
  const contador = document.getElementById('deseosContador');
  if (contador) contador.textContent = leerDeseos().length;
}

async function quitarDeDeseos(idProducto) {
  const id = Number(idProducto);
  if (esClienteAutenticado()) {
    try {
      await Api.alternarFavorito(id);
    } catch { /* continúa limpiando vista local */ }
  }
  guardarDeseos(leerDeseos().filter((p) => Number(p.id_producto) !== id));
  renderizarListaDeseos();
  marcarFavoritosGuardados();
}

function resolverRutaImagenDeseo(img) {
  if (!img) return '../img/productos/sin-imagen.jpg';
  if (img.startsWith('http') || img.startsWith('/') || img.startsWith('../')) return img;
  const base = window.RUTA_BASE ?? '../';
  return `${base}${img}`;
}

function renderizarListaDeseos() {
  const contenedor = document.getElementById('listaDeseosGrid');
  if (!contenedor) return;

  const lista = leerDeseos();
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
             onerror="this.src='../img/productos/sin-imagen.jpg'">
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

  contenedor.querySelectorAll('.btn-favorito').forEach((boton) => {
    boton.addEventListener('click', () => quitarDeDeseos(boton.dataset.id));
  });

  contenedor.querySelectorAll('.btn-agregar').forEach((boton) => {
    boton.addEventListener('click', () => {
      const producto = {
        id_producto: Number(boton.dataset.id),
        nombre: boton.dataset.nombre,
        precio: Number(boton.dataset.precio),
        imagen: boton.dataset.imagen
      };
      if (typeof agregarAlCarrito === 'function') agregarAlCarrito(producto);
    });
  });
}

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

/** Marca los corazones de los productos ya guardados para el usuario actual. */
function marcarFavoritosGuardados() {
  const ids = leerDeseos().map((p) => Number(p.id_producto));
  document.querySelectorAll('.btn-favorito[data-id]').forEach((boton) => {
    boton.classList.toggle('activo', ids.includes(Number(boton.dataset.id)));
  });
}

/** Activa los botones de las tarjetas generadas dinámicamente. */
function inicializarBotonesTarjetas() {
  document.querySelectorAll('.btn-agregar').forEach((boton) => {
    if (boton.dataset.activo) return;
    boton.dataset.activo = 'true';
    boton.addEventListener('click', () => {
      if (typeof agregarAlCarrito !== 'function') return;
      agregarAlCarrito({
        id_producto: Number(boton.dataset.id),
        nombre: boton.dataset.nombre,
        precio: Number(boton.dataset.precio),
        imagen: boton.dataset.imagen
      });
    });
  });

  document.querySelectorAll('.btn-favorito').forEach((boton) => {
    if (boton.dataset.activo) return;
    boton.dataset.activo = 'true';
    boton.addEventListener('click', async () => {
      const id = Number(boton.dataset.id);
      const lista = leerDeseos();
      const existe = lista.find((p) => Number(p.id_producto) === id);

      if (esClienteAutenticado()) {
        try {
          const resp = await Api.alternarFavorito(id);
          const agregado = Boolean(resp?.esFavorito);
          boton.classList.toggle('activo', agregado);
          await sincronizarDeseosDesdeApi();
          return;
        } catch {
          /* fallback a almacenamiento del usuario actual */
        }
      }

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

document.addEventListener('DOMContentLoaded', () => {
  const contCarritoDeseos = document.getElementById('carritoDeseosContenedor');
  if (contCarritoDeseos) contCarritoDeseos.style.display = 'none';
  limpiarClaveGlobalLegacyDeseos();
  actualizarContadorDeseos();
  renderizarListaDeseos();
  sincronizarDeseosDesdeApi();
});

document.addEventListener('sesion:cambiada', () => {
  sincronizarDeseosDesdeApi();
});

document.addEventListener('sesion:cerrada', () => {
  actualizarContadorDeseos();
  marcarFavoritosGuardados();
  renderizarListaDeseos();
  actualizarDeseosEnCarrito();
});

window.toggleDeseosCarrito = toggleDeseosCarrito;
window.actualizarDeseosEnCarrito = actualizarDeseosEnCarrito;
