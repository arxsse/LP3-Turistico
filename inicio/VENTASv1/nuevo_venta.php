<?php
$token = null;
if (isset($_POST['token'])) { $token = 'Bearer ' . $_POST['token']; } elseif (isset($_GET['token'])) { $token = 'Bearer ' . $_GET['token']; } elseif (isset($_COOKIE['userToken'])) { $token = 'Bearer ' . $_COOKIE['userToken']; }
if (!$token) { $token = null; }

$baseUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/ventas';
$error = null; $success = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $datos = [
        'empresa' => ['idEmpresa' => intval($_POST['idEmpresa'] ?? 1)],
        'cliente' => isset($_POST['clienteId']) && $_POST['clienteId'] !== '' ? ['idCliente' => intval($_POST['clienteId'])] : null,
        'codigoVenta' => $_POST['codigoVenta'] ?? '',
        'fechaVenta' => $_POST['fechaVenta'] ?? date('Y-m-d'),
        'precioTotal' => $_POST['precioTotal'] ?? 0,
        'observaciones' => $_POST['observaciones'] ?? ''
    ];

    $curl = curl_init();
    curl_setopt_array($curl, [
        CURLOPT_URL => $baseUrl,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CUSTOMREQUEST => 'POST',
        CURLOPT_POSTFIELDS => json_encode($datos),
        CURLOPT_HTTPHEADER => [ 'Content-Type: application/json', 'Authorization: ' . ($token ?? '') ]
    ]);
    $response = curl_exec($curl);
    $httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    $curlError = curl_error($curl);
    curl_close($curl);

    if ($curlError) { $error = 'Error de conexión: ' . $curlError; }
    elseif ($httpCode === 200 || $httpCode === 201) {
        $success = 'Venta creada correctamente';
        header('Location: ventas.php?ajax=1'); exit;
    } else {
        $error = 'Error al crear venta. HTTP ' . $httpCode;
        if ($response) { $resp = json_decode($response, true); if (isset($resp['message'])) $error .= ' - ' . $resp['message']; }
    }
}

// Mostrar formulario simple vía AJAX
if (isset($_GET['ajax']) && $_GET['ajax'] == '1') {
    header('Content-Type: text/html; charset=utf-8');
    ?>
    <div class="content-header">
        <div class="card">
            <div class="card-header">
                <h2 class="section-title">Crear Venta</h2>
            </div>
            <div class="card-body">
                <?php if ($error): ?>
                    <div class="alert alert-danger"><?php echo htmlspecialchars($error); ?></div>
                <?php endif; ?>

                <form id="formNuevoVenta" method="POST">
                    <input type="hidden" name="ajax" value="1">
                    <div class="form-grid">
                        <div class="form-group">
                            <label for="codigoVenta">Código</label>
                            <input type="text" id="codigoVenta" name="codigoVenta" class="form-input" required>
                        </div>
                        <div class="form-group">
                            <label for="clienteId">ID Cliente</label>
                            <input type="number" id="clienteId" name="clienteId" class="form-input">
                        </div>
                        <div class="form-group">
                            <label for="fechaVenta">Fecha venta</label>
                            <input type="date" id="fechaVenta" name="fechaVenta" class="form-input" value="<?php echo date('Y-m-d'); ?>">
                        </div>
                        <div class="form-group">
                            <label for="precioTotal">Total (S/)</label>
                            <input type="number" step="0.01" id="precioTotal" name="precioTotal" class="form-input" required>
                        </div>
                        <div class="form-group full-width">
                            <label for="observaciones">Observaciones</label>
                            <textarea id="observaciones" name="observaciones" class="form-input" rows="3"></textarea>
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Guardar Venta</button>
                        <button type="button" class="btn btn-secondary" onclick="if(typeof loadVentasContent==='function'){loadVentasContent();}else{window.location.href='ventas.php?ajax=1';}">Cancelar</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <script>
        document.getElementById('formNuevoVenta').addEventListener('submit', async function(e){
            e.preventDefault();
            const form = e.target;
            const data = new FormData(form);
            data.append('idEmpresa', '1');
            const url = 'VENTAS/nuevo_venta.php?ajax=1';
            const response = await fetch(url, { method: 'POST', body: data });
            if (response.ok) {
                // recargar listado
                if (typeof loadVentasContent === 'function') loadVentasContent(); else window.location.href='ventas.php?ajax=1';
            } else {
                const text = await response.text(); alert('Error: ' + response.status + '\n' + text);
            }
        });
    </script>
    <?php
    exit;
}

?>
