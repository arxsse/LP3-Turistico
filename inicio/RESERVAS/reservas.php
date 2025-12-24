<?php
// Serve reservations to the admin dashboard via AJAX while keeping direct navigation within admin.php.
$isAjax = isset($_GET['ajax']) && $_GET['ajax'] === '1';

if (!$isAjax) {
    $redirectParams = $_GET;
    unset($redirectParams['ajax']);
    $queryString = http_build_query($redirectParams);
    $targetUrl = '../admin.php' . ($queryString ? '?' . $queryString : '') . '#reservas';
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
    $baseUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/reservas';
    $query = [];

    // Verificar si es gerente
    $esGerente = false;
    if (isset($_GET['rol'])) {
        $rol = strtolower(trim($_GET['rol']));
        $esGerente = ($rol === 'gerente');
    } elseif (isset($_GET['rolId'])) {
        $rolId = intval($_GET['rolId']);
        $esGerente = ($rolId === 3 || $rolId === 4);
    }
    
    // Siempre agregar empresaId si está disponible (aceptar idEmpresa o empresaId)
    $empresaId = null;
    if (!empty($_GET['empresaId'])) {
        $empresaId = intval($_GET['empresaId']);
    } elseif (!empty($_GET['idEmpresa'])) {
        $empresaId = intval($_GET['idEmpresa']);
    }
    if ($empresaId !== null) {
        $query['empresaId'] = $empresaId;
    }
    
    // Filtrar por sucursal SOLO si es gerente
    // Los gerentes solo ven las reservas de su sucursal
    if ($esGerente && !empty($_GET['idSucursal'])) {
        $query['idSucursal'] = intval($_GET['idSucursal']);
        // Log para depuración
        if (isset($_GET['debug']) && $_GET['debug'] === '1') {
            error_log("🔍 Filtrando por idSucursal en API: " . $query['idSucursal']);
        }
    } elseif ($esGerente && empty($_GET['idSucursal'])) {
        // Log de advertencia si es gerente pero no tiene idSucursal
        if (isset($_GET['debug']) && $_GET['debug'] === '1') {
            error_log("⚠️ Usuario es gerente pero no se proporcionó idSucursal en la URL");
        }
    }
    
    // Otros filtros opcionales
    if (!empty($_GET['estado'])) {
        $query['estado'] = $_GET['estado'];
    }
    if (!empty($_GET['busqueda'])) {
        $query['busqueda'] = $_GET['busqueda'];
    }
    if (isset($_GET['page']) && is_numeric($_GET['page'])) {
        $query['page'] = (int) $_GET['page'];
    }
    if (isset($_GET['size']) && is_numeric($_GET['size'])) {
        $query['size'] = (int) $_GET['size'];
    }

    // Construir URL con parámetros
    $requestUrl = $baseUrl;
    if (!empty($query)) {
        $requestUrl .= '?' . http_build_query($query);
    }
    
    // Log para depuración
    if (isset($_GET['debug']) && $_GET['debug'] === '1') {
        error_log("📡 URL de petición: " . $requestUrl);
        error_log("📋 Parámetros: " . json_encode($query));
    }

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
            if (isset($data['data']) && is_array($data['data'])) {
                $reservas = $data['data'];
            } elseif (isset($data['content']) && is_array($data['content'])) {
                $reservas = $data['content'];
            } elseif (isset($data['reservas']) && is_array($data['reservas'])) {
                $reservas = $data['reservas'];
            } elseif (isset($data['items']) && is_array($data['items'])) {
                $reservas = $data['items'];
            } elseif (isset($data['results']) && is_array($data['results'])) {
                $reservas = $data['results'];
            } elseif (is_array($data) && !empty($data)) {
                $reservas = isset($data[0]) ? $data : [$data];
            }
            
            // Para gerentes: aplicar filtro estricto por idSucursal
            // SOLO mostrar reservas que tengan el idSucursal correcto
            // Los gerentes solo ven las reservas de su sucursal
            if ($esGerente && !empty($_GET['idSucursal']) && !empty($reservas)) {
                $idSucursalFiltro = intval($_GET['idSucursal']);
                $reservasAntes = count($reservas);
                $reservasFiltradas = [];
                $reservasExcluidas = [];
                
                foreach ($reservas as $reserva) {
                    $sucursalId = null;
                    
                    // Buscar idSucursal en diferentes campos posibles
                    if (isset($reserva['idSucursal'])) {
                        $sucursalId = intval($reserva['idSucursal']);
                    } elseif (isset($reserva['sucursalId'])) {
                        $sucursalId = intval($reserva['sucursalId']);
                    } elseif (isset($reserva['sucursal']) && is_array($reserva['sucursal'])) {
                        if (isset($reserva['sucursal']['idSucursal'])) {
                            $sucursalId = intval($reserva['sucursal']['idSucursal']);
                        } elseif (isset($reserva['sucursal']['id'])) {
                            $sucursalId = intval($reserva['sucursal']['id']);
                        } elseif (isset($reserva['sucursal']['sucursalId'])) {
                            $sucursalId = intval($reserva['sucursal']['sucursalId']);
                        }
                    }
                    
                    // SOLO incluir si el idSucursal coincide exactamente
                    // Si no se puede determinar el idSucursal, EXCLUIR la reserva (por seguridad)
                    if ($sucursalId !== null && $sucursalId === $idSucursalFiltro) {
                        $reservasFiltradas[] = $reserva;
                    } else {
                        $reservasExcluidas[] = [
                            'id' => $reserva['idReserva'] ?? $reserva['id'] ?? 'N/A',
                            'sucursalId' => $sucursalId,
                            'codigo' => $reserva['codigoReserva'] ?? 'N/A'
                        ];
                        // Log para depuración
                        if (isset($_GET['debug']) && $_GET['debug'] === '1') {
                            error_log("❌ Reserva EXCLUIDA - ID: " . ($reserva['idReserva'] ?? $reserva['id'] ?? 'N/A') . 
                                     " - SucursalId encontrado: " . ($sucursalId !== null ? $sucursalId : 'NO ENCONTRADO') . 
                                     " - Filtro esperado: $idSucursalFiltro");
                        }
                    }
                }
                
                // Usar solo las reservas filtradas
                $reservas = $reservasFiltradas;
                
                // Log para depuración
                error_log("🔍 FILTRO DE SUCURSAL (Gerente) - idSucursal esperado: $idSucursalFiltro");
                error_log("  - Reservas recibidas de API: $reservasAntes");
                error_log("  - Reservas después del filtro: " . count($reservas));
                error_log("  - Reservas excluidas: " . count($reservasExcluidas));
                if (count($reservasExcluidas) > 0 && isset($_GET['debug']) && $_GET['debug'] === '1') {
                    error_log("  - Detalle de excluidas: " . json_encode($reservasExcluidas));
                }
            }
        }
    }
}

if (isset($_GET['debug']) && $_GET['debug'] === '1') {
    header('Content-Type: text/plain; charset=utf-8');
    echo "=== DEBUG DE RESERVAS ===\n\n";
    echo "Parámetros recibidos:\n";
    echo "  - idSucursal: " . ($_GET['idSucursal'] ?? 'NO RECIBIDO') . "\n";
    echo "  - empresaId: " . ($_GET['empresaId'] ?? 'NO RECIBIDO') . "\n";
    echo "  - rol: " . ($_GET['rol'] ?? 'NO RECIBIDO') . "\n";
    echo "  - esGerente: " . (isset($esGerente) && $esGerente ? 'SI' : 'NO') . "\n";
    if (isset($requestUrl)) {
        echo "  - URL de API: " . $requestUrl . "\n";
    }
    echo "\n";
    
    echo "Total de reservas recibidas de la API: " . count($reservas) . "\n\n";
    
    if (!empty($reservas)) {
        echo "Primera reserva (estructura completa):\n";
        print_r($reservas[0]);
        echo "\n\nCampos de sucursal en la primera reserva:\n";
        if (isset($reservas[0]['idSucursal'])) {
            echo "  - idSucursal: " . $reservas[0]['idSucursal'] . "\n";
        }
        if (isset($reservas[0]['sucursalId'])) {
            echo "  - sucursalId: " . $reservas[0]['sucursalId'] . "\n";
        }
        if (isset($reservas[0]['sucursal']) && is_array($reservas[0]['sucursal'])) {
            echo "  - sucursal (objeto):\n";
            print_r($reservas[0]['sucursal']);
        }
    } else {
        echo "No hay reservas en la respuesta.\n";
    }
    
    if (isset($data)) {
        echo "\n\nRespuesta completa de la API:\n";
        print_r($data);
    }
    exit;
}

// Funciones helper de reservas_table.php
if (!function_exists('buildReservaItemNombre')) {
    function buildReservaItemNombre(array $item): string
    {
        $candidates = [
            $item['nombreServicio'] ?? null,
            $item['nombrePaquete'] ?? null,
            $item['nombre'] ?? null,
        ];

        foreach ($candidates as $candidate) {
            if ($candidate !== null && $candidate !== '') {
                return (string) $candidate;
            }
        }

        if (!empty($item['descripcionExtra'])) {
            return (string) $item['descripcionExtra'];
        }

        $tipo = $item['tipoItem'] ?? null;
        $refId = $item['idServicio'] ?? ($item['idPaquete'] ?? ($item['idReservaItem'] ?? null));
        if ($tipo) {
            $label = ucfirst(strtolower((string) $tipo));
            return $refId ? $label . ' #' . $refId : $label;
        }

        return $refId ? 'Ítem #' . $refId : 'Ítem sin nombre';
    }
}

if (!function_exists('buildReservaItemsUi')) {
    function buildReservaItemsUi(array $items): array
    {
        $filtered = [];
        foreach ($items as $item) {
            if (is_array($item)) {
                $filtered[] = $item;
            }
        }

        $count = count($filtered);
        if ($count === 0) {
            return ['summary' => '', 'detail' => '', 'count' => 0];
        }

        $firstItem = $filtered[0];
        $firstTipo = strtoupper($firstItem['tipoItem'] ?? 'SERVICIO');
        $firstTipoLabel = $firstTipo === 'PAQUETE' ? 'Paquete' : 'Servicio';
        $firstNombre = buildReservaItemNombre($firstItem);
        $firstCantidad = (int) ($firstItem['cantidad'] ?? 1);
        if ($firstCantidad <= 0) {
            $firstCantidad = 1;
        }

        $summary = $firstTipoLabel . ': ' . $firstNombre;
        if ($firstCantidad > 1) {
            $summary .= ' x' . $firstCantidad;
        }
        $restantes = $count - 1;
        if ($restantes > 0) {
            $summary .= ' + ' . $restantes . ' ítem' . ($restantes === 1 ? '' : 's');
        }

        ob_start();
        echo '<ul class="items-detail-list">';
        foreach ($filtered as $item) {
            $tipoItem = strtoupper($item['tipoItem'] ?? 'SERVICIO');
            $isPaquete = $tipoItem === 'PAQUETE';
            $tipoLabel = $isPaquete ? 'Paquete' : 'Servicio';
            $chipClass = $isPaquete ? 'chip chip-paquete' : 'chip chip-servicio';
            $nombreItem = buildReservaItemNombre($item);
            $cantidadItem = (int) ($item['cantidad'] ?? 1);
            if ($cantidadItem <= 0) {
                $cantidadItem = 1;
            }
            $unitario = isset($item['precioUnitario']) ? (float) $item['precioUnitario'] : 0.0;
            $totalItem = isset($item['precioTotal']) ? (float) $item['precioTotal'] : ($cantidadItem * $unitario);
            $descripcionExtra = trim((string) ($item['descripcionExtra'] ?? ''));

            echo '<li class="items-detail-item">';
            echo '<div class="item-main">';
            echo '<span class="' . $chipClass . '"><i class="fas ' . ($isPaquete ? 'fa-box' : 'fa-suitcase-rolling') . '"></i>' . htmlspecialchars($tipoLabel, ENT_QUOTES, 'UTF-8') . '</span>';
            echo '<span class="item-name">' . htmlspecialchars($nombreItem, ENT_QUOTES, 'UTF-8') . '</span>';
            echo '</div>';
            echo '<div class="item-meta">';
            echo '<span class="item-meta-entry"><i class="fas fa-users"></i>x' . htmlspecialchars((string) $cantidadItem, ENT_QUOTES, 'UTF-8') . '</span>';
            echo '<span class="item-meta-entry"><i class="fas fa-tag"></i>S/ ' . number_format($unitario, 2) . '</span>';
            echo '<span class="item-meta-entry"><i class="fas fa-wallet"></i>S/ ' . number_format($totalItem, 2) . '</span>';
            echo '</div>';
            if ($descripcionExtra !== '') {
                echo '<div class="item-notes"><i class="fas fa-comment-dots"></i> ' . htmlspecialchars($descripcionExtra, ENT_QUOTES, 'UTF-8') . '</div>';
            }
            echo '</li>';
        }
        echo '</ul>';
        $detailHtml = ob_get_clean();

        return [
            'summary' => $summary,
            'detail' => $detailHtml,
            'count' => $count
        ];
    }
}

