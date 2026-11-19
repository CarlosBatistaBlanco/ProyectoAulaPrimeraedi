/**
 * websocket.js — Cliente STOMP mínimo (sin dependencias externas)
 * -----------------------------------------------------------------
 * El backend publica en:
 *   /topic/chat/{idCliente}            → mensajes del hilo de conversación
 *   /topic/notificaciones/{idUsuario}  → notificaciones del usuario
 *
 * No se usa SockJS: se habla WebSocket nativo con STOMP sobre texto
 * (los mensajes del servidor llegan serializados en JSON).
 *
 * Regla de oro: si el WebSocket no está disponible, `suscribir` devuelve
 * `false` y quien llama sigue funcionando igual con su sondeo periódico.
 */

const Ws = {
  _socket: null,
  _suscriptores: new Map(),
  _intento: null,
  _espera: [],

  /** URL del endpoint STOMP del backend (mismo origen que la API). */
  url() {
    const origen = window.API_BASE
      ? new URL(window.API_BASE, window.location.href).origin
      : window.location.origin;
    return origen.replace(/^http/, 'ws') + '/ws';
  },

  _conectar() {
    if (this._socket && this._socket.readyState <= 1) return this._intento;
    if (this._intento) return this._intento;

    this._intento = new Promise((resolver) => {
      let socket;
      try {
        socket = new WebSocket(this.url());
      } catch {
        this._intento = null;
        resolver(false);
        return;
      }

      const conteo = setTimeout(() => {
        if (socket.readyState !== 1) {
          try { socket.close(); } catch { /* ya cerrado */ }
          this._intento = null;
          resolver(false);
        }
      }, 5000);

      socket.onopen = () => {
        clearTimeout(conteo);
        this._socket = socket;
        socket.send('CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0');
        resolver(true);
        // Re-suscribe lo que se pidió antes de tener conexión.
        const pendientes = [...this._espera];
        this._espera = [];
        pendientes.forEach(({ destino, manejador }) => this._enviarSuscripcion(destino, manejador));
      };

      socket.onmessage = (evento) => this._recibir(evento.data);

      socket.onerror = () => {
        clearTimeout(conteo);
        this._intento = null;
        resolver(false);
      };

      socket.onclose = () => {
        clearTimeout(conteo);
        this._socket = null;
        this._intento = null;
      };
    });

    return this._intento;
  },

  /** Procesa los frames STOMP que llegan del servidor. */
  _recibir(datos) {
    String(datos).split('\0').forEach((frame) => {
      if (!frame.trim()) return;
      const corte = frame.indexOf('\n\n');
      const cabeceras = (corte === -1 ? frame : frame.slice(0, corte)).split('\n');
      const cuerpo = corte === -1 ? '' : frame.slice(corte + 2);
      if (!cabeceras.length || cabeceras[0].indexOf('MESSAGE') !== 0) return;

      let destino = null;
      cabeceras.forEach((linea) => {
        const [clave, ...resto] = linea.split(':');
        if (clave === 'destination') destino = resto.join(':');
      });

      const manejador = destino && this._suscriptores.get(destino);
      if (!manejador) return;

      try {
        manejador(JSON.parse(cuerpo));
      } catch {
        /* cuerpo no JSON: se ignora */
      }
    });
  },

  _idSuscripcion() {
    return 'sub-' + Math.random().toString(36).slice(2);
  },

  _enviarSuscripcion(destino, manejador) {
    this._suscriptores.set(destino, manejador);
    this._socket.send(`SUBSCRIBE\nid:${this._idSuscripcion()}\ndestination:${destino}\n\n\0`);
  },

  /**
   * Suscribe `manejador` a un destino STOMP.
   * Devuelve `true` si la suscripción quedó activa, `false` si no hay
   * WebSocket disponible (el llamador debe usar su respaldo por sondeo).
   */
  async suscribir(destino, manejador) {
    if (this._socket && this._socket.readyState === 1) {
      this._enviarSuscripcion(destino, manejador);
      return true;
    }
    this._espera.push({ destino, manejador });
    const conectado = await this._conectar();
    if (!conectado) {
      this._espera = this._espera.filter((s) => s.manejador !== manejador);
    }
    return conectado;
  }
};
