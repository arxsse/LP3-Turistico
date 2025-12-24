<?php
$ventasStylesHref = '../web.css?v=20251201';
if (!defined('VENTAS_CSS_LOADED')) {
    define('VENTAS_CSS_LOADED', true);
    echo '<link rel="stylesheet" href="' . htmlspecialchars($ventasStylesHref, ENT_QUOTES, 'UTF-8') . '" id="ventasStyles">';
    echo '<link rel="stylesheet" href="VENTAS/alertas.css" id="ventasAlertas">';
}

$apiVentasBase = 'http://turistas.spring.informaticapp.com:2410/api/v1/ventas';

if ($error && empty($ventas)) {
    echo '<div class="alert alert-danger">' . htmlspecialchars($error) . '</div>';
}

?>
<div class="content-header">
    <div class="card">
        <div class="card-header">
            <h2 class="section-title">Gestión de Ventas</h2>
            <div class="header-actions">
                <div class="search-filters" style="display: flex; gap: 10px; align-items: center;">
                    <input type="text" id="busquedaVentas" class="form-control" placeholder="Buscar por cliente, código..." value="<?php echo htmlspecialchars($_GET['busqueda'] ?? ''); ?>" style="width: 250px; padding: 5px 10px; border: 1px solid #ddd; border-radius: 4px;">
                    <input type="date" id="fechaVentas" class="form-control" value="<?php echo htmlspecialchars($_GET['fecha'] ?? ''); ?>" style="width: 150px; padding: 5px 10px; border: 1px solid #ddd; border-radius: 4px;">
                    <select id="estadoVentas" class="form-control" style="width: 150px; padding: 5px; border: 1px solid #ddd; border-radius: 4px;">
                        <option value="">Todos los estados</option>
                        <option value="Pendiente" <?php echo (isset($_GET['estado']) && $_GET['estado'] === 'Pendiente') ? 'selected' : ''; ?>>Pendiente</option>
                        <option value="Confirmada" <?php echo (isset($_GET['estado']) && $_GET['estado'] === 'Confirmada') ? 'selected' : ''; ?>>Confirmada</option>
                        <option value="PagoParcial" <?php echo (isset($_GET['estado']) && $_GET['estado'] === 'PagoParcial') ? 'selected' : ''; ?>>Pago Parcial</option>
                        <option value="Pagada" <?php echo (isset($_GET['estado']) && $_GET['estado'] === 'Pagada') ? 'selected' : ''; ?>>Pagada</option>
                        <option value="Completada" <?php echo (isset($_GET['estado']) && $_GET['estado'] === 'Completada') ? 'selected' : ''; ?>>Completada</option>
                        <option value="Cancelada" <?php echo (isset($_GET['estado']) && $_GET['estado'] === 'Cancelada') ? 'selected' : ''; ?>>Cancelada</option>
                    </select>
                    <button onclick="filtrarVentas()" class="btn btn-primary" style="padding: 5px 15px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;">
                        <i class="fas fa-search"></i> Buscar
                    </button>
                    <?php if (!empty($_GET['busqueda']) || !empty($_GET['estado']) || !empty($_GET['fecha'])): ?>
                        <button onclick="limpiarFiltrosVentas()" class="btn btn-secondary" title="Limpiar filtros" style="padding: 5px 10px; background-color: #6c757d; color: white; border: none; border-radius: 4px; cursor: pointer;">
                            <i class="fas fa-times"></i>
                        </button>
                    <?php endif; ?>
                </div>
            </div>
        </div>
        <div class="card-body">
            <div class="table-responsive">
                <?php if (!empty($reservas)): ?>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Código</th>
                                <th>Cliente</th>
                                <th>Fecha Reserva</th>
                                <th>Fecha Servicio</th>
                                <th>Personas</th>
                                <th>Total</th>
                                <th>Estado</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
        <?php foreach ($reservas as $r): 
            $reservaId = $r['id'] ?? ($r['idReserva'] ?? ($r['reservaId'] ?? ''));
            
            // Lógica de extracción de datos igual a reservas.php
            $codigo = $r['codigoReserva'] ?? ($r['codigo'] ?? $reservaId);

            $clienteNombre = '-';
            if (isset($r['nombreCliente']) || isset($r['apellidoCliente'])) {
                $clienteNombre = trim(($r['nombreCliente'] ?? '') . ' ' . ($r['apellidoCliente'] ?? ''));
            } elseif (isset($r['cliente']['nombre']) || isset($r['cliente']['apellido'])) {
                $clienteNombre = trim(($r['cliente']['nombre'] ?? '') . ' ' . ($r['cliente']['apellido'] ?? ''));
            }
            if ($clienteNombre === '') {
                $clienteNombre = isset($r['cliente']['nombreCompleto']) ? $r['cliente']['nombreCompleto'] : 'Sin asignar';
            }

            $fechaReserva = '-';
            if (!empty($r['fechaReserva'])) {
                $fechaReserva = date('d/m/Y', strtotime($r['fechaReserva']));
            } elseif (!empty($r['fechaCreacion'])) {
                $fechaReserva = date('d/m/Y', strtotime($r['fechaCreacion']));
            }

            $fechaServicio = '-';
            if (!empty($r['fechaServicio'])) {
                $fechaServicio = date('d/m/Y', strtotime($r['fechaServicio']));
            } elseif (!empty($r['fechaServicioInicio'])) {
                $fechaServicio = date('d/m/Y', strtotime($r['fechaServicioInicio']));
            } elseif (!empty($r['fecha'])) {
                $fechaServicio = date('d/m/Y', strtotime($r['fecha']));
            }

            $personas = $r['numeroPersonas'] ?? ($r['cantidadPersonas'] ?? ($r['personas'] ?? ($r['cantidad'] ?? 1)));
            $total = $r['precioTotal'] ?? ($r['total'] ?? ($r['montoTotal'] ?? 0));
            // Asegurar que total sea numérico
            $total = is_numeric($total) ? (float)$total : 0;
            
            $estado = $r['estado'] ?? 'Pendiente';
            $isPagada = strtolower($estado) === 'pagada';
            $badgeClass = $isPagada ? 'status-active' : 'status-warning';
        ?>
            <tr>
                <td><?= htmlspecialchars($codigo) ?></td>
                <td><?= htmlspecialchars($clienteNombre) ?></td>
                <td><?= htmlspecialchars($fechaReserva) ?></td>
                <td><?= htmlspecialchars($fechaServicio) ?></td>
                <td><?= htmlspecialchars($personas) ?></td>
                <td><?= htmlspecialchars(number_format((float)$total, 2)) ?></td>
                                    <td><span class="status-badge <?= $badgeClass ?>" id="status-<?= $reservaId ?>"><?= htmlspecialchars($estado) ?></span></td>
                                    <td>
                                        <div class="action-buttons">
                                            <?php if ($reservaId): ?>
                                                <button class="btn-action btn-pay" title="Pagar" data-id="<?= htmlspecialchars($reservaId) ?>" data-total="<?= htmlspecialchars($total) ?>" onclick="openPagoModal('<?= htmlspecialchars($reservaId) ?>', '<?= htmlspecialchars($total) ?>')" <?= $isPagada ? 'disabled style="opacity:0.5;cursor:not-allowed;"' : '' ?>>
                                                    <i class="fas fa-money-bill-wave"></i>
                                                </button>
                                                <button class="btn-action btn-receipt" id="btn-receipt-<?= $reservaId ?>" title="Emitir Comprobante" <?= !$isPagada ? 'disabled' : '' ?> onclick="imprimirComprobante('<?= htmlspecialchars($reservaId) ?>')">
                                                    <i class="fas fa-file-invoice-dollar"></i>
                                                </button>
                                            <?php else: ?>
                                                <span class="text-muted" title="ID no disponible"><i class="fas fa-exclamation-circle"></i></span>
                                            <?php endif; ?>
                                        </div>
                                    </td>
                                </tr>
                            <?php endforeach; ?>
                        </tbody>
                    </table>
                <?php else: ?>
                    <div class="alert alert-info">
                        <i class="fas fa-info-circle"></i> No hay reservas pendientes de pago.
                    </div>
                <?php endif; ?>
            </div>
        </div>
    </div>
