<?php
// Obtener token del usuario (desde parámetro GET, POST o cookie)
$tokenRaw = null;
if (isset($_POST['token'])) {
    $tokenRaw = $_POST['token'];
} elseif (isset($_GET['token'])) {
    $tokenRaw = $_GET['token'];
} elseif (isset($_COOKIE['userToken'])) {
    $tokenRaw = $_COOKIE['userToken'];
}

// Si no hay token, usar uno por defecto (para compatibilidad con acceso directo)
if (!$tokenRaw) {
    $tokenRaw = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjbEBnbWFpbC5jb20iLCJ1c2VySWQiOjE1LCJlbXByZXNhSWQiOjEsInJvbGVzIjpbIlJPTEVfU1VQRVJBRE1JTklTVFJBRE9SIl0sImlzcyI6InNpc3RlbWEtdHVyaXN0aWNvLWJhY2tlbmQiLCJpYXQiOjE3NjQzMzk3MTYsImV4cCI6MTc2NDQyNjExNn0.H-geg1tf1JJI5i7aagghYZJ9NWtL7DQ2Cutz1uB3kqc';
}

// Preparar token con Bearer para el header
$token = 'Bearer ' . $tokenRaw;

$baseUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/reservas';

// Variables
$error = null;
$success = null;

// Obtener idEmpresa si viene como parámetro (GET o POST)
$idEmpresa = isset($_POST['empresaId']) ? intval($_POST['empresaId']) : (isset($_GET['idEmpresa']) ? intval($_GET['idEmpresa']) : null);

// Obtener idSucursal si viene como parámetro (GET o POST)
$idSucursal = isset($_POST['idSucursal']) ? intval($_POST['idSucursal']) : (isset($_GET['idSucursal']) ? intval($_GET['idSucursal']) : null);

// Verificar si es gerente (los gerentes no tienen sucursal específica)
$esGerente = false;
if (isset($_GET['rol'])) {
    $rol = strtolower(trim($_GET['rol']));
    $esGerente = ($rol === 'gerente');
} elseif (isset($_GET['rolId'])) {
    $rolId = intval($_GET['rolId']);
    $esGerente = ($rolId === 3 || $rolId === 4);
}

// Si es gerente, no usar idSucursal
if ($esGerente) {
    $idSucursal = null;
}

// Procesar creación si se envió el formulario
if ($_SERVER['REQUEST_METHOD'] === 'POST' && (isset($_POST['crear']) || isset($_GET['ajax']))) {
    try {
        // Validar campos requeridos
        if (empty($_POST['clienteId'])) {
            throw new Exception('El cliente es requerido');
        }
        if (empty($_POST['fechaServicio'])) {
            throw new Exception('La fecha de servicio es requerida');
        }
        if (empty($_POST['numeroPersonas']) || intval($_POST['numeroPersonas']) <= 0) {
            throw new Exception('El número de personas debe ser mayor a 0');
        }
        
        // Preparar datos para la API
        $reservaData = [
            'empresaId' => $idEmpresa ?: null,
            'idSucursal' => $idSucursal ?: null,
            'clienteId' => intval($_POST['clienteId']),
            'promocionId' => !empty($_POST['promocionId']) ? intval($_POST['promocionId']) : null,
            'codigoReserva' => $_POST['codigoReserva'] ?? '',
            'fechaServicio' => $_POST['fechaServicio'],
            'fechaReserva' => $_POST['fechaReserva'] ?? date('Y-m-d'),
            'numeroPersonas' => intval($_POST['numeroPersonas']),
            'descuentoAplicado' => !empty($_POST['descuentoAplicado']) ? floatval($_POST['descuentoAplicado']) : 0.00,
            'observaciones' => $_POST['observaciones'] ?? '',
            'items' => [],
            'asignaciones' => []
        ];
        
        // Procesar items si existen
        $idServicio = null;
        $idPaquete = null;
        if (isset($_POST['items']) && is_array($_POST['items'])) {
            foreach ($_POST['items'] as $item) {
                if (!empty($item['tipoItem'])) {
                    $itemData = [
                        'tipoItem' => $item['tipoItem'],
                        'servicioId' => null,
                        'paqueteId' => null,
                        'cantidad' => intval($item['cantidad'] ?? 1),
                        'precioUnitario' => floatval($item['precioUnitario'] ?? 0),
                        'precioTotal' => floatval($item['precioTotal'] ?? 0),
                        'descripcionExtra' => $item['descripcionExtra'] ?? ''
                    ];
                    
                    if (!empty($item['servicioId'])) {
                        $itemData['servicioId'] = intval($item['servicioId']);
                        // Asignar el primer servicio encontrado a la reserva
                        if ($idServicio === null) {
                            $idServicio = intval($item['servicioId']);
                        }
                    }
                    
                    if (!empty($item['paqueteId'])) {
                        $itemData['paqueteId'] = intval($item['paqueteId']);
                        // Asignar el primer paquete encontrado a la reserva
                        if ($idPaquete === null) {
                            $idPaquete = intval($item['paqueteId']);
                        }
                    }
                    
                    // Solo agregar el item si tiene servicioId o paqueteId
                    if (!empty($item['servicioId']) || !empty($item['paqueteId'])) {
                        $reservaData['items'][] = $itemData;
                    }
                }
            }
        }
        
        // Agregar idServicio e idPaquete directamente a la reserva
        if ($idServicio !== null) {
            $reservaData['idServicio'] = $idServicio;
        }
        if ($idPaquete !== null) {
            $reservaData['idPaquete'] = $idPaquete;
        }
        
        // Procesar asignaciones si existen
        if (isset($_POST['asignaciones']) && is_array($_POST['asignaciones'])) {
            foreach ($_POST['asignaciones'] as $asignacion) {
                if (!empty($asignacion['idPersonal'])) {
                    $reservaData['asignaciones'][] = [
                        'idPersonal' => intval($asignacion['idPersonal']),
                        'fechaAsignacion' => $asignacion['fechaAsignacion'] ?? $reservaData['fechaServicio'],
                        'observaciones' => $asignacion['observaciones'] ?? ''
                    ];
                }
            }
        }
        
        // Realizar petición a la API
        $curl = curl_init();
        curl_setopt_array($curl, [
            CURLOPT_URL => $baseUrl,
  CURLOPT_RETURNTRANSFER => true,
  CURLOPT_ENCODING => '',
  CURLOPT_MAXREDIRS => 10,
            CURLOPT_TIMEOUT => 30,
  CURLOPT_FOLLOWLOCATION => true,
  CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
  CURLOPT_CUSTOMREQUEST => 'POST',
            CURLOPT_POSTFIELDS => json_encode($reservaData),
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json',
                'Authorization: ' . $token
            ],
        ]);
        
        $response = curl_exec($curl);
        $httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
        $curlError = curl_error($curl);
        curl_close($curl);
        
        if ($curlError) {
            throw new Exception('Error de conexión: ' . $curlError);
        }
        
        if ($httpCode !== 200 && $httpCode !== 201) {
            $errorData = json_decode($response, true);
            $errorMessage = 'Error HTTP: ' . $httpCode;
            if (isset($errorData['message'])) {
                $errorMessage .= ' - ' . $errorData['message'];
            } elseif (isset($errorData['error'])) {
                $errorMessage .= ' - ' . $errorData['error'];
            } elseif (!empty($response)) {
                $errorMessage .= ' - ' . substr($response, 0, 200);
            }
            throw new Exception($errorMessage);
        }
        
        $data = json_decode($response, true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception('Error al decodificar respuesta: ' . json_last_error_msg());
        }
        
        $success = 'Reserva creada exitosamente';
        
        // Si es AJAX, retornar JSON
        if (isset($_GET['ajax']) && $_GET['ajax'] === '1') {
            header('Content-Type: application/json');
            echo json_encode([
                'success' => true,
                'message' => $success,
                'data' => $data
            ]);
            exit;
        }
        
    } catch (Exception $e) {
        $error = $e->getMessage();
        
        // Si es AJAX, retornar JSON con error
        if (isset($_GET['ajax']) && $_GET['ajax'] === '1') {
            header('Content-Type: application/json');
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'message' => $error
            ]);
            exit;
        }
    }
}

