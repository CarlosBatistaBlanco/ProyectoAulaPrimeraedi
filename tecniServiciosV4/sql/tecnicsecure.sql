-- =====================================================================
-- TECNISECURE - Base de datos
-- Importar este archivo en phpMyAdmin (XAMPP) o MySQL Workbench
-- =====================================================================
--
-- IMPORTANTE: este script SIEMPRE empieza borrando la base de datos
-- anterior (si existía) y la crea desde cero. Esto es a propósito:
-- evita el error más común al reimportar ("Table ... already exists"),
-- que en MySQL Workbench detiene TODO el resto del script apenas
-- ocurre, dejando tablas nuevas (como resenas o favoritos) sin crear.
-- Si ya tenías datos reales cargados (productos propios, pedidos,
-- clientes), haz un respaldo antes desde
-- panel/administrador/respaldo.php.
-- =====================================================================

DROP DATABASE IF EXISTS tecnicsecure;

CREATE DATABASE IF NOT EXISTS tecnicsecure
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE tecnicsecure;

-- ---------------------------------------------------------------------
-- Tabla: categorias  (categorías de productos, ej: Cámaras, Alarmas...)
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL
);

-- ---------------------------------------------------------------------
-- Tabla: productos  (lo que se vende en el carrito)
-- ---------------------------------------------------------------------
CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    id_categoria INT,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0,
    stock INT NOT NULL DEFAULT 0,
    imagen VARCHAR(255) DEFAULT 'img/productos/sin-imagen.jpg',
    destacado TINYINT(1) DEFAULT 0,
    activo TINYINT(1) DEFAULT 1,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria)
        ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- Tabla: servicios  (servicios que ofrece la empresa)
-- ---------------------------------------------------------------------
CREATE TABLE servicios (
    id_servicio INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    icono VARCHAR(100) DEFAULT 'shield',
    orden INT DEFAULT 0
);

