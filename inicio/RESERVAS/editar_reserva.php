<?php
// Obtener token del usuario
$tokenRaw = $_POST['token'] ?? $_GET['token'] ?? ($_COOKIE['userToken'] ?? null);
$token = $tokenRaw ? 'Bearer ' . $tokenRaw : null;

$apiBase = 'http://turistas.spring.informaticapp.com:2410/api/v1';

// Obtener ID de reserva
$reservaId = null;
if (isset($_POST['reservaId'])) {
    $reservaIdRaw = trim((string) $_POST['reservaId']);
    $reservaIdRaw = preg_replace('/[^0-9]+$/', '', $reservaIdRaw);
    $reservaId = (int) $reservaIdRaw;
} elseif (isset($_GET['id'])) {
    $reservaIdRaw = trim((string) $_GET['id']);
    $reservaIdRaw = preg_replace('/[^0-9]+$/', '', $reservaIdRaw);
    $reservaId = (int) $reservaIdRaw;
}

// Obtener idEmpresa
$idEmpresa = isset($_POST['empresaId']) ? intval($_POST['empresaId']) : (isset($_GET['idEmpresa']) ? intval($_GET['idEmpresa']) : null);

// Variables
$error = null;
$success = null;
$reservaDetalle = null;

// Función para obtener detalle de reserva
function obtenerDetalleReserva($apiBase, $reservaId, $token) {
    if (!$token || !$reservaId) {
        return ['success' => false, 'data' => null, 'message' => 'Token o ID de reserva no disponible'];
    }

    $curl = curl_init();
    curl_setopt_array($curl, [
        CURLOPT_URL => $apiBase . '/reservas/' . $reservaId,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_ENCODING => '',
        CURLOPT_MAXREDIRS => 10,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
        CURLOPT_CUSTOMREQUEST => 'GET',
        CURLOPT_HTTPHEADER => [
            'Authorization: ' . $token,
            'Content-Type: application/json'
        ],
    ]);

    $response = curl_exec($curl);
    $httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    $curlError = curl_error($curl);
    curl_close($curl);

    if ($curlError) {
        return ['success' => false, 'data' => null, 'message' => 'Error de conexión: ' . $curlError];
    }

    if ($httpCode !== 200) {
        $message = 'HTTP ' . $httpCode;
        $decoded = json_decode($response, true);
        if (json_last_error() === JSON_ERROR_NONE && isset($decoded['message'])) {
            $message .= ' - ' . $decoded['message'];
        }
        return ['success' => false, 'data' => null, 'message' => $message];
    }

    $data = json_decode($response, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        return ['success' => false, 'data' => null, 'message' => 'Respuesta inválida de la API'];
    }

    if (isset($data['data']) && is_array($data['data'])) {
        return ['success' => true, 'data' => $data['data'], 'message' => null];
    }

    return ['success' => true, 'data' => is_array($data) ? $data : null, 'message' => null];
}

