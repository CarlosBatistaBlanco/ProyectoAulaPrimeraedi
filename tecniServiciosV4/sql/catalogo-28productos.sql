-- =====================================================================
-- Catalogo TECNISECURE: 7 productos por categoria (28 en total)
-- Las imagenes son provisionales (img/productos/<categoria>-NN.jpg).
-- Para reemplazar por fotos reales conserva el mismo nombre de archivo.
-- =====================================================================

START TRANSACTION;

DELETE FROM productos;
ALTER TABLE productos AUTO_INCREMENT = 1;

INSERT INTO productos (id_categoria, id_marca, nombre, sku, descripcion, precio, descuento_pct, stock, stock_minimo, garantia_meses, impuesto_pct, imagen, destacado, activo) VALUES
-- ---------------- CAMARAS DE SEGURIDAD (categoria 1) ----------------
(1,1,'Camara Domo 4K IP','CAM-DOM-4K-001','Domo antivandálico de 8 MP con visión nocturna IR de 30 m, detección de movimiento por IA y resistencia IP67. Ideal para interiores y fachadas.',249900,15.00,25,5,12,19.00,'img/productos/camaras-01.jpg',1,1),
(1,2,'Camara Bullet IP 1080p','CAM-BUL-1080-001','Cámara tipo bala de 2 MP con lente de 3.6 mm, infrarrojo de 30 m y carcasa metálica IP66. Perfecta para perímetros y entradas.',179900,0.00,40,5,12,19.00,'img/productos/camaras-02.jpg',1,1),
(1,1,'Camara PTZ 360 con Seguimiento','CAM-PTZ-360-001','Cámara motorizada con giro de 355° y seguimiento automático de personas, zoom óptico 4x y audio bidireccional vía app.',689900,20.00,8,5,12,19.00,'img/productos/camaras-03.jpg',1,1),
(1,3,'Camara WiFi Interior 2K','CAM-WIFI-2K-001','Cámara compacta de 3 MP para interiores con audio bidireccional, almacenamiento en microSD y notificaciones al celular.',149900,0.00,35,5,12,19.00,'img/productos/camaras-04.jpg',0,1),
(1,2,'Camara ColorVu Full Color','CAM-CLRVU-001','Visión a color en completa oscuridad gracias a su apertura F1.0 e iluminación de 25 m. Grabación continua 24 horas.',439900,10.00,15,5,12,19.00,'img/productos/camaras-05.jpg',0,1),
(1,1,'Camara Solar 4G Autónoma','CAM-SOLAR-4G-001','Cámara inalámbrica con panel solar y conectividad 4G LTE. No requiere cableado ni corriente eléctrica. Batería de 10.400 mAh.',899900,0.00,6,3,12,19.00,'img/productos/camaras-06.jpg',0,1),
(1,2,'Camara Ojo de Pez 360°','CAM-FISHEYE-001','Lente ojo de pez de 5 MP que cubre 360° sin puntos ciegos. Una sola cámara reemplaza cuatro convencionales.',529900,0.00,10,5,12,19.00,'img/productos/camaras-07.jpg',0,1),

-- ---------------- ALARMAS (categoria 2) ----------------
(2,4,'Alarma Perimetral Inalambrica','ALM-PERI-001','Sistema de alarma con sensores de movimiento y sirena de 120 dB. Control y notificaciones desde la aplicación móvil.',349900,25.00,18,5,12,19.00,'img/productos/alarmas-01.jpg',1,1),
(2,4,'Panel de Alarma GSM','ALM-GSM-001','Central de alarma con notificación por llamada y SMS ante cualquier activación. Funciona sin internet ni línea fija.',289900,0.00,22,5,12,19.00,'img/productos/alarmas-02.jpg',0,1),
(2,4,'Sensor de Movimiento PIR','ALM-PIR-001','Detector infrarrojo pasivo con alcance de 12 m y ángulo de 110°. Inmune a mascotas de hasta 25 kg.',79900,15.00,60,10,12,19.00,'img/productos/alarmas-03.jpg',0,1),
(2,4,'Sirena Exterior con Luz','ALM-SIRENA-001','Sirena de 115 dB con destello estroboscópico y batería de respaldo. Carcasa resistente a la intemperie.',119900,0.00,30,5,12,19.00,'img/productos/alarmas-04.jpg',0,1),
(2,4,'Contacto Magnetico Puerta','ALM-MAG-001','Sensor de apertura para puertas y ventanas con batería de larga duración y aviso inmediato al panel.',34900,0.00,80,15,12,19.00,'img/productos/alarmas-05.jpg',0,1),
(2,4,'Detector de Humo Inteligente','ALM-HUMO-001','Detector fotoeléctrico de humo con aviso audible de 85 dB y alerta simultánea al celular del usuario.',89900,0.00,40,10,12,19.00,'img/productos/alarmas-06.jpg',0,1),
(2,4,'Boton de Panico Inalambrico','ALM-PANICO-001','Pulsador de emergencia con alcance de 80 m que activa la alarma y envía la notificación al instante.',49900,0.00,50,10,12,19.00,'img/productos/alarmas-07.jpg',0,1),