</div>

<!-- Modal de Pago -->
<div id="pagoModal" class="modal" style="display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; overflow: auto; background-color: rgba(0,0,0,0.5);">
    <div class="modal-content" style="background-color: #fff; margin: 5% auto; padding: 0; border: 1px solid #888; width: 500px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1);">
        <div class="modal-header" style="background: #f8f9fa; padding: 15px 20px; border-bottom: 1px solid #dee2e6; border-radius: 8px 8px 0 0; display: flex; justify-content: space-between; align-items: center;">
            <h3 style="margin: 0; font-size: 1.25rem; color: #333;">Realizar Pago</h3>
            <span class="close" onclick="closePagoModal()" style="cursor: pointer; font-size: 24px; color: #aaa;">&times;</span>
        </div>
        <div class="modal-body" style="padding: 20px;">
            <div style="background: #e9ecef; padding: 10px; border-radius: 4px; margin-bottom: 20px; text-align: center;">
                <span style="font-size: 0.9rem; color: #666;">Total a Pagar</span>
                <div style="font-size: 1.5rem; font-weight: bold; color: #28a745;">S/ <span id="modalTotal">0.00</span></div>
            </div>
            <input type="hidden" id="modalReservaId">
            
            <div class="form-group" style="margin-bottom: 20px;">
                <label style="display: block; margin-bottom: 8px; font-weight: 600;">Método de Pago:</label>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                    <label style="cursor: pointer; display: flex; align-items: center; gap: 5px;"><input type="radio" name="metodoPago" value="VISA"> Visa</label>
                    <label style="cursor: pointer; display: flex; align-items: center; gap: 5px;"><input type="radio" name="metodoPago" value="YAPE"> Yape</label>
                    <label style="cursor: pointer; display: flex; align-items: center; gap: 5px;"><input type="radio" name="metodoPago" value="PLIN"> Plin</label>
                    <label style="cursor: pointer; display: flex; align-items: center; gap: 5px;"><input type="radio" name="metodoPago" value="EFECTIVO"> Efectivo</label>
                </div>
            </div>

            <div class="form-group" style="margin-bottom: 20px; border-top: 1px solid #eee; padding-top: 15px;">
                <label style="display: block; margin-bottom: 8px; font-weight: 600;">Tipo de Comprobante:</label>
                <div style="display: flex; gap: 20px; margin-bottom: 15px;">
                    <label style="cursor: pointer;"><input type="radio" name="tipoComprobante" value="BOLETA" checked onchange="toggleComprobanteFields()"> Boleta</label>
                    <label style="cursor: pointer;"><input type="radio" name="tipoComprobante" value="FACTURA" onchange="toggleComprobanteFields()"> Factura</label>
                </div>

                <div id="comprobanteFields">
                    <div class="form-group" style="margin-bottom: 10px;">
                        <label id="lblDocNumber" style="display: block; margin-bottom: 5px; font-size: 0.9rem;">DNI:</label>
                        <input type="text" id="docNumber" class="form-control" style="width: 100%; padding: 8px; border: 1px solid #ced4da; border-radius: 4px;">
                    </div>
                    <div class="form-group" style="margin-bottom: 10px;">
                        <label id="lblClientName" style="display: block; margin-bottom: 5px; font-size: 0.9rem;">Nombre:</label>
                        <input type="text" id="clientName" class="form-control" style="width: 100%; padding: 8px; border: 1px solid #ced4da; border-radius: 4px;">
                    </div>
                    <div class="form-group" style="margin-bottom: 10px;">
                        <label style="display: block; margin-bottom: 5px; font-size: 0.9rem;">Dirección:</label>
                        <input type="text" id="clientAddress" class="form-control" style="width: 100%; padding: 8px; border: 1px solid #ced4da; border-radius: 4px;">
                    </div>
                </div>
            </div>
        </div>
        <div class="modal-footer" style="padding: 15px 20px; border-top: 1px solid #dee2e6; text-align: right; background: #f8f9fa; border-radius: 0 0 8px 8px;">
            <button class="btn btn-secondary" onclick="closePagoModal()" style="margin-right: 10px;">Cancelar</button>
            <button class="btn btn-primary" onclick="confirmarPago()">Confirmar Pago</button>
        </div>
    </div>