if (!function_exists('normalizeReservaAssignments')) {
    function normalizeReservaAssignments(array $assignments): array
    {
        $normalized = [];

        foreach ($assignments as $assignment) {
            if (!is_array($assignment)) {
                continue;
            }

            $idAsignacion = isset($assignment['idAsignacion']) ? (int) $assignment['idAsignacion'] : (isset($assignment['id']) ? (int) $assignment['id'] : null);
            $personalData = isset($assignment['personal']) && is_array($assignment['personal']) ? $assignment['personal'] : [];

            $idPersonal = null;
            if (isset($assignment['idPersonal'])) {
                $idPersonal = (int) $assignment['idPersonal'];
            } elseif (isset($personalData['idPersonal'])) {
                $idPersonal = (int) $personalData['idPersonal'];
            } elseif (isset($personalData['id'])) {
                $idPersonal = (int) $personalData['id'];
            }

            if (!$idPersonal) {
                continue;
            }

            $nombre = $assignment['nombre']
                ?? ($assignment['nombrePersonal'] ?? null)
                ?? ($personalData['nombreCompleto'] ?? null);
            if (!$nombre) {
                if (isset($assignment['nombrePersonal']) || isset($assignment['apellidoPersonal'])) {
                    $componentes = [
                        $assignment['nombrePersonal'] ?? '',
                        $assignment['apellidoPersonal'] ?? ''
                    ];
                    $componentes = array_filter(array_map(static function ($value) {
                        return trim((string) $value);
                    }, $componentes));
                    if (!empty($componentes)) {
                        $nombre = implode(' ', $componentes);
                    }
                }
                $componentes = [
                    $personalData['nombres'] ?? $personalData['nombre'] ?? '',
                    $personalData['apellidos'] ?? $personalData['apellido'] ?? ''
                ];

                $componentes = array_filter(array_map(static function ($value) {
                    return trim((string) $value);
                }, $componentes));

                if (!empty($componentes)) {
                    $nombre = implode(' ', $componentes);
                }
            }

            if (!$nombre) {
                $nombre = 'Personal #' . $idPersonal;
            }

            $cargo = $assignment['cargo'] ?? ($assignment['cargoPersonal'] ?? null);
            if (!$cargo && isset($personalData['cargo'])) {
                if (is_array($personalData['cargo'])) {
                    $cargo = $personalData['cargo']['nombre'] ?? $personalData['cargo']['descripcion'] ?? null;
                } else {
                    $cargo = (string) $personalData['cargo'];
                }
            }
            if (!$cargo && isset($personalData['rol'])) {
                $cargo = is_array($personalData['rol']) ? ($personalData['rol']['nombre'] ?? null) : (string) $personalData['rol'];
            }

            if (!$cargo) {
                $cargo = 'Sin cargo';
            }

            $normalized[] = array_filter([
                'idAsignacion' => $idAsignacion,
                'idPersonal' => $idPersonal,
                'nombre' => $nombre,
                'cargo' => $cargo,
                'estado' => $assignment['estado'] ?? null,
                'observaciones' => isset($assignment['observaciones']) ? (string) $assignment['observaciones'] : null,
                'fechaAsignacion' => $assignment['fechaAsignacion'] ?? null,
                'telefono' => $assignment['telefono']
                    ?? ($assignment['telefonoPersonal'] ?? null)
                    ?? ($personalData['telefono'] ?? ($personalData['celular'] ?? null)),
                'email' => $assignment['email']
                    ?? ($assignment['emailPersonal'] ?? null)
                    ?? ($personalData['email'] ?? null)
            ], static function ($value) {
                return $value !== null && $value !== '';
            });
        }

        return $normalized;
    }
}

$statusClasses = [
    'Pendiente' => 'status-reserved',
    'Confirmada' => 'status-available',
    'PagoParcial' => 'status-reserved',
    'Pagada' => 'status-active',
    'Completada' => 'status-active',
    'Cancelada' => 'status-inactive'
];

$apiReservasBase = 'http://turistas.spring.informaticapp.com:2410/api/v1/reservas';
$busquedaActual = isset($_GET['busqueda']) ? trim((string) $_GET['busqueda']) : '';
?>
<div class="content-header reservas-content">
    <div class="card">
        <div class="card-header">
            <h2 class="section-title">Gestión de Reservas</h2>
            <div class="header-actions">
                <div class="search-container reservas-search">
                    <div class="search-box">
                        <i class="fas fa-search"></i>
                        <input
                            type="search"
                            id="reservasSearchInput"
                            class="search-input"
                            placeholder="Buscar por cliente..."
                            value="<?php echo htmlspecialchars($busquedaActual, ENT_QUOTES, 'UTF-8'); ?>"
                            autocomplete="off"
                        >
                    </div>
                </div>
                <a href="#" class="btn btn-primary" title="Crear reserva" onclick="if (typeof loadNuevaReservaContent === 'function') { loadNuevaReservaContent(); } else { window.location.href='nueva_reserva.php'; } return false;">
                    <i class="fas fa-plus"></i>
                    Nueva Reserva
                </a>
            </div>
        </div>
        <div class="card-body">
            <?php if ($error && !empty($reservas)): ?>
                <div style="padding:15px;background:#f8d7da;color:#721c24;border-radius:8px;margin:20px;border:1px solid #f5c6cb;">
                    <i class="fas fa-exclamation-triangle"></i> <?php echo htmlspecialchars($error); ?>
                </div>
            <?php endif; ?>
            <?php 
            // Mensaje de depuración temporal para ayudar a diagnosticar el problema
            if (isset($_GET['debug']) && $_GET['debug'] === '1'): 
                $esGerente = false;
                if (isset($_GET['rol'])) {
                    $rol = strtolower(trim($_GET['rol']));
                    $esGerente = ($rol === 'gerente');
                } elseif (isset($_GET['rolId'])) {
                    $rolId = intval($_GET['rolId']);
                    $esGerente = ($rolId === 3 || $rolId === 4);
                }
            ?>
                <div style="padding:15px;background:#d1ecf1;color:#0c5460;border-radius:8px;margin:20px;border:1px solid #bee5eb;font-size:0.9rem;">
                    <strong>🔍 Información de Depuración:</strong><br>
                    - Es Gerente: <?php echo $esGerente ? 'Sí' : 'No'; ?><br>
                    - idSucursal en URL: <?php echo !empty($_GET['idSucursal']) ? htmlspecialchars($_GET['idSucursal']) : 'No proporcionado'; ?><br>
                    - empresaId en URL: <?php echo !empty($_GET['empresaId']) ? htmlspecialchars($_GET['empresaId']) : 'No proporcionado'; ?><br>
                    - Total de reservas recibidas: <?php echo count($reservas); ?><br>
                    <?php if (!empty($reservas)): ?>
                        - Primera reserva - idSucursal: <?php 
                            $primera = $reservas[0];
                            $sucursalId = $primera['idSucursal'] ?? $primera['sucursalId'] ?? 
                                        ($primera['sucursal']['idSucursal'] ?? $primera['sucursal']['id'] ?? $primera['sucursal']['sucursalId'] ?? 'No encontrado');
                            echo htmlspecialchars($sucursalId !== 'No encontrado' ? $sucursalId : 'No encontrado');
                        ?><br>
                    <?php endif; ?>
                </div>
            <?php endif; ?>
            <div class="table-responsive">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Código</th>
                            <th>Cliente</th>
                            <th>Fecha Reserva</th>
                            <th>Fecha Servicio</th>
                            <th>Personas</th>
                            <th>Total (S/)</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if ($error && empty($reservas)): ?>
                            <tr>
                                <td colspan="8" class="texto-vacio" style="color:#dc3545;">
                                    <i class="fas fa-exclamation-triangle"></i> <?php echo htmlspecialchars($error); ?>
                                </td>
                            </tr>
                        <?php elseif (empty($reservas)): ?>
                            <tr>
                                <td colspan="8" class="texto-vacio">
                                    <?php if ($busquedaActual !== ''): ?>
                                        No se encontraron reservas para "<?php echo htmlspecialchars($busquedaActual, ENT_QUOTES, 'UTF-8'); ?>"
                                    <?php else: ?>
                                        No hay reservas registradas
                                    <?php endif; ?>
                                </td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($reservas as $reserva): ?>
                                <?php
                                // Obtener el ID y asegurarse de que sea numérico
                                $idRaw = $reserva['idReserva'] ?? $reserva['id'] ?? null;
                                $id = null;
                                if ($idRaw !== null) {
                                    // Convertir a entero si es posible, sino mantener como string limpio
                                    if (is_numeric($idRaw)) {
                                        $id = (int) $idRaw;
                                    } else {
                                        // Si no es numérico, intentar extraer solo números
                                        $idNumeric = preg_replace('/[^0-9]/', '', (string) $idRaw);
                                        $id = $idNumeric !== '' ? (int) $idNumeric : null;
                                    }
                                }
                                $codigo = $reserva['codigoReserva'] ?? ($reserva['codigo'] ?? '');

                                $clienteNombre = '-';
                                if (isset($reserva['nombreCliente']) || isset($reserva['apellidoCliente'])) {
                                    $clienteNombre = trim(($reserva['nombreCliente'] ?? '') . ' ' . ($reserva['apellidoCliente'] ?? ''));
                                } elseif (isset($reserva['cliente']['nombre']) || isset($reserva['cliente']['apellido'])) {
                                    $clienteNombre = trim(($reserva['cliente']['nombre'] ?? '') . ' ' . ($reserva['cliente']['apellido'] ?? ''));
                                }
                                if ($clienteNombre === '') {
                                    $clienteNombre = isset($reserva['cliente']['nombreCompleto']) ? $reserva['cliente']['nombreCompleto'] : 'Sin asignar';
                                }

                                $servicioNombre = '-';
                                if (isset($reserva['nombreServicio']) && $reserva['nombreServicio']) {
                                    $servicioNombre = $reserva['nombreServicio'];
                                } elseif (isset($reserva['servicio']['nombreServicio']) && $reserva['servicio']['nombreServicio']) {
                                    $servicioNombre = $reserva['servicio']['nombreServicio'];
                                } elseif (isset($reserva['nombrePaquete']) && $reserva['nombrePaquete']) {
                                    $servicioNombre = $reserva['nombrePaquete'];
                                } elseif (isset($reserva['idPaquete'])) {
                                    $servicioNombre = 'Paquete #' . $reserva['idPaquete'];
                                }

                                $items = isset($reserva['items']) && is_array($reserva['items']) ? $reserva['items'] : [];
                                $itemsUi = buildReservaItemsUi($items);
                                $itemsSummary = $itemsUi['summary'] ?: ($servicioNombre ?: 'Sin ítems registrados');
                                $itemsCount = (int) $itemsUi['count'];
                                $itemsJson = htmlspecialchars(json_encode($items, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES), ENT_QUOTES, 'UTF-8');
                                $rawAsignaciones = [];
                                foreach (['asignaciones', 'asignacionesPersonal', 'personalAsignaciones', 'personalAsignado'] as $asignacionesKey) {
                                    if (isset($reserva[$asignacionesKey]) && is_array($reserva[$asignacionesKey])) {
                                        $rawAsignaciones = $reserva[$asignacionesKey];
                                        break;
                                    }
                                }
                                $normalizedAsignaciones = normalizeReservaAssignments($rawAsignaciones);
                                $asignacionesCount = count($normalizedAsignaciones);
                                $asignacionesJsonAttr = htmlspecialchars(json_encode($normalizedAsignaciones, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES), ENT_QUOTES, 'UTF-8');

                                $reservaForAttr = $reserva;
                                if (!isset($reservaForAttr['asignaciones']) || !is_array($reservaForAttr['asignaciones'])) {
                                    $reservaForAttr['asignaciones'] = $normalizedAsignaciones;
                                }
                                $reservaJson = htmlspecialchars(json_encode($reservaForAttr, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES), ENT_QUOTES, 'UTF-8');
                                $fallbackServicio = htmlspecialchars($servicioNombre, ENT_QUOTES, 'UTF-8');
                                $fallbackPaquete = htmlspecialchars($reserva['nombrePaquete'] ?? '', ENT_QUOTES, 'UTF-8');
                                $itemsSummaryAttr = htmlspecialchars($itemsSummary, ENT_QUOTES, 'UTF-8');

                                $fechaServicio = '-';
                                if (!empty($reserva['fechaServicio'])) {
                                    $fechaServicio = date('d/m/Y', strtotime($reserva['fechaServicio']));
                                } elseif (!empty($reserva['fechaServicioInicio'])) {
                                    $fechaServicio = date('d/m/Y', strtotime($reserva['fechaServicioInicio']));
                                }

                                $fechaReserva = '-';
                                if (!empty($reserva['fechaReserva'])) {
                                    $fechaReserva = date('d/m/Y', strtotime($reserva['fechaReserva']));
                                }

                                $personas = $reserva['numeroPersonas'] ?? ($reserva['cantidadPersonas'] ?? '-');
                                $total = $reserva['precioTotal'] ?? ($reserva['total'] ?? 0);
                                $estado = $reserva['estado'] ?? ($reserva['estadoReserva'] ?? 'Desconocido');

                                $estadoClass = $statusClasses[$estado] ?? 'status-unknown';
                                ?>
                                <tr>
                                    <td><?php echo htmlspecialchars($codigo ?: '-'); ?></td>
                                    <td><?php echo htmlspecialchars($clienteNombre); ?></td>
                                    <td><?php echo htmlspecialchars($fechaReserva); ?></td>
                                    <td><?php echo htmlspecialchars($fechaServicio); ?></td>
                                    <td>
                                        <div class="personas-cell">
                                            <span class="personas-main"><?php echo htmlspecialchars($personas); ?></span>
                                            <?php if ($asignacionesCount > 0): ?>
                                                <span class="chip chip-personal" title="Personal asignado">
                                                    <i class="fas fa-user-friends"></i>
                                                    <?php echo htmlspecialchars((string) $asignacionesCount, ENT_QUOTES, 'UTF-8'); ?>
                                                </span>
                                            <?php endif; ?>
                                        </div>
                                    </td>
                                    <td><?php echo number_format((float) $total, 2); ?></td>
                                    <td>
                                        <span class="status-badge <?php echo $estadoClass; ?>">
                                            <?php echo htmlspecialchars($estado); ?>
                                        </span>
                                    </td>
                                    <td>
                                        <div class="action-buttons">
                                            <?php if ($id !== null): ?>
                                                <button
                                                    type="button"
                                                    class="btn-action btn-view"
                                                    title="Ver detalle"
                                                    data-reserva-detalle="<?php echo htmlspecialchars((string) $id, ENT_QUOTES, 'UTF-8'); ?>"
                                                    data-reserva-items="<?php echo $itemsJson; ?>"
                                                    data-reserva-json="<?php echo $reservaJson; ?>"
                                                    data-reserva-servicio="<?php echo $fallbackServicio; ?>"
                                                    data-reserva-paquete="<?php echo $fallbackPaquete; ?>"
                                                    data-reserva-items-summary="<?php echo $itemsSummaryAttr; ?>"
                                                    data-reserva-codigo="<?php echo htmlspecialchars($codigo ?: (string) $id, ENT_QUOTES, 'UTF-8'); ?>"
                                                    data-reserva-asignaciones="<?php echo $asignacionesJsonAttr; ?>"
                                                >
                                                    <i class="fas fa-eye"></i>
                                                </button>
                                                <?php if ($estado === 'Pendiente'): ?>
                                                    <button type="button" class="btn-action btn-edit" title="Editar reserva" data-editar-reserva="<?php echo htmlspecialchars($id); ?>" data-reserva-codigo="<?php echo htmlspecialchars($codigo ?: (string) $id); ?>">
                                                        <i class="fas fa-pen"></i>
                                                    </button>
                                                <?php endif; ?>
                                                <?php if ($estado === 'Pagada'): ?>
                                                    <button type="button" class="btn-action btn-complete" title="Marcar como completada" data-completar-reserva="<?php echo htmlspecialchars($id); ?>" data-reserva-codigo="<?php echo htmlspecialchars($codigo ?: (string) $id); ?>">
                                                        <i class="fas fa-check"></i>
                                                    </button>
                                                <?php endif; ?>
                                                <?php if ($estado !== 'Completada' && $estado !== 'Cancelada'): ?>
                                                    <button type="button" class="btn-action btn-delete" title="Cancelar reserva" data-cancel-reserva="<?php echo htmlspecialchars($id); ?>" data-reserva-codigo="<?php echo htmlspecialchars($codigo ?: (string) $id); ?>">
                                                        <i class="fas fa-ban"></i>
                                                    </button>
                                                <?php endif; ?>
                                            <?php else: ?>
                                                <span class="btn-action btn-view" style="opacity:0.5;cursor:not-allowed;" title="ID no disponible">
                                                    <i class="fas fa-eye"></i>
                                                </span>
                                                <span class="btn-action btn-edit" style="opacity:0.5;cursor:not-allowed;" title="ID no disponible">
                                                    <i class="fas fa-pen"></i>
                                                </span>
                                                <span class="btn-action btn-delete" style="opacity:0.5;cursor:not-allowed;" title="ID no disponible">
                                                    <i class="fas fa-ban"></i>
                                                </span>
                                            <?php endif; ?>
                                        </div>
                                    </td>
                                </tr>
                            <?php endforeach; ?>
                        <?php endif; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
