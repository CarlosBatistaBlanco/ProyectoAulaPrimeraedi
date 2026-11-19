-- =====================================================================
-- TECNISECURE · Datos iniciales
-- ---------------------------------------------------------------------
-- Se ejecuta al arrancar la aplicación, DESPUÉS de que Hibernate crea
-- las tablas (ver CargaDatosIniciales.java).
-- Los INSERT son idempotentes: si el dato ya existe, no se duplica.
-- Separador de sentencias: ^^^
-- Codificación: UTF-8 (sin BOM)
-- =====================================================================

-- Roles ---------------------------------------------------------------
INSERT INTO roles (nombre)
SELECT 'administrador' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'administrador')^^^
INSERT INTO roles (nombre)
SELECT 'empleado' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'empleado')^^^
INSERT INTO roles (nombre)
SELECT 'cliente' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'cliente')^^^

-- Categorías ----------------------------------------------------------
INSERT INTO categorias (nombre)
SELECT 'Cámaras de Seguridad' WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Cámaras de Seguridad')^^^
INSERT INTO categorias (nombre)
SELECT 'Alarmas' WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Alarmas')^^^
INSERT INTO categorias (nombre)
SELECT 'Control de Acceso' WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Control de Acceso')^^^
INSERT INTO categorias (nombre)
SELECT 'Accesorios' WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE nombre = 'Accesorios')^^^
-- Marcas --------------------------------------------------------------
INSERT INTO marcas (nombre)
SELECT 'Hikvision' WHERE NOT EXISTS (SELECT 1 FROM marcas WHERE nombre = 'Hikvision')^^^
INSERT INTO marcas (nombre)
SELECT 'Dahua' WHERE NOT EXISTS (SELECT 1 FROM marcas WHERE nombre = 'Dahua')^^^
INSERT INTO marcas (nombre)
SELECT 'TP-Link' WHERE NOT EXISTS (SELECT 1 FROM marcas WHERE nombre = 'TP-Link')^^^
INSERT INTO marcas (nombre)
SELECT 'Genérica' WHERE NOT EXISTS (SELECT 1 FROM marcas WHERE nombre = 'Genérica')^^^

-- Servicios -----------------------------------------------------------
INSERT INTO servicios (titulo, descripcion, icono, orden)
SELECT 'Instalación de Cámaras', 'Instalación profesional de sistemas CCTV en hogares, negocios e industrias.', 'camera', 1
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE titulo = 'Instalación de Cámaras')^^^
INSERT INTO servicios (titulo, descripcion, icono, orden)
SELECT 'Monitoreo 24/7', 'Monitoreo remoto en tiempo real desde tu celular o computador, todos los días del año.', 'monitor', 2
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE titulo = 'Monitoreo 24/7')^^^
INSERT INTO servicios (titulo, descripcion, icono, orden)
SELECT 'Alarmas y Sensores', 'Diseño e instalación de sistemas de alarma contra intrusos con sensores de movimiento.', 'bell', 3
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE titulo = 'Alarmas y Sensores')^^^
INSERT INTO servicios (titulo, descripcion, icono, orden)
SELECT 'Control de Acceso', 'Sistemas biométricos, tarjetas RFID y citófonos para control de ingreso.', 'lock', 4
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE titulo = 'Control de Acceso')^^^
INSERT INTO servicios (titulo, descripcion, icono, orden)
SELECT 'Mantenimiento Preventivo', 'Revisión periódica de equipos para garantizar el funcionamiento óptimo del sistema.', 'wrench', 5
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE titulo = 'Mantenimiento Preventivo')^^^
INSERT INTO servicios (titulo, descripcion, icono, orden)
SELECT 'Asesoría y Diseño', 'Diagnóstico del espacio y propuesta a la medida de la seguridad que necesitas.', 'clipboard', 6
WHERE NOT EXISTS (SELECT 1 FROM servicios WHERE titulo = 'Asesoría y Diseño')^^^

-- Limpieza y normalización de servicios duplicados o con codificación dañada --
UPDATE servicios
SET titulo = 'Instalación de Cámaras',
    descripcion = 'Instalación profesional de sistemas CCTV en hogares, negocios e industrias.'
WHERE LOWER(titulo) LIKE 'instalaci%c%maras%'^^^
UPDATE servicios
SET titulo = 'Asesoría y Diseño',
    descripcion = 'Diagnóstico del espacio y propuesta a la medida de la seguridad que necesitas.'
