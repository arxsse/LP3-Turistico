<?php
$apiBase = 'http://turistas.spring.informaticapp.com:2410/api/v1';
$token = $_COOKIE['userToken'] ?? '';
$isAjax = isset($_GET['ajax']);

$empresaParam = isset($_GET['idEmpresa']) ? preg_replace('/[^0-9]/', '', (string) $_GET['idEmpresa']) : '';
$rolParamRaw = isset($_GET['rol']) ? (string) $_GET['rol'] : '';
$rolParam = strtolower(trim($rolParamRaw));
$rolIdParam = isset($_GET['rolId']) ? (int) $_GET['rolId'] : null;
$idSucursalParam = isset($_GET['idSucursal']) ? (int) $_GET['idSucursal'] : null;
$rolesParam = $_GET['roles'] ?? null;
$rolesList = [];
if (is_string($rolesParam)) {
    $rolesList = array_filter(array_map('trim', explode(',', $rolesParam)));
} elseif (is_array($rolesParam)) {
    $rolesList = array_filter(array_map('trim', $rolesParam));
}
$esGerenteParam = isset($_GET['esGerente']) ? filter_var($_GET['esGerente'], FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE) : null;
$esGerenteComputed = in_array($rolParam, ['gerente', 'manager'], true) || ($rolIdParam !== null && in_array($rolIdParam, [3, 4], true));
if (!$esGerenteComputed && !empty($rolesList)) {
    foreach ($rolesList as $roleValue) {
        $upper = strtoupper($roleValue);
        if (strpos($upper, 'GERENTE') !== false || strpos($upper, 'MANAGER') !== false) {
            $esGerenteComputed = true;
            break;
        }
    }
}
if ($esGerenteParam !== null) {
    $esGerenteComputed = $esGerenteParam;
}
$empresaAttr = $empresaParam !== '' ? $empresaParam : '';
$idSucursalAttr = $idSucursalParam !== null ? (string) $idSucursalParam : '';
$rolIdAttr = $rolIdParam !== null ? (string) $rolIdParam : '';
$rolesAttr = !empty($rolesList) ? implode(',', $rolesList) : '';

