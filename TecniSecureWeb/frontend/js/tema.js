/**
 * Este archivo controla el botón de sol y luna que cambia los colores de todo el sitio entre modo claro
 * (el diseño por defecto) y modo oscuro. Su efecto se nota tanto en la tienda pública como dentro de los
 * paneles de clientes, empleados y administradores, y recuerda la elección en la memoria del navegador
 * de cada visitante.
 */
// Bloque cerrado para manejar el cambio de apariencia sin interferir con otras funciones de la página
(function () {
  // Nombre con el que se recuerda la preferencia del visitante en la memoria del navegador
  const CLAVE_TEMA = 'tecniservicios_tema';
  const TEMA_POR_DEFECTO = 'claro';
  const html = document.documentElement;

  // Cambia los colores de la pantalla (claro u oscuro) y actualiza el ícono del botón (sol o luna)
  function aplicarTema(tema) {
    // El modo claro viene activo por defecto; solo se marca la página cuando el visitante elige modo oscuro
    if (tema === 'oscuro') {
      html.setAttribute('data-tema', 'oscuro');
    } else {
      html.removeAttribute('data-tema');
    }
    // En modo claro se muestra la luna para invitar a cambiar a oscuro, y en modo oscuro se muestra el sol
    document.querySelectorAll('.btn-tema i').forEach(icono => {
      icono.className = tema === 'oscuro' ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
    });
  }

  // Aplica de inmediato el tema guardado para evitar parpadeos al abrir la página
  const temaGuardado = localStorage.getItem(CLAVE_TEMA) || TEMA_POR_DEFECTO;
  aplicarTema(temaGuardado);

  // Espera a que la página termine de cargar para activar los clics en todos los botones de sol/luna
  document.addEventListener('DOMContentLoaded', () => {
    // Asegura que los íconos queden correctos apenas termina de cargar la pantalla
    aplicarTema(temaGuardado);

    // Detecta cuando el visitante hace clic en el botón de tema, alterna el color y recuerda la elección
    document.querySelectorAll('.btn-tema').forEach(boton => {
      boton.addEventListener('click', () => {
        const actual = html.getAttribute('data-tema') === 'oscuro' ? 'oscuro' : 'claro';
        const nuevo = actual === 'oscuro' ? 'claro' : 'oscuro';
        aplicarTema(nuevo);
        localStorage.setItem(CLAVE_TEMA, nuevo);
      });
    });
  });
})();
