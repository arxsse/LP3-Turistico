<?php
// Obtener token del usuario (desde parámetro GET, POST o cookie)
$token = null;
if (isset($_POST['token'])) {
    $token = 'Bearer ' . $_POST['token'];
} elseif (isset($_GET['token'])) {
    $token = 'Bearer ' . $_GET['token'];
} elseif (isset($_COOKIE['userToken'])) {
    $token = 'Bearer ' . $_COOKIE['userToken'];
}

// Si no hay token, usar uno por defecto (para compatibilidad con acceso directo)
if (!$token) {
    $token = 'Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjbEBnbWFpbC5jb20iLCJ1c2VySWQiOjE1LCJlbXByZXNhSWQiOjEsInJvbGVzIjpbIlJPTEVfU1VQRVJBRE1JTklTVFJBRE9SIl0sImlzcyI6InNpc3RlbWEtdHVyaXN0aWNvLWJhY2tlbmQiLCJpYXQiOjE3NjQzMzk3MTYsImV4cCI6MTc2NDQyNjExNn0.H-geg1tf1JJI5i7aagghYZJ9NWtL7DQ2Cutz1uB3kqc';
}

$baseUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/clientes';

// Variables
$error = null;
$clienteId = isset($_GET['id']) ? intval($_GET['id']) : (isset($_POST['id']) ? intval($_POST['id']) : 0);
$motivo = isset($_GET['motivo']) ? $_GET['motivo'] : (isset($_POST['motivo']) ? $_POST['motivo'] : 'Eliminación solicitada por administrador');

// Verificar que se proporcionó un ID
if ($clienteId <= 0) {
    // Si se está cargando vía AJAX, devolver JSON
    if (isset($_GET['ajax']) && $_GET['ajax'] == '1' || isset($_POST['ajax']) && $_POST['ajax'] == '1') {
        header('Content-Type: application/json');
        echo json_encode([
            'success' => false,
            'message' => 'ID de cliente no válido'
        ]);
        exit();
    }
    header('Location: clientes.php?error=1&mensaje=' . urlencode('ID de cliente no válido'));
    exit();
}

// Realizar petición DELETE a la API
$url = $baseUrl . '/' . $clienteId . '?motivo=' . urlencode($motivo);

$curl = curl_init();
curl_setopt_array($curl, array(
    CURLOPT_URL => $url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_ENCODING => '',
    CURLOPT_MAXREDIRS => 10,
    CURLOPT_TIMEOUT => 0,
    CURLOPT_FOLLOWLOCATION => true,
    CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
    CURLOPT_CUSTOMREQUEST => 'DELETE',
    CURLOPT_HTTPHEADER => array(
        'Authorization: ' . $token,
        'Content-Type: application/json'
    ),
));

$response = curl_exec($curl);
$httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
$curlError = curl_error($curl);
curl_close($curl);

if ($curlError) {
    // Si se está cargando vía AJAX, devolver JSON
    if (isset($_GET['ajax']) && $_GET['ajax'] == '1' || isset($_POST['ajax']) && $_POST['ajax'] == '1') {
        header('Content-Type: application/json');
        echo json_encode([
            'success' => false,
            'message' => 'Error de conexión: ' . $curlError
        ]);
        exit();
    }
    header('Location: clientes.php?error=1&mensaje=' . urlencode('Error de conexión: ' . $curlError));
    exit();
} elseif ($httpCode === 200 || $httpCode === 204) {
    // Si se está cargando vía AJAX, devolver JSON
    if (isset($_GET['ajax']) && $_GET['ajax'] == '1' || isset($_POST['ajax']) && $_POST['ajax'] == '1') {
        header('Content-Type: application/json');
        echo json_encode([
            'success' => true,
            'message' => 'Cliente eliminado correctamente'
        ]);
        exit();
    }
    // Redirigir a clientes.php con mensaje de éxito
    header('Location: clientes.php?success=1&mensaje=' . urlencode('Cliente eliminado correctamente'));
    exit();
} else {
    $mensajeError = "Error al eliminar cliente: HTTP " . $httpCode;
    if ($response) {
        $errorData = json_decode($response, true);
        if (isset($errorData['message'])) {
            $mensajeError .= " - " . $errorData['message'];
        } elseif (isset($errorData['error'])) {
            $mensajeError .= " - " . $errorData['error'];
        }
    }
    // Si se está cargando vía AJAX, devolver JSON
    if (isset($_GET['ajax']) && $_GET['ajax'] == '1' || isset($_POST['ajax']) && $_POST['ajax'] == '1') {
        header('Content-Type: application/json');
        echo json_encode([
            'success' => false,
            'message' => $mensajeError
        ]);
        exit();
    }
    header('Location: clientes.php?error=1&mensaje=' . urlencode($mensajeError));
    exit();
}
?>
