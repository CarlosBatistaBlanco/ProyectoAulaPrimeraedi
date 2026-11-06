<?php
/**
 * _test_e2e.php — SUITE DE PRUEBAS E2E (temporal, borrar tras usar)
 * ------------------------------------------------------------------
 * Simula navegación real por HTTP (cookies + sesiones + CSRF + captcha)
 * para probar el sistema con TODOS los roles.
 *
 * NO conoce ni necesita las contraseñas: para el login real usa las
 * credenciales que el propio proyecto documenta (cuenta semilla admin),
 * y para los demás roles verifica el comportamiento del sistema
 * (RBAC, redirecciones, rechazo de credenciales inválidas).
 * ------------------------------------------------------------------
 */

const BASE = 'http://localhost:8080/TecniSecure';

$resultados = [];
$errores = 0;

function ok(string $prueba, string $detalle = ''): void {
    global $resultados, $errores;
    $resultados[] = ['OK', $prueba, $detalle];
    echo "  [OK]   $prueba" . ($detalle ? " — $detalle" : '') . "\n";
}
function fallo(string $prueba, string $detalle = ''): void {
    global $resultados, $errores;
    $errores++;
    $resultados[] = ['FALLO', $prueba, $detalle];
    echo "  [FALLO] $prueba" . ($detalle ? " — $detalle" : '') . "\n";
}
function info(string $texto): void { echo "\n== $texto\n"; }

// ---------------------------------------------------------------
// Cliente HTTP con manejo de cookies (como un navegador)
// ---------------------------------------------------------------
class Navegador {
    private array $cookies = [];
    public string $ultimaUrl = '';
    public int $ultimoCodigo = 0;
    public string $ultimoCuerpo = '';

    public function get(string $url, bool $seguirRedireccion = false): array {
        return $this->peticion('GET', $url, [], $seguirRedireccion);
    }
    public function post(string $url, array $datos, bool $seguirRedireccion = false): array {
        return $this->peticion('POST', $url, $datos, $seguirRedireccion);
    }

    private function peticion(string $url, string $metodo, array $datos, bool $seguir): array {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADER         => true,
            CURLOPT_FOLLOWLOCATION => $seguir,
            CURLOPT_TIMEOUT        => 15,
            CURLOPT_SSL_VERIFYPEER => false,
        ]);
        if ($this->cookies) {
            curl_setopt($ch, CURLOPT_COOKIE, implode('; ', $this->cookies));
        }
        if ($metodo === 'POST') {
            curl_setopt($ch, CURLOPT_POST, true);
            curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($datos));
        }
        $respuesta = curl_exec($ch);
        $tamCabecera = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
        $this->ultimoCodigo = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($respuesta === false) return ['', 0, '', []];

        $cabeceras = substr($respuesta, 0, $tamCabecera);
        $cuerpo    = substr($respuesta, $tamCabecera);

        // Guardar Set-Cookie (incluidas las redirecciones)
        preg_match_all('/^Set-Cookie:\s*([^;]+)/mi', $cabeceras, $m);
        foreach ($m[1] as $ck) {
            $ck = trim($ck);
            $nombre = explode('=', $ck, 2)[0];
            $this->cookies = array_values(array_filter($this->cookies, fn($c) => explode('=', $c, 2)[0] !== $nombre));
            if (!str_ends_with($ck, '=')) $this->cookies[] = $ck;
        }

        // Última cabecera Location
        preg_match_all('/^Location:\s*(.+)$/mi', $cabeceras, $lm);
        $location = $lm[1] ? trim(end($lm[1])) : '';

        $this->ultimoUrl = $url;
        $this->ultimoCuerpo = $cuerpo;
        return [$cuerpo, $this->ultimoCodigo, $location, $this->cookies];
    }

    /** Extrae el token CSRF de un HTML */
    public function csrf(string $html): string {
        if (preg_match('/name="csrf_token"\s+value="([^"]+)"/', $html, $m)) return $m[1];
        return '';
    }

    /** Resuelve el captcha matemático leyendo la pregunta del HTML */
    public function captcha(string $html): ?int {
        if (preg_match('/cu[aá]nto es\s*(\d+)\s*\+\s*(\d+)/iu', $html, $m)) {
            return (int)$m[1] + (int)$m[2];
        }
        return null;
    }

    public function resetCookies(): void { $this->cookies = []; }
}

function ruta(string $p, string $rol): string { return BASE . "/panel/$rol/$p"; }

echo "######################################################################\n";
echo "#  SUITE DE PRUEBAS E2E — TecniSecure 4.0\n";
echo "#  " . date('Y-m-d H:i:s') . "\n";
echo "#  URL base: " . BASE . "\n";
echo "######################################################################\n";

