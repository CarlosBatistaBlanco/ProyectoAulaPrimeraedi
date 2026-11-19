/**
 * catalogo.js — Catálogo de productos (consume la API REST)
 * -----------------------------------------------------------------
 * la página es HTML estático y
 * los productos llegan desde GET /api/productos (paginado y filtrable).
 */

const CATALOGO_ESTADO = {
  pagina: 0,
  tamanio: 9,
  texto: '',
  idCategoria: null,
  orden: 'idProducto',
  totalPaginas: 0
};

const MAPA_ICONOS = {
  camera: 'fa-solid fa-video',
  monitor: 'fa-solid fa-display',
  bell: 'fa-solid fa-bell',
  lock: 'fa-solid fa-fingerprint',
  wrench: 'fa-solid fa-screwdriver-wrench',
  clipboard: 'fa-solid fa-clipboard-list'
};

/** Tarjeta de producto reutilizable (portada y catálogo). */
function tarjetaProducto(p, rutaBase = '') {
  const imagen = `${rutaBase}${p.imagen || 'img/productos/sin-imagen.jpg'}`;
  const tieneDescuento = Number(p.descuentoPct) > 0;
  const stockBajo = p.stock > 0 && p.stockBajo;
  const agotado = p.stock <= 0;

  return `
  <article class="tarjeta-producto" data-id="${p.idProducto}">
    <div class="tarjeta-producto__imagen">
      ${tieneDescuento ? `<span class="badge-rebaja">-${Math.round(p.descuentoPct)}% OFF</span>`
        : (p.destacado ? '<span class="badge">Destacado</span>' : '')}

      <button type="button" class="btn-favorito"
              data-id="${p.idProducto}"
              data-nombre="${escaparHtml(p.nombre)}"
              data-precio="${p.precioFinal}"
              data-precio-original="${p.precio}"
              data-descuento="${p.descuentoPct || 0}"
              data-imagen="${escaparHtml(imagen)}"
              data-marca="${escaparHtml(p.marca || '')}"
              title="Guardar en mi lista de compras">
        <i class="fa-solid fa-heart"></i>
      </button>

      <img src="${escaparHtml(imagen)}" alt="${escaparHtml(p.nombre)}" loading="lazy"
           onerror="this.src='${rutaBase}img/productos/sin-imagen.jpg'">
      <span class="tarjeta-producto__zoom-hint"><i class="fa-solid fa-magnifying-glass-plus"></i> Ampliar</span>
    </div>

    <div class="tarjeta-producto__info">
      ${p.marca ? `<span class="tarjeta-producto__marca">${escaparHtml(p.marca)}</span>` : ''}
      <h3>${escaparHtml(p.nombre)}</h3>
      <span class="tarjeta-producto__categoria">
        <i class="fa-solid fa-tag"></i> ${escaparHtml(p.categoria || 'Sin categoría')}
      </span>
      ${p.descripcion ? `<p class="tarjeta-producto__desc">${escaparHtml(p.descripcion)}</p>` : ''}

      <div class="tarjeta-producto__footer">
        <div class="precio-contenedor">
          ${tieneDescuento ? `
          <div class="precio-contenedor__fila">
            <span class="precio-original">${formatearPesos(p.precio)}</span>
            <span class="badge-descuento">-${Math.round(p.descuentoPct)}%</span>
          </div>` : ''}
          <span class="precio ${tieneDescuento ? 'precio--con-descuento' : ''}">
            ${formatearPesos(p.precioFinal)}
          </span>
        </div>

        <button class="btn-agregar"
                data-id="${p.idProducto}"
                data-nombre="${escaparHtml(p.nombre)}"
                data-precio="${p.precioFinal}"
                data-imagen="${escaparHtml(imagen)}"
                ${agotado ? 'disabled' : ''}>
          <i class="fa-solid fa-cart-plus"></i> Agregar
        </button>
      </div>

      ${stockBajo ? `<p class="stock-bajo"><i class="fa-solid fa-triangle-exclamation"></i> ¡Últimas ${p.stock} unidades!</p>`
        : (agotado ? '<p class="stock-agotado"><i class="fa-solid fa-ban"></i> Agotado temporalmente</p>' : '')}
    </div>
  </article>`;
}

function tarjetaServicio(s, rutaBase = '') {
  const icono = MAPA_ICONOS[s.icono] || 'fa-solid fa-shield';
  return `
  <article class="tarjeta-servicio">
    <div class="tarjeta-servicio__icono"><i class="${icono}"></i></div>
    <h3>${escaparHtml(s.titulo)}</h3>
    <p>${escaparHtml(s.descripcion)}</p>
    <a href="${rutaBase}pages/solicitar-servicio.html?id=${s.idServicio}" class="tarjeta-servicio__boton">
      Solicitar este servicio <i class="fa-solid fa-arrow-right"></i>
    </a>
  </article>`;
}

