<?php
// Inicializar variables
$clientes = [];
$error = null;
$success = null;

// Verificar si hay mensaje de éxito en la URL
if (isset($_GET['success']) && $_GET['success'] == '1') {
    $success = isset($_GET['mensaje']) ? $_GET['mensaje'] : 'Operación realizada correctamente';
}

// Verificar si hay mensaje de error en la URL
if (isset($_GET['error']) && $_GET['error'] == '1') {
    $error = isset($_GET['mensaje']) ? $_GET['mensaje'] : 'Ocurrió un error';
}

// Obtener token del usuario (desde sessionStorage vía JavaScript o desde parámetro)
$token = null;
if (isset($_GET['token'])) {
    $token = $_GET['token'];
} elseif (isset($_COOKIE['userToken'])) {
    $token = $_COOKIE['userToken'];
}

// Si no hay token, usar uno por defecto (para compatibilidad con acceso directo)
if (!$token) {
    $token = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ5dnZlbnpAZ21haWwuY29tIiwidXNlcklkIjo2LCJlbXByZXNhSWQiOjMsInJvbGVzIjpbIlJPTEVfQURNSU5JU1RSQURPUiJdLCJwZXJtaXNvcyI6W10sImlzcyI6InNpc3RlbWEtdHVyaXN0aWNvLWJhY2tlbmQiLCJpYXQiOjE3NjQ0MzE1MjQsImV4cCI6MTc2NDUxNzkyNH0.HCleOjbWQGtoK8AzXPeHChnXJ3QgEQRIqm4SKyGryE0';
}

// Realizar petición a la API
$curl = curl_init();

curl_setopt_array($curl, array(
  CURLOPT_URL => 'http://turistas.spring.informaticapp.com:2410/api/v1/clientes',
  CURLOPT_RETURNTRANSFER => true,
  CURLOPT_ENCODING => '',
  CURLOPT_MAXREDIRS => 10,
  CURLOPT_TIMEOUT => 0,
  CURLOPT_FOLLOWLOCATION => true,
  CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
  CURLOPT_CUSTOMREQUEST => 'GET',
  CURLOPT_HTTPHEADER => array(
    'Authorization: Bearer ' . $token
  ),
));

$response = curl_exec($curl);
$httpCode = curl_getinfo($curl, CURLINFO_HTTP_CODE);
$curlError = curl_error($curl);

curl_close($curl);

// Procesar respuesta
if ($curlError) {
    $error = "Error de conexión: " . $curlError;
} elseif ($httpCode !== 200) {
    // Obtener más detalles del error
    $errorData = json_decode($response, true);
    $errorMessage = "Error HTTP: " . $httpCode;
    
    if (isset($errorData['message'])) {
        $errorMessage .= " - " . $errorData['message'];
    } elseif (isset($errorData['error'])) {
        $errorMessage .= " - " . $errorData['error'];
    } elseif ($response) {
        // Si hay respuesta pero no es JSON válido, mostrar los primeros caracteres
        $errorMessage .= " - " . substr($response, 0, 200);
    }
    
    $error = $errorMessage;
    
    // Log para debugging (solo si se solicita vía AJAX)
    if (isset($_GET['ajax']) && $_GET['ajax'] == '1') {
        error_log("Error al obtener clientes - HTTP Code: $httpCode, Response: " . substr($response, 0, 500));
    }
} else {
    $data = json_decode($response, true);
    
    // Verificar si la respuesta es válida
    if (json_last_error() === JSON_ERROR_NONE) {
        // Extraer array de clientes según la estructura de la respuesta
        if (isset($data['data']) && is_array($data['data'])) {
            $clientes = $data['data'];
        } elseif (isset($data['content']) && is_array($data['content'])) {
            $clientes = $data['content'];
        } elseif (isset($data['clientes']) && is_array($data['clientes'])) {
            $clientes = $data['clientes'];
        } elseif (isset($data['items']) && is_array($data['items'])) {
            $clientes = $data['items'];
        } elseif (isset($data['results']) && is_array($data['results'])) {
            $clientes = $data['results'];
        } elseif (is_array($data)) {
            // Si es un array directo, verificar si tiene elementos numéricos o asociativos
            if (!empty($data) && isset($data[0])) {
                $clientes = $data;
            } else {
                // Si es un objeto único, convertirlo a array
                $clientes = [$data];
            }
        }
    } else {
        $error = "Error al decodificar JSON: " . json_last_error_msg();
    }
}

// Debug opcional: agregar ?debug=1 a la URL para ver la estructura de datos
if (isset($_GET['debug']) && $_GET['debug'] == '1') {
    echo "<pre style='background: #f5f5f5; padding: 20px; margin: 20px; border: 1px solid #ddd; border-radius: 8px;'>";
    echo "Estructura de datos recibidos:\n\n";
    echo "Total de clientes: " . count($clientes) . "\n\n";
    if (!empty($clientes)) {
        echo "Primer cliente:\n";
        print_r($clientes[0]);
    }
    echo "\n\nDatos completos:\n";
    print_r($data ?? []);
    echo "</pre>";
}

// Si se solicita vía AJAX, devolver solo el contenido
if (isset($_GET['ajax']) && $_GET['ajax'] == '1') {
    // Iniciar buffer de salida para capturar el contenido
    ob_start();
    ?>
    <div class="content-header">
        <div class="card">
            <div class="card-header">
                <h2 class="section-title">Gestión de Clientes</h2>
                <div class="header-actions">
                    <button type="button" class="btn btn-primary" onclick="if(typeof loadNuevoClienteContent === 'function') { loadNuevoClienteContent(); } else { window.location.href = 'CLIENTES/nuevo_cliente.php'; }">
                        <i class="fas fa-plus"></i>
                        Nuevo Cliente
                    </button>
                </div>
            </div>
            <div class="card-body">
                <?php if ($error && !empty($clientes)): ?>
                    <div style="padding: 15px; background: #f8d7da; color: #721c24; border-radius: 8px; margin-bottom: 20px; border: 1px solid #f5c6cb;">
                        <i class="fas fa-exclamation-triangle"></i> <?php echo htmlspecialchars($error); ?>
                    </div>
                <?php endif; ?>
                <div class="table-responsive">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Nombre</th>
                                <th>Email</th>
                                <th>Teléfono</th>
                                <th>Fecha Registro</th>
                                <th>Acciones</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if ($error): ?>
                                <tr>
                                    <td colspan="6" class="texto-vacio" style="color: #dc3545;">
                                        <i class="fas fa-exclamation-triangle"></i> <?php echo htmlspecialchars($error); ?>
                                    </td>
                                </tr>
                            <?php elseif (empty($clientes)): ?>
                                <tr>
                                    <td colspan="6" class="texto-vacio">No hay clientes registrados</td>
                                </tr>
                            <?php else: ?>
                                <?php foreach ($clientes as $cliente): ?>
                                    <tr>
                                        <td>
                                            <?php 
                                            // Buscar ID en diferentes campos posibles
                                            $id = null;
                                            if (isset($cliente['id'])) {
                                                $id = $cliente['id'];
                                            } elseif (isset($cliente['clienteId'])) {
                                                $id = $cliente['clienteId'];
                                            } elseif (isset($cliente['idCliente'])) {
                                                $id = $cliente['idCliente'];
                                            } elseif (isset($cliente['ID'])) {
                                                $id = $cliente['ID'];
                                            } elseif (isset($cliente['ClienteId'])) {
                                                $id = $cliente['ClienteId'];
                                            }
                                            echo $id !== null ? htmlspecialchars($id) : '-';
                                            ?>
                                        </td>
                                        <td>
                                            <?php 
                                            $nombre = '';
                                            if (isset($cliente['nombre'])) {
                                                $nombre = $cliente['nombre'];
                                            } elseif (isset($cliente['nombres'])) {
                                                $nombre = $cliente['nombres'];
                                                if (isset($cliente['apellidos'])) {
                                                    $nombre .= ' ' . $cliente['apellidos'];
                                                }
                                            } elseif (isset($cliente['nombreCompleto'])) {
                                                $nombre = $cliente['nombreCompleto'];
                                            }
                                            echo htmlspecialchars($nombre ?: 'Sin nombre');
                                            ?>
                                        </td>
                                        <td><?php echo isset($cliente['email']) ? htmlspecialchars($cliente['email']) : '-'; ?></td>
                                        <td><?php echo isset($cliente['telefono']) ? htmlspecialchars($cliente['telefono']) : (isset($cliente['celular']) ? htmlspecialchars($cliente['celular']) : '-'); ?></td>
                                        <td>
                                            <?php 
                                            if (isset($cliente['fechaRegistro'])) {
                                                echo date('d/m/Y', strtotime($cliente['fechaRegistro']));
                                            } elseif (isset($cliente['createdAt'])) {
                                                echo date('d/m/Y', strtotime($cliente['createdAt']));
                                            } elseif (isset($cliente['fechaCreacion'])) {
                                                echo date('d/m/Y', strtotime($cliente['fechaCreacion']));
                                            } else {
                                                echo '-';
                                            }
                                            ?>
                                        </td>
                                        <td>
                                            <div class="action-buttons">
                                                <?php 
                                                // Obtener ID para los botones de acción
                                                $clienteId = null;
                                                if (isset($cliente['id'])) {
                                                    $clienteId = $cliente['id'];
                                                } elseif (isset($cliente['clienteId'])) {
                                                    $clienteId = $cliente['clienteId'];
                                                } elseif (isset($cliente['idCliente'])) {
                                                    $clienteId = $cliente['idCliente'];
                                                } elseif (isset($cliente['ID'])) {
                                                    $clienteId = $cliente['ID'];
                                                } elseif (isset($cliente['ClienteId'])) {
                                                    $clienteId = $cliente['ClienteId'];
                                                }
                                                ?>
                                                <?php if ($clienteId !== null): ?>
                                                    <button class="btn-action btn-edit" title="Editar" onclick="if(typeof loadEditarClienteContent === 'function') { loadEditarClienteContent(<?php echo htmlspecialchars($clienteId); ?>); } else { window.location.href = 'CLIENTES/editar_cliente.php?id=<?php echo htmlspecialchars($clienteId); ?>'; }">
                                                        <i class="fas fa-edit"></i>
                                                    </button>
                                                    <button class="btn-action btn-delete" title="Eliminar" onclick="confirmarEliminar(<?php echo htmlspecialchars($clienteId); ?>)">
                                                        <i class="fas fa-trash"></i>
                                                    </button>
                                                <?php else: ?>
                                                    <span class="btn-action btn-edit" style="opacity: 0.5; cursor: not-allowed;" title="ID no disponible">
                                                        <i class="fas fa-edit"></i>
                                                    </span>
                                                    <span class="btn-action btn-delete" style="opacity: 0.5; cursor: not-allowed;" title="ID no disponible">
                                                        <i class="fas fa-trash"></i>
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
    <script>
        // Función para confirmar eliminación (necesaria cuando se carga vía AJAX)
        function confirmarEliminar(id) {
            if (confirm('¿Está seguro de que desea eliminar este cliente? Esta acción no se puede deshacer.')) {
                // Si estamos en admin.php, usar la función de eliminación AJAX
                if (typeof eliminarCliente === 'function') {
                    eliminarCliente(id);
                } else {
                    // Si no, redirigir normalmente
                    window.location.href = 'CLIENTES/eliminar_cliente.php?id=' + id;
                }
            }
        }
    </script>
    <?php
    $content = ob_get_clean();
    header('Content-Type: text/html; charset=utf-8');
    echo $content;
    exit;
}
?>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Clientes - Sistema de Gestión</title>
    
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;500;600;700&family=Playfair+Display:wght@400;700&display=swap" rel="stylesheet">
    
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    
    <!-- CSS -->
    <link rel="stylesheet" href="web.css">
    <link rel="stylesheet" href="alertas.css">
