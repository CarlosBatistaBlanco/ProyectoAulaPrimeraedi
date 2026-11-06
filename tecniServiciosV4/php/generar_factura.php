<?php
/**
 * generar_factura.php
 * -----------------------------------------------------------------
 * Genera el PDF de la factura de un pedido usando la librería FPDF
 * (incluida en php/lib/fpdf/, sin dependencias externas).
 * Acceso: administrador/empleado (desde el panel) o el propio cliente
 * dueño del pedido (desde "Mis pedidos").
 * -----------------------------------------------------------------
 */
require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/lib/fpdf/fpdf.php';

$u = requerirLogin();
$idPedido = (int)($_GET['id'] ?? 0);

$stmt = $pdo->prepare("SELECT p.*, c.nombre AS cliente, c.correo AS correo_cliente, c.telefono, c.direccion, c.id_usuario AS id_usuario_cliente
                        FROM pedidos p LEFT JOIN clientes c ON c.id_cliente = p.id_cliente WHERE p.id_pedido = :id");
$stmt->execute([':id' => $idPedido]);
$pedido = $stmt->fetch();

if (!$pedido) { die('Pedido no encontrado.'); }

// Control de acceso: solo admin/empleado, o el cliente dueño del pedido
$esStaff = in_array($u['rol'], ['administrador', 'empleado'], true);
$esDueno = $u['rol'] === 'cliente' && $pedido['id_usuario_cliente'] == $u['id_usuario'];
if (!$esStaff && !$esDueno) {
    http_response_code(403);
    die('No tienes permiso para ver esta factura.');
}

$factura = $pdo->prepare('SELECT * FROM facturas WHERE id_pedido = :id');
$factura->execute([':id' => $idPedido]);
$factura = $factura->fetch();
if (!$factura) { die('Este pedido todavía no tiene factura generada.'); }

$detalle = $pdo->prepare('SELECT pd.*, pr.nombre FROM pedido_detalle pd JOIN productos pr ON pr.id_producto = pd.id_producto WHERE pd.id_pedido = :id');
$detalle->execute([':id' => $idPedido]);
$detalle = $detalle->fetchAll();

// --- Construcción del PDF ---
$pdf = new FPDF();
$pdf->AddPage();
$pdf->SetMargins(15, 15, 15);

// Encabezado
$pdf->SetFont('Arial', 'B', 18);
$pdf->SetTextColor(11, 61, 92);
$pdf->Cell(0, 10, 'TECNISECURE', 0, 1);
$pdf->SetFont('Arial', '', 10);
$pdf->SetTextColor(90, 90, 90);
$pdf->Cell(0, 6, 'Seguridad Electronica y Camaras', 0, 1);
$pdf->Cell(0, 6, 'Cra. 10 #20-30, Cartagena, Colombia - Tel: +57 300 123 4567', 0, 1);
$pdf->Ln(4);

$pdf->SetDrawColor(200, 200, 200);
$pdf->Line(15, $pdf->GetY(), 195, $pdf->GetY());
$pdf->Ln(6);

// Datos de la factura
$pdf->SetFont('Arial', 'B', 13);
$pdf->SetTextColor(0, 0, 0);
$pdf->Cell(100, 8, 'FACTURA ' . $factura['numero_factura'], 0, 0);
$pdf->SetFont('Arial', '', 10);
$pdf->Cell(0, 8, 'Fecha: ' . date('d/m/Y', strtotime($factura['creado_en'])), 0, 1, 'R');
if ($factura['estado'] === 'anulada') {
    $pdf->SetFont('Arial', 'B', 12);
    $pdf->SetTextColor(217, 83, 30);
    $pdf->Cell(0, 8, 'FACTURA ANULADA', 0, 1);
    $pdf->SetTextColor(0, 0, 0);
}
$pdf->Ln(2);

$pdf->SetFont('Arial', 'B', 10);
$pdf->Cell(0, 6, 'Cliente:', 0, 1);
$pdf->SetFont('Arial', '', 10);
$pdf->Cell(0, 6, texto_pdf($pedido['cliente'] ?? 'Consumidor final'), 0, 1);
$pdf->Cell(0, 6, texto_pdf($pedido['correo_cliente'] ?? ''), 0, 1);
$pdf->Cell(0, 6, texto_pdf($pedido['telefono'] ?? ''), 0, 1);
$pdf->Cell(0, 6, texto_pdf('Direccion: ' . ($pedido['direccion'] ?? 'No especificada')), 0, 1);
$pdf->Ln(6);

// Tabla de productos
$pdf->SetFillColor(11, 61, 92);
$pdf->SetTextColor(255, 255, 255);
$pdf->SetFont('Arial', 'B', 10);
$pdf->Cell(90, 9, 'Producto', 1, 0, 'L', true);
$pdf->Cell(25, 9, 'Cantidad', 1, 0, 'C', true);
$pdf->Cell(35, 9, 'Precio unit.', 1, 0, 'R', true);
$pdf->Cell(30, 9, 'Subtotal', 1, 1, 'R', true);

$pdf->SetTextColor(0, 0, 0);
$pdf->SetFont('Arial', '', 10);
foreach ($detalle as $d) {
    $subtotal = $d['precio_unitario'] * $d['cantidad'];
    $pdf->Cell(90, 8, texto_pdf($d['nombre']), 1);
    $pdf->Cell(25, 8, $d['cantidad'], 1, 0, 'C');
    $pdf->Cell(35, 8, '$' . number_format($d['precio_unitario'], 0, ',', '.'), 1, 0, 'R');
    $pdf->Cell(30, 8, '$' . number_format($subtotal, 0, ',', '.'), 1, 1, 'R');
}

$pdf->Ln(4);
$pdf->SetFont('Arial', 'B', 10);
$pdf->Cell(150, 8, 'Subtotal', 0, 0, 'R');
$pdf->Cell(30, 8, '$' . number_format($factura['subtotal'], 0, ',', '.'), 0, 1, 'R');
$pdf->Cell(150, 8, 'Descuento', 0, 0, 'R');
$pdf->Cell(30, 8, '$' . number_format($factura['descuento'], 0, ',', '.'), 0, 1, 'R');
$pdf->Cell(150, 8, 'Impuestos', 0, 0, 'R');
$pdf->Cell(30, 8, '$' . number_format($factura['impuestos'], 0, ',', '.'), 0, 1, 'R');
$pdf->SetFont('Arial', 'B', 12);
$pdf->SetTextColor(11, 61, 92);
$pdf->Cell(150, 10, 'TOTAL', 0, 0, 'R');
$pdf->Cell(30, 10, '$' . number_format($factura['total'], 0, ',', '.'), 0, 1, 'R');

$pdf->Ln(10);
$pdf->SetFont('Arial', 'I', 8);
$pdf->SetTextColor(140, 140, 140);
$pdf->Cell(0, 6, 'Gracias por confiar en TECNISECURE.', 0, 1, 'C');

$pdf->Output('I', $factura['numero_factura'] . '.pdf');
