<?php
header('Content-Type: application/json; charset=utf-8');

// Obtener parámetros
$tipo = isset($_GET['tipo']) ? $_GET['tipo'] : 'DNI';
$documento = isset($_GET['documento']) ? trim($_GET['documento']) : '';

// Validar que se haya proporcionado un documento
if (empty($documento)) {
    echo json_encode([
        'success' => false,
        'message' => 'Por favor ingrese un documento para buscar'
    ]);
    exit;
}

// Validar formato del documento (solo números)
if (!preg_match('/^[0-9]+$/', $documento)) {
    echo json_encode([
        'success' => false,
        'message' => 'El documento solo debe contener números'
    ]);
    exit;
}

// Validar longitud según el tipo
$longitudEsperada = ($tipo === 'RUC') ? 11 : 8;
if (strlen($documento) !== $longitudEsperada) {
    echo json_encode([
        'success' => false,
        'message' => "El {$tipo} debe tener exactamente {$longitudEsperada} dígitos"
    ]);
    exit;
}

// Construir URL según el tipo de documento
$apiKey = 'sk_1391.SkVX8sZbjKHKoEiI5OKUwIXQHMK38PUQ';
$url = '';

if ($tipo === 'DNI') {
    $url = "https://api.decolecta.com/v1/reniec/dni/{$documento}";
} elseif ($tipo === 'RUC') {
    // Para RUC, usar endpoint de SUNAT (ajusta la URL si es diferente)
    $url = "https://api.decolecta.com/v1/sunat/ruc/{$documento}";
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Tipo de documento no válido'
    ]);
    exit;
}

// Realizar petición a la API
$curl = curl_init();

curl_setopt_array($curl, array(
    CURLOPT_URL => $url,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_ENCODING => '',
    CURLOPT_MAXREDIRS => 10,
    CURLOPT_TIMEOUT => 30,
    CURLOPT_FOLLOWLOCATION => true,
    CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
    CURLOPT_CUSTOMREQUEST => 'GET',
    CURLOPT_HTTPHEADER => array(
        'Authorization: Bearer ' . $apiKey
    ),
));

$response = curl_exec($curl);
$httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
$curlError = curl_error($curl);
curl_close($curl);

// Manejar errores de conexión
if ($curlError) {
    echo json_encode([
        'success' => false,
        'message' => 'Error de conexión: ' . $curlError
    ]);
    exit;
}

// Procesar respuesta
$responseData = json_decode($response, true);
$jsonError = json_last_error();

// Para debugging: incluir respuesta cruda si hay un parámetro debug
$debug = isset($_GET['debug']) && $_GET['debug'] === '1';

// Verificar si hubo error al decodificar JSON
if ($jsonError !== JSON_ERROR_NONE) {
    $resultado = [
        'success' => false,
        'message' => 'Error al procesar la respuesta de la API',
        'httpCode' => $httpCode,
        'jsonError' => json_last_error_msg()
    ];
    
    if ($debug) {
        $resultado['debug'] = [
            'rawResponse' => $response,
            'url' => $url
        ];
    }
    
    echo json_encode($resultado, JSON_UNESCAPED_UNICODE);
    exit;
}