ob_start();
?>
    <main class="reportes-main" id="reportesMain" tabindex="-1" aria-busy="false"
        data-api-base="<?php echo htmlspecialchars($apiBase, ENT_QUOTES, 'UTF-8'); ?>"
        data-token="<?php echo htmlspecialchars($token, ENT_QUOTES, 'UTF-8'); ?>"
        data-empresa-id="<?php echo htmlspecialchars($empresaAttr, ENT_QUOTES, 'UTF-8'); ?>"
        data-rol="<?php echo htmlspecialchars($rolParam, ENT_QUOTES, 'UTF-8'); ?>"
        data-rol-id="<?php echo htmlspecialchars($rolIdAttr, ENT_QUOTES, 'UTF-8'); ?>"
        data-id-sucursal="<?php echo htmlspecialchars($idSucursalAttr, ENT_QUOTES, 'UTF-8'); ?>"
        data-es-gerente="<?php echo $esGerenteComputed ? 'true' : 'false'; ?>"
        data-roles="<?php echo htmlspecialchars($rolesAttr, ENT_QUOTES, 'UTF-8'); ?>">
        <header class="reportes-header">
            <div class="header-left">
                <h1 class="page-title">Reportes</h1>
                <p class="header-subtitle">Consulta métricas operativas, comerciales y financieras en tiempo real.</p>
            </div>
            <div class="header-actions" role="group" aria-label="Acciones rápidas de reportes">
                <button class="btn btn-primary" type="button" id="btnRefrescar" aria-label="Actualizar los datos mostrados en los reportes" aria-describedby="reportesAccionesHelp">
                    <i class="fas fa-rotate" aria-hidden="true"></i>
                    <span>Actualizar</span>
                </button>
            </div>
        </header>
        <span id="reportesAccionesHelp" class="visually-hidden">Acción disponible para actualizar la información con los datos más recientes.</span>

        <section class="reportes-filtros" id="reportesFiltros" role="region" aria-labelledby="tituloFiltros" aria-describedby="reportesFiltrosHelp">
            <h2 id="tituloFiltros" class="visually-hidden">Filtros del módulo de reportes</h2>
            <div class="filtro-group">
                <label for="filtroFechaInicio">Desde</label>
                <input type="date" id="filtroFechaInicio" class="input-control">
            </div>
            <div class="filtro-group">
                <label for="filtroFechaFin">Hasta</label>
                <input type="date" id="filtroFechaFin" class="input-control">
            </div>
            <div class="filtro-group filtro-estado">
                <label for="filtroEstadoReserva">Estado de reserva</label>
                <select id="filtroEstadoReserva" class="input-control">
                    <option value="">Todos</option>
                    <option value="Pendiente">Pendiente</option>
                    <option value="Pago Parcial">Pago Parcial</option>
                    <option value="Pagada">Pagada</option>
                    <option value="Cancelada">Cancelada</option>
                    <option value="Completada">Completada</option>
                </select>
            </div>
            <div class="filtro-group filtro-impuestos">
                <label for="filtroImpuesto">% Impuesto</label>
                <input type="number" id="filtroImpuesto" class="input-control" min="0" max="50" step="0.01" value="18.00">
            </div>
            <p id="reportesFiltrosHelp" class="visually-hidden">Selecciona la empresa y el rango de fechas deseado. El botón actualizar obtiene la información nuevamente.</p>
        </section>

        <section class="alerta-contenedor" id="alertasContainer" role="status" aria-live="polite" aria-atomic="true"></section>

        <section class="kpi-grid" id="kpiGrid" role="region" aria-label="Indicadores clave de desempeño">
            <article class="kpi-card" id="kpiReservas" role="article" aria-labelledby="kpiReservasTitulo">
                <div class="kpi-icon kpi-icon-primary"><i class="fas fa-calendar-check"></i></div>
                <div class="kpi-content">
                    <p class="kpi-title" id="kpiReservasTitulo">Reservas en período</p>
                    <h3 class="kpi-value" data-field="reservas-total">--</h3>
                    <p class="kpi-detail" data-field="reservas-estado">En proceso de carga</p>
                </div>
            </article>
            <article class="kpi-card" id="kpiVentas" role="article" aria-labelledby="kpiVentasTitulo">
                <div class="kpi-icon kpi-icon-success"><i class="fas fa-chart-line"></i></div>
                <div class="kpi-content">
                    <p class="kpi-title" id="kpiVentasTitulo">Ventas totales</p>
                    <h3 class="kpi-value" data-field="ventas-total">--</h3>
                    <p class="kpi-detail" data-field="ventas-cantidad">Cargando información</p>
                </div>
            </article>
            <article class="kpi-card" id="kpiClientes" role="article" aria-labelledby="kpiClientesTitulo">
                <div class="kpi-icon kpi-icon-info"><i class="fas fa-user-friends"></i></div>
                <div class="kpi-content">
                    <p class="kpi-title" id="kpiClientesTitulo">Clientes activos</p>
                    <h3 class="kpi-value" data-field="clientes-activos">--</h3>
                    <p class="kpi-detail" data-field="clientes-total">Total clientes: --</p>
                </div>
            </article>
            <article class="kpi-card" id="kpiFinanzas" role="article" aria-labelledby="kpiFinanzasTitulo">
                <div class="kpi-icon kpi-icon-warning"><i class="fas fa-cash-register"></i></div>
                <div class="kpi-content">
                    <p class="kpi-title" id="kpiFinanzasTitulo">Saldo de caja</p>
                    <h3 class="kpi-value" data-field="finanzas-saldo">--</h3>
                    <p class="kpi-detail" data-field="finanzas-cajas">Cajas abiertas: --</p>
                </div>
            </article>
        </section>

        <section class="reportes-grid">
            <article class="report-card" id="reporteReservas" role="region" aria-labelledby="reporteReservasTitulo">
                <header class="report-card__header">
                    <div>
                        <h2 id="reporteReservasTitulo">Reporte de Reservas</h2>
                        <p>Detalle de reservas por estado y evolución en el período seleccionado.</p>
                    </div>
                    <span class="badge badge-primary" data-field="reservas-rango">--</span>
                </header>
                <div class="report-card__body">
                    <div class="chart-wrapper">
                        <canvas id="chartReservasEstado" height="220" role="img" aria-label="Gráfico de barras con reservas agrupadas por estado"></canvas>
                    </div>
                    <div class="table-wrapper">
                        <table class="report-table" id="tablaReservasEstado">
                            <caption class="visually-hidden">Tabla de reservas agrupadas por estado en el período seleccionado</caption>
                            <thead>
                                <tr>
                                    <th>Estado</th>
                                    <th>Total</th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                    <div class="report-details" role="list">
                        <div class="detail-item" role="listitem">
                            <span>Total facturado</span>
                            <strong data-field="reservas-monto-total">--</strong>
                        </div>
                    </div>
                </div>
            </article>

            <article class="report-card" id="reporteVentas" role="region" aria-labelledby="reporteVentasTitulo">
                <header class="report-card__header">
                    <div>
                        <h2 id="reporteVentasTitulo">Reporte de Ventas</h2>
                        <p>Distribución y comprobantes emitidos durante el proceso de venta.</p>
                    </div>
                    <span class="badge badge-success" data-field="ventas-rango">--</span>
                </header>
                <div class="report-card__body">
                    <div class="chart-wrapper">
                        <canvas id="chartVentasMetodo" height="220" role="img" aria-label="Gráfico circular de ventas por método de pago"></canvas>
                    </div>
                    <div class="report-insights" id="resumenVentas"></div>
                    <section class="receipt-preview" id="ventasDocumentosSection" aria-labelledby="ventasDocumentosTitulo">
                        <div class="receipt-preview__header">
                            <h3 id="ventasDocumentosTitulo">Comprobantes recientes</h3>
                            <small class="receipt-preview__hint">Los últimos documentos emitidos confirman la finalización de la venta.</small>
                        </div>
                        <p class="receipt-preview__empty" id="ventasDocumentosEmpty" hidden>Aún no se registran comprobantes en el rango seleccionado.</p>
                        <ul class="receipt-list" id="listaVentasDocumentos" aria-live="polite"></ul>
                    </section>
                    <div class="report-details" role="list">
                        <div class="detail-item" role="listitem">
                            <span>Ticket promedio</span>
                            <strong data-field="ventas-ticket-promedio">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Método más usado</span>
                            <strong data-field="ventas-metodo-top">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Comprobante más reciente</span>
                            <strong data-field="ventas-documento-tipo">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Número de comprobante</span>
                            <strong data-field="ventas-documento-numero">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Monto del comprobante</span>
                            <strong data-field="ventas-documento-monto">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Emisión registrada</span>
                            <strong data-field="ventas-documento-fecha">--</strong>
                        </div>
                    </div>
                </div>
            </article>

            <article class="report-card" id="reporteClientes" role="region" aria-labelledby="reporteClientesTitulo">
                <header class="report-card__header">
                    <div>
                        <h2 id="reporteClientesTitulo">Reporte de Clientes</h2>
                        <p>Resumen de clientes activos y distribución por nacionalidad.</p>
                    </div>
                    <span class="badge badge-info" data-field="clientes-empresa">--</span>
                </header>
                <div class="report-card__body">
                    <div class="chart-wrapper">
                        <canvas id="chartClientesNacionalidad" height="220" role="img" aria-label="Gráfico circular con clientes agrupados por nacionalidad"></canvas>
                    </div>
                    <div class="report-details" role="list">
                        <div class="detail-item" role="listitem">
                            <span>Tasa de actividad</span>
                            <strong data-field="clientes-actividad-percent">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Nacionalidad destacada</span>
                            <strong data-field="clientes-nacionalidad-top">--</strong>
                        </div>
                    </div>
                </div>
            </article>




            <article class="report-card report-card--highlight" id="reporteCaja" role="region" aria-labelledby="reporteCajaTitulo">
                <header class="report-card__header">
                    <div>
                        <h2 id="reporteCajaTitulo">Caja diaria destacada</h2>
                        <p>Resumen compacto de las operaciones registradas en la fecha analizada.</p>
                    </div>
                    <span class="badge badge-secondary" data-field="caja-fecha">--</span>
                </header>
                <div class="report-card__body">
                    <div class="report-details" role="list">
                        <div class="detail-item" role="listitem">
                            <span>Cajas analizadas</span>
                            <strong data-field="caja-total-cajas">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Monto inicial total</span>
                            <strong data-field="caja-monto-inicial">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Ingresos registrados</span>
                            <strong data-field="caja-ingresos">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Egresos registrados</span>
                            <strong data-field="caja-egresos">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Saldo actual</span>
                            <strong data-field="caja-saldo-actual">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Variación neta</span>
                            <strong data-field="caja-variacion">--</strong>
                        </div>
                    </div>
                </div>
            </article>

            <article class="report-card" id="reporteFinanzas" role="region" aria-labelledby="reporteFinanzasTitulo">
                <header class="report-card__header">
                    <div>
                        <h2 id="reporteFinanzasTitulo">Resumen Financiero</h2>
                        <p>Ingresos, egresos e impuestos estimados en el período.</p>
                    </div>
                    <span class="badge badge-secondary" data-field="finanzas-rango">--</span>
                </header>
                <div class="report-card__body">
                    <div class="chart-wrapper">
                        <canvas id="chartFinanzasIngresos" height="220" role="img" aria-label="Gráfico de barras con ingresos, egresos, saldo e impuestos"></canvas>
                    </div>
                    <div class="table-wrapper">
                        <table class="report-table" id="tablaFinanzas">
                            <caption class="visually-hidden">Tabla con el resumen financiero del período seleccionado</caption>
                            <thead>
                                <tr>
                                    <th>Indicador</th>
                                    <th>Valor</th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                    <div class="report-details" role="list">
                        <div class="detail-item" role="listitem">
                            <span>Ingresos período</span>
                            <strong data-field="finanzas-ingresos">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Egresos período</span>
                            <strong data-field="finanzas-egresos">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Impuestos estimados</span>
                            <strong data-field="finanzas-impuestos">--</strong>
                        </div>
                        <div class="detail-item" role="listitem">
                            <span>Neto después de impuestos</span>
                            <strong data-field="finanzas-neto">--</strong>
                        </div>
                    </div>
                </div>
            </article>
        </section>

    </main>
