-- =====================================================================
-- PROYECTO DE AULA: TECNISERVICIOS (TECNISECURE)
-- Script DDL de Creación de Tablas (Esquema Relacional)
-- Compatible con: MySQL 8.x / MariaDB / H2 Database
-- Codificación: UTF-8
-- =====================================================================

-- 1. Tabla de Roles
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE
);

-- 2. Tabla de Usuarios (Credenciales y datos generales)
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    rol_id INT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL,
    CONSTRAINT fk_usuarios_roles FOREIGN KEY (rol_id) REFERENCES roles(id)
);

-- 3. Tabla de Clientes (Extensión de Usuario)
CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT PRIMARY KEY,
    documento_tipo VARCHAR(10),
    documento_numero VARCHAR(30) UNIQUE,
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    CONSTRAINT fk_clientes_usuarios FOREIGN KEY (id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 4. Tabla de Empleados / Técnicos (Extensión de Usuario)
CREATE TABLE IF NOT EXISTS empleados (
    id BIGINT PRIMARY KEY,
    cargo VARCHAR(100) NOT NULL,
    especialidad VARCHAR(150),
    fecha_contratacion DATE,
    CONSTRAINT fk_empleados_usuarios FOREIGN KEY (id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 5. Tokens de Recuperación de Contraseña
CREATE TABLE IF NOT EXISTS tokens_recuperacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tokens_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 6. Tabla de Categorías de Productos
CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    icono VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 7. Tabla de Marcas
CREATE TABLE IF NOT EXISTS marcas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    logo_url VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 8. Tabla de Productos (Seguridad Electrónica)
CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(12,2) NOT NULL,
    descuento_porcentaje INT NOT NULL DEFAULT 0,
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 5,
    categoria_id BIGINT NOT NULL,
    marca_id BIGINT NOT NULL,
    destacado BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_productos_categorias FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    CONSTRAINT fk_productos_marcas FOREIGN KEY (marca_id) REFERENCES marcas(id)
);

-- 9. Galería de Imágenes de Producto
CREATE TABLE IF NOT EXISTS producto_imagenes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    orden INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_imagenes_productos FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

-- 10. Movimientos de Inventario (Kardex)
CREATE TABLE IF NOT EXISTS inventario_movimientos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL, -- ENTRADA, SALIDA, AJUSTE
    cantidad INT NOT NULL,
    stock_anterior INT NOT NULL,
    stock_nuevo INT NOT NULL,
    motivo VARCHAR(255),
    usuario_id BIGINT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_productos FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT fk_inv_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- 11. Lista de Deseos (Favoritos N:M)
CREATE TABLE IF NOT EXISTS favoritos (
    cliente_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cliente_id, producto_id),
    CONSTRAINT fk_fav_clientes FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_productos FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

-- 12. Estados de Pedido
CREATE TABLE IF NOT EXISTS estados_pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE, -- PENDIENTE, PAGADO, EN_CAMINO, ENTREGADO, CANCELADO
    nombre VARCHAR(50) NOT NULL
);

-- 13. Pedidos / Órdenes de Compra
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_pedido VARCHAR(30) NOT NULL UNIQUE,
    cliente_id BIGINT NOT NULL,
    estado_id INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    descuento_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    impuestos DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(12,2) NOT NULL,
    direccion_envio VARCHAR(255) NOT NULL,
    telefono_contacto VARCHAR(30) NOT NULL,
    notas TEXT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL,
    CONSTRAINT fk_pedidos_clientes FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_pedidos_estados FOREIGN KEY (estado_id) REFERENCES estados_pedido(id)
);

-- 14. Detalle de Pedido (Ítems)
CREATE TABLE IF NOT EXISTS pedido_detalle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    descuento_unitario DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_pedidos FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_productos FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- 15. Historial de Cambios de Estado del Pedido
CREATE TABLE IF NOT EXISTS pedidos_historial (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    estado_id INT NOT NULL,
    observacion VARCHAR(255),
    cambiado_por BIGINT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hist_pedidos FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_hist_estados FOREIGN KEY (estado_id) REFERENCES estados_pedido(id),
    CONSTRAINT fk_hist_usuarios FOREIGN KEY (cambiado_por) REFERENCES usuarios(id)
);

-- 16. Facturación
CREATE TABLE IF NOT EXISTS facturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_factura VARCHAR(50) NOT NULL UNIQUE,
    pedido_id BIGINT NOT NULL UNIQUE,
    monto_total DECIMAL(12,2) NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_facturas_pedidos FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
);

-- 17. Catálogo de Servicios Técnicos
CREATE TABLE IF NOT EXISTS servicios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    descripcion TEXT,
    precio_base DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 18. Solicitudes de Servicio Técnico (Instalación, Reparación)
CREATE TABLE IF NOT EXISTS solicitudes_servicio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    servicio_id BIGINT NOT NULL,
    empleado_id BIGINT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'SOLICITADO', -- SOLICITADO, ASIGNADO, EN_PROCESO, FINALIZADO
    direccion VARCHAR(255) NOT NULL,
    descripcion_falla TEXT,
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_programada TIMESTAMP NULL,
    observaciones_tecnico TEXT,
    CONSTRAINT fk_sol_clientes FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_sol_servicios FOREIGN KEY (servicio_id) REFERENCES servicios(id),
    CONSTRAINT fk_sol_empleados FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

-- 19. Mensajes de Soporte y Chat en Vivo
CREATE TABLE IF NOT EXISTS mensajes_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    empleado_id BIGINT NULL,
    enviado_por VARCHAR(20) NOT NULL, -- CLIENTE, EMPLEADO
    contenido TEXT NOT NULL,
    leido BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_clientes FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_chat_empleados FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

-- 20. Reseñas y Calificaciones de Productos
CREATE TABLE IF NOT EXISTS resenas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    calificacion INT NOT NULL CHECK (calificacion BETWEEN 1 AND 5),
    comentario TEXT,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_clientes FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_res_productos FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- 21. Notificaciones del Sistema
CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 22. Mensajes de Contacto Web
CREATE TABLE IF NOT EXISTS contactos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    mensaje TEXT NOT NULL,
    atendido BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 23. Auditoría y Logs del Sistema
CREATE TABLE IF NOT EXISTS logs_actividad (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NULL,
    accion VARCHAR(100) NOT NULL,
    detalles TEXT,
    ip_origen VARCHAR(50),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_usuarios FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);