<div class="modal-overlay" id="reservaDetailModal" aria-hidden="true">
    <div class="modal-content reserva-modal">
        <div class="modal-header">
            <h3 class="modal-title" id="reservaDetailTitle">Detalle de la reserva</h3>
            <button type="button" class="close-modal" data-close-modal aria-label="Cerrar">
                <i class="fas fa-times"></i>
            </button>
        </div>
        <div class="modal-body reserva-modal-body" id="reservaDetailBody">
            <div class="reserva-modal-placeholder">
                <i class="fas fa-eye"></i>
                <p>Selecciona una reserva para ver el detalle.</p>
            </div>
        </div>
    </div>
</div>
<script>
    (function () {
        const apiBaseReservas = '<?php echo addslashes($apiReservasBase); ?>';
        const estadoClassMap = {
            Pendiente: 'status-reserved',
            Confirmada: 'status-available',
            PagoParcial: 'status-reserved',
            Pagada: 'status-active',
            Completada: 'status-active',
            Cancelada: 'status-inactive'
        };
        const assignmentStateClassMap = {
            Asignado: 'chip-personal',
            Completado: 'chip-personal-completed',
            Cancelado: 'chip-personal-cancelled'
        };

        const searchInput = document.getElementById('reservasSearchInput');
        if (searchInput) {
            let debounceTimer = null;
            let lastSearchValue = searchInput.value.trim();
            let isSearching = false;

            const performSearch = async (rawValue, force = false) => {
                const nextValue = (rawValue || '').trim();
                if (!force && nextValue === lastSearchValue) {
                    return;
                }
                if (isSearching) {
                    return; // Evitar múltiples búsquedas simultáneas
                }
                
                lastSearchValue = nextValue;
                isSearching = true;

                // Mostrar indicador de búsqueda sutil sin reemplazar todo el contenido
                const reservasTable = document.querySelector('.reservas-table-container');
                const reservasTableBody = document.querySelector('.reservas-table tbody');
                let loadingIndicator = null;
                
                if (reservasTableBody && !document.querySelector('.search-loading-indicator')) {
                    loadingIndicator = document.createElement('div');
                    loadingIndicator.className = 'search-loading-indicator';
                    loadingIndicator.style.cssText = 'position: absolute; top: 10px; right: 10px; padding: 8px 16px; background: rgba(59, 130, 246, 0.1); border: 1px solid rgba(59, 130, 246, 0.3); border-radius: 6px; color: #3b82f6; font-size: 0.875rem; z-index: 1000; display: flex; align-items: center; gap: 8px;';
                    loadingIndicator.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Buscando...';
                    if (reservasTable) {
                        reservasTable.style.position = 'relative';
                        reservasTable.appendChild(loadingIndicator);
                    }
                }

                try {
                    if (typeof loadReservasContent === 'function') {
                        const filters = nextValue ? { busqueda: nextValue } : {};
                        await loadReservasContent(filters);
                    } else {
                        try {
                            const targetUrl = new URL(window.location.href);
                            if (nextValue) {
                                targetUrl.searchParams.set('busqueda', nextValue);
                            } else {
                                targetUrl.searchParams.delete('busqueda');
                            }
                            targetUrl.searchParams.delete('ajax');
                            window.location.href = targetUrl.toString();
                        } catch (error) {
                            window.location.href = nextValue ? ('?busqueda=' + encodeURIComponent(nextValue)) : window.location.pathname;
                        }
                    }
                } finally {
                    isSearching = false;
                    // Remover indicador de carga
                    if (loadingIndicator && loadingIndicator.parentNode) {
                        loadingIndicator.parentNode.removeChild(loadingIndicator);
                    }
                }
            };

            const scheduleSearch = (rawValue) => {
                clearTimeout(debounceTimer);
                // Aumentar el tiempo de debounce a 800ms para evitar recargas innecesarias
                debounceTimer = setTimeout(() => performSearch(rawValue), 800);
            };

            searchInput.addEventListener('input', (event) => {
                scheduleSearch(event.target.value);
            });

            searchInput.addEventListener('search', (event) => {
                clearTimeout(debounceTimer);
                performSearch(event.target.value, true);
            });

            searchInput.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    clearTimeout(debounceTimer);
                    performSearch(event.target.value, true);
                }
            });
        }

        const modal = document.getElementById('reservaDetailModal');
        const modalBody = document.getElementById('reservaDetailBody');
        const modalTitle = document.getElementById('reservaDetailTitle');
        let lastDetalleTrigger = null;

        function resolveAuthToken() {
            try {
                const stored = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                if (stored) {
                    const parsed = JSON.parse(stored);
                    if (parsed && typeof parsed === 'object' && parsed.token) {
                        return parsed.token;
                    }
                }
            } catch (error) {
                console.warn('No se pudo leer userData:', error);
            }

            const match = document.cookie.match(/(?:^|;\s*)userToken=([^;]+)/);
            if (match) {
                return decodeURIComponent(match[1]);
            }
            return null;
        }

        function escapeHtml(value) {
            if (value === null || value === undefined) {
                return '';
            }
            return String(value)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function formatLocalDate(value) {
            if (!value) {
                return '—';
            }
            const isoMatch = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
            if (isoMatch) {
                return `${isoMatch[3]}/${isoMatch[2]}/${isoMatch[1]}`;
            }
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) {
                return value;
            }
            return date.toLocaleDateString('es-PE', { day: '2-digit', month: 'long', year: 'numeric' });
        }

        function formatDateTime(value) {
            if (!value) {
                return '—';
            }
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) {
                return value;
            }
            return date.toLocaleString('es-PE', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        function formatCurrency(value) {
            const number = Number(value);
            if (Number.isNaN(number)) {
                return 'S/ 0.00';
            }
            return 'S/ ' + number.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }

        function parseJsonAttribute(trigger, attributeName) {
            if (!trigger) {
                return null;
            }
            const raw = trigger.getAttribute(attributeName);
            if (!raw) {
                return null;
            }
            try {
                return JSON.parse(raw);
            } catch (error) {
                console.warn('No se pudo parsear', attributeName, error);
                return null;
            }
        }

        function normalizeItemEntry(item, fallbackTipo) {
            if (!item || typeof item !== 'object') {
                return null;
            }

            const resolvedTipo = (item.tipoItem || item.tipo || fallbackTipo || '').toString().toUpperCase();
            const cantidad = Number(item.cantidad ?? item.cant ?? 1) || 1;
            const precioUnitario = Number(item.precioUnitario ?? item.precio_unitario ?? item.precio ?? 0) || 0;
            const precioTotal = Number(item.precioTotal ?? item.precio_total ?? item.total ?? (precioUnitario * cantidad)) || 0;
            const descripcionExtra = item.descripcionExtra ?? item.observaciones ?? item.descripcion ?? '';
            const nombreServicio = item.nombreServicio ?? item.servicio?.nombreServicio ?? item.nombre ?? item.descripcionExtra ?? '';
            const nombrePaquete = item.nombrePaquete ?? item.paquete?.nombrePaquete ?? '';

            return {
                tipoItem: resolvedTipo,
                cantidad,
                precioUnitario,
                precioTotal,
                descripcionExtra,
                nombreServicio,
                nombrePaquete,
                idServicio: item.idServicio ?? item.servicio?.idServicio ?? null,
                idPaquete: item.idPaquete ?? item.paquete?.idPaquete ?? null,
            };
        }

        function normalizeAssignmentEntry(entry) {
            if (!entry || typeof entry !== 'object') {
                return null;
            }

            const personal = (entry.personal && typeof entry.personal === 'object') ? entry.personal : {};
            const idAsignacion = Number(entry.idAsignacion ?? entry.id ?? 0) || null;
            const idPersonal = Number(entry.idPersonal ?? personal.idPersonal ?? personal.id ?? 0);

            if (!idPersonal) {
                return null;
            }

            let nombre = entry.nombre ?? entry.nombrePersonal ?? personal.nombreCompleto ?? '';
            if (!nombre) {
                if (entry.nombrePersonal || entry.apellidoPersonal) {
                    const partesDirectas = [entry.nombrePersonal ?? '', entry.apellidoPersonal ?? '']
                        .map((parte) => (parte ?? '').toString().trim())
                        .filter((parte) => parte !== '');
                    if (partesDirectas.length) {
                        nombre = partesDirectas.join(' ');
                    }
                }
                const partes = [
                    personal.nombres ?? personal.nombre ?? '',
                    personal.apellidos ?? personal.apellido ?? ''
                ]
                    .map((parte) => (parte ?? '').toString().trim())
                    .filter((parte) => parte !== '');
                if (partes.length) {
                    nombre = partes.join(' ');
                }
            }
            if (!nombre) {
                nombre = `Personal #${idPersonal}`;
            }

            let cargo = entry.cargo ?? entry.cargoPersonal ?? '';
            if (!cargo && personal.cargo) {
                cargo = typeof personal.cargo === 'object' && personal.cargo !== null
                    ? (personal.cargo.nombre ?? personal.cargo.descripcion ?? '')
                    : String(personal.cargo);
            }
            if (!cargo && personal.rol) {
                cargo = typeof personal.rol === 'object' && personal.rol !== null
                    ? (personal.rol.nombre ?? '')
                    : String(personal.rol);
            }
            if (!cargo) {
                cargo = 'Sin cargo';
            }

            const estado = (entry.estado ?? 'Asignado').toString();
            const fechaAsignacion = entry.fechaAsignacion ?? entry.fecha ?? null;
            const observaciones = entry.observaciones ?? '';
            const telefono = entry.telefono ?? entry.telefonoPersonal ?? personal.telefono ?? personal.celular ?? null;
            const email = entry.email ?? entry.emailPersonal ?? personal.email ?? null;

            return {
                idAsignacion,
                idPersonal,
                nombre,
                cargo,
                estado,
                fechaAsignacion,
                observaciones,
                telefono,
                email
            };
        }

        function shouldFetchDetailedAssignments(asignaciones) {
            if (!Array.isArray(asignaciones) || asignaciones.length === 0) {
                return true;
            }

            const fallbackPattern = /^personal\s*#\d+$/i;

            return asignaciones.some((entry) => {
                if (!entry || typeof entry !== 'object') {
                    return true;
                }

                const name = entry.nombre || '';
                const cargo = entry.cargo || '';

                const hasName = name !== '' && !fallbackPattern.test(name);
                const hasCargo = cargo !== '' && cargo.toLowerCase() !== 'sin cargo';

                return !(hasName && hasCargo);
            });
        }

        async function fetchDetailedAssignments(reservaId, token) {
            const requestUrl = `${apiBaseReservas}/${encodeURIComponent(reservaId)}/asignaciones`;
            const response = await fetch(requestUrl, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                let message = `No se pudo obtener el personal asignado (HTTP ${response.status})`;
                try {
                    const payload = await response.json();
                    if (payload && typeof payload === 'object') {
                        message = payload.message || payload.error || message;
                    }
                } catch (parseError) {
                    const fallback = await response.text();
                    if (fallback) {
                        message += `: ${fallback.substring(0, 120)}`;
                    }
                }
                throw new Error(message);
            }

            const payload = await response.json();
            const data = (payload && typeof payload === 'object')
                ? (payload.data ?? payload.asignaciones ?? payload)
                : [];

            return Array.isArray(data) ? data : [];
        }

        function enrichReservaWithTriggerData(reserva) {
            const trigger = lastDetalleTrigger;
            const baseFromTrigger = trigger ? parseJsonAttribute(trigger, 'data-reserva-json') : null;
            const enriched = {
                ...(baseFromTrigger && typeof baseFromTrigger === 'object' ? baseFromTrigger : {}),
                ...(reserva && typeof reserva === 'object' ? reserva : {})
            };

            const hasItems = Array.isArray(enriched.items) && enriched.items.length > 0;
            if (hasItems) {
                enriched.items = enriched.items
                    .map((entry) => normalizeItemEntry(entry))
                    .filter(Boolean);
            } else {
                const parsed = parseJsonAttribute(trigger, 'data-reserva-items');
                if (Array.isArray(parsed) && parsed.length > 0) {
                    enriched.items = parsed
                        .map((entry) => normalizeItemEntry(entry))
                        .filter(Boolean);
                }
            }

            if (!Array.isArray(enriched.items) || enriched.items.length === 0) {
                const fallbackItems = [];
                if (trigger) {
                    const fallbackServicio = trigger.getAttribute('data-reserva-servicio');
                    const fallbackPaquete = trigger.getAttribute('data-reserva-paquete');
                    const fallbackSummary = trigger.getAttribute('data-reserva-items-summary');
                    const cantidad = Number(enriched.numeroPersonas ?? enriched.cantidadPersonas ?? 1) || 1;
                    const total = Number(enriched.precioTotal ?? enriched.total ?? 0) || 0;

                    if (fallbackServicio) {
                        fallbackItems.push({
                            tipoItem: 'SERVICIO',
                            nombreServicio: fallbackServicio,
                            cantidad,
                            precioUnitario: cantidad > 0 ? total / cantidad : total,
                            precioTotal: total,
                            descripcionExtra: ''
                        });
                    }

                    if (fallbackPaquete && (!fallbackServicio || fallbackPaquete !== fallbackServicio)) {
                        fallbackItems.push({
                            tipoItem: 'PAQUETE',
                            nombrePaquete: fallbackPaquete,
                            cantidad,
                            precioUnitario: cantidad > 0 ? total / cantidad : total,
                            precioTotal: total,
                            descripcionExtra: ''
                        });
                    }

                    if (fallbackItems.length === 0 && fallbackSummary) {
                        fallbackItems.push({
                            tipoItem: 'SERVICIO',
                            nombreServicio: fallbackSummary,
                            cantidad,
                            precioUnitario: cantidad > 0 ? total / cantidad : total,
                            precioTotal: total,
                            descripcionExtra: ''
                        });
                    }
                }

                if (fallbackItems.length > 0) {
                    enriched.items = fallbackItems
                        .map((entry) => normalizeItemEntry(entry))
                        .filter(Boolean);
                }
            }

            if (!enriched.nombreServicio) {
                const firstServicio = Array.isArray(enriched.items)
                    ? enriched.items.find((item) => item.tipoItem === 'SERVICIO' && item.nombreServicio)
                    : null;
                if (firstServicio) {
                    enriched.nombreServicio = firstServicio.nombreServicio;
                }
            }

            if (!enriched.nombrePaquete) {
                const firstPaquete = Array.isArray(enriched.items)
                    ? enriched.items.find((item) => item.tipoItem === 'PAQUETE' && item.nombrePaquete)
                    : null;
                if (firstPaquete) {
                    enriched.nombrePaquete = firstPaquete.nombrePaquete;
                }
            }

            if (!enriched.codigoReserva && trigger) {
                enriched.codigoReserva = trigger.getAttribute('data-reserva-codigo') || enriched.codigo || null;
            }

            if (!enriched.fechaServicio && enriched.fechaServicioInicio) {
                enriched.fechaServicio = enriched.fechaServicioInicio;
            }

            if (!enriched.numeroPersonas && enriched.cantidadPersonas) {
                enriched.numeroPersonas = enriched.cantidadPersonas;
            }

            if (!enriched.precioTotal && enriched.total) {
                enriched.precioTotal = enriched.total;
            }

            if (!enriched.estado && enriched.estadoReserva) {
                enriched.estado = enriched.estadoReserva;
            }

            if (!Array.isArray(enriched.asignaciones) || enriched.asignaciones.length === 0) {
                const parsedAsignaciones = parseJsonAttribute(trigger, 'data-reserva-asignaciones');
                if (Array.isArray(parsedAsignaciones)) {
                    enriched.asignaciones = parsedAsignaciones;
                } else {
                    enriched.asignaciones = [];
                }
            }

            return enriched;
        }

        function buildFallbackReservaFromTrigger() {
            const trigger = lastDetalleTrigger;
            if (!trigger) {
                return null;
            }

            let base = parseJsonAttribute(trigger, 'data-reserva-json');
            if (!base || typeof base !== 'object') {
                base = {};
            }

            const reservaIdAttr = trigger.getAttribute('data-reserva-detalle');
            if (reservaIdAttr && !base.idReserva) {
                const numericId = Number(reservaIdAttr);
                base.idReserva = Number.isNaN(numericId) ? reservaIdAttr : numericId;
            }

            const codigoAttr = trigger.getAttribute('data-reserva-codigo');
            if (codigoAttr && !base.codigoReserva) {
                base.codigoReserva = codigoAttr;
            }

            const parsedItems = parseJsonAttribute(trigger, 'data-reserva-items');
            if (!Array.isArray(base.items) || base.items.length === 0) {
                base.items = Array.isArray(parsedItems) ? parsedItems : [];
            }

            if (!Array.isArray(base.asignaciones) || base.asignaciones.length === 0) {
                const parsedAssignments = parseJsonAttribute(trigger, 'data-reserva-asignaciones');
                base.asignaciones = Array.isArray(parsedAssignments) ? parsedAssignments : [];
            }

            const servicioAttr = trigger.getAttribute('data-reserva-servicio');
            if (servicioAttr && !base.nombreServicio) {
                base.nombreServicio = servicioAttr;
            }

            const paqueteAttr = trigger.getAttribute('data-reserva-paquete');
            if (paqueteAttr && !base.nombrePaquete) {
                base.nombrePaquete = paqueteAttr;
            }

            return enrichReservaWithTriggerData(base);
        }

        function buildEstadoBadge(estado) {
            const estadoKey = estado || 'Desconocido';
            const clase = estadoClassMap[estadoKey] || 'status-unknown';
            return `<span class="status-badge ${clase}">${escapeHtml(estadoKey)}</span>`;
        }

        let previousActiveElement = null;

        function openReservaModal() {
            if (!modal) {
                return;
            }
            // Guardar el elemento que tenía foco antes de abrir el modal
            previousActiveElement = document.activeElement;
            modal.classList.add('active');
            modal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('modal-open');
            
            // Mover el foco al primer elemento enfocable del modal (el botón de cerrar)
            const closeButton = modal.querySelector('[data-close-modal]');
            if (closeButton) {
                setTimeout(() => {
                    closeButton.focus();
                }, 100);
            }
        }

        function closeReservaModal() {
            if (!modal) {
                return;
            }
            
            // Remover el foco de cualquier elemento dentro del modal antes de cerrarlo
            const focusedElement = modal.querySelector(':focus');
            if (focusedElement) {
                focusedElement.blur();
            }
            
            modal.classList.remove('active');
            modal.setAttribute('aria-hidden', 'true');
            document.body.classList.remove('modal-open');
            
            // Devolver el foco al elemento que abrió el modal
            if (previousActiveElement && typeof previousActiveElement.focus === 'function') {
                try {
                    previousActiveElement.focus();
                } catch (e) {
                    // Si el elemento ya no existe o no puede recibir foco, ignorar el error
                    console.warn('No se pudo devolver el foco al elemento anterior:', e);
                }
            }
            previousActiveElement = null;
        }

        function setModalContent(html) {
            if (modalBody) {
                modalBody.innerHTML = html;
                // Forzar re-renderizado para asegurar que los estilos se apliquen
                modalBody.offsetHeight;
                
                // Asegurar que el modal body tenga los estilos correctos
                if (modalBody) {
                    modalBody.style.cssText += 'padding: 32px !important; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%) !important; display: flex !important; flex-direction: column !important; gap: 24px !important; overflow-y: auto !important;';
                }
                
                // Aplicar estilos directamente a las secciones si es necesario
                const sections = modalBody.querySelectorAll('.reserva-detail-section');
                sections.forEach(section => {
                    // Asegurar que las secciones sean visibles
                    section.style.cssText += 'display: block !important; visibility: visible !important; opacity: 1 !important;';
                    
                    // Asegurar que el contenido dentro de las secciones sea visible
                    const cards = section.querySelectorAll('.reserva-detail-card');
                    cards.forEach(card => {
                        card.style.cssText += 'display: block !important; visibility: visible !important; opacity: 1 !important;';
                    });
                    
                    const items = section.querySelectorAll('.reserva-item');
                    items.forEach(item => {
                        item.style.cssText += 'display: flex !important; visibility: visible !important; opacity: 1 !important;';
                    });
                    
                    const personalCards = section.querySelectorAll('.reserva-personal-card');
                    personalCards.forEach(card => {
                        card.style.cssText += 'display: flex !important; visibility: visible !important; opacity: 1 !important;';
                    });
                });
            }
        }

        function showLoadingContent() {
            setModalContent('<div class="reserva-modal-loader"><i class="fas fa-spinner fa-spin"></i> Cargando detalle...</div>');
        }

        function buildAssignmentStatusBadge(estado) {
            const key = (estado || 'Asignado').toString();
            const cssClass = assignmentStateClassMap[key] || assignmentStateClassMap.Asignado;
            const icon = key === 'Completado'
                ? 'fa-circle-check'
                : key === 'Cancelado'
                    ? 'fa-circle-xmark'
                    : 'fa-user-check';
            return `<span class="chip ${cssClass}"><i class="fas ${icon}"></i>${escapeHtml(key)}</span>`;
        }

        function buildAssignmentsSection(reserva) {
            const assignmentsRaw = Array.isArray(reserva && reserva.asignaciones) ? reserva.asignaciones : [];
            const assignments = assignmentsRaw
                .map((entry) => normalizeAssignmentEntry(entry))
                .filter(Boolean);

            if (!assignments.length) {
                return `
                    <div class="reserva-detail-section">
                        <h4>Personal asignado</h4>
                        <div class="reserva-personal-empty">
                            <i class="fas fa-user-slash"></i>
                            <span>No hay personal asignado a esta reserva.</span>
                        </div>
                    </div>
                `;
            }

            const listItems = assignments.map((assignment) => {
                const metaParts = [];
                if (assignment.fechaAsignacion) {
                    metaParts.push(`<span><i class="fas fa-calendar-day"></i>${formatLocalDate(assignment.fechaAsignacion)}</span>`);
                }
                if (assignment.telefono) {
                    metaParts.push(`<span><i class="fas fa-phone"></i>${escapeHtml(assignment.telefono)}</span>`);
                }
                if (assignment.email) {
                    metaParts.push(`<span><i class="fas fa-envelope"></i>${escapeHtml(assignment.email)}</span>`);
                }
                const metaHtml = metaParts.length ? `<div class="reserva-personal-meta" style="display: flex; flex-wrap: wrap; gap: 16px; font-size: 0.9rem; color: #475569; margin-top: 12px; padding: 12px 0; border-top: 1px solid #e2e8f0;">${metaParts.map(part => `<span style="display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px; background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%); border-radius: 8px; font-weight: 600; border: 1px solid #cbd5e1; color: #334155;"><i style="color: #64748b;"></i>${part.replace(/<i[^>]*>/g, '<i style="color: #64748b;">')}</span>`).join('')}</div>` : '';
                const notesHtml = assignment.observaciones
                    ? `<div class="reserva-personal-notes" style="padding: 12px 14px; border-radius: 10px; background: #f1f5f9; color: #374151; font-size: 0.9rem; display: flex; gap: 10px; align-items: flex-start;"><i class="fas fa-comment-dots" style="margin-top: 2px; color: #64748b;"></i>${escapeHtml(assignment.observaciones)}</div>`
                    : '';

                return `
                    <li class="reserva-personal-card" style="border: 2px solid #d1fae5; border-radius: 14px; padding: 20px 24px; background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%); display: flex; flex-direction: column; gap: 14px; box-shadow: 0 4px 12px rgba(34, 197, 94, 0.1); position: relative; overflow: visible; list-style: none;">
                        <div class="reserva-personal-header" style="display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap;">
                            <div class="reserva-personal-info" style="display: flex; flex-direction: column; gap: 4px;">
                                <span class="reserva-personal-name" style="font-weight: 600; color: #1f2937; font-size: 1rem;">${escapeHtml(assignment.nombre)}</span>
                                <span class="reserva-personal-role" style="font-size: 0.9rem; color: #6b7280;">${escapeHtml(assignment.cargo)}</span>
                            </div>
                            ${buildAssignmentStatusBadge(assignment.estado)}
                        </div>
                        ${metaHtml}
                        ${notesHtml}
                    </li>
                `;
            }).join('');

            return `
                <div class="reserva-detail-section" style="margin-bottom: 0; padding: 28px 32px; background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%); border: 2px solid #86efac; border-radius: 16px; box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15); position: relative; overflow: visible;">
                    <h4 style="margin: 0 0 24px 0; font-size: 1.25rem; font-weight: 800; color: #047857; display: flex; align-items: center; gap: 12px; padding: 0 0 18px 0; text-transform: uppercase; letter-spacing: 1px; border-bottom: 3px solid #bbf7d0; position: relative;">
                        Personal asignado (${assignments.length})
                        <span style="position: absolute; left: 0; bottom: -3px; width: 80px; height: 3px; background: linear-gradient(90deg, #22c55e 0%, #10b981 100%); border-radius: 2px; box-shadow: 0 2px 4px rgba(34, 197, 94, 0.3);"></span>
                    </h4>
                    <ul class="reserva-personal-list" style="list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px;">
                        ${listItems}
                    </ul>
                </div>
            `;
        }

        function renderReservaDetail(reserva) {
            if (!modalBody || !modalTitle) {
                return;
            }

            const codigo = reserva.codigoReserva || `#${reserva.idReserva ?? ''}`;
            modalTitle.textContent = `Detalle de la reserva ${codigo}`;

            const generalInfo = `
                <div class="reserva-detail-section" style="margin-bottom: 0; padding: 28px 32px; background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%); border: 2px solid #86efac; border-radius: 16px; box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15); position: relative; overflow: visible;">
                    <h4 style="margin: 0 0 24px 0; font-size: 1.25rem; font-weight: 800; color: #047857; display: flex; align-items: center; gap: 12px; padding: 0 0 18px 0; text-transform: uppercase; letter-spacing: 1px; border-bottom: 3px solid #bbf7d0; position: relative;">
                        Información general
                        <span style="position: absolute; left: 0; bottom: -3px; width: 80px; height: 3px; background: linear-gradient(90deg, #22c55e 0%, #10b981 100%); border-radius: 2px; box-shadow: 0 2px 4px rgba(34, 197, 94, 0.3);"></span>
                    </h4>
                    <div class="reserva-detail-status" style="display: flex !important; align-items: center; gap: 16px; margin-bottom: 20px; visibility: visible !important; opacity: 1 !important;">
                        ${buildEstadoBadge(reserva.estado)}
                        <span class="reserva-detail-code" style="color: #6b7280; font-size: 0.95rem; font-weight: 500; display: flex; align-items: center; gap: 6px; visibility: visible !important; opacity: 1 !important;"><i class="fas fa-hashtag"></i>${escapeHtml(codigo)}</span>
                    </div>
                    <div class="reserva-detail-grid" style="display: grid !important; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 18px; visibility: visible !important; opacity: 1 !important;">
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible;">
                            <span class="detail-label" style="display: block !important; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9; visibility: visible !important;">Fecha servicio</span>
                            <span class="detail-value" style="display: block !important; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em; visibility: visible !important; opacity: 1 !important;">${formatLocalDate(reserva.fechaServicio)}</span>
                        </div>
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible;">
                            <span class="detail-label" style="display: block; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9;">Fecha reserva</span>
                            <span class="detail-value" style="display: block; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em;">${formatLocalDate(reserva.fechaReserva)}</span>
                        </div>
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible;">
                            <span class="detail-label" style="display: block; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9;">Personas</span>
                            <span class="detail-value" style="display: block; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em;">${escapeHtml(reserva.numeroPersonas ?? '—')}</span>
                        </div>
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible;">
                            <span class="detail-label" style="display: block; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9;">Total</span>
                            <span class="detail-value" style="display: block; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em;">${formatCurrency(reserva.precioTotal)}</span>
                        </div>
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible;">
                            <span class="detail-label" style="display: block; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9;">Descuento</span>
                            <span class="detail-value" style="display: block; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em;">${formatCurrency(reserva.descuentoAplicado ?? 0)}</span>
                        </div>
                    </div>
                </div>
            `;

            const clienteNombre = (reserva.nombreCliente && reserva.apellidoCliente)
                ? `${reserva.nombreCliente} ${reserva.apellidoCliente}`
                : (reserva.nombreCliente || reserva.apellidoCliente || 'No registrado');

            const clienteInfo = `
                <div class="reserva-detail-section" style="margin-bottom: 0; padding: 28px 32px; background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%); border: 2px solid #86efac; border-radius: 16px; box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15); position: relative; overflow: visible;">
                    <h4 style="margin: 0 0 24px 0; font-size: 1.25rem; font-weight: 800; color: #047857; display: flex; align-items: center; gap: 12px; padding: 0 0 18px 0; text-transform: uppercase; letter-spacing: 1px; border-bottom: 3px solid #bbf7d0; position: relative;">
                        Cliente
                        <span style="position: absolute; left: 0; bottom: -3px; width: 80px; height: 3px; background: linear-gradient(90deg, #22c55e 0%, #10b981 100%); border-radius: 2px; box-shadow: 0 2px 4px rgba(34, 197, 94, 0.3);"></span>
                    </h4>
                    <div class="reserva-detail-grid" style="display: grid !important; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 18px; visibility: visible !important; opacity: 1 !important;">
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible; display: block !important; visibility: visible !important; opacity: 1 !important;">
                            <span class="detail-label" style="display: block; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9;">Nombre</span>
                            <span class="detail-value" style="display: block; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em;">${escapeHtml(clienteNombre)}</span>
                        </div>
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible;">
                            <span class="detail-label" style="display: block; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9;">Correo</span>
                            <span class="detail-value" style="display: block; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em;">${escapeHtml(reserva.emailCliente ?? '—')}</span>
                        </div>
                        <div class="reserva-detail-card" style="padding: 20px 24px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); border-radius: 12px; border: 1.5px solid #bbf7d0; box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08); position: relative; overflow: visible;">
                            <span class="detail-label" style="display: block; font-size: 0.75rem; color: #059669; margin-bottom: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 1.2px; opacity: 0.9;">Teléfono</span>
                            <span class="detail-value" style="display: block; font-size: 1.2rem; font-weight: 800; color: #0f172a; line-height: 1.5; letter-spacing: -0.02em;">${escapeHtml(reserva.telefonoCliente ?? '—')}</span>
                        </div>
                    </div>
                </div>
            `;

            const items = Array.isArray(reserva.items) ? reserva.items : [];
            let itemsMarkup = '<div class="reserva-item-empty">Esta reserva no tiene servicios ni paquetes registrados.</div>';
            if (items.length > 0) {
                const itemsHtml = items.map((item) => {
                    const tipo = (item.tipoItem || '').toString().toUpperCase();
                    const isPaquete = tipo === 'PAQUETE';
                    const tipoLabel = isPaquete ? 'Paquete' : 'Servicio';
                    const iconClass = isPaquete ? 'fa-box' : 'fa-suitcase-rolling';
                    const nombre = item.nombreServicio || item.nombrePaquete || item.descripcionExtra || `${tipoLabel} #${item.idServicio || item.idPaquete || item.idReservaItem || ''}`;
                    const cantidad = item.cantidad ?? 1;
                    const precioUnitario = formatCurrency(item.precioUnitario ?? 0);
                    const precioTotal = formatCurrency(item.precioTotal ?? (cantidad * (item.precioUnitario ?? 0)));
                    const descripcionExtra = item.descripcionExtra ? `<div class="reserva-item-notes"><i class="fas fa-comment-dots"></i> ${escapeHtml(item.descripcionExtra)}</div>` : '';
                    return `
                        <li class="reserva-item" style="border: 2px solid #d1fae5; border-radius: 14px; padding: 20px 24px; background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%); display: flex; flex-direction: column; gap: 14px; box-shadow: 0 4px 12px rgba(34, 197, 94, 0.1); position: relative; overflow: visible; list-style: none; margin-bottom: 14px;">
                            <div class="reserva-item-header" style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                                <span class="chip ${isPaquete ? 'chip-paquete' : 'chip-servicio'}" style="display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: 20px; font-size: 0.85rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1); ${isPaquete ? 'background: linear-gradient(135deg, rgba(139, 92, 246, 0.15) 0%, rgba(109, 40, 217, 0.2) 100%); color: #6d28d9; border: 1.5px solid rgba(139, 92, 246, 0.3);' : 'background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(37, 99, 235, 0.2) 100%); color: #1e40af; border: 1.5px solid rgba(59, 130, 246, 0.3);'}"><i class="fas ${iconClass}"></i>${tipoLabel}</span>
                                <span class="reserva-item-name" style="font-weight: 600; color: #1f2937; font-size: 1rem; flex: 1;">${escapeHtml(nombre)}</span>
                            </div>
                            <div class="reserva-item-meta" style="display: flex; gap: 20px; font-size: 0.9rem; color: #475569; flex-wrap: wrap; margin-top: 12px; padding: 12px 0; border-top: 1px solid #e2e8f0;">
                                <span style="display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px; background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%); border-radius: 8px; font-weight: 600; border: 1px solid #cbd5e1; color: #334155;"><i class="fas fa-users" style="color: #64748b;"></i>${escapeHtml(cantidad)}</span>
                                <span style="display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px; background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%); border-radius: 8px; font-weight: 600; border: 1px solid #cbd5e1; color: #334155;"><i class="fas fa-tag" style="color: #64748b;"></i>${precioUnitario}</span>
                                <span style="display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px; background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%); border-radius: 8px; font-weight: 600; border: 1px solid #cbd5e1; color: #334155;"><i class="fas fa-wallet" style="color: #64748b;"></i>${precioTotal}</span>
                            </div>
                            ${descripcionExtra ? `<div class="reserva-item-notes" style="padding: 12px 14px; background: #f1f5f9; border-radius: 10px; color: #374151; font-size: 0.9rem; display: flex; align-items: flex-start; gap: 10px;"><i class="fas fa-comment-dots" style="margin-top: 3px; color: #64748b;"></i> ${escapeHtml(item.descripcionExtra)}</div>` : ''}
                        </li>
                    `;
                }).join('');
                itemsMarkup = `<ul class="reserva-item-list">${itemsHtml}</ul>`;
            }

            const itemsSection = `
                <div class="reserva-detail-section" style="margin-bottom: 0; padding: 28px 32px; background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%); border: 2px solid #86efac; border-radius: 16px; box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15); position: relative; overflow: visible;">
                    <h4 style="margin: 0 0 24px 0; font-size: 1.25rem; font-weight: 800; color: #047857; display: flex; align-items: center; gap: 12px; padding: 0 0 18px 0; text-transform: uppercase; letter-spacing: 1px; border-bottom: 3px solid #bbf7d0; position: relative;">
                        Ítems reservados
                        <span style="position: absolute; left: 0; bottom: -3px; width: 80px; height: 3px; background: linear-gradient(90deg, #22c55e 0%, #10b981 100%); border-radius: 2px; box-shadow: 0 2px 4px rgba(34, 197, 94, 0.3);"></span>
                    </h4>
                    ${itemsMarkup}
                </div>
            `;

            const observaciones = reserva.observaciones ? escapeHtml(reserva.observaciones) : 'Sin observaciones registradas.';
            const observacionesSection = `
                <div class="reserva-detail-section" style="margin-bottom: 0; padding: 28px 32px; background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%); border: 2px solid #86efac; border-radius: 16px; box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15); position: relative; overflow: visible;">
                    <h4 style="margin: 0 0 24px 0; font-size: 1.25rem; font-weight: 800; color: #047857; display: flex; align-items: center; gap: 12px; padding: 0 0 18px 0; text-transform: uppercase; letter-spacing: 1px; border-bottom: 3px solid #bbf7d0; position: relative;">
                        Observaciones
                        <span style="position: absolute; left: 0; bottom: -3px; width: 80px; height: 3px; background: linear-gradient(90deg, #22c55e 0%, #10b981 100%); border-radius: 2px; box-shadow: 0 2px 4px rgba(34, 197, 94, 0.3);"></span>
                    </h4>
                    <div class="reserva-observaciones" style="padding: 20px 24px; border-radius: 12px; background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%); color: #1e293b; line-height: 1.8; border: 1.5px solid #bbf7d0; font-size: 0.95rem; box-shadow: inset 0 2px 4px rgba(34, 197, 94, 0.05);">${observaciones}</div>
                </div>
            `;

            const personalSection = buildAssignmentsSection(reserva);

            const fullContent = `${generalInfo}${clienteInfo}${itemsSection}${personalSection}${observacionesSection}`;
            console.log('📋 Contenido completo generado:', fullContent.substring(0, 500));
            console.log('📊 Longitud del contenido:', fullContent.length);
            setModalContent(fullContent);
            
            // Verificar que el contenido se insertó correctamente
            setTimeout(() => {
                const insertedSections = modalBody.querySelectorAll('.reserva-detail-section');
                console.log('✅ Secciones insertadas:', insertedSections.length);
                insertedSections.forEach((section, index) => {
                    const title = section.querySelector('h4');
                    const content = section.innerHTML;
                    console.log(`📦 Sección ${index + 1}:`, title ? title.textContent : 'Sin título', 'Contenido length:', content.length);
                });
            }, 100);
        }

        async function fetchReservaDetalle(reservaId) {
            const token = resolveAuthToken();
            if (!token) {
                if (typeof mostrarAlerta === 'function') {
                    mostrarAlerta('error', 'No se encontró token de sesión. Vuelve a iniciar sesión.');
                } else {
                    alert('No se encontró token de sesión. Vuelve a iniciar sesión.');
                }
                return;
            }

            // Limpiar y validar el ID de la reserva
            let cleanReservaId = String(reservaId || '').trim();
            // Remover cualquier carácter no numérico al final (como ":1")
            cleanReservaId = cleanReservaId.replace(/[^0-9]+$/, '');
            // Si no es un número válido, intentar extraer solo números
            if (!/^\d+$/.test(cleanReservaId)) {
                const numericMatch = cleanReservaId.match(/\d+/);
                if (numericMatch) {
                    cleanReservaId = numericMatch[0];
                } else {
                    console.error('ID de reserva inválido:', reservaId);
                    setModalContent(`<div class="reserva-modal-error"><i class="fas fa-exclamation-triangle"></i> ID de reserva inválido: ${escapeHtml(String(reservaId))}</div>`);
                    return;
                }
            }

            if (!cleanReservaId || cleanReservaId === '0' || cleanReservaId === 'null' || cleanReservaId === 'undefined') {
                console.error('ID de reserva no válido:', reservaId);
                setModalContent(`<div class="reserva-modal-error"><i class="fas fa-exclamation-triangle"></i> No se pudo obtener el ID de la reserva.</div>`);
                return;
            }

            openReservaModal();
            showLoadingContent();
            if (modalTitle) {
                modalTitle.textContent = 'Detalle de la reserva';
            }

            try {
                const requestUrl = `${apiBaseReservas}/${encodeURIComponent(cleanReservaId)}`;
                console.log('🔍 Solicitando detalle de reserva:', requestUrl);
                const response = await fetch(requestUrl, {
                    method: 'GET',
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (!response.ok) {
                    let errorMessage = `Error del servidor (HTTP ${response.status})`;
                    let errorDetails = '';
                    
                    try {
                        const errorText = await response.text();
                        if (errorText) {
                            try {
                                const errorJson = JSON.parse(errorText);
                                errorMessage = errorJson.message || errorJson.error || errorMessage;
                                errorDetails = errorJson.details || errorJson.stack || '';
                            } catch (e) {
                                errorDetails = errorText.substring(0, 200);
                            }
                        }
                    } catch (parseError) {
                        console.error('Error al parsear respuesta de error:', parseError);
                    }
                    
                    console.error('❌ Error al obtener detalle de reserva:', {
                        status: response.status,
                        statusText: response.statusText,
                        url: requestUrl,
                        message: errorMessage,
                        details: errorDetails
                    });
                    
                    // Si es error 500, intentar usar datos del fallback
                    if (response.status === 500) {
                        console.warn('⚠️ Error 500 del servidor, intentando usar datos locales...');
                        const fallbackReserva = buildFallbackReservaFromTrigger();
                        if (fallbackReserva && Object.keys(fallbackReserva).length > 0) {
                            console.log('✅ Usando datos de fallback exitosamente:', fallbackReserva);
                            // Renderizar sin mostrar advertencia si tenemos datos suficientes
                            renderReservaDetail(fallbackReserva);
                            // Solo mostrar advertencia si faltan datos importantes
                            const tieneDatosMinimos = fallbackReserva.codigoReserva || fallbackReserva.idReserva;
                            if (!tieneDatosMinimos && modalBody) {
                                modalBody.insertAdjacentHTML('afterbegin', 
                                    `<div class="reserva-modal-warning">
                                        <i class="fas fa-info-circle"></i> 
                                        Algunos datos pueden estar incompletos debido a un problema temporal del servidor.
                                    </div>`
                                );
                            }
                            return;
                        }
                    }
                    
                    throw new Error(errorMessage + (errorDetails ? `: ${errorDetails}` : ''));
                }

                const payload = await response.json();
                const reserva = (payload && typeof payload === 'object') ? (payload.data || payload) : null;
                if (!reserva || typeof reserva !== 'object') {
                    throw new Error('No se pudo interpretar la respuesta del servidor.');
                }

                if (shouldFetchDetailedAssignments(reserva.asignaciones)) {
                    try {
                        const detailedAssignments = await fetchDetailedAssignments(cleanReservaId, token);
                        if (Array.isArray(detailedAssignments) && detailedAssignments.length > 0) {
                            reserva.asignaciones = detailedAssignments;
                        }
                    } catch (detailError) {
                        console.warn('No se pudo enriquecer el personal asignado:', detailError);
                    }
                }

                const enriched = enrichReservaWithTriggerData(reserva);
                renderReservaDetail(enriched);
            } catch (error) {
                console.error('❌ Error al cargar detalle de la reserva:', error);
                console.error('Detalles del error:', {
                    message: error.message,
                    stack: error.stack,
                    reservaId: cleanReservaId,
                    originalReservaId: reservaId
                });
                
                // Intentar usar datos del fallback
                const fallbackReserva = buildFallbackReservaFromTrigger();
                if (fallbackReserva && Object.keys(fallbackReserva).length > 0) {
                    console.log('✅ Usando datos de fallback como respaldo:', fallbackReserva);
                    renderReservaDetail(fallbackReserva);
                    // Solo mostrar advertencia si faltan datos importantes
                    const tieneDatosMinimos = fallbackReserva.codigoReserva || fallbackReserva.idReserva;
                    if (!tieneDatosMinimos && modalBody) {
                        const errorMsg = error.message || 'Error desconocido';
                        modalBody.insertAdjacentHTML('afterbegin', 
                            `<div class="reserva-modal-warning">
                                <i class="fas fa-info-circle"></i> 
                                Algunos datos pueden estar incompletos. Error: ${escapeHtml(errorMsg)}
                            </div>`
                        );
                    }
                } else {
                    // Si no hay datos de fallback, mostrar error completo
                    const errorDetails = error.message || 'Error inesperado al cargar el detalle de la reserva';
                    setModalContent(
                        `<div class="reserva-modal-error">
                            <i class="fas fa-exclamation-triangle"></i> 
                            <strong>Error al cargar el detalle</strong>
                            <p style="margin-top: 12px; font-size: 0.9rem;">${escapeHtml(errorDetails)}</p>
                            <p style="margin-top: 8px; font-size: 0.85rem; opacity: 0.8;">
                                ID de reserva: ${escapeHtml(String(cleanReservaId))}
                            </p>
                        </div>`
                    );
                }
            }
        }

        document.addEventListener('click', (event) => {
            const trigger = event.target.closest('[data-reserva-detalle]');
            if (trigger) {
                event.preventDefault();
                lastDetalleTrigger = trigger;
                let reservaId = trigger.getAttribute('data-reserva-detalle');
                if (reservaId) {
                    // Limpiar el ID antes de usarlo
                    reservaId = String(reservaId).trim();
                    console.log('🔍 ID de reserva obtenido del atributo:', reservaId);
                    fetchReservaDetalle(reservaId);
                } else {
                    console.error('No se encontró el atributo data-reserva-detalle en el elemento');
                }
            }
        });

        document.addEventListener('click', (event) => {
            const trigger = event.target.closest('[data-editar-reserva]');
            if (!trigger) {
                return;
            }
            event.preventDefault();
            const reservaId = trigger.getAttribute('data-editar-reserva');
            if (!reservaId) {
                return;
            }

            if (typeof loadEditarReservaContent === 'function') {
                loadEditarReservaContent(reservaId);
            } else {
                const basePath = window.location.pathname.replace(/[^/]*$/, '');
                window.location.href = basePath + 'RESERVAS/editar_reserva.php?id=' + encodeURIComponent(reservaId);
            }
        });

        if (modal) {
            modal.addEventListener('click', (event) => {
                if (event.target === modal) {
                    closeReservaModal();
                }
            });
        }

        const closeButton = modal ? modal.querySelector('[data-close-modal]') : null;
        if (closeButton) {
            closeButton.addEventListener('click', (event) => {
                event.preventDefault();
                closeReservaModal();
            });
        }

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                closeReservaModal();
            }
        });

        const cancelButtons = document.querySelectorAll('[data-cancel-reserva]');
        cancelButtons.forEach((button) => {
            button.addEventListener('click', async () => {
                const reservaId = button.getAttribute('data-cancel-reserva');
                if (!reservaId) {
                    return;
                }

                const codigo = button.getAttribute('data-reserva-codigo') || '';
                const mensaje = codigo
                    ? `¿Deseas cancelar la reserva #${codigo}?`
                    : '¿Deseas cancelar esta reserva?';

                if (!window.confirm(mensaje)) {
                    return;
                }

                const token = resolveAuthToken();
                if (!token) {
                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('error', 'No se encontró token de sesión. Vuelve a iniciar sesión.');
                    } else {
                        alert('No se encontró token de sesión. Vuelve a iniciar sesión.');
                    }
                    return;
                }

                const originalHtml = button.innerHTML;
                button.disabled = true;
                button.classList.add('btn-action-busy');
                button.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

                try {
                    const requestUrl = `${apiBaseReservas}/${encodeURIComponent(reservaId)}/cancelar`;
                    const response = await fetch(requestUrl, {
                        method: 'PUT',
                        headers: {
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json'
                        }
                    });

                    if (!response.ok) {
                        let errorMessage = `Error al cancelar la reserva (HTTP ${response.status})`;
                        try {
                            const errorPayload = await response.json();
                            if (errorPayload && typeof errorPayload === 'object') {
                                if (errorPayload.message) {
                                    errorMessage = errorPayload.message;
                                } else if (errorPayload.error) {
                                    errorMessage = errorPayload.error;
                                }
                            }
                        } catch (parseError) {
                            const fallbackText = await response.text();
                            if (fallbackText) {
                                errorMessage += `: ${fallbackText.substring(0, 120)}`;
                            }
                        }

                        throw new Error(errorMessage);
                    }

                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('success', 'Reserva cancelada correctamente.');
                    } else {
                        alert('Reserva cancelada correctamente.');
                    }

                    if (typeof loadReservasContent === 'function') {
                        loadReservasContent();
                    }
                } catch (error) {
                    console.error('Error al cancelar reserva:', error);
                    const message = error && error.message ? error.message : 'Error inesperado al cancelar la reserva.';
                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('error', message);
                    } else {
                        alert(message);
                    }
                } finally {
                    button.disabled = false;
                    button.classList.remove('btn-action-busy');
                    button.innerHTML = originalHtml;
                }
            });
        });

        const completarButtons = document.querySelectorAll('[data-completar-reserva]');
        completarButtons.forEach((button) => {
            button.addEventListener('click', async () => {
                const reservaId = button.getAttribute('data-completar-reserva');
                if (!reservaId) {
                    return;
                }

                const codigo = button.getAttribute('data-reserva-codigo') || '';
                const mensaje = codigo
                    ? `¿Confirmas marcar la reserva #${codigo} como completada?`
                    : '¿Confirmas marcar esta reserva como completada?';

                if (!window.confirm(mensaje)) {
                    return;
                }

                const token = resolveAuthToken();
                if (!token) {
                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('error', 'No se encontró token de sesión. Vuelve a iniciar sesión.');
                    } else {
                        alert('No se encontró token de sesión. Vuelve a iniciar sesión.');
                    }
                    return;
                }

                const originalHtml = button.innerHTML;
                button.disabled = true;
                button.classList.add('btn-action-busy');
                button.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

                try {
                    const requestUrl = `${apiBaseReservas}/${encodeURIComponent(reservaId)}/completar`;
                    const response = await fetch(requestUrl, {
                        method: 'PUT',
                        headers: {
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json'
                        }
                    });

                    if (!response.ok) {
                        let errorMessage = `Error al completar la reserva (HTTP ${response.status})`;
                        try {
                            const errorPayload = await response.json();
                            if (errorPayload && typeof errorPayload === 'object') {
                                errorMessage = errorPayload.message || errorPayload.error || errorMessage;
                            }
                        } catch (parseError) {
                            const fallbackText = await response.text();
                            if (fallbackText) {
                                errorMessage += `: ${fallbackText.substring(0, 120)}`;
                            }
                        }
                        throw new Error(errorMessage);
                    }

                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('success', 'Reserva marcada como completada.');
                    } else {
                        alert('Reserva marcada como completada.');
                    }

                    if (typeof loadReservasContent === 'function') {
                        loadReservasContent();
                    }
                } catch (error) {
                    console.error('Error al completar reserva:', error);
                    const message = error && error.message ? error.message : 'Error inesperado al completar la reserva.';
                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('error', message);
                    } else {
                        alert(message);
                    }
                } finally {
                    button.disabled = false;
                    button.classList.remove('btn-action-busy');
                    button.innerHTML = originalHtml;
                }
            });
        });

        // Exportar funciones globalmente
        window.resolveAuthToken = resolveAuthToken;
        window.fetchReservaDetalle = fetchReservaDetalle;
    })();
