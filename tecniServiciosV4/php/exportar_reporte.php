<?php
/**
 * exportar_reporte.php
 * -----------------------------------------------------------------
 * Exporta el resumen de ventas/productos/clientes a PDF (con FPDF)
 * o a Excel. La exportación "Excel" usa el truco estándar de generar
 * una tabla HTML con Content-Type de Excel: Excel la abre perfecto
 * y no requiere ninguna librería pesada (PhpSpreadsheet) que XAMPP
 * no trae instalada por defecto.
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
$u = requerirRol(['administrador']);

$formato = $_GET['formato'] ?? 'pdf';

$ventasPorMes = $pdo->query("SELECT DATE_FORMAT(creado_en, '%Y-%m') AS mes, COUNT(*) AS pedidos, SUM(total) AS total
    FROM pedidos WHERE estado != 'cancelado' GROUP BY mes ORDER BY mes DESC LIMIT 12")->fetchAll();

$masVendidos = $pdo->query("SELECT p.nombre, SUM(pd.cantidad) AS unidades, SUM(pd.cantidad*pd.precio_unitario) AS total
    FROM pedido_detalle pd JOIN productos p ON p.id_producto = pd.id_producto
    GROUP BY pd.id_producto ORDER BY unidades DESC LIMIT 10")->fetchAll();

$clientesFrecuentes = $pdo->query("SELECT c.nombre, COUNT(*) AS pedidos, SUM(p.total) AS total
    FROM pedidos p JOIN clientes c ON c.id_cliente = p.id_cliente
    WHERE p.estado != 'cancelado' GROUP BY p.id_cliente ORDER BY pedidos DESC LIMIT 10")->fetchAll();

registrarLog($u['id_usuario'], 'exportar_reporte', $formato);

if ($formato === 'excel') {
    header('Content-Type: application/vnd.ms-excel; charset=utf-8');
    header('Content-Disposition: attachment; filename="reporte_TecniSecure_' . date('Y-m-d') . '.xls"');
    echo "\xEF\xBB\xBF"; // BOM para que Excel reconozca UTF-8 y los acentos se vean bien
    ?>
    <table border="1">
      <tr><th colspan="3">Ventas por mes</th></tr>
      <tr><th>Mes</th><th>Pedidos</th><th>Total</th></tr>
      <?php foreach ($ventasPorMes as $v): ?>
        <tr><td><?= $v['mes']; ?></td><td><?= $v['pedidos']; ?></td><td><?= $v['total']; ?></td></tr>
      <?php endforeach; ?>
      <tr><td colspan="3"></td></tr>
      <tr><th colspan="3">Productos más vendidos</th></tr>
      <tr><th>Producto</th><th>Unidades</th><th>Total</th></tr>
      <?php foreach ($masVendidos as $m): ?>
        <tr><td><?= htmlspecialchars($m['nombre']); ?></td><td><?= $m['unidades']; ?></td><td><?= $m['total']; ?></td></tr>
      <?php endforeach; ?>
      <tr><td colspan="3"></td></tr>
      <tr><th colspan="3">Clientes frecuentes</th></tr>
      <tr><th>Cliente</th><th>Pedidos</th><th>Total</th></tr>
      <?php foreach ($clientesFrecuentes as $c): ?>
        <tr><td><?= htmlspecialchars($c['nombre']); ?></td><td><?= $c['pedidos']; ?></td><td><?= $c['total']; ?></td></tr>
      <?php endforeach; ?>
    </table>
    <?php
    exit;
}

// --- PDF ---
require_once __DIR__ . '/lib/fpdf/fpdf.php';
$pdf = new FPDF();
$pdf->AddPage();
$pdf->SetFont('Arial', 'B', 16);
$pdf->SetTextColor(11, 61, 92);
$pdf->Cell(0, 10, 'Reporte General de Ventas - TECNISECURE', 0, 1);
$pdf->SetFont('Arial', '', 9);
$pdf->SetTextColor(100, 100, 100);
$pdf->Cell(0, 6, 'Generado el ' . date('d/m/Y H:i'), 0, 1);
$pdf->Ln(4);

function tablaSimple($pdf, $titulo, $encabezados, $anchos, $filas) {
    $pdf->SetFont('Arial', 'B', 12);
    $pdf->SetTextColor(0,0,0);
    $pdf->Cell(0, 8, texto_pdf($titulo), 0, 1);
    $pdf->SetFont('Arial', 'B', 9);
    $pdf->SetFillColor(11, 61, 92);
    $pdf->SetTextColor(255,255,255);
    foreach ($encabezados as $i => $h) $pdf->Cell($anchos[$i], 8, texto_pdf($h), 1, 0, 'C', true);
    $pdf->Ln();
    $pdf->SetFont('Arial', '', 9);
    $pdf->SetTextColor(0,0,0);
    foreach ($filas as $fila) {
        foreach ($fila as $i => $val) $pdf->Cell($anchos[$i], 7, texto_pdf((string)$val), 1);
        $pdf->Ln();
    }
    $pdf->Ln(6);
}

tablaSimple($pdf, 'Ventas por mes', ['Mes','Pedidos','Total'], [60,40,60],
    array_map(fn($v) => [$v['mes'], $v['pedidos'], '$'.number_format($v['total'],0,',','.')], $ventasPorMes));

tablaSimple($pdf, 'Productos más vendidos', ['Producto','Unidades','Total'], [90,30,40],
    array_map(fn($m) => [$m['nombre'], $m['unidades'], '$'.number_format($m['total'],0,',','.')], $masVendidos));

tablaSimple($pdf, 'Clientes frecuentes', ['Cliente','Pedidos','Total'], [90,30,40],
    array_map(fn($c) => [$c['nombre'], $c['pedidos'], '$'.number_format($c['total'],0,',','.')], $clientesFrecuentes));

$pdf->Output('I', 'reporte_TecniSecure_' . date('Y-m-d') . '.pdf');
