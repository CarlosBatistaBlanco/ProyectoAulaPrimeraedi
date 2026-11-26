/**
 * Este archivo controla las funciones generales de la tienda pública: el menú desplegable en celulares,
 * el reloj en vivo de la portada, el video decorativo de fondo, la sombra de la barra superior al bajar,
 * el formulario de contacto, la ventana de foto ampliada al hacer clic en un producto y la ventana para
 * pedir asesoría sobre equipos que no aparecen en el catálogo.
 */

// Espera a que la página termine de cargar para activar las funciones interactivas de la tienda pública
document.addEventListener('DOMContentLoaded', () => {

  /* ---------------- 1. MENÚ EN CELULARES ---------------- */
  // Controla la apertura y cierre del menú de navegación en pantallas pequeñas
  const menuToggle = document.getElementById('menuToggle');
  const menuPrincipal = document.getElementById('menuPrincipal');

  if (menuToggle && menuPrincipal) {
    // Al tocar el botón de tres rayas, despliega u oculta las opciones del menú
    menuToggle.addEventListener('click', () => {
      const abierto = menuPrincipal.classList.toggle('activo');
      menuToggle.setAttribute('aria-expanded', abierto);
      menuToggle.classList.toggle('activo');
    });

    // Cierra el menú cuando la persona toca cualquier lugar fuera de él
    document.addEventListener('click', (e) => {
      if (!menuPrincipal.classList.contains('activo')) return;
      if (menuPrincipal.contains(e.target) || menuToggle.contains(e.target)) return;
      menuPrincipal.classList.remove('activo');
      menuToggle.classList.remove('activo');
      menuToggle.setAttribute('aria-expanded', 'false');
    });

    // Cierra el menú si la persona presiona la tecla Escape
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && menuPrincipal.classList.contains('activo')) {
        menuPrincipal.classList.remove('activo');
        menuToggle.classList.remove('activo');
        menuToggle.setAttribute('aria-expanded', 'false');
      }
    });

    // Cierra el menú automáticamente al elegir un enlace en el celular
    menuPrincipal.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        menuPrincipal.classList.remove('activo');
        menuToggle.classList.remove('activo');
        menuToggle.setAttribute('aria-expanded', 'false');
      });
    });
  }

  /* ---------------- 2. RELOJ EN VIVO DE LA PORTADA ---------------- */
  // Actualiza cada segundo el reloj digital de la portada que simula una cámara de seguridad en vivo
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

  /* ---------------- 2b. VIDEO DECORATIVO DE LA PORTADA ----------------
     El video de fondo es opcional: si el archivo no existe en el servidor,
     la portada muestra la imagen fija con su efecto visual sin dar errores. */
  const heroVideo = document.querySelector('.hero__video');
  if (heroVideo) {
    const rutaVideo = heroVideo.dataset.videoOpcional;
    const marcarConVideo = () => document.body.classList.add('hero-con-video');

    // Prepara la reproducción silenciosa en bucle del video y la retoma si la persona vuelve a la pestaña
    const prepararReproduccion = () => {
      if (heroVideo.readyState >= 2) {
        marcarConVideo();
      } else {
        heroVideo.addEventListener('loadeddata', marcarConVideo, { once: true });
      }
      heroVideo.querySelectorAll('source').forEach(src => {
        src.addEventListener('error', () => document.body.classList.remove('hero-con-video'));
      });
      heroVideo.muted = true;
      const intentarReproducir = () => {
        const p = heroVideo.play();
        if (p && p.catch) { p.catch(() => { /* si el navegador bloquea la reproducción automática, queda la imagen fija */ }); }
      };
      intentarReproducir();
      document.addEventListener('visibilitychange', () => {
        if (!document.hidden) { intentarReproducir(); }
      });
    };

    if (!rutaVideo) {
      prepararReproduccion();
    } else {
      // Consulta rápido si el video existe en el servidor antes de descargarlo completo
      fetch(rutaVideo, { method: 'HEAD' })
        .then((r) => {
          if (!r.ok) return;
          const fuente = document.createElement('source');
          fuente.src = rutaVideo;
          fuente.type = 'video/mp4';
          heroVideo.appendChild(fuente);
          heroVideo.load();
          prepararReproduccion();
        })
        .catch(() => { /* si no hay video disponible, se mantiene la imagen de portada */ });
    }
  }

  /* ---------------- 3. SOMBRA DE LA BARRA AL BAJAR ---------------- */
  // Agrega una sombra suave a la barra superior cuando el visitante baja por la página
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

  /* ---------------- 4. FORMULARIO DE CONTACTO ---------------- */
  // Envía el mensaje de la sección "Contáctanos" al servidor sin recargar la página
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
        const fd = new FormData(formContacto);
        const datos = await Api.enviarContacto({
          nombre: fd.get('nombre'),
          correo: fd.get('correo'),
          telefono: fd.get('telefono'),
          asunto: fd.get('asunto'),
          tipo: fd.get('tipo') || 'mensaje',
          mensaje: fd.get('mensaje'),
        });

        respuestaContacto.textContent = datos.mensaje;
        respuestaContacto.className = 'form-respuesta ok';
        formContacto.reset();
      } catch (error) {
        respuestaContacto.textContent = 'Error de conexión. Verifica que Apache/MySQL estén encendidos.';
        respuestaContacto.className = 'form-respuesta error';
      } finally {
        boton.innerHTML = textoOriginal;
        boton.disabled = false;
      }
    });
  }

  /* ---------------- 5. VISTA AMPLIADA DE PRODUCTO ---------------- */
  // Crea la ventana emergente que muestra la foto en grande y los detalles completos de cualquier producto
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
          <img alt="" id="modalZoomImg">
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

  // Oculta la vista ampliada del producto y devuelve el desplazamiento normal a la página
  const cerrarModalZoom = () => {
    modalZoom.classList.remove('activo');
    document.body.style.overflow = '';
  };

  // Permite cerrar la vista ampliada con la X, tocando el fondo oscuro o presionando Escape
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

  // Detecta cuando el cliente hace clic en la foto de un producto para abrir la ventana ampliada
  document.addEventListener('click', (e) => {
    const contenedorImg = e.target.closest('.tarjeta-producto__imagen');
    if (!contenedorImg) return;

    // Ignora el clic si la persona tocó el corazón de favoritos o las etiquetas de oferta
    if (e.target.closest('.btn-favorito') || e.target.closest('.btn-quitar-deseo') || e.target.closest('.badge') || e.target.closest('.badge-rebaja')) return;

    const tarjeta = contenedorImg.closest('.tarjeta-producto');
    if (!tarjeta) return;

    // Toma los datos visibles de la tarjeta pulsada para rellenar la ventana ampliada
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

    // Copia los botones "Agregar al carrito" y "Favorito" dentro de la ventana ampliada
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

  /* ---------------- 6. VENTANA DE ASESORÍA PERSONALIZADA ---------------- */
  // Abre la ventana emergente para pedir cotización de un equipo que no aparece en el catálogo
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

  // Cierra la ventana emergente de asesoría personalizada
  window.cerrarModalAsesoria = function() {
    const modal = document.getElementById('modalAsesoria');
    if (!modal) return;
    modal.classList.remove('activo');
    document.body.style.overflow = '';
  };

  // Activa el cierre por clic fuera o tecla Escape y el envío del formulario de asesoría al servidor
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
          const fd = new FormData(formAsesoria);
          const datos = await Api.enviarContacto({
            nombre: fd.get('nombre'),
            correo: fd.get('correo'),
            telefono: fd.get('telefono'),
            asunto: fd.get('asunto'),
            tipo: fd.get('tipo') || 'mensaje',
            mensaje: fd.get('mensaje'),
          });

          respAsesoria.textContent = datos.mensaje || '?Solicitud recibida! Un asesor t?cnico se comunicar? contigo.';
          respAsesoria.className = 'form-respuesta ok';

          formAsesoria.reset();
          setTimeout(() => {
            window.cerrarModalAsesoria();
            respAsesoria.textContent = '';
          }, 3000);
        } catch (err) {
          respAsesoria.textContent = 'Error de conexión. Puedes contactarnos directamente por WhatsApp.';
        } finally {
          boton.innerHTML = origText;
          boton.disabled = false;
        }
      });
    }
  }
});