</script>
<style>
/* Estilos específicos para reservas - solo se aplican dentro de .reservas-content */
.reservas-content .status-badge {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 0.85rem;
    font-weight: 600;
}

.reservas-content .status-reserved {
    background-color: #fef3c7;
    color: #92400e;
}

.status-available {
    background-color: #d1fae5;
    color: #065f46;
}

.status-active {
    background-color: #dbeafe;
    color: #1e40af;
}

.status-inactive {
    background-color: #fee2e2;
    color: #991b1b;
}

.reservas-content .action-buttons {
    display: flex;
    gap: 8px;
    justify-content: center;
}

.reservas-content .btn-action {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
}

.reservas-content .btn-view {
    background-color: #6b7280;
    color: white;
}

.reservas-content .btn-view:hover {
    background-color: #4b5563;
}

.reservas-content .btn-edit {
    background-color: #3b82f6;
    color: white;
}

.reservas-content .btn-edit:hover {
    background-color: #2563eb;
}

.reservas-content .btn-delete {
    background-color: #f59e0b;
    color: white;
}

.reservas-content .btn-delete:hover {
    background-color: #d97706;
}

.reservas-content .btn-complete {
    background-color: #10b981;
    color: white;
}

.reservas-content .btn-complete:hover {
    background-color: #059669;
}

