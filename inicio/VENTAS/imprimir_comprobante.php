<?php
$reservaId = $_GET['reservaId'] ?? null;
$token = $_GET['token'] ?? ($_COOKIE['userToken'] ?? null);

if (!$reservaId) {
    die('ID de reserva no proporcionado.');
}

// Fetch reservation data
$apiBase = 'http://turistas.spring.informaticapp.com:2410/api/v1/reservas/' . urlencode($reservaId);
$curl = curl_init();
curl_setopt_array($curl, [
    CURLOPT_URL => $apiBase,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        'Authorization: Bearer ' . $token
    ],
]);
$response = curl_exec($curl);
$httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
curl_close($curl);

$reserva = null;
if ($httpCode === 200) {
    $decoded = json_decode($response, true);
    // Handle wrapped response (e.g. { "data": { ... } })
    if (isset($decoded['data']) && is_array($decoded['data'])) {
        $reserva = $decoded['data'];
    } else {
        $reserva = $decoded;
    }
} else {
    die('Error al obtener datos de la reserva. HTTP ' . $httpCode);
}

if (!$reserva) {
    die('No se encontraron datos para la reserva.');
}

// Extract data
$codigo = $reserva['codigoReserva'] ?? ($reserva['codigo'] ?? $reserva['id'] ?? '-');
$fecha = date('d/m/Y H:i'); // Current date for receipt

// Cliente extraction logic
$clienteNombre = '-';
if (!empty($reserva['nombreCliente']) || !empty($reserva['apellidoCliente'])) {
    $clienteNombre = trim(($reserva['nombreCliente'] ?? '') . ' ' . ($reserva['apellidoCliente'] ?? ''));
} elseif (!empty($reserva['cliente'])) {
    if (is_array($reserva['cliente'])) {
        $n = $reserva['cliente']['nombre'] ?? ($reserva['cliente']['nombres'] ?? '');
        $a = $reserva['cliente']['apellido'] ?? ($reserva['cliente']['apellidos'] ?? '');
        $clienteNombre = trim("$n $a");
        if (!$clienteNombre && !empty($reserva['cliente']['nombreCompleto'])) {
            $clienteNombre = $reserva['cliente']['nombreCompleto'];
        }
    } elseif (is_string($reserva['cliente'])) {
        $clienteNombre = $reserva['cliente'];
    }
}

$items = $reserva['items'] ?? [];

// Total extraction logic
$total = $reserva['precioTotal'] ?? ($reserva['total'] ?? ($reserva['montoTotal'] ?? ($reserva['precio'] ?? 0)));

// If total is 0, try to sum items
if ($total == 0 && !empty($items)) {
    foreach ($items as $item) {
        $p = $item['precio'] ?? ($item['monto'] ?? ($item['costo'] ?? 0));
        $q = $item['cantidad'] ?? 1;
        $total += $p * $q;
    }
}

$metodoPago = $reserva['metodoPago'] ?? 'Efectivo'; // Default or from API

// Comprobante details - Prefer GET params if available (from localStorage fallback)
$tipoComprobante = $_GET['tipo'] ?? strtoupper($reserva['tipoComprobante'] ?? ($reserva['datosComprobante']['tipo'] ?? 'BOLETA'));
$tituloComprobante = ($tipoComprobante === 'FACTURA') ? 'FACTURA ELECTRÓNICA' : 'BOLETA DE VENTA ELECTRÓNICA';

$docNumero = $_GET['docNum'] ?? ($reserva['numeroDocumento'] ?? ($reserva['datosComprobante']['numero'] ?? '-'));
$docNombre = $_GET['docName'] ?? ($reserva['nombreFacturacion'] ?? ($reserva['datosComprobante']['nombre'] ?? $clienteNombre));
$docDireccion = $_GET['docAddr'] ?? ($reserva['direccionFacturacion'] ?? ($reserva['datosComprobante']['direccion'] ?? '-'));

