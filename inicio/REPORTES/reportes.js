(function() {
    const DEFAULT_API_BASE = 'http://turistas.spring.informaticapp.com:2410/api/v1';
    const CONFIG = window.REPORTES_CONFIG || {};
    let API_BASE = CONFIG.apiBase || DEFAULT_API_BASE;

    const state = {
        token: CONFIG.token || '',
        user: null,
        empresaId: '',
        empresaNombre: '',
        isSuperAdmin: false,
        esGerente: Boolean(CONFIG.esGerente),
        idSucursal: toFiniteNumberOrNull(CONFIG.idSucursal),
        filters: {
            fechaInicio: '',
            fechaFin: '',
            estadoReserva: '',
            impuesto: '18.00'
        },
        charts: {},
        cache: {
            reservas: null,
            ventas: null,
            clientes: null,
            personal: null,
            servicios: null,
            paquetes: null,
            finanzas: {
                caja: null,
                impuestos: null
            },
            ventasDocumentos: []
        }
    };

    const elements = {};
    let isMounted = false;
    let initPromise = null;

    const autoStart = () => {
        scheduleInit({ resetFilters: true, forceRefresh: true }).catch(error => {
            console.error('❌ Error al inicializar el módulo de reportes:', error);
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autoStart, { once: true });
    } else {
        autoStart();
    }

    function scheduleInit(options = {}) {
        return (async () => {
            if (initPromise) {
                try {
                    await initPromise;
                } catch (error) {
                    console.warn('⚠️ Inicialización previa fallida, reintentando...', error);
                }
            }

            initPromise = init(options).catch(error => {
                throw error;
            });

            try {
                await initPromise;
            } finally {
                initPromise = null;
            }
        })();
    }

    async function init(options = {}) {
        const {
            resetFilters = false,
            preserveFilters = false,
            forceRefresh = false
        } = options;

        applyConfigFromDom();
        cacheDom();

        if (!elements.main) {
            console.warn('⚠️ Reportes: contenedor principal no encontrado durante la inicialización');
            isMounted = false;
            return;
        }

        state.user = resolveUserData();
        state.token = resolveToken();
        determineEmpresa();

        if (resetFilters || !isMounted) {
            configureFilters();
        } else if (preserveFilters) {
            applyFiltersToInputs();
        }

        bindEvents();

        await preloadEmpresaSelect();
        updateEmpresaSelection();

        const shouldForceRefresh = forceRefresh || !isMounted;
        await refreshAll(shouldForceRefresh);
        focusMain();
        isMounted = true;
    }

    function applyConfigFromDom() {
        const main = document.getElementById('reportesMain');
        if (!main) {
            API_BASE = CONFIG.apiBase || DEFAULT_API_BASE;
            return;
        }
        if (!CONFIG.apiBase && main.dataset.apiBase) {
            CONFIG.apiBase = main.dataset.apiBase;
        }
        if (!CONFIG.token && main.dataset.token) {
            CONFIG.token = main.dataset.token;
        }
        if (!CONFIG.rol && main.dataset.rol) {
            CONFIG.rol = main.dataset.rol;
        }
        if (CONFIG.rolId === undefined && main.dataset.rolId) {
            CONFIG.rolId = main.dataset.rolId;
        }
        if (!CONFIG.roles && main.dataset.roles) {
            CONFIG.roles = main.dataset.roles.split(',').map(item => item.trim()).filter(Boolean);
        }
        if (CONFIG.esGerente === undefined && main.dataset.esGerente) {
            CONFIG.esGerente = main.dataset.esGerente === 'true';
        }
        if (CONFIG.idSucursal === undefined && main.dataset.idSucursal) {
            CONFIG.idSucursal = main.dataset.idSucursal;
        }
        if (!CONFIG.empresaId && main.dataset.empresaId) {
            CONFIG.empresaId = main.dataset.empresaId;
        }
        API_BASE = CONFIG.apiBase || DEFAULT_API_BASE;
        if (!state.token) {
            state.token = CONFIG.token || '';
        }
        if (CONFIG.esGerente !== undefined) {
            state.esGerente = Boolean(CONFIG.esGerente);
        }
        const configSucursal = toFiniteNumberOrNull(CONFIG.idSucursal);
        if (configSucursal !== null) {
            state.idSucursal = configSucursal;
        }
        if (!state.empresaId) {
            const datasetEmpresaId = toFiniteNumberOrNull(main.dataset.empresaId || CONFIG.empresaId);
            if (datasetEmpresaId !== null) {
                state.empresaId = String(datasetEmpresaId);
            }
        }
    }

    function toFiniteNumberOrNull(value) {
        if (value === undefined || value === null) {
            return null;
        }
        if (typeof value === 'string' && value.trim() === '') {
            return null;
        }
        const num = Number(value);
        return Number.isFinite(num) ? num : null;
    }

    function cacheDom() {
        elements.main = document.getElementById('reportesMain');
        elements.alertasContainer = document.getElementById('alertasContainer');
        elements.empresaSelect = document.getElementById('filtroEmpresa');
        elements.fechaInicio = document.getElementById('filtroFechaInicio');
        elements.fechaFin = document.getElementById('filtroFechaFin');
        elements.estadoReserva = document.getElementById('filtroEstadoReserva');
        elements.impuesto = document.getElementById('filtroImpuesto');
        elements.btnRefrescar = document.getElementById('btnRefrescar');
        elements.btnExportarCsv = document.getElementById('btnExportarCsv');
        elements.btnImprimir = document.getElementById('btnImprimir');
        elements.reportesEmpty = document.getElementById('reportesEmpty');
        elements.kpiGrid = document.getElementById('kpiGrid');
    }

    function resolveUserData() {
        const storageSource = sessionStorage.getItem('userData') || localStorage.getItem('userData');
        if (!storageSource) {
            return null;
        }
        try {
            return JSON.parse(storageSource);
        } catch (error) {
            console.error('No se pudo parsear userData almacenado:', error);
            return null;
        }
    }

    function resolveToken() {
        if (state.user && state.user.token) {
            return state.user.token;
        }
        if (CONFIG.token) {
            return CONFIG.token;
        }
        const match = document.cookie.match(/(?:^|;\s*)userToken=([^;]+)/);
        if (match) {
            return decodeURIComponent(match[1]);
        }
        return '';
    }

    function determineEmpresa() {
        const selectedCompanyStr = sessionStorage.getItem('selectedCompany');
        if (selectedCompanyStr) {
            try {
                const selectedCompany = JSON.parse(selectedCompanyStr);
                if (selectedCompany && selectedCompany.id) {
                    state.empresaId = String(selectedCompany.id);
                    state.empresaNombre = selectedCompany.name || '';
                }
            } catch (error) {
                console.warn('selectedCompany inválido en sessionStorage:', error);
            }
        }

        if (!state.empresaId && state.user && state.user.empresaId && state.user.empresaId !== 0) {
            state.empresaId = String(state.user.empresaId);
            state.empresaNombre = state.user.empresaNombre || '';
        }

        const rolesArrayUser = Array.isArray(state.user?.roles) ? state.user.roles : [];
        const rolesArrayConfig = Array.isArray(CONFIG.roles) ? CONFIG.roles : [];
        const combinedRoles = [...rolesArrayUser, ...rolesArrayConfig];

        const rolPrimary = state.user?.rol ?? CONFIG.rol ?? '';
        const rol = String(rolPrimary).toLowerCase().trim();
        const rolIdFromConfig = toFiniteNumberOrNull(CONFIG.rolId);
        const rolId = state.user?.rolId !== undefined && state.user?.rolId !== null
            ? Number(state.user.rolId)
            : rolIdFromConfig;

        state.isSuperAdmin = Boolean(
            (state.user && Number(state.user.empresaId) === 0) ||
            rol === 'superadmin' ||
            rolId === 1 ||
            combinedRoles.some(r => String(r).toUpperCase().includes('SUPERADMIN'))
        );

        const esGerente = (CONFIG.esGerente === true) ||
            rol === 'gerente' ||
            rol === 'manager' ||
            rolId === 3 ||
            rolId === 4 ||
            combinedRoles.some(r => {
                const upper = String(r).toUpperCase();
                return upper.includes('GERENTE') || upper.includes('MANAGER');
            });

        state.esGerente = esGerente;

        let idSucursal = extractSucursalId(state.user || null);
        if (idSucursal === null) {
            const configIdSucursal = toFiniteNumberOrNull(CONFIG.idSucursal);
            if (configIdSucursal !== null) {
                idSucursal = configIdSucursal;
            }
        }
        if (idSucursal === null) {
            try {
                const urlParams = new URLSearchParams(window.location.search);
                idSucursal = toFiniteNumberOrNull(urlParams.get('idSucursal'));
            } catch (error) {
                console.warn('No se pudo obtener idSucursal desde la URL:', error);
            }
        }

        state.idSucursal = esGerente ? idSucursal : null;
    }

    function configureFilters() {
        const hoy = new Date();
        const haceUnMes = new Date();
        haceUnMes.setDate(hoy.getDate() - 30);

        state.filters.fechaFin = formatDateInput(hoy);
        state.filters.fechaInicio = formatDateInput(haceUnMes);

        if (elements.fechaInicio) {
            elements.fechaInicio.value = state.filters.fechaInicio;
        }
        if (elements.fechaFin) {
            elements.fechaFin.value = state.filters.fechaFin;
        }
        if (elements.impuesto) {
            elements.impuesto.value = state.filters.impuesto;
        }
    }

    function applyFiltersToInputs() {
        if (elements.fechaInicio) {
            elements.fechaInicio.value = state.filters.fechaInicio || '';
        }
        if (elements.fechaFin) {
            elements.fechaFin.value = state.filters.fechaFin || '';
        }
        if (elements.estadoReserva) {
            elements.estadoReserva.value = state.filters.estadoReserva || '';
        }
        if (elements.impuesto) {
            elements.impuesto.value = state.filters.impuesto || '';
        }
    }

    function bindEvents() {
        if (elements.empresaSelect) {
            elements.empresaSelect.addEventListener('change', onEmpresaChange);
        }
        if (elements.fechaInicio) {
            elements.fechaInicio.addEventListener('change', () => handleFilterChange('fechaInicio', elements.fechaInicio.value));
        }
        if (elements.fechaFin) {
            elements.fechaFin.addEventListener('change', () => handleFilterChange('fechaFin', elements.fechaFin.value));
        }
        if (elements.estadoReserva) {
            elements.estadoReserva.addEventListener('change', () => handleFilterChange('estadoReserva', elements.estadoReserva.value));
        }
        if (elements.impuesto) {
            elements.impuesto.addEventListener('change', () => handleFilterChange('impuesto', elements.impuesto.value));
        }
        if (elements.btnRefrescar) {
            elements.btnRefrescar.addEventListener('click', () => refreshAll(true));
        }
        if (elements.btnExportarCsv) {
            elements.btnExportarCsv.addEventListener('click', exportCsv);
        }
        if (elements.btnImprimir) {
            elements.btnImprimir.addEventListener('click', printDashboard);
        }
    }

    async function preloadEmpresaSelect() {
        if (!elements.empresaSelect) {
            return;
        }

        if (!state.isSuperAdmin) {
            elements.empresaSelect.innerHTML = '';
            if (state.empresaId) {
                const option = document.createElement('option');
                option.value = state.empresaId;
                option.textContent = state.empresaNombre || `Empresa #${state.empresaId}`;
                option.selected = true;
                option.disabled = true;
                elements.empresaSelect.appendChild(option);
            } else {
                const option = document.createElement('option');
                option.value = '';
                option.textContent = 'Sin empresa asignada';
                option.disabled = true;
                option.selected = true;
                elements.empresaSelect.appendChild(option);
            }
            return;
        }

        if (!state.token) {
            showAlert('error', 'No se encontró el token de autorización. Inicia sesión nuevamente.');
            elements.empresaSelect.innerHTML = '<option value="" disabled selected>No hay token disponible</option>';
            return;
        }

        try {
            const lista = await fetchJson('/empresas', { size: 100, page: 0 });
            const empresas = normalizeCollection(lista, 'nombreEmpresa');

            elements.empresaSelect.innerHTML = '';

            const placeholder = document.createElement('option');
            placeholder.value = '';
            placeholder.disabled = true;
            placeholder.selected = !state.empresaId;
            placeholder.textContent = 'Selecciona una empresa';
            elements.empresaSelect.appendChild(placeholder);

            empresas.forEach(empresa => {
                if (!empresa || empresa.estado === 0) {
                    return;
                }
                const option = document.createElement('option');
                option.value = String(empresa.idEmpresa || empresa.id || empresa.empresaId);
                option.textContent = empresa.nombreEmpresa || empresa.nombre || empresa.razonSocial || option.value;
                option.selected = option.value === state.empresaId;
                elements.empresaSelect.appendChild(option);
            });

            if (!state.empresaId && empresas.length > 0) {
                state.empresaId = String(empresas[0].idEmpresa || empresas[0].id || empresas[0].empresaId);
                state.empresaNombre = empresas[0].nombreEmpresa || empresas[0].nombre || empresas[0].razonSocial || '';
            }
        } catch (error) {
            console.error('Error al cargar empresas:', error);
            elements.empresaSelect.innerHTML = '<option value="" disabled selected>No se pudieron cargar las empresas</option>';
            showAlert('error', 'No se pudieron cargar las empresas disponibles.');
        }
    }

    function updateEmpresaSelection() {
        if (!elements.empresaSelect || !state.empresaId) {
            return;
        }
        const option = Array.from(elements.empresaSelect.options).find(opt => opt.value === state.empresaId);
        if (option) {
            option.selected = true;
        }
    }

    function onEmpresaChange(event) {
        state.empresaId = event.target.value;
        state.empresaNombre = event.target.selectedOptions[0]?.textContent || '';
        sessionStorage.setItem('selectedCompany', JSON.stringify({ id: state.empresaId, name: state.empresaNombre }));
        refreshAll();
    }

    function handleFilterChange(filterKey, value) {
        state.filters[filterKey] = value;
        if (filterKey === 'fechaInicio' || filterKey === 'fechaFin') {
            if (!validateDateRange()) {
                return;
            }
        }
        refreshAll();
    }

    function validateDateRange() {
        const { fechaInicio, fechaFin } = state.filters;
        if (!fechaInicio || !fechaFin) {
            return true;
        }
        const inicio = new Date(fechaInicio);
        const fin = new Date(fechaFin);
        if (inicio > fin) {
            showAlert('warning', 'La fecha de inicio no puede ser mayor a la fecha fin.');
            return false;
        }
        return true;
    }

    async function refreshAll(force = false) {
        if (!state.empresaId) {
            toggleMainContent(false);
            return;
        }
        if (!state.token) {
            showAlert('error', 'No se encontró un token válido. Inicia sesión nuevamente.');
            toggleMainContent(false);
            return;
        }

        toggleMainContent(true);
        setLoading(true);

        try {
            const [reservas, ventas, clientes, personal, servicios, paquetes] = await Promise.all([
                cargarReporteReservas(),
                cargarReporteVentas(),
                cargarReporteClientes(),
                cargarReportePersonal(),
                cargarReporteServicios(),
                cargarReportePaquetes()
            ]);

            const finanzas = await cargarReporteFinanciero();

            state.cache.reservas = reservas;
            state.cache.ventas = ventas;
            state.cache.clientes = clientes;
            state.cache.personal = personal;
            state.cache.servicios = servicios;
            state.cache.paquetes = paquetes;
            state.cache.finanzas = finanzas;

            actualizarKpis({ reservas, ventas, clientes, personal, servicios, finanzas });
        } catch (error) {
            console.error('Error al refrescar los reportes:', error);
            if (!force) {
                showAlert('error', 'Ocurrió un problema al cargar los reportes.');
            }
        } finally {
            setLoading(false);
            focusMain();
        }
    }

    function toggleMainContent(hasEmpresa) {
        if (!elements.main || !elements.kpiGrid) {
            return;
        }
        elements.kpiGrid.style.display = hasEmpresa ? 'grid' : 'none';
        elements.kpiGrid.setAttribute('aria-hidden', hasEmpresa ? 'false' : 'true');
        if (elements.reportesEmpty) {
            elements.reportesEmpty.hidden = hasEmpresa;
            elements.reportesEmpty.setAttribute('aria-hidden', hasEmpresa ? 'true' : 'false');
        }
    }

    async function cargarReporteReservas() {
        const params = buildBaseParams();
        if (state.filters.estadoReserva) {
            params.estado = state.filters.estadoReserva;
        }
        try {
            const respuesta = await fetchJson('/reportes/reservas', params);
            let data = respuesta?.data || respuesta;

            if (state.esGerente && state.idSucursal !== null) {
                let reservasRaw = Array.isArray(data?.reservasRaw) ? data.reservasRaw : null;

                if (!reservasRaw || reservasRaw.length === 0) {
                    try {
                        reservasRaw = await obtenerReservasCrudasGerente();
                        console.log('ℹ️ Reservas raw obtenidas desde fallback', {
                            total: reservasRaw.length,
                            idSucursal: state.idSucursal
                        });
                    } catch (fallbackError) {
                        console.warn('No se pudieron obtener reservas crudas para el gerente:', fallbackError);
                        reservasRaw = [];
                    }
                }

                if (reservasRaw) {
                    const reservasFiltradas = filtrarReservasPorSucursal(reservasRaw, state.idSucursal);
                    console.log('🎯 Reportes reservas (gerente)', {
                        idSucursal: state.idSucursal,
                        totalApi: reservasRaw.length,
                        filtradas: reservasFiltradas.length
                    });
                    const agregados = recalcularAgregadosReservas(reservasFiltradas, data);
                    data = { ...data, ...agregados };
                }
            }

            renderReporteReservas(data);
            return data;
        } catch (error) {
            showAlert('error', 'No fue posible cargar el reporte de reservas.');
            throw error;
        }
    }

    async function obtenerReservasCrudasGerente() {
        const params = buildBaseParams();
        if (state.filters.estadoReserva) {
            params.estado = state.filters.estadoReserva;
        }
        params.idSucursal = state.idSucursal;
        params.size = 200;
        params.page = 0;

        const respuesta = await fetchJson('/reservas', params);
        const coleccion = normalizeCollection(respuesta, 'codigoReserva');
        return Array.isArray(coleccion) ? coleccion : [];
    }

    async function cargarReporteVentas() {
        const params = buildBaseParams();
        try {
            const respuesta = await fetchJson('/reportes/ventas', params);
            let data = respuesta?.data || respuesta;
            let ventasRaw = Array.isArray(data?.ventasRaw) ? data.ventasRaw : null;
            let ventasParaDocumentos = ventasRaw;

            if (state.esGerente && state.idSucursal !== null) {
                if (!ventasRaw || ventasRaw.length === 0) {
                    try {
                        ventasRaw = await obtenerVentasCrudasGerente();
                        console.log('ℹ️ Ventas raw obtenidas desde fallback', {
                            total: ventasRaw.length,
                            idSucursal: state.idSucursal
                        });
                    } catch (errorFallbackVentas) {
                        console.warn('No se pudieron obtener ventas crudas para el gerente:', errorFallbackVentas);
                        ventasRaw = [];
                    }
                }

                if (ventasRaw) {
                    const ventasFiltradas = filtrarVentasPorSucursal(ventasRaw, state.idSucursal);
                    console.log('🎯 Reportes ventas (gerente)', {
                        idSucursal: state.idSucursal,
                        totalApi: ventasRaw.length,
                        filtradas: ventasFiltradas.length
                    });
                    const agregados = recalcularAgregadosVentas(ventasFiltradas, data);
                    data = { ...data, ...agregados };
                    ventasParaDocumentos = ventasFiltradas;
                }
            }

            renderReporteVentas(data);
            await cargarVentasDocumentos(ventasParaDocumentos);
            return data;
        } catch (error) {
            showAlert('error', 'No fue posible cargar el reporte de ventas.');
            throw error;
        }
    }

    async function cargarReporteClientes() {
        const params = buildEmpresaParam();
        try {
            const respuesta = await fetchJson('/reportes/clientes', params);
            let data = respuesta?.data || respuesta;

            if (state.esGerente && state.idSucursal !== null && Array.isArray(data?.clientesRaw)) {
                const clientesFiltrados = filtrarClientesPorSucursal(data.clientesRaw, state.idSucursal);
                console.log('🎯 Reportes clientes (gerente)', {
                    idSucursal: state.idSucursal,
                    totalApi: data.clientesRaw.length,
                    filtradas: clientesFiltrados.length
                });
                const agregados = recalcularAgregadosClientes(clientesFiltrados);
                data = { ...data, ...agregados };
            }

            renderReporteClientes(data);
            return data;
        } catch (error) {
            showAlert('error', 'No fue posible cargar el reporte de clientes.');
            throw error;
        }
    }

    async function cargarReportePersonal() {
        const params = buildEmpresaParam();
        try {
            const respuesta = await fetchJson('/reportes/personal', params);
            let data = respuesta?.data || respuesta;

            if (state.esGerente && state.idSucursal !== null && Array.isArray(data?.personalRaw)) {
                const personalFiltrado = filtrarPersonalPorSucursal(data.personalRaw, state.idSucursal);
                console.log('🎯 Reportes personal (gerente)', {
                    idSucursal: state.idSucursal,
                    totalApi: data.personalRaw.length,
                    filtradas: personalFiltrado.length
                });
                const agregados = recalcularAgregadosPersonal(personalFiltrado);
                data = { ...data, ...agregados };
            }

            renderReportePersonal(data);
            return data;
        } catch (error) {
            showAlert('error', 'No fue posible cargar el reporte de personal.');
            throw error;
        }
    }

    async function cargarReporteServicios() {
        const params = buildEmpresaParam();
        try {
            const respuesta = await fetchJson('/reportes/servicios', params);
            let data = respuesta?.data || respuesta;

            if (state.esGerente && state.idSucursal !== null && Array.isArray(data?.serviciosRaw)) {
                const serviciosFiltrados = filtrarServiciosPorSucursal(data.serviciosRaw, state.idSucursal);
                console.log('🎯 Reportes servicios (gerente)', {
                    idSucursal: state.idSucursal,
                    totalApi: data.serviciosRaw.length,
                    filtradas: serviciosFiltrados.length
                });
                const agregados = recalcularAgregadosServicios(serviciosFiltrados, data);
                data = { ...data, ...agregados };
            }

            renderReporteServicios(data);
            return data;
        } catch (error) {
            showAlert('error', 'No fue posible cargar el reporte de servicios.');
            throw error;
        }
    }

    async function cargarReportePaquetes() {
        const params = buildEmpresaParam();
        try {
            const respuesta = await fetchJson('/reportes/paquetes', params);
            let data = respuesta?.data || respuesta;

            if (state.esGerente && state.idSucursal !== null && Array.isArray(data?.paquetesRaw)) {
                const paquetesFiltrados = filtrarPaquetesPorSucursal(data.paquetesRaw, state.idSucursal);
                console.log('🎯 Reportes paquetes (gerente)', {
                    idSucursal: state.idSucursal,
                    totalApi: data.paquetesRaw.length,
                    filtradas: paquetesFiltrados.length
                });
                const agregados = recalcularAgregadosPaquetes(paquetesFiltrados);
                data = { ...data, ...agregados };
            }

            renderReportePaquetes(data);
            return data;
        } catch (error) {
            showAlert('error', 'No fue posible cargar el reporte de paquetes.');
            throw error;
        }
    }

    async function cargarReporteFinanciero() {
        const paramsCaja = { ...buildEmpresaParam(), fecha: state.filters.fechaFin };
        const paramsImpuestos = buildBaseParams();
        paramsImpuestos.porcentajeImpuesto = state.filters.impuesto || '18.00';

        try {
            const [caja, impuestos] = await Promise.all([
                fetchJson('/reportes/finanzas/caja-diaria', paramsCaja),
                fetchJson('/reportes/finanzas/impuestos', paramsImpuestos)
            ]);
            const cajaData = caja?.data || caja;
            const impuestosData = impuestos?.data || impuestos;
            renderReporteFinanzas(cajaData, impuestosData);
            return { caja: cajaData, impuestos: impuestosData };
        } catch (error) {
            showAlert('error', 'No fue posible cargar el resumen financiero.');
            throw error;
        }
    }

    function renderReporteReservas(data) {
        if (!data) {
            return;
        }
        const montoFacturado = obtenerMontoFacturadoReservas(data);

        updateField('reservas-total', formatNumber(data.totalReservas || 0));
        updateField('reservas-estado', `Total facturado: ${formatCurrency(montoFacturado)}`);
        updateField('reservas-rango', formatRangoFechas(data.fechaInicio, data.fechaFin));
        updateField('reservas-monto-total', formatCurrency(montoFacturado));

        const entries = Object.entries(data.reservasPorEstado || {});
        renderTable('tablaReservasEstado', entries, value => formatNumber(value));

        const labels = entries.map(([estado]) => estado);
        const values = entries.map(([, total]) => Number(total) || 0);

        renderChart('reservasEstado', 'chartReservasEstado', 'bar', labels, values, {
            backgroundColor: buildPalette(values.length),
            borderRadius: 8
        });

    }

    function obtenerMontoFacturadoReservas(data) {
        if (!data) {
            return 0;
        }

        const directKeys = [
            'totalMontoCompletado',
            'totalMontoCompletadas',
            'montoFacturado',
            'totalFacturado',
            'montoFacturadoCompletado'
        ];

        for (const key of directKeys) {
            if (Object.prototype.hasOwnProperty.call(data, key)) {
                return toNumber(data[key]);
            }
        }

        const collections = [
            Array.isArray(data.reservasCompletadas) ? data.reservasCompletadas : null,
            Array.isArray(data.reservasRaw) ? data.reservasRaw : null,
            Array.isArray(data.reservas) ? data.reservas : null
        ];

        for (const collection of collections) {
            if (!collection || collection.length === 0) {
                continue;
            }
            let tieneCompletadas = false;
            const subtotal = collection.reduce((acum, reserva) => {
                const estado = normalizeEstado(reserva?.estado);
                if (!esEstadoCompletado(estado)) {
                    return acum;
                }
                tieneCompletadas = true;
                return acum + obtenerMontoReserva(reserva);
            }, 0);
            if (tieneCompletadas) {
                return subtotal;
            }
        }

        return toNumber(data.totalMonto || 0);
    }

    function renderReporteVentas(data) {
        if (!data) {
            return;
        }
        updateField('ventas-total', formatCurrency(data.montoTotal || 0));
        updateField('ventas-cantidad', `Ventas registradas: ${formatNumber(data.totalVentas || 0)}`);
        updateField('ventas-rango', formatRangoFechas(data.fechaInicio, data.fechaFin));

        const entries = Object.entries(data.ventasPorMetodoPago || {});
        const labels = entries.map(([metodo]) => metodo || 'Otro');
        const values = entries.map(([, total]) => Number(total) || 0);

        renderChart('ventasMetodo', 'chartVentasMetodo', 'doughnut', labels, values, {
            backgroundColor: buildPalette(values.length, 0.8)
        });

        const container = document.getElementById('resumenVentas');
        if (container) {
            container.innerHTML = '';
            if (entries.length === 0) {
                container.textContent = 'No hay ventas registradas en el período.';
            } else {
                entries.forEach(([metodo, total]) => {
                    const item = document.createElement('div');
                    item.className = 'insight-item';
                    item.innerHTML = `<span>${metodo || 'Sin método'}</span><strong>${formatNumber(total || 0)} ventas</strong>`;
                    container.appendChild(item);
                });
            }
        }

        const totalVentas = Number(data.totalVentas || 0);
        const montoTotal = Number(data.montoTotal || 0);
        const topMetodo = getTopEntry(entries);
        const metodoDescripcion = totalVentas > 0 && topMetodo.value > 0 ? `${topMetodo.key} (${formatPercent(topMetodo.value / totalVentas)})` : topMetodo.key;

        updateField('ventas-ticket-promedio', totalVentas > 0 ? formatCurrency(montoTotal / totalVentas) : formatCurrency(0));
        updateField('ventas-metodo-top', metodoDescripcion);
    }

    async function cargarVentasDocumentos(ventasPrecalculadas = null) {
        let lista = Array.isArray(ventasPrecalculadas) ? ventasPrecalculadas : null;

        if (!lista) {
            const params = buildEmpresaParam();
            if (state.filters.fechaInicio) {
                params.fechaDesde = state.filters.fechaInicio;
            }
            if (state.filters.fechaFin) {
                params.fechaHasta = state.filters.fechaFin;
            }
            params.estado = true;
            params.size = 200;
            params.page = 0;
            try {
                const respuesta = await fetchJson('/ventas', params);
                lista = normalizeCollection(respuesta, 'numeroOperacion');
            } catch (error) {
                console.warn('No fue posible obtener comprobantes de ventas:', error);
                lista = [];
            }
        }

        try {
            const ordenadas = [...lista]
                .filter(item => item)
                .sort((a, b) => {
                    const fechaA = new Date(a.fechaHora || a.createdAt || 0).getTime();
                    const fechaB = new Date(b.fechaHora || b.createdAt || 0).getTime();
                    return fechaB - fechaA;
                });
            const filtradas = state.esGerente && state.idSucursal !== null
                ? filtrarVentasPorSucursal(ordenadas, state.idSucursal)
                : ordenadas;
            state.cache.ventasDocumentos = filtradas;
            renderVentasDocumentos(filtradas);
            return filtradas;
        } catch (error) {
            console.warn('No fue posible obtener comprobantes de ventas:', error);
            state.cache.ventasDocumentos = [];
            renderVentasDocumentos([]);
            return [];
        }
    }

    async function obtenerVentasCrudasGerente() {
        const params = buildBaseParams();
        params.idSucursal = state.idSucursal;
        params.estado = true;
        params.size = 200;
        params.page = 0;
        const respuesta = await fetchJson('/ventas', params);
        const coleccion = normalizeCollection(respuesta, 'numeroOperacion');
        return Array.isArray(coleccion) ? coleccion : [];
    }

    function renderVentasDocumentos(documentos) {
        const list = document.getElementById('listaVentasDocumentos');
        const emptyMessage = document.getElementById('ventasDocumentosEmpty');
        if (!list) {
            return;
        }
        list.innerHTML = '';
        const hasDocumentos = Array.isArray(documentos) && documentos.length > 0;
        if (emptyMessage) {
            emptyMessage.hidden = hasDocumentos;
        }
        if (!hasDocumentos) {
            updateField('ventas-documento-tipo', '--');
            updateField('ventas-documento-numero', '--');
            updateField('ventas-documento-monto', '--');
            updateField('ventas-documento-fecha', '--');
            return;
        }

        documentos.forEach(venta => {
            const item = document.createElement('li');
            item.className = 'receipt-list__item';

            const docContainer = document.createElement('div');
            docContainer.className = 'receipt-list__doc';

            const tipo = document.createElement('span');
            tipo.textContent = venta.comprobante || 'Comprobante';
            docContainer.appendChild(tipo);

            const numero = document.createElement('small');
            numero.textContent = venta.numeroOperacion || 'Sin numeración';
            docContainer.appendChild(numero);

            const meta = document.createElement('div');
            meta.className = 'receipt-list__meta';

            const monto = document.createElement('span');
            monto.className = 'receipt-list__amount';
            monto.textContent = formatCurrency(venta.montoTotal || 0);
            meta.appendChild(monto);

            const fecha = document.createElement('span');
            fecha.textContent = formatDateTimeDisplay(venta.fechaHora || venta.createdAt || venta.updatedAt);
            meta.appendChild(fecha);

            const metodo = document.createElement('span');
            metodo.textContent = venta.metodoPago || 'Método no registrado';
            meta.appendChild(metodo);

            item.appendChild(docContainer);
            item.appendChild(meta);
            list.appendChild(item);
        });

        const masReciente = documentos[0];
        updateField('ventas-documento-tipo', masReciente.comprobante || 'Sin comprobante');
        updateField('ventas-documento-numero', masReciente.numeroOperacion || 'Sin numeración');
        updateField('ventas-documento-monto', formatCurrency(masReciente.montoTotal || 0));
        updateField('ventas-documento-fecha', formatDateTimeDisplay(masReciente.fechaHora || masReciente.createdAt || masReciente.updatedAt));
    }

    function renderReporteClientes(data) {
        if (!data) {
            return;
        }
        updateField('clientes-activos', formatNumber(data.clientesActivos || 0));
        updateField('clientes-total', `Total clientes: ${formatNumber(data.totalClientes || 0)}`);
        updateField('clientes-empresa', state.empresaNombre || `Empresa #${state.empresaId}`);

        const nacionalidades = Object.entries(data.clientesPorNacionalidad || {});
        const labels = nacionalidades.map(([nacionalidad]) => nacionalidad || 'Sin dato');
        const values = nacionalidades.map(([, total]) => Number(total) || 0);

        renderChart('clientesNacionalidad', 'chartClientesNacionalidad', 'doughnut', labels, values, {
            backgroundColor: buildPalette(values.length, 0.75)
        });

        const totalClientes = Number(data.totalClientes || 0);
        const clientesActivos = Number(data.clientesActivos || 0);
        const topNacionalidad = getTopEntry(nacionalidades);

        updateField('clientes-actividad-percent', totalClientes > 0 ? formatPercent(clientesActivos / totalClientes) : '0 %');
        updateField('clientes-nacionalidad-top', topNacionalidad.key);
    }

    function renderReportePersonal(data) {
        if (!data) {
            return;
        }
        updateField('personal-empresa', state.empresaNombre || `Empresa #${state.empresaId}`);

        const cargos = Object.entries(data.personalPorCargo || {});
        const etiquetas = cargos.map(([cargo]) => cargo || 'Sin cargo');
        const valores = cargos.map(([, total]) => Number(total) || 0);

        renderChart('personalCargo', 'chartPersonalCargo', 'bar', etiquetas, valores, {
            backgroundColor: buildPalette(valores.length),
            borderRadius: 8
        });

        const turnos = Object.entries(data.personalPorTurno || {});
        renderTable('tablaPersonalTurno', turnos, value => formatNumber(value));

        const totalPersonal = data.personalActivo !== undefined ? Number(data.personalActivo || 0) : valores.reduce((acc, current) => acc + current, 0);
        const topCargo = getTopEntry(cargos);
        const topTurno = getTopEntry(turnos);

        updateField('personal-total', formatNumber(totalPersonal));
        updateField('personal-cargo-top', topCargo.key);
        updateField('personal-turno-top', topTurno.key);
    }

    function renderReporteServicios(data) {
        if (!data) {
            return;
        }
        updateField('servicios-total', `${formatNumber(data.serviciosActivos || 0)} activos / ${formatNumber(data.totalServicios || 0)} totales`);

        const tipos = Object.entries(data.serviciosPorTipo || {});
        renderChart('serviciosTipo', 'chartServiciosTipo', 'doughnut', tipos.map(([tipo]) => tipo || 'Otro'), tipos.map(([, total]) => Number(total) || 0), {
            backgroundColor: buildPalette(tipos.length, 0.7)
        });

        const categorias = Object.entries(data.serviciosPorCategoria || {});
        renderTable('tablaServiciosCategoria', categorias, value => formatNumber(value));

        const topTipo = getTopEntry(tipos);

        updateField('servicios-tipo-top', topTipo.key);
        updateField('servicios-activos-resumen', formatNumber(data.serviciosActivos || 0));
    }

    function renderReportePaquetes(data) {
        if (!data) {
            return;
        }
        updateField('paquetes-total', `${formatNumber(data.paquetesActivos || 0)} activos / ${formatNumber(data.totalPaquetes || 0)} totales`);

        const container = document.getElementById('resumenPaquetes');
        if (container) {
            container.innerHTML = '';
            const items = [
                { label: 'Paquetes con promoción', value: formatNumber(data.paquetesConPromocion || 0) },
                { label: 'Precio promedio', value: formatCurrency(data.precioPromedio || 0) }
            ];
            items.forEach(({ label, value }) => {
                const item = document.createElement('div');
                item.className = 'insight-item';
                item.innerHTML = `<span>${label}</span><strong>${value}</strong>`;
                container.appendChild(item);
            });
        }

        const tableData = [
            ['Total paquetes', formatNumber(data.totalPaquetes || 0)],
            ['Paquetes activos', formatNumber(data.paquetesActivos || 0)],
            ['Paquetes con promoción', formatNumber(data.paquetesConPromocion || 0)],
            ['Precio promedio', formatCurrency(data.precioPromedio || 0)]
        ];
        renderTable('tablaPaquetesPromocion', tableData, value => value, true);

        const totalPaquetes = Number(data.totalPaquetes || 0);
        const conPromocion = Number(data.paquetesConPromocion || 0);

        updateField('paquetes-promocion-percent', totalPaquetes > 0 ? formatPercent(conPromocion / totalPaquetes) : '0 %');
        updateField('paquetes-precio-promedio', formatCurrency(data.precioPromedio || 0));
        updateField('paquetes-activos-resumen', formatNumber(data.paquetesActivos || 0));
    }

    function renderReporteFinanzas(cajaData, impuestosData) {
        if (!cajaData && !impuestosData) {
            return;
        }

        const labels = ['Ingresos', 'Egresos', 'Saldo actual', 'Impuestos', 'Neto'];

        if (cajaData) {
            updateField('finanzas-ingresos', formatCurrency(cajaData.ingresos || 0));
            updateField('finanzas-egresos', formatCurrency(cajaData.egresos || 0));
        } else {
            updateField('finanzas-ingresos', '--');
            updateField('finanzas-egresos', '--');
        }

        if (impuestosData) {
            updateField('finanzas-impuestos', formatCurrency(impuestosData.impuestos || 0));
            updateField('finanzas-neto', formatCurrency(impuestosData.neto || 0));
        } else {
            updateField('finanzas-impuestos', '--');
            updateField('finanzas-neto', '--');
        }

        if (cajaData) {
            updateField('finanzas-saldo', formatCurrency(cajaData.saldoActual || 0));
            updateField('finanzas-cajas', `Cajas analizadas: ${formatNumber(cajaData.totalCajas || 0)}`);
            const fechaReferencia = cajaData.fecha || cajaData.fechaCierre || cajaData.fechaApertura || impuestosData?.fechaFin || impuestosData?.fechaInicio;
            updateField('caja-fecha', formatDateDisplay(fechaReferencia));
            updateField('caja-total-cajas', formatNumber(cajaData.totalCajas || 0));
            updateField('caja-monto-inicial', formatCurrency(cajaData.montoInicial || 0));
            updateField('caja-ingresos', formatCurrency(cajaData.ingresos || 0));
            updateField('caja-egresos', formatCurrency(cajaData.egresos || 0));
            updateField('caja-saldo-actual', formatCurrency(cajaData.saldoActual || 0));
            const variacion = Number(cajaData.saldoActual || 0) - Number(cajaData.montoInicial || 0);
            updateField('caja-variacion', formatCurrency(variacion));
        } else {
            updateField('finanzas-saldo', '--');
            updateField('finanzas-cajas', 'Cajas analizadas: 0');
            updateField('caja-fecha', '--');
            updateField('caja-total-cajas', '--');
            updateField('caja-monto-inicial', '--');
            updateField('caja-ingresos', '--');
            updateField('caja-egresos', '--');
            updateField('caja-saldo-actual', '--');
            updateField('caja-variacion', '--');
        }

        if (!impuestosData) {
            updateField('finanzas-rango', '--');
            renderTable('tablaFinanzas', [], value => value, true);
            const emptyValues = [
                Number(cajaData?.ingresos || 0),
                Number(cajaData?.egresos || 0),
                Number(cajaData?.saldoActual || 0),
                0,
                0
            ];
            renderChart('finanzasIngresos', 'chartFinanzasIngresos', 'bar', labels, emptyValues, {
                backgroundColor: buildPalette(emptyValues.length),
                borderRadius: 6
            });
            return;
        }

        updateField('finanzas-rango', formatRangoFechas(impuestosData.fechaInicio, impuestosData.fechaFin));

        const tableData = [
            ['Monto inicial', formatCurrency(cajaData?.montoInicial || 0)],
            ['Ingresos', formatCurrency(cajaData?.ingresos || 0)],
            ['Egresos', formatCurrency(cajaData?.egresos || 0)],
            ['Saldo actual', formatCurrency(cajaData?.saldoActual || 0)],
            ['Saldo calculado', formatCurrency(cajaData?.saldoCalculado || 0)],
            ['Ventas registradas', formatNumber(impuestosData.cantidadVentas || 0)],
            ['Total ventas', formatCurrency(impuestosData.totalVentas || 0)],
            ['Impuestos estimados', formatCurrency(impuestosData.impuestos || 0)],
            ['Neto después de impuestos', formatCurrency(impuestosData.neto || 0)]
        ];
        renderTable('tablaFinanzas', tableData, value => value, true);

        const values = [
            Number(cajaData?.ingresos || 0),
            Number(cajaData?.egresos || 0),
            Number(cajaData?.saldoActual || 0),
            Number(impuestosData.impuestos || 0),
            Number(impuestosData.neto || 0)
        ];

        renderChart('finanzasIngresos', 'chartFinanzasIngresos', 'bar', labels, values, {
            backgroundColor: buildPalette(values.length),
            borderRadius: 6
        });
    }

    function renderTable(tableId, entries, formatter, skipFormatter = false) {
        const table = document.getElementById(tableId);
        if (!table) {
            return;
        }
        const tbody = table.querySelector('tbody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = '';
        const rows = Array.isArray(entries) ? entries : Object.entries(entries || {});
        if (!rows.length) {
            const tr = document.createElement('tr');
            const td = document.createElement('td');
            td.colSpan = table.querySelectorAll('thead th').length || 2;
            td.textContent = 'Sin datos disponibles para el período seleccionado.';
            td.style.textAlign = 'center';
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        rows.forEach(row => {
            const [label, value] = Array.isArray(row) ? row : [row, entries[row]];
            const tr = document.createElement('tr');
            const tdLabel = document.createElement('td');
            tdLabel.textContent = label || 'Sin dato';
            const tdValue = document.createElement('td');
            tdValue.textContent = skipFormatter ? value : formatter(value);
            tr.appendChild(tdLabel);
            tr.appendChild(tdValue);
            tbody.appendChild(tr);
        });
    }

    function renderChart(key, canvasId, type, labels, values, datasetOptions = {}) {
        if (typeof Chart === 'undefined') {
            return;
        }
        const canvas = document.getElementById(canvasId);
        if (!canvas) {
            return;
        }

        const previous = state.charts[key];
        if (previous) {
            const canvasChanged = previous.canvas !== canvas;
            const canvasDetached = previous.canvas && !previous.canvas.isConnected;
            if (canvasChanged || canvasDetached) {
                try {
                    previous.destroy();
                } catch (destroyError) {
                    console.warn('⚠️ Error al destruir gráfico previo', key, destroyError);
                }
                delete state.charts[key];
            }
        }

        if (state.charts[key]) {
            state.charts[key].data.labels = labels;
            state.charts[key].data.datasets[0].data = values;
            Object.assign(state.charts[key].data.datasets[0], datasetOptions);
            state.charts[key].update();
            return;
        }

        const context = canvas.getContext ? canvas.getContext('2d') : null;
        if (!context) {
            console.warn('⚠️ No se pudo obtener el contexto del canvas para', key);
            return;
        }

        state.charts[key] = new Chart(context, {
            type,
            data: {
                labels,
                datasets: [{
                    label: 'Total',
                    data: values,
                    ...datasetOptions
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: type === 'bar' ? 'top' : 'bottom' }
                },
                scales: type === 'bar' ? {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: value => formatNumber(value)
                        }
                    }
                } : {}
            }
        });
    }

    function actualizarKpis(data) {
        if (!data) {
            return;
        }
        if (data.reservas) {
            updateField('reservas-total', formatNumber(data.reservas.totalReservas || 0));
        }
        if (data.ventas) {
            updateField('ventas-total', formatCurrency(data.ventas.montoTotal || 0));
        }
        if (data.clientes) {
            updateField('clientes-activos', formatNumber(data.clientes.clientesActivos || 0));
        }
        if (data.finanzas?.caja) {
            updateField('finanzas-saldo', formatCurrency(data.finanzas.caja.saldoActual || 0));
        }
    }

    function exportCsv() {
        const cache = state.cache;
        if (!cache || !cache.reservas) {
            showAlert('warning', 'Primero carga los reportes para poder exportarlos.');
            return;
        }
        const rows = [];
        const addRow = (section, key, value) => rows.push([section, key, value]);

        addRow('Reservas', 'Total', formatNumber(cache.reservas?.totalReservas || 0));
        Object.entries(cache.reservas?.reservasPorEstado || {}).forEach(([estado, total]) => {
            addRow('Reservas por estado', estado, formatNumber(total));
        });

        addRow('Ventas', 'Monto total', formatCurrency(cache.ventas?.montoTotal || 0));
        Object.entries(cache.ventas?.ventasPorMetodoPago || {}).forEach(([metodo, total]) => {
            addRow('Ventas por método', metodo, formatNumber(total));
        });
        const ultimoComprobante = Array.isArray(cache.ventasDocumentos) ? cache.ventasDocumentos[0] : null;
        if (ultimoComprobante) {
            const descriptor = [ultimoComprobante.comprobante || 'Comprobante', ultimoComprobante.numeroOperacion || ''].join(' ').trim();
            addRow('Ventas', 'Último comprobante', descriptor);
            addRow('Ventas', 'Monto último comprobante', formatCurrency(ultimoComprobante.montoTotal || 0));
        }

        addRow('Clientes', 'Clientes activos', formatNumber(cache.clientes?.clientesActivos || 0));
        addRow('Personal', 'Personal activo', formatNumber(cache.personal?.personalActivo || 0));
        Object.entries(cache.personal?.personalPorCargo || {}).forEach(([cargo, total]) => {
            addRow('Personal por cargo', cargo, formatNumber(total));
        });

        addRow('Servicios', 'Servicios activos', formatNumber(cache.servicios?.serviciosActivos || 0));
        Object.entries(cache.servicios?.serviciosPorTipo || {}).forEach(([tipo, total]) => {
            addRow('Servicios por tipo', tipo, formatNumber(total));
        });

        addRow('Paquetes', 'Paquetes activos', formatNumber(cache.paquetes?.paquetesActivos || 0));
        addRow('Paquetes', 'Con promoción', formatNumber(cache.paquetes?.paquetesConPromocion || 0));
        addRow('Paquetes', 'Precio promedio', formatCurrency(cache.paquetes?.precioPromedio || 0));

        addRow('Finanzas', 'Ingresos (caja)', formatCurrency(cache.finanzas?.caja?.ingresos || 0));
        addRow('Finanzas', 'Egresos (caja)', formatCurrency(cache.finanzas?.caja?.egresos || 0));
        addRow('Finanzas', 'Saldo actual', formatCurrency(cache.finanzas?.caja?.saldoActual || 0));
        addRow('Finanzas', 'Impuestos estimados', formatCurrency(cache.finanzas?.impuestos?.impuestos || 0));
        addRow('Finanzas', 'Neto', formatCurrency(cache.finanzas?.impuestos?.neto || 0));

        const csvContent = rows.map(row => row.map(value => `"${String(value).replace(/"/g, '""')}"`).join(',')).join('\n');
        const blob = new Blob([`Sección,Indicador,Valor\n${csvContent}`], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        const empresaSlug = state.empresaNombre ? state.empresaNombre.replace(/[^a-z0-9]+/gi, '_').toLowerCase() : `empresa_${state.empresaId}`;
        link.download = `reportes_${empresaSlug}_${state.filters.fechaInicio}_${state.filters.fechaFin}.csv`;
        link.click();
        URL.revokeObjectURL(url);

        showAlert('success', 'Se generó el archivo CSV con los indicadores principales.');
    }

    function printDashboard() {
        window.print();
    }

    function buildBaseParams() {
        const params = buildEmpresaParam();
        if (state.filters.fechaInicio) {
            params.fechaInicio = state.filters.fechaInicio;
        }
        if (state.filters.fechaFin) {
            params.fechaFin = state.filters.fechaFin;
        }
        return params;
    }

    function buildEmpresaParam() {
        const params = {};
        if (state.empresaId) {
            params.empresaId = state.empresaId;
        }
        if (state.esGerente && state.idSucursal !== null) {
            params.idSucursal = state.idSucursal;
        }
        return params;
    }

    async function fetchJson(path, params = {}) {
        if (!state.token) {
            throw new Error('Token no disponible');
        }
        const url = new URL(`${API_BASE}${path}`);
        Object.entries(params).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                url.searchParams.append(key, value);
            }
        });
        const response = await fetch(url.toString(), {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            }
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(`HTTP ${response.status}: ${text}`);
        }
        return response.json();
    }

    function normalizeCollection(response, nameKey) {
        if (!response) {
            return [];
        }
        if (Array.isArray(response)) {
            return response;
        }
        if (Array.isArray(response.data)) {
            return response.data;
        }
        if (Array.isArray(response.content)) {
            return response.content;
        }
        if (Array.isArray(response.items)) {
            return response.items;
        }
        if (typeof response === 'object') {
            return Object.values(response).filter(item => typeof item === 'object' && item && (item[nameKey] || item.id));
        }
        return [];
    }

    function updateField(fieldName, value) {
        if (!fieldName) {
            return;
        }
        document.querySelectorAll(`[data-field="${fieldName}"]`).forEach(el => {
            el.textContent = value;
        });
    }

    function formatDateInput(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    }

    function formatCurrency(value) {
        const numberValue = Number(value || 0);
        return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', minimumFractionDigits: 2 }).format(numberValue);
    }

    function formatNumber(value) {
        const numberValue = Number(value || 0);
        return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 0 }).format(numberValue);
    }

    function formatNumberWithDecimals(value, maximumFractionDigits = 1) {
        const numberValue = Number(value || 0);
        return new Intl.NumberFormat('es-PE', { minimumFractionDigits: 0, maximumFractionDigits }).format(numberValue);
    }

    function formatPercent(value, maximumFractionDigits = 1) {
        const numberValue = Number(value || 0) * 100;
        if (!Number.isFinite(numberValue)) {
            return '0 %';
        }
        return `${new Intl.NumberFormat('es-PE', { minimumFractionDigits: 0, maximumFractionDigits }).format(numberValue)} %`;
    }

    function formatDateDisplay(fecha) {
        if (!fecha) {
            return '--';
        }
        const parsed = new Date(fecha);
        if (Number.isNaN(parsed.getTime())) {
            return fecha;
        }
        return parsed.toLocaleDateString('es-PE', { year: 'numeric', month: 'short', day: 'numeric' });
    }

    function formatDateTimeDisplay(fecha) {
        if (!fecha) {
            return '--';
        }
        const parsed = new Date(fecha);
        if (Number.isNaN(parsed.getTime())) {
            return fecha;
        }
        return parsed.toLocaleString('es-PE', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function getTopEntry(entries) {
        if (!Array.isArray(entries) || entries.length === 0) {
            return { key: 'Sin datos', value: 0 };
        }
        let topKey = entries[0][0] || 'Sin datos';
        let topValue = Number(entries[0][1]) || 0;
        entries.forEach(([key, value]) => {
            const numeric = Number(value) || 0;
            if (numeric > topValue) {
                topKey = key || 'Sin datos';
                topValue = numeric;
            }
        });
        return { key: topKey || 'Sin datos', value: topValue };
    }

    function formatRangoFechas(inicio, fin) {
        if (!inicio && !fin) {
            return 'Sin rango definido';
        }
        const formato = (fecha) => {
            if (!fecha) {
                return '---';
            }
            const parsed = new Date(fecha);
            if (Number.isNaN(parsed.getTime())) {
                return fecha;
            }
            return parsed.toLocaleDateString('es-PE', { year: 'numeric', month: 'short', day: 'numeric' });
        };
        return `${formato(inicio)} - ${formato(fin)}`;
    }

    function buildPalette(length, alpha = 1) {
        const baseColors = [
            [37, 99, 235],
            [16, 185, 129],
            [236, 72, 153],
            [249, 115, 22],
            [14, 165, 233],
            [107, 114, 128],
            [124, 58, 237],
            [220, 38, 38],
            [8, 145, 178]
        ];
        const colors = [];
        for (let i = 0; i < length; i += 1) {
            const [r, g, b] = baseColors[i % baseColors.length];
            colors.push(`rgba(${r}, ${g}, ${b}, ${alpha})`);
        }
        return colors;
    }

    function filtrarReservasPorSucursal(reservas, sucursalId) {
        const targetId = toFiniteNumberOrNull(sucursalId);
        if (!Array.isArray(reservas) || targetId === null) {
            return reservas;
        }
        return reservas.filter(reserva => {
            const sucursal = extractSucursalId(reserva);
            const coincide = sucursal !== null && sucursal === targetId;
            if (!coincide) {
                debugSucursalMismatch('reserva', reserva, sucursal, targetId);
            }
            return coincide;
        });
    }

    function filtrarVentasPorSucursal(ventas, sucursalId) {
        const targetId = toFiniteNumberOrNull(sucursalId);
        if (!Array.isArray(ventas) || targetId === null) {
            return ventas;
        }
        return ventas.filter(venta => {
            const sucursal = extractSucursalId(venta);
            const coincide = sucursal !== null && sucursal === targetId;
            if (!coincide) {
                debugSucursalMismatch('venta', venta, sucursal, targetId);
            }
            return coincide;
        });
    }

    function filtrarClientesPorSucursal(clientes, sucursalId) {
        const targetId = toFiniteNumberOrNull(sucursalId);
        if (!Array.isArray(clientes) || targetId === null) {
            return clientes;
        }
        return clientes.filter(cliente => {
            const sucursal = extractSucursalId(cliente);
            const coincide = sucursal !== null && sucursal === targetId;
            if (!coincide) {
                debugSucursalMismatch('cliente', cliente, sucursal, targetId);
            }
            return coincide;
        });
    }

    function filtrarPersonalPorSucursal(personal, sucursalId) {
        const targetId = toFiniteNumberOrNull(sucursalId);
        if (!Array.isArray(personal) || targetId === null) {
            return personal;
        }
        return personal.filter(persona => {
            const sucursal = extractSucursalId(persona);
            const coincide = sucursal !== null && sucursal === targetId;
            if (!coincide) {
                debugSucursalMismatch('personal', persona, sucursal, targetId);
            }
            return coincide;
        });
    }

    function filtrarServiciosPorSucursal(servicios, sucursalId) {
        const targetId = toFiniteNumberOrNull(sucursalId);
        if (!Array.isArray(servicios) || targetId === null) {
            return servicios;
        }
        return servicios.filter(servicio => {
            const sucursal = extractSucursalId(servicio);
            const coincide = sucursal !== null && sucursal === targetId;
            if (!coincide) {
                debugSucursalMismatch('servicio', servicio, sucursal, targetId);
            }
            return coincide;
        });
    }

    function filtrarPaquetesPorSucursal(paquetes, sucursalId) {
        const targetId = toFiniteNumberOrNull(sucursalId);
        if (!Array.isArray(paquetes) || targetId === null) {
            return paquetes;
        }
        return paquetes.filter(paquete => {
            const sucursal = extractSucursalId(paquete);
            const coincide = sucursal !== null && sucursal === targetId;
            if (!coincide) {
                debugSucursalMismatch('paquete', paquete, sucursal, targetId);
            }
            return coincide;
        });
    }

    function recalcularAgregadosReservas(reservas, baseData = {}) {
        const totalReservas = reservas.length;
        const reservasPorEstado = {};
        let totalMonto = 0;
        let totalMontoCompletado = 0;

        reservas.forEach(reserva => {
            const estado = normalizeEstado(reserva?.estado) || 'Desconocido';
            reservasPorEstado[estado] = (reservasPorEstado[estado] || 0) + 1;

            const monto = obtenerMontoReserva(reserva);
            totalMonto += monto;
            if (esEstadoCompletado(estado)) {
                totalMontoCompletado += monto;
            }
        });

        return {
            reservasRaw: reservas,
            totalReservas,
            reservasPorEstado,
            totalMonto,
            totalMontoCompletado
        };
    }

    function recalcularAgregadosVentas(ventas, baseData = {}) {
        const totalVentas = ventas.length;
        const montoTotal = ventas.reduce((acum, venta) => acum + toNumber(venta?.montoTotal), 0);
        const ventasPorMetodoPago = ventas.reduce((acc, venta) => {
            const metodo = (venta?.metodoPago || 'Desconocido').toString().trim() || 'Desconocido';
            acc[metodo] = (acc[metodo] || 0) + 1;
            return acc;
        }, {});

        return {
            ventasRaw: ventas,
            totalVentas,
            montoTotal,
            ventasPorMetodoPago
        };
    }

    function recalcularAgregadosClientes(clientes) {
        const totalClientes = clientes.length;
        const clientesActivos = clientes.filter(cliente => Boolean(cliente?.estado)).length;
        const clientesPorNacionalidad = clientes.reduce((acc, cliente) => {
            const nacionalidad = (cliente?.nacionalidad || 'No especificada').toString();
            acc[nacionalidad] = (acc[nacionalidad] || 0) + 1;
            return acc;
        }, {});
        const clientesPorNivelMembresia = clientes.reduce((acc, cliente) => {
            const nivel = (cliente?.nivelMembresia || 'Bronce').toString();
            acc[nivel] = (acc[nivel] || 0) + 1;
            return acc;
        }, {});

        return {
            clientesRaw: clientes,
            totalClientes,
            clientesActivos,
            clientesPorNacionalidad,
            clientesPorNivelMembresia
        };
    }

    function recalcularAgregadosPersonal(personal) {
        const totalPersonal = personal.length;
        const personalActivo = personal.filter(persona => Boolean(persona?.estado)).length;
        const personalPorCargo = personal.reduce((acc, persona) => {
            const cargo = (persona?.cargo || persona?.cargoNombre || 'Desconocido').toString();
            acc[cargo] = (acc[cargo] || 0) + 1;
            return acc;
        }, {});
        const personalPorTurno = personal.reduce((acc, persona) => {
            const turno = (persona?.turno || persona?.turnoNombre || 'Sin asignar').toString();
            acc[turno] = (acc[turno] || 0) + 1;
            return acc;
        }, {});

        return {
            personalRaw: personal,
            totalPersonal,
            personalActivo,
            personalPorCargo,
            personalPorTurno
        };
    }

    function recalcularAgregadosServicios(servicios, baseData = {}) {
        const totalServicios = servicios.length;
        const serviciosActivos = servicios.filter(servicio => Boolean(servicio?.estado) || servicio?.estado === 1).length;
        const serviciosPorTipo = servicios.reduce((acc, servicio) => {
            const tipo = (servicio?.tipoServicio || 'Desconocido').toString();
            acc[tipo] = (acc[tipo] || 0) + 1;
            return acc;
        }, {});
        const serviciosPorCategoria = servicios.reduce((acc, servicio) => {
            const categoria = obtenerNombreCategoria(servicio, baseData?.serviciosPorCategoria);
            acc[categoria] = (acc[categoria] || 0) + 1;
            return acc;
        }, {});

        return {
            serviciosRaw: servicios,
            totalServicios,
            serviciosActivos,
            serviciosPorTipo,
            serviciosPorCategoria
        };
    }

    function recalcularAgregadosPaquetes(paquetes) {
        const totalPaquetes = paquetes.length;
        const paquetesActivos = paquetes.filter(paquete => Boolean(paquete?.estado)).length;
        const paquetesConPromocion = paquetes.filter(paquete => Boolean(paquete?.promocion)).length;
        const precioPromedio = paquetes.length
            ? paquetes.reduce((acum, paquete) => acum + toNumber(paquete?.precioTotal), 0) / paquetes.length
            : 0;

        return {
            paquetesRaw: paquetes,
            totalPaquetes,
            paquetesActivos,
            paquetesConPromocion,
            precioPromedio
        };
    }

    function extractSucursalId(entity, visited = new WeakSet()) {
        if (!entity || typeof entity !== 'object') {
            return null;
        }
        if (visited.has(entity)) {
            return null;
        }
        visited.add(entity);

        const directFields = ['idSucursal', 'sucursalId', 'idsucursal'];
        for (const field of directFields) {
            if (Object.prototype.hasOwnProperty.call(entity, field)) {
                const parsed = toFiniteNumberOrNull(entity[field]);
                if (parsed !== null) {
                    return parsed;
                }
            }
        }

        if (entity.sucursal && typeof entity.sucursal === 'object') {
            const nested = extractSucursalId(entity.sucursal, visited);
            if (nested !== null) {
                return nested;
            }
        }

        const fallbackKeys = ['usuario', 'personal', 'venta', 'movimientoCaja', 'caja', 'detalle', 'reserva', 'responsable'];
        for (const key of fallbackKeys) {
            const nested = entity[key];
            if (nested && typeof nested === 'object') {
                const nestedId = extractSucursalId(nested, visited);
                if (nestedId !== null) {
                    return nestedId;
                }
            }
        }

        if (entity.id !== undefined && entity.id !== null) {
            const heuristicKeys = ['nombreSucursal', 'codigoSucursal', 'direccionSucursal'];
            const hasSucursalContext = heuristicKeys.some(prop => Object.prototype.hasOwnProperty.call(entity, prop));
            if (hasSucursalContext) {
                const parsed = toFiniteNumberOrNull(entity.id);
                if (parsed !== null) {
                    return parsed;
                }
            }
        }

        return null;
    }

    function debugSucursalMismatch(tipo, entidad, encontrado, esperado) {
        if (!state.esGerente || state.idSucursal === null) {
            return;
        }
        if (!state._debugMismatchCounters) {
            state._debugMismatchCounters = {};
        }
        state._debugMismatchCounters[tipo] = (state._debugMismatchCounters[tipo] || 0) + 1;
        const contador = state._debugMismatchCounters[tipo];
        if (contador <= 5) {
            console.warn(`⚠️ ${tipo} descartado por sucursal distinta`, {
                esperado,
                encontrado,
                entidad
            });
        }
    }

    function normalizeEstado(estado) {
        if (estado == null) {
            return null;
        }
        if (typeof estado === 'string') {
            return estado;
        }
        if (typeof estado === 'object' && estado.name) {
            return estado.name;
        }
        return String(estado);
    }

    function esEstadoCompletado(estado) {
        if (!estado) {
            return false;
        }
        const normalized = String(estado).toLowerCase().trim();
        if (normalized.startsWith('complet')) {
            return true;
        }
        if (normalized.startsWith('finaliz')) {
            return true;
        }
        return normalized === 'cerrada' || normalized === 'cerrado';
    }

    function toNumber(value) {
        if (value == null) {
            return 0;
        }
        const numeric = Number(value);
        return Number.isFinite(numeric) ? numeric : 0;
    }

    function obtenerMontoReserva(reserva) {
        if (!reserva || typeof reserva !== 'object') {
            return 0;
        }

        const valor = reserva.precioTotal ??
            reserva.montoTotal ??
            reserva.monto ??
            reserva.total ??
            reserva.totalPagar ??
            reserva.totalAPagar ??
            reserva.montoReserva ??
            reserva.total_pagado ??
            reserva.montoPagado ??
            0;

        return toNumber(valor);
    }

    function obtenerNombreCategoria(servicio, categoriasBase) {
        const nombreDirecto = servicio?.categoria?.nombreCategoria
            || servicio?.categoria?.nombre
            || servicio?.nombreCategoria
            || servicio?.categoriaNombre;
        if (nombreDirecto) {
            return String(nombreDirecto);
        }

        if (servicio?.idCategoria != null && categoriasBase && typeof categoriasBase === 'object') {
            const id = Number(servicio.idCategoria);
            const coincidencia = Object.keys(categoriasBase).find(nombre => nombre.includes(`#${id}`));
            if (coincidencia) {
                return coincidencia;
            }
        }

        if (servicio?.idCategoria != null) {
            return `Categoría #${servicio.idCategoria}`;
        }
        return 'Sin categoría';
    }

    function focusMain() {
        if (!elements.main) {
            return;
        }
        requestAnimationFrame(() => {
            elements.main.focus({ preventScroll: true });
        });
    }

    function showAlert(type, message, duration = 5000) {
        if (!elements.alertasContainer || !message) {
            return;
        }
        const alerta = document.createElement('div');
        alerta.className = `alerta alerta-${type}`;
        alerta.setAttribute('role', 'alert');
        alerta.setAttribute('aria-live', 'assertive');
        alerta.setAttribute('aria-atomic', 'true');
        alerta.innerHTML = `
            <div class="alerta__content">
                <span>${message}</span>
            </div>
            <button class="cerrar-alerta" type="button" aria-label="Cerrar alerta"><i class="fas fa-times" aria-hidden="true"></i></button>
        `;
        elements.alertasContainer.appendChild(alerta);
        setTimeout(() => alerta.classList.add('alerta-visible'), 10);
        const remover = () => {
            alerta.classList.remove('alerta-visible');
            setTimeout(() => alerta.remove(), 300);
        };
        alerta.querySelector('.cerrar-alerta')?.addEventListener('click', remover);
        if (duration > 0) {
            setTimeout(remover, duration);
        }
    }

    function setLoading(isLoading) {
        if (!elements.main) {
            return;
        }
        elements.main.style.cursor = isLoading ? 'progress' : 'default';
        elements.main.setAttribute('aria-busy', isLoading ? 'true' : 'false');
        const toggleButtons = [elements.btnRefrescar, elements.btnExportarCsv];
        toggleButtons.forEach(button => {
            if (!button) {
                return;
            }
            if (isLoading) {
                button.setAttribute('aria-disabled', 'true');
                button.setAttribute('disabled', 'disabled');
            } else {
                button.removeAttribute('aria-disabled');
                button.removeAttribute('disabled');
            }
        });
        if (isLoading) {
            elements.main.classList.add('loading');
        } else {
            elements.main.classList.remove('loading');
        }
    }

    window.ReportesDashboard = {
        mount(options = {}) {
            const preserveFilters = options.preserveFilters ?? isMounted;
            const resetFilters = options.resetFilters ?? !preserveFilters;
            const forceRefresh = options.forceRefresh ?? true;
            return scheduleInit({ preserveFilters, resetFilters, forceRefresh });
        },
        refresh(force = false) {
            return refreshAll(force);
        },
        focus: focusMain,
        get isMounted() {
            return isMounted;
        },
        get state() {
            return state;
        }
    };
})();