.reservas-content .btn-action-busy {
    opacity: 0.6;
    cursor: not-allowed;
}

.reservas-content .personas-cell {
    display: flex;
    align-items: center;
    gap: 8px;
}

.personas-main {
    font-weight: 500;
}

.reservas-content .chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 0.75rem;
    font-weight: 500;
}

.reservas-content .chip-personal {
    background-color: #e0e7ff;
    color: #3730a3;
}

.reservas-content .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: none;
    align-items: center;
    justify-content: center;
    z-index: 1000;
}

.reservas-content .modal-overlay.active {
    display: flex;
}

.reservas-content .modal-content {
    background: #f0fdf4 !important;
    border-radius: 8px;
    max-width: 850px;
    width: 90%;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
    border: 1px solid #86efac !important;
}

.reservas-content .modal-content.reserva-modal,
.modal-content.reserva-modal,
#reservaDetailModal .modal-content {
    background: #ffffff !important;
    border-radius: 16px;
    box-shadow: 0 25px 70px rgba(0, 0, 0, 0.3) !important;
    border: 2px solid #22c55e !important;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    max-width: 960px;
    width: 92%;
}

.reservas-content .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24px 28px;
    background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
    border-bottom: 3px solid #15803d;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    flex-shrink: 0;
}

.reservas-content .modal-title {
    margin: 0;
    font-size: 1.4rem;
    font-weight: 700;
    color: white;
    letter-spacing: -0.02em;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    gap: 10px;
}