-- ---------------- CONTROL DE ACCESO (categoria 3) ----------------
(3,4,'Control de Acceso Biometrico','CTRL-BIO-001','Lector de huella y tarjeta RFID para control de puertas. Registro de entradas y salidas con reportes exportables.',459900,18.00,12,5,12,19.00,'img/productos/acceso-01.jpg',1,1),
(3,4,'Cerraduras Electronicas','CTRL-CERRA-001','Cerraduras digitales con teclado, huella o app. Permiten crear accesos temporales para visitantes.',389900,0.00,14,5,12,19.00,'img/productos/acceso-02.jpg',0,1),
(3,4,'Videoportero WiFi','CTRL-PORTERO-001','Videoportero con cámara HD de 160°, audio bidireccional y desbloqueo remoto desde el celular.',329900,10.00,20,5,12,19.00,'img/productos/acceso-03.jpg',1,1),
(3,4,'Torniquete de Acceso','CTRL-TORNI-001','Torniquete de acero inoxidable con lectura de tarjeta o huella. Apto para oficinas y recepciones de alto flujo.',1899900,0.00,5,2,12,19.00,'img/productos/acceso-04.jpg',0,1),
(3,4,'Lector RFID de Larga Distancia','CTRL-RFID-001','Lector RFID de largo alcance que abre la barrera sin contacto hasta 8 m de distancia.',259900,0.00,16,5,12,19.00,'img/productos/acceso-05.jpg',0,1),
(3,4,'Control de Acceso Vehicular','CTRL-VEHIC-001','Sistema con barrera automática y lector para parqueaderos. Registro de placas y cobro por tiempo.',2450000,0.00,4,2,12,19.00,'img/productos/acceso-06.jpg',0,1),
(3,4,'Cerraduras Electromagneticas','CTRL-MAG-001','Electroimán de 280 kg de retención para puertas de seguridad. Liberación ante corte de energía.',189900,0.00,25,5,12,19.00,'img/productos/acceso-07.jpg',0,1),

-- ---------------- ACCESORIOS (categoria 4) ----------------
(4,3,'Switch PoE 8 Puertos Gigabit','ACC-SW-POE-001','Alimenta hasta 8 cámaras IP por cable de red sin enchufes adicionales. 120 W de potencia total.',289900,12.00,20,5,12,19.00,'img/productos/accesorios-01.jpg',1,1),
(4,4,'Disco Duro Vigilancia 2TB','ACC-HDD-2TB-001','Diseñado para grabación 24/7 en grabadores DVR/NVR con tecnología antipérdida de cuadros de video.',219900,12.00,30,5,12,19.00,'img/productos/accesorios-02.jpg',0,1),
(4,4,'Cable UTP Cat6 305 Metros','ACC-UTP-CAT6-001','Bobina de cable de red 100% cobre para exteriores con protección UV. Transmisión confiable a 1 Gbps.',189900,0.00,15,3,12,19.00,'img/productos/accesorios-03.jpg',0,1),
(4,4,'Balun Pasivo para Camaras','ACC-BALUN-001','Par de transceptores pasivos para transmitir video y energía por cable UTP hasta 300 metros.',18900,0.00,100,20,12,19.00,'img/productos/accesorios-04.jpg',0,1),
(4,4,'Fuente de Poder Centralizada','ACC-FUENTE-001','Caja metálica con llave, 9 salidas de 12V 10A con fusibles independientes para proteger las cámaras.',99900,0.00,25,5,12,19.00,'img/productos/accesorios-05.jpg',0,1),
(4,4,'Soporte de Camara Pared','ACC-SOPORTE-001','Brazo metálico con paso interno de cables para cámaras domo o bala. Resistente a la intemperie.',29900,20.00,45,10,12,19.00,'img/productos/accesorios-06.jpg',0,1),
(4,4,'Caja de Paso Estanca IP65','ACC-CAJA-001','Caja de conexiones para ocultar cables y conectores en exteriores protegiéndolos de agua y polvo.',14900,0.00,120,25,12,19.00,'img/productos/accesorios-07.jpg',0,1);

COMMIT;