WHERE LOWER(titulo) LIKE 'asesor%dise%o%'^^^
UPDATE servicios
SET titulo = 'Control de Acceso',
    descripcion = 'Sistemas biométricos, tarjetas RFID y citófonos para control de ingreso.'
WHERE LOWER(titulo) LIKE 'control de acceso%'^^^
UPDATE servicios
SET titulo = 'Mantenimiento Preventivo',
    descripcion = 'Revisión periódica de equipos para garantizar el funcionamiento óptimo del sistema.'
WHERE LOWER(titulo) LIKE 'mantenimiento preventivo%'^^^
UPDATE solicitudes_servicio
SET id_servicio = (
    SELECT MIN(sv2.id_servicio)
    FROM servicios sv2
    WHERE sv2.titulo = (SELECT sv1.titulo FROM servicios sv1 WHERE sv1.id_servicio = solicitudes_servicio.id_servicio)
)
WHERE id_servicio IS NOT NULL
  AND id_servicio <> (
    SELECT MIN(sv2.id_servicio)
    FROM servicios sv2
    WHERE sv2.titulo = (SELECT sv1.titulo FROM servicios sv1 WHERE sv1.id_servicio = solicitudes_servicio.id_servicio)
)^^^
DELETE FROM servicios
WHERE id_servicio > (
    SELECT MIN(sv2.id_servicio)
    FROM servicios sv2
    WHERE sv2.titulo = servicios.titulo
)
AND NOT EXISTS (SELECT 1 FROM solicitudes_servicio s WHERE s.id_servicio = servicios.id_servicio)^^^

-- Limpieza de categorías duplicadas por codificación dañada ----------------
-- Algunas bases migradas guardaron "Cámaras de Seguridad" con caracteres rotos.
-- Se reasignan sus productos a la categoría correcta y se borra el duplicado.
UPDATE productos SET id_categoria = (SELECT MIN(id_categoria) FROM categorias WHERE nombre = 'Cámaras de Seguridad')
WHERE id_categoria IN (SELECT id_categoria FROM categorias WHERE nombre <> 'Cámaras de Seguridad' AND nombre LIKE 'C%maras de Seguridad')^^^
DELETE FROM categorias WHERE nombre <> 'Cámaras de Seguridad' AND nombre LIKE 'C%maras de Seguridad'
AND NOT EXISTS (SELECT 1 FROM productos p WHERE p.id_categoria = categorias.id_categoria)^^^

-- Elimina la categoría de prueba usada al validar el catálogo ----------------
DELETE FROM categorias WHERE nombre = 'ZZPrueba'
AND NOT EXISTS (SELECT 1 FROM productos p WHERE p.id_categoria = categorias.id_categoria)^^^
-- Normaliza nombres escritos con codificación dañada (bases migradas) --------
UPDATE productos SET nombre = 'Cámara Domo 4K IP' WHERE nombre = 'CÃ¡mara Domo 4K IP'^^^
UPDATE productos SET nombre = 'Cámara Bullet IP 1080p' WHERE nombre = 'CÃ¡mara Bullet IP 1080p'^^^
UPDATE productos SET nombre = 'Alarma Perimetral Inalámbrica' WHERE nombre = 'Alarma Perimetral InalÃ¡mbrica'^^^
UPDATE productos SET nombre = 'Control de Acceso Biométrico' WHERE nombre = 'Control de Acceso BiomÃ©trico'^^^
UPDATE productos SET nombre = 'Cámara PTZ 360°' WHERE nombre = 'Cámara PTZ 360Â°'^^^
UPDATE productos SET nombre = 'Soporte de Cámara Pared' WHERE nombre = 'Soporte de Montaje para CÃ¡mara'^^^
UPDATE productos SET descripcion = REPLACE(REPLACE(REPLACE(REPLACE(descripcion, 'Ã³', 'ó'), 'Ã¡', 'á'), 'Ã©', 'é'), 'Ã', 'í')
WHERE descripcion LIKE '%Ã%'^^^
UPDATE categorias SET nombre = 'Cámaras de Seguridad' WHERE nombre = 'CÃ¡maras de Seguridad'^^^
UPDATE productos SET id_marca = (SELECT MIN(id_marca) FROM marcas WHERE nombre = 'Genérica')
WHERE id_marca IN (SELECT id_marca FROM marcas WHERE nombre <> 'Genérica' AND nombre LIKE 'Gen%rica')^^^
DELETE FROM marcas WHERE nombre <> 'Genérica' AND nombre LIKE 'Gen%rica'
AND NOT EXISTS (SELECT 1 FROM productos p WHERE p.id_marca = marcas.id_marca)^^^
UPDATE productos SET nombre = 'Cámara PTZ 360°' WHERE nombre LIKE 'C%maras PTZ%' AND nombre NOT LIKE '%°'^^^
UPDATE productos
SET nombre = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(nombre, 'Ã³', 'ó'), 'Ã¡', 'á'), 'Ã©', 'é'), 'Ã', 'í'), 'Â°', '°')
WHERE nombre LIKE '%Ã%' OR nombre LIKE '%Â%'^^^