.reservas-content .modal-title::before {
    content: '📋';
    font-size: 1.3rem;
}

.reservas-content .close-modal {
    background: rgba(255, 255, 255, 0.2);
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-radius: 8px;
    font-size: 1.25rem;
    cursor: pointer;
    color: white;
    padding: 8px 12px;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
}

.reservas-content .close-modal:hover {
    background: rgba(255, 255, 255, 0.3);
    border-color: rgba(255, 255, 255, 0.5);
    transform: rotate(90deg);
}

.reservas-content .modal-body {
    padding: 24px;
    background: #f0fdf4 !important;
}

.reservas-content .reserva-modal-body,
.reserva-modal-body,
#reservaDetailModal .reserva-modal-body,
#reservaDetailBody {
    padding: 32px !important;
    background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%) !important;
    flex: 1;
    overflow-y: auto;
    display: flex !important;
    flex-direction: column !important;
    gap: 24px !important;
}

.reservas-content .reserva-modal-placeholder {
    text-align: center;
    padding: 60px 40px;
    color: #64748b;
}

.reservas-content .reserva-modal-placeholder i {
    font-size: 4rem;
    color: #cbd5e1;
    margin-bottom: 20px;
    display: block;
    opacity: 0.6;
}

.reservas-content .reserva-modal-placeholder p {
    font-size: 1.125rem;
    font-weight: 500;
    color: #94a3b8;
}