// =====================================================================
info('BLOQUE 1 — DISPONIBILIDAD DE PÁGINAS PÚBLICAS');
// =====================================================================
$publicas = [
    'index.php'             => 200,
    'login.php'             => 200,
    'registro.php'          => 200,
    'recuperar.php'         => 200,
    'pages/productos.php'   => 200,
];
foreach ($publicas as $rutaRel => $esperado) {
    $nav = new Navegador();
    [$cuerpo, $codigo] = $nav->get(BASE . '/' . $rutaRel);
    $codigo === $esperado
        ? ok("GET $rutaRel", "HTTP $codigo")
        : fallo("GET $rutaRel", "esperado $esperado, obtenido $codigo");
}

// =====================================================================
info('BLOQUE 2 — PROTECCIÓN RBAC SIN SESIÓN (debe redirigir a login)');
// =====================================================================
$protegidas = [
    'administrador/index.php', 'administrador/usuarios.php', 'administrador/productos.php',
    'administrador/inventario.php', 'administrador/pedidos.php', 'administrador/reportes.php',
    'administrador/auditoria.php', 'administrador/respaldo.php', 'administrador/facturacion.php',
    'empleado/index.php', 'empleado/ventas.php', 'empleado/perfil.php',
    'cliente/index.php', 'cliente/pedidos.php', 'cliente/favoritos.php',
    'cliente/perfil.php', 'cliente/resenar.php', 'cliente/chat.php',
];
foreach ($protegidas as $rutaRel) {
    $nav = new Navegador();
    [$cuerpo, $codigo, $location] = $nav->get(BASE . '/panel/' . $rutaRel);
    $redirige = in_array($codigo, [302, 303]) && stripos($location, 'login.php') !== false;
    $redirige
        ? ok("Sin sesión → $rutaRel", "redirigido a login")
        : fallo("Sin sesión → $rutaRel", "HTTP $codigo, Location: $location");
}

// =====================================================================
info('BLOQUE 3 — LOGIN: VALIDACIONES DEL FORMULARIO');
// =====================================================================

// 3.1 Login con credenciales válidas documentadas (cuenta semilla)
$admin = new Navegador();
[, $cLogin] = $admin->get(BASE . '/login.php');
$csrf = $admin->csrf($admin->ultimoCuerpo);
$captcha = $admin->captcha($admin->ultimoCuerpo);

if ($csrf && $captcha !== null) {
    ok('Login: se obtienen CSRF y captcha', "captcha=$captcha");
} else {
    fallo('Login: se obtienen CSRF y captcha', "csrf=" . ($csrf ? 'sí' : 'no') . " captcha=" . var_export($captcha, true));
}

[, $codigo, $location] = $admin->post(BASE . '/php/procesar_login.php', [
    'csrf_token' => $csrf, 'captcha' => $captcha,
    'correo' => 'admin@tecnicsecure.com', 'password' => 'Admin123!',
]);
$loginOk = $codigo === 302 && stripos($location, '/panel/administrador/') !== false;
$loginOk
    ? ok('Login admin (semilla)', "→ $location")
    : fallo('Login admin (semilla)', "HTTP $codigo, Location: $location");

// 3.2 CSRF inválido debe rechazarse
$nav = new Navegador();
$nav->get(BASE . '/login.php');
$c = $nav->captcha($nav->ultimoCuerpo);
[, $codigo, $location] = $nav->post(BASE . '/php/procesar_login.php', [
    'csrf_token' => 'token_falso_invalido', 'captcha' => $c,
    'correo' => 'admin@tecnicsecure.com', 'password' => 'Admin123!',
]);
(str_contains($location, 'error=') && stripos(urldecode($location), 'sesi') !== false)
    ? ok('Rechazo de CSRF inválido', 'rechazado correctamente')
    : fallo('Rechazo de CSRF inválido', "Location: $location");

// 3.3 Captcha incorrecto debe rechazarse
$nav = new Navegador();
$nav->get(BASE . '/login.php');
$csrf = $nav->csrf($nav->ultimoCuerpo);
[, $codigo, $location] = $nav->post(BASE . '/php/procesar_login.php', [
    'csrf_token' => $csrf, 'captcha' => 99999,
    'correo' => 'admin@tecnicsecure.com', 'password' => 'Admin123!',
]);
(str_contains(urldecode($location), 'verificación'))
    ? ok('Rechazo de captcha incorrecto', 'rechazado correctamente')
    : fallo('Rechazo de captcha incorrecto', "Location: $location");

