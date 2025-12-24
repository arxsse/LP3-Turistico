<?php
$isAjax = isset($_GET['ajax']) && $_GET['ajax'] === '1';

if (!$isAjax) {
    $redirectParams = $_GET;
    unset($redirectParams['ajax']);
    $queryString = http_build_query($redirectParams);
    $targetUrl = '../admin.php' . ($queryString ? '?' . $queryString : '') . '#ventas';
    header('Location: ' . $targetUrl);
    exit;
}

$reservas = [];
$error = null;
$data = [];

$token = $_GET['token'] ?? ($_COOKIE['userToken'] ?? null);

if (!$token) {
    $error = 'No se encontró el token de sesión. Inicia sesión nuevamente.';
} else {
    // 1. Verificar si existe una caja abierta
    $cajaUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/cajas';
    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $cajaUrl,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 10,
        CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token],
    ]);
    $cajaResponse = curl_exec($ch);
    $cajaHttpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    $cajaAbierta = false;
    $idCajaAbierta = null;
    if ($cajaHttpCode === 200) {
        $cajasData = json_decode($cajaResponse, true);
        $listaCajas = [];
        
        if (isset($cajasData['data']) && is_array($cajasData['data'])) {
            $listaCajas = $cajasData['data'];
        } elseif (isset($cajasData['content']) && is_array($cajasData['content'])) {
            $listaCajas = $cajasData['content'];
        } elseif (is_array($cajasData)) {
            $listaCajas = $cajasData;
        }

        foreach ($listaCajas as $caja) {
            // Verificar campo 'estado'
            $estado = $caja['estado'] ?? '';
            if (strtoupper($estado) === 'ABIERTA') {
                $cajaAbierta = true;
                $idCajaAbierta = $caja['id'] ?? ($caja['idCaja'] ?? null);
                break;
            }
        }
    }

    if (!$cajaAbierta) {
        $error = '⚠️ ACCESO DENEGADO: No existe una caja abierta. Por favor, abra una caja antes de gestionar ventas.';
    } else {
        // 2. Si hay caja abierta, consultamos las reservas
        $baseUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/reservas';
        $query = [];

    // Verificar si es gerente (Lógica copiada de reservas.php)
    $esGerente = false;
    if (isset($_GET['rol'])) {
        $rol = strtolower(trim($_GET['rol']));
        $esGerente = ($rol === 'gerente');
    } elseif (isset($_GET['rolId'])) {
        $rolId = intval($_GET['rolId']);
        $esGerente = ($rolId === 3 || $rolId === 4);
    }

    // Filtrar por sucursal SOLO si es gerente
    if ($esGerente && !empty($_GET['idSucursal'])) {
        $query['idSucursal'] = intval($_GET['idSucursal']);
    }

    if (!empty($_GET['estado'])) {
        $query['estado'] = $_GET['estado'];
    }
    if (!empty($_GET['busqueda'])) {
        $query['busqueda'] = $_GET['busqueda'];
    }
    if (!empty($_GET['fecha'])) {
        $query['fecha'] = $_GET['fecha'];
    }
    if (!empty($_GET['empresaId'])) {
        $query['empresaId'] = $_GET['empresaId'];
    }
    if (isset($_GET['page']) && is_numeric($_GET['page'])) {
        $query['page'] = (int) $_GET['page'];
    }
    if (isset($_GET['size']) && is_numeric($_GET['size'])) {
        $query['size'] = (int) $_GET['size'];
    }

    $requestUrl = $baseUrl . (empty($query) ? '' : '?' . http_build_query($query));

    $curl = curl_init();
    curl_setopt_array($curl, [
        CURLOPT_URL => $requestUrl,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_ENCODING => '',
        CURLOPT_MAXREDIRS => 10,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
        CURLOPT_CUSTOMREQUEST => 'GET',
        CURLOPT_HTTPHEADER => [
            'Authorization: Bearer ' . $token
        ],
    ]);

    $response = curl_exec($curl);
    $httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    $curlError = curl_error($curl);
    curl_close($curl);

    if ($curlError) {
        $error = 'Error de conexión: ' . $curlError;
    } elseif ($httpCode !== 200) {
        $errorData = json_decode($response, true);
        $errorMessage = 'Error HTTP: ' . $httpCode;
        if (isset($errorData['message'])) {
            $errorMessage .= ' - ' . $errorData['message'];
        } elseif (isset($errorData['error'])) {
            $errorMessage .= ' - ' . $errorData['error'];
        } elseif (!empty($response)) {
            $errorMessage .= ' - ' . substr($response, 0, 200);
        }
        $error = $errorMessage;
        error_log('Reservas API error: ' . $errorMessage);
    } else {
        $data = json_decode($response, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            $error = 'Error al decodificar JSON: ' . json_last_error_msg();
        } else {
            $all = [];
            if (isset($data['data']) && is_array($data['data'])) {
                $all = $data['data'];
            } elseif (isset($data['content']) && is_array($data['content'])) {
                $all = $data['content'];
            } elseif (isset($data['reservas']) && is_array($data['reservas'])) {
                $all = $data['reservas'];
            } elseif (isset($data['items']) && is_array($data['items'])) {
                $all = $data['items'];
            } elseif (isset($data['results']) && is_array($data['results'])) {
                $all = $data['results'];
            } elseif (is_array($data) && !empty($data)) {
                $all = isset($data[0]) ? $data : [$data];
            }

            // No filtrar, mostrar todas las reservas
            $reservas = $all;

            // Filtrado PHP por fecha (fallback si la API no lo soporta)
            if (!empty($_GET['fecha'])) {
                $filterDate = $_GET['fecha'];
                $reservas = array_filter($reservas, function($r) use ($filterDate) {
                    // Campos posibles de fecha
                    $candidates = [
                        $r['fechaReserva'] ?? null,
                        $r['fechaCreacion'] ?? null,
                        $r['fecha'] ?? null
                    ];
                    
                    foreach ($candidates as $val) {
                        if ($val) {
                            // Comparar solo la parte YYYY-MM-DD
                            if (strpos($val, $filterDate) === 0) {
                                return true;
                            }
                        }
                    }
                    return false;
                });
            }
        }
    }
    } // Fin else cajaAbierta
}

ob_start();
include __DIR__ . '/ventas_table.php';
$content = ob_get_clean();

header('Content-Type: text/html; charset=utf-8');
echo $content;
exit;
?>