.reservas-content .reserva-detail-section,
.reserva-detail-section,
#reservaDetailBody .reserva-detail-section {
    margin-bottom: 0 !important;
    padding: 28px 32px !important;
    background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%) !important;
    border: 2px solid #86efac !important;
    border-radius: 16px !important;
    box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15), 0 0 0 1px rgba(34, 197, 94, 0.05) !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: visible !important;
    display: block !important;
    visibility: visible !important;
    opacity: 1 !important;
    min-height: auto !important;
}

.reservas-content .reserva-detail-section::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #22c55e 0%, #10b981 50%, #059669 100%);
    opacity: 0.8;
}

.reservas-content .reserva-detail-section:hover {
    box-shadow: 0 8px 24px rgba(34, 197, 94, 0.25), 0 0 0 1px rgba(34, 197, 94, 0.1);
    border-color: #22c55e !important;
    transform: translateY(-3px);
}

.reservas-content .reserva-detail-section:last-child {
    margin-bottom: 0;
}

.reservas-content .reserva-detail-section h4 {
    margin: 0 0 24px 0;
    font-size: 1.25rem;
    font-weight: 800;
    color: #047857;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 0 18px 0;
    background: transparent;
    border-radius: 0;
    box-shadow: none;
    text-transform: uppercase;
    letter-spacing: 1px;
    border-bottom: 3px solid #bbf7d0;
    position: relative;
}

.reservas-content .reserva-detail-section h4::before {
    content: '';
    position: absolute;
    left: 0;
    bottom: -3px;
    width: 80px;
    height: 3px;
    background: linear-gradient(90deg, #22c55e 0%, #10b981 100%);
    border-radius: 2px;
    box-shadow: 0 2px 4px rgba(34, 197, 94, 0.3);
}

.reservas-content .reserva-detail-section h4 i {
    display: none;
}

.reservas-content .reserva-detail-status {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    padding: 0;
    background: transparent;
    border-radius: 0;
    border: none;
    box-shadow: none;
}

.reservas-content .reserva-detail-code {
    color: #6b7280;
    font-size: 0.95rem;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0;
    background: transparent;
    border-radius: 0;
    border: none;
}

.reservas-content .reserva-detail-code i {
    color: #9ca3af;
    font-size: 0.875rem;
}

.reservas-content .reserva-detail-grid,
.reserva-detail-grid,
#reservaDetailModal .reserva-detail-grid {
    display: grid !important;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)) !important;
    gap: 18px !important;
    visibility: visible !important;
    opacity: 1 !important;
}

.reservas-content .reserva-detail-card,
.reserva-detail-card,
#reservaDetailBody .reserva-detail-card {
    padding: 20px 24px !important;
    background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%) !important;
    border-radius: 12px !important;
    border: 1.5px solid #bbf7d0 !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08) !important;
    position: relative;
    overflow: visible !important;
    display: block !important;
    visibility: visible !important;
    opacity: 1 !important;
    min-height: 60px !important;
}

.reservas-content .reserva-detail-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(180deg, #22c55e 0%, #10b981 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
    z-index: 0;
    pointer-events: none;
}

.reservas-content .reserva-detail-card > *,
.reserva-detail-card > * {
    position: relative !important;
    z-index: 1 !important;
    visibility: visible !important;
    opacity: 1 !important;
}

.reservas-content .reserva-detail-card:hover {
    background: linear-gradient(135deg, #dcfce7 0%, #d1fae5 100%);
    border-color: #22c55e;
    box-shadow: 0 4px 12px rgba(34, 197, 94, 0.2);
    transform: translateY(-2px) scale(1.01);
}

.reservas-content .reserva-detail-card:hover::before {
    opacity: 1;
}

.reservas-content .detail-label,
.detail-label,
#reservaDetailBody .detail-label,
.reserva-detail-card .detail-label {
    display: block !important;
    font-size: 0.75rem !important;
    color: #059669 !important;
    margin-bottom: 10px !important;
    font-weight: 700 !important;
    text-transform: uppercase !important;
    letter-spacing: 1.2px !important;
    opacity: 0.9 !important;
    visibility: visible !important;
}

.reservas-content .detail-label i,
.detail-label i {
    display: none;
}

.reservas-content .detail-value,
.detail-value,
#reservaDetailBody .detail-value,
.reserva-detail-card .detail-value {
    display: block !important;
    font-size: 1.2rem !important;
    font-weight: 800 !important;
    color: #0f172a !important;
    line-height: 1.5 !important;
    letter-spacing: -0.02em !important;
    visibility: visible !important;
    opacity: 1 !important;
}

.reservas-content .reserva-item-list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.reservas-content .reserva-item {
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    margin-bottom: 12px;
    border: 1px solid #e5e7eb;
    transition: all 0.2s ease;
}

.reservas-content .reserva-item:hover {
    background: #f3f4f6;
    border-color: #d1d5db;
}

.reservas-content .reserva-item-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e5e7eb;
}

.reservas-content .reserva-item-name {
    font-weight: 600;
    font-size: 1rem;
    color: #111827;
    flex: 1;
    line-height: 1.4;
}

.reservas-content .reserva-item-meta {
    display: flex;
    gap: 20px;
    font-size: 0.9rem;
    color: #475569;
    flex-wrap: wrap;
    margin-top: 12px;
    padding: 12px 0;
    border-top: 1px solid #e2e8f0;
}

.reservas-content .reserva-item-meta span {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
    border-radius: 8px;
    font-weight: 600;
    border: 1px solid #cbd5e1;
    color: #334155;
    transition: all 0.2s ease;
}

.reservas-content .reserva-item-meta span:hover {
    background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
    transform: translateY(-1px);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.reservas-content .reserva-item-meta i {
    color: #64748b;
    font-size: 0.9rem;
}

.reservas-content .reserva-item-notes {
    margin-top: 12px;
    padding: 12px;
    background: #fef3c7;
    border-left: 3px solid #f59e0b;
    border-radius: 4px;
    font-size: 0.875rem;
    color: #92400e;
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-weight: 500;
}

.reservas-content .reserva-item-notes i {
    margin-top: 2px;
    flex-shrink: 0;
    font-size: 0.875rem;
}

.reservas-content .reserva-item-empty {
    text-align: center;
    padding: 32px 20px;
    color: #9ca3af;
    background: #f9fafb;
    border-radius: 8px;
    border: 1px dashed #d1d5db;
    font-weight: 500;
    font-size: 0.9375rem;
}

.reservas-content .reserva-personal-list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.reservas-content .reserva-personal-card {
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    margin-bottom: 12px;
    border: 1px solid #e5e7eb;
    transition: all 0.2s ease;
}

.reservas-content .reserva-personal-card:hover {
    background: #f3f4f6;
    border-color: #d1d5db;
}

.reservas-content .reserva-personal-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e5e7eb;
}

.reservas-content .reserva-personal-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;
}

.reservas-content .reserva-personal-name {
    font-weight: 600;
    font-size: 1rem;
    color: #111827;
    line-height: 1.4;
}

.reservas-content .reserva-personal-role {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
}

.reservas-content .reserva-personal-meta {
    display: flex;
    gap: 16px;
    font-size: 0.875rem;
    color: #6b7280;
    margin-top: 8px;
    flex-wrap: wrap;
}

.reservas-content .reserva-personal-meta span {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0;
    background: transparent;
    border-radius: 0;
    font-weight: 500;
    border: none;
    color: #6b7280;
}

.reservas-content .reserva-personal-meta i {
    color: #9ca3af;
    font-size: 0.875rem;
}

.reservas-content .reserva-personal-notes {
    margin-top: 12px;
    padding: 12px;
    background: #eff6ff;
    border-left: 3px solid #3b82f6;
    border-radius: 4px;
    font-size: 0.875rem;
    color: #1e40af;
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-weight: 500;
}

.reservas-content .reserva-personal-notes i {
    margin-top: 2px;
    flex-shrink: 0;
    font-size: 0.875rem;
}

.reservas-content .reserva-personal-empty {
    text-align: center;
    padding: 32px 20px;
    color: #9ca3af;
    background: #f9fafb;
    border-radius: 8px;
    border: 1px dashed #d1d5db;
    font-weight: 500;
    font-size: 0.9375rem;
}

.reservas-content .reserva-personal-empty i {
    font-size: 2.5rem;
    margin-bottom: 12px;
    display: block;
    color: #d1d5db;
    opacity: 0.6;
}

.reservas-content .reserva-observaciones {
    padding: 16px;
    background: #f9fafb;
    border-radius: 8px;
    color: #111827;
    border: 1px solid #e5e7eb;
    line-height: 1.6;
    font-size: 0.9375rem;
    font-weight: 400;
    min-height: 50px;
}

.reservas-content .reserva-observaciones:empty::before {
    content: 'Sin observaciones registradas.';
    color: #9ca3af;
    font-style: italic;
    font-weight: 400;
}

.reservas-content .reserva-modal-loader {
    text-align: center;
    padding: 60px 40px;
    color: #64748b;
}

.reservas-content .reserva-modal-loader i {
    font-size: 3rem;
    color: #dc2626;
    animation: spin 1s linear infinite;
    display: block;
    margin-bottom: 20px;
}