/* ------------------------- Portada (index.html) ------------------------- */

async function cargarPortada() {
  const listaServicios = document.getElementById('listaServicios');
  const listaDestacados = document.getElementById('listaDestacados');

  if (listaServicios) {
    try {
      const servicios = await Api.servicios();
      listaServicios.innerHTML = servicios.map((s) => tarjetaServicio(s)).join('')
        || '<p class="cargando">No hay servicios registrados.</p>';
    } catch {
      listaServicios.innerHTML = '<p class="cargando">No se pudieron cargar los servicios.</p>';
    }
  }

  if (listaDestacados) {
    try {
      const productos = await Api.destacados();
      listaDestacados.innerHTML = productos.map((p) => tarjetaProducto(p)).join('')
        || '<p class="cargando">No hay productos destacados.</p>';
      if (typeof inicializarBotonesTarjetas === 'function') inicializarBotonesTarjetas();
    } catch {
      listaDestacados.innerHTML = '<p class="cargando">No se pudieron cargar los productos.</p>';
    }
  }
}

/* ---------------------- Catálogo (pages/productos) --------------------- */

const MAPA_ORDEN = {
  relevancia: 'idProducto',
  precio_asc: 'precio',
  precio_desc: 'precio',
  nombre: 'nombre',
  nuevos: 'creadoEn'
};

async function cargarCatalogo() {
  const contenedor = document.getElementById('listaProductos');
  if (!contenedor) return;

  const params = {
    pagina: CATALOGO_ESTADO.pagina,
    tamanio: CATALOGO_ESTADO.tamanio,
    orden: MAPA_ORDEN[CATALOGO_ESTADO.orden] || 'idProducto'
  };
  if (CATALOGO_ESTADO.texto) params.texto = CATALOGO_ESTADO.texto;
  if (CATALOGO_ESTADO.idCategoria) params.idCategoria = CATALOGO_ESTADO.idCategoria;

  contenedor.innerHTML = '<p class="cargando">Cargando productos…</p>';

  try {
    const resultado = await Api.productos(params);
    CATALOGO_ESTADO.totalPaginas = resultado.totalPaginas;
    // Si la página actual quedó fuera de rango (p. ej. tras filtrar), vuelve a la última válida.
    if (resultado.totalPaginas > 0 && CATALOGO_ESTADO.pagina >= resultado.totalPaginas) {
      CATALOGO_ESTADO.pagina = resultado.totalPaginas - 1;
      return cargarCatalogo();
    }

    const productos = CATALOGO_ESTADO.orden === 'precio_desc'
      ? [...resultado.contenido].sort((a, b) => Number(b.precioFinal) - Number(a.precioFinal))
      : resultado.contenido;

    contenedor.innerHTML = productos.length
      ? productos.map((p) => tarjetaProducto(p, '../')).join('')
      : `<div class="catalogo__vacio">
           <i class="fa-solid fa-box-open"></i>
           <h3>${CATALOGO_ESTADO.texto
             ? `No encontramos productos que coincidan con "${escaparHtml(CATALOGO_ESTADO.texto)}"`
             : 'No hay productos en esta categoría'}</h3>
           <p>¿Buscas una referencia especial, otra marca o repuesto técnico? Nuestro equipo te lo cotiza e instala.</p>
         </div>`;

    const etiqueta = document.getElementById('resultadoCatalogo');
    if (etiqueta) {
      etiqueta.innerHTML = `<strong>${resultado.totalElementos}</strong> ${resultado.totalElementos === 1 ? 'producto' : 'productos'}`;
    }

    pintarPaginacion(CATALOGO_ESTADO.totalPaginas);
    if (typeof inicializarBotonesTarjetas === 'function') inicializarBotonesTarjetas();
    if (typeof marcarFavoritosGuardados === 'function') marcarFavoritosGuardados();
  } catch (error) {
    contenedor.innerHTML = `<p class="cargando">${escaparHtml(error.message)}</p>`;
  }
}