</div>

<script>
    const ID_CAJA_ABIERTA = <?php echo json_encode($idCajaAbierta ?? null); ?>;

    function toggleComprobanteFields() {
        const tipo = document.querySelector('input[name="tipoComprobante"]:checked').value;
        const lblDoc = document.getElementById('lblDocNumber');
        const lblName = document.getElementById('lblClientName');
        const inputDoc = document.getElementById('docNumber');
        
        if (tipo === 'FACTURA') {
            lblDoc.textContent = 'RUC:';
            lblName.textContent = 'Razón Social:';
            inputDoc.maxLength = 11;
            inputDoc.placeholder = '11 dígitos';
        } else {
            lblDoc.textContent = 'DNI:';
            lblName.textContent = 'Nombre:';
            inputDoc.maxLength = 8;
            inputDoc.placeholder = '8 dígitos';
        }
        // Limpiar valor si cambia el tipo para evitar inconsistencias
        inputDoc.value = inputDoc.value.replace(/\D/g, '').slice(0, inputDoc.maxLength);
    }

    function openPagoModal(id, total) {
        document.getElementById('modalReservaId').value = id;
        // Guardar el total en un atributo data para recuperarlo fácilmente
        const modalTotalEl = document.getElementById('modalTotal');
        modalTotalEl.textContent = parseFloat(total).toFixed(2);
        modalTotalEl.dataset.total = total;
        
        document.getElementById('pagoModal').style.display = 'block';
        
        // Reset fields
        const radios = document.getElementsByName('metodoPago');
        for(let i=0; i<radios.length; i++) radios[i].checked = false;
        
        document.querySelector('input[name="tipoComprobante"][value="BOLETA"]').checked = true;
        document.getElementById('docNumber').value = '';
        document.getElementById('clientName').value = '';
        document.getElementById('clientAddress').value = '';
        toggleComprobanteFields();
    }

    function closePagoModal() {
        document.getElementById('pagoModal').style.display = 'none';
    }

    async function confirmarPago() {
        const id = document.getElementById('modalReservaId').value;
        const radios = document.getElementsByName('metodoPago');
        let metodo = null;
        for (const radio of radios) {
            if (radio.checked) {
                metodo = radio.value;
                break;
            }
        }

        if (!metodo) {
            alert('Por favor seleccione un método de pago.');
            return;
        }

        const tipoComprobante = document.querySelector('input[name="tipoComprobante"]:checked').value;
        const docNumber = document.getElementById('docNumber').value.trim();
        const clientName = document.getElementById('clientName').value.trim();
        const clientAddress = document.getElementById('clientAddress').value.trim();

        // Validaciones completas
        if (!docNumber) {
            alert('Por favor ingrese el ' + (tipoComprobante === 'FACTURA' ? 'RUC' : 'DNI') + '.');
            return;
        }
        
        if (tipoComprobante === 'BOLETA') {
            if (!/^\d{8}$/.test(docNumber)) {
                alert('El DNI debe tener exactamente 8 dígitos numéricos.');
                return;
            }
        } else if (tipoComprobante === 'FACTURA') {
            if (!/^\d{11}$/.test(docNumber)) {
                alert('El RUC debe tener exactamente 11 dígitos numéricos.');
                return;
            }
        }

        if (!clientName) {
            alert('Por favor ingrese ' + (tipoComprobante === 'FACTURA' ? 'la Razón Social' : 'el Nombre') + '.');
            return;
        }
        if (!clientAddress) {
            alert('Por favor ingrese la Dirección.');
            return;
        }

        // Obtener token y datos de usuario
        let token = null;
        let userData = null;
        try { 
            const s = sessionStorage.getItem('userData') || localStorage.getItem('userData'); 
            if (s) { 
                userData = JSON.parse(s); 
                token = userData.token; 
            } 
        } catch(e) {}
        
        if (!token) { alert('No se encontró token de sesión.'); return; }

        const url = 'http://turistas.spring.informaticapp.com:2410/api/v1/reservas/' + encodeURIComponent(id);
        
        try {
            // Primero obtenemos la reserva actual
            const getRes = await fetch(url, { headers: { 'Authorization': 'Bearer ' + token } });
            if (!getRes.ok) throw new Error('Error al obtener reserva');
            const reservaData = await getRes.json();
            
            // Actualizamos estado y datos del comprobante
            // Nota: Enviamos los datos del comprobante en un objeto 'comprobante' o campos planos según lo que soporte la API.
            // Como no conocemos la API, enviaremos ambos formatos por si acaso.
            const updateData = { 
                ...reservaData, 
                estado: 'Pagada', 
                metodoPago: metodo,
                tipoComprobante: tipoComprobante,
                numeroDocumento: docNumber,
                nombreFacturacion: clientName,
                direccionFacturacion: clientAddress,
                // Objeto anidado por si la API lo prefiere
                datosComprobante: {
                    tipo: tipoComprobante,
                    numero: docNumber,
                    nombre: clientName,
                    direccion: clientAddress
                }
            };
            
            const res = await fetch(url, {
                method: 'PUT',
                headers: { 
                    'Authorization': 'Bearer ' + token,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(updateData)
            });

            if (res.ok) {
                // Registrar movimiento en caja si existe una caja abierta
                console.log('Intentando registrar movimiento. ID Caja:', ID_CAJA_ABIERTA);
                
                if (ID_CAJA_ABIERTA) {
                    try {
                        // Obtener usuario ID
                        let usuarioId = null;
                        if (userData && userData.id) usuarioId = userData.id;
                        else if (userData && userData.usuarioId) usuarioId = userData.usuarioId;

                        // Preparar datos
                        // Intentar obtener el monto de varias fuentes
                        let monto = 0;
                        
                        // 1. Intentar desde el DOM (fuente más confiable ya que viene del PHP)
                        const modalTotalEl = document.getElementById('modalTotal');
                        if (modalTotalEl && modalTotalEl.dataset.total) {
                            monto = parseFloat(modalTotalEl.dataset.total);
                        } else if (modalTotalEl) {
                            monto = parseFloat(modalTotalEl.textContent);
                        }

                        // 2. Si falla, intentar desde reservaData
                        if ((!monto || isNaN(monto)) && reservaData) {
                            if (reservaData.total) monto = parseFloat(reservaData.total);
                            else if (reservaData.precioTotal) monto = parseFloat(reservaData.precioTotal);
                        }
                        
                        // 3. Validación final
                        if (!monto || isNaN(monto) || monto <= 0) {
                            // Si sigue siendo 0, pedir al usuario
                            const manualMonto = prompt("No se pudo detectar el monto automáticamente. Por favor ingrese el monto a pagar:", "0.00");
                            if (manualMonto) {
                                monto = parseFloat(manualMonto);
                            }
                        }

                        if (!monto || isNaN(monto) || monto <= 0) {
                            alert("Error: El monto a pagar no es válido (0). No se puede registrar el movimiento.");
                            return;
                        }

                        const metodoUpper = metodo.toUpperCase();
                        const concepto = `Venta Reserva #${reservaData.codigoReserva || id} - ${clientName}`;
                        const fechaISO = new Date().toISOString();

                        // Payload "Kitchen Sink" Ajustado (Sin vincular ID de Venta para evitar error 404)
                        const movimientoPayload = {
                            // Campos estándar DTO
                            tipoMovimiento: 'Ingreso', // TitleCase
                            monto: monto,
                            descripcion: concepto,
                            
                            // Campos de Fecha
                            fechaHora: fechaISO,

                            // IMPORTANTE: No enviamos ventaId ni idVenta porque el backend
                            // lanza "Venta no encontrada" si el ID no corresponde a una tabla 'ventas' existente.
                            // La relación queda en la descripción.

                            // Compatibilidad Legacy
                            tipo: 'Ingreso',
                            tipo_movimiento: 'Ingreso',
                            
                            // IDs planos
                            cajaId: ID_CAJA_ABIERTA,
                            idCaja: ID_CAJA_ABIERTA,
                            id_caja: ID_CAJA_ABIERTA,

                            // Usuario
                            usuarioId: usuarioId,
                            idUsuario: usuarioId
                        };

                        console.log('Enviando movimiento:', movimientoPayload);

                        // Estrategia 1: Endpoint anidado /cajas/{id}/movimientos (Confirmado por CajaController.java)
                        let movRes = await fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/cajas/${ID_CAJA_ABIERTA}/movimientos`, {
                            method: 'POST',
                            headers: { 
                                'Authorization': 'Bearer ' + token,
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(movimientoPayload)
                        });

                        if (!movRes.ok) {
                            console.warn('Fallo /cajas/.../movimientos:', await movRes.text());
                            
                            // Estrategia 2: Endpoint estándar /movimientos (Fallback)
                            movRes = await fetch('http://turistas.spring.informaticapp.com:2410/api/v1/movimientos', {
                                method: 'POST',
                                headers: { 
                                    'Authorization': 'Bearer ' + token,
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify(movimientoPayload)
                            });
                        }

                        if (!movRes.ok) {
                            console.warn('Fallo /movimientos:', await movRes.text());

                            // Estrategia 3: Endpoint específico /movimientos-caja
                            movRes = await fetch('http://turistas.spring.informaticapp.com:2410/api/v1/movimientos-caja', {
                                method: 'POST',
                                headers: { 
                                    'Authorization': 'Bearer ' + token,
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify(movimientoPayload)
                            });
                        }

                        if (!movRes.ok) {
                            const errorTxt = await movRes.text();
                            console.error('Error final al registrar movimiento:', errorTxt);
                            alert('Advertencia: El pago se registró, pero falló el registro en caja. Revise la consola para más detalles.\nÚltimo error: ' + errorTxt);
                        } else {
                            console.log('Movimiento registrado correctamente en caja.');
                        }
                    } catch (movError) {
                        console.error('Error al registrar movimiento en caja:', movError);
                        alert('Advertencia: Error de conexión al registrar movimiento en caja.');
                    }
                } else {
                    console.warn('No se encontró ID de caja abierta');
                    alert('Advertencia: No hay una caja abierta detectada. El movimiento no se registrará en caja.');
                }

                // Guardar datos del comprobante localmente por si la API no los persiste
                const receiptData = {
                    tipo: tipoComprobante,
                    numero: docNumber,
                    nombre: clientName,
                    direccion: clientAddress
                };
                localStorage.setItem('receipt_' + id, JSON.stringify(receiptData));

                alert('Pago registrado correctamente.');
                closePagoModal();
                
                // Actualizar UI
                const statusBadge = document.getElementById('status-' + id);
                if (statusBadge) {
                    statusBadge.textContent = 'Pagada';
                    statusBadge.className = 'status-badge status-active';
                }
                
                const btnReceipt = document.getElementById('btn-receipt-' + id);
                if (btnReceipt) {
                    btnReceipt.disabled = false;
                    btnReceipt.classList.remove('btn-secondary');
                    btnReceipt.classList.add('btn-success');
                }
                
                const btnPay = document.querySelector(`button[data-id="${id}"].btn-pay`);
                if (btnPay) {
                    btnPay.disabled = true;
                    btnPay.style.opacity = '0.5';
                    btnPay.style.cursor = 'not-allowed';
                }

            } else {
                const txt = await res.text();
                throw new Error('Error al actualizar reserva: ' + txt);
            }
        } catch (err) {
            console.error(err);
            alert('Error al procesar el pago: ' + err.message);
        }
    }

    // Close modal when clicking outside
    window.onclick = function(event) {
        const modal = document.getElementById('pagoModal');
        if (event.target == modal) {
            closePagoModal();
        }
    }

    function imprimirComprobante(id) {
        let token = null;
        try { const s = sessionStorage.getItem('userData') || localStorage.getItem('userData'); if (s) { const p = JSON.parse(s); token = p.token; } } catch(e) {}
        
        let url = 'VENTAS/imprimir_comprobante.php?reservaId=' + encodeURIComponent(id) + '&token=' + encodeURIComponent(token || '');
        
        // Verificar si hay datos locales del comprobante
        const stored = localStorage.getItem('receipt_' + id);
        if (stored) {
            try {
                const data = JSON.parse(stored);
                if (data.tipo) url += '&tipo=' + encodeURIComponent(data.tipo);
                if (data.numero) url += '&docNum=' + encodeURIComponent(data.numero);
                if (data.nombre) url += '&docName=' + encodeURIComponent(data.nombre);
                if (data.direccion) url += '&docAddr=' + encodeURIComponent(data.direccion);
            } catch(e) {}
        }

        window.open(url, 'Comprobante', 'width=400,height=600,scrollbars=yes');
    }

    // Funciones para filtros de búsqueda
    function filtrarVentas() {
        const busqueda = document.getElementById('busquedaVentas').value;
        const estado = document.getElementById('estadoVentas').value;
        const fecha = document.getElementById('fechaVentas').value;
        
        // Usar la función global loadVentasContent definida en admin.php
        if (typeof loadVentasContent === 'function') {
            loadVentasContent({
                busqueda: busqueda,
                estado: estado,
                fecha: fecha
            });
        } else {
            console.error('loadVentasContent no está definida');
        }
    }

    function limpiarFiltrosVentas() {
        const inputBusqueda = document.getElementById('busquedaVentas');
        const selectEstado = document.getElementById('estadoVentas');
        const inputFecha = document.getElementById('fechaVentas');
        
        if (inputBusqueda) inputBusqueda.value = '';
        if (selectEstado) selectEstado.value = '';
        if (inputFecha) inputFecha.value = '';
        
        filtrarVentas();
    }

    // Permitir búsqueda al presionar Enter
    const inputBusqueda = document.getElementById('busquedaVentas');
    if (inputBusqueda) {
        inputBusqueda.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                filtrarVentas();
            }
        });
    }
</script>
