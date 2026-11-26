/**
 * Este archivo prepara el respaldo de la aplicación cuando el celular o computador pierde la señal de internet.
 * Guarda copias básicas del logo y de la pantalla de aviso sin conexión para que el visitante vea un mensaje
 * amigable en lugar de un error del navegador, pero nunca guarda precios, inventario, sesiones ni el chat
 * para que esa información se consulte siempre en vivo desde el servidor.
 */

// Nombre del espacio de respaldo en el navegador y lista de archivos básicos que se guardan al instalar la app
const CACHE_NAME = 'tecnisecure-pwa-v1';
const PRECACHE_URLS = [
  '/offline.html',
  '/manifest.webmanifest',
  '/img/logo/logo.svg',
  '/img/icons/icon-192.png',
  '/img/icons/icon-512.png'
];

// Al instalar la app, guarda en el dispositivo el logo y la pantalla de aviso sin conexión
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting())
  );
});

// Al activar una versión nueva, borra las copias antiguas para que el usuario siempre vea lo más reciente
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

// Revisa cada consulta que hace la página para traerla de internet o mostrar la copia de respaldo si no hay señal
self.addEventListener('fetch', (event) => {
  const { request } = event;
  // Solo aplica respaldo a consultas de lectura; envíos de formularios y compras siempre van directo al servidor
  if (request.method !== 'GET') return;

  const url = new URL(request.url);
  // Solo atiende archivos propios de esta tienda; nunca guarda datos en vivo como precios, chat o diagnósticos
  if (url.origin !== self.location.origin) return;
  if (
    url.pathname.startsWith('/api/') ||
    url.pathname.startsWith('/ws') ||
    url.pathname.startsWith('/actuator/') ||
    url.pathname.startsWith('/swagger-ui') ||
    url.pathname.startsWith('/v3/api-docs')
  ) {
    return;
  }

  // Al abrir una página de la tienda: intenta cargarla de internet y, si no hay señal, muestra la copia guardada o el aviso sin conexión
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response && response.ok) {
            const copia = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, copia)).catch(() => {});
          }
          return response;
        })
        .catch(() =>
          caches.match(request).then((cached) => cached || caches.match('/offline.html'))
        )
    );
    return;
  }

  // Para estilos, funciones e imágenes de la tienda: busca primero en internet y usa la copia de respaldo si falla la señal
  if (
    url.pathname.startsWith('/css/') ||
    url.pathname.startsWith('/js/') ||
    url.pathname.startsWith('/img/')
  ) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response && response.ok) {
            const copia = response.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, copia)).catch(() => {});
          }
          return response;
        })
        .catch(() => caches.match(request))
    );
  }
});