-- ---------------------------------------------------------------------
-- Tabla: clientes (se crea automáticamente al primer pedido/contacto)
-- ---------------------------------------------------------------------
CREATE TABLE clientes (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- Tabla: pedidos (encabezado de una compra del carrito)
-- ---------------------------------------------------------------------
CREATE TABLE pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    estado ENUM('pendiente','confirmado','enviado','cancelado') DEFAULT 'pendiente',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
        ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- Tabla: pedido_detalle (líneas de cada pedido)
-- ---------------------------------------------------------------------
CREATE TABLE pedido_detalle (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- ---------------------------------------------------------------------
-- Tabla: contactos (mensajes del formulario de contacto)
-- ---------------------------------------------------------------------
CREATE TABLE contactos (
    id_contacto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    asunto VARCHAR(150),
    mensaje TEXT NOT NULL,
    leido TINYINT(1) DEFAULT 0,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
-- DATOS DE EJEMPLO
-- =====================================================================

INSERT INTO categorias (nombre) VALUES
('Cámaras de Seguridad'),
('Alarmas'),
('Control de Acceso'),
('Accesorios');

INSERT INTO productos (id_categoria, nombre, descripcion, precio, stock, imagen, destacado) VALUES
(1, 'Cámara Domo 4K IP', 'Cámara domo de interior/exterior, resolución 4K, visión nocturna 30m, detección de movimiento.', 249900, 25, 'img/productos/camara-domo.jpg', 1),
(1, 'Cámara Bullet IP 1080p', 'Cámara tipo bala para exteriores, resistente al agua IP66, visión nocturna infrarroja.', 179900, 40, 'img/productos/camara-bullet.jpg', 1),
(1, 'Kit 4 Cámaras + DVR', 'Kit completo de 4 cámaras con grabador DVR de 1TB incluido, cableado e instalación.', 1299900, 10, 'img/productos/kit-camaras.jpg', 1),
(2, 'Alarma Perimetral Inalámbrica', 'Sistema de alarma con sensores de movimiento y sirena de 120dB, control vía app.', 349900, 18, 'img/productos/alarma.jpg', 0),
(3, 'Control de Acceso Biométrico', 'Lector de huella y tarjeta RFID para control de acceso de puertas.', 459900, 12, 'img/productos/control-acceso.jpg', 1),
(4, 'Disco Duro Vigilancia 2TB', 'Disco duro especial para grabación continua 24/7, compatible con DVR/NVR.', 219900, 30, 'img/productos/disco-duro.jpg', 0);

INSERT INTO servicios (titulo, descripcion, icono, orden) VALUES
('Instalación de Cámaras', 'Instalación profesional de sistemas CCTV en hogares, negocios e industrias.', 'camera', 1),
('Monitoreo 24/7', 'Monitoreo remoto en tiempo real desde tu celular o computador, todos los días del año.', 'monitor', 2),
('Alarmas y Sensores', 'Diseño e instalación de sistemas de alarma contra intrusos con sensores de movimiento.', 'bell', 3),
('Control de Acceso', 'Sistemas biométricos, tarjetas RFID y citófonos para control de ingreso.', 'lock', 4),
('Mantenimiento Preventivo', 'Revisión periódica de equipos para garantizar el funcionamiento óptimo del sistema.', 'wrench', 5),
('Asesoría y Diseño', 'Diagnóstico del espacio y propuesta a la medida de la seguridad que necesitas.', 'clipboard', 6);

-- ---------------------------------------------------------------------
-- Tabla: solicitudes_servicio (cuando un cliente pide cotización o cita
-- para un servicio específico, ej. "Instalación de Cámaras")
-- ---------------------------------------------------------------------
CREATE TABLE solicitudes_servicio (
    id_solicitud INT AUTO_INCREMENT PRIMARY KEY,
    id_servicio INT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    direccion VARCHAR(255),
    tipo ENUM('cotizacion','cita') NOT NULL DEFAULT 'cotizacion',
    fecha_preferida DATE NULL,
    hora_preferida TIME NULL,
    descripcion TEXT,
    estado ENUM('pendiente','contactado','agendado','completado','cancelado') DEFAULT 'pendiente',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_servicio) REFERENCES servicios(id_servicio) ON DELETE CASCADE
);

-- =====================================================================
-- FASE 1: AUTENTICACIÓN, ROLES Y PANELES
-- =====================================================================

-- ---------------------------------------------------------------------
-- Tabla: roles (administrador, empleado, cliente)
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE
);

-- ---------------------------------------------------------------------
-- Tabla: usuarios (login de administradores, empleados y clientes)
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    documento VARCHAR(30) UNIQUE,
    telefono VARCHAR(30),
    avatar VARCHAR(255) NULL,
    password_hash VARCHAR(255) NOT NULL,
    activo TINYINT(1) DEFAULT 1,
    bloqueado TINYINT(1) DEFAULT 0,
    bloqueado_hasta DATETIME NULL,
    intentos_fallidos INT DEFAULT 0,
    ultimo_acceso TIMESTAMP NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

-- ---------------------------------------------------------------------
-- Tabla: empleados (datos adicionales de un usuario con rol "empleado")
-- ---------------------------------------------------------------------
CREATE TABLE empleados (
    id_empleado INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    cargo VARCHAR(100) DEFAULT 'Empleado',
    fecha_ingreso DATE DEFAULT (CURRENT_DATE),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: tokens_recuperacion (recuperar contraseña olvidada)
-- ---------------------------------------------------------------------
CREATE TABLE tokens_recuperacion (
    id_token INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    token VARCHAR(100) NOT NULL,
    expira_en DATETIME NOT NULL,
    usado TINYINT(1) DEFAULT 0,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: recordar_tokens (checkbox "Recordarme" del login)
-- ---------------------------------------------------------------------
CREATE TABLE recordar_tokens (
    id_recordar INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    selector VARCHAR(40) NOT NULL UNIQUE,
    validador_hash VARCHAR(255) NOT NULL,
    expira_en DATETIME NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Tabla: logs_actividad (auditoría: quién hizo qué y cuándo)
-- ---------------------------------------------------------------------
CREATE TABLE logs_actividad (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NULL,
    accion VARCHAR(100) NOT NULL,
    detalle VARCHAR(255),
    ip VARCHAR(45),
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- Vincular clientes con su cuenta de usuario (login propio)
-- ---------------------------------------------------------------------
ALTER TABLE clientes
    ADD COLUMN id_usuario INT UNIQUE NULL AFTER id_cliente,
    ADD FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

-- ---------------------------------------------------------------------
-- Datos semilla: roles + 1 administrador de prueba
-- ---------------------------------------------------------------------
INSERT INTO roles (nombre) VALUES ('administrador'), ('empleado'), ('cliente');

-- Usuario administrador de prueba
-- Correo: admin@tecnicsecure.com   |   Contraseña: Admin123!
INSERT INTO usuarios (id_rol, nombre, correo, password_hash, activo)
VALUES (
  (SELECT id_rol FROM roles WHERE nombre = 'administrador'),
  'Administrador Principal',
  'admin@tecnicsecure.com',
  '$2y$10$8FLlQqkdr4CMLbxvdKLfku8Nvev/R34PhYEXj46CImRIoJ/nIqfu.',
  1
);

-- =====================================================================
-- FASE 2: PRODUCTOS AVANZADOS + INVENTARIO
-- =====================================================================

CREATE TABLE marcas (
    id_marca INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

ALTER TABLE productos
    ADD COLUMN id_marca INT NULL AFTER id_categoria,
    ADD COLUMN sku VARCHAR(40) UNIQUE NULL AFTER nombre,
    ADD COLUMN codigo_barras VARCHAR(40) UNIQUE NULL AFTER sku,
    ADD COLUMN descuento_pct DECIMAL(5,2) DEFAULT 0,
    ADD COLUMN impuesto_pct DECIMAL(5,2) DEFAULT 19.00,
    ADD COLUMN garantia_meses INT DEFAULT 12,
    ADD COLUMN stock_minimo INT DEFAULT 5,
    ADD COLUMN stock_maximo INT DEFAULT 100,
    ADD FOREIGN KEY (id_marca) REFERENCES marcas(id_marca) ON DELETE SET NULL;

-- Varias imágenes por producto (además de la imagen principal en `productos.imagen`)
CREATE TABLE producto_imagenes (
    id_imagen INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    ruta VARCHAR(255) NOT NULL,
    orden INT DEFAULT 0,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

-- Kardex: cada entrada/salida/ajuste de inventario queda registrada aquí
CREATE TABLE inventario_movimientos (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_usuario INT NULL,
    tipo ENUM('entrada','salida','ajuste') NOT NULL,
    cantidad INT NOT NULL,
    stock_resultante INT NOT NULL,
    motivo VARCHAR(255),
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

INSERT INTO marcas (nombre) VALUES ('Hikvision'), ('Dahua'), ('TP-Link'), ('Genérica');

UPDATE productos SET id_marca = 1, sku = 'CAM-DOM-4K-001' WHERE nombre = 'Cámara Domo 4K IP';
UPDATE productos SET id_marca = 2, sku = 'CAM-BUL-1080-001' WHERE nombre = 'Cámara Bullet IP 1080p';
UPDATE productos SET id_marca = 1, sku = 'KIT-4CAM-DVR-001' WHERE nombre = 'Kit 4 Cámaras + DVR';
UPDATE productos SET id_marca = 4, sku = 'ALM-PERI-001' WHERE nombre = 'Alarma Perimetral Inalámbrica';
UPDATE productos SET id_marca = 3, sku = 'CTRL-BIO-001' WHERE nombre = 'Control de Acceso Biométrico';
UPDATE productos SET id_marca = 4, sku = 'HDD-2TB-001' WHERE nombre = 'Disco Duro Vigilancia 2TB';

-- Registra el stock inicial de cada producto como un movimiento de entrada (arranque del Kardex)
INSERT INTO inventario_movimientos (id_producto, tipo, cantidad, stock_resultante, motivo)
SELECT id_producto, 'entrada', stock, stock, 'Inventario inicial de apertura' FROM productos;

-- =====================================================================
-- FASE 3: PEDIDOS (flujo de estados) + FACTURACIÓN
-- =====================================================================

-- Amplía los estados del pedido al flujo completo pedido por el negocio
ALTER TABLE pedidos MODIFY COLUMN estado
    ENUM('pendiente','confirmado','en_preparacion','empacado','enviado','entregado','cancelado')
    DEFAULT 'pendiente';

-- Un empleado puede registrar una venta manualmente (además del checkout web)
ALTER TABLE pedidos
    ADD COLUMN id_empleado_registro INT NULL AFTER id_cliente,
    ADD FOREIGN KEY (id_empleado_registro) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

-- Historial de cambios de estado de un pedido (auditoría específica de pedidos)
CREATE TABLE pedidos_historial (
    id_historial INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    id_usuario INT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

CREATE TABLE facturas (
    id_factura INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL UNIQUE,
    numero_factura VARCHAR(20) NOT NULL UNIQUE,
    subtotal DECIMAL(12,2) NOT NULL,
    impuestos DECIMAL(12,2) NOT NULL DEFAULT 0,
    descuento DECIMAL(12,2) NOT NULL DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,
    estado ENUM('emitida','anulada') DEFAULT 'emitida',
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE
);

-- =====================================================================
-- FASE 4: REPORTES
-- (los reportes son solo consultas SQL sobre las tablas ya existentes;
--  no necesitan tablas nuevas, ver php/reportes.php)
-- =====================================================================

-- =====================================================================
-- FASE 5: NOTIFICACIONES + ATENCIÓN AL CLIENTE + CORREOS
-- =====================================================================

CREATE TABLE notificaciones (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje VARCHAR(255),
    enlace VARCHAR(255),
    leida TINYINT(1) DEFAULT 0,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- Clasifica los mensajes de contacto en mensaje / reclamo / sugerencia
ALTER TABLE contactos
    ADD COLUMN tipo ENUM('mensaje','reclamo','sugerencia') DEFAULT 'mensaje' AFTER asunto,
    ADD COLUMN respuesta TEXT NULL,
    ADD COLUMN id_usuario INT NULL,
    ADD FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL;

-- Chat simple cliente <-> asesor (un hilo por cliente)
CREATE TABLE mensajes_chat (
    id_mensaje INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    remitente ENUM('cliente','asesor') NOT NULL,
    id_usuario_envia INT NULL,
    mensaje TEXT NOT NULL,
    leido TINYINT(1) DEFAULT 0,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario_envia) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- =====================================================================
-- FASE 6: FAVORITOS, RESEÑAS Y SEGURIDAD AVANZADA
-- =====================================================================

CREATE TABLE favoritos (
    id_usuario INT NOT NULL,
    id_producto INT NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, id_producto),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

CREATE TABLE resenas (
    id_resena INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_usuario INT NOT NULL,
    calificacion TINYINT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario TEXT,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    UNIQUE KEY unico_resena (id_producto, id_usuario)
);
