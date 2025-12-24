<?php
// Obtener token y userId del usuario desde parámetros GET, POST o cookie
$token = null;
$userId = null;

// Intentar obtener token desde parámetro GET
if (isset($_GET['token'])) {
    $token = $_GET['token'];
}
// Intentar obtener desde parámetro POST
elseif (isset($_POST['token'])) {
    $token = $_POST['token'];
}
// Intentar obtener desde cookie
elseif (isset($_COOKIE['userToken'])) {
    $token = $_COOKIE['userToken'];
}

// Obtener userId desde parámetros
if (isset($_GET['userId'])) {
    $userId = intval($_GET['userId']);
} elseif (isset($_POST['userId'])) {
    $userId = intval($_POST['userId']);
} elseif (isset($_GET['id'])) {
    $userId = intval($_GET['id']);
} elseif (isset($_POST['id'])) {
    $userId = intval($_POST['id']);
}

// Si no hay token, retornar error
if (!$token) {
    if (isset($_GET['ajax']) && $_GET['ajax'] == '1' || isset($_POST['ajax']) && $_POST['ajax'] == '1') {
        header('Content-Type: application/json');
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Token no proporcionado'
        ]);
    } else {
        http_response_code(400);
        echo 'Token no proporcionado';
    }
    exit();
}

// Si no hay userId, retornar error
if (!$userId || $userId <= 0) {
    if (isset($_GET['ajax']) && $_GET['ajax'] == '1' || isset($_POST['ajax']) && $_POST['ajax'] == '1') {
        header('Content-Type: application/json');
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'UserId no proporcionado'
        ]);
    } else {
        http_response_code(400);
        echo 'UserId no proporcionado';
    }
    exit();
}

// Preparar body JSON (igual que en Postman)
$body = json_encode([
    'id' => $userId,
    'token' => ''
]);

// Realizar petición POST al endpoint de logout
$curl = curl_init();

curl_setopt_array($curl, array(
    CURLOPT_URL => 'http://turistas.spring.informaticapp.com:2410/api/v1/auth/logout',
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_ENCODING => '',
    CURLOPT_MAXREDIRS => 10,
    CURLOPT_TIMEOUT => 30,
    CURLOPT_FOLLOWLOCATION => true,
    CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
    CURLOPT_CUSTOMREQUEST => 'POST',
    CURLOPT_POSTFIELDS => $body,
    CURLOPT_HTTPHEADER => array(
        'Content-Type: application/json',
        'Authorization: Bearer ' . $token
    ),
));

$response = curl_exec($curl);
$httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
$curlError = curl_error($curl);

curl_close($curl);

// Si se solicita vía AJAX, devolver JSON
if (isset($_GET['ajax']) && $_GET['ajax'] == '1' || isset($_POST['ajax']) && $_POST['ajax'] == '1') {
    header('Content-Type: application/json');
    
    if ($curlError) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error de conexión: ' . $curlError
        ]);
    } elseif ($httpCode === 200 || $httpCode === 204) {
        echo json_encode([
            'success' => true,
            'message' => 'Token invalidado correctamente',
            'httpCode' => $httpCode
        ]);
    } else {
        http_response_code($httpCode);
        $errorData = json_decode($response, true);
        echo json_encode([
            'success' => false,
            'message' => 'Error al invalidar token: HTTP ' . $httpCode,
            'details' => $errorData ?? $response,
            'httpCode' => $httpCode
        ]);
    }
    exit();
}

// Si no es AJAX, devolver respuesta simple
if ($curlError) {
    http_response_code(500);
    echo 'Error de conexión: ' . $curlError;
} elseif ($httpCode === 200 || $httpCode === 204) {
    echo 'Token invalidado correctamente';
} else {
    http_response_code($httpCode);
    echo 'Error al invalidar token: HTTP ' . $httpCode;
    if ($response) {
        echo ' - ' . $response;
    }
}
?>