/** Dibuja los controles de paginación del catálogo. */
function pintarPaginacion(totalPaginas) {
  let nav = document.getElementById('paginacionCatalogo');
  const contenedor = document.getElementById('listaProductos');
  if (!contenedor || !contenedor.parentElement) return;

  if (!nav) {
    nav = document.createElement('nav');
    nav.id = 'paginacionCatalogo';
    nav.className = 'catalogo__paginacion';
    nav.setAttribute('aria-label', 'Paginación del catálogo');
    contenedor.parentElement.appendChild(nav);
  }

  if (totalPaginas <= 1) {
    nav.innerHTML = '';
    nav.hidden = true;
    return;
  }
  nav.hidden = false;

  const actual = CATALOGO_ESTADO.pagina;
  const paginas = [];
  for (let i = 0; i < totalPaginas; i++) {
    if (i === 0 || i === totalPaginas - 1 || Math.abs(i - actual) <= 1) {
      paginas.push(i);
    } else if (paginas[paginas.length - 1] !== '...') {
      paginas.push('...');
    }
  }

  const boton = (texto, destino, extra = '') =>
    `<button type="button" class="catalogo__pagina ${extra}" data-pagina="${destino}"
             ${destino === actual ? 'aria-current="page"' : ''}>${texto}</button>`;

  nav.innerHTML = [
    boton('<i class="fa-solid fa-chevron-left"></i>', actual - 1, actual === 0 ? 'deshabilitada' : ''),
    ...paginas.map((p) => (p === '...'
      ? '<span class="catalogo__pagina-separador">…</span>'
      : boton(p + 1, p, p === actual ? 'activa' : ''))),
    boton('<i class="fa-solid fa-chevron-right"></i>', actual + 1, actual >= totalPaginas - 1 ? 'deshabilitada' : '')
  ].join('');

  nav.querySelectorAll('button[data-pagina]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const destino = Number(btn.dataset.pagina);
      if (btn.classList.contains('deshabilitada') || destino === CATALOGO_ESTADO.pagina) return;
      CATALOGO_ESTADO.pagina = destino;
      cargarCatalogo();
      const cabecera = document.querySelector('.catalogo');
      if (cabecera) cabecera.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });
}

async function cargarCategorias() {
  const lista = document.getElementById('listaCategorias');
  if (!lista) return;
  try {
    const [categorias, todos] = await Promise.all([
      Api.categorias(),
      Api.productos({ tamanio: 1 })
    ]);
    const conteos = await Promise.all(categorias.map((c) =>
      Api.productos({ tamanio: 1, idCategoria: c.idCategoria })
        .then((r) => r.totalElementos)
        .catch(() => 0)
    ));

    const items = categorias.map((c, i) => `
      <li>
        <a href="#" data-categoria="${c.idCategoria}" class="${CATALOGO_ESTADO.idCategoria === c.idCategoria ? 'activo' : ''}">
          ${escaparHtml(c.nombre)} <span class="catalogo__conteo">${conteos[i]}</span>
        </a>
      </li>`).join('');

    lista.innerHTML = `
      <li><a href="#" data-categoria="" class="${!CATALOGO_ESTADO.idCategoria ? 'activo' : ''}">
        Todas <span class="catalogo__conteo">${todos.totalElementos}</span>
      </a></li>${items}`;

    lista.querySelectorAll('a[data-categoria]').forEach((enlace) => {
      enlace.addEventListener('click', (e) => {
        e.preventDefault();
        const valor = enlace.dataset.categoria;
        // data-categoria guarda el id numérico; el backend solo filtra por id.
        CATALOGO_ESTADO.idCategoria = valor === '' ? null : Number(valor);
        CATALOGO_ESTADO.pagina = 0;
        lista.querySelectorAll('a').forEach((a) => a.classList.remove('activo'));
        enlace.classList.add('activo');
        cargarCatalogo();
      });
    });
  } catch {
    /* el filtro de categorías es opcional */
  }
}

function inicializarCatalogo() {
  const contenedor = document.getElementById('listaProductos');
  if (!contenedor) return;

  const form = document.getElementById('formBuscarCatalogo');
  const buscador = document.getElementById('buscarCatalogo');
  const orden = document.getElementById('ordenCatalogo');
  const params = new URLSearchParams(window.location.search);

  const sincronizarUrl = () => {
    const url = new URL(window.location.href);
    if (CATALOGO_ESTADO.texto) {
      url.searchParams.set('buscar', CATALOGO_ESTADO.texto);
    } else {
      url.searchParams.delete('buscar');
    }
    window.history.replaceState({}, '', url);
  };

  if (form) {
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      // Sin recargar la página: si el formulario navega por GET el texto se perdía.
      CATALOGO_ESTADO.texto = (buscador.value || '').trim();
      CATALOGO_ESTADO.pagina = 0;
      sincronizarUrl();
      cargarCatalogo();
    });
  }

  if (orden) {
    orden.addEventListener('change', () => {
      CATALOGO_ESTADO.orden = orden.value;
      CATALOGO_ESTADO.pagina = 0;
      cargarCatalogo();
    });
  }

  if (params.get('buscar')) {
    CATALOGO_ESTADO.texto = params.get('buscar');
    if (buscador) buscador.value = CATALOGO_ESTADO.texto;
  }

  cargarCategorias();
  cargarCatalogo();
}

document.addEventListener('DOMContentLoaded', () => {
  cargarPortada();
  inicializarCatalogo();
});
