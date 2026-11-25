-- =====================================================================
-- PROYECTO DE AULA: TECNISERVICIOS (TECNISECURE) - VERSION 2.0 (branch v2)
-- Script DDL de Creacion de Tablas (Esquema Relacional Real - 27 Tablas)
-- Sincronizado con las entidades JPA de com.tecnicsecure.entity (origin/v2)
-- Compatible con: H2 Database (MODE=MySQL) / MySQL 8.x / MariaDB
-- Codificacion: UTF-8
-- =====================================================================

-- =====================================================================
-- MODULO 1: USUARIOS, ROLES, PERFILES, DIRECCIONES Y SEGURIDAD
-- =====================================================================

-- 1. Tabla de Roles
CREATE TABLE IF NOT EXISTS roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE
);

-- 2. Tabla de Usuarios (credenciales, bloqueo por fuerza bruta y perfil base)
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    documento VARCHAR(30) UNIQUE,
    telefono VARCHAR(30),
    avatar VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    bloqueado_hasta TIMESTAMP NULL,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    ultimo_acceso TIMESTAMP NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_roles FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

-- 3. Tabla de Clientes (asociacion 1:1 opcional con usuarios)
CREATE TABLE IF NOT EXISTS clientes (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_clientes_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 4. Tabla de Empleados (asociacion 1:1 obligatoria con usuarios)
CREATE TABLE IF NOT EXISTS empleados (
    id_empleado INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    cargo VARCHAR(100) DEFAULT 'Empleado',
    fecha_ingreso DATE NULL,
    CONSTRAINT fk_empleados_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 5. Perfiles Operativos y Permisos de Modulos por Empleado
CREATE TABLE IF NOT EXISTS empleados_perfiles (
    id_perfil_empleado INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL UNIQUE,
    area VARCHAR(40) NOT NULL DEFAULT 'OPERATIVO',
    modulos_permitidos VARCHAR(300) NOT NULL DEFAULT 'dashboard,tareas,perfil,direccion,ventas,pedidos,facturacion,inventario,productos,soporte,chat',
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_emp_perfiles_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 6. Tareas Asignadas a Empleados (seguimiento, evidencias y calificacion)
CREATE TABLE IF NOT EXISTS tareas_empleados (
    id_tarea INT AUTO_INCREMENT PRIMARY KEY,
    id_empleado_usuario INT NOT NULL,
    id_asignador_usuario INT NULL,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    prioridad VARCHAR(20) NOT NULL DEFAULT 'media',
    fecha_limite DATE NULL,
    estado VARCHAR(25) NOT NULL DEFAULT 'pendiente',
    evidencia TEXT,
    comentario_empleado TEXT,
    calificacion INT NULL,
    observacion_admin TEXT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completada_en TIMESTAMP NULL,
    CONSTRAINT fk_tareas_empleado FOREIGN KEY (id_empleado_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_tareas_asignador FOREIGN KEY (id_asignador_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 6.1. Historial Archivado de Entregas y Revisiones de Tareas
CREATE TABLE IF NOT EXISTS tareas_historial_entregas (
    id_entrega INT AUTO_INCREMENT PRIMARY KEY,
    id_tarea INT NOT NULL,
    numero_intento INT NOT NULL,
    evidencia TEXT,
    comentario_empleado TEXT,
    estado_revision VARCHAR(30) NOT NULL DEFAULT 'en_revision',
    calificacion INT NULL,
    observacion_admin TEXT,
    enviado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revisado_en TIMESTAMP NULL,
    id_revisor_usuario INT NULL,
    CONSTRAINT fk_hist_entrega_tarea FOREIGN KEY (id_tarea) REFERENCES tareas_empleados(id_tarea) ON DELETE CASCADE,
    CONSTRAINT fk_hist_entrega_revisor FOREIGN KEY (id_revisor_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 7. Direcciones de Entrega por Usuario (hasta 15 por usuario, 1 predeterminada)
CREATE TABLE IF NOT EXISTS direcciones_usuario (
    id_direccion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    etiqueta VARCHAR(60) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    ciudad VARCHAR(100),
    barrio VARCHAR(100),
    telefono VARCHAR(30),
    nombre_recibe VARCHAR(120),
    referencia VARCHAR(255),
    latitud DOUBLE NULL,
    longitud DOUBLE NULL,
    predeterminada BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_direcciones_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 8. Tokens de Recuperacion de Contrasena
CREATE TABLE IF NOT EXISTS tokens_recuperacion (
    id_token INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    token VARCHAR(100) NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tokens_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 9. Auditoria y Logs de Actividad
CREATE TABLE IF NOT EXISTS logs_actividad (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NULL,
    accion VARCHAR(100) NOT NULL,
    detalle VARCHAR(255),
    ip VARCHAR(45),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- =====================================================================
-- MODULO 2: CATALOGO, INVENTARIO, FAVORITOS Y RESENAS
-- =====================================================================

-- 10. Categorias de Productos
CREATE TABLE IF NOT EXISTS categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL
);

-- 11. Marcas de Productos
CREATE TABLE IF NOT EXISTS marcas (
    id_marca INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

-- 12. Productos de Seguridad Electronica
CREATE TABLE IF NOT EXISTS productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    id_categoria INT NULL,
    id_marca INT NULL,
    nombre VARCHAR(150) NOT NULL,
    sku VARCHAR(40) UNIQUE,
    codigo_barras VARCHAR(40) UNIQUE,
    descripcion TEXT,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    descuento_pct DECIMAL(5,2) DEFAULT 0.00,
    impuesto_pct DECIMAL(5,2) DEFAULT 19.00,
    garantia_meses INT DEFAULT 12,
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT DEFAULT 5,
    stock_maximo INT DEFAULT 100,
    imagen VARCHAR(255) DEFAULT 'img/productos/sin-imagen.jpg',
    destacado BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_productos_categorias FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
    CONSTRAINT fk_productos_marcas FOREIGN KEY (id_marca) REFERENCES marcas(id_marca)
);

-- 13. Galeria de Imagenes de Producto
CREATE TABLE IF NOT EXISTS producto_imagenes (
    id_imagen INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    ruta VARCHAR(255) NOT NULL,
    orden INT DEFAULT 0,
    CONSTRAINT fk_imagenes_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

-- 14. Movimientos de Inventario (Kardex)
CREATE TABLE IF NOT EXISTS inventario_movimientos (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_usuario INT NULL,
    tipo VARCHAR(10) NOT NULL, -- ENTRADA, SALIDA, AJUSTE
    cantidad INT NOT NULL,
    stock_resultante INT NOT NULL,
    motivo VARCHAR(255),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT fk_inv_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 15. Lista de Deseos / Favoritos (PK compuesta usuario-producto)
CREATE TABLE IF NOT EXISTS favoritos (
    id_usuario INT NOT NULL,
    id_producto INT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, id_producto),
    CONSTRAINT fk_fav_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_fav_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

-- 16. Resenas y Calificaciones de Productos (una por usuario y producto)
CREATE TABLE IF NOT EXISTS resenas (
    id_resena INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    id_usuario INT NOT NULL,
    calificacion INT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario TEXT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unico_resena UNIQUE (id_producto, id_usuario),
    CONSTRAINT fk_res_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE,
    CONSTRAINT fk_res_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- =====================================================================
-- MODULO 3: PEDIDOS, TRAZABILIDAD Y FACTURACION
-- =====================================================================

-- 17. Pedidos / Ordenes de Compra
-- Estados validos (EstadoPedido): pendiente, confirmado, en_preparacion,
-- empacado, enviado, entregado, cancelado
CREATE TABLE IF NOT EXISTS pedidos (
    id_pedido INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NULL,
    id_empleado_registro INT NULL,
    total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(30) DEFAULT 'pendiente',
    direccion_entrega VARCHAR(255),
    latitud DOUBLE NULL,
    longitud DOUBLE NULL,
    metodo_pago VARCHAR(40),
    detalle_pago VARCHAR(200),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pedidos_clientes FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON DELETE SET NULL,
    CONSTRAINT fk_pedidos_empleado FOREIGN KEY (id_empleado_registro) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 18. Detalle de Pedido (Lineas de Compra)
CREATE TABLE IF NOT EXISTS pedido_detalle (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_pedidos FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_productos FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- 19. Historial de Cambios de Estado del Pedido
CREATE TABLE IF NOT EXISTS pedidos_historial (
    id_historial INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL,
    estado_anterior VARCHAR(30) NULL,
    estado_nuevo VARCHAR(30) NOT NULL,
    id_usuario INT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_pedidos FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_hist_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 20. Facturas
CREATE TABLE IF NOT EXISTS facturas (
    id_factura INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido INT NOT NULL UNIQUE,
    numero_factura VARCHAR(20) NOT NULL UNIQUE,
    subtotal DECIMAL(12,2) NOT NULL,
    impuestos DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    descuento DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(12,2) NOT NULL,
    estado VARCHAR(20) DEFAULT 'emitida',
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_facturas_pedidos FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido)
);

-- =====================================================================
-- MODULO 4: SERVICIOS TECNICOS, SOPORTE, CHAT PUBLICO Y NOTIFICACIONES
-- =====================================================================

-- 21. Catalogo de Servicios Tecnicos
CREATE TABLE IF NOT EXISTS servicios (
    id_servicio INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    icono VARCHAR(100) DEFAULT 'shield',
    orden INT DEFAULT 0
);

-- 22. Solicitudes de Servicio Tecnico y Cotizaciones (formulario publico)
CREATE TABLE IF NOT EXISTS solicitudes_servicio (
    id_solicitud INT AUTO_INCREMENT PRIMARY KEY,
    id_servicio INT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    direccion VARCHAR(255),
    tipo VARCHAR(20) NOT NULL DEFAULT 'cotizacion',
    fecha_preferida DATE NULL,
    hora_preferida TIME NULL,
    descripcion TEXT,
    tecnico_asignado VARCHAR(120),
    observaciones TEXT,
    estado VARCHAR(20) DEFAULT 'pendiente',
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sol_servicios FOREIGN KEY (id_servicio) REFERENCES servicios(id_servicio)
);

-- 23. Mensajes de Contacto Web
CREATE TABLE IF NOT EXISTS contactos (
    id_contacto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    asunto VARCHAR(150),
    tipo VARCHAR(20) DEFAULT 'mensaje',
    mensaje TEXT NOT NULL,
    respuesta TEXT,
    id_usuario INT NULL,
    leido BOOLEAN DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contactos_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 24. Mensajes de Chat en Vivo (Clientes Registrados <-> Asesores)
CREATE TABLE IF NOT EXISTS mensajes_chat (
    id_mensaje INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    remitente VARCHAR(10) NOT NULL, -- cliente, soporte
    id_usuario_envia INT NULL,
    mensaje TEXT NOT NULL,
    leido BOOLEAN DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_clientes FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON DELETE CASCADE,
    CONSTRAINT fk_chat_usuario_envia FOREIGN KEY (id_usuario_envia) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 25. Conversaciones de Visitantes sin Cuenta (Asistente IA + Escalado Humano)
CREATE TABLE IF NOT EXISTS conversaciones_visitante (
    id_conversacion INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,
    nombre VARCHAR(120),
    correo VARCHAR(150),
    telefono VARCHAR(30),
    id_solicitud INT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'abierta',
    sin_leer INT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conv_visita_solicitud FOREIGN KEY (id_solicitud) REFERENCES solicitudes_servicio(id_solicitud) ON DELETE SET NULL
);

-- 26. Mensajes del Chat Publico de Visitantes (visitante / bot / asesor)
CREATE TABLE IF NOT EXISTS mensajes_visitante (
    id_mensaje INT AUTO_INCREMENT PRIMARY KEY,
    id_conversacion INT NOT NULL,
    remitente VARCHAR(12) NOT NULL, -- visitante, bot, asesor
    id_usuario_envia INT NULL,
    mensaje TEXT NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_msg_visita_conv FOREIGN KEY (id_conversacion) REFERENCES conversaciones_visitante(id_conversacion) ON DELETE CASCADE,
    CONSTRAINT fk_msg_visita_usuario FOREIGN KEY (id_usuario_envia) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
);

-- 27. Notificaciones del Sistema
CREATE TABLE IF NOT EXISTS notificaciones (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje VARCHAR(255),
    enlace VARCHAR(255),
    leida BOOLEAN DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_usuarios FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);