if ($httpCode === 200 && $responseData) {
    // La API puede devolver los datos directamente o dentro de un objeto 'data'
    // Intentar obtener los datos desde diferentes estructuras posibles
    $datosApi = null;
    $estructuraEncontrada = 'raiz';
    
    if (isset($responseData['data']) && is_array($responseData['data'])) {
        // Los datos están dentro de un objeto 'data'
        $datosApi = $responseData['data'];
        $estructuraEncontrada = 'data';
    } elseif (isset($responseData['result']) && is_array($responseData['result'])) {
        // Los datos están dentro de un objeto 'result'
        $datosApi = $responseData['result'];
        $estructuraEncontrada = 'result';
    } elseif (isset($responseData['resultado']) && is_array($responseData['resultado'])) {
        // Los datos están dentro de un objeto 'resultado'
        $datosApi = $responseData['resultado'];
        $estructuraEncontrada = 'resultado';
    } else {
        // Los datos están directamente en el objeto raíz
        $datosApi = $responseData;
        $estructuraEncontrada = 'raiz';
    }
    
    // Si no se encontraron datos, devolver error
    if (!$datosApi || !is_array($datosApi) || empty($datosApi)) {
        $resultado = [
            'success' => false,
            'message' => 'La API no devolvió datos válidos',
            'httpCode' => $httpCode
        ];
        
        if ($debug) {
            $resultado['debug'] = [
                'rawResponse' => $responseData,
                'estructuraEncontrada' => $estructuraEncontrada,
                'url' => $url
            ];
        }
        
        echo json_encode($resultado, JSON_UNESCAPED_UNICODE);
        exit;
    }
    
    // Formatear respuesta según el tipo de documento
    $datos = [];
    
    if ($tipo === 'DNI') {
        // Formatear datos de DNI/RENIEC - intentar múltiples estructuras posibles
        // La API de decolecta puede devolver los datos en diferentes formatos
        $nombre = '';
        $apellido = '';
        $apellidoPaterno = '';
        $apellidoMaterno = '';
        
        // Intentar obtener nombre desde diferentes campos posibles
        if (isset($datosApi['nombres'])) {
            $nombre = $datosApi['nombres'];
        } elseif (isset($datosApi['nombre'])) {
            $nombre = $datosApi['nombre'];
        } elseif (isset($datosApi['primerNombre'])) {
            $nombre = $datosApi['primerNombre'];
            if (isset($datosApi['segundoNombre'])) {
                $nombre .= ' ' . $datosApi['segundoNombre'];
            }
        }
        
        // Intentar obtener apellidos desde diferentes campos posibles (más variantes)
        if (isset($datosApi['apellidoPaterno']) || isset($datosApi['apellidoMaterno'])) {
            $apellidoPaterno = $datosApi['apellidoPaterno'] ?? '';
            $apellidoMaterno = $datosApi['apellidoMaterno'] ?? '';
            $apellido = trim($apellidoPaterno . ' ' . $apellidoMaterno);
        } elseif (isset($datosApi['apellidoPaterno'])) {
            $apellidoPaterno = $datosApi['apellidoPaterno'];
            $apellido = $apellidoPaterno;
        } elseif (isset($datosApi['apellidoMaterno'])) {
            $apellidoMaterno = $datosApi['apellidoMaterno'];
            $apellido = $apellidoMaterno;
        } elseif (isset($datosApi['apellido'])) {
            $apellido = $datosApi['apellido'];
        } elseif (isset($datosApi['apellidos'])) {
            $apellido = $datosApi['apellidos'];
        } elseif (isset($datosApi['apellidoPrimero'])) {
            $apellido = $datosApi['apellidoPrimero'];
            if (isset($datosApi['apellidoSegundo'])) {
                $apellido .= ' ' . $datosApi['apellidoSegundo'];
            }
        } elseif (isset($datosApi['primerApellido'])) {
            $apellidoPaterno = $datosApi['primerApellido'];
            $apellido = $apellidoPaterno;
            if (isset($datosApi['segundoApellido'])) {
                $apellidoMaterno = $datosApi['segundoApellido'];
                $apellido .= ' ' . $apellidoMaterno;
            }
        }
        
        // Si aún no hay apellido, buscar cualquier campo que contenga "apellido" en su nombre
        if (empty($apellido)) {
            foreach ($datosApi as $key => $value) {
                // Buscar campos que contengan "apellido" en su nombre (case-insensitive)
                if (stripos($key, 'apellido') !== false && !empty($value)) {
                    if (is_string($value) || is_numeric($value)) {
                        if (empty($apellido)) {
                            $apellido = $value;
                        } else {
                            $apellido .= ' ' . $value;
                        }
                    }
                }
            }
            $apellido = trim($apellido);
        }
        
        // Si aún no hay apellido pero tenemos "nombreCompleto" o todo junto, intentar separar
        if (empty($apellido)) {
            // Buscar campo que pueda contener nombre completo
            $nombreCompleto = '';
            if (isset($datosApi['nombreCompleto'])) {
                $nombreCompleto = $datosApi['nombreCompleto'];
            } elseif (isset($datosApi['nombre'])) {
                $nombreCompleto = $datosApi['nombre'];
            }
            
            // Intentar separar nombres y apellidos (generalmente apellidos vienen después)
            if (!empty($nombreCompleto) && !empty($nombre)) {
                // Si el nombre completo contiene más que solo el nombre, los extras podrían ser apellidos
                $nombreCompletoTrim = trim($nombreCompleto);
                $nombreTrim = trim($nombre);
                if (strpos($nombreCompletoTrim, $nombreTrim) === 0 && strlen($nombreCompletoTrim) > strlen($nombreTrim)) {
                    // El nombre completo empieza con el nombre, lo que sigue son apellidos
                    $apellido = trim(substr($nombreCompletoTrim, strlen($nombreTrim)));
                }
            }
        }
        
        $datos = [
            'nombre' => trim($nombre),
            'apellido' => trim($apellido),
            'apellidoPaterno' => trim($apellidoPaterno),
            'apellidoMaterno' => trim($apellidoMaterno),
            'dni' => $datosApi['dni'] ?? $datosApi['numeroDocumento'] ?? $datosApi['documento'] ?? $documento
        ];
    } else {
        // Formatear datos de RUC/SUNAT
        $nombre = '';
        
        // Intentar obtener el nombre desde múltiples campos posibles
        $camposNombre = [
            'razonSocial',
            'razon_social',
            'nombre',
            'nombreCompleto',
            'nombre_completo',
            'denominacion',
            'denominacionSocial',
            'denominacion_social',
            'nombreLegal',
            'nombre_legal'
        ];
        
        foreach ($camposNombre as $campo) {
            if (isset($datosApi[$campo]) && !empty($datosApi[$campo])) {
                $nombre = $datosApi[$campo];
                break;
            }
        }
        
        // Si aún no hay nombre, buscar cualquier campo que contenga "razon", "nombre" o "denominacion"
        if (empty($nombre)) {
            foreach ($datosApi as $key => $value) {
                $keyLower = strtolower($key);
                if ((stripos($keyLower, 'razon') !== false || 
                     stripos($keyLower, 'nombre') !== false || 
                     stripos($keyLower, 'denominacion') !== false) && 
                    !empty($value) && is_string($value)) {
                    $nombre = $value;
                    break;
                }
            }
        }
        
        $datos = [
            'nombre' => trim($nombre),
            'nombreCompleto' => trim($nombre),
            'razonSocial' => trim($nombre), // Incluir también como razón social
            'ruc' => $datosApi['ruc'] ?? $datosApi['numeroDocumento'] ?? $datosApi['numero_documento'] ?? $documento,
            'estado' => $datosApi['estado'] ?? $datosApi['condicion'] ?? $datosApi['estadoContribuyente'] ?? '',
            'direccion' => $datosApi['direccion'] ?? $datosApi['domicilioFiscal'] ?? $datosApi['direccionFiscal'] ?? $datosApi['domicilio_fiscal'] ?? ''
        ];
    }
    
    $resultado = [
        'success' => true,
        'datos' => $datos,
        'message' => "Información del {$tipo} encontrada"
    ];
    
    // Agregar respuesta cruda si está en modo debug
    if ($debug) {
        // Obtener todas las claves disponibles en los datos
        $camposDisponibles = [];
        if (is_array($datosApi)) {
            $camposDisponibles = array_keys($datosApi);
        }
        
        $resultado['debug'] = [
            'httpCode' => $httpCode,
            'rawResponse' => $responseData,
            'estructuraEncontrada' => $estructuraEncontrada,
            'datosExtraidos' => $datosApi,
            'camposDisponibles' => $camposDisponibles, // Nuevo: lista de todos los campos disponibles
            'datosFormateados' => $datos,
            'url' => $url
        ];
    }
    
    echo json_encode($resultado, JSON_UNESCAPED_UNICODE);
} else {
    // Error en la respuesta de la API
    $errorMessage = 'No se encontró información para el ' . $tipo;
    
    // Intentar obtener mensaje de error desde diferentes campos posibles
    if ($responseData) {
        if (isset($responseData['message'])) {
            $errorMessage = $responseData['message'];
        } elseif (isset($responseData['error'])) {
            $errorMessage = is_string($responseData['error']) ? $responseData['error'] : (isset($responseData['error']['message']) ? $responseData['error']['message'] : 'Error desconocido');
        } elseif (isset($responseData['mensaje'])) {
            $errorMessage = $responseData['mensaje'];
        } elseif (isset($responseData['errorMessage'])) {
            $errorMessage = $responseData['errorMessage'];
        }
    }
    
    // Mensajes específicos según el código HTTP
    if ($httpCode === 404) {
        $errorMessage = "No se encontró información para el {$tipo} {$documento}";
    } elseif ($httpCode === 401) {
        $errorMessage = "Error de autenticación con la API";
    } elseif ($httpCode === 403) {
        $errorMessage = "Acceso denegado a la API";
    } elseif ($httpCode === 429) {
        $errorMessage = "Demasiadas solicitudes. Por favor intente más tarde";
    } elseif ($httpCode >= 500) {
        $errorMessage = "Error del servidor de la API";
    } elseif ($httpCode !== 200) {
        $errorMessage = "Error HTTP {$httpCode}: " . $errorMessage;
    }
    
    $resultado = [
        'success' => false,
        'message' => $errorMessage,
        'httpCode' => $httpCode
    ];
    
    // Agregar respuesta cruda si está en modo debug
    if ($debug) {
        $resultado['debug'] = [
            'rawResponse' => $responseData ? $responseData : $response,
            'rawResponseText' => $response,
            'url' => $url,
            'curlError' => $curlError
        ];
    }
    
    echo json_encode($resultado, JSON_UNESCAPED_UNICODE);
}
?>