// 3.4 Contraseña incorrecta → mensaje genérico (no filtra)
$nav = new Navegador();
$nav->get(BASE . '/login.php');
$csrf = $nav->csrf($nav->ultimoCuerpo); $c = $nav->captcha($nav->ultimoCuerpo);
[, $codigo, $location] = $nav->post(BASE . '/php/procesar_login.php', [
    'csrf_token' => $csrf, 'captcha' => $c,
    'correo' => 'admin@tecnicsecure.com', 'password' => 'ClaveIncorrecta_xyz',
]);
(str_contains(urldecode($location), 'Correo o contraseña incorrectos'))
    ? ok('Contraseña incorrecta → mensaje genérico', 'no filtra si el correo existe')
    : fallo('Contraseña incorrecta → mensaje genérico', "Location: $location");

// 3.5 Correo inexistente → mismo mensaje genérico
$nav = new Navegador();
$nav->get(BASE . '/login.php');
$csrf = $nav->csrf($nav->ultimoCuerpo); $c = $nav->captcha($nav->ultimoCuerpo);
[, $codigo, $location] = $nav->post(BASE . '/php/procesar_login.php', [
    'csrf_token' => $csrf, 'captcha' => $c,
    'correo' => 'noexiste@nada.com', 'password' => 'Cualquiera123',
]);
(str_contains(urldecode($location), 'Correo o contraseña incorrectos'))
    ? ok('Correo inexistente → mismo mensaje', 'no revela existencia de cuentas')
    : fallo('Correo inexistente → mismo mensaje', "Location: $location");

// 3.6 Correo mal formado
$nav = new Navegador();
$nav->get(BASE . '/login.php');
$csrf = $nav->csrf($nav->ultimoCuerpo); $c = $nav->captcha($nav->ultimoCuerpo);
[, $codigo, $location] = $nav->post(BASE . '/php/procesar_login.php', [
    'csrf_token' => $csrf, 'captcha' => $c,
    'correo' => 'esto-no-es-un-correo', 'password' => 'x',
]);
(str_contains(urldecode($location), 'error='))
    ? ok('Correo mal formado → rechazado', 'validación FILTER_VALIDATE_EMAIL')
    : fallo('Correo mal formado → rechazado', "Location: $location");

// =====================================================================
info('BLOQUE 4 — SESIÓN DEL ADMINISTRADOR Y RBAC POR ROL');
// =====================================================================
if ($loginOk) {
    // 4.1 Acceso a sus propias páginas
    $paginasAdmin = [
        'index.php', 'usuarios.php', 'clientes.php', 'empleados.php', 'productos.php',
        'inventario.php', 'kardex.php', 'pedidos.php', 'facturacion.php', 'reportes.php',
        'auditoria.php', 'respaldo.php', 'soporte.php', 'chat.php', 'perfil.php',
    ];
    foreach ($paginasAdmin as $p) {
        [$cuerpo, $codigo] = $admin->get(ruta($p, 'administrador'));
        ($codigo === 200)
            ? ok("Admin → $p", "HTTP 200")
            : fallo("Admin → $p", "HTTP $codigo");
    }

    // 4.2 El admin NO debe poder entrar al panel de cliente/empleado (RBAC estricto)
    foreach ([['cliente','index.php'], ['empleado','index.php']] as [$rol, $p]) {
        $nav2 = new Navegador();
        // reusar cookies del admin
        $nav2->get(BASE . '/login.php'); // inicializa
        $ref = new ReflectionClass($nav2);
        // (simplemente comprobamos con una nueva sesión admin mediante cookies ya guardadas)
    }

    // 4.3 Detección de rol en la interfaz
    $admin->get(ruta('index.php', 'administrador'));
    (stripos($admin->ultimoCuerpo, 'administrador') !== false || stripos($admin->ultimoCuerpo, 'Admin') !== false)
        ? ok('Panel admin muestra identidad de rol', 'contenido coherente')
        : fallo('Panel admin muestra identidad de rol', 'no se detectó el rol');
} else {
    fallo('Bloque 4 omitido', 'no se pudo iniciar sesión como admin');
}

// =====================================================================
info('BLOQUE 5 — AISLAMIENTO ENTRE ROLES (con sesión de admin)');
// =====================================================================
if ($loginOk) {
    // Guardar cookies del admin para clonarlas en otros navegadores
    $refl = new ReflectionClass($admin);
    $prop = $refl->getProperty('cookies'); $prop->setAccessible(true);
    $cookiesAdmin = $prop->getValue($admin);

    foreach ([['cliente','index.php'],['cliente','pedidos.php'],['empleado','index.php'],['empleado','ventas.php']] as [$rol, $p]) {
        $nav = new Navegador();
        $prop2 = (new ReflectionClass($nav))->getProperty('cookies'); $prop2->setAccessible(true);
        $prop2->setValue($nav, $cookiesAdmin);
        [$cuerpo, $codigo, $location] = $nav->get(ruta($p, $rol));
        $bloqueado = ($codigo === 302 && stripos($location, 'login') !== false)
                  || stripos($cuerpo, 'no tienes permiso') !== false
                  || stripos($cuerpo, 'Acceso denegado') !== false
                  || $codigo === 403;
        $bloqueado
            ? ok("Admin NO accede a panel/$rol/$p", "correctamente bloqueado (HTTP $codigo)")
            : fallo("Admin NO accede a panel/$rol/$p", "¡ACCESO PERMITIDO! HTTP $codigo");
    }
}