// Procesar actualización si se envió el formulario (AJAX)
if ($_SERVER['REQUEST_METHOD'] === 'POST' && (isset($_POST['actualizar']) || isset($_POST['ajax']))) {
    if (!$token) {
        $error = 'No se encontró token de sesión. Inicia sesión nuevamente.';
    } elseif (!$reservaId) {
        $error = 'ID de reserva no especificado.';
        } else {
        try {
            // Validar campos requeridos
            if (empty($_POST['fechaServicio'])) {
                throw new Exception('La fecha de servicio es requerida');
            }
            if (empty($_POST['numeroPersonas']) || intval($_POST['numeroPersonas']) <= 0) {
                throw new Exception('El número de personas debe ser mayor a 0');
            }

            // Preparar items desde items_json
            $items = [];
            if (isset($_POST['items_json']) && !empty($_POST['items_json'])) {
                $itemsData = json_decode($_POST['items_json'], true);
                if (json_last_error() === JSON_ERROR_NONE && is_array($itemsData)) {
                    foreach ($itemsData as $item) {
                        $itemData = [
                            'tipoItem' => strtoupper($item['tipoItem'] ?? 'SERVICIO'),
                            'servicioId' => !empty($item['servicioId']) ? intval($item['servicioId']) : null,
                            'paqueteId' => !empty($item['paqueteId']) ? intval($item['paqueteId']) : null,
                            'cantidad' => intval($item['cantidad'] ?? 1),
                            'precioUnitario' => floatval($item['precioUnitario'] ?? 0),
                            'precioTotal' => floatval($item['precioTotal'] ?? 0),
                            'descripcionExtra' => $item['descripcionExtra'] ?? ''
                        ];
                        
                        // Incluir idReservaItem si existe (para actualizar items existentes)
                        if (!empty($item['idReservaItem'])) {
                            $itemData['idReservaItem'] = intval($item['idReservaItem']);
                        }
                        
                        $items[] = $itemData;
                    }
                }
            }

            // Preparar asignaciones desde asignaciones_json
            $asignaciones = [];
            if (isset($_POST['asignaciones_json']) && !empty($_POST['asignaciones_json'])) {
                $asignacionesData = json_decode($_POST['asignaciones_json'], true);
                if (json_last_error() === JSON_ERROR_NONE && is_array($asignacionesData)) {
                    foreach ($asignacionesData as $asignacion) {
                        $asignacionData = [
                            'idPersonal' => intval($asignacion['idPersonal'] ?? 0),
                            'fechaAsignacion' => $asignacion['fechaAsignacion'] ?? $_POST['fechaServicio'],
                            'observaciones' => $asignacion['observaciones'] ?? ''
                        ];
                        
                        // Incluir idAsignacion si existe (para actualizar asignaciones existentes)
                        if (!empty($asignacion['idAsignacion'])) {
                            $asignacionData['idAsignacion'] = intval($asignacion['idAsignacion']);
                        }
                        
                        $asignaciones[] = $asignacionData;
                    }
                }
            }

            // Preparar datos para la API (formato del ejemplo)
            $reservaData = [
                'empresaId' => $idEmpresa ?: null,
                'fechaServicio' => $_POST['fechaServicio'],
                'numeroPersonas' => intval($_POST['numeroPersonas']),
                'descuentoAplicado' => !empty($_POST['descuentoAplicado']) ? floatval($_POST['descuentoAplicado']) : 0.00,
                'observaciones' => $_POST['observaciones'] ?? '',
                'items' => $items,
                'asignaciones' => $asignaciones,
                'sincronizarAsignaciones' => true
            ];

            // Remover valores null
            $reservaData = array_filter($reservaData, function($value) {
            return $value !== null;
        });

            // Convertir a JSON
            $jsonData = json_encode($reservaData, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

            // Realizar petición PUT al endpoint /reservas/{id}/detalle (formato del ejemplo)
    $curl = curl_init();
    curl_setopt_array($curl, [
                CURLOPT_URL => $apiBase . '/reservas/' . $reservaId . '/detalle',
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_ENCODING => '',
        CURLOPT_MAXREDIRS => 10,
                CURLOPT_TIMEOUT => 0,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
                CURLOPT_CUSTOMREQUEST => 'PUT',
                CURLOPT_POSTFIELDS => $jsonData,
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
        $decoded = json_decode($response, true);
                $errorMsg = 'Error HTTP ' . $httpCode;
        if (json_last_error() === JSON_ERROR_NONE && isset($decoded['message'])) {
                    $errorMsg .= ': ' . $decoded['message'];
        }
                throw new Exception($errorMsg);
    }

            $result = json_decode($response, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
                throw new Exception('Respuesta inválida de la API');
            }

            $success = 'Reserva actualizada correctamente';
            
            // Si es AJAX, devolver JSON
            if (isset($_GET['ajax']) || isset($_POST['ajax'])) {
                header('Content-Type: application/json');
        echo json_encode([
                    'success' => true,
                    'message' => $success,
                    'data' => $result
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }

        } catch (Exception $e) {
            $error = $e->getMessage();
            
            // Si es AJAX, devolver JSON con error
            if (isset($_GET['ajax']) || isset($_POST['ajax'])) {
                header('Content-Type: application/json');
        http_response_code(400);
        echo json_encode([
            'success' => false,
                    'message' => $error
        ], JSON_UNESCAPED_UNICODE);
        exit;
    }
        }
    }
}

// Obtener detalle de la reserva para mostrar en el formulario
if ($token && $reservaId && !$success) {
    $detalleResult = obtenerDetalleReserva($apiBase, $reservaId, $token);
    if ($detalleResult['success']) {
        $reservaDetalle = $detalleResult['data'];
    } else {
        $error = $detalleResult['message'] ?? 'No se pudo obtener el detalle de la reserva.';
    }
}

// Si es AJAX, mostrar el formulario completo
if (isset($_GET['ajax']) && $_GET['ajax'] === '1') {
    // Preparar datos de la reserva para JavaScript
    $reservaJson = $reservaDetalle ? json_encode($reservaDetalle, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) : 'null';
    $itemsJson = isset($reservaDetalle['items']) ? json_encode($reservaDetalle['items'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) : '[]';
    $asignacionesJson = isset($reservaDetalle['asignaciones']) ? json_encode($reservaDetalle['asignaciones'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES) : '[]';
        ?>
        <div class="content-header">
            <div class="card">
                <div class="card-header">
                    <h2 class="section-title">Editar Reserva</h2>
                    <div class="header-actions">
                    <button type="button" class="btn btn-secondary" onclick="if (typeof loadReservasContent === 'function') { loadReservasContent(); } else { window.location.href='reservas.php'; }">
                        <i class="fas fa-arrow-left"></i> Volver
                        </button>
                    </div>
                </div>
                <div class="card-body">
                    <?php if ($error && !$reservaDetalle): ?>
                        <div class="alerta alerta-error" style="margin-bottom: 20px;">
                            <i class="fas fa-exclamation-triangle"></i>
                            <?php echo htmlspecialchars($error); ?>
                        </div>
                    <?php elseif ($reservaDetalle): ?>
                    <form method="POST" action="javascript:void(0);" class="form-container" id="formEditarReserva" onsubmit="event.preventDefault(); return false;">
                            <input type="hidden" name="ajax" value="1">
                        <input type="hidden" name="actualizar" value="1">
                            <input type="hidden" name="reservaId" value="<?php echo htmlspecialchars($reservaId); ?>">
                        <input type="hidden" name="empresaId" id="empresaId" value="<?php echo htmlspecialchars($idEmpresa ?? ''); ?>">
                        <input type="hidden" name="items_json" id="itemsJsonField" value="">
                        <input type="hidden" name="asignaciones_json" id="asignacionesJsonField" value="">
                        <input type="hidden" name="token" id="tokenField" value="<?php echo htmlspecialchars($tokenRaw ?? ''); ?>">
                        
                                <div class="form-grid">
                            <!-- Código de Reserva (solo lectura) -->
                                    <div class="form-group">
                                <label class="form-label">Código de Reserva</label>
                                <input 
                                    type="text" 
                                    class="form-input" 
                                    value="<?php echo htmlspecialchars($reservaDetalle['codigoReserva'] ?? ''); ?>"
                                    disabled
                                >
                                    </div>

                            <!-- Fecha de Servicio -->
                                    <div class="form-group">
                                <label class="form-label" for="fechaServicio">Fecha de Servicio <span class="required">*</span></label>
                                <input 
                                    type="date" 
                                    id="fechaServicio" 
                                    name="fechaServicio" 
                                    class="form-input" 
                                    value="<?php echo htmlspecialchars($reservaDetalle['fechaServicio'] ?? ''); ?>"
                                    required
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
                                    value="<?php echo htmlspecialchars($reservaDetalle['numeroPersonas'] ?? '1'); ?>"
                                    required
                                >
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
                                    value="<?php echo htmlspecialchars($reservaDetalle['descuentoAplicado'] ?? '0.00'); ?>"
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
                                ><?php echo htmlspecialchars($reservaDetalle['observaciones'] ?? ''); ?></textarea>
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
                                    <div style="margin-bottom: 15px; position: relative;">
                                        <input 
                                            type="search" 
                                            id="personalSearchInput" 
                                            class="form-input" 
                                            placeholder="Buscar por nombre o cargo..."
                                            style="padding-left: 40px;"
                                        >
                                        <i class="fas fa-search" style="position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: #9ca3af;"></i>
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
                            <button type="button" class="btn btn-secondary" onclick="if (typeof loadReservasContent === 'function') { loadReservasContent(); } else { window.location.href='reservas.php'; }">
                                <i class="fas fa-times"></i> Cancelar
                                </button>
                            <button type="submit" class="btn btn-primary" id="btnActualizarReserva">
                                <i class="fas fa-save"></i> Guardar Cambios
                                </button>
                            </div>
                        </form>

                        <script>
                            (function() {
                            const reservaData = <?php echo $reservaJson; ?>;
                            const itemsIniciales = <?php echo $itemsJson; ?>;
                            const asignacionesIniciales = <?php echo $asignacionesJson; ?>;
                            
                            // Obtener token
                            let userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                            let userData = null;
                            if (userDataStr) {
                                try {
                                    userData = JSON.parse(userDataStr);
                                } catch (e) {
                                    console.error('Error al parsear userData:', e);
                                }
                            }
                            const token = userData?.token || document.getElementById('tokenField')?.value || '<?php echo $tokenRaw ?? ''; ?>';

                                const form = document.getElementById('formEditarReserva');
                                const itemsJsonField = document.getElementById('itemsJsonField');
                                const asignacionesJsonField = document.getElementById('asignacionesJsonField');
                                const fechaServicioInput = document.getElementById('fechaServicio');
                            
                            // Variables para personal
                                let personalCatalog = [];
                            let personalSeleccionado = new Map(); // Map para mantener idPersonal -> {idPersonal, idAsignacion, fechaAsignacion, observaciones}
                            
                            // Variables para items (servicios/paquetes)
                            let items = [];
                            let servicios = [];
                            let paquetes = [];
                            
                            // Inicializar items desde los datos de la reserva
                            if (itemsIniciales && Array.isArray(itemsIniciales) && itemsIniciales.length > 0) {
                                // Normalizar items iniciales
                                items = itemsIniciales.map(item => ({
                                    idReservaItem: item.idReservaItem || null,
                                    tipoItem: (item.tipoItem || 'SERVICIO').toUpperCase(),
                                    servicioId: item.servicioId || item.idServicio || null,
                                    paqueteId: item.paqueteId || item.idPaquete || null,
                                    cantidad: parseInt(item.cantidad || 1),
                                    precioUnitario: parseFloat(item.precioUnitario || item.precio || 0),
                                    precioTotal: parseFloat(item.precioTotal || (item.precioUnitario || 0) * (item.cantidad || 1)),
                                    descripcionExtra: item.descripcionExtra || ''
                                }));
                                itemsJsonField.value = JSON.stringify(items);
                            } else {
                                items = [];
                                itemsJsonField.value = '[]';
                            }
                            
                            // Inicializar personal seleccionado desde asignaciones iniciales
                            if (asignacionesIniciales && Array.isArray(asignacionesIniciales)) {
                                asignacionesIniciales.forEach(asignacion => {
                                    const idPersonal = asignacion.idPersonal || asignacion.personal?.idPersonal || asignacion.personal?.id;
                                    if (idPersonal) {
                                        // Intentar obtener el nombre desde múltiples fuentes
                                        let nombre = asignacion.nombre || asignacion.nombrePersonal || '';
                                        if (!nombre && asignacion.personal) {
                                            const personal = asignacion.personal;
                                            // Intentar nombreCompleto
                                            if (personal.nombreCompleto) {
                                                nombre = personal.nombreCompleto;
                                            } else {
                                                // Construir desde nombres y apellidos
                                                const nombres = personal.nombres || personal.nombre || '';
                                                const apellidos = personal.apellidos || personal.apellido || '';
                                                const partes = [nombres, apellidos].filter(p => p && p.trim()).map(p => p.trim());
                                                if (partes.length > 0) {
                                                    nombre = partes.join(' ');
                                                }
                                            }
                                        }
                                        // Si aún no hay nombre, intentar desde nombrePersonal/apellidoPersonal
                                        if (!nombre) {
                                            const partes = [
                                                asignacion.nombrePersonal || '',
                                                asignacion.apellidoPersonal || ''
                                            ].filter(p => p && p.trim()).map(p => p.trim());
                                            if (partes.length > 0) {
                                                nombre = partes.join(' ');
                                            }
                                        }
                                        // Si aún no hay nombre, usar el fallback
                                        if (!nombre) {
                                            nombre = `Personal #${idPersonal}`;
                                        }
                                        
                                        // Intentar obtener el cargo desde múltiples fuentes
                                        let cargo = asignacion.cargo || asignacion.cargoPersonal || '';
                                        if (!cargo && asignacion.personal) {
                                            const personal = asignacion.personal;
                                            if (personal.cargo) {
                                                if (typeof personal.cargo === 'object' && personal.cargo !== null) {
                                                    cargo = personal.cargo.nombre || personal.cargo.descripcion || '';
                                        } else {
                                                    cargo = String(personal.cargo);
                                                }
                                            }
                                            if (!cargo && personal.rol) {
                                                cargo = typeof personal.rol === 'object' && personal.rol !== null
                                                    ? (personal.rol.nombre || '')
                                                    : String(personal.rol);
                                            }
                                            if (!cargo && personal.puesto) {
                                                cargo = String(personal.puesto);
                                            }
                                        }
                                        if (!cargo) {
                                            cargo = 'Sin cargo';
                                        }
                                        
                                        personalSeleccionado.set(String(idPersonal), {
                                            idPersonal: parseInt(idPersonal),
                                            idAsignacion: asignacion.idAsignacion || null,
                                            fechaAsignacion: asignacion.fechaAsignacion || (fechaServicioInput ? fechaServicioInput.value : null),
                                            observaciones: asignacion.observaciones || '',
                                            nombre: nombre,
                                            cargo: cargo
                                        });
                                    }
                                });
                                actualizarAsignacionesField();
                            } else {
                                asignacionesJsonField.value = '[]';
                            }
                            
                            // Función para actualizar el campo hidden de asignaciones
                            function actualizarAsignacionesField() {
                                const asignacionesArray = Array.from(personalSeleccionado.values()).map(item => {
                                    const asignacion = {
                                        idPersonal: item.idPersonal
                                    };
                                    if (item.idAsignacion) {
                                        asignacion.idAsignacion = item.idAsignacion;
                                    }
                                    if (item.fechaAsignacion) {
                                        asignacion.fechaAsignacion = item.fechaAsignacion;
                                    }
                                    if (item.observaciones) {
                                        asignacion.observaciones = item.observaciones;
                                    }
                                    return asignacion;
                                });
                                asignacionesJsonField.value = JSON.stringify(asignacionesArray);
                            }
                            
                            // Cargar catálogo de personal
                            async function cargarPersonal() {
                                try {
                                    const idEmpresa = document.getElementById('empresaId')?.value;
                                    const personalUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/personal${idEmpresa ? '?empresaId=' + idEmpresa + '&estado=true' : '?estado=true'}`;
                                    const response = await fetch(personalUrl, {
                                        headers: { 'Authorization': 'Bearer ' + token }
                                    });
                                    if (response.ok) {
                                        const data = await response.json();
                                        personalCatalog = data.data || data.content || data.items || data.results || [];
                                        renderPersonalCatalog();
                                        renderPersonalSeleccionado();
                                    } else {
                                        console.warn('Error al cargar personal:', response.status, response.statusText);
                                        personalCatalog = [];
                                        const container = document.getElementById('personalCatalogList');
                                        if (container) {
                                            container.innerHTML = '<div style="padding: 20px; color: #dc3545;">Error al cargar personal. Intente nuevamente.</div>';
                                        }
                                    }
                                } catch (error) {
                                    console.error('Error al cargar personal:', error);
                                    personalCatalog = [];
                                    const container = document.getElementById('personalCatalogList');
                                    if (container) {
                                        container.innerHTML = '<div style="padding: 20px; color: #dc3545;">Error al cargar personal. Verifica tu conexión a internet.</div>';
                                    }
                                }
                            }
                            
                            // Renderizar catálogo de personal disponible
                            function renderPersonalCatalog() {
                                const container = document.getElementById('personalCatalogList');
                                const searchInput = document.getElementById('personalSearchInput');
                                const searchTerm = (searchInput?.value || '').toLowerCase();
                                
                                const filtered = personalCatalog.filter(p => {
                                    const personalId = String(p.id || p.idPersonal || p.idEmpleado);
                                    // Excluir los que ya están seleccionados
                                    if (personalSeleccionado.has(personalId)) {
                                            return false;
                                        }
                                    // Filtrar por término de búsqueda
                                    if (searchTerm) {
                                        const nombre = ((p.nombre || p.nombres || '') + ' ' + (p.apellido || p.apellidos || '')).toLowerCase();
                                        const cargo = (p.cargo || p.puesto || '').toLowerCase();
                                        return nombre.includes(searchTerm) || cargo.includes(searchTerm);
                                    }
                                            return true;
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
                                        <div class="personal-item" data-id="${personalId}" style="padding: 12px; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; transition: all 0.2s; background: white;" 
                                             onmouseover="this.style.background='#f3f4f6'; this.style.borderColor='#10b981';" 
                                             onmouseout="this.style.background='white'; this.style.borderColor='#e5e7eb';"
                                             onclick="window.agregarPersonalEditar(${personalId})">
                                            <div style="font-weight: 600; margin-bottom: 4px; color: #111827;">${nombre}</div>
                                            <div style="font-size: 12px; color: #6b7280;">${cargo}</div>
                                        </div>
                                    `;
                                }).join('');
                            }
                            
                            // Agregar personal seleccionado
                            window.agregarPersonalEditar = function(personalId) {
                                const personalIdStr = String(personalId);
                                if (personalSeleccionado.has(personalIdStr)) {
                                    return;
                                }
                                
                                // Buscar el personal en el catálogo para obtener su información
                                const personalInfo = personalCatalog.find(p => String(p.id || p.idPersonal || p.idEmpleado) === personalIdStr);
                                
                                personalSeleccionado.set(personalIdStr, {
                                    idPersonal: parseInt(personalId),
                                    idAsignacion: null,
                                    fechaAsignacion: fechaServicioInput ? fechaServicioInput.value : null,
                                    observaciones: '',
                                    nombre: personalInfo ? `${personalInfo.nombre || personalInfo.nombres || ''} ${personalInfo.apellido || personalInfo.apellidos || ''}`.trim() : `Personal #${personalId}`,
                                    cargo: personalInfo ? (personalInfo.cargo || personalInfo.puesto || 'Sin cargo') : 'Sin cargo'
                                });
                                
                                actualizarAsignacionesField();
                                renderPersonalCatalog();
                                renderPersonalSeleccionado();
                            };
                            
                            // Remover personal seleccionado
                            window.removerPersonalEditar = function(personalId) {
                                const personalIdStr = String(personalId);
                                personalSeleccionado.delete(personalIdStr);
                                actualizarAsignacionesField();
                                renderPersonalCatalog();
                                renderPersonalSeleccionado();
                            };
                            
                            // Renderizar personal seleccionado
                            function renderPersonalSeleccionado() {
                                const container = document.getElementById('personalSelectedList');
                                const empty = document.getElementById('personalSelectedEmpty');
                                const counter = document.getElementById('personalSelectedCounter');
                                
                                if (!container) return;
                                
                                const seleccionados = Array.from(personalSeleccionado.values());
                                
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
                                
                                container.innerHTML = seleccionados.map(item => {
                                    return `
                                        <div class="personal-selected-item" data-id="${item.idPersonal}" style="padding: 12px; border: 1px solid #10b981; background: #ecfdf5; border-radius: 8px;">
                                            <div style="margin-bottom: 8px;">
                                                <div style="font-weight: 600; margin-bottom: 4px; color: #111827;">${item.nombre || `Personal #${item.idPersonal}`}</div>
                                                <div style="font-size: 12px; color: #6b7280;">${item.cargo || 'Sin cargo'}</div>
                                            </div>
                                            <div style="margin-top: 8px;">
                                                <label style="font-size: 12px; color: #6b7280; display: block; margin-bottom: 4px;">Observaciones:</label>
                                                <textarea 
                                                    class="form-input" 
                                                    rows="2" 
                                                    placeholder="Notas opcionales"
                                                    style="font-size: 12px; padding: 6px;"
                                                    onchange="window.actualizarObservacionesPersonal(${item.idPersonal}, this.value)"
                                                >${item.observaciones || ''}</textarea>
                                            </div>
                                            <button 
                                                type="button" 
                                                onclick="window.removerPersonalEditar(${item.idPersonal})" 
                                                style="margin-top: 8px; background: #dc2626; color: white; border: none; border-radius: 4px; padding: 6px 12px; cursor: pointer; width: 100%;">
                                                <i class="fas fa-times"></i> Remover
                                            </button>
                                        </div>
                                    `;
                                }).join('');
                            }
                            
                            // Actualizar observaciones de personal
                            window.actualizarObservacionesPersonal = function(personalId, observaciones) {
                                const personalIdStr = String(personalId);
                                if (personalSeleccionado.has(personalIdStr)) {
                                    const item = personalSeleccionado.get(personalIdStr);
                                    item.observaciones = observaciones;
                                    personalSeleccionado.set(personalIdStr, item);
                                    actualizarAsignacionesField();
                                }
                            };
                            
                            // Event listeners
                            document.getElementById('personalSearchInput')?.addEventListener('input', function(e) {
                                        renderPersonalCatalog();
                                    });
                            
                            document.getElementById('btnRecargarPersonal')?.addEventListener('click', function() {
                                cargarPersonal();
                            });
                            
                            // Actualizar fecha de asignación cuando cambia la fecha de servicio
                            if (fechaServicioInput) {
                                fechaServicioInput.addEventListener('change', function() {
                                    const nuevaFecha = this.value;
                                    personalSeleccionado.forEach((item, key) => {
                                        item.fechaAsignacion = nuevaFecha;
                                        personalSeleccionado.set(key, item);
                                    });
                                    actualizarAsignacionesField();
                                });
                            }
                            
                            // Cargar personal al iniciar
                            cargarPersonal();
                            
                            // ========== FUNCIONES PARA ITEMS (SERVICIOS/PAQUETES) ==========
                            
                            // Cargar servicios y paquetes
                            async function cargarServicios() {
                                try {
                                    const idEmpresa = document.getElementById('empresaId')?.value;
                                    const serviciosUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/servicios${idEmpresa ? '?empresaId=' + idEmpresa + '&estado=true' : '?estado=true'}`;
                                    const response = await fetch(serviciosUrl, {
                                        headers: { 'Authorization': 'Bearer ' + token }
                                    });
                                    if (response.ok) {
                                        const data = await response.json();
                                        servicios = data.data || data.content || data.items || data.results || [];
                                        renderItems();
                                    } else {
                                        console.warn('Error al cargar servicios:', response.status, response.statusText);
                                        servicios = [];
                                    }
                                        } catch (error) {
                                    console.error('Error al cargar servicios:', error);
                                    servicios = [];
                                }
                            }
                            
                            async function cargarPaquetes() {
                                try {
                                    const idEmpresa = document.getElementById('empresaId')?.value;
                                    const paquetesUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/paquetes${idEmpresa ? '?empresaId=' + idEmpresa + '&estado=true' : '?estado=true'}`;
                                    const response = await fetch(paquetesUrl, {
                                        headers: { 'Authorization': 'Bearer ' + token }
                                    });
                                    if (response.ok) {
                                        const data = await response.json();
                                        paquetes = data.data || data.content || data.items || data.results || [];
                                        renderItems();
                                    } else {
                                        console.warn('Error al cargar paquetes:', response.status, response.statusText);
                                        paquetes = [];
                                    }
                                } catch (error) {
                                    console.error('Error al cargar paquetes:', error);
                                    paquetes = [];
                                }
                            }
                            
                            // Agregar item (servicio o paquete)
                            window.agregarItemEditar = function(tipo) {
                                const item = {
                                        idReservaItem: null,
                                    tipoItem: tipo,
                                        servicioId: null,
                                        paqueteId: null,
                                        cantidad: 1,
                                        precioUnitario: 0,
                                        precioTotal: 0,
                                        descripcionExtra: ''
                                    };
                                items.push(item);
                                actualizarItemsField();
                                renderItems();
                            };
                            
                            // Eliminar item
                            window.eliminarItemEditar = function(index) {
                                items.splice(index, 1);
                                actualizarItemsField();
                                renderItems();
                            };
                            
                            // Renderizar items
                            function renderItems() {
                                const tbody = document.getElementById('itemsTableBody');
                                const table = document.getElementById('itemsTable');
                                const empty = document.getElementById('itemsEmptyState');
                                
                                if (!tbody || !table || !empty) return;
                                
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
                                                <select class="form-input" onchange="window.actualizarItemEditar(${index}, 'tipoItem', this.value)" style="width: 100%;">
                                                    <option value="SERVICIO" ${item.tipoItem === 'SERVICIO' ? 'selected' : ''}>Servicio</option>
                                                    <option value="PAQUETE" ${item.tipoItem === 'PAQUETE' ? 'selected' : ''}>Paquete</option>
                                                </select>
                                            </td>
                                            <td style="padding: 12px;">
                                                <select class="form-input" onchange="window.actualizarItemSelectEditar(${index}, this)" style="width: 100%;">
                                                    <option value="">Seleccione...</option>
                                                    ${opcionesHtml}
                                                </select>
                                            </td>
                                            <td style="padding: 12px;">
                                                <input type="number" class="form-input" min="1" value="${item.cantidad}" 
                                                       onchange="window.actualizarItemEditar(${index}, 'cantidad', this.value)" style="width: 100%;">
                                            </td>
                                            <td style="padding: 12px;">
                                                <input type="number" class="form-input" step="0.01" min="0" value="${item.precioUnitario}" 
                                                       onchange="window.actualizarItemEditar(${index}, 'precioUnitario', this.value)" style="width: 100%;">
                                            </td>
                                            <td style="padding: 12px; font-weight: 600;">S/ ${subtotal.toFixed(2)}</td>
                                            <td style="padding: 12px;">
                                                <input type="text" class="form-input" value="${item.descripcionExtra || ''}" 
                                                       onchange="window.actualizarItemEditar(${index}, 'descripcionExtra', this.value)" 
                                                       placeholder="Notas adicionales..." style="width: 100%;">
                                            </td>
                                            <td style="padding: 12px; text-align: center;">
                                                <button type="button" onclick="window.eliminarItemEditar(${index})" 
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
                            window.actualizarItemEditar = function(index, campo, valor) {
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
                                    
                                    actualizarItemsField();
                                                renderItems();
                                }
                            };
                            
                            // Actualizar item desde select
                            window.actualizarItemSelectEditar = function(index, select) {
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
                                    actualizarItemsField();
                                                renderItems();
                                }
                            };
                            
                            // Actualizar campo hidden de items
                            function actualizarItemsField() {
                                const itemsArray = items.map(item => {
                                    const itemData = {
                                        tipoItem: item.tipoItem,
                                        cantidad: item.cantidad,
                                        precioUnitario: item.precioUnitario,
                                        precioTotal: item.precioTotal,
                                        descripcionExtra: item.descripcionExtra || ''
                                    };
                                    if (item.idReservaItem) {
                                        itemData.idReservaItem = item.idReservaItem;
                                    }
                                    if (item.tipoItem === 'SERVICIO' && item.servicioId) {
                                        itemData.servicioId = item.servicioId;
                                        itemData.paqueteId = null;
                                    } else if (item.tipoItem === 'PAQUETE' && item.paqueteId) {
                                        itemData.paqueteId = item.paqueteId;
                                        itemData.servicioId = null;
                                    }
                                    return itemData;
                                });
                                itemsJsonField.value = JSON.stringify(itemsArray);
                            }
                            
                            // Actualizar totales
                            function actualizarTotales() {
                                const subtotal = items.reduce((sum, item) => sum + (item.precioTotal || 0), 0);
                                const descuento = parseFloat(document.getElementById('descuentoAplicado')?.value || 0);
                                const total = subtotal - descuento;
                                
                                const subtotalDisplay = document.getElementById('subtotalDisplay');
                                const descuentoDisplay = document.getElementById('descuentoDisplay');
                                const totalDisplay = document.getElementById('totalDisplay');
                                
                                if (subtotalDisplay) subtotalDisplay.textContent = subtotal.toFixed(2);
                                if (descuentoDisplay) descuentoDisplay.textContent = descuento.toFixed(2);
                                if (totalDisplay) totalDisplay.textContent = Math.max(0, total).toFixed(2);
                            }
                            
                            // Event listeners para items
                            document.getElementById('btnAgregarServicio')?.addEventListener('click', () => {
                                window.agregarItemEditar('SERVICIO');
                            });
                            
                            document.getElementById('btnAgregarPaquete')?.addEventListener('click', () => {
                                window.agregarItemEditar('PAQUETE');
                            });
                            
                            // Actualizar totales cuando cambia el descuento
                            document.getElementById('descuentoAplicado')?.addEventListener('input', actualizarTotales);
                            
                            // Cargar servicios y paquetes al iniciar
                            cargarServicios();
                            cargarPaquetes();
                            
                            // Renderizar items iniciales
                            renderItems();
                            
                            // Manejar envío del formulario
                            form.addEventListener('submit', async function(e) {
                                e.preventDefault();
                                
                                const btnSubmit = document.getElementById('btnActualizarReserva');
                                const originalText = btnSubmit.innerHTML;
                                
                                // Deshabilitar botón
                                btnSubmit.disabled = true;
                                btnSubmit.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';
                                
                                try {
                                    // Obtener datos del formulario
                                    const formData = new FormData(form);
                                    const fechaServicio = formData.get('fechaServicio');
                                    const numeroPersonas = parseInt(formData.get('numeroPersonas'));
                                    const descuentoAplicado = parseFloat(formData.get('descuentoAplicado') || 0);
                                    const observaciones = formData.get('observaciones') || '';
                                    
                                    // Obtener items y asignaciones desde los campos hidden (ya están actualizados)
                                    const itemsPayload = JSON.parse(itemsJsonField.value || '[]');
                                    const asignacionesPayload = JSON.parse(asignacionesJsonField.value || '[]');
                                    
                                    // Preparar datos para la API (formato del ejemplo)
                                    const reservaData = {
                                        empresaId: document.getElementById('empresaId')?.value ? parseInt(document.getElementById('empresaId').value) : null,
                                        fechaServicio: fechaServicio,
                                        numeroPersonas: numeroPersonas,
                                        descuentoAplicado: descuentoAplicado,
                                        observaciones: observaciones,
                                        items: itemsPayload,
                                        asignaciones: asignacionesPayload,
                                        sincronizarAsignaciones: true
                                    };
                                    
                                    // Remover valores null
                                    Object.keys(reservaData).forEach(key => {
                                        if (reservaData[key] === null) {
                                            delete reservaData[key];
                                        }
                                    });
                                    
                                    console.log('📤 Enviando actualización de reserva:', reservaData);
                                    
                                    // Realizar petición PUT al endpoint /reservas/{id}/detalle (formato del ejemplo)
                                    const response = await fetch('<?php echo $apiBase; ?>/reservas/<?php echo $reservaId; ?>/detalle', {
                                        method: 'PUT',
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
                                            mostrarAlerta('success', 'Reserva actualizada exitosamente');
                                        } else if (typeof window.mostrarAlerta === 'function') {
                                            window.mostrarAlerta('success', 'Reserva actualizada exitosamente');
                                            } else {
                                            alert('Reserva actualizada exitosamente');
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
                                        throw new Error(result.message || result.error || 'Error al actualizar la reserva');
                                        }
                                    } catch (error) {
                                        console.error('Error al actualizar reserva:', error);
                                        if (typeof mostrarAlerta === 'function') {
                                        mostrarAlerta('error', error.message || 'Error al actualizar la reserva');
                                    } else if (typeof window.mostrarAlerta === 'function') {
                                        window.mostrarAlerta('error', error.message || 'Error al actualizar la reserva');
                                        } else {
                                        alert('Error: ' + (error.message || 'Error al actualizar la reserva'));
                                        }
                                    } finally {
                                    // Restaurar botón
                                    btnSubmit.disabled = false;
                                    btnSubmit.innerHTML = originalText;
                                    }
                                });
                            })();
                        </script>
                    <?php else: ?>
                        <div class="alerta alerta-error" style="margin-bottom: 20px;">
                            <i class="fas fa-exclamation-triangle"></i>
                        No se pudo cargar la información de la reserva.
                        </div>
                    <?php endif; ?>
                </div>
            </div>
        </div>
        <?php
    exit;
}

// Si no es AJAX, redirigir a admin.php
header('Location: ../admin.php?idEmpresa=' . ($idEmpresa ?? '') . '#reservas');
exit;
?>
