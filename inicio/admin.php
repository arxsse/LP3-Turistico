<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Administrador</title>
    <link rel="stylesheet" href="web.css">
    <link rel="stylesheet" href="alertas.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
</head>
<body class="admin-body fondo-gris">
    <!-- Menú Lateral -->
    <aside class="admin-sidebar" id="sidebar">
        <div class="sidebar-header">
            <h2 class="sidebar-title">XXXXXXX</h2>
        </div>
        <nav class="sidebar-nav">
            <a href="#reservas" class="sidebar-link" data-section="reservas">
                <i class="fas fa-calendar-check"></i>
                Reservas
            </a>
            <a href="#reportes" class="sidebar-link" data-section="reportes">
                <i class="fas fa-chart-pie"></i>
                Reportes
            </a>
        </nav>
        <button class="logout-btn logout-btn-sidebar" data-logout>
            Cerrar Sesión
        </button>
    </aside>

    <!-- Contenido Principal -->
    <main class="admin-main-content" id="mainContent">
        <!-- Header -->
        <header class="admin-header">
            <div class="header-left">
                <button class="sidebar-toggle" id="sidebarToggle">
                    <i class="fas fa-bars"></i>
                </button>
                <h1 class="page-title" id="pageTitle"> Dashboard </h1>
            </div>
            <div class="header-right">
                <div class="user-profile">
                    <div class="profile-info">
                        <div class="profile-image" style="width: 45px; height: 45px; border-radius: 50%; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); display: flex; align-items: center; justify-content: center; color: white; font-weight: bold; font-size: 18px;">
                            <i class="fas fa-user"></i>
                        </div>
                        <div class="user-info">
                            <span class="user-name" id="admin-username">Cargando...</span>
                            <span class="user-role" id="admin-userrole">Cargando...</span>
                        </div>
                    </div>
                </div>
            </div>
        </header>

        <!-- Contenido -->
        <div class="admin-content">

            <!-- Sección Reservas -->
            <section id="reservas-section" class="content-section">
            </section>

            <!-- Sección Reportes -->
            <section id="reportes-section" class="content-section">
            </section>

        </div>
    </main>

    <!-- Contenedor de Alertas -->
    <div id="alertContainer"></div>

    <script>
        // Validar acceso - Verificar sesión y rol
        (function() {
            const userData = sessionStorage.getItem('userData') || localStorage.getItem('userData');
            
            if (!userData) {
                // No hay sesión, redirigir al login
                alert('Debes iniciar sesión para acceder a esta página');
                window.location.href = 'login.php';
                return;
            }

            try {
                const user = JSON.parse(userData);
                
                // PRIORIDAD MÁXIMA: Verificar si el correo es bry@gmail.com (acceso directo a superadmin)
                const emailUsuario = String(user.email || '').toLowerCase().trim();
                const esEmailSuperAdmin = emailUsuario === 'bry@gmail.com';
                
                if (esEmailSuperAdmin) {
                    console.log('✅ Acceso directo a SUPERADMIN detectado por correo: bry@gmail.com - Redirigiendo desde admin.php');
                    window.location.href = 'superadmin.php';
                    return;
                }
                
                // Verificar si el usuario es superadmin (solo por rol, sin considerar empresa)
                const rolId = user.rolId ? parseInt(user.rolId) : null;
                const rol = (user.rol || '').toLowerCase();
                const rolesArray = Array.isArray(user.roles) ? user.roles : [];
                const tieneRolSuperAdmin = rolesArray.some(r => {
                    const rolStr = String(r).toUpperCase();
                    return rolStr.includes('SUPERADMIN') || 
                           rolStr.includes('SUPER_ADMIN') ||
                           rolStr === 'ROLE_SUPERADMINISTRADOR' ||
                           rolStr === 'ROLE_SUPERADMIN';
                });
                
                const esSuperAdmin = rolId === 1 || rol === 'superadmin' || tieneRolSuperAdmin;
                
                // Si el usuario es superadmin, redirigir a superadmin.php (sin considerar empresa)
                if (esSuperAdmin) {
                    console.log(`Usuario es SUPERADMIN - Redirigiendo a superadmin.php desde admin.php`);
                    window.location.href = 'superadmin.php';
                    return;
                }
            } catch (e) {
                console.error('Error al validar usuario:', e);
                window.location.href = 'login.php';
                return;
            }
        })();

        // Leer empresa seleccionada (desde superadmin o desde login del admin)
        async function loadSelectedCompany() {
            try {
                console.log('🔍 Iniciando loadSelectedCompany...');
                const params = new URLSearchParams(window.location.search);
                let idEmpresa = params.get('idEmpresa');
                
                // Buscar el elemento sidebar-title con múltiples selectores
                let sidebarTitle = document.querySelector('.sidebar-title') 
                    || document.querySelector('[class*="sidebar-title"]')
                    || document.querySelector('[class*="title"]');
                
                // Si no se encuentra, intentar después de un pequeño delay
                if (!sidebarTitle) {
                    console.warn('⚠️ No se encontró sidebar-title inmediatamente, esperando 100ms...');
                    await new Promise(resolve => setTimeout(resolve, 100));
                    sidebarTitle = document.querySelector('.sidebar-title') 
                        || document.querySelector('[class*="sidebar-title"]')
                        || document.querySelector('[class*="title"]');
                }
                
                console.log('📋 idEmpresa desde URL:', idEmpresa);
                console.log('📋 sidebarTitle encontrado:', sidebarTitle ? 'Sí' : 'No');

                // Intentar obtener empresaId del usuario logueado si no viene en la URL
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                        console.log('👤 userData encontrado:', {
                            empresaId: userData.empresaId,
                            empresaNombre: userData.empresaNombre,
                            email: userData.email
                        });
                    } catch (e) {
                        console.error('Error al parsear userData en admin.php:', e);
                    }
                } else {
                    console.warn('⚠️ No se encontró userData en sessionStorage ni localStorage');
                }

                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = String(userData.empresaId);
                    console.log('✅ idEmpresa obtenido de userData:', idEmpresa);
                    // Actualizar la URL sin recargar
                    const url = new URL(window.location.href);
                    url.searchParams.set('idEmpresa', idEmpresa);
                    window.history.replaceState({}, '', url.toString());
                }

                // Si tenemos nombre de empresa en userData y coincide el id, usarlo directamente
                if (idEmpresa && userData && String(userData.empresaId) === String(idEmpresa) && userData.empresaNombre) {
                    console.log('✅ Usando empresaNombre de userData:', userData.empresaNombre);
                    const companyFromUser = {
                        id: userData.empresaId,
                        name: userData.empresaNombre
                    };
                    sessionStorage.setItem('selectedCompany', JSON.stringify(companyFromUser));
                    if (sidebarTitle) {
                        sidebarTitle.textContent = userData.empresaNombre;
                        console.log('✅ Título actualizado desde userData:', userData.empresaNombre);
                    }
                    return; // Ya mostrarmos el nombre correcto, no hace falta llamar a la API
                }

                // Si no tenemos idEmpresa, dejar texto por defecto
                if (!idEmpresa) {
                    console.warn('⚠️ No se encontró idEmpresa');
                    sessionStorage.removeItem('selectedCompany');
                    if (sidebarTitle) {
                        sidebarTitle.textContent = 'Restaurante';
                    }
                    return;
                }

                // 1) Revisar si ya hay una empresa seleccionada en sessionStorage
                const stored = sessionStorage.getItem('selectedCompany');
                let company = null;
                if (stored) {
                    try {
                        const storedCompany = JSON.parse(stored);
                        if (storedCompany && String(storedCompany.id) === String(idEmpresa)) {
                            company = storedCompany;
                        }
                    } catch (e) {
                        console.error('Error al parsear selectedCompany:', e);
                    }
                }

                // 2) Si no hay company aún, intentar obtenerla desde la API
                if (!company && userData && userData.token && idEmpresa) {
                    console.log('🔍 Intentando obtener empresa desde la API, idEmpresa:', idEmpresa);
                    // Intentar primero obtener directamente desde la API
                    try {
                        const apiUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/empresas/' + encodeURIComponent(idEmpresa);
                        console.log('📡 Llamando a API directa:', apiUrl);
                        const directResponse = await fetch(apiUrl, {
                            method: 'GET',
                            headers: {
                                'Authorization': 'Bearer ' + userData.token,
                                'Content-Type': 'application/json'
                            }
                        });

                        console.log('📥 Respuesta directa:', directResponse.status, directResponse.statusText);

                        if (directResponse.ok) {
                            const directResult = await directResponse.json();
                            console.log('📦 Resultado directo:', directResult);
                            const raw = directResult.data || directResult;
                            
                            if (raw && (raw.idEmpresa || raw.id || raw.empresaId)) {
                                company = {
                                    id: raw.idEmpresa || raw.id || raw.empresaId || idEmpresa,
                                    name: raw.nombreEmpresa || raw.nombre || raw.razonSocial || 'Sin nombre',
                                    ruc: raw.ruc || '',
                                    email: raw.email || '',
                                    phone: raw.telefono || '',
                                    direccion: raw.direccion || ''
                                };
                                console.log('✅ Empresa obtenida desde API directa:', company.name);
                                sessionStorage.setItem('selectedCompany', JSON.stringify(company));
                            }
                        } else {
                            const errorText = await directResponse.text();
                            console.warn('⚠️ Error en respuesta directa:', directResponse.status, errorText);
                        }
                    } catch (e) {
                        console.warn('⚠️ No se pudo obtener empresa directamente desde la API, intentando vía proxy...', e);
                    }

                    // Si aún no tenemos company, intentar obtener el listado completo de empresas
                    if (!company) {
                        try {
                            console.log('📡 Intentando obtener listado completo de empresas...');
                            const listadoUrl = 'http://turistas.spring.informaticapp.com:2410/api/v1/empresas';
                            console.log('📡 URL del listado:', listadoUrl);
                            
                            const listadoResponse = await fetch(listadoUrl, {
                                method: 'GET',
                                headers: {
                                    'Authorization': 'Bearer ' + userData.token,
                                    'Content-Type': 'application/json'
                                }
                            });

                            console.log('📥 Respuesta del listado:', listadoResponse.status);

                            if (listadoResponse.ok) {
                                const listadoResult = await listadoResponse.json();
                                console.log('📦 Resultado del listado:', listadoResult);
                                
                                // El listado puede venir en varios formatos
                                let empresas = [];
                                if (Array.isArray(listadoResult)) {
                                    empresas = listadoResult;
                                } else if (listadoResult.data && Array.isArray(listadoResult.data)) {
                                    empresas = listadoResult.data;
                                } else if (listadoResult.content && Array.isArray(listadoResult.content)) {
                                    empresas = listadoResult.content;
                                }
                                
                                console.log(`📋 Se encontraron ${empresas.length} empresas en el listado`);

                                if (empresas.length > 0) {
                                    const raw = empresas.find(e => {
                                        const eid = e.idEmpresa || e.id || e.empresaId;
                                        return String(eid) === String(idEmpresa);
                                    });

                                    if (raw) {
                                        console.log('✅ Empresa encontrada en el listado:', raw);
                                        company = {
                                            id: raw.idEmpresa || raw.id || raw.empresaId || idEmpresa,
                                            name: raw.nombreEmpresa || raw.nombre || raw.razonSocial || 'Sin nombre',
                                            ruc: raw.ruc || '',
                                            email: raw.email || '',
                                            phone: raw.telefono || '',
                                            direccion: raw.direccion || ''
                                        };

                                        console.log('✅ Empresa obtenida desde listado:', company.name);
                                        sessionStorage.setItem('selectedCompany', JSON.stringify(company));
                                    } else {
                                        console.warn('⚠️ Empresa no encontrada en el listado para idEmpresa:', idEmpresa);
                                        console.log('📋 IDs disponibles en el listado:', empresas.map(e => e.idEmpresa || e.id || e.empresaId));
                                    }
                                } else {
                                    console.warn('⚠️ El listado de empresas está vacío');
                                }
                            } else {
                                const errorText = await listadoResponse.text();
                                console.warn('⚠️ Error al obtener listado de empresas:', listadoResponse.status, errorText);
                            }
                        } catch (e) {
                            console.error('❌ Error al obtener listado de empresas:', e);
                        }
                    }
                }

                // 3) Actualizar el título del sidebar
                if (sidebarTitle) {
                    if (company) {
                        sidebarTitle.textContent = company.name || 'Dashboard Empresa';
                        console.log('✅ Título actualizado con nombre de empresa:', company.name);
                        
                        // Actualizar también userData para futuras cargas
                        if (userData && company.name !== 'Sin nombre') {
                            userData.empresaNombre = company.name;
                            sessionStorage.setItem('userData', JSON.stringify(userData));
                            console.log('💾 userData actualizado con empresaNombre:', company.name);
                        }
                    } else {
                        sidebarTitle.textContent = 'Empresa #' + idEmpresa;
                        console.warn('⚠️ No se pudo obtener nombre de empresa, mostrando fallback: Empresa #' + idEmpresa);
                    }
                } else {
                    console.warn('⚠️ No se encontró el elemento .sidebar-title en el DOM');
                }
            } catch (e) {
                console.error('Error al cargar empresa seleccionada:', e);
            }
        }

        // Función helper para cargar contenido de clientes.php
        async function loadClientesContent() {
            console.log('🔍 loadClientesContent() llamada');
            const clientesSection = document.getElementById('clientes-section');
            if (!clientesSection) {
                console.warn('⚠️ Sección de clientes no disponible, se omite la carga.');
                return;
            }
            
            console.log('✅ Sección de clientes encontrada, reemplazando contenido...');
            
            // Asegurar que la sección esté visible
            clientesSection.style.display = 'block';
            clientesSection.classList.add('active');

            // Mostrar indicador de carga
            clientesSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando clientes...</p></div>';

            try {
                // Obtener datos del usuario desde sessionStorage o localStorage
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa de la URL actual o de userData
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                // Si no está en la URL, intentar obtenerlo de userData
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Verificar si el usuario es gerente
                const rol = String(userData?.rol || '').toLowerCase().trim();
                const rolId = userData?.rolId ? parseInt(userData.rolId) : null;
                const rolesArray = Array.isArray(userData?.roles) ? userData.roles : [];
                const esGerente = rol === 'gerente' || 
                                 rolId === 3 || 
                                 rolId === 4 || 
                                 rolesArray.some(r => {
                                     const rolStr = String(r).toUpperCase();
                                     return rolStr.includes('GERENTE') || rolStr.includes('MANAGER');
                                 });
                
                // Obtener idSucursal del userData SOLO si NO es gerente
                // Los gerentes deben ver todos los clientes de todas las sucursales
                let idSucursal = null;
                if (!esGerente && userData && (userData.sucursalId || userData.idSucursal)) {
                    idSucursal = userData.sucursalId || userData.idSucursal;
                }
                
                // Obtener token del usuario
                const token = userData && userData.token ? userData.token : null;
                
                // Construir la URL para obtener el contenido
                let url = 'CLIENTES/clientes.php?ajax=1';
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (idSucursal) {
                    url += '&idSucursal=' + encodeURIComponent(idSucursal);
                    console.log('✅ Filtrando por sucursal:', idSucursal);
                } else if (esGerente) {
                    console.log('✅ Usuario es Gerente - Mostrando todos los clientes de todas las sucursales');
                }
                // Pasar información del rol para que clientes.php sepa si es gerente
                if (esGerente) {
                    url += '&rol=gerente';
                    if (rolId) {
                        url += '&rolId=' + encodeURIComponent(rolId);
                    }
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                    console.log('✅ Token encontrado, longitud:', token.length);
                } else {
                    console.warn('⚠️ No se encontró token del usuario');
                }
                
                console.log('📡 URL de carga:', url.substring(0, 100) + '...');
                
                // Cargar contenido vía fetch
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status} - ${errorText.substring(0, 100)}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar el contenido en la sección
                clientesSection.innerHTML = html;
                console.log('✅ Contenido insertado en clientes-section');
                
                // Ejecutar scripts que puedan estar en el contenido cargado
                const scripts = clientesSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Contenido de clientes cargado correctamente');
            } catch (error) {
                console.error('Error al cargar contenido de clientes:', error);
                clientesSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Gestión de Clientes</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar los clientes: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadClientesContent()" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Función helper para cargar contenido de reservas
        async function loadReservasContent(filters = {}) {
            console.log('🔍 loadReservasContent() llamada con filtros:', filters);
            const reservasSection = document.getElementById('reservas-section');
            if (!reservasSection) {
                console.error('❌ No se encontró la sección de reservas');
                alert('Error: No se encontró la sección de reservas');
                return;
            }
            
            console.log('✅ Sección de reservas encontrada, reemplazando contenido...');
            
            // Asegurar que la sección esté visible
            reservasSection.style.display = 'block';
            reservasSection.classList.add('active');

            // Si es una búsqueda (tiene filtro de busqueda), mostrar indicador sutil
            // Si no, mostrar el indicador completo
            const isSearch = filters && filters.busqueda && filters.busqueda.trim();
            
            if (isSearch) {
                // Para búsquedas, mantener el contenido actual y solo mostrar un indicador sutil
                const existingContent = reservasSection.innerHTML;
                const searchIndicator = document.createElement('div');
                searchIndicator.id = 'search-loading-indicator';
                searchIndicator.style.cssText = 'position: fixed; top: 80px; right: 20px; padding: 12px 20px; background: rgba(59, 130, 246, 0.95); border: 1px solid rgba(59, 130, 246, 0.5); border-radius: 8px; color: white; font-size: 0.875rem; z-index: 10000; display: flex; align-items: center; gap: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);';
                searchIndicator.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Buscando reservas...';
                
                // Remover indicador anterior si existe
                const oldIndicator = document.getElementById('search-loading-indicator');
                if (oldIndicator) {
                    oldIndicator.remove();
                }
                
                document.body.appendChild(searchIndicator);
            } else {
                // Para carga inicial, mostrar indicador completo
                reservasSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando reservas...</p></div>';
            }

            try {
                // Obtener datos del usuario desde sessionStorage o localStorage
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa de la URL actual o de userData
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                // Si no está en la URL, intentar obtenerlo de userData
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Verificar si el usuario es gerente
                const rol = String(userData?.rol || '').toLowerCase().trim();
                const rolId = userData?.rolId ? parseInt(userData.rolId) : null;
                const rolesArray = Array.isArray(userData?.roles) ? userData.roles : [];
                const esGerente = rol === 'gerente' || 
                                 rolId === 3 || 
                                 rolId === 4 || 
                                 rolesArray.some(r => {
                                     const rolStr = String(r).toUpperCase();
                                     return rolStr.includes('GERENTE') || rolStr.includes('MANAGER');
                                 });
                
                // Obtener idSucursal del userData SOLO para gerentes
                // Los gerentes solo ven las reservas de su sucursal
                let idSucursal = null;
                if (esGerente && userData) {
                    idSucursal = userData.sucursalId || userData.idSucursal;
                    console.log('🔍 userData completo para gerente:', {
                        sucursalId: userData.sucursalId,
                        idSucursal: userData.idSucursal,
                        idSucursalFinal: idSucursal,
                        userDataKeys: Object.keys(userData)
                    });
                }
                
                // Obtener token del usuario
                const token = userData && userData.token ? userData.token : null;
                
                // Construir la URL para obtener el contenido
                let url = 'RESERVAS/reservas.php?ajax=1';
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                // Solo pasar idSucursal si es gerente
                if (esGerente && idSucursal) {
                    url += '&idSucursal=' + encodeURIComponent(idSucursal);
                    console.log('✅ Usuario es Gerente - Mostrando reservas de su sucursal:', idSucursal);
                    console.log('📋 URL completa para reservas:', url);
                } else if (esGerente && !idSucursal) {
                    console.warn('⚠️ Usuario es Gerente pero NO se encontró idSucursal en userData');
                }
                // Pasar información del rol para que reservas.php sepa si es gerente
                if (esGerente) {
                    url += '&rol=gerente';
                    if (rolId) {
                        url += '&rolId=' + encodeURIComponent(rolId);
                    }
                }
                // Agregar filtros de búsqueda si se proporcionaron
                if (filters && typeof filters === 'object') {
                    if (filters.busqueda && filters.busqueda.trim()) {
                        url += '&busqueda=' + encodeURIComponent(filters.busqueda.trim());
                        console.log('🔍 Agregando filtro de búsqueda:', filters.busqueda.trim());
                    }
                    if (filters.estado) {
                        url += '&estado=' + encodeURIComponent(filters.estado);
                        console.log('🔍 Agregando filtro de estado:', filters.estado);
                    }
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                    console.log('✅ Token encontrado, longitud:', token.length);
                } else {
                    console.warn('⚠️ No se encontró token del usuario');
                }
                
                console.log('📡 URL de carga:', url.substring(0, 100) + '...');
                
                // Cargar contenido vía fetch
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status} - ${errorText.substring(0, 100)}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar el contenido en la sección
                reservasSection.innerHTML = html;
                console.log('✅ Contenido insertado en reservas-section');
                
                // Remover indicador de búsqueda si existe
                const searchIndicator = document.getElementById('search-loading-indicator');
                if (searchIndicator) {
                    searchIndicator.remove();
                }
                
                // Ejecutar scripts que puedan estar en el contenido cargado
                const scripts = reservasSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Contenido de reservas cargado correctamente');
            } catch (error) {
                console.error('Error al cargar contenido de reservas:', error);
                reservasSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Gestión de Reservas</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar las reservas: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadReservasContent()" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Función helper para cargar contenido de sucursales.php
        async function loadSucursalesContent() {
            console.log('🔍 loadSucursalesContent() llamada');
            const sucursalesSection = document.getElementById('sucursales-section');
            if (!sucursalesSection) {
                console.warn('⚠️ Sección de sucursales no disponible, se omite la carga.');
                return;
            }
            
            console.log('✅ Sección de sucursales encontrada, reemplazando contenido...');
            
            // Asegurar que la sección esté visible
            sucursalesSection.style.display = 'block';
            sucursalesSection.classList.add('active');

            // Mostrar indicador de carga
            sucursalesSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando sucursales...</p></div>';

            try {
                // Obtener datos del usuario desde sessionStorage o localStorage
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa de la URL actual o de userData
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                // Si no está en la URL, intentar obtenerlo de userData
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Obtener token del usuario
                const token = userData && userData.token ? userData.token : null;
                
                // Construir la URL para obtener el contenido
                let url = 'sucursales.php?ajax=1';
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                    console.log('✅ Token encontrado, longitud:', token.length);
                } else {
                    console.warn('⚠️ No se encontró token del usuario');
                }
                
                console.log('📡 URL de carga:', url.substring(0, 100) + '...');
                
                // Cargar contenido vía fetch
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status} - ${errorText.substring(0, 100)}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar el contenido en la sección
                sucursalesSection.innerHTML = html;
                console.log('✅ Contenido insertado en sucursales-section');
                
                // Ejecutar scripts que puedan estar en el contenido cargado
                const scripts = sucursalesSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Contenido de sucursales cargado correctamente');
            } catch (error) {
                console.error('Error al cargar contenido de sucursales:', error);
                sucursalesSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Gestión de Sucursales</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar las sucursales: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadSucursalesContent()" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Función helper para cargar el dashboard de reportes
        function ensureReportesStyles() {
            const head = document.head;
            if (!head) {
                console.warn('⚠️ No se encontró el elemento <head> para cargar estilos de reportes');
                return;
            }

            const styles = [
                { href: 'REPORTES/reportes.css', key: 'main' }
            ];

            styles.forEach(style => {
                if (!document.querySelector(`link[data-reportes-style="${style.key}"]`)) {
                    const link = document.createElement('link');
                    link.rel = 'stylesheet';
                    link.href = style.href;
                    link.setAttribute('data-reportes-style', style.key);
                    head.appendChild(link);
                    console.log('🎨 Estilo de reportes cargado:', style.href);
                }
            });
        }

        function loadExternalScriptOnce(src, key) {
            return new Promise((resolve, reject) => {
                const existing = document.querySelector(`script[data-reportes-script="${key}"]`);
                if (existing) {
                    if (existing.dataset.loaded === 'true' || existing.readyState === 'complete') {
                        resolve(existing);
                        return;
                    }
                    existing.addEventListener('load', () => resolve(existing), { once: true });
                    existing.addEventListener('error', event => reject(new Error(`No se pudo cargar el script ${src}`)), { once: true });
                    return;
                }

                const script = document.createElement('script');
                script.src = src;
                script.async = false;
                script.setAttribute('data-reportes-script', key);
                script.addEventListener('load', () => {
                    script.dataset.loaded = 'true';
                    console.log('📦 Script cargado:', src);
                    resolve(script);
                }, { once: true });
                script.addEventListener('error', () => {
                    console.error('❌ Error al cargar script:', src);
                    reject(new Error(`No se pudo cargar el script ${src}`));
                }, { once: true });
                (document.head || document.body).appendChild(script);
            });
        }

        async function ensureReportesScripts(config) {
            await loadExternalScriptOnce('https://cdn.jsdelivr.net/npm/chart.js@4.4.6/dist/chart.umd.min.js', 'chartjs');
            if (config) {
                window.REPORTES_CONFIG = Object.assign({}, window.REPORTES_CONFIG || {}, config);
            }
            await loadExternalScriptOnce('REPORTES/reportes.js', 'dashboard');
        }

        async function loadReportesContent() {
            console.log('🔍 loadReportesContent() llamada');
            const reportesSection = document.getElementById('reportes-section');
            if (!reportesSection) {
                console.error('❌ No se encontró la sección de reportes');
                alert('Error: No se encontró la sección de reportes');
                return;
            }

            reportesSection.style.display = 'block';
            reportesSection.classList.add('active');
            reportesSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando reportes...</p></div>';

            try {
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                } else {
                    console.warn('⚠️ No se encontró userData para reportes');
                }

                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }

                const rol = String(userData?.rol || '').toLowerCase().trim();
                const rolId = userData?.rolId ? parseInt(userData.rolId, 10) : null;
                const rolesArrayRaw = Array.isArray(userData?.roles) ? userData.roles : [];
                const rolesNormalized = rolesArrayRaw.map(role => String(role)).filter(Boolean);
                const esGerente = rol === 'gerente' ||
                    rolId === 3 ||
                    rolId === 4 ||
                    rolesNormalized.some(r => {
                        const rolStr = r.toUpperCase();
                        return rolStr.includes('GERENTE') || rolStr.includes('MANAGER');
                    });

                let idSucursal = null;
                if (esGerente) {
                    idSucursal = userData?.sucursalId || userData?.idSucursal || null;
                }

                const token = userData?.token || null;

                let url = 'REPORTES/dashboard.php?ajax=1';
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (esGerente) {
                    url += '&rol=gerente';
                    if (rolId) {
                        url += '&rolId=' + encodeURIComponent(rolId);
                    }
                    if (idSucursal) {
                        url += '&idSucursal=' + encodeURIComponent(idSucursal);
                        console.log('✅ Usuario gerente, filtrando reportes por sucursal:', idSucursal);
                    } else {
                        console.warn('⚠️ Usuario gerente sin idSucursal disponible para reportes');
                    }
                } else {
                    if (rol) {
                        url += '&rol=' + encodeURIComponent(rol);
                    }
                    if (rolId) {
                        url += '&rolId=' + encodeURIComponent(rolId);
                    }
                }
                if (rolesNormalized.length) {
                    url += '&roles=' + encodeURIComponent(rolesNormalized.join(','));
                }
                url += '&esGerente=' + (esGerente ? 'true' : 'false');
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                } else {
                    console.warn('⚠️ No se encontró token del usuario para reportes');
                }

                console.log('📡 URL de reportes:', url.substring(0, 120) + '...');

                const response = await fetch(url);
                console.log('📥 Respuesta de reportes:', response.status, response.statusText);

                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(`Error HTTP: ${response.status} - ${errorText.substring(0, 100)}`);
                }

                const html = await response.text();
                console.log('📦 HTML de reportes recibido, longitud:', html.length);

                ensureReportesStyles();

                reportesSection.innerHTML = html;

                const scripts = reportesSection.querySelectorAll('script');
                console.log('📜 Scripts de reportes encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script de reportes ${index + 1} ejecutado`);
                });

                try {
                    await ensureReportesScripts({
                        apiBase: 'http://turistas.spring.informaticapp.com:2410/api/v1',
                        token: token || '',
                        empresaId: idEmpresa || userData?.empresaId || null,
                        empresaNombre: userData?.empresaNombre || null,
                        esGerente,
                        idSucursal: idSucursal ?? null,
                        rol,
                        rolId,
                        roles: rolesNormalized
                    });
                } catch (assetError) {
                    console.error('❌ Error al cargar assets de reportes:', assetError);
                    mostrarAlerta('Error al cargar scripts de reportes. Intenta recargar la sección.', 'error');
                }

                if (window.ReportesDashboard?.mount) {
                    try {
                        await window.ReportesDashboard.mount({ preserveFilters: true, forceRefresh: true });
                    } catch (dashboardError) {
                        console.error('❌ Error al volver a montar el dashboard de reportes:', dashboardError);
                        mostrarAlerta('Ocurrió un problema al preparar los reportes. Intenta recargar la sección.', 'error');
                    }
                } else {
                    console.warn('⚠️ ReportesDashboard.mount no está disponible tras cargar los assets.');
                }

                console.log('✅ Contenido de reportes cargado correctamente');
            } catch (error) {
                console.error('Error al cargar contenido de reportes:', error);
                reportesSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Centro de Reportes</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar los reportes: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadReportesContent()" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Navegación entre secciones
        const sidebarLinks = document.querySelectorAll('.sidebar-link');
        const sections = document.querySelectorAll('.content-section');
        const pageTitle = document.getElementById('pageTitle');
        const sidebarToggle = document.getElementById('sidebarToggle');
        const sidebar = document.getElementById('sidebar');
        const mainContent = document.getElementById('mainContent');

        // Cambiar sección
        sidebarLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const targetSection = link.getAttribute('data-section');
                console.log('🔍 Click en sección:', targetSection);

                // Remover activo de todos
                sidebarLinks.forEach(l => l.classList.remove('active'));
                sections.forEach(s => {
                    s.classList.remove('active');
                    s.style.display = 'none';
                });

                // Activar seleccionado
                link.classList.add('active');
                const targetSectionElement = document.getElementById(`${targetSection}-section`);
                if (targetSectionElement) {
                    targetSectionElement.classList.add('active');
                    // Asegurar que la sección sea visible
                    targetSectionElement.style.display = 'block';
                    console.log('✅ Sección activada:', targetSection);
                }

                // Si es la sección de reservas, cargar contenido dinámicamente
                if (targetSection === 'reservas') {
                    console.log('🔍 Sección reservas seleccionada, cargando contenido...');
                    // Cargar inmediatamente
                    loadReservasContent();
                }

                // Si es la sección de reportes, cargar contenido dinámicamente
                if (targetSection === 'reportes') {
                    console.log('🔍 Sección reportes seleccionada, cargando contenido...');
                    loadReportesContent();
                }

                // Actualizar título - usar nombre de sucursal si está disponible (parte blanca)
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let sucursalNombre = null;
                if (userDataStr) {
                    try {
                        const userData = JSON.parse(userDataStr);
                        sucursalNombre = userData.sucursalNombre;
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                if (pageTitle) {
                    // Si estamos en reservas, mostrar el nombre de la sucursal si está disponible
                    if (targetSection === 'reservas' && sucursalNombre) {
                        pageTitle.textContent = sucursalNombre;
                    } else if (sucursalNombre) {
                        // Si tenemos nombre de sucursal y NO estamos en la sección de sucursales, usarlo
                        pageTitle.textContent = sucursalNombre;
                    } else {
                        const titles = {
                            'dashboard': 'Dashboard',
                            'personal': 'Personal',
                            'reservas': 'Reservas',
                            'caja': 'Caja',
                            'reportes': 'Reportes'
                        };
                        pageTitle.textContent = titles[targetSection] || 'Dashboard';
                    }
                }
            });
        });

        // Toggle sidebar en móvil
        sidebarToggle.addEventListener('click', () => {
            sidebar.classList.toggle('sidebar-collapsed');
            mainContent.classList.toggle('content-expanded');
        });

        // Función para mostrar alertas
        function mostrarAlerta(mensaje, tipo = 'info') {
            const alertContainer = document.getElementById('alertContainer');
            const alerta = document.createElement('div');
            alerta.className = `alerta alerta-${tipo}`;
            alerta.innerHTML = `
                <span>${mensaje}</span>
                <button class="cerrar-alerta" onclick="this.parentElement.remove()">&times;</button>
            `;
            alertContainer.appendChild(alerta);

            // Animación de entrada
            setTimeout(() => {
                alerta.style.opacity = '1';
                alerta.style.transform = 'translateY(0)';
            }, 10);

            // Auto cerrar después de 5 segundos
            setTimeout(() => {
                alerta.style.opacity = '0';
                alerta.style.transform = 'translateX(100%)';
                setTimeout(() => alerta.remove(), 300);
            }, 5000);
        }

        // Event listeners para botones
        document.getElementById('btnNuevoPersonal')?.addEventListener('click', () => {
            mostrarAlerta('Funcionalidad de nuevo personal en desarrollo', 'info');
        });

        // El botón btnNuevaReserva se maneja con event delegation más abajo
        // para que funcione tanto cuando se carga dinámicamente como cuando está en el DOM

        document.getElementById('btnAbrirCaja')?.addEventListener('click', () => {
            mostrarAlerta('Caja abierta correctamente', 'success');
        });

        document.getElementById('btnCerrarCaja')?.addEventListener('click', () => {
            mostrarAlerta('Caja cerrada correctamente', 'success');
        });

        // Función para cargar editar_cliente.php dinámicamente
        async function loadEditarClienteContent(clienteId) {
            console.log('🔍 loadEditarClienteContent() llamada con ID:', clienteId);
            const clientesSection = document.getElementById('clientes-section');
            if (!clientesSection) {
                console.error('❌ No se encontró la sección de clientes');
                return;
            }
            
            if (!clienteId) {
                console.error('❌ No se proporcionó ID de cliente');
                alert('Error: No se proporcionó ID de cliente');
                return;
            }
            
            console.log('✅ Sección de clientes encontrada, cargando formulario de edición...');
            
            // Asegurar que la sección esté visible
            clientesSection.style.display = 'block';
            clientesSection.classList.add('active');

            // Mostrar indicador de carga
            clientesSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando formulario...</p></div>';

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Obtener token
                const token = userData && userData.token ? userData.token : null;
                
                // Construir URL
                let url = 'CLIENTES/editar_cliente.php?ajax=1&id=' + encodeURIComponent(clienteId);
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                }
                
                console.log('📡 URL de carga:', url);
                
                // Cargar contenido
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar contenido
                clientesSection.innerHTML = html;
                console.log('✅ Contenido insertado en clientes-section');
                
                // Ejecutar scripts
                const scripts = clientesSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Formulario de edición cargado correctamente');
            } catch (error) {
                console.error('Error al cargar formulario de edición:', error);
                clientesSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Editar Cliente</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar el formulario: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadEditarClienteContent(${clienteId})" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                                <button class="btn btn-secondary" onclick="loadClientesContent()" style="margin-top: 10px; margin-left: 10px;">
                                    <i class="fas fa-arrow-left"></i> Volver
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Función para cargar nuevo_cliente.php dinámicamente
        async function loadNuevoClienteContent() {
            console.log('🔍 loadNuevoClienteContent() llamada');
            const clientesSection = document.getElementById('clientes-section');
            if (!clientesSection) {
                console.error('❌ No se encontró la sección de clientes');
                return;
            }
            
            console.log('✅ Sección de clientes encontrada, cargando formulario de nuevo cliente...');
            
            // Asegurar que la sección esté visible
            clientesSection.style.display = 'block';
            clientesSection.classList.add('active');

            // Mostrar indicador de carga
            clientesSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando formulario...</p></div>';

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Obtener token
                const token = userData && userData.token ? userData.token : null;
                
                // Construir URL
                let url = 'CLIENTES/nuevo_cliente.php?ajax=1';
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                }
                
                console.log('📡 URL de carga:', url);
                
                // Cargar contenido
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar contenido
                clientesSection.innerHTML = html;
                console.log('✅ Contenido insertado en clientes-section');
                
                // Ejecutar scripts
                const scripts = clientesSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                // Asegurar que el event listener del formulario se configure correctamente
                setTimeout(() => {
                    if (typeof window.setupFormSubmitHandler === 'function') {
                        console.log('🔧 Ejecutando setupFormSubmitHandler desde admin.php...');
                        window.setupFormSubmitHandler();
                    }
                }, 100);
                
                console.log('✅ Formulario de nuevo cliente cargado correctamente');
            } catch (error) {
                console.error('Error al cargar formulario de nuevo cliente:', error);
                clientesSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Crear Nuevo Cliente</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar el formulario: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadNuevoClienteContent()" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        document.getElementById('btnNuevoCliente')?.addEventListener('click', () => {
            // Activar sección de clientes primero
            const clientesLink = document.querySelector('.sidebar-link[data-section="clientes"]');
            if (clientesLink) {
                clientesLink.click();
            }
            // Cargar formulario de nuevo cliente
            setTimeout(() => {
                loadNuevoClienteContent();
            }, 100);
        });

        // Función para cargar nueva_reserva.php dinámicamente
        async function loadNuevaReservaContent() {
            console.log('🔍 loadNuevaReservaContent() llamada');
            const reservasSection = document.getElementById('reservas-section');
            if (!reservasSection) {
                console.error('❌ No se encontró la sección de reservas');
                return;
            }
            
            console.log('✅ Sección de reservas encontrada, cargando formulario de nueva reserva...');
            
            // Asegurar que la sección esté visible
            reservasSection.style.display = 'block';
            reservasSection.classList.add('active');

            // Mostrar indicador de carga
            reservasSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando formulario...</p></div>';

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Verificar si el usuario es gerente
                const rol = String(userData?.rol || '').toLowerCase().trim();
                const rolId = userData?.rolId ? parseInt(userData.rolId) : null;
                const rolesArray = Array.isArray(userData?.roles) ? userData.roles : [];
                const esGerente = rol === 'gerente' || 
                                 rolId === 3 || 
                                 rolId === 4 || 
                                 rolesArray.some(r => {
                                     const rolStr = String(r).toUpperCase();
                                     return rolStr.includes('GERENTE') || rolStr.includes('MANAGER');
                                 });
                
                // Obtener idSucursal SOLO si NO es gerente
                let idSucursal = null;
                if (!esGerente && userData && (userData.sucursalId || userData.idSucursal)) {
                    idSucursal = userData.sucursalId || userData.idSucursal;
                }
                
                // Obtener token
                const token = userData && userData.token ? userData.token : null;
                
                // Construir URL
                let url = 'RESERVAS/nueva_reserva.php?ajax=1';
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (idSucursal) {
                    url += '&idSucursal=' + encodeURIComponent(idSucursal);
                }
                if (esGerente) {
                    url += '&rol=gerente';
                    if (rolId) {
                        url += '&rolId=' + encodeURIComponent(rolId);
                    }
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                }
                
                console.log('📡 URL de carga:', url);
                
                // Cargar contenido
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar contenido
                reservasSection.innerHTML = html;
                console.log('✅ Contenido insertado en reservas-section');
                
                // Ejecutar scripts
                const scripts = reservasSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Formulario de nueva reserva cargado correctamente');
            } catch (error) {
                console.error('Error al cargar formulario de nueva reserva:', error);
                reservasSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Crear Nueva Reserva</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar el formulario: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadNuevaReservaContent()" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Función para cargar editar_reserva.php dinámicamente
        async function loadEditarReservaContent(reservaId) {
            console.log('🔍 loadEditarReservaContent() llamada con reservaId:', reservaId);
            const reservasSection = document.getElementById('reservas-section');
            if (!reservasSection) {
                console.error('❌ No se encontró la sección de reservas');
                return;
            }
            
            if (!reservaId) {
                console.error('❌ No se proporcionó el ID de la reserva');
                return;
            }
            
            console.log('✅ Sección de reservas encontrada, cargando formulario de editar reserva...');
            
            // Asegurar que la sección esté visible
            reservasSection.style.display = 'block';
            reservasSection.classList.add('active');

            // Mostrar indicador de carga
            reservasSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando formulario de edición...</p></div>';

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Verificar si el usuario es gerente
                const rol = String(userData?.rol || '').toLowerCase().trim();
                const rolId = userData?.rolId ? parseInt(userData.rolId) : null;
                const rolesArray = Array.isArray(userData?.roles) ? userData.roles : [];
                const esGerente = rol === 'gerente' || 
                                 rolId === 3 || 
                                 rolId === 4 || 
                                 rolesArray.some(r => {
                                     const rolStr = String(r).toUpperCase();
                                     return rolStr.includes('GERENTE') || rolStr.includes('MANAGER');
                                 });
                
                // Obtener idSucursal SOLO si NO es gerente
                let idSucursal = null;
                if (!esGerente && userData && (userData.sucursalId || userData.idSucursal)) {
                    idSucursal = userData.sucursalId || userData.idSucursal;
                }
                
                // Obtener token
                const token = userData && userData.token ? userData.token : null;
                
                // Construir URL
                let url = 'RESERVAS/editar_reserva.php?ajax=1&id=' + encodeURIComponent(reservaId);
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (idSucursal) {
                    url += '&idSucursal=' + encodeURIComponent(idSucursal);
                }
                if (esGerente) {
                    url += '&rol=gerente';
                    if (rolId) {
                        url += '&rolId=' + encodeURIComponent(rolId);
                    }
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                }
                
                console.log('📡 URL de carga:', url);
                
                // Cargar contenido
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                console.log('📦 Primeros 500 caracteres del HTML:', html.substring(0, 500));
                
                // Verificar si el HTML contiene el formulario
                if (html.includes('formEditarReserva')) {
                    console.log('✅ El HTML contiene el formulario formEditarReserva');
                } else {
                    console.warn('⚠️ El HTML NO contiene el formulario formEditarReserva');
                    console.log('📋 Contenido completo recibido:', html);
                }
                
                // Insertar contenido
                reservasSection.innerHTML = html;
                console.log('✅ Contenido insertado en reservas-section');
                
                // Verificar si el formulario está en el DOM después de insertar
                const form = reservasSection.querySelector('#formEditarReserva');
                if (form) {
                    console.log('✅ Formulario encontrado en el DOM después de insertar');
                } else {
                    console.error('❌ Formulario NO encontrado en el DOM después de insertar');
                    const cardBody = reservasSection.querySelector('.card-body');
                    if (cardBody) {
                        console.log('📋 Contenido de card-body:', cardBody.innerHTML.substring(0, 500));
                    }
                }
                
                // Ejecutar scripts
                const scripts = reservasSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Formulario de editar reserva cargado correctamente');
            } catch (error) {
                console.error('Error al cargar formulario de editar reserva:', error);
                reservasSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Editar Reserva</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar el formulario: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadEditarReservaContent('${reservaId}')" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Exportar funciones globalmente
        window.loadNuevaReservaContent = loadNuevaReservaContent;
        window.loadEditarReservaContent = loadEditarReservaContent;

        // Event listener para el botón "Nueva Reserva" (se agregará dinámicamente cuando se cargue reservas.php)
        document.addEventListener('click', function(e) {
            if (e.target && (e.target.id === 'btnNuevaReserva' || e.target.closest('#btnNuevaReserva'))) {
                e.preventDefault();
                // Activar sección de reservas primero
                const reservasLink = document.querySelector('.sidebar-link[data-section="reservas"]');
                if (reservasLink) {
                    reservasLink.click();
                }
                // Cargar formulario de nueva reserva
                setTimeout(() => {
                    loadNuevaReservaContent();
                }, 100);
            }
        });

        // Función para eliminar cliente vía AJAX
        // Nota: Esta función NO muestra confirmación, ya que confirmarEliminar() la maneja antes de llamarla
        async function eliminarCliente(clienteId, nombreCliente = '') {
            if (!clienteId) {
                console.error('❌ No se proporcionó ID de cliente');
                mostrarAlerta('error', 'Error: No se proporcionó ID de cliente');
                return;
            }

            console.log('🔍 eliminarCliente() llamada con ID:', clienteId);

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }

                // Obtener token
                const token = userData && userData.token ? userData.token : null;

                // Construir URL
                let url = 'CLIENTES/eliminar_cliente.php?ajax=1&id=' + encodeURIComponent(clienteId);
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                }

                console.log('📡 URL de eliminación:', url);

                // Realizar petición DELETE
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                });

                console.log('📥 Respuesta recibida:', response.status, response.statusText);

                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status}`);
                }

                const result = await response.json();
                console.log('📦 Resultado:', result);

                if (result.success) {
                    mostrarAlerta('success', result.message || 'Cliente eliminado correctamente');
                    // Recargar la lista de clientes
                    setTimeout(() => {
                        if (typeof loadClientesContent === 'function') {
                            loadClientesContent();
                        } else {
                            window.location.reload();
                        }
                    }, 1500);
                } else {
                    throw new Error(result.message || 'Error al eliminar cliente');
                }
            } catch (error) {
                console.error('Error al eliminar cliente:', error);
                mostrarAlerta('error', error.message || 'Error al eliminar el cliente');
            }
        }

        // Exportar función globalmente para que pueda ser llamada desde clientes.php
        window.eliminarCliente = eliminarCliente;

        // Función para eliminar sucursal vía AJAX
        async function eliminarSucursal(sucursalId, nombreSucursal = '') {
            if (!sucursalId) {
                console.error('❌ No se proporcionó ID de sucursal');
                mostrarAlerta('error', 'Error: No se proporcionó ID de sucursal');
                return;
            }

            console.log('🔍 eliminarSucursal() llamada con ID:', sucursalId);

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }

                // Obtener token
                const token = userData && userData.token ? userData.token : null;

                if (!token) {
                    console.error('❌ No se encontró token de autenticación');
                    mostrarAlerta('error', 'Error: No se encontró token de autenticación');
                    return;
                }

                // Preparar datos para enviar
                const data = {
                    idSucursal: parseInt(sucursalId),
                    token: token
                };

                console.log('📡 Enviando petición a eliminar_sucursal.php con datos:', data);

                // Realizar petición POST (eliminar_sucursal.php acepta POST o DELETE)
                const response = await fetch('eliminar_sucursal.php', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(data)
                });

                console.log('📥 Respuesta recibida:', response.status, response.statusText);

                // Leer la respuesta como texto primero para debugging
                const responseText = await response.text();
                console.log('📦 Respuesta completa:', responseText);

                let result;
                try {
                    result = JSON.parse(responseText);
                } catch (e) {
                    console.error('❌ Error al parsear respuesta JSON:', e);
                    throw new Error('Error al procesar la respuesta del servidor');
                }

                if (!response.ok) {
                    console.error('❌ Error en respuesta:', result);
                    throw new Error(result.message || result.error || `Error HTTP: ${response.status}`);
                }

                if (result.success) {
                    mostrarAlerta('success', result.message || 'Sucursal eliminada correctamente');
                    // Recargar la lista de sucursales
                    setTimeout(() => {
                        if (typeof loadSucursalesContent === 'function') {
                            loadSucursalesContent();
                        } else {
                            window.location.reload();
                        }
                    }, 1500);
                } else {
                    throw new Error(result.message || result.error || 'Error al eliminar sucursal');
                }
            } catch (error) {
                console.error('Error al eliminar sucursal:', error);
                mostrarAlerta('error', error.message || 'Error al eliminar la sucursal');
            }
        }

        // Exportar función globalmente para que pueda ser llamada desde sucursales.php
        window.eliminarSucursal = eliminarSucursal;

        // Función para cargar nueva_sucursal.php dinámicamente
        async function loadNuevaSucursalContent() {
            console.log('🔍 loadNuevaSucursalContent() llamada');
            const sucursalesSection = document.getElementById('sucursales-section');
            if (!sucursalesSection) {
                console.error('❌ No se encontró la sección de sucursales');
                return;
            }
            
            console.log('✅ Sección de sucursales encontrada, cargando formulario de nueva sucursal...');
            
            // Asegurar que la sección esté visible
            sucursalesSection.style.display = 'block';
            sucursalesSection.classList.add('active');

            // Mostrar indicador de carga
            sucursalesSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando formulario...</p></div>';

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Obtener token
                const token = userData && userData.token ? userData.token : null;
                
                // Construir URL para cargar el formulario vía AJAX
                let url = 'nueva_sucursal.php?ajax=1';
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                }
                
                console.log('📡 URL de carga:', url);
                
                // Cargar contenido
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar contenido
                sucursalesSection.innerHTML = html;
                console.log('✅ Contenido insertado en sucursales-section');
                
                // Ejecutar scripts
                const scripts = sucursalesSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Formulario de nueva sucursal cargado correctamente');
            } catch (error) {
                console.error('Error al cargar formulario de nueva sucursal:', error);
                sucursalesSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Crear Nueva Sucursal</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar el formulario: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadNuevaSucursalContent()" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Función para cargar editar_sucursal.php dinámicamente
        async function loadEditarSucursalContent(sucursalId) {
            console.log('🔍 loadEditarSucursalContent() llamada con ID:', sucursalId);
            const sucursalesSection = document.getElementById('sucursales-section');
            if (!sucursalesSection) {
                console.error('❌ No se encontró la sección de sucursales');
                return;
            }
            
            if (!sucursalId) {
                console.error('❌ No se proporcionó ID de sucursal');
                alert('Error: No se proporcionó ID de sucursal');
                return;
            }
            
            console.log('✅ Sección de sucursales encontrada, cargando formulario de edición...');
            
            // Asegurar que la sección esté visible
            sucursalesSection.style.display = 'block';
            sucursalesSection.classList.add('active');

            // Mostrar indicador de carga
            sucursalesSection.innerHTML = '<div style="padding: 40px; text-align: center;"><i class="fas fa-spinner fa-spin" style="font-size: 2rem; color: #007bff;"></i><p style="margin-top: 20px;">Cargando formulario...</p></div>';

            try {
                // Obtener datos del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let userData = null;
                if (userDataStr) {
                    try {
                        userData = JSON.parse(userDataStr);
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                }
                
                // Obtener idEmpresa
                const urlParams = new URLSearchParams(window.location.search);
                let idEmpresa = urlParams.get('idEmpresa');
                
                if (!idEmpresa && userData && userData.empresaId) {
                    idEmpresa = userData.empresaId;
                }
                
                // Obtener token
                const token = userData && userData.token ? userData.token : null;
                
                // Construir URL para cargar el formulario vía AJAX
                let url = 'editar_sucursal.php?ajax=1&id=' + encodeURIComponent(sucursalId);
                if (idEmpresa) {
                    url += '&idEmpresa=' + encodeURIComponent(idEmpresa);
                }
                if (token) {
                    url += '&token=' + encodeURIComponent(token);
                }
                
                console.log('📡 URL de carga:', url);
                
                // Cargar contenido
                const response = await fetch(url);
                console.log('📥 Respuesta recibida:', response.status, response.statusText);
                
                if (!response.ok) {
                    const errorText = await response.text();
                    console.error('❌ Error en respuesta:', errorText);
                    throw new Error(`Error HTTP: ${response.status}`);
                }
                
                const html = await response.text();
                console.log('📦 HTML recibido, longitud:', html.length);
                
                // Insertar contenido
                sucursalesSection.innerHTML = html;
                console.log('✅ Contenido insertado en sucursales-section');
                
                // Ejecutar scripts
                const scripts = sucursalesSection.querySelectorAll('script');
                console.log('📜 Scripts encontrados:', scripts.length);
                scripts.forEach((oldScript, index) => {
                    const newScript = document.createElement('script');
                    Array.from(oldScript.attributes).forEach(attr => {
                        newScript.setAttribute(attr.name, attr.value);
                    });
                    newScript.appendChild(document.createTextNode(oldScript.innerHTML));
                    oldScript.parentNode.replaceChild(newScript, oldScript);
                    console.log(`✅ Script ${index + 1} ejecutado`);
                });
                
                console.log('✅ Formulario de edición cargado correctamente');
            } catch (error) {
                console.error('Error al cargar formulario de edición:', error);
                sucursalesSection.innerHTML = `
                    <div class="content-header">
                        <h2 class="section-title">Editar Sucursal</h2>
                    </div>
                    <div class="card">
                        <div class="card-body">
                            <div style="padding: 20px; text-align: center; color: #dc3545;">
                                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 10px;"></i>
                                <p>Error al cargar el formulario: ${error.message}</p>
                                <button class="btn btn-primary" onclick="loadEditarSucursalContent(${sucursalId})" style="margin-top: 10px;">
                                    <i class="fas fa-redo"></i> Reintentar
                                </button>
                                <button class="btn btn-secondary" onclick="loadSucursalesContent()" style="margin-top: 10px; margin-left: 10px;">
                                    <i class="fas fa-arrow-left"></i> Volver
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }
        }

        // Exportar funciones globalmente
        window.loadNuevaSucursalContent = loadNuevaSucursalContent;
        window.loadEditarSucursalContent = loadEditarSucursalContent;

        document.getElementById('btnGenerarReporte')?.addEventListener('click', () => {
            mostrarAlerta('Reporte generado correctamente', 'success');
        });

        document.getElementById('btnExportarReporte')?.addEventListener('click', () => {
            mostrarAlerta('Reporte exportado correctamente', 'success');
        });

        // Cargar información del usuario
        function loadUserInfo() {
            const userData = sessionStorage.getItem('userData') || localStorage.getItem('userData');
            if (userData) {
                try {
                    const user = JSON.parse(userData);
                    
                    // Debug: Mostrar información del nombre
                    console.log('🔍 loadUserInfo - Datos del nombre:', {
                        nombre: user.nombre,
                        email: user.email,
                        nombreCompleto: user.nombreCompleto,
                        todosLosDatos: user
                    });
                    
                    // Intentar obtener el nombre de múltiples fuentes
                    let nombreAMostrar = user.nombre || user.nombreCompleto || user.email || 'Usuario';
                    
                    // Si el nombre está vacío o es solo "Usuario", intentar obtenerlo desde la API
                    if ((!nombreAMostrar || nombreAMostrar === 'Usuario' || nombreAMostrar.trim() === '') && user.id && user.token) {
                        console.log('⚠️ Nombre no encontrado, intentando obtener desde API...');
                        fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/usuarios/${user.id}`, {
                            method: 'GET',
                            headers: {
                                'Authorization': 'Bearer ' + user.token,
                                'Content-Type': 'application/json'
                            }
                        })
                        .then(response => response.json())
                        .then(data => {
                            const usuario = data.data || data;
                            const nombreCompleto = usuario.nombreCompleto 
                                || (usuario.nombre && usuario.apellido ? `${usuario.nombre} ${usuario.apellido}`.trim() : null)
                                || (usuario.nombres && usuario.apellidos ? `${usuario.nombres} ${usuario.apellidos}`.trim() : null)
                                || usuario.nombre 
                                || usuario.email
                                || 'Usuario';
                            
                            if (nombreCompleto && nombreCompleto !== 'Usuario') {
                                // Actualizar userData con el nombre obtenido
                                user.nombre = nombreCompleto;
                                sessionStorage.setItem('userData', JSON.stringify(user));
                                if (localStorage.getItem('userData')) {
                                    localStorage.setItem('userData', JSON.stringify(user));
                                }
                                
                                // Actualizar el nombre en la interfaz
                                document.getElementById('admin-username').textContent = nombreCompleto;
                                console.log('✅ Nombre obtenido desde API y actualizado:', nombreCompleto);
                            }
                        })
                        .catch(error => {
                            console.warn('⚠️ Error al obtener nombre desde API:', error);
                        });
                    }
                    
                    document.getElementById('admin-username').textContent = nombreAMostrar;
                    
                    // Debug: Mostrar información del rol
                    console.log('🔍 loadUserInfo - Datos del usuario:', {
                        rol: user.rol,
                        rolId: user.rolId,
                        roles: user.roles,
                        rolOriginal: user.rol
                    });
                    
                    // Mostrar rol según el usuario
                    const roleNames = {
                        'superadmin': 'Super Administrador',
                        'admin': 'Administrador',
                        'gerente': 'Gerente',
                        'usuario': 'Usuario'
                    };
                    
                    // Normalizar el rol a minúsculas para la búsqueda
                    const rolNormalizado = String(user.rol || '').toLowerCase().trim();
                    console.log('🔍 Rol normalizado:', rolNormalizado, 'RolId:', user.rolId, 'Roles array:', user.roles);
                    
                    // PRIORIDAD 1: Verificar por rolId primero (más confiable)
                    let rolFinal = null;
                    if (user.rolId) {
                        const rolIdInt = parseInt(user.rolId);
                        console.log('🔍 Verificando por rolId:', rolIdInt);
                        if (rolIdInt === 1) {
                            rolFinal = 'Super Administrador';
                        } else if (rolIdInt === 2) {
                            rolFinal = 'Administrador';
                        } else if (rolIdInt === 3 || rolIdInt === 4) {
                            // rolId 3 o 4 = Gerente (puede variar según el sistema)
                            rolFinal = 'Gerente';
                            console.log('✅ Rol detectado como Gerente por rolId =', rolIdInt);
                        }
                    }
                    
                    // PRIORIDAD 2: Verificar en el array de roles
                    if (!rolFinal && Array.isArray(user.roles) && user.roles.length > 0) {
                        console.log('🔍 Verificando en array de roles:', user.roles);
                        const rolesUpper = user.roles.map(r => String(r).toUpperCase());
                        if (rolesUpper.some(r => r.includes('GERENTE') || r.includes('MANAGER'))) {
                            rolFinal = 'Gerente';
                            console.log('✅ Rol detectado como Gerente por array de roles');
                        } else if (rolesUpper.some(r => r.includes('ADMIN') && !r.includes('SUPER'))) {
                            rolFinal = 'Administrador';
                        } else if (rolesUpper.some(r => r.includes('SUPERADMIN'))) {
                            rolFinal = 'Super Administrador';
                        }
                    }
                    
                    // PRIORIDAD 3: Verificar por el campo rol normalizado
                    if (!rolFinal && rolNormalizado) {
                        console.log('🔍 Verificando por campo rol:', rolNormalizado);
                        rolFinal = roleNames[rolNormalizado];
                        if (rolFinal) {
                            console.log('✅ Rol detectado por campo rol:', rolFinal);
                        }
                    }
                    
                    // Usar el rol final o el valor por defecto
                    const rolAMostrar = rolFinal || roleNames[rolNormalizado] || (user.rol ? String(user.rol).charAt(0).toUpperCase() + String(user.rol).slice(1).toLowerCase() : 'Usuario');
                    console.log('✅ Rol final a mostrar:', rolAMostrar, {
                        rolFinal: rolFinal,
                        rolNormalizado: rolNormalizado,
                        roleNamesMatch: roleNames[rolNormalizado],
                        userRol: user.rol
                    });
                    
                    // Asegurarse de que el elemento existe antes de actualizarlo
                    const roleElement = document.getElementById('admin-userrole');
                    if (roleElement) {
                        roleElement.textContent = rolAMostrar;
                        console.log('✅ Rol actualizado en la interfaz:', rolAMostrar);
                    } else {
                        console.error('❌ No se encontró el elemento admin-userrole');
                    }
                } catch (e) {
                    console.error('Error al cargar información del usuario:', e);
                }
            } else {
                console.warn('⚠️ No se encontró userData en loadUserInfo');
            }
        }

        // Función para cerrar sesión usando cerrar_sesion.php
        async function logout() {
            try {
                // Obtener token y userId del usuario
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                let token = null;
                let userId = null;
                
                if (userDataStr) {
                    try {
                        const userData = JSON.parse(userDataStr);
                        token = userData.token;
                        // Intentar obtener userId de múltiples campos posibles
                        userId = userData.id || userData.userId || userData.idUsuario || userData.user_id || userData.userIdUsuario;
                        
                        // Si aún no tenemos userId, intentar desde el token decodificado
                        if (!userId && token) {
                            try {
                                // El token JWT contiene el userId en el payload
                                const payload = JSON.parse(atob(token.split('.')[1]));
                                userId = payload.userId || payload.idUsuario || payload.id;
                            } catch (e) {
                                console.warn('No se pudo decodificar userId del token:', e);
                            }
                        }
                        
                        console.log('👤 Datos del usuario para logout:', {
                            userDataCompleto: userData,
                            id: userData.id,
                            userId: userData.userId,
                            idUsuario: userData.idUsuario,
                            userIdFinal: userId,
                            email: userData.email,
                            tokenPresente: !!token
                        });
                    } catch (e) {
                        console.error('Error al parsear userData:', e);
                    }
                } else {
                    console.warn('⚠️ No se encontró userData en sessionStorage ni localStorage');
                }

                // Si hay token y userId, llamar a cerrar_sesion.php para invalidarlo en el backend
                if (token && userId) {
                    const logoutUrl = 'cerrar_sesion.php?ajax=1&token=' + encodeURIComponent(token) + '&userId=' + encodeURIComponent(userId);
                    console.log('🔍 Llamando a cerrar_sesion.php para invalidar token...', { userId, url: logoutUrl });
                    
                    try {
                        const response = await fetch(logoutUrl, {
                            method: 'GET'
                        });

                        console.log('📥 Respuesta recibida:', response.status, response.statusText);

                        if (response.ok) {
                            const result = await response.json();
                            console.log('📦 Resultado:', result);
                            if (result.success) {
                                console.log('✅ Token invalidado correctamente en el backend');
                            } else {
                                console.warn('⚠️ Error al invalidar token:', result.message);
                                alert('Error al invalidar token: ' + (result.message || 'Error desconocido'));
                            }
                        } else {
                            const errorText = await response.text();
                            console.warn('⚠️ Error al invalidar token en el backend:', response.status, errorText);
                            alert('Error al invalidar token: HTTP ' + response.status);
                        }
                    } catch (error) {
                        console.error('❌ Error al llamar a cerrar_sesion.php:', error);
                        alert('Error al conectar con el servidor: ' + error.message);
                    }
                } else {
                    console.warn('⚠️ No se encontró token o userId, no se puede invalidar en el backend');
                    if (!token) {
                        alert('No se encontró token. La sesión local se limpiará pero el token no se invalidará en el servidor.');
                    }
                    if (!userId) {
                        alert('No se encontró userId. La sesión local se limpiará pero el token no se invalidará en el servidor.');
                    }
                }

                // Limpiar datos locales (siempre se hace, independientemente del resultado del backend)
                sessionStorage.removeItem('userData');
                localStorage.removeItem('userData');
                sessionStorage.removeItem('selectedCompany');
                console.log('✅ Datos locales limpiados');
                
                // Redirigir al login
                window.location.href = 'login.php';
            } catch (error) {
                console.error('Error en logout:', error);
                // Aún así, limpiar datos locales y redirigir
                sessionStorage.removeItem('userData');
                localStorage.removeItem('userData');
                sessionStorage.removeItem('selectedCompany');
                window.location.href = 'login.php';
            }
        }

        // Botón de cerrar sesión (si existe)
        const logoutBtn = document.querySelector('.logout-btn, [data-logout]');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                if (confirm('¿Estás seguro de que deseas cerrar sesión?')) {
                    logout();
                }
            });
        }

        // Agregar event listeners a todos los enlaces que apuntan a #clientes (excepto los sidebar-links que ya tienen su propio handler)
        document.addEventListener('DOMContentLoaded', () => {
            const clientesLinks = document.querySelectorAll('a[href="#clientes"]:not(.sidebar-link)');
            clientesLinks.forEach(link => {
                link.addEventListener('click', async (e) => {
                    e.preventDefault();
                    
                    // Activar la sección de clientes
                    sidebarLinks.forEach(l => l.classList.remove('active'));
                    sections.forEach(s => s.classList.remove('active'));
                    
                    const clientesLink = document.querySelector('.sidebar-link[data-section="clientes"]');
                    if (clientesLink) {
                        clientesLink.classList.add('active');
                    }
                    
                    const clientesSection = document.getElementById('clientes-section');
                    if (clientesSection) {
                        clientesSection.classList.add('active');
                    }
                    
                    // Actualizar título con nombre de sucursal si está disponible (parte blanca)
                    const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                    let sucursalNombre = null;
                    if (userDataStr) {
                        try {
                            const userData = JSON.parse(userDataStr);
                            sucursalNombre = userData.sucursalNombre;
                        } catch (e) {
                            console.error('Error al parsear userData:', e);
                        }
                    }
                    
                    if (pageTitle) {
                        pageTitle.textContent = sucursalNombre || 'Clientes';
                    }
                    
                    // Cargar contenido
                    await loadClientesContent();
                });
            });
        });

        // Función para cargar el nombre de la empresa del usuario (para el sidebar - parte oscura)
        async function loadUserEmpresa() {
            try {
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                if (!userDataStr) {
                    console.warn('⚠️ No se encontró userData en loadUserEmpresa');
                    updateSidebarWithEmpresa(null);
                    return;
                }

                const userData = JSON.parse(userDataStr);
                const empresaNombre = userData.empresaNombre;
                const empresaId = userData.empresaId;

                console.log('🔍 loadUserEmpresa - Datos encontrados:', {
                    empresaNombre: empresaNombre,
                    empresaId: empresaId,
                    tieneToken: !!userData.token
                });

                // Si ya tenemos el nombre de la empresa, usarlo directamente
                if (empresaNombre) {
                    console.log('✅ Usando empresaNombre de userData:', empresaNombre);
                    updateSidebarWithEmpresa(empresaNombre);
                    return;
                }

                // Si tenemos empresaId pero no nombre, intentar obtenerlo desde la API
                if (empresaId && userData.token && empresaId !== 0 && empresaId !== '0') {
                    try {
                        console.log('📡 Obteniendo nombre de empresa desde API, empresaId:', empresaId);
                        const empresaUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/empresas/${empresaId}`;
                        const empresaResponse = await fetch(empresaUrl, {
                            method: 'GET',
                            headers: {
                                'Authorization': 'Bearer ' + userData.token,
                                'Content-Type': 'application/json'
                            }
                        });

                        console.log('📥 Respuesta de API empresa:', empresaResponse.status, empresaResponse.statusText);

                        if (empresaResponse.ok) {
                            const empresaResult = await empresaResponse.json();
                            const empresa = empresaResult.data || empresaResult;
                            const nombreEmpresa = empresa.nombreEmpresa || empresa.nombre || empresa.razonSocial || null;

                            if (nombreEmpresa) {
                                // Guardar en userData para futuras cargas
                                userData.empresaNombre = nombreEmpresa;
                                sessionStorage.setItem('userData', JSON.stringify(userData));
                                if (localStorage.getItem('userData')) {
                                    localStorage.setItem('userData', JSON.stringify(userData));
                                }
                                
                                console.log('✅ Nombre de empresa obtenido y guardado:', nombreEmpresa);
                                updateSidebarWithEmpresa(nombreEmpresa);
                                return;
                            } else {
                                console.warn('⚠️ No se encontró nombreEmpresa en la respuesta de la API');
                            }
                        } else {
                            const errorText = await empresaResponse.text();
                            console.warn('⚠️ Error al obtener empresa desde API:', empresaResponse.status, errorText);
                        }
                    } catch (e) {
                        console.error('❌ Error al obtener información de la empresa:', e);
                    }
                } else {
                    console.warn('⚠️ No se puede obtener empresa - empresaId:', empresaId, 'tieneToken:', !!userData.token);
                }

                // Si no se pudo obtener, usar fallback
                updateSidebarWithEmpresa(null);
            } catch (e) {
                console.error('❌ Error al cargar empresa del usuario:', e);
                updateSidebarWithEmpresa(null);
            }
        }

        // Función para actualizar el sidebar con el nombre de la empresa (parte oscura)
        function updateSidebarWithEmpresa(empresaNombre) {
            const sidebarTitle = document.querySelector('.sidebar-title');
            if (sidebarTitle) {
                if (empresaNombre) {
                    sidebarTitle.textContent = empresaNombre;
                    console.log('✅ Sidebar actualizado con nombre de empresa:', empresaNombre);
                } else {
                    sidebarTitle.textContent = 'XXXXXXX';
                }
            }
        }

        // Función para cargar el nombre de la sucursal del usuario (para el contenido principal - parte blanca)
        async function loadUserSucursal() {
            try {
                const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                if (!userDataStr) {
                    console.warn('⚠️ No se encontró userData en loadUserSucursal');
                    updatePageTitleWithSucursal(null);
                    return;
                }

                const userData = JSON.parse(userDataStr);
                const token = userData.token;
                const userId = userData.id || userData.userId || userData.idUsuario;
                const sucursalId = userData.sucursalId || userData.idSucursal;
                const sucursalNombre = userData.sucursalNombre;

                console.log('🔍 loadUserSucursal - Datos encontrados:', {
                    sucursalNombre: sucursalNombre,
                    sucursalId: sucursalId,
                    userId: userId,
                    tieneToken: !!token
                });

                // Si ya tenemos el nombre de la sucursal, usarlo directamente
                if (sucursalNombre) {
                    console.log('✅ Usando sucursalNombre de userData:', sucursalNombre);
                    updatePageTitleWithSucursal(sucursalNombre);
                    return;
                }

                // Si no tenemos sucursalId, intentar obtenerlo del usuario desde la API
                if (!sucursalId && userId && token) {
                    try {
                        const userUrl = `http://turistas.spring.informaticapp.com:2410/api/v1/usuarios/${userId}`;
                        const userResponse = await fetch(userUrl, {
                            method: 'GET',
                            headers: {
                                'Authorization': 'Bearer ' + token,
                                'Content-Type': 'application/json'
                            }
                        });

                        if (userResponse.ok) {
                            const userResult = await userResponse.json();
                            const usuario = userResult.data || userResult;
                            const idSucursal = usuario.sucursalId || usuario.idSucursal || usuario.sucursal?.idSucursal || usuario.sucursal?.id;
                            
                            if (idSucursal) {
                                console.log('✅ sucursalId obtenido del usuario:', idSucursal);
                                // Guardar sucursalId en userData
                                userData.sucursalId = idSucursal;
                                sessionStorage.setItem('userData', JSON.stringify(userData));
                                if (localStorage.getItem('userData')) {
                                    localStorage.setItem('userData', JSON.stringify(userData));
                                }
                                
                                // Obtener información de la sucursal
                                await loadSucursalInfo(idSucursal, token);
                            } else {
                                console.warn('⚠️ No se encontró sucursalId en la información del usuario');
                                updatePageTitleWithSucursal(null);
                            }
                        } else {
                            updatePageTitleWithSucursal(null);
                        }
                    } catch (e) {
                        console.error('Error al obtener información del usuario:', e);
                        updatePageTitleWithSucursal(null);
                    }
                } else if (sucursalId && token) {
                    // Si ya tenemos sucursalId, obtener la información directamente
                    console.log('✅ Usando sucursalId existente:', sucursalId);
                    await loadSucursalInfo(sucursalId, token);
                } else {
                    console.warn('⚠️ No se puede obtener sucursal - sucursalId:', sucursalId, 'tieneToken:', !!token);
                    updatePageTitleWithSucursal(null);
                }
            } catch (e) {
                console.error('Error al cargar sucursal del usuario:', e);
                updatePageTitleWithSucursal(null);
            }
        }

        // Función para obtener información de la sucursal y actualizar el título (parte blanca)
        async function loadSucursalInfo(sucursalId, token) {
            try {
                console.log('📡 Obteniendo información de sucursal desde API, sucursalId:', sucursalId);
                const url = `http://turistas.spring.informaticapp.com:2410/api/v1/sucursales/${sucursalId}`;
                
                const response = await fetch(url, {
                    method: 'GET',
                    headers: {
                        'Authorization': 'Bearer ' + token,
                        'Content-Type': 'application/json'
                    }
                });

                console.log('📥 Respuesta de API sucursal:', response.status, response.statusText);

                if (response.ok) {
                    const result = await response.json();
                    const sucursal = result.data || result;
                    const nombreSucursal = sucursal.nombreSucursal || sucursal.nombre || 'Sucursal';

                    console.log('✅ Nombre de sucursal obtenido:', nombreSucursal);

                    // Actualizar el título de la página (parte blanca)
                    updatePageTitleWithSucursal(nombreSucursal);

                    // Guardar en userData para futuras cargas
                    const userDataStr = sessionStorage.getItem('userData') || localStorage.getItem('userData');
                    if (userDataStr) {
                        try {
                            const userData = JSON.parse(userDataStr);
                            userData.sucursalNombre = nombreSucursal;
                            userData.sucursalId = sucursalId;
                            sessionStorage.setItem('userData', JSON.stringify(userData));
                            if (localStorage.getItem('userData')) {
                                localStorage.setItem('userData', JSON.stringify(userData));
                            }
                            console.log('💾 userData actualizado con sucursalNombre:', nombreSucursal);
                        } catch (e) {
                            console.error('❌ Error al actualizar userData:', e);
                        }
                    }
                } else {
                    const errorText = await response.text();
                    console.warn('⚠️ Error al obtener sucursal desde API:', response.status, errorText);
                    updatePageTitleWithSucursal(null);
                }
            } catch (e) {
                console.error('❌ Error al cargar información de sucursal:', e);
                updatePageTitleWithSucursal(null);
            }
        }

        // Función para actualizar el título de la página con el nombre de la sucursal (parte blanca)
        function updatePageTitleWithSucursal(sucursalNombre) {
            const pageTitle = document.getElementById('pageTitle');
            if (pageTitle) {
                if (sucursalNombre) {
                    pageTitle.textContent = sucursalNombre;
                    console.log('✅ Título de página actualizado con nombre de sucursal:', sucursalNombre);
                } else {
                    pageTitle.textContent = 'Reservas';
                }
            }
        }

        // Sidebar solo muestra Reservas y Reportes, no se requiere lógica adicional
        function hideSucursalesForGerente() {
            return;
        }

        // Inicializar información del usuario y empresa
        // Esperar a que el DOM esté completamente cargado
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                loadUserInfo();
                loadSelectedCompany();
                loadUserEmpresa(); // Cargar nombre de empresa (para sidebar - parte oscura)
                loadUserSucursal(); // Cargar nombre de sucursal (para contenido principal - parte blanca)
                hideSucursalesForGerente(); // Ocultar sucursales si es Gerente
            });
        } else {
            // El DOM ya está cargado
            loadUserInfo();
            loadSelectedCompany();
            loadUserEmpresa(); // Cargar nombre de empresa (para sidebar - parte oscura)
            loadUserSucursal(); // Cargar nombre de sucursal (para contenido principal - parte blanca)
            hideSucursalesForGerente(); // Ocultar sucursales si es Gerente
        }
    </script>
</body>
</html>