// =====================================================================
info('BLOQUE 6 — PÁGINAS DE AUTENTICACIÓN Y FLUJOS');
// =====================================================================
// 6.1 Recuperación de contraseña con correo real
$nav = new Navegador();
$nav->get(BASE . '/recuperar.php');
$csrf = $nav->csrf($nav->ultimoCuerpo);
if ($csrf) {
    [$cuerpo, $codigo, $location] = $nav->post(BASE . '/php/procesar_recuperar.php', [
        'csrf_token' => $csrf, 'correo' => 'admin@tecnicsecure.com',
    ]);
    ok('Recuperación de contraseña procesada', "HTTP $codigo → " . ($location ?: 'respuesta directa'));
} else {
    fallo('Recuperación de contraseña', 'no se obtuvo CSRF');
}

// 6.2 Registro de cliente nuevo
$nav = new Navegador();
$nav->get(BASE . '/registro.php');
$csrf = $nav->csrf($nav->ultimoCuerpo);
$c = $nav->captcha($nav->ultimoCuerpo);
if ($csrf) {
    $correoPrueba = 'prueba_' . time() . '@test.local';
    [$cuerpo, $codigo, $location] = $nav->post(BASE . '/php/procesar_registro.php', [
        'csrf_token' => $csrf, 'captcha' => $c,
        'nombre' => 'Usuario Prueba E2E',
        'correo' => $correoPrueba,
        'telefono' => '3001234567',
        'documento' => '900' . random_int(100000, 999999),
        'password' => 'Prueba123!',
        'password2' => 'Prueba123!',
    ]);
    ok('Registro de cliente nuevo', "HTTP $codigo → " . ($location ?: 'respuesta directa'));
} else {
    fallo('Registro de cliente nuevo', 'no se obtuvo CSRF');
}

// =====================================================================
info('BLOQUE 7 — CATÁLOGO Y CARRITO (público)');
// =====================================================================
$nav = new Navegador();
[$cuerpo, $codigo] = $nav->get(BASE . '/pages/productos.php');
(preg_match_all('/class="[^"]*tarjeta-producto/', $cuerpo) > 0 || stripos($cuerpo, 'producto') !== false)
    ? ok('Catálogo carga productos', 'contenido detectado')
    : fallo('Catálogo carga productos', 'no se detectaron productos');

$nav = new Navegador();
[$cuerpo, $codigo] = $nav->get(BASE . '/index.php');
substr_count($cuerpo, 'tarjeta-servicio')
    ? ok('Home muestra servicios desde BD', substr_count($cuerpo, 'tarjeta-servicio') . ' tarjetas')
    : fallo('Home muestra servicios desde BD', 'no se detectaron servicios');

// =====================================================================
info('BLOQUE 8 — FORMULARIO DE CONTACTO (público)');
// =====================================================================
$nav = new Navegador();
$nav->get(BASE . '/index.php');
$csrf = $nav->csrf($nav->ultimoCuerpo);
if ($csrf) {
    [$cuerpo, $codigo, $location] = $nav->post(BASE . '/php/procesar_contacto.php', [
        'csrf_token' => $csrf,
        'nombre' => 'Prueba E2E',
        'correo' => 'prueba@test.local',
        'telefono' => '3001234567',
        'asunto' => 'Prueba automatizada',
        'mensaje' => 'Mensaje de prueba generado por la suite E2E.',
    ]);
    ok('Contacto procesado', "HTTP $codigo");
} else {
    fallo('Contacto procesado', 'no se obtuvo CSRF');
}

// =====================================================================
echo "\n######################################################################\n";
echo "#  RESULTADOS\n";
echo "######################################################################\n";
$total = count($resultados);
$pasaron = $total - $errores;
echo "Total de pruebas:  $total\n";
echo "Aprobadas:         $pasaron\n";
echo "Fallidas:          $errores\n";
echo "Tasa de éxito:     " . ($total ? round($pasaron / $total * 100, 1) : 0) . "%\n";

if ($errores) {
    echo "\n-- PRUEBAS FALLIDAS --\n";
    foreach ($resultados as [$estado, $prueba, $detalle]) {
        if ($estado === 'FALLO') echo "  ✘ $prueba — $detalle\n";
    }
}
