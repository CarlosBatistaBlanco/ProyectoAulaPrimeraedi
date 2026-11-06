/**
 * tema.js
 * -----------------------------------------------------------------
 * Controla el botón de sol/luna que cambia entre tema claro (el
 * predeterminado) y tema oscuro.
 *   - Si nadie toca el botón, el sitio se ve en CLARO.
 *   - La elección se guarda en localStorage del navegador, así que
 *     solo afecta a quien lo presionó, no a otros visitantes.
 *   - Funciona igual en el sitio público y dentro del panel, porque
 *     ambos leen las mismas variables CSS definidas en css/style.css.
 * -----------------------------------------------------------------
 */
(function () {
  const CLAVE_TEMA = 'tecniservicios_tema';
  const TEMA_POR_DEFECTO = 'claro';
  const html = document.documentElement;

  function aplicarTema(tema) {
    // El tema claro es el :root por defecto, así que solo marcamos el
    // atributo cuando es "oscuro" para sobreescribir las variables.
    if (tema === 'oscuro') {
      html.setAttribute('data-tema', 'oscuro');
    } else {
      html.removeAttribute('data-tema');
    }
    // En claro se muestra la luna (invita a ir a oscuro) y viceversa
    document.querySelectorAll('.btn-tema i').forEach(icono => {
      icono.className = tema === 'oscuro' ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
    });
  }

  // Aplica de inmediato el tema guardado para evitar parpadeos
  const temaGuardado = localStorage.getItem(CLAVE_TEMA) || TEMA_POR_DEFECTO;
  aplicarTema(temaGuardado);

  document.addEventListener('DOMContentLoaded', () => {
    // Asegura que los íconos queden correctos apenas carga la página
    aplicarTema(temaGuardado);

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