-- Productos ----------------------------------------------------------
-- 28 productos (7 por categoría). Las imágenes están en frontend/img/productos.
-- Cada INSERT es idempotente por SKU: se puede ejecutar en cada arranque.
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Cámaras de Seguridad')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Hikvision')),
       'Cámara Domo 4K IP', 'CAM-DOM-4K-001',
       'Domo antivandálico IP67 de 8 MP (4K), lente 2.8 mm, IR de 30 m con Smart IR, detección de movimiento por IA (persona/vehículo) y compresión H.265+ para ahorrar disco.',
       249900.00, 15.00, 19.00, 12, 25, 5, 100, 'img/productos/camaras-01.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CAM-DOM-4K-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Cámaras de Seguridad')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Dahua')),
       'Cámara Bullet IP 1080p', 'CAM-BUL-1080-001',
       'Bala metálica IP66 de 2 MP (1080p), sensor CMOS 1/2.8" con WDR de 120 dB, lente fija 3.6 mm, IR de 30 m y ranura microSD para grabación local.',
       179900.00, 0.00, 19.00, 12, 40, 5, 160, 'img/productos/camaras-02.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CAM-BUL-1080-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Cámaras de Seguridad')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Hikvision')),
       'Cámara PTZ 360 con Seguimiento', 'CAM-PTZ-360-001',
       'PTZ motorizada con giro horizontal de 355° y vertical de 90°, zoom óptico 4x, auto-seguimiento de personas y audio bidireccional. Control remoto desde la app.',
       689900.00, 20.00, 19.00, 12, 8, 5, 32, 'img/productos/camaras-03.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CAM-PTZ-360-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Cámaras de Seguridad')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('TP-Link')),
       'Cámara WiFi Interior 2K', 'CAM-WIFI-2K-001',
       'Cámara WiFi 2K (3 MP) con paneo 360° y tilt 114°, visión nocturna hasta 9 m, audio bidireccional, zonas de privacidad y almacenamiento microSD hasta 512 GB.',
       149900.00, 0.00, 19.00, 12, 35, 5, 140, 'img/productos/camaras-04.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CAM-WIFI-2K-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Cámaras de Seguridad')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Dahua')),
       'Cámara ColorVu Full Color', 'CAM-CLRVU-001',
       'Tecnología ColorVu: imagen a color 24/7 incluso sin luz, apertura F1.0, iluminación integrada de 25 m y audio integrado para grabación continua.',
       439900.00, 10.00, 19.00, 12, 15, 5, 60, 'img/productos/camaras-05.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CAM-CLRVU-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Cámaras de Seguridad')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Hikvision')),
       'Cámara Solar 4G Autónoma', 'CAM-SOLAR-4G-001',
       'Cámara autónoma con panel solar de 6 W y batería de 10.400 mAh, conectividad 4G LTE sin cableado, detección PIR y visión nocturna a color.',
       899900.00, 0.00, 19.00, 12, 6, 3, 24, 'img/productos/camaras-06.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CAM-SOLAR-4G-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Cámaras de Seguridad')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Dahua')),
       'Cámara Ojo de Pez 360°', 'CAM-FISHEYE-001',
       'Ojo de pez de 5 MP con cobertura de 360° sin puntos ciegos, modo dewarp para vista panorámica y detección de personas. Reemplaza 4 cámaras.',
       529900.00, 0.00, 19.00, 12, 10, 5, 40, 'img/productos/camaras-07.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CAM-FISHEYE-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Alarmas')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Alarma Perimetral Inalámbrica', 'ALM-PERI-001',
       'Central de alarma inalámbrica con sensores de movimiento, sirena de 120 dB, armado por zonas, control desde app y batería de respaldo ante cortes.',
       349900.00, 25.00, 19.00, 12, 18, 5, 72, 'img/productos/alarmas-01.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ALM-PERI-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Alarmas')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Panel de Alarma GSM', 'ALM-GSM-001',
       'Panel de alarma GSM con tarjeta SIM: notifica por llamada y SMS ante cualquier activación, funciona sin internet ni línea fija, soporta hasta 100 sensores.',
       289900.00, 0.00, 19.00, 12, 22, 5, 88, 'img/productos/alarmas-02.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ALM-GSM-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Alarmas')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Sensor de Movimiento PIR', 'ALM-PIR-001',
       'Detector infrarrojo pasivo (PIR) con alcance de 12 m y ángulo de 110°, sensibilidad ajustable e inmunidad a mascotas de hasta 25 kg.',
       79900.00, 15.00, 19.00, 12, 60, 10, 240, 'img/productos/alarmas-03.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ALM-PIR-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Alarmas')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Sirena Exterior con Luz', 'ALM-SIRENA-001',
       'Sirena exterior de 115 dB con destello estroboscópico, batería de respaldo recargable y carcasa IP65 resistente a lluvia y polvo.',
       119900.00, 0.00, 19.00, 12, 30, 5, 120, 'img/productos/alarmas-04.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ALM-SIRENA-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Alarmas')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Contacto Magnético Puerta', 'ALM-MAG-001',
       'Contacto magnético para puertas y ventanas, instalación inalámbrica sin cables, batería de larga duración y aviso inmediato de apertura al panel.',
       34900.00, 0.00, 19.00, 12, 80, 15, 320, 'img/productos/alarmas-05.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ALM-MAG-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Alarmas')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Detector de Humo Inteligente', 'ALM-HUMO-001',
       'Detector fotoeléctrico de humo con alarma audible de 85 dB, indicador LED de estado y alerta simultánea al celular del usuario.',
       89900.00, 0.00, 19.00, 12, 40, 10, 160, 'img/productos/alarmas-06.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ALM-HUMO-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Alarmas')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Botón de Pánico Inalámbrico', 'ALM-PANICO-001',
       'Botón de pánico inalámbrico de una pulsación con alcance de 80 m, activa la sirena y envía alerta inmediata a la app y contactos configurados.',
       49900.00, 0.00, 19.00, 12, 50, 10, 200, 'img/productos/alarmas-07.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ALM-PANICO-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Control de Acceso')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Control de Acceso Biométrico', 'CTRL-BIO-001',
       'Lector biométrico de huella y tarjeta RFID (125 kHz/13.56 MHz) para control de puertas, capacidad de 3.000 huellas y reportes de entradas/salidas exportables.',
       459900.00, 18.00, 19.00, 12, 12, 5, 48, 'img/productos/acceso-01.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CTRL-BIO-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Control de Acceso')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Cerraduras Electrónicas', 'CTRL-CERRA-001',
       'Cerradura digital con teclado, huella y app, acceso por código temporal para visitantes, registro de aperturas y batería de larga duración.',
       389900.00, 0.00, 19.00, 12, 14, 5, 56, 'img/productos/acceso-02.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CTRL-CERRA-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Control de Acceso')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Videoportero WiFi', 'CTRL-PORTERO-001',
       'Videoportero WiFi con cámara HD de 160°, audio bidireccional, timbre en app y desbloqueo remoto de la puerta desde el celular.',
       329900.00, 10.00, 19.00, 12, 20, 5, 80, 'img/productos/acceso-03.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CTRL-PORTERO-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Control de Acceso')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Torniquete de Acceso', 'CTRL-TORNI-001',
       'Torniquete de acero inoxidable con lectura de tarjeta o huella, mecanismo bidireccional y relé de salida para integrar con alarma e incendio.',
       1899900.00, 0.00, 19.00, 12, 5, 2, 20, 'img/productos/acceso-04.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CTRL-TORNI-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Control de Acceso')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Lector RFID de Larga Distancia', 'CTRL-RFID-001',
       'Lector RFID de largo alcance con lectura sin contacto hasta 8 m, ideal para parqueaderos y control vehicular de flujo rápido.',
       259900.00, 0.00, 19.00, 12, 16, 5, 64, 'img/productos/acceso-05.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CTRL-RFID-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Control de Acceso')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Control de Acceso Vehicular', 'CTRL-VEHIC-001',
       'Sistema vehicular con barrera automática de 3 m, lector de tarjetas y registro de placas con cobro por tiempo para parqueaderos.',
       2450000.00, 0.00, 19.00, 12, 4, 2, 16, 'img/productos/acceso-06.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CTRL-VEHIC-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Control de Acceso')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Cerraduras Electromagnéticas', 'CTRL-MAG-001',
       'Electroimán de 280 kg de fuerza de retención, liberación automática ante corte de energía y sensor de puerta para instalaciones de seguridad.',
       189900.00, 0.00, 19.00, 12, 25, 5, 100, 'img/productos/acceso-07.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'CTRL-MAG-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Accesorios')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('TP-Link')),
       'Switch PoE 8 Puertos Gigabit', 'ACC-SW-POE-001',
       'Switch PoE+ de 8 puertos Gigabit con 120 W de presupuesto total, alimenta cámaras IP y access points sin enchufes adicionales.',
       289900.00, 12.00, 19.00, 12, 20, 5, 80, 'img/productos/accesorios-01.jpg', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ACC-SW-POE-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Accesorios')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Disco Duro Vigilancia 2TB', 'ACC-HDD-2TB-001',
       'Disco duro de 2 TB de grado vigilancia, diseñado para grabación continua 24/7 en DVR/NVR con tecnología antipérdida de cuadros.',
       219900.00, 12.00, 19.00, 12, 30, 5, 120, 'img/productos/accesorios-02.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ACC-HDD-2TB-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Accesorios')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Cable UTP Cat6 305 Metros', 'ACC-UTP-CAT6-001',
       'Bobina de cable UTP Cat6 de 305 m, 100% cobre, apto para exterior con protección UV y soporte de 1 Gbps en tendidos de hasta 100 m.',
       189900.00, 0.00, 19.00, 12, 15, 3, 60, 'img/productos/accesorios-03.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ACC-UTP-CAT6-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Accesorios')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Balun Pasivo para Cámaras', 'ACC-BALUN-001',
       'Par de baluns pasivos que transmiten video y alimentación por cable UTP hasta 300 m, reduciendo costo de cableado coaxial.',
       18900.00, 0.00, 19.00, 12, 100, 20, 400, 'img/productos/accesorios-04.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ACC-BALUN-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Accesorios')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Fuente de Poder Centralizada', 'ACC-FUENTE-001',
       'Fuente centralizada de 12V 10A en caja metálica con llave y 9 salidas con fusible independiente para proteger cada cámara.',
       99900.00, 0.00, 19.00, 12, 25, 5, 100, 'img/productos/accesorios-05.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ACC-FUENTE-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Accesorios')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Soporte de Cámara Pared', 'ACC-SOPORTE-001',
       'Brazo metálico ajustable con paso interno de cables para cámaras domo o bala, acabado anticorrosivo apto para intemperie.',
       29900.00, 20.00, 19.00, 12, 45, 10, 180, 'img/productos/accesorios-06.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ACC-SOPORTE-001')^^^
INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, impuesto_pct, garantia_meses, stock, stock_minimo, stock_maximo, imagen, destacado, activo)
SELECT (SELECT MIN(id_categoria) FROM categorias WHERE LOWER(nombre) = LOWER('Accesorios')),
       (SELECT MIN(id_marca) FROM marcas WHERE LOWER(nombre) = LOWER('Genérica')),
       'Caja de Paso Estanca IP65', 'ACC-CAJA-001',
       'Caja de paso estanca IP65 para ocultar cables y conectores en exteriores, protege las conexiones de agua, polvo y sol.',
       14900.00, 0.00, 19.00, 12, 120, 25, 480, 'img/productos/accesorios-07.jpg', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE sku = 'ACC-CAJA-001')^^^


-- Mantiene las descripciones al día en bases creadas antes de este cambio ---
UPDATE productos SET descripcion = 'Domo antivandálico IP67 de 8 MP (4K), lente 2.8 mm, IR de 30 m con Smart IR, detección de movimiento por IA (persona/vehículo) y compresión H.265+ para ahorrar disco.' WHERE sku = 'CAM-DOM-4K-001' AND (descripcion IS NULL OR descripcion <> 'Domo antivandálico IP67 de 8 MP (4K), lente 2.8 mm, IR de 30 m con Smart IR, detección de movimiento por IA (persona/vehículo) y compresión H.265+ para ahorrar disco.')^^^
UPDATE productos SET descripcion = 'Bala metálica IP66 de 2 MP (1080p), sensor CMOS 1/2.8" con WDR de 120 dB, lente fija 3.6 mm, IR de 30 m y ranura microSD para grabación local.' WHERE sku = 'CAM-BUL-1080-001' AND (descripcion IS NULL OR descripcion <> 'Bala metálica IP66 de 2 MP (1080p), sensor CMOS 1/2.8" con WDR de 120 dB, lente fija 3.6 mm, IR de 30 m y ranura microSD para grabación local.')^^^
UPDATE productos SET descripcion = 'PTZ motorizada con giro horizontal de 355° y vertical de 90°, zoom óptico 4x, auto-seguimiento de personas y audio bidireccional. Control remoto desde la app.' WHERE sku = 'CAM-PTZ-360-001' AND (descripcion IS NULL OR descripcion <> 'PTZ motorizada con giro horizontal de 355° y vertical de 90°, zoom óptico 4x, auto-seguimiento de personas y audio bidireccional. Control remoto desde la app.')^^^
UPDATE productos SET descripcion = 'Cámara WiFi 2K (3 MP) con paneo 360° y tilt 114°, visión nocturna hasta 9 m, audio bidireccional, zonas de privacidad y almacenamiento microSD hasta 512 GB.' WHERE sku = 'CAM-WIFI-2K-001' AND (descripcion IS NULL OR descripcion <> 'Cámara WiFi 2K (3 MP) con paneo 360° y tilt 114°, visión nocturna hasta 9 m, audio bidireccional, zonas de privacidad y almacenamiento microSD hasta 512 GB.')^^^
UPDATE productos SET descripcion = 'Tecnología ColorVu: imagen a color 24/7 incluso sin luz, apertura F1.0, iluminación integrada de 25 m y audio integrado para grabación continua.' WHERE sku = 'CAM-CLRVU-001' AND (descripcion IS NULL OR descripcion <> 'Tecnología ColorVu: imagen a color 24/7 incluso sin luz, apertura F1.0, iluminación integrada de 25 m y audio integrado para grabación continua.')^^^
UPDATE productos SET descripcion = 'Cámara autónoma con panel solar de 6 W y batería de 10.400 mAh, conectividad 4G LTE sin cableado, detección PIR y visión nocturna a color.' WHERE sku = 'CAM-SOLAR-4G-001' AND (descripcion IS NULL OR descripcion <> 'Cámara autónoma con panel solar de 6 W y batería de 10.400 mAh, conectividad 4G LTE sin cableado, detección PIR y visión nocturna a color.')^^^
UPDATE productos SET descripcion = 'Ojo de pez de 5 MP con cobertura de 360° sin puntos ciegos, modo dewarp para vista panorámica y detección de personas. Reemplaza 4 cámaras.' WHERE sku = 'CAM-FISHEYE-001' AND (descripcion IS NULL OR descripcion <> 'Ojo de pez de 5 MP con cobertura de 360° sin puntos ciegos, modo dewarp para vista panorámica y detección de personas. Reemplaza 4 cámaras.')^^^
UPDATE productos SET descripcion = 'Central de alarma inalámbrica con sensores de movimiento, sirena de 120 dB, armado por zonas, control desde app y batería de respaldo ante cortes.' WHERE sku = 'ALM-PERI-001' AND (descripcion IS NULL OR descripcion <> 'Central de alarma inalámbrica con sensores de movimiento, sirena de 120 dB, armado por zonas, control desde app y batería de respaldo ante cortes.')^^^
UPDATE productos SET descripcion = 'Panel de alarma GSM con tarjeta SIM: notifica por llamada y SMS ante cualquier activación, funciona sin internet ni línea fija, soporta hasta 100 sensores.' WHERE sku = 'ALM-GSM-001' AND (descripcion IS NULL OR descripcion <> 'Panel de alarma GSM con tarjeta SIM: notifica por llamada y SMS ante cualquier activación, funciona sin internet ni línea fija, soporta hasta 100 sensores.')^^^
UPDATE productos SET descripcion = 'Detector infrarrojo pasivo (PIR) con alcance de 12 m y ángulo de 110°, sensibilidad ajustable e inmunidad a mascotas de hasta 25 kg.' WHERE sku = 'ALM-PIR-001' AND (descripcion IS NULL OR descripcion <> 'Detector infrarrojo pasivo (PIR) con alcance de 12 m y ángulo de 110°, sensibilidad ajustable e inmunidad a mascotas de hasta 25 kg.')^^^
UPDATE productos SET descripcion = 'Sirena exterior de 115 dB con destello estroboscópico, batería de respaldo recargable y carcasa IP65 resistente a lluvia y polvo.' WHERE sku = 'ALM-SIRENA-001' AND (descripcion IS NULL OR descripcion <> 'Sirena exterior de 115 dB con destello estroboscópico, batería de respaldo recargable y carcasa IP65 resistente a lluvia y polvo.')^^^
UPDATE productos SET descripcion = 'Contacto magnético para puertas y ventanas, instalación inalámbrica sin cables, batería de larga duración y aviso inmediato de apertura al panel.' WHERE sku = 'ALM-MAG-001' AND (descripcion IS NULL OR descripcion <> 'Contacto magnético para puertas y ventanas, instalación inalámbrica sin cables, batería de larga duración y aviso inmediato de apertura al panel.')^^^
UPDATE productos SET descripcion = 'Detector fotoeléctrico de humo con alarma audible de 85 dB, indicador LED de estado y alerta simultánea al celular del usuario.' WHERE sku = 'ALM-HUMO-001' AND (descripcion IS NULL OR descripcion <> 'Detector fotoeléctrico de humo con alarma audible de 85 dB, indicador LED de estado y alerta simultánea al celular del usuario.')^^^
UPDATE productos SET descripcion = 'Botón de pánico inalámbrico de una pulsación con alcance de 80 m, activa la sirena y envía alerta inmediata a la app y contactos configurados.' WHERE sku = 'ALM-PANICO-001' AND (descripcion IS NULL OR descripcion <> 'Botón de pánico inalámbrico de una pulsación con alcance de 80 m, activa la sirena y envía alerta inmediata a la app y contactos configurados.')^^^
UPDATE productos SET descripcion = 'Lector biométrico de huella y tarjeta RFID (125 kHz/13.56 MHz) para control de puertas, capacidad de 3.000 huellas y reportes de entradas/salidas exportables.' WHERE sku = 'CTRL-BIO-001' AND (descripcion IS NULL OR descripcion <> 'Lector biométrico de huella y tarjeta RFID (125 kHz/13.56 MHz) para control de puertas, capacidad de 3.000 huellas y reportes de entradas/salidas exportables.')^^^
UPDATE productos SET descripcion = 'Cerradura digital con teclado, huella y app, acceso por código temporal para visitantes, registro de aperturas y batería de larga duración.' WHERE sku = 'CTRL-CERRA-001' AND (descripcion IS NULL OR descripcion <> 'Cerradura digital con teclado, huella y app, acceso por código temporal para visitantes, registro de aperturas y batería de larga duración.')^^^
UPDATE productos SET descripcion = 'Videoportero WiFi con cámara HD de 160°, audio bidireccional, timbre en app y desbloqueo remoto de la puerta desde el celular.' WHERE sku = 'CTRL-PORTERO-001' AND (descripcion IS NULL OR descripcion <> 'Videoportero WiFi con cámara HD de 160°, audio bidireccional, timbre en app y desbloqueo remoto de la puerta desde el celular.')^^^
UPDATE productos SET descripcion = 'Torniquete de acero inoxidable con lectura de tarjeta o huella, mecanismo bidireccional y relé de salida para integrar con alarma e incendio.' WHERE sku = 'CTRL-TORNI-001' AND (descripcion IS NULL OR descripcion <> 'Torniquete de acero inoxidable con lectura de tarjeta o huella, mecanismo bidireccional y relé de salida para integrar con alarma e incendio.')^^^
UPDATE productos SET descripcion = 'Lector RFID de largo alcance con lectura sin contacto hasta 8 m, ideal para parqueaderos y control vehicular de flujo rápido.' WHERE sku = 'CTRL-RFID-001' AND (descripcion IS NULL OR descripcion <> 'Lector RFID de largo alcance con lectura sin contacto hasta 8 m, ideal para parqueaderos y control vehicular de flujo rápido.')^^^
UPDATE productos SET descripcion = 'Sistema vehicular con barrera automática de 3 m, lector de tarjetas y registro de placas con cobro por tiempo para parqueaderos.' WHERE sku = 'CTRL-VEHIC-001' AND (descripcion IS NULL OR descripcion <> 'Sistema vehicular con barrera automática de 3 m, lector de tarjetas y registro de placas con cobro por tiempo para parqueaderos.')^^^
UPDATE productos SET descripcion = 'Electroimán de 280 kg de fuerza de retención, liberación automática ante corte de energía y sensor de puerta para instalaciones de seguridad.' WHERE sku = 'CTRL-MAG-001' AND (descripcion IS NULL OR descripcion <> 'Electroimán de 280 kg de fuerza de retención, liberación automática ante corte de energía y sensor de puerta para instalaciones de seguridad.')^^^
UPDATE productos SET descripcion = 'Switch PoE+ de 8 puertos Gigabit con 120 W de presupuesto total, alimenta cámaras IP y access points sin enchufes adicionales.' WHERE sku = 'ACC-SW-POE-001' AND (descripcion IS NULL OR descripcion <> 'Switch PoE+ de 8 puertos Gigabit con 120 W de presupuesto total, alimenta cámaras IP y access points sin enchufes adicionales.')^^^
UPDATE productos SET descripcion = 'Disco duro de 2 TB de grado vigilancia, diseñado para grabación continua 24/7 en DVR/NVR con tecnología antipérdida de cuadros.' WHERE sku = 'ACC-HDD-2TB-001' AND (descripcion IS NULL OR descripcion <> 'Disco duro de 2 TB de grado vigilancia, diseñado para grabación continua 24/7 en DVR/NVR con tecnología antipérdida de cuadros.')^^^
UPDATE productos SET descripcion = 'Bobina de cable UTP Cat6 de 305 m, 100% cobre, apto para exterior con protección UV y soporte de 1 Gbps en tendidos de hasta 100 m.' WHERE sku = 'ACC-UTP-CAT6-001' AND (descripcion IS NULL OR descripcion <> 'Bobina de cable UTP Cat6 de 305 m, 100% cobre, apto para exterior con protección UV y soporte de 1 Gbps en tendidos de hasta 100 m.')^^^
UPDATE productos SET descripcion = 'Par de baluns pasivos que transmiten video y alimentación por cable UTP hasta 300 m, reduciendo costo de cableado coaxial.' WHERE sku = 'ACC-BALUN-001' AND (descripcion IS NULL OR descripcion <> 'Par de baluns pasivos que transmiten video y alimentación por cable UTP hasta 300 m, reduciendo costo de cableado coaxial.')^^^
UPDATE productos SET descripcion = 'Fuente centralizada de 12V 10A en caja metálica con llave y 9 salidas con fusible independiente para proteger cada cámara.' WHERE sku = 'ACC-FUENTE-001' AND (descripcion IS NULL OR descripcion <> 'Fuente centralizada de 12V 10A en caja metálica con llave y 9 salidas con fusible independiente para proteger cada cámara.')^^^
UPDATE productos SET descripcion = 'Brazo metálico ajustable con paso interno de cables para cámaras domo o bala, acabado anticorrosivo apto para intemperie.' WHERE sku = 'ACC-SOPORTE-001' AND (descripcion IS NULL OR descripcion <> 'Brazo metálico ajustable con paso interno de cables para cámaras domo o bala, acabado anticorrosivo apto para intemperie.')^^^
UPDATE productos SET descripcion = 'Caja de paso estanca IP65 para ocultar cables y conectores en exteriores, protege las conexiones de agua, polvo y sol.' WHERE sku = 'ACC-CAJA-001' AND (descripcion IS NULL OR descripcion <> 'Caja de paso estanca IP65 para ocultar cables y conectores en exteriores, protege las conexiones de agua, polvo y sol.')^^^


-- Kardex inicial (solo para productos sin movimientos registrados) -----
INSERT INTO inventario_movimientos (id_producto, tipo, cantidad, stock_resultante, motivo)
SELECT p.id_producto, 'entrada', p.stock, p.stock, 'Inventario inicial de apertura'
FROM productos p
WHERE NOT EXISTS (SELECT 1 FROM inventario_movimientos m WHERE m.id_producto = p.id_producto)
