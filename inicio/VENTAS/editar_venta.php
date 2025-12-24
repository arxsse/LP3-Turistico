<?php
$token = null;
if (isset($_POST['token'])) { $token = 'Bearer ' . $_POST['token']; } elseif (isset($_GET['token'])) { $token = 'Bearer ' . $_GET['token']; } elseif (isset($_COOKIE['userToken'])) { $token = 'Bearer ' . $_COOKIE['userToken']; }
if (!$token) { $token = null; }

$baseUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/ventas';
$error = null;

$ventaId = isset($_GET['id']) ? intval($_GET['id']) : (isset($_POST['idVenta']) ? intval($_POST['idVenta']) : null);
if (!$ventaId) { echo 'ID de venta requerido'; exit; }

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $datos = [
        'codigoVenta' => $_POST['codigoVenta'] ?? '',
        'fechaVenta' => $_POST['fechaVenta'] ?? '',
        'precioTotal' => $_POST['precioTotal'] ?? 0,
        'observaciones' => $_POST['observaciones'] ?? ''
    ];

    $curl = curl_init();
    curl_setopt_array($curl, [
        CURLOPT_URL => $baseUrl . '/' . urlencode($ventaId),
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CUSTOMREQUEST => 'PUT',
        CURLOPT_POSTFIELDS => json_encode($datos),
        CURLOPT_HTTPHEADER => [ 'Content-Type: application/json', 'Authorization: ' . ($token ?? '') ]
    ]);
    $response = curl_exec($curl);
    $httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    $curlError = curl_error($curl);
    curl_close($curl);

    if ($curlError) { $error = 'Error de conexión: ' . $curlError; }
    elseif ($httpCode === 200) { header('Location: ventas.php?ajax=1'); exit; }
    else { $error = 'Error al actualizar venta. HTTP ' . $httpCode; }
}

// Si se solicita AJAX, devolver formulario con datos
if (isset($_GET['ajax']) && $_GET['ajax'] == '1') {
    $curl = curl_init();
    curl_setopt_array($curl, [
        CURLOPT_URL => 'http://turistas.spring.informaticapp.com:2410/api/v1/ventas/' . urlencode($ventaId),
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => [ 'Authorization: ' . ($token ?? '') ]
    ]);
    $response = curl_exec($curl);
    $httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    curl_close($curl);
    $venta = null;
    if ($httpCode === 200 && $response) {
        $payload = json_decode($response, true);
        $venta = $payload['data'] ?? $payload;
    }

    header('Content-Type: text/html; charset=utf-8');
    ?>
    <div class="content-header">
        <div class="card">
            <div class="card-header"><h2 class="section-title">Editar Venta</h2></div>
            <div class="card-body">
                <?php if ($error): ?><div class="alert alert-danger"><?php echo htmlspecialchars($error); ?></div><?php endif; ?>

                <form id="formEditarVenta" method="POST">
                    <input type="hidden" name="idVenta" value="<?php echo htmlspecialchars($ventaId); ?>">
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="codigoVenta">Código</label>
                            <input type="text" id="codigoVenta" name="codigoVenta" class="form-input" value="<?php echo htmlspecialchars($venta['codigoVenta'] ?? ''); ?>">
                        </div>
                        <div class="form-group">
                            <label for="fechaVenta">Fecha venta</label>
                            <input type="date" id="fechaVenta" name="fechaVenta" class="form-input" value="<?php echo htmlspecialchars($venta['fechaVenta'] ?? date('Y-m-d')); ?>">
                        </div>
                        <div class="form-group">
                            <label for="precioTotal">Total (S/)</label>
                            <input type="number" step="0.01" id="precioTotal" name="precioTotal" class="form-input" value="<?php echo htmlspecialchars($venta['precioTotal'] ?? $venta['total'] ?? '0'); ?>">
                        </div>
                        <div class="form-group full-width">
                            <label for="observaciones">Observaciones</label>
                            <textarea id="observaciones" name="observaciones" class="form-input" rows="3"><?php echo htmlspecialchars($venta['observaciones'] ?? ''); ?></textarea>
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Actualizar Venta</button>
                        <button type="button" class="btn btn-secondary" onclick="if(typeof loadVentasContent==='function'){loadVentasContent();}else{window.location.href='ventas.php?ajax=1';}">Cancelar</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <script>
        document.getElementById('formEditarVenta').addEventListener('submit', async function(e){
            e.preventDefault();
            const form = e.target; const data = new FormData(form);
            const url = 'VENTAS/editar_venta.php?ajax=1&id=' + encodeURIComponent('<?php echo $ventaId; ?>');
            const resp = await fetch(url, { method: 'POST', body: data });
            if (resp.ok) { if(typeof loadVentasContent==='function') loadVentasContent(); else window.location.href='ventas.php?ajax=1'; }
            else { alert('Error: ' + resp.status); }
        });
    </script>
    <?php
    exit;
}

?>
