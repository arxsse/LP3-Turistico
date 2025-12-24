<?php
// Login con preservación del diseño original y ajustes para el backend multi-tenant
// Ajusta BASE_API_URL si cambias la URL pública del backend.

define('BASE_API_URL', 'http://turistas.spring.informaticapp.com:2410');
define('LOGIN_PATH', '/api/v1/auth/login'); // Ruta del endpoint de login

defaultHeaders();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    handleLoginRequest();
    exit;
}

function defaultHeaders(): void
{
    if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Methods: POST, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type');
        exit;
    }
}

function handleLoginRequest(): void
{
    header('Content-Type: application/json');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type');

    $payload = json_decode(file_get_contents('php://input'), true) ?: [];
    $email = isset($payload['email']) ? trim($payload['email']) : '';
    $password = isset($payload['password']) ? (string)$payload['password'] : '';

    if ($email === '' || $password === '') {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Email y contraseña son requeridos'
        ]);
        return;
    }

    $loginBody = json_encode([
        'email' => $email,
        'password' => $password
    ]);

    $loginCurl = curl_init();
    curl_setopt_array($loginCurl, [
        CURLOPT_URL => BASE_API_URL . LOGIN_PATH,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 10,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
        CURLOPT_CUSTOMREQUEST => 'POST',
        CURLOPT_POSTFIELDS => $loginBody,
        CURLOPT_HTTPHEADER => [
            'Content-Type: application/json'
        ],
    ]);

    $loginResponse = curl_exec($loginCurl);
    $loginCode = curl_getinfo($loginCurl, CURLINFO_HTTP_CODE);
    $loginError = curl_error($loginCurl);
    curl_close($loginCurl);

    if ($loginError) {
        http_response_code(502);
        echo json_encode([
            'success' => false,
            'message' => 'Error al conectar con el backend: ' . $loginError,
            'debug' => ['url' => BASE_API_URL . LOGIN_PATH]
        ]);
        return;
    }

    // Intentar decodificar la respuesta
    $loginData = json_decode($loginResponse, true);
    
    // Si el JSON no se pudo decodificar, intentar como string
    if ($loginData === null && json_last_error() !== JSON_ERROR_NONE) {
        // Para debugging: descomentar la siguiente línea para ver la respuesta cruda
        // error_log("Login API Response: " . substr($loginResponse, 0, 1000));
        
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Error al procesar la respuesta del servidor',
            'debug' => [
                'http_code' => $loginCode,
                'response_raw' => substr($loginResponse, 0, 500),
                'json_error' => json_last_error_msg(),
                'url' => BASE_API_URL . LOGIN_PATH
            ]
        ]);
        return;
    }

    // Si no hay datos, usar array vacío
    $loginData = $loginData ?: [];

    // Verificar si la respuesta tiene una estructura anidada (ej: {data: {...}})
    if (isset($loginData['data']) && is_array($loginData['data'])) {
        $loginData = array_merge($loginData, $loginData['data']);
    }

    if ($loginCode !== 200 || (!isset($loginData['token']) && !isset($loginData['accessToken']))) {
        http_response_code($loginCode ?: 401);
        echo json_encode([
            'success' => false,
            'message' => $loginData['message'] ?? $loginData['error'] ?? 'Credenciales inválidas',
            'debug' => [
                'http_code' => $loginCode,
                'response_keys' => array_keys($loginData)
            ]
        ]);
        return;
    }

    // Obtener token (puede venir como 'token' o 'accessToken')
    $token = $loginData['token'] ?? $loginData['accessToken'] ?? null;
    
    if (!$token) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'No se recibió token de autenticación',
            'debug' => ['response_keys' => array_keys($loginData)]
        ]);
        return;
    }

    // ============================================
    // PASO 1: VERIFICAR ROLES PRIMERO
    // ============================================
    
    // Extraer datos básicos del usuario
    $userId = $loginData['userId']
        ?? $loginData['idUsuario']
        ?? $loginData['id']
        ?? ($loginData['usuario']['id'] ?? $loginData['usuario']['idUsuario'] ?? null)
        ?? ($loginData['user']['id'] ?? $loginData['user']['idUsuario'] ?? null);
    $roles = $loginData['roles'] ?? [];
    $permisos = $loginData['permisos'] ?? [];
    
    // Extraer rolId de la respuesta inicial (puede venir en diferentes formatos)
    $rolId = $loginData['rolId'] ?? $loginData['idRol'] ?? $loginData['roleId'] ?? null;
    if ($rolId === null && isset($loginData['rol']) && is_array($loginData['rol'])) {
        $rolId = $loginData['rol']['id'] ?? $loginData['rol']['idRol'] ?? $loginData['rol']['rolId'] ?? null;
    }

    // Obtener datos completos del usuario si no están en la respuesta inicial
    $usuario = $loginData['usuario'] ?? $loginData['user'] ?? null;
    
    // Si no tenemos datos del usuario y tenemos userId, obtenerlos de la API
    // IMPORTANTE: Para gerentes, siempre intentar obtener datos completos desde la API
    // para asegurar que tengamos empresa y sucursal
    if (($usuario === null || !isset($usuario['empresa']) || !isset($usuario['sucursal'])) && $userId !== null) {
        $usuarioCompleto = fetchResource('/api/v1/usuarios/' . urlencode($userId), $token);
        if (is_array($usuarioCompleto)) {
            // Si ya teníamos usuario, combinar los datos
            if (is_array($usuario)) {
                $usuario = array_merge($usuario, $usuarioCompleto);
            } else {
                $usuario = $usuarioCompleto;
            }
        }
    }
    
    // Si todavía no tenemos rolId, intentar obtenerlo del usuario
    if ($rolId === null && is_array($usuario)) {
        $rolId = $usuario['rolId'] ?? $usuario['idRol'] ?? null;
        if ($rolId === null && isset($usuario['rol']) && is_array($usuario['rol'])) {
            $rolId = $usuario['rol']['id'] ?? $usuario['rol']['idRol'] ?? $usuario['rol']['rolId'] ?? null;
        }
    }

    // Determinar el rol: si rolId es 1, es superadmin
    $rol = inferRole($roles, $rolId);
    
    // Extraer nombre del usuario (necesario siempre)
    $nombreUsuario = 'Usuario';
    
    // PRIORIDAD 1: Desde el objeto usuario (más completo)
    if (is_array($usuario)) {
        $nombreUsuario = $usuario['nombreCompleto']
            ?? trim(($usuario['nombre'] ?? $usuario['nombres'] ?? '') . ' ' . ($usuario['apellido'] ?? $usuario['apellidos'] ?? ''))
            ?: ($usuario['email'] ?? 'Usuario');
    }
    
    // PRIORIDAD 2: Desde la respuesta del login
    if (($nombreUsuario === 'Usuario' || empty(trim($nombreUsuario))) && isset($loginData)) {
        $nombreDesdeLogin = $loginData['nombreCompleto'] 
            ?? $loginData['nombre'] 
            ?? trim(($loginData['nombres'] ?? '') . ' ' . ($loginData['apellidos'] ?? ''))
            ?? null;
        
        if ($nombreDesdeLogin && !empty(trim($nombreDesdeLogin))) {
            $nombreUsuario = $nombreDesdeLogin;
        }
    }
    
    // PRIORIDAD 3: Si aún no tenemos nombre, intentar obtenerlo desde la API
    if (($nombreUsuario === 'Usuario' || empty(trim($nombreUsuario))) && $userId !== null && $token) {
        $usuarioDesdeAPI = fetchResource('/api/v1/usuarios/' . urlencode($userId), $token);
        if (is_array($usuarioDesdeAPI)) {
            $nombreDesdeAPI = $usuarioDesdeAPI['nombreCompleto']
                ?? trim(($usuarioDesdeAPI['nombre'] ?? $usuarioDesdeAPI['nombres'] ?? '') . ' ' . ($usuarioDesdeAPI['apellido'] ?? $usuarioDesdeAPI['apellidos'] ?? ''))
                ?? null;
            
            if ($nombreDesdeAPI && !empty(trim($nombreDesdeAPI))) {
                $nombreUsuario = $nombreDesdeAPI;
            }
        }
    }
    
    // PRIORIDAD 4: Como último recurso, usar el email
    if (($nombreUsuario === 'Usuario' || empty(trim($nombreUsuario))) && isset($loginData['email'])) {
        $nombreUsuario = $loginData['nombre'] ?? $loginData['nombreCompleto'] ?? explode('@', $loginData['email'])[0];
    }

    // ============================================
    // PASO 2: VALIDAR EMPRESAS (solo si NO es superadmin)
    // ============================================
    
    $empresaId = null;
    $empresaNombre = null;
    
    // IMPORTANTE: Si el usuario es superadmin, NO se valida ni obtiene información de empresa
    // Los superadmin no necesitan estar asociados a una empresa específica
    if ($rol === 'superadmin') {
        // No hacer ninguna verificación de empresa para superadmin
        // Dejamos empresaId y empresaNombre como null
    } else {
        // Extraer empresaId de la respuesta inicial
        $empresaId = $loginData['empresaId'] ?? $loginData['idEmpresa'] ?? $loginData['empresa']['id'] ?? $loginData['empresa']['idEmpresa'] ?? null;
        
        // Verificar si empresaId es 0 (acceso directo a superadmin)
        if ($empresaId !== null && (int)$empresaId === 0) {
            $rol = 'superadmin';
            // Mantener empresaId como 0 para que el frontend pueda detectarlo
            $empresaId = 0;
            $empresaNombre = null;
        } else {
            // Solo obtener datos de empresa si NO es superadmin y empresaId no es 0
            // Si no tenemos empresaId y tenemos usuario, intentar obtenerlo del usuario
            if ($empresaId === null && is_array($usuario)) {
                $empresaId = $usuario['empresa']['idEmpresa'] 
                    ?? $usuario['empresa']['id'] 
                    ?? $usuario['empresaId'] 
                    ?? $usuario['idEmpresa'] 
                    ?? null;
            }
            
            // Verificar nuevamente si el empresaId obtenido del usuario es 0
            if ($empresaId !== null && (int)$empresaId === 0) {
                $rol = 'superadmin';
                // Mantener empresaId como 0 para que el frontend pueda detectarlo
                $empresaId = 0;
                $empresaNombre = null;
            } else {
                // Obtener nombre de la empresa si tenemos empresaId válido
                // PRIORIDAD 1: Desde la respuesta del login
                $empresaNombre = $loginData['empresaNombre'] 
                    ?? $loginData['empresa']['nombreEmpresa'] 
                    ?? $loginData['empresa']['nombre'] 
                    ?? null;
                
                // PRIORIDAD 2: Desde el objeto usuario (puede tener empresa anidada)
                if ($empresaNombre === null && is_array($usuario)) {
                    $empresaNombre = $usuario['empresa']['nombreEmpresa']
                        ?? $usuario['empresa']['nombre']
                        ?? $usuario['empresaNombre']
                        ?? null;
                }
                    
                // PRIORIDAD 3: Intentar obtener desde la API (puede fallar si no tiene permisos)
                    // IMPORTANTE: Para gerentes, siempre intentar obtener desde la API si no tenemos nombre
                    if ($empresaNombre === null && $empresaId !== null && $empresaId !== 0) {
                    $empresa = fetchResource('/api/v1/empresas/' . urlencode($empresaId), $token);
                    if (is_array($empresa)) {
                        $empresaNombre = $empresa['nombreEmpresa']
                            ?? $empresa['nombre']
                            ?? $empresa['razonSocial']
                            ?? null;
                    }
                }
            }
        }
    }

    // Extraer información de sucursal del usuario
    $sucursalId = null;
    $sucursalNombre = null;
    
    // PRIORIDAD 1: Desde la respuesta del login
    $sucursalId = $loginData['sucursalId'] ?? $loginData['idSucursal'] ?? null;
    $sucursalNombre = $loginData['sucursalNombre'] ?? null;
    
    // PRIORIDAD 2: Desde el objeto usuario
    if (is_array($usuario)) {
        if ($sucursalId === null) {
        $sucursalId = $usuario['sucursalId'] 
            ?? $usuario['idSucursal'] 
            ?? ($usuario['sucursal']['idSucursal'] ?? $usuario['sucursal']['id'] ?? null);
        }
        
        if ($sucursalNombre === null) {
        $sucursalNombre = $usuario['sucursalNombre']
            ?? ($usuario['sucursal']['nombreSucursal'] ?? $usuario['sucursal']['nombre'] ?? null);
        }
    }
    
    // PRIORIDAD 3: Si tenemos sucursalId pero no nombre, intentar obtenerlo desde la API
    if ($sucursalId && !$sucursalNombre && $token) {
        $sucursal = fetchResource('/api/v1/sucursales/' . urlencode($sucursalId), $token);
        if (is_array($sucursal)) {
            $sucursalNombre = $sucursal['nombreSucursal'] 
                ?? $sucursal['nombre'] 
                ?? null;
        }
    }

    // Debug: Log de datos obtenidos (solo en desarrollo)
    // error_log("Login - Rol: $rol, EmpresaId: " . ($empresaId ?? 'null') . ", EmpresaNombre: " . ($empresaNombre ?? 'null') . ", SucursalId: " . ($sucursalId ?? 'null') . ", SucursalNombre: " . ($sucursalNombre ?? 'null'));

    echo json_encode([
        'success' => true,
        'message' => 'Login exitoso',
        'usuario' => [
            'id' => $userId,
            'userId' => $userId,
            'idUsuario' => $userId,
            'email' => $email,
            'nombre' => $nombreUsuario,
            'rol' => $rol,
            'rolId' => $rolId,
            'roles' => $roles,
            'permisos' => $permisos,
            'empresaId' => $empresaId,
            'empresaNombre' => $empresaNombre,
            'sucursalId' => $sucursalId,
            'sucursalNombre' => $sucursalNombre,
            'token' => $token,
        ],
        'debug' => [
            'rol_detectado' => $rol,
            'rolId' => $rolId,
            'empresaId' => $empresaId,
            'empresaNombre' => $empresaNombre,
            'sucursalId' => $sucursalId,
            'sucursalNombre' => $sucursalNombre,
            'usuario_obtenido_desde_api' => $usuario !== null && isset($usuario['id'])
        ]
    ]);
}