@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.reservas-content .reserva-modal-warning {
    padding: 16px 20px;
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    color: #92400e;
    border-radius: 10px;
    margin-bottom: 24px;
    border: 1px solid #fbbf24;
    box-shadow: 0 2px 4px rgba(251, 191, 36, 0.2);
    display: flex;
    align-items: center;
    gap: 12px;
    font-weight: 500;
}

.reservas-content .reserva-modal-warning i {
    font-size: 1.25rem;
    flex-shrink: 0;
}

.reservas-content .reserva-modal-error {
    padding: 16px 20px;
    background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
    color: #991b1b;
    border-radius: 10px;
    border: 1px solid #f87171;
    box-shadow: 0 2px 4px rgba(248, 113, 113, 0.2);
    display: flex;
    align-items: center;
    gap: 12px;
    font-weight: 500;
}

.reservas-content .reserva-modal-error i {
    font-size: 1.25rem;
    flex-shrink: 0;
}

/* ========== ESTILOS DE DETALLE DE RESERVA DESDE RESERVAS2/web.css ========== */

/* Modal de detalle de reserva */
.reservas-content .reserva-modal {
    max-width: 960px;
    width: 92%;
    background: var(--color-blanco);
    border-radius: 18px;
    overflow: hidden;
}

.reservas-content .reserva-modal .modal-header {
    padding: 22px 28px;
    background: #f4f6fb;
    border-bottom: 1px solid #e6e9f5;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.reservas-content .reserva-modal .modal-title {
    font-size: 1.35rem;
    font-weight: 600;
    color: var(--color-secundario);
}

.reservas-content .reserva-modal .close-modal {
    color: #6b7280;
    transition: color 0.2s ease, transform 0.2s ease;
}

.reservas-content .reserva-modal .close-modal:hover {
    color: var(--color-primario);
    transform: scale(1.05);
}

.reservas-content .reserva-modal-loader,
.reservas-content .reserva-modal-error {
    padding: 40px 24px;
    text-align: center;
    color: var(--color-secundario);
    font-weight: 500;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
}

.reservas-content .reserva-modal-loader i {
    font-size: 1.6rem;
}

.reservas-content .reserva-detail-status {
    display: flex;
    align-items: center;
    gap: 12px;
}

.reservas-content .reserva-detail-code {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    color: var(--color-gris-oscuro);
    font-weight: 600;
}

.reservas-content .reserva-detail-code i {
    color: var(--color-secundario);
}

.reservas-content .reserva-detail-timestamps {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 0.9rem;
    color: #6b7280;
}

.reservas-content .reserva-detail-timestamps i {
    color: var(--color-secundario);
    margin-right: 6px;
}

.reservas-content .reserva-item {
    border: 2px solid #d1fae5;
    border-radius: 14px;
    padding: 20px 24px;
    background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%);
    display: flex;
    flex-direction: column;
    gap: 14px;
    box-shadow: 0 4px 12px rgba(34, 197, 94, 0.1);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
}

.reservas-content .reserva-item::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
}

.reservas-content .reserva-item:hover {
    border-color: #86efac;
    box-shadow: 0 8px 20px rgba(34, 197, 94, 0.2);
    transform: translateY(-2px);
}

.reservas-content .reserva-item-header {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
}

.reservas-content .reserva-item-name {
    font-weight: 600;
    color: #1f2937;
    font-size: 1rem;
}

.reservas-content .reserva-item-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    font-size: 0.9rem;
    color: #4b5563;
}

.reservas-content .reserva-item-meta span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
}

.reservas-content .reserva-item-meta i {
    color: var(--color-secundario);
}

.reservas-content .reserva-item-notes {
    padding: 12px 14px;
    background: #f1f5f9;
    border-radius: 10px;
    color: #374151;
    font-size: 0.9rem;
    display: flex;
    align-items: flex-start;
    gap: 10px;
}

.reservas-content .reserva-item-notes i {
    margin-top: 3px;
    color: var(--color-secundario);
}

.reservas-content .reserva-item-empty {
    padding: 18px;
    border: 1px dashed #cbd5f5;
    border-radius: 12px;
    background: #f8faff;
    color: #4b5563;
    text-align: center;
}

.reservas-content .reserva-observaciones {
    padding: 20px 24px;
    border-radius: 12px;
    background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
    color: #1e293b;
    line-height: 1.8;
    border: 1.5px solid #bbf7d0;
    font-size: 0.95rem;
    box-shadow: inset 0 2px 4px rgba(34, 197, 94, 0.05);
}

.reservas-content .reserva-modal-warning {
    padding: 12px 16px;
    margin-bottom: 16px;
    border-radius: 12px;
    background: rgba(255, 193, 7, 0.18);
    color: #8a6d3b;
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 0.95rem;
}

.reservas-content .reserva-modal-warning i {
    color: #d39e00;
}

.reservas-content .chip-personal-completed {
    background: rgba(34, 197, 94, 0.12);
    color: #166534;
}

.reservas-content .chip-personal-cancelled {
    background: rgba(239, 68, 68, 0.12);
    color: #b91c1c;
}

.reservas-content .reserva-personal-card {
    border: 2px solid #d1fae5;
    border-radius: 14px;
    padding: 20px 24px;
    background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%);
    display: flex;
    flex-direction: column;
    gap: 14px;
    box-shadow: 0 4px 12px rgba(34, 197, 94, 0.1);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
}

.reservas-content .reserva-personal-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(180deg, #22c55e 0%, #10b981 100%);
}

.reservas-content .reserva-personal-card:hover {
    border-color: #86efac;
    box-shadow: 0 8px 20px rgba(34, 197, 94, 0.2);
    transform: translateY(-2px);
}

.reservas-content .reserva-personal-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    flex-wrap: wrap;
}

.reservas-content .reserva-personal-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.reservas-content .reserva-personal-name {
    font-weight: 600;
    color: #1f2937;
}

.reservas-content .reserva-personal-role {
    font-size: 0.9rem;
    color: #6b7280;
}

.reservas-content .reserva-personal-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 0.9rem;
    color: #475569;
    margin-top: 12px;
    padding: 12px 0;
    border-top: 1px solid #e2e8f0;
}

.reservas-content .reserva-personal-meta span {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 12px;
    background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
    border-radius: 8px;
    font-weight: 600;
    border: 1px solid #cbd5e1;
    color: #334155;
    transition: all 0.2s ease;
}

.reservas-content .reserva-personal-meta span:hover {
    background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
    transform: translateY(-1px);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.reservas-content .reserva-personal-meta i {
    color: #64748b;
    font-size: 0.9rem;
}

.reservas-content .reserva-personal-notes {
    padding: 12px 14px;
    border-radius: 10px;
    background: #f1f5f9;
    color: #374151;
    font-size: 0.9rem;
    display: flex;
    gap: 10px;
    align-items: flex-start;
}

.reservas-content .reserva-personal-notes i {
    margin-top: 2px;
    color: var(--color-secundario);
}

.reservas-content .reserva-personal-empty {
    padding: 18px;
    border: 1px dashed #cbd5f5;
    border-radius: 12px;
    background: #f8faff;
    color: #4b5563;
    display: flex;
    align-items: center;
    gap: 10px;
}

.reservas-content .reserva-personal-empty i {
    color: #94a3b8;
}

.reservas-content .chip {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 0.85rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
}

.reservas-content .chip:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.reservas-content .chip-servicio {
    background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(37, 99, 235, 0.2) 100%);
    color: #1e40af;
    border: 1.5px solid rgba(59, 130, 246, 0.3);
}

.reservas-content .chip-paquete {
    background: linear-gradient(135deg, rgba(139, 92, 246, 0.15) 0%, rgba(109, 40, 217, 0.2) 100%);
    color: #6d28d9;
    border: 1.5px solid rgba(139, 92, 246, 0.3);
}

/* Estilos directos para el modal sin depender de .reservas-content */
#reservaDetailModal .reserva-detail-section,
.reserva-detail-section,
#reservaDetailBody .reserva-detail-section {
    margin-bottom: 0 !important;
    padding: 28px 32px !important;
    background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%) !important;
    border: 2px solid #86efac !important;
    border-radius: 16px !important;
    box-shadow: 0 4px 16px rgba(34, 197, 94, 0.15), 0 0 0 1px rgba(34, 197, 94, 0.05) !important;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: visible !important;
    display: block !important;
    visibility: visible !important;
    opacity: 1 !important;
    min-height: auto !important;
}

#reservaDetailModal .reserva-detail-section > *,
.reserva-detail-section > *,
#reservaDetailBody .reserva-detail-section > * {
    position: relative !important;
    z-index: 1 !important;
    visibility: visible !important;
    opacity: 1 !important;
    display: block !important;
}

#reservaDetailModal .reserva-detail-grid,
.reserva-detail-grid,
#reservaDetailBody .reserva-detail-grid {
    display: grid !important;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)) !important;
    gap: 18px !important;
    visibility: visible !important;
    opacity: 1 !important;
    position: relative !important;
    z-index: 1 !important;
}

#reservaDetailModal .reserva-detail-card,
.reserva-detail-card,
#reservaDetailBody .reserva-detail-card {
    position: relative !important;
    z-index: 1 !important;
}

#reservaDetailModal .reserva-detail-card > *,
.reserva-detail-card > *,
#reservaDetailBody .reserva-detail-card > * {
    position: relative !important;
    z-index: 2 !important;
    visibility: visible !important;
    opacity: 1 !important;
}

#reservaDetailModal .reserva-detail-section::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #22c55e 0%, #10b981 50%, #059669 100%);
    opacity: 0.8;
}

#reservaDetailModal .reserva-detail-section h4 {
    margin: 0 0 24px 0 !important;
    font-size: 1.25rem !important;
    font-weight: 800 !important;
    color: #047857 !important;
    display: flex !important;
    align-items: center !important;
    gap: 12px !important;
    padding: 0 0 18px 0 !important;
    text-transform: uppercase !important;
    letter-spacing: 1px !important;
    border-bottom: 3px solid #bbf7d0 !important;
    position: relative;
}

#reservaDetailModal .reserva-detail-section h4::before {
    content: '';
    position: absolute;
    left: 0;
    bottom: -3px;
    width: 80px;
    height: 3px;
    background: linear-gradient(90deg, #22c55e 0%, #10b981 100%);
    border-radius: 2px;
    box-shadow: 0 2px 4px rgba(34, 197, 94, 0.3);
}

#reservaDetailModal .reserva-detail-card {
    padding: 20px 24px !important;
    background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%) !important;
    border-radius: 12px !important;
    border: 1.5px solid #bbf7d0 !important;
    box-shadow: 0 2px 8px rgba(34, 197, 94, 0.08) !important;
    position: relative;
    overflow: visible !important;
    display: block !important;
    visibility: visible !important;
    opacity: 1 !important;
}

#reservaDetailModal .reserva-detail-card > * {
    position: relative !important;
    z-index: 1 !important;
    visibility: visible !important;
    opacity: 1 !important;
}

#reservaDetailModal .detail-label {
    display: block !important;
    font-size: 0.75rem !important;
    color: #059669 !important;
    margin-bottom: 10px !important;
    font-weight: 700 !important;
    text-transform: uppercase !important;
    letter-spacing: 1.2px !important;
    opacity: 0.9;
}

#reservaDetailModal .detail-value {
    display: block !important;
    font-size: 1.2rem !important;
    font-weight: 800 !important;
    color: #0f172a !important;
    line-height: 1.5 !important;
    letter-spacing: -0.02em !important;
}

#reservaDetailModal .reserva-item {
    border: 2px solid #d1fae5 !important;
    border-radius: 14px !important;
    padding: 20px 24px !important;
    background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%) !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    box-shadow: 0 4px 12px rgba(34, 197, 94, 0.1) !important;
    position: relative;
    overflow: visible !important;
    visibility: visible !important;
    opacity: 1 !important;
}

#reservaDetailModal .reserva-item > * {
    position: relative !important;
    z-index: 1 !important;
    visibility: visible !important;
    opacity: 1 !important;
}

#reservaDetailModal .reserva-personal-card {
    border: 2px solid #d1fae5 !important;
    border-radius: 14px !important;
    padding: 20px 24px !important;
    background: linear-gradient(135deg, #ffffff 0%, #f8fffe 100%) !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 14px !important;
    box-shadow: 0 4px 12px rgba(34, 197, 94, 0.1) !important;
    position: relative;
    overflow: hidden;
}

#reservaDetailModal .reserva-observaciones {
    padding: 20px 24px !important;
    border-radius: 12px !important;
    background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%) !important;
    color: #1e293b !important;
    line-height: 1.8 !important;
    border: 1.5px solid #bbf7d0 !important;
    font-size: 0.95rem !important;
    box-shadow: inset 0 2px 4px rgba(34, 197, 94, 0.05) !important;
}

@media (max-width: 768px) {
    .reservas-content .reserva-modal-body,
    #reservaDetailModal .reserva-modal-body {
        padding: 22px !important;
        gap: 20px !important;
    }

    .reservas-content .reserva-detail-grid,
    #reservaDetailModal .reserva-detail-grid {
        grid-template-columns: 1fr !important;
    }

    .reservas-content .reserva-detail-timestamps,
    #reservaDetailModal .reserva-detail-timestamps {
        flex-direction: column;
        align-items: flex-start;
    }

    .reservas-content .reserva-item,
    #reservaDetailModal .reserva-item {
        padding: 14px !important;
    }
}
</style>