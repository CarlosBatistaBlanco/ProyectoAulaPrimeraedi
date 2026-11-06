/**
 * main.js
 * -----------------------------------------------------------------
 * Funciones generales del sitio:
 *   1. Menú hamburguesa (móvil)
 *   2. Reloj en vivo del HUD del hero (efecto cámara de seguridad)
 *   3. Barra de navegación con sombra al hacer scroll
 *   4. Envío del formulario de contacto por fetch() a PHP
 * -----------------------------------------------------------------
 */

document.addEventListener('DOMContentLoaded', () => {

  /* ---------------- 1. MENÚ HAMBURGUESA ---------------- */
  const menuToggle = document.getElementById('menuToggle');
  const menuPrincipal = document.getElementById('menuPrincipal');

  if (menuToggle && menuPrincipal) {
    menuToggle.addEventListener('click', () => {
      const abierto = menuPrincipal.classList.toggle('activo');
      menuToggle.setAttribute('aria-expanded', abierto);
      menuToggle.classList.toggle('activo');
    });

    // Cerrar el menú al hacer clic en un enlace (en móvil)
    menuPrincipal.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        menuPrincipal.classList.remove('activo');
        menuToggle.setAttribute('aria-expanded', 'false');
      });
    });
  }

  /* ---------------- 2. RELOJ DEL HUD (estilo cámara) ---------------- */
  const hudClock = document.getElementById('hudClock');
  if (hudClock) {
    const actualizarReloj = () => {
      const ahora = new Date();
      const hh = String(ahora.getHours()).padStart(2, '0');
      const mm = String(ahora.getMinutes()).padStart(2, '0');
      const ss = String(ahora.getSeconds()).padStart(2, '0');
      hudClock.textContent = `${hh}:${mm}:${ss}`;
    };
    actualizarReloj();
    setInterval(actualizarReloj, 1000);
  }

  /* ---------------- 2b. VIDEO DEL HERO ----------------
     Si video/video.mp4 existe y el navegador puede reproducirlo, se
     oculta la animación CSS de respaldo y se garantiza el autoplay
     (algunos navegadores lo bloquean si el video no está silenciado). */
  const heroVideo = document.querySelector('.hero__video');
  if (heroVideo) {
    const marcarConVideo = () => document.body.classList.add('hero-con-video');

    if (heroVideo.readyState >= 2) {
      marcarConVideo();
    } else {
      heroVideo.addEventListener('loadeddata', marcarConVideo, { once: true });
    }

    // Un <source> que falla no dispara 'error' en el <video>: se escucha
    // en el source para no dejar el respaldo CSS tapando nada.
    heroVideo.querySelectorAll('source').forEach(src => {
      src.addEventListener('error', () => {
        document.body.classList.remove('hero-con-video');
      });
    });

    heroVideo.muted = true;
    const intentarReproducir = () => {
      const p = heroVideo.play();
      if (p && p.catch) { p.catch(() => { /* autoplay bloqueado: queda el póster */ }); }
    };
    intentarReproducir();
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) { intentarReproducir(); }
    });
  }

  /* ---------------- 3. NAV CON SOMBRA AL HACER SCROLL ---------------- */
  const nav = document.getElementById('navPrincipal');
  if (nav) {
    window.addEventListener('scroll', () => {
      if (window.scrollY > 30) {
        nav.style.boxShadow = '0 6px 20px rgba(0,0,0,0.35)';
      } else {
        nav.style.boxShadow = 'none';
      }
    });
  }

  /* ---------------- 4. FORMULARIO DE CONTACTO (AJAX) ---------------- */
  const formContacto = document.getElementById('formContacto');
  const respuestaContacto = document.getElementById('respuestaContacto');

  if (formContacto) {
    formContacto.addEventListener('submit', async (e) => {
      e.preventDefault();

      const boton = formContacto.querySelector('button[type="submit"]');
      const textoOriginal = boton.innerHTML;
      boton.innerHTML = 'Enviando...';
      boton.disabled = true;

      try {
        // Detecta la ruta correcta según si estamos en / o en /pages
        const rutaPhp = window.location.pathname.includes('/pages/')
          ? '../php/procesar_contacto.php'
          : 'php/procesar_contacto.php';

        const respuesta = await fetch(rutaPhp, {
          method: 'POST',
          body: new FormData(formContacto),
        });
        const datos = await respuesta.json();

        respuestaContacto.textContent = datos.mensaje;
        respuestaContacto.className = 'form-respuesta ' + (datos.ok ? 'ok' : 'error');

        if (datos.ok) formContacto.reset();
      } catch (error) {
        respuestaContacto.textContent = 'Error de conexión. Verifica que Apache/MySQL estén encendidos.';
        respuestaContacto.className = 'form-respuesta error';
      } finally {
        boton.innerHTML = textoOriginal;
        boton.disabled = false;
      }
    });
  }

  /* ---------------- 5. ZOOM Y LIGHTBOX DE PRODUCTO ---------------- */
  let modalZoom = document.querySelector('.modal-zoom');
  if (!modalZoom) {
    modalZoom = document.createElement('div');
    modalZoom.className = 'modal-zoom';
    modalZoom.setAttribute('role', 'dialog');
    modalZoom.setAttribute('aria-modal', 'true');
    modalZoom.setAttribute('aria-label', 'Vista ampliada del producto');
    modalZoom.innerHTML = `
      <div class="modal-zoom__caja">
        <button class="modal-zoom__cerrar" aria-label="Cerrar modal">&times;</button>
        <div class="modal-zoom__visor">
          <img src="" alt="" id="modalZoomImg">
        </div>
        <div class="modal-zoom__detalles">
          <span class="modal-zoom__marca" id="modalZoomMarca"></span>
          <h3 class="modal-zoom__titulo" id="modalZoomTitulo"></h3>
          <p class="modal-zoom__desc" id="modalZoomDesc"></p>
          <div class="modal-zoom__garantia" id="modalZoomGarantia"></div>
          <div class="modal-zoom__precio" id="modalZoomPrecio"></div>
          <div class="modal-zoom__acciones" id="modalZoomAcciones"></div>
        </div>
      </div>
    `;
    document.body.appendChild(modalZoom);
  }

  const cerrarModalZoom = () => {
    modalZoom.classList.remove('activo');
    document.body.style.overflow = '';
  };

  const btnCerrar = modalZoom.querySelector('.modal-zoom__cerrar');
  if (btnCerrar) btnCerrar.addEventListener('click', cerrarModalZoom);

  modalZoom.addEventListener('click', (e) => {
    if (e.target === modalZoom) cerrarModalZoom();
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && modalZoom.classList.contains('activo')) {
      cerrarModalZoom();
    }
  });

  // Abrir modal con imagen en alta calidad y detalles completos al hacer clic en la imagen
  document.addEventListener('click', (e) => {
    const contenedorImg = e.target.closest('.tarjeta-producto__imagen');
    if (!contenedorImg) return;

    // No abrir modal si se hace clic en favoritos, botones de quitar o badges
    if (e.target.closest('.btn-favorito') || e.target.closest('.btn-quitar-deseo') || e.target.closest('.badge') || e.target.closest('.badge-rebaja')) return;

    const tarjeta = contenedorImg.closest('.tarjeta-producto');
    if (!tarjeta) return;

    const img = contenedorImg.querySelector('img');
    const titulo = tarjeta.querySelector('h3')?.textContent.trim() || '';
    const marca = tarjeta.querySelector('.tarjeta-producto__marca')?.textContent.trim() || '';
    const desc = tarjeta.querySelector('.tarjeta-producto__desc')?.textContent.trim() || '';
    const garantia = tarjeta.querySelector('.tarjeta-producto__garantia')?.innerHTML || '';
    const precioContenedor = tarjeta.querySelector('.precio-contenedor');
    const precio = tarjeta.querySelector('.precio')?.textContent.trim() || '';
    const btnAgregar = tarjeta.querySelector('.btn-agregar');
    const btnFav = tarjeta.querySelector('.btn-favorito');

    const modalImg = document.getElementById('modalZoomImg');
    const modalMarca = document.getElementById('modalZoomMarca');
    const modalTitulo = document.getElementById('modalZoomTitulo');
    const modalDesc = document.getElementById('modalZoomDesc');
    const modalGarantia = document.getElementById('modalZoomGarantia');
    const modalPrecio = document.getElementById('modalZoomPrecio');
    const modalAcciones = document.getElementById('modalZoomAcciones');

    if (modalImg && img) {
      modalImg.src = img.src;
      modalImg.alt = img.alt || titulo;
    }
    if (modalMarca) modalMarca.textContent = marca;
    if (modalTitulo) modalTitulo.textContent = titulo;
    if (modalDesc) modalDesc.textContent = desc;
    if (modalGarantia) modalGarantia.innerHTML = garantia;

    if (modalPrecio) {
      if (precioContenedor) {
        modalPrecio.innerHTML = precioContenedor.innerHTML;
      } else {
        modalPrecio.textContent = precio;
      }
    }

    if (modalAcciones) {
      modalAcciones.innerHTML = '';
      if (btnAgregar) {
        const clonBtn = btnAgregar.cloneNode(true);
        clonBtn.addEventListener('click', () => {
          btnAgregar.click();
          const orig = clonBtn.innerHTML;
          clonBtn.innerHTML = '<i class="fa-solid fa-check"></i> Agregado';
          setTimeout(() => (clonBtn.innerHTML = orig), 1200);
        });
        modalAcciones.appendChild(clonBtn);
      }
      if (btnFav) {
        const clonFav = btnFav.cloneNode(true);
        clonFav.style.position = 'static';
        clonFav.style.width = '42px';
        clonFav.style.height = '42px';
        clonFav.addEventListener('click', () => {
          btnFav.click();
          setTimeout(() => {
            clonFav.classList.toggle('activo', btnFav.classList.contains('activo'));
          }, 50);
        });
        modalAcciones.appendChild(clonFav);
      }
    }

    modalZoom.classList.add('activo');
    document.body.style.overflow = 'hidden';
  });

  /* ---------------- 6. MODAL ASESORÍA ("¿NO ENCUENTRAS LO QUE BUSCAS?") ---------------- */
  window.abrirModalAsesoria = function(textoPrellenado = '') {
    const modal = document.getElementById('modalAsesoria');
    if (!modal) return;
    if (textoPrellenado) {
      const textarea = modal.querySelector('#asesoriaProducto');
      if (textarea) textarea.value = textoPrellenado;
    }
    modal.classList.add('activo');
    document.body.style.overflow = 'hidden';
  };

  window.cerrarModalAsesoria = function() {
    const modal = document.getElementById('modalAsesoria');
    if (!modal) return;
    modal.classList.remove('activo');
    document.body.style.overflow = '';
  };

  const modalAsesoria = document.getElementById('modalAsesoria');
  if (modalAsesoria) {
    modalAsesoria.addEventListener('click', (e) => {
      if (e.target === modalAsesoria) window.cerrarModalAsesoria();
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && modalAsesoria.classList.contains('activo')) {
        window.cerrarModalAsesoria();
      }
    });

    const formAsesoria = document.getElementById('formAsesoriaProducto');
    const respAsesoria = document.getElementById('respuestaAsesoria');
    if (formAsesoria && respAsesoria) {
      formAsesoria.addEventListener('submit', async (e) => {
        e.preventDefault();
        const boton = formAsesoria.querySelector('button[type="submit"]');
        const origText = boton.innerHTML;
        boton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Enviando...';
        boton.disabled = true;

        try {
          const ruta = window.location.pathname.includes('/pages/')
            ? '../php/procesar_contacto.php'
            : 'php/procesar_contacto.php';

          const res = await fetch(ruta, {
            method: 'POST',
            body: new FormData(formAsesoria)
          });
          const datos = await res.json();
          respAsesoria.textContent = datos.mensaje || '¡Solicitud recibida! Un asesor técnico se comunicará contigo.';
          respAsesoria.className = 'form-respuesta ' + (datos.ok ? 'ok' : 'error');

          if (datos.ok) {
            formAsesoria.reset();
            setTimeout(() => {
              window.cerrarModalAsesoria();
              respAsesoria.textContent = '';
            }, 3000);
          }
        } catch (err) {
          respAsesoria.textContent = 'Error de conexión. Puedes contactarnos directamente por WhatsApp.';
          respAsesoria.className = 'form-respuesta error';
        } finally {
          boton.innerHTML = origText;
          boton.disabled = false;
        }
      });
    }
  }
});