function fetchResource(string $path, string $token): ?array
{
    $curl = curl_init();
    curl_setopt_array($curl, [
        CURLOPT_URL => BASE_API_URL . $path,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT => 10,
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
        CURLOPT_CUSTOMREQUEST => 'GET',
        CURLOPT_HTTPHEADER => [
            'Authorization: Bearer ' . $token,
            'Content-Type: application/json'
        ],
    ]);

    $response = curl_exec($curl);
    $code = curl_getinfo($curl, CURLINFO_HTTP_CODE);
    $error = curl_error($curl);
    curl_close($curl);

    if ($error || $code !== 200 || !$response) {
        return null;
    }

    $decoded = json_decode($response, true);
    
    // Si no se puede decodificar, retornar null
    if ($decoded === null && json_last_error() !== JSON_ERROR_NONE) {
        return null;
    }
    
    // Manejar diferentes estructuras de respuesta
    if (isset($decoded['data']) && is_array($decoded['data'])) {
        return $decoded['data'];
    }
    
    // Si es un array directo, retornarlo
    return is_array($decoded) ? $decoded : null;
}

function inferRole(array $roles, $rolId = null): string
{
    // Si el rolId es 1, es superadmin
    if ($rolId !== null && (int)$rolId === 1) {
        return 'superadmin';
    }
    
    // PRIORIDAD 1: Verificar por rolId primero (más confiable)
    if ($rolId !== null) {
        $rolIdInt = (int)$rolId;
        if ($rolIdInt === 1) {
            return 'superadmin';
        } elseif ($rolIdInt === 2) {
            return 'admin';
        } elseif ($rolIdInt === 3 || $rolIdInt === 4) {
            // rolId 3 o 4 = Gerente (puede variar según el sistema)
            return 'gerente';
        }
    }
    
    // PRIORIDAD 2: Verificar en el array de roles
    $roleLabel = 'usuario';
    foreach ($roles as $role) {
        $roleUpper = strtoupper((string)$role);
        if (str_contains($roleUpper, 'SUPERADMIN')) {
            return 'superadmin';
        }
        // Detectar Gerente ANTES de admin (para evitar que admin sobrescriba gerente)
        if (str_contains($roleUpper, 'GERENTE') || str_contains($roleUpper, 'MANAGER')) {
            $roleLabel = 'gerente';
        } elseif (str_contains($roleUpper, 'ADMIN') && $roleLabel !== 'superadmin' && $roleLabel !== 'gerente') {
            $roleLabel = 'admin';
        }
    }
    
    return $roleLabel;
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar Sesión - Sistema Turístico</title>
    <link rel="stylesheet" href="login.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
</head>
<body>
    <div class="login-container">
        <div class="login-wrapper">
            <div class="login-right">
                <div class="login-form-container">
                    <div class="login-header">
                        <h2 class="login-title">Bienvenido</h2>
                        <p class="login-subtitle">Ingresa tus credenciales para continuar</p>
                    </div>

                    <div id="loginMessage" class="login-message" style="display: none;">
                        <i class="fas fa-exclamation-circle"></i>
                        <span id="loginMessageText"></span>
                    </div>

                    <form id="loginForm" class="login-form">
                        <div class="form-group">
                            <label for="email" class="form-label">
                                <i class="fas fa-envelope"></i>
                                Email
                            </label>
                            <div class="input-wrapper">
                                <input
                                    type="email"
                                    id="email"
                                    name="email"
                                    class="form-input"
                                    placeholder="usuario@empresa.com"
                                    required
                                    autocomplete="email"
                                >
                                <span class="input-icon">
                                    <i class="fas fa-envelope"></i>
                                </span>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="password" class="form-label">
                                <i class="fas fa-lock"></i>
                                Contraseña
                            </label>
                            <div class="input-wrapper">
                                <input
                                    type="password"
                                    id="password"
                                    name="password"
                                    class="form-input"
                                    placeholder="Ingresa tu contraseña"
                                    required
                                    autocomplete="current-password"
                                >
                                <span class="input-icon">
                                    <i class="fas fa-lock"></i>
                                </span>
                                <button
                                    type="button"
                                    class="toggle-password"
                                    id="togglePassword"
                                    aria-label="Mostrar/Ocultar contraseña"
                                >
                                    <i class="fas fa-eye"></i>
                                </button>
                            </div>
                        </div>

                        <div class="form-options">
                            <label class="checkbox-wrapper">
                                <input type="checkbox" id="remember" name="remember">
                                <span class="checkbox-label">Recordar sesión</span>
                            </label>
                            <a href="#forgot" class="forgot-link"></a>
                        </div>

                        <button type="submit" class="login-button" id="loginButton">
                            <span class="button-text">Iniciar Sesión</span>
                            <span class="button-loader" style="display: none;">
                                <i class="fas fa-spinner fa-spin"></i>
                            </span>
                        </button>

                    </form>
                </div>
            </div>
        </div>
    </div>

    <script>
        const togglePassword = document.getElementById('togglePassword');
        const passwordInput = document.getElementById('password');
        const passwordIcon = togglePassword.querySelector('i');
        // Candado del campo contraseña (solo este, no el del email)
        const passwordLockIcon = document.querySelector('#password + .input-icon');

        // Mostrar el "ojito" solo cuando haya texto en el campo
        // y ocultar el candado cuando el usuario empiece a escribir
        function updatePasswordToggleVisibility() {
            if (!passwordInput || !togglePassword) return;

            const hasValue = passwordInput.value && passwordInput.value.length > 0;

            if (hasValue) {
                togglePassword.style.display = 'flex';
                if (passwordLockIcon) {
                    passwordLockIcon.style.display = 'none';
                }
            } else {
                togglePassword.style.display = 'none';
                if (passwordLockIcon) {
                    passwordLockIcon.style.display = 'inline-flex';
                }
            }
        }

        // Estado inicial (por si el navegador autocompleta la contraseña)
        updatePasswordToggleVisibility();

        // Escuchar cambios en el campo de contraseña
        if (passwordInput) {
            passwordInput.addEventListener('input', updatePasswordToggleVisibility);
        }

        // Botón para mostrar/ocultar la contraseña
        togglePassword.addEventListener('click', () => {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);
            passwordIcon.classList.toggle('fa-eye');
            passwordIcon.classList.toggle('fa-eye-slash');
        });

        const loginForm = document.getElementById('loginForm');
        const loginButton = document.getElementById('loginButton');
        const buttonText = loginButton.querySelector('.button-text');
        const buttonLoader = loginButton.querySelector('.button-loader');
        const loginMessage = document.getElementById('loginMessage');
        const loginMessageText = document.getElementById('loginMessageText');

        loginForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const remember = document.getElementById('remember').checked;

            if (!email || !password) {
                showMessage('Por favor, completa todos los campos', 'error');
                return;
            }

            buttonText.style.display = 'none';
            buttonLoader.style.display = 'inline-block';
            loginButton.disabled = true;

            try {
                const response = await fetch('login.php', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ email, password })
                });

                // Verificar si la respuesta es válida
                if (!response.ok) {
                    const errorText = await response.text();
                    let errorData;
                    try {
                        errorData = JSON.parse(errorText);
                    } catch (e) {
                        errorData = { message: errorText || 'Error al conectar con el servidor' };
                    }
                    throw new Error(errorData.message || errorData.error || `Error ${response.status}: ${response.statusText}`);
                }
                
                const data = await response.json();
                
                // Log para debugging (puedes eliminarlo después)
                console.log('Respuesta del servidor:', data);

                if (!data.success) {
                    // Mostrar información de debug en la consola
                    if (data.debug) {
                        console.error('Error en login - Debug info:', data.debug);
                    }
                    throw new Error(data.message || 'Credenciales inválidas');
                }

                if (!data.usuario) {
                    console.error('Error: No se recibieron datos del usuario', data);
                    throw new Error('No se recibieron datos del usuario. Por favor, intenta nuevamente.');
                }

                // Normalizar el rol que viene del servidor
                let rolNormalizado = String(data.usuario.rol || '').toLowerCase().trim();
                
                // Si el rol no está claro, intentar determinarlo por rolId
                if ((!rolNormalizado || rolNormalizado === 'usuario') && data.usuario.rolId) {
                    const rolIdInt = parseInt(data.usuario.rolId);
                    if (rolIdInt === 1) {
                        rolNormalizado = 'superadmin';
                    } else if (rolIdInt === 2) {
                        rolNormalizado = 'admin';
                    } else if (rolIdInt === 3 || rolIdInt === 4) {
                        // rolId 3 o 4 = Gerente (puede variar según el sistema)
                        rolNormalizado = 'gerente';
                        console.log('✅ Rol detectado como gerente por rolId =', rolIdInt);
                    }
                }
                
                // Si aún no está claro, verificar en el array de roles
                if ((!rolNormalizado || rolNormalizado === 'usuario') && Array.isArray(data.usuario.roles) && data.usuario.roles.length > 0) {
                    const rolesUpper = data.usuario.roles.map(r => String(r).toUpperCase());
                    if (rolesUpper.some(r => r.includes('GERENTE') || r.includes('MANAGER'))) {
                        rolNormalizado = 'gerente';
                    } else if (rolesUpper.some(r => r.includes('ADMIN') && !r.includes('SUPER'))) {
                        rolNormalizado = 'admin';
                    } else if (rolesUpper.some(r => r.includes('SUPERADMIN'))) {
                        rolNormalizado = 'superadmin';
                    }
                }

                const userData = {
                    id: data.usuario.id,
                    email: data.usuario.email || email,
                    nombre: data.usuario.nombre || 'Usuario',
                    rol: rolNormalizado || 'usuario',
                    rolId: data.usuario.rolId || null,
                    roles: data.usuario.roles || [],
                    empresaId: data.usuario.empresaId || null,
                    empresaNombre: data.usuario.empresaNombre || null,
                    sucursalId: data.usuario.sucursalId || null,
                    sucursalNombre: data.usuario.sucursalNombre || null,
                    token: data.usuario.token,
                    permisos: data.usuario.permisos || []
                };
                
                console.log('🔍 Rol normalizado en frontend:', {
                    rolOriginal: data.usuario.rol,
                    rolNormalizado: rolNormalizado,
                    rolId: data.usuario.rolId,
                    roles: data.usuario.roles,
                    rolFinal: userData.rol
                });
                
                // DEBUG: Mostrar información del usuario recibido
                console.log('🔍 DATOS DEL USUARIO RECIBIDOS:', {
                    rol: userData.rol,
                    rolId: userData.rolId,
                    roles: userData.roles,
                    email: userData.email,
                    nombre: userData.nombre,
                    empresaId: userData.empresaId,
                    empresaNombre: userData.empresaNombre,
                    sucursalId: userData.sucursalId,
                    sucursalNombre: userData.sucursalNombre
                });
                
                // Si hay información de debug en la respuesta, mostrarla
                if (data.debug) {
                    console.log('🔍 DEBUG DEL SERVIDOR:', data.debug);
                }
                
                // Si no tenemos empresaNombre pero sí tenemos empresaId, intentar obtenerlo desde la API
                // IMPORTANTE: Esto es especialmente necesario para gerentes
                if (!userData.empresaNombre && userData.empresaId && userData.token && userData.empresaId !== 0) {
                    console.log('⚠️ No se recibió empresaNombre, intentando obtenerlo desde la API...');
                    try {
                        const empresaResponse = await fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/empresas/${userData.empresaId}`, {
                            method: 'GET',
                            headers: {
                                'Authorization': 'Bearer ' + userData.token,
                                'Content-Type': 'application/json'
                            }
                        });
                        
                        if (empresaResponse.ok) {
                            const empresaData = await empresaResponse.json();
                            const empresa = empresaData.data || empresaData;
                            if (empresa && (empresa.nombreEmpresa || empresa.nombre || empresa.razonSocial)) {
                                userData.empresaNombre = empresa.nombreEmpresa || empresa.nombre || empresa.razonSocial;
                                console.log('✅ empresaNombre obtenido desde API:', userData.empresaNombre);
                            }
                        } else {
                            console.warn('⚠️ Error al obtener empresa desde API:', empresaResponse.status);
                        }
                    } catch (e) {
                        console.warn('⚠️ No se pudo obtener empresaNombre desde la API:', e);
                    }
                }
                
                // Si no tenemos sucursalNombre pero sí tenemos sucursalId, intentar obtenerlo desde la API
                // IMPORTANTE: Esto es especialmente necesario para gerentes
                if (!userData.sucursalNombre && userData.sucursalId && userData.token) {
                    console.log('⚠️ No se recibió sucursalNombre, intentando obtenerlo desde la API...');
                    try {
                        const sucursalResponse = await fetch(`http://turistas.spring.informaticapp.com:2410/api/v1/sucursales/${userData.sucursalId}`, {
                            method: 'GET',
                            headers: {
                                'Authorization': 'Bearer ' + userData.token,
                                'Content-Type': 'application/json'
                            }
                        });
                        
                        if (sucursalResponse.ok) {
                            const sucursalData = await sucursalResponse.json();
                            const sucursal = sucursalData.data || sucursalData;
                            if (sucursal && (sucursal.nombreSucursal || sucursal.nombre)) {
                                userData.sucursalNombre = sucursal.nombreSucursal || sucursal.nombre;
                                console.log('✅ sucursalNombre obtenido desde API:', userData.sucursalNombre);
                            }
                        } else {
                            console.warn('⚠️ Error al obtener sucursal desde API:', sucursalResponse.status);
                        }
                    } catch (e) {
                        console.warn('⚠️ No se pudo obtener sucursalNombre desde la API:', e);
                    }
                }
                
                // Verificar si es superadmin por rolId o roles (SIN considerar empresa)
                // IMPORTANTE: Si es superadmin por rol, NO se valida empresa
                const esSuperAdminPorRolId = userData.rolId === 1 || userData.rolId === '1' || parseInt(userData.rolId) === 1;
                const rolesArray = Array.isArray(userData.roles) ? userData.roles : [];
                const tieneRolSuperAdmin = rolesArray.some(r => {
                    const rolStr = String(r).toUpperCase();
                    return rolStr.includes('SUPERADMIN') || 
                           rolStr.includes('SUPER_ADMIN') ||
                           rolStr === 'ROLE_SUPERADMINISTRADOR' ||
                           rolStr === 'ROLE_SUPERADMIN';
                });
                
                // Verificar también si el rol ya viene como 'superadmin'
                const rolEsSuperAdmin = String(userData.rol).toLowerCase() === 'superadmin';
                
                // Verificar si es superadmin por ROL (sin considerar empresa)
                const esSuperAdminPorRol = esSuperAdminPorRolId || tieneRolSuperAdmin || rolEsSuperAdmin;
                
                console.log('🔍 VERIFICACIÓN DE SUPERADMIN:', {
                    rolId: userData.rolId,
                    esSuperAdminPorRolId: esSuperAdminPorRolId,
                    esSuperAdminPorRol: esSuperAdminPorRol,
                    roles: rolesArray,
                    tieneRolSuperAdmin: tieneRolSuperAdmin,
                    rolActual: userData.rol,
                    rolEsSuperAdmin: rolEsSuperAdmin
                });
                
                // Si es superadmin por ROL, forzar el rol (sin considerar empresa)
                if (esSuperAdminPorRol) {
                    userData.rol = 'superadmin';
                    console.log(`✅ Usuario detectado como SUPERADMIN por ROL - Redirigiendo a superadmin.php (sin validar empresa)`);
                } else {
                    console.log('⚠️ Usuario NO es superadmin - Rol:', userData.rol, 'RolId:', userData.rolId);
                }

                // Validar que tenemos al menos el token
                if (!userData.token) {
                    console.error('Error: No se recibió token de autenticación', data);
                    throw new Error('No se recibió token de autenticación. Por favor, intenta nuevamente.');
                }

                // Guardar userData actualizado (puede incluir empresaNombre obtenido desde API)
                console.log('💾 Guardando userData en storage:', {
                    rol: userData.rol,
                    rolId: userData.rolId,
                    roles: userData.roles,
                    nombre: userData.nombre,
                    empresaId: userData.empresaId,
                    empresaNombre: userData.empresaNombre,
                    sucursalId: userData.sucursalId,
                    sucursalNombre: userData.sucursalNombre
                });
                
                sessionStorage.setItem('userData', JSON.stringify(userData));
                if (remember) {
                    localStorage.setItem('userData', JSON.stringify(userData));
                }
                
                console.log('✅ userData guardado en storage correctamente');

                showMessage('Inicio de sesión exitoso. Redirigiendo...', 'success');

                setTimeout(() => {
                    // Obtener valores desde sessionStorage por si fueron actualizados
                    const storedData = sessionStorage.getItem('userData');
                    let finalUserData = userData;
                    if (storedData) {
                        try {
                            finalUserData = { ...userData, ...JSON.parse(storedData) };
                        } catch (e) {
                            console.error('Error al parsear userData del storage:', e);
                        }
                    }
                    
                    const rol = String(finalUserData.rol || '').toLowerCase().trim();
                    const rolId = finalUserData.rolId ? parseInt(finalUserData.rolId) : null;
                    const empresaId = finalUserData.empresaId !== null && finalUserData.empresaId !== undefined ? parseInt(finalUserData.empresaId) : null;
                    const rolesArrayCheck = Array.isArray(finalUserData.roles) ? finalUserData.roles : [];
                    
                    // Verificar nuevamente los roles para la redirección
                    const tieneRolSuperAdminCheck = rolesArrayCheck.some(r => {
                        const rolStr = String(r).toUpperCase();
                        return rolStr.includes('SUPERADMIN') || 
                               rolStr.includes('SUPER_ADMIN') ||
                               rolStr === 'ROLE_SUPERADMINISTRADOR' ||
                               rolStr === 'ROLE_SUPERADMIN';
                    });
                    
                    // Verificación final: ¿Es superadmin?
                    // PRIORIDAD 1: Si empresaId es 0, es superadmin
                    // PRIORIDAD 2: Si es superadmin por rolId o roles
                    const isSuperAdmin = empresaId === 0 || 
                                        rolId === 1 || 
                                        rol === 'superadmin' || 
                                        tieneRolSuperAdminCheck;
                    
                    console.log('🔍 DECISIÓN DE REDIRECCIÓN:', {
                        rol: rol,
                        rolId: rolId,
                        empresaId: empresaId,
                        isSuperAdmin: isSuperAdmin,
                        tieneRolSuperAdmin: tieneRolSuperAdminCheck,
                        roles: rolesArrayCheck
                    });
                    
                    // Redirección según el rol o empresaId
                    // PRIORIDAD 1: Si empresaId es 0 o es SUPERADMIN, ir a superadmin.php
                    if (empresaId === 0 || isSuperAdmin) {
                        const motivo = empresaId === 0 ? 'EmpresaId = 0' : `SuperAdmin - Rol: ${rol}, RolId: ${rolId}`;
                        console.log(`✅ REDIRIGIENDO A SUPERADMIN.PHP - Motivo: ${motivo}`);
                        window.location.href = 'superadmin.php';
                        return; // Asegurar que no continúe con otras redirecciones
                    }
                    
                    // PRIORIDAD 2: Si es admin o gerente, ir a admin.php
                    if (rol === 'admin' || rol === 'administrador' || rol === 'gerente') {
                        console.log('✅ REDIRIGIENDO A ADMIN.PHP - Rol:', rol);
                        const empresaIdStr = finalUserData.empresaId ? encodeURIComponent(finalUserData.empresaId) : '';
                        const destino = empresaIdStr ? `admin.php?idEmpresa=${empresaIdStr}` : 'admin.php';
                        window.location.href = destino;
                        return;
                    }
                    
                    // PRIORIDAD 3: Por defecto, ir a admin.php (solo si NO es superadmin)
                    console.log('⚠️ REDIRIGIENDO A ADMIN.PHP (por defecto) - Rol:', rol, 'RolId:', rolId, 'EmpresaId:', empresaId);
                    console.warn('⚠️ ADVERTENCIA: Usuario no identificado como superadmin');
                    const empresaIdStr = finalUserData.empresaId ? encodeURIComponent(finalUserData.empresaId) : '';
                    const destino = empresaIdStr ? `admin.php?idEmpresa=${empresaIdStr}` : 'admin.php';
                    window.location.href = destino;
                }, 1000);
            } catch (error) {
                console.error('Error en login:', error);
                console.error('Stack trace:', error.stack);
                
                // Mostrar mensaje de error más descriptivo
                let errorMessage = error.message || 'Error al autenticar';
                
                // Si el error contiene información de debug, agregarla al mensaje
                if (error.debug) {
                    console.error('Debug info:', error.debug);
                }
                
                showMessage(errorMessage, 'error');
                buttonText.style.display = 'inline-block';
                buttonLoader.style.display = 'none';
                loginButton.disabled = false;
            }
        });

        function showMessage(message, type) {
            loginMessageText.textContent = message;
            loginMessage.className = `login-message ${type}`;
            loginMessage.style.display = 'flex';
            setTimeout(() => {
                loginMessage.style.display = 'none';
            }, 5000);
        }

        const emailInput = document.getElementById('email');
        const passwordInputField = document.getElementById('password');

        emailInput.addEventListener('blur', () => {
            if (emailInput.value && !isValidEmail(emailInput.value)) {
                emailInput.classList.add('input-error');
            } else {
                emailInput.classList.remove('input-error');
            }
        });

        passwordInputField.addEventListener('blur', () => {
            if (passwordInputField.value && passwordInputField.value.length < 6) {
                passwordInputField.classList.add('input-error');
            } else {
                passwordInputField.classList.remove('input-error');
            }
        });

        function isValidEmail(email) {
            return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
        }

        window.addEventListener('DOMContentLoaded', () => {
            const saved = localStorage.getItem('userData');
            if (saved) {
                try {
                    const parsed = JSON.parse(saved);
                    document.getElementById('email').value = parsed.email || '';
                    document.getElementById('remember').checked = true;
                } catch (error) {
                    console.error('No se pudo cargar la sesión guardada', error);
                }
            }
        });
    </script>
</body>
</html>