</head>
<body class="admin-body">
    <!-- Sidebar -->
    <aside class="admin-sidebar" id="sidebar">
        <div class="sidebar-header">
            <h2 class="sidebar-title">Sistema</h2>
        </div>
        <nav class="sidebar-nav">
            <a href="#" class="sidebar-link active">
                <i class="fas fa-users"></i>
                <span>Clientes</span>
            </a>
        </nav>
    </aside>

    <!-- Contenido Principal -->
    <main class="admin-main-content" id="mainContent">
        <!-- Header -->
        <header class="admin-header">
            <div class="header-left">
                <button class="sidebar-toggle" id="sidebarToggle">
                    <i class="fas fa-bars"></i>
                </button>
                <h1 class="page-title">Clientes</h1>
            </div>
            <div class="header-right">
                <div class="user-profile">
                    <div class="profile-info">
                        <img src="https://via.placeholder.com/45" alt="Usuario" class="profile-image">
                        <div class="user-info">
                            <span class="user-name">Administrador</span>
                            <span class="user-role">Admin</span>
                        </div>
                    </div>
                </div>
            </div>
        </header>

        <!-- Contenido -->
        <div class="admin-content">
            <div class="content-header">
                <div class="card">
                    <div class="card-header">
                        <h2 class="section-title">Gestión de Clientes</h2>
                        <div class="header-actions">
                            <a href="nuevo_cliente.php" class="btn btn-primary">
                                <i class="fas fa-plus"></i>
                                Nuevo Cliente
                            </a>
                        </div>
                    </div>
                    <div class="card-body">
                        <?php if ($error && !empty($clientes)): ?>
                            <div style="padding: 15px; background: #f8d7da; color: #721c24; border-radius: 8px; margin-bottom: 20px; border: 1px solid #f5c6cb;">
                                <i class="fas fa-exclamation-triangle"></i> <?php echo htmlspecialchars($error); ?>
                            </div>
                        <?php endif; ?>
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Nombre</th>
                                        <th>Email</th>
                                        <th>Teléfono</th>
                                        <th>Fecha Registro</th>
                                        <th>Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <?php if ($error): ?>
                                        <tr>
                                            <td colspan="6" class="texto-vacio" style="color: #dc3545;">
                                                <i class="fas fa-exclamation-triangle"></i> <?php echo htmlspecialchars($error); ?>
                                            </td>
                                        </tr>
                                    <?php elseif (empty($clientes)): ?>
                                        <tr>
                                            <td colspan="6" class="texto-vacio">No hay clientes registrados</td>
                                        </tr>
                                    <?php else: ?>
                                        <?php foreach ($clientes as $cliente): ?>
                                            <tr>
                                                <td>
                                                    <?php 
                                                    // Buscar ID en diferentes campos posibles
                                                    $id = null;
                                                    if (isset($cliente['id'])) {
                                                        $id = $cliente['id'];
                                                    } elseif (isset($cliente['clienteId'])) {
                                                        $id = $cliente['clienteId'];
                                                    } elseif (isset($cliente['idCliente'])) {
                                                        $id = $cliente['idCliente'];
                                                    } elseif (isset($cliente['ID'])) {
                                                        $id = $cliente['ID'];
                                                    } elseif (isset($cliente['ClienteId'])) {
                                                        $id = $cliente['ClienteId'];
                                                    }
                                                    echo $id !== null ? htmlspecialchars($id) : '-';
                                                    ?>
                                                </td>
                                                <td>
                                                    <?php 
                                                    $nombre = '';
                                                    if (isset($cliente['nombre'])) {
                                                        $nombre = $cliente['nombre'];
                                                    } elseif (isset($cliente['nombres'])) {
                                                        $nombre = $cliente['nombres'];
                                                        if (isset($cliente['apellidos'])) {
                                                            $nombre .= ' ' . $cliente['apellidos'];
                                                        }
                                                    } elseif (isset($cliente['nombreCompleto'])) {
                                                        $nombre = $cliente['nombreCompleto'];
                                                    }
                                                    echo htmlspecialchars($nombre ?: 'Sin nombre');
                                                    ?>
                                                </td>
                                                <td><?php echo isset($cliente['email']) ? htmlspecialchars($cliente['email']) : '-'; ?></td>
                                                <td><?php echo isset($cliente['telefono']) ? htmlspecialchars($cliente['telefono']) : (isset($cliente['celular']) ? htmlspecialchars($cliente['celular']) : '-'); ?></td>
                                                <td>
                                                    <?php 
                                                    if (isset($cliente['fechaRegistro'])) {
                                                        echo date('d/m/Y', strtotime($cliente['fechaRegistro']));
                                                    } elseif (isset($cliente['createdAt'])) {
                                                        echo date('d/m/Y', strtotime($cliente['createdAt']));
                                                    } elseif (isset($cliente['fechaCreacion'])) {
                                                        echo date('d/m/Y', strtotime($cliente['fechaCreacion']));
                                                    } else {
                                                        echo '-';
                                                    }
                                                    ?>
                                                </td>
                                                <td>
                                                    <div class="action-buttons">
                                                        <?php 
                                                        // Obtener ID para los botones de acción
                                                        $clienteId = null;
                                                        if (isset($cliente['id'])) {
                                                            $clienteId = $cliente['id'];
                                                        } elseif (isset($cliente['clienteId'])) {
                                                            $clienteId = $cliente['clienteId'];
                                                        } elseif (isset($cliente['idCliente'])) {
                                                            $clienteId = $cliente['idCliente'];
                                                        } elseif (isset($cliente['ID'])) {
                                                            $clienteId = $cliente['ID'];
                                                        } elseif (isset($cliente['ClienteId'])) {
                                                            $clienteId = $cliente['ClienteId'];
                                                        }
                                                        ?>
                                                        <?php if ($clienteId !== null): ?>
                                                            <a href="editar_cliente.php?id=<?php echo htmlspecialchars($clienteId); ?>" class="btn-action btn-edit" title="Editar">
                                                                <i class="fas fa-edit"></i>
                                                            </a>
                                                            <button class="btn-action btn-delete" title="Eliminar" onclick="confirmarEliminar(<?php echo htmlspecialchars($clienteId); ?>)">
                                                                <i class="fas fa-trash"></i>
                                                            </button>
                                                        <?php else: ?>
                                                            <span class="btn-action btn-edit" style="opacity: 0.5; cursor: not-allowed;" title="ID no disponible">
                                                                <i class="fas fa-edit"></i>
                                                            </span>
                                                            <span class="btn-action btn-delete" style="opacity: 0.5; cursor: not-allowed;" title="ID no disponible">
                                                                <i class="fas fa-trash"></i>
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
        </div>
    </main>

    <!-- Contenedor para alertas -->
    <div id="alertasContainer"></div>

    <!-- JavaScript -->
    <script>
        // Toggle sidebar en móviles
        const sidebarToggle = document.getElementById('sidebarToggle');
        const sidebar = document.getElementById('sidebar');
        const mainContent = document.getElementById('mainContent');

        if (sidebarToggle) {
            sidebarToggle.addEventListener('click', function() {
                sidebar.classList.toggle('sidebar-collapsed');
                mainContent.classList.toggle('content-expanded');
            });
        }

        // Cerrar sidebar al hacer clic fuera en móviles
        document.addEventListener('click', function(event) {
            if (window.innerWidth <= 1024) {
                if (!sidebar.contains(event.target) && !sidebarToggle.contains(event.target)) {
                    sidebar.classList.remove('sidebar-collapsed');
                    mainContent.classList.remove('content-expanded');
                }
            }
        });

        // Función para mostrar alertas
        function mostrarAlerta(tipo, mensaje, duracion = 5000) {
            const alertasContainer = document.getElementById('alertasContainer');
            
            if (!alertasContainer) {
                const container = document.createElement('div');
                container.id = 'alertasContainer';
                document.body.appendChild(container);
            }
            
            const alerta = document.createElement('div');
            alerta.className = `alerta alerta-${tipo}`;
            
            const iconos = {
                success: '<i class="fas fa-check-circle"></i>',
                error: '<i class="fas fa-exclamation-circle"></i>',
                warning: '<i class="fas fa-exclamation-triangle"></i>',
                info: '<i class="fas fa-info-circle"></i>'
            };
            
            alerta.innerHTML = `
                <div style="display: flex; align-items: center; gap: 10px;">
                    ${iconos[tipo] || ''}
                    <span>${mensaje}</span>
                </div>
                <button class="cerrar-alerta" onclick="this.parentElement.remove()">
                    <i class="fas fa-times"></i>
                </button>
            `;
            
            alertasContainer.appendChild(alerta);
            
            setTimeout(() => {
                alerta.style.opacity = '1';
                alerta.style.transform = 'translateY(0)';
            }, 10);
            
            setTimeout(() => {
                alerta.style.opacity = '0';
                alerta.style.transform = 'translateY(-100%)';
                setTimeout(() => {
                    if (alerta.parentNode) {
                        alerta.remove();
                    }
                }, 300);
            }, duracion);
        }

        // Exportar función globalmente
        window.mostrarAlerta = mostrarAlerta;
        
        // Mostrar alerta de éxito si existe
        <?php if ($success): ?>
            mostrarAlerta('success', '<?php echo addslashes($success); ?>');
        <?php endif; ?>
        
        // Función para confirmar eliminación
        function confirmarEliminar(id) {
            if (confirm('¿Está seguro de que desea eliminar este cliente? Esta acción no se puede deshacer.')) {
                // Si estamos en admin.php, usar la función de eliminación AJAX
                if (typeof eliminarCliente === 'function') {
                    eliminarCliente(id);
                } else {
                    // Si no, redirigir normalmente
                    window.location.href = 'eliminar_cliente.php?id=' + id;
                }
            }
        }
    </script>
</body>
</html>