// Si es una petición AJAX y no hay POST, mostrar solo el formulario
if (isset($_GET['ajax']) && $_GET['ajax'] === '1' && $_SERVER['REQUEST_METHOD'] !== 'POST') {
    // Continuar para mostrar el formulario
}
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nueva Reserva</title>
    <?php if (isset($_GET['ajax']) && $_GET['ajax'] === '1'): ?>
        <!-- Cuando se carga dinámicamente desde admin.php, los estilos ya están cargados -->
    <?php else: ?>
        <link rel="stylesheet" href="../web.css">
        <link rel="stylesheet" href="alertas.css">
    <?php endif; ?>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
</head>
<body>
    <div class="content-header" style="margin-bottom: 20px;">
        <h2 class="section-title">Nueva Reserva</h2>
        <button class="btn btn-secondary" onclick="volverAReservas()">
            <i class="fas fa-arrow-left"></i> Volver
        </button>
    </div>

    <?php if ($error): ?>
        <div class="alerta alerta-error" style="margin-bottom: 20px;">
            <i class="fas fa-exclamation-circle"></i>
            <span><?php echo htmlspecialchars($error); ?></span>
        </div>
    <?php endif; ?>

    <?php if ($success): ?>
        <div class="alerta alerta-success" style="margin-bottom: 20px;">
            <i class="fas fa-check-circle"></i>
            <span><?php echo htmlspecialchars($success); ?></span>
        </div>
    <?php endif; ?>

    <form method="POST" action="javascript:void(0);" class="form-container" id="formNuevaReserva" onsubmit="event.preventDefault(); event.stopPropagation(); return false;">
        <input type="hidden" name="ajax" value="1">
        <input type="hidden" name="empresaId" id="empresaId" value="<?php echo htmlspecialchars($idEmpresa ?? ''); ?>">
        <input type="hidden" name="idSucursal" id="idSucursal" value="<?php echo htmlspecialchars($idSucursal ?? ''); ?>">
        
        <div class="form-grid">
            <!-- Cliente -->
            <div class="form-group full-width">
                <label class="form-label" for="clienteId">Cliente <span class="required">*</span></label>
                <select id="clienteId" name="clienteId" class="form-input" required>
                    <option value="">Seleccione un cliente</option>
                </select>
                <small class="form-help">Seleccione el cliente para la reserva</small>
            </div>

            <!-- Fecha de Servicio -->
            <div class="form-group">
                <label class="form-label" for="fechaServicio">Fecha de Servicio <span class="required">*</span></label>
                <input 
                    type="date" 
                    id="fechaServicio" 
                    name="fechaServicio" 
                    class="form-input" 
                    value="<?php echo htmlspecialchars($_POST['fechaServicio'] ?? ''); ?>"
                    required
                >
            </div>

            <!-- Fecha de Reserva -->
            <div class="form-group">
                <label class="form-label" for="fechaReserva">Fecha de Reserva</label>
                <input 
                    type="date" 
                    id="fechaReserva" 
                    name="fechaReserva" 
                    class="form-input" 
                    value="<?php echo htmlspecialchars($_POST['fechaReserva'] ?? date('Y-m-d')); ?>"
                >
            </div>

            <!-- Número de Personas -->
            <div class="form-group">
                <label class="form-label" for="numeroPersonas">Número de Personas <span class="required">*</span></label>
                <input 
                    type="number" 
                    id="numeroPersonas" 
                    name="numeroPersonas" 
                    class="form-input" 
                    min="1"
                    value="<?php echo htmlspecialchars($_POST['numeroPersonas'] ?? '1'); ?>"
                    required
                >
            </div>

            <!-- Código de Reserva -->
            <div class="form-group">
                <label class="form-label" for="codigoReserva">Código de Reserva</label>
                <input 
                    type="text" 
                    id="codigoReserva" 
                    name="codigoReserva" 
                    class="form-input" 
                    placeholder="Se generará automáticamente según la sucursal"
                    value="<?php echo htmlspecialchars($_POST['codigoReserva'] ?? ''); ?>"
                >
                <small class="form-help">Se generará automáticamente basado en la empresa y sucursal (formato: RES-{ID_EMPRESA}-{ID_SUCURSAL}-{NUMERO})</small>
            </div>

            <!-- Descuento Aplicado -->
            <div class="form-group">
                <label class="form-label" for="descuentoAplicado">Descuento Aplicado</label>
                <input 
                    type="number" 
                    id="descuentoAplicado" 
                    name="descuentoAplicado" 
                    class="form-input" 
                    step="0.01"
                    min="0"
                    value="<?php echo htmlspecialchars($_POST['descuentoAplicado'] ?? '0.00'); ?>"
                >
            </div>

            <!-- Observaciones -->
            <div class="form-group full-width">
                <label class="form-label" for="observaciones">Observaciones</label>
                <textarea 
                    id="observaciones" 
                    name="observaciones" 
                    class="form-input" 
                    rows="3"
                    placeholder="Observaciones adicionales sobre la reserva..."
                ><?php echo htmlspecialchars($_POST['observaciones'] ?? ''); ?></textarea>
            </div>
        </div>

        <!-- Sección de Items (Servicios/Paquetes) -->
        <section class="form-section" style="margin-top: 30px; padding: 20px; background: #f9fafb; border-radius: 8px;">
            <div class="section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <h3 class="section-subtitle" style="margin: 0; font-size: 18px; font-weight: 600;">
                    <i class="fas fa-layer-group"></i> Ítems de la Reserva
                </h3>
                <div style="display: flex; gap: 10px;">
                    <button type="button" class="btn btn-outline" id="btnAgregarServicio" style="padding: 8px 16px;">
                        <i class="fas fa-plus-circle"></i> Agregar Servicio
                    </button>
                    <button type="button" class="btn btn-outline" id="btnAgregarPaquete" style="padding: 8px 16px;">
                        <i class="fas fa-box"></i> Agregar Paquete
                    </button>
                </div>
            </div>

            <div id="itemsContainer" style="margin-top: 20px;">
                <div id="itemsEmptyState" style="text-align: center; padding: 40px; color: #6b7280; border: 2px dashed #d1d5db; border-radius: 8px;">
                    <i class="fas fa-layer-group" style="font-size: 3rem; margin-bottom: 10px; opacity: 0.5;"></i>
                    <p style="margin: 0;">No hay ítems agregados. Haz clic en "Agregar Servicio" o "Agregar Paquete" para comenzar.</p>
                </div>
                <table id="itemsTable" style="display: none; width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden;">
                    <thead style="background: #f3f4f6;">
                        <tr>
                            <th style="padding: 12px; text-align: left; font-weight: 600; width: 120px;">Tipo</th>
                            <th style="padding: 12px; text-align: left; font-weight: 600;">Detalle</th>
                            <th style="padding: 12px; text-align: left; font-weight: 600; width: 100px;">Cantidad</th>
                            <th style="padding: 12px; text-align: left; font-weight: 600; width: 130px;">Precio Unit.</th>
                            <th style="padding: 12px; text-align: left; font-weight: 600; width: 130px;">Subtotal</th>
                            <th style="padding: 12px; text-align: left; font-weight: 600;">Notas</th>
                            <th style="padding: 12px; text-align: center; font-weight: 600; width: 80px;">Acciones</th>
                        </tr>
                    </thead>
                    <tbody id="itemsTableBody">
                    </tbody>
                </table>
            </div>

            <div style="margin-top: 20px; padding: 15px; background: white; border-radius: 8px; display: flex; justify-content: space-between; align-items: center; border: 1px solid #e5e7eb;">
                <div>
                    <span style="color: #6b7280; font-size: 14px;">Subtotal:</span>
                    <strong style="font-size: 18px; color: #111827; margin-left: 10px;">S/ <span id="subtotalDisplay">0.00</span></strong>
                </div>
                <div>
                    <span style="color: #6b7280; font-size: 14px;">Descuento:</span>
                    <strong style="font-size: 18px; color: #dc2626; margin-left: 10px;">S/ <span id="descuentoDisplay">0.00</span></strong>
                </div>
                <div>
                    <span style="color: #6b7280; font-size: 14px;">Total:</span>
                    <strong style="font-size: 20px; color: #059669; margin-left: 10px;">S/ <span id="totalDisplay">0.00</span></strong>
                </div>
            </div>
        </section>

        <!-- Sección de Asignaciones (Personal) -->
        <section class="form-section" style="margin-top: 30px; padding: 20px; background: #f9fafb; border-radius: 8px;">
            <div class="section-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <h3 class="section-subtitle" style="margin: 0; font-size: 18px; font-weight: 600;">
                    <i class="fas fa-user-friends"></i> Personal Asignado
                </h3>
                <button type="button" class="btn btn-outline" id="btnRecargarPersonal" style="padding: 8px 16px;">
                    <i class="fas fa-sync-alt"></i> Actualizar
                </button>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                <!-- Personal Disponible -->
                <div style="background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px;">
                    <h4 style="margin: 0 0 15px 0; font-size: 16px; font-weight: 600;">Disponibles</h4>
                    <div style="margin-bottom: 15px;">
                        <input 
                            type="search" 
                            id="personalSearchInput" 
                            class="form-input" 
                            placeholder="Buscar por nombre o cargo..."
                            style="padding-left: 40px;"
                        >
                        <i class="fas fa-search" style="position: absolute; margin-left: -30px; margin-top: 12px; color: #9ca3af;"></i>
                    </div>
                    <div id="personalCatalogList" style="max-height: 300px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px;">
                        <div style="text-align: center; padding: 20px; color: #6b7280;">
                            <i class="fas fa-spinner fa-spin"></i> Cargando personal...
                        </div>
                    </div>
                </div>

                <!-- Personal Seleccionado -->
                <div style="background: white; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                        <h4 style="margin: 0; font-size: 16px; font-weight: 600;">Seleccionados</h4>
                        <span id="personalSelectedCounter" style="font-size: 14px; color: #6b7280;">0</span>
                    </div>
                    <div id="personalSelectedList" style="max-height: 300px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px;">
                        <div id="personalSelectedEmpty" style="text-align: center; padding: 40px; color: #6b7280; border: 2px dashed #d1d5db; border-radius: 8px;">
                            <i class="fas fa-user-plus" style="font-size: 2rem; margin-bottom: 10px; opacity: 0.5;"></i>
                            <p style="margin: 0;">Aún no hay personal asignado</p>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- Botones -->
        <div class="form-actions" style="margin-top: 30px; display: flex; gap: 10px; justify-content: flex-end;">
            <button type="button" class="btn btn-secondary" onclick="volverAReservas()">
                <i class="fas fa-times"></i> Cancelar
            </button>
            <button type="submit" class="btn btn-primary" id="btnCrearReserva">
                <i class="fas fa-save"></i> Crear Reserva
            </button>
        </div>
    </form>

    <script>
        // Evitar redeclaración de variables si el script se ejecuta múltiples veces
        (function() {
            // Obtener token y datos del usuario
            let userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
            let userData = null;
            if (userDataStr) {
                try {
                    userData = JSON.parse(userDataStr);
                } catch (e) {
                    console.error('Error al parsear userData:', e);
                }
            }

            const token = userData?.token || '<?php echo $tokenRaw; ?>';
            const idEmpresa = userData?.empresaId || document.getElementById('empresaId')?.value;
            const idSucursal = userData?.sucursalId || userData?.idSucursal || document.getElementById('idSucursal')?.value;

            // Estado para items y asignaciones
            let items = [];
            let asignaciones = [];
            let servicios = [];
            let paquetes = [];
            let personal = [];
            let personalSeleccionado = new Set();
            let sucursalInfo = null; // Información de la sucursal

            // Cargar servicios y paquetes
            async function cargarServiciosYPaquetes() {
            try {
                // Cargar servicios
                try {
                    const serviciosUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/servicios${idEmpresa ? '?empresaId=' + idEmpresa : ''}`;
                    const serviciosResponse = await fetch(serviciosUrl, {
                        headers: { 'Authorization': 'Bearer ' + token }
                    });
                    if (serviciosResponse.ok) {
                        const serviciosData = await serviciosResponse.json();
                        servicios = serviciosData.data || serviciosData.content || serviciosData.items || serviciosData.results || [];
                        console.log('Servicios cargados:', servicios.length);
                    } else {
                        // Solo mostrar warning si no es un error 500 (problema del servidor)
                        if (serviciosResponse.status !== 500) {
                            console.warn('Error al cargar servicios:', serviciosResponse.status, serviciosResponse.statusText);
                        }
                        servicios = [];
                    }
                } catch (serviciosError) {
                    // Manejar errores de red (DNS, conexión, etc.)
                    if (serviciosError.name === 'TypeError' && serviciosError.message.includes('Failed to fetch')) {
                        console.warn('No se pudo conectar al servidor para cargar servicios. Verifica tu conexión a internet.');
                    } else {
                        console.warn('Error de red al cargar servicios:', serviciosError.message);
                    }
                    servicios = [];
                }

                // Cargar paquetes
                try {
                    const paquetesUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/paquetes${idEmpresa ? '?empresaId=' + idEmpresa : ''}`;
                    const paquetesResponse = await fetch(paquetesUrl, {
                        headers: { 'Authorization': 'Bearer ' + token }
                    });
                    if (paquetesResponse.ok) {
                        const paquetesData = await paquetesResponse.json();
                        paquetes = paquetesData.data || paquetesData.content || paquetesData.items || paquetesData.results || [];
                        console.log('Paquetes cargados:', paquetes.length);
                    } else {
                        // Solo mostrar warning si no es un error 500 (problema del servidor)
                        if (paquetesResponse.status !== 500) {
                            console.warn('Error al cargar paquetes:', paquetesResponse.status, paquetesResponse.statusText);
                        } else {
                            // Error 500: problema del servidor, no mostrar error pero registrar silenciosamente
                            console.info('El servidor no pudo procesar la solicitud de paquetes (error 500). Continuando sin paquetes.');
                        }
                        paquetes = [];
                    }
                } catch (paquetesError) {
                    // Manejar errores de red (DNS, conexión, etc.)
                    if (paquetesError.name === 'TypeError' && paquetesError.message.includes('Failed to fetch')) {
                        console.warn('No se pudo conectar al servidor para cargar paquetes. Verifica tu conexión a internet.');
                    } else {
                        console.warn('Error de red al cargar paquetes:', paquetesError.message);
                    }
                    paquetes = [];
                }
            } catch (error) {
                console.error('Error general al cargar servicios y paquetes:', error);
                servicios = servicios || [];
                paquetes = paquetes || [];
            }
        }

            // Cargar información de la sucursal
            async function cargarSucursalInfo() {
                if (!idSucursal) {
                    console.log('No hay idSucursal, no se cargará información de sucursal');
                    // Aún así, intentar generar código sin sucursal
                    generarCodigoReserva();
                    return;
                }
                
                try {
                    const sucursalUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/sucursales/${idSucursal}`;
                    const response = await fetch(sucursalUrl, {
                        headers: { 'Authorization': 'Bearer ' + token }
                    });
                    
                    if (response.ok) {
                        const data = await response.json();
                        sucursalInfo = data.data || data;
                        console.log('Información de sucursal cargada:', sucursalInfo);
                        
                        // Generar código de reserva automáticamente
                        await generarCodigoReserva();
                    } else {
                        console.warn('Error al cargar información de sucursal:', response.status, response.statusText);
                        // Intentar generar código sin información de sucursal
                        await generarCodigoReserva();
                    }
                } catch (error) {
                    // Manejar errores de red (DNS, conexión, etc.)
                    if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
                        console.warn('No se pudo conectar al servidor para cargar información de sucursal. Verifica tu conexión a internet.');
                    } else {
                        console.error('Error al cargar información de sucursal:', error);
                    }
                    // Intentar generar código sin información de sucursal
                    await generarCodigoReserva();
                }
            }

            // Generar código de reserva basado en la empresa y sucursal
            async function generarCodigoReserva() {
                const codigoInput = document.getElementById('codigoReserva');
                if (!codigoInput) return;
                
                // Si el usuario ya ingresó un código, no lo sobrescribimos
                if (codigoInput.value && codigoInput.value.trim() !== '') {
                    return;
                }
                
                // Obtener código de empresa
                let codigoEmpresa = '';
                if (idEmpresa) {
                    codigoEmpresa = String(idEmpresa);
                } else {
                    codigoEmpresa = '00'; // Valor por defecto si no hay empresa
                }
                
                // Prioridad 1: Usar idSucursal directamente
                let codigoSucursal = '';
                if (idSucursal) {
                    codigoSucursal = String(idSucursal);
                } else if (sucursalInfo && (sucursalInfo.idSucursal || sucursalInfo.id)) {
                    codigoSucursal = String(sucursalInfo.idSucursal || sucursalInfo.id);
                } else if (sucursalInfo) {
                    // Prioridad 2: Generar código desde el nombre de la sucursal
                    const nombreSucursal = sucursalInfo.nombreSucursal || sucursalInfo.nombre || '';
                    
                    if (nombreSucursal) {
                        // Generar código de sucursal desde las iniciales o primeras letras
                        const palabras = nombreSucursal.trim().split(/\s+/);
                        if (palabras.length > 1) {
                            // Si tiene múltiples palabras, tomar la primera letra de cada una
                            codigoSucursal = palabras.map(p => p.charAt(0).toUpperCase()).join('').substring(0, 3);
                        } else {
                            // Si es una sola palabra, tomar las primeras 3 letras
                            codigoSucursal = nombreSucursal.substring(0, 3).toUpperCase();
                        }
                    }
                }
                
                // Si aún no hay código de sucursal, usar un valor por defecto
                if (!codigoSucursal) {
                    codigoSucursal = '00';
                }
                
                // Intentar obtener el siguiente número de secuencia desde la API
                let numeroSecuencia = '00001';
                try {
                    // Construir URL con filtros de empresa y sucursal
                    const params = new URLSearchParams();
                    if (idEmpresa) {
                        params.append('empresaId', idEmpresa);
                    }
                    if (idSucursal) {
                        params.append('idSucursal', idSucursal);
                    }
                    params.append('size', '100');
                    
                    const reservasUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/reservas?${params.toString()}`;
                    const response = await fetch(reservasUrl, {
                        headers: { 'Authorization': 'Bearer ' + token }
                    });
                    
                    if (response.ok) {
                        const data = await response.json();
                        const reservas = data.data || data.content || data.items || data.results || [];
                        
                        if (reservas.length > 0) {
                            // Buscar el último código de reserva de esta empresa y sucursal
                            // Formato nuevo: RES-{ID_EMPRESA}-{ID_SUCURSAL}-{NUMERO}
                            // También soportar el formato antiguo con año: RES-{ID_EMPRESA}-{ID_SUCURSAL}-{AÑO}-{NUMERO}
                            const patronSinAño = new RegExp(`RES-${codigoEmpresa}-${codigoSucursal}-(\\d{5})$`);
                            const patronConAño = new RegExp(`RES-${codigoEmpresa}-${codigoSucursal}-\\d{4}-(\\d{5})$`);
                            let maxNumero = 0;
                            
                            reservas.forEach(reserva => {
                                const codigo = reserva.codigoReserva || reserva.codigo || '';
                                // Intentar primero el formato sin año
                                let match = codigo.match(patronSinAño);
                                if (!match) {
                                    // Si no coincide, intentar el formato con año (para compatibilidad)
                                    match = codigo.match(patronConAño);
                                }
                                if (match) {
                                    const numero = parseInt(match[1]);
                                    if (numero > maxNumero) {
                                        maxNumero = numero;
                                    }
                                }
                            });
                            
                            if (maxNumero > 0) {
                                numeroSecuencia = String(maxNumero + 1).padStart(5, '0');
                            }
                        }
                    }
                } catch (error) {
                    // Manejar errores de red (DNS, conexión, etc.) de forma silenciosa
                    if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
                        console.info('No se pudo conectar al servidor para obtener el siguiente número de secuencia. Usando 00001.');
                    } else {
                        console.warn('No se pudo obtener el siguiente número de secuencia, usando 00001:', error.message);
                    }
                }
                
                // Generar código: RES-{ID_EMPRESA}-{ID_SUCURSAL}-{NUMERO}
                // Ejemplo: RES-14-11-00001 (Empresa 14, Sucursal 11, Número 00001)
                const codigoGenerado = `RES-${codigoEmpresa}-${codigoSucursal}-${numeroSecuencia}`;
                
                codigoInput.value = codigoGenerado;
                console.log('Código de reserva generado:', codigoGenerado);
            }

            // Cargar personal
            async function cargarPersonal() {
            try {
                const personalUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/personal${idEmpresa ? '?empresaId=' + idEmpresa : ''}`;
                const response = await fetch(personalUrl, {
                    headers: { 'Authorization': 'Bearer ' + token }
                });
                if (response.ok) {
                    const data = await response.json();
                    personal = data.data || data.content || data.items || data.results || [];
                    renderPersonalCatalog();
                } else {
                    console.warn('Error al cargar personal:', response.status, response.statusText);
                    personal = [];
                    const container = document.getElementById('personalCatalogList');
                    if (container) {
                        container.innerHTML = '<div style="padding: 20px; color: #dc3545;">Error al cargar personal. Intente nuevamente.</div>';
                    }
                }
            } catch (error) {
                // Manejar errores de red (DNS, conexión, etc.)
                if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
                    console.warn('No se pudo conectar al servidor para cargar personal. Verifica tu conexión a internet.');
                } else {
                    console.error('Error al cargar personal:', error);
                }
                personal = [];
                const container = document.getElementById('personalCatalogList');
                if (container) {
                    container.innerHTML = '<div style="padding: 20px; color: #dc3545;">Error al cargar personal. Verifica tu conexión a internet.</div>';
                }
            }
        }

            // Renderizar catálogo de personal
            function renderPersonalCatalog(searchTerm = '') {
            const container = document.getElementById('personalCatalogList');
            const term = searchTerm.toLowerCase();
            const filtered = personal.filter(p => {
                const nombre = (p.nombre || p.nombres || '').toLowerCase();
                const apellido = (p.apellido || p.apellidos || '').toLowerCase();
                const cargo = (p.cargo || p.puesto || '').toLowerCase();
                return !personalSeleccionado.has(String(p.id || p.idPersonal || p.idEmpleado)) &&
                       (nombre.includes(term) || apellido.includes(term) || cargo.includes(term));
            });

            if (filtered.length === 0) {
                container.innerHTML = '<div style="text-align: center; padding: 20px; color: #6b7280;">No hay personal disponible</div>';
                return;
            }

            container.innerHTML = filtered.map(p => {
                const personalId = String(p.id || p.idPersonal || p.idEmpleado);
                const nombre = `${p.nombre || p.nombres || ''} ${p.apellido || p.apellidos || ''}`.trim() || 'Sin nombre';
                const cargo = p.cargo || p.puesto || 'Sin cargo';
                return `
                    <div class="personal-item" data-id="${personalId}" style="padding: 12px; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; transition: all 0.2s;" 
                         onmouseover="this.style.background='#f3f4f6'" 
                         onmouseout="this.style.background='white'"
                         onclick="agregarPersonal(${personalId})">
                        <div style="font-weight: 600; margin-bottom: 4px;">${nombre}</div>
                        <div style="font-size: 12px; color: #6b7280;">${cargo}</div>
                    </div>
                `;
            }).join('');
        }

            // Agregar personal seleccionado
            function agregarPersonal(personalId) {
            if (personalSeleccionado.has(String(personalId))) return;
            personalSeleccionado.add(String(personalId));
            renderPersonalCatalog(document.getElementById('personalSearchInput')?.value || '');
            renderPersonalSeleccionado();
        }

            // Remover personal seleccionado
            function removerPersonal(personalId) {
            personalSeleccionado.delete(String(personalId));
            renderPersonalCatalog(document.getElementById('personalSearchInput')?.value || '');
            renderPersonalSeleccionado();
        }

            // Renderizar personal seleccionado
            function renderPersonalSeleccionado() {
            const container = document.getElementById('personalSelectedList');
            const empty = document.getElementById('personalSelectedEmpty');
            const counter = document.getElementById('personalSelectedCounter');
            
            // Validar que los elementos existan
            if (!container) {
                console.warn('No se encontró el contenedor de personal seleccionado');
                return;
            }
            
            const seleccionados = personal.filter(p => 
                personalSeleccionado.has(String(p.id || p.idPersonal || p.idEmpleado))
            );

            if (counter) {
                counter.textContent = seleccionados.length;
            }

            if (seleccionados.length === 0) {
                container.innerHTML = '';
                if (empty) {
                    empty.style.display = 'block';
                }
                return;
            }

            if (empty) {
                empty.style.display = 'none';
            }
            
            container.innerHTML = seleccionados.map(p => {
                const personalId = String(p.id || p.idPersonal || p.idEmpleado);
                const nombre = `${p.nombre || p.nombres || ''} ${p.apellido || p.apellidos || ''}`.trim() || 'Sin nombre';
                const cargo = p.cargo || p.puesto || 'Sin cargo';
                return `
                    <div class="personal-selected-item" data-id="${personalId}" style="padding: 12px; border: 1px solid #10b981; background: #ecfdf5; border-radius: 8px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 600; margin-bottom: 4px;">${nombre}</div>
                            <div style="font-size: 12px; color: #6b7280;">${cargo}</div>
                        </div>
                        <button type="button" onclick="removerPersonal(${personalId})" style="background: #dc2626; color: white; border: none; border-radius: 4px; padding: 6px 12px; cursor: pointer;">
                            <i class="fas fa-times"></i>
                        </button>
                    </div>
                `;
            }).join('');
        }

            // Agregar item (servicio o paquete)
            function agregarItem(tipo) {
            const item = {
                tipoItem: tipo,
                servicioId: null,
                paqueteId: null,
                cantidad: 1,
                precioUnitario: 0,
                precioTotal: 0,
                descripcionExtra: ''
            };
            items.push(item);
            renderItems();
        }

            // Eliminar item
            function eliminarItem(index) {
            items.splice(index, 1);
            renderItems();
        }

            // Renderizar items
            function renderItems() {
            const tbody = document.getElementById('itemsTableBody');
            const table = document.getElementById('itemsTable');
            const empty = document.getElementById('itemsEmptyState');

            if (items.length === 0) {
                table.style.display = 'none';
                empty.style.display = 'block';
                actualizarTotales();
                return;
            }

            table.style.display = 'table';
            empty.style.display = 'none';

            tbody.innerHTML = items.map((item, index) => {
                const opciones = item.tipoItem === 'SERVICIO' ? servicios : paquetes;
                const opcionesHtml = opciones.map(op => {
                    const opId = op.idServicio || op.idPaquete || op.id;
                    const nombre = op.nombreServicio || op.nombrePaquete || op.nombre || 'Sin nombre';
                    const precio = op.precioBase || op.precio || 0;
                    const selected = (item.tipoItem === 'SERVICIO' && item.servicioId == opId) ||
                                   (item.tipoItem === 'PAQUETE' && item.paqueteId == opId) ? 'selected' : '';
                    return `<option value="${opId}" data-precio="${precio}" ${selected}>${nombre} - S/ ${precio.toFixed(2)}</option>`;
                }).join('');

                const subtotal = item.precioTotal || (item.precioUnitario * item.cantidad);
                return `
                    <tr>
                        <td style="padding: 12px;">
                            <select class="form-input" onchange="actualizarItem(${index}, 'tipoItem', this.value)" style="width: 100%;">
                                <option value="SERVICIO" ${item.tipoItem === 'SERVICIO' ? 'selected' : ''}>Servicio</option>
                                <option value="PAQUETE" ${item.tipoItem === 'PAQUETE' ? 'selected' : ''}>Paquete</option>
                            </select>
                        </td>
                        <td style="padding: 12px;">
                            <select class="form-input" onchange="actualizarItemSelect(${index}, this)" style="width: 100%;">
                                <option value="">Seleccione...</option>
                                ${opcionesHtml}
                            </select>
                        </td>
                        <td style="padding: 12px;">
                            <input type="number" class="form-input" min="1" value="${item.cantidad}" 
                                   onchange="actualizarItem(${index}, 'cantidad', this.value)" style="width: 100%;">
                        </td>
                        <td style="padding: 12px;">
                            <input type="number" class="form-input" step="0.01" min="0" value="${item.precioUnitario}" 
                                   onchange="actualizarItem(${index}, 'precioUnitario', this.value)" style="width: 100%;">
                        </td>
                        <td style="padding: 12px; font-weight: 600;">S/ ${subtotal.toFixed(2)}</td>
                        <td style="padding: 12px;">
                            <input type="text" class="form-input" value="${item.descripcionExtra || ''}" 
                                   onchange="actualizarItem(${index}, 'descripcionExtra', this.value)" 
                                   placeholder="Notas adicionales..." style="width: 100%;">
                        </td>
                        <td style="padding: 12px; text-align: center;">
                            <button type="button" onclick="eliminarItem(${index})" 
                                    style="background: #dc2626; color: white; border: none; border-radius: 4px; padding: 6px 12px; cursor: pointer;">
                                <i class="fas fa-trash"></i>
                            </button>
                        </td>
                    </tr>
                `;
            }).join('');

            actualizarTotales();
        }

            // Actualizar item
            function actualizarItem(index, campo, valor) {
            if (items[index]) {
                items[index][campo] = campo === 'cantidad' ? parseInt(valor) : 
                                     campo === 'precioUnitario' ? parseFloat(valor) : valor;
                
                if (campo === 'cantidad' || campo === 'precioUnitario') {
                    items[index].precioTotal = items[index].cantidad * items[index].precioUnitario;
                }
                
                if (campo === 'tipoItem') {
                    items[index].servicioId = null;
                    items[index].paqueteId = null;
                }
                
                renderItems();
            }
        }

            // Actualizar item desde select
            function actualizarItemSelect(index, select) {
            const selectedOption = select.options[select.selectedIndex];
            const precio = parseFloat(selectedOption.dataset.precio || 0);
            const id = parseInt(select.value);
            
            if (items[index]) {
                if (items[index].tipoItem === 'SERVICIO') {
                    items[index].servicioId = id;
                    items[index].paqueteId = null;
                } else {
                    items[index].paqueteId = id;
                    items[index].servicioId = null;
                }
                items[index].precioUnitario = precio;
                items[index].precioTotal = items[index].cantidad * precio;
                renderItems();
            }
        }

            // Actualizar totales
            function actualizarTotales() {
            const subtotal = items.reduce((sum, item) => sum + (item.precioTotal || 0), 0);
            const descuento = parseFloat(document.getElementById('descuentoAplicado')?.value || 0);
            const total = subtotal - descuento;

            document.getElementById('subtotalDisplay').textContent = subtotal.toFixed(2);
            document.getElementById('descuentoDisplay').textContent = descuento.toFixed(2);
            document.getElementById('totalDisplay').textContent = Math.max(0, total).toFixed(2);
        }

            // Cargar clientes
            async function cargarClientes() {
            try {
                const url = `http://turistas.spring.informaticapp.com:2410/api/v1/clientes${idEmpresa ? '?empresaId=' + idEmpresa : ''}`;
                const response = await fetch(url, {
                    headers: {
                        'Authorization': 'Bearer ' + token
                    }
                });

                if (response.ok) {
                    const data = await response.json();
                    const clientes = data.data || data.content || data.clientes || data.items || data.results || [];
                    
                    const select = document.getElementById('clienteId');
                    if (select) {
                        clientes.forEach(cliente => {
                            const option = document.createElement('option');
                            option.value = cliente.id || cliente.clienteId || cliente.idCliente;
                            option.textContent = `${cliente.nombre || cliente.nombres || ''} ${cliente.apellido || cliente.apellidos || ''}`.trim() || cliente.email || 'Cliente sin nombre';
                            select.appendChild(option);
                        });
                    }
                } else {
                    console.warn('Error al cargar clientes:', response.status, response.statusText);
                }
            } catch (error) {
                // Manejar errores de red (DNS, conexión, etc.)
                if (error.name === 'TypeError' && error.message.includes('Failed to fetch')) {
                    console.warn('No se pudo conectar al servidor para cargar clientes. Verifica tu conexión a internet.');
                } else {
                    console.error('Error al cargar clientes:', error);
                }
            }
        }

            // Event listeners para botones
            document.getElementById('btnAgregarServicio')?.addEventListener('click', () => agregarItem('SERVICIO'));
            document.getElementById('btnAgregarPaquete')?.addEventListener('click', () => agregarItem('PAQUETE'));
            document.getElementById('btnRecargarPersonal')?.addEventListener('click', cargarPersonal);
            
            // Búsqueda de personal
            document.getElementById('personalSearchInput')?.addEventListener('input', function(e) {
                renderPersonalCatalog(e.target.value);
            });

            // Actualizar totales cuando cambia el descuento
            document.getElementById('descuentoAplicado')?.addEventListener('input', actualizarTotales);

            // Función para volver a la lista de reservas
            function volverAReservas() {
            // Si existe la función loadReservasContent, usarla (cuando se carga desde admin.php)
            if (typeof loadReservasContent === 'function') {
                loadReservasContent();
            } else if (typeof window.loadReservasContent === 'function') {
                window.loadReservasContent();
            } else {
                // Si no existe, intentar activar la sección de reservas desde el sidebar
                const reservasLink = document.querySelector('.sidebar-link[data-section="reservas"]');
                if (reservasLink) {
                    reservasLink.click();
                } else {
                    // Como último recurso, recargar la página
                    window.location.href = 'reservas.php';
                }
            }
        }

            // Exportar funciones globalmente
            window.agregarPersonal = agregarPersonal;
            window.removerPersonal = removerPersonal;
            window.agregarItem = agregarItem;
            window.eliminarItem = eliminarItem;
            window.actualizarItem = actualizarItem;
            window.actualizarItemSelect = actualizarItemSelect;
            window.volverAReservas = volverAReservas;

            // Manejar envío del formulario
            document.getElementById('formNuevaReserva').addEventListener('submit', async function(e) {
                e.preventDefault();
                
                const form = this;
                const btnSubmit = document.getElementById('btnCrearReserva');
                const originalText = btnSubmit.innerHTML;
                
                // Validar que haya al menos un item
                if (items.length === 0) {
                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('error', 'Debe agregar al menos un servicio o paquete');
                    } else if (typeof window.mostrarAlerta === 'function') {
                        window.mostrarAlerta('error', 'Debe agregar al menos un servicio o paquete');
                    } else {
                        alert('Debe agregar al menos un servicio o paquete');
                    }
                    return;
                }
                
                // Deshabilitar botón
                btnSubmit.disabled = true;
                btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creando...';
                
                try {
                    // Obtener datos del formulario
                    const formData = new FormData(form);
                    const fechaServicio = formData.get('fechaServicio');
                    
                    // Preparar items
                    const itemsData = items.map(item => {
                        const isServicio = item.tipoItem === 'SERVICIO';
                        const isPaquete = item.tipoItem === 'PAQUETE';
                        return {
                            tipoItem: item.tipoItem,
                            servicioId: isServicio && item.servicioId ? parseInt(item.servicioId) : null,
                            paqueteId: isPaquete && item.paqueteId ? parseInt(item.paqueteId) : null,
                            cantidad: parseInt(item.cantidad) || 1,
                            precioUnitario: parseFloat(item.precioUnitario) || 0,
                            precioTotal: parseFloat(item.precioTotal) || 0,
                            descripcionExtra: item.descripcionExtra || ''
                        };
                    }).filter(item => (item.servicioId || item.paqueteId)); // Solo items con servicio o paquete seleccionado
                    
                    // Obtener promocionId del formulario si existe
                    const promocionId = formData.get('promocionId');

                    // Obtener el primer servicio o paquete para asignarlo directamente a la reserva
                    let idServicio = null;
                    let idPaquete = null;
                    if (itemsData.length > 0) {
                        const primerItem = itemsData[0];
                        if (primerItem.servicioId) {
                            idServicio = primerItem.servicioId;
                        }
                        if (primerItem.paqueteId) {
                            idPaquete = primerItem.paqueteId;
                        }
                    }

                    // Preparar asignaciones
                    const asignacionesData = Array.from(personalSeleccionado).map(personalId => ({
                        idPersonal: parseInt(personalId),
                        fechaAsignacion: fechaServicio || new Date().toISOString().split('T')[0],
                        observaciones: ''
                    }));

                    const reservaData = {
                        empresaId: idEmpresa ? parseInt(idEmpresa) : null,
                        idSucursal: idSucursal ? parseInt(idSucursal) : null,
                        clienteId: parseInt(formData.get('clienteId')),
                        promocionId: promocionId ? parseInt(promocionId) : null,
                        codigoReserva: formData.get('codigoReserva') || '',
                        fechaServicio: fechaServicio,
                        fechaReserva: formData.get('fechaReserva') || new Date().toISOString().split('T')[0],
                        numeroPersonas: parseInt(formData.get('numeroPersonas')),
                        descuentoAplicado: parseFloat(formData.get('descuentoAplicado') || 0),
                        observaciones: formData.get('observaciones') || '',
                        items: itemsData,
                        asignaciones: asignacionesData
                    };
                    
                    // Log para depuración
                    console.log('📤 Enviando reserva con datos:', {
                        empresaId: reservaData.empresaId,
                        idSucursal: reservaData.idSucursal,
                        clienteId: reservaData.clienteId,
                        codigoReserva: reservaData.codigoReserva,
                        items: reservaData.items.length,
                        asignaciones: reservaData.asignaciones.length
                    });
                    
                    // Enviar a la API
                    const response = await fetch('http://turistas.spring.informaticapp.com:2410/api/v1/reservas', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': 'Bearer ' + token
                        },
                        body: JSON.stringify(reservaData)
                    });
                    
                    const result = await response.json();
                    
                    if (response.ok) {
                        // Mostrar mensaje de éxito
                        if (typeof mostrarAlerta === 'function') {
                            mostrarAlerta('success', 'Reserva creada exitosamente');
                        } else if (typeof window.mostrarAlerta === 'function') {
                            window.mostrarAlerta('success', 'Reserva creada exitosamente');
                        } else {
                            alert('Reserva creada exitosamente');
                        }
                        
                        // Recargar la sección de reservas después de 1 segundo
                        setTimeout(() => {
                            if (typeof loadReservasContent === 'function') {
                                loadReservasContent();
                            } else {
                                window.location.reload();
                            }
                        }, 1000);
                    } else {
                        throw new Error(result.message || result.error || 'Error al crear la reserva');
                    }
                } catch (error) {
                    console.error('Error al crear reserva:', error);
                    if (typeof mostrarAlerta === 'function') {
                        mostrarAlerta('error', error.message || 'Error al crear la reserva');
                    } else if (typeof window.mostrarAlerta === 'function') {
                        window.mostrarAlerta('error', error.message || 'Error al crear la reserva');
                    } else {
                        alert('Error: ' + (error.message || 'Error al crear la reserva'));
                    }
                } finally {
                    // Restaurar botón
                    btnSubmit.disabled = false;
                    btnSubmit.innerHTML = originalText;
                }
            });
        
            // Cargar datos al cargar la página
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', function() {
                    cargarClientes();
                    cargarServiciosYPaquetes();
                    cargarPersonal();
                    cargarSucursalInfo(); // Cargar información de sucursal para generar código
                });
            } else {
                cargarClientes();
                cargarServiciosYPaquetes();
                cargarPersonal();
                cargarSucursalInfo(); // Cargar información de sucursal para generar código
            }
        })(); // Cerrar IIFE
    </script>
</body>
</html>