// Label for document number
$docLabel = ($tipoComprobante === 'FACTURA') ? 'RUC' : 'DNI';

?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Comprobante de Pago - <?= htmlspecialchars($codigo) ?></title>
    <style>
        body { font-family: 'Courier New', Courier, monospace; font-size: 14px; max-width: 300px; margin: 0 auto; padding: 20px; background: #fff; color: #000; }
        .header { text-align: center; margin-bottom: 20px; border-bottom: 1px dashed #000; padding-bottom: 10px; }
        .info { margin-bottom: 10px; font-size: 12px; }
        .items-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; font-size: 12px; }
        .items-table th, .items-table td { text-align: left; border-bottom: 1px dashed #ccc; padding: 5px 0; }
        .items-table th { border-bottom: 1px solid #000; }
        .items-table td.price { text-align: right; }
        .total { text-align: right; font-weight: bold; font-size: 16px; margin-top: 10px; border-top: 1px solid #000; padding-top: 5px; }
        .footer { text-align: center; margin-top: 20px; font-size: 12px; border-top: 1px dashed #000; padding-top: 10px; }
        @media print {
            .no-print { display: none; }
            body { margin: 0; padding: 0; }
        }
    </style>
</head>
<body>
    <div class="header">
        <h2 style="margin: 0;">TURISMO</h2>
        <p style="margin: 5px 0;">RUC: 20123456789</p>
        <p style="margin: 5px 0;">Av. Principal 123, Ciudad</p>
        <h3 style="margin: 10px 0;"><?= htmlspecialchars($tituloComprobante) ?></h3>
    </div>

    <div class="info">
        <p style="margin: 2px 0;"><strong>Reserva:</strong> <?= htmlspecialchars($codigo) ?></p>
        <p style="margin: 2px 0;"><strong>Fecha:</strong> <?= $fecha ?></p>
        <p style="margin: 2px 0;"><strong>Cliente:</strong> <?= htmlspecialchars($docNombre) ?></p>
        <?php if ($docNumero !== '-'): ?>
        <p style="margin: 2px 0;"><strong><?= $docLabel ?>:</strong> <?= htmlspecialchars($docNumero) ?></p>
        <?php endif; ?>
        <?php if ($docDireccion !== '-'): ?>
        <p style="margin: 2px 0;"><strong>Dirección:</strong> <?= htmlspecialchars($docDireccion) ?></p>
        <?php endif; ?>
    </div>

    <table class="items-table">
        <thead>
            <tr>
                <th style="width: 10%;">Cant.</th>
                <th style="width: 60%;">Descripción</th>
                <th style="width: 30%; text-align: right;">Total</th>
            </tr>
        </thead>
        <tbody>
            <?php foreach ($items as $item): 
                $nombre = $item['nombreServicio'] ?? ($item['nombrePaquete'] ?? 'Item');
                $cant = $item['cantidad'] ?? 1;
                $precio = $item['precioTotal'] ?? 0;
            ?>
            <tr>
                <td><?= $cant ?></td>
                <td><?= htmlspecialchars($nombre) ?></td>
                <td class="price"><?= number_format($precio, 2) ?></td>
            </tr>
            <?php endforeach; ?>
        </tbody>
    </table>

    <div class="total">
        TOTAL: S/ <?= number_format($total, 2) ?>
    </div>

    <div class="footer">
        <p>¡Gracias por su preferencia!</p>
        <p>Conserve este comprobante.</p>
    </div>

    <div class="no-print" style="text-align: center; margin-top: 20px;">
        <button onclick="window.print()" style="padding: 10px 20px; cursor: pointer;">Imprimir</button>
        <button onclick="window.close()" style="padding: 10px 20px; cursor: pointer;">Cerrar</button>
    </div>

    <script>
        window.onload = function() {
            setTimeout(function() {
                window.print();
            }, 500);
        }
    </script>
</body>
</html>