/**
 * Este archivo mantiene un canal de comunicación instantánea con el servidor para que el chat de soporte
 * y la campanita de notificaciones se actualicen al momento sin tener que recargar la página. Su efecto
 * se nota dentro de los paneles de clientes, empleados y administradores; si la conexión instantánea no
 * está disponible, las pantallas siguen funcionando con revisiones periódicas de respaldo.
 */

// Manejador central de la conexión en vivo para el chat instantáneo y las notificaciones
const Ws = {
  _socket: null,
  _suscriptores: new Map(),
  _intento: null,
  _espera: [],

  // Calcula la dirección del canal en vivo a partir de la dirección del servidor de la tienda
  url() {
    const origen = window.API_BASE
      ? new URL(window.API_BASE, window.location.href).origin
      : window.location.origin;
    return origen.replace(/^http/, 'ws') + '/ws';
  },

  // Abre el canal instantáneo con el servidor; si tarda más de 5 segundos cancela para no congelar la pantalla
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

      // Espera un máximo de 5 segundos por la conexión antes de pasar al modo de respaldo
      const conteo = setTimeout(() => {
        if (socket.readyState !== 1) {
          try { socket.close(); } catch { /* conexión ya cerrada */ }
          this._intento = null;
          resolver(false);
        }
      }, 5000);

      // Cuando el canal se abre, se presenta ante el servidor y activa los avisos que estaban en fila
      socket.onopen = () => {
        clearTimeout(conteo);
        this._socket = socket;
        socket.send('CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0');
        resolver(true);
        // Activa las escuchas que se pidieron mientras se establecía la conexión
        const pendientes = [...this._espera];
        this._espera = [];
        pendientes.forEach(({ destino, manejador }) => this._enviarSuscripcion(destino, manejador));
      };

      // Cada vez que llega un aviso del servidor, lo pasa al lector de mensajes
      socket.onmessage = (evento) => this._recibir(evento.data);

      // Si hay un fallo de red, libera el intento para que la pantalla use revisiones periódicas
      socket.onerror = () => {
        clearTimeout(conteo);
        this._intento = null;
        resolver(false);
      };

      // Cuando la conexión se cierra, limpia los datos temporales de la llamada
      socket.onclose = () => {
        clearTimeout(conteo);
        this._socket = null;
        this._intento = null;
      };
    });

    return this._intento;
  },

  // Lee cada mensaje que llega por el canal en vivo y lo entrega a la pantalla correspondiente
  _recibir(datos) {
    String(datos).split('\0').forEach((frame) => {
      if (!frame.trim()) return;
      const corte = frame.indexOf('\n\n');
      const cabeceras = (corte === -1 ? frame : frame.slice(0, corte)).split('\n');
      const cuerpo = corte === -1 ? '' : frame.slice(corte + 2);
      if (!cabeceras.length || cabeceras[0].indexOf('MESSAGE') !== 0) return;

      // Identifica si el aviso corresponde al chat o a las notificaciones de un usuario
      let destino = null;
      cabeceras.forEach((linea) => {
        const [clave, ...resto] = linea.split(':');
        if (clave === 'destination') destino = resto.join(':');
      });

      const manejador = destino && this._suscriptores.get(destino);
      if (!manejador) return;

      // Interpreta el contenido recibido y avisa a la pantalla para mostrarlo al instante
      try {
        manejador(JSON.parse(cuerpo));
      } catch {
        /* si el mensaje no trae datos legibles, se ignora */
      }
    });
  },

  // Genera un número único para identificar cada canal de escucha abierto
  _idSuscripcion() {
    return 'sub-' + Math.random().toString(36).slice(2);
  },

  // Guarda el canal en memoria y le indica al servidor qué avisos desea recibir esta pantalla
  _enviarSuscripcion(destino, manejador) {
    this._suscriptores.set(destino, manejador);
    this._socket.send(`SUBSCRIBE\nid:${this._idSuscripcion()}\ndestination:${destino}\n\n\0`);
  },

  // Conecta una pantalla a un canal en vivo y avisa si quedó activo o si debe usar revisiones periódicas
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