<?php
$mainContent = ob_get_clean();

if ($isAjax) {
    header('Content-Type: text/html; charset=UTF-8');
    echo $mainContent;
    return;
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reportes del Sistema Turístico</title>
    <link rel="stylesheet" href="../web.css">
    <link rel="stylesheet" href="../alertas.css">
    <link rel="stylesheet" href="reportes.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" integrity="sha512-7I0vR1jeaUp43EIBgzHuLlHotE5T7V6czS4fFqqpwYBFvToNiOfzmiEJeZVrDCTnhgypKekJy5o+1OtS9O35Vw==" crossorigin="anonymous" referrerpolicy="no-referrer" />
</head>
<body class="admin-body fondo-gris">
<?php echo $mainContent; ?>

    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.6/dist/chart.umd.min.js" integrity="sha256-r2n+KV0tzuSHoX2HFqRSbuuSSMzdON3NofM8JrIoMFw=" crossorigin="anonymous"></script>
    <script>
        window.REPORTES_CONFIG = {
            apiBase: <?php echo json_encode($apiBase, JSON_UNESCAPED_SLASHES); ?>,
            token: <?php echo json_encode($token, JSON_UNESCAPED_SLASHES); ?>
        };
    </script>
    <script src="reportes.js"></script>
</body>
</html>
