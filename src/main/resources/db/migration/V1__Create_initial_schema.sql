-- Crear usuario administrador por defecto
INSERT INTO empresas (id_empresa, nombre_empresa, ruc, email, telefono, direccion, estado, fecha_registro, created_at, updated_at, deleted_at)
VALUES (1, 'Empresa Demo Turística', '12345678901', 'admin@demoturistica.com', '999888777', 'Dirección de ejemplo', 1, NOW(), NOW(), NOW(), NULL);

INSERT INTO roles (id_rol, nombre_rol, descripcion, estado, created_at, updated_at, deleted_at)
VALUES (1, 'Superadministrador', 'Control total del sistema multiempresa', 1, NOW(), NOW(), NULL),
       (2, 'Administrador', 'Administrador de empresa específica', 1, NOW(), NOW(), NULL),
       (3, 'Empleado', 'Usuario con permisos limitados', 1, NOW(), NOW(), NULL);

INSERT INTO usuarios (id_usuario, id_empresa, id_rol, nombre, apellido, email, password_hash, dni, estado, created_at, updated_at, deleted_at)
VALUES (1, 1, 1, 'Admin', 'Sistema', 'admin@demoturistica.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '12345678', 1, NOW(), NOW(), NULL);
-- Password: password (BCrypt encoded)

INSERT INTO modulos (id_modulo, nombre_modulo, descripcion, estado, created_at, updated_at)
VALUES (1, 'seguridad', 'Módulo de gestión de seguridad y usuarios', 1, NOW(), NOW()),
       (2, 'reservas', 'Módulo de reservas y paquetes turísticos', 1, NOW(), NOW()),
       (3, 'proveedores', 'Módulo de gestión de proveedores', 1, NOW(), NOW()),
       (4, 'financiero', 'Módulo financiero y caja', 1, NOW(), NOW()),
       (5, 'reportes', 'Módulo de reportes', 1, NOW(), NOW()),
       (6, 'clientes', 'Módulo de gestión de clientes', 1, NOW(), NOW()),
       (7, 'inventario', 'Módulo de inventario de servicios', 1, NOW(), NOW()),
       (8, 'recursos_humanos', 'Módulo de recursos humanos', 1, NOW(), NOW()),
       (9, 'gastos', 'Módulo de gastos operativos', 1, NOW(), NOW());

INSERT INTO permisos (id_permiso, id_modulo, nombre_permiso, descripcion, estado, created_at, updated_at)
VALUES (1, 1, 'ver_usuarios', 'Ver lista de usuarios', 1, NOW(), NOW()),
       (2, 1, 'crear_usuario', 'Crear nuevos usuarios', 1, NOW(), NOW()),
       (3, 1, 'editar_usuario', 'Editar usuarios existentes', 1, NOW(), NOW()),
       (4, 1, 'eliminar_usuario', 'Eliminar usuarios', 1, NOW(), NOW()),
       (5, 1, 'gestionar_roles', 'Gestionar roles y permisos', 1, NOW(), NOW()),
       (6, 2, 'ver_reservas', 'Ver reservas', 1, NOW(), NOW()),
       (7, 2, 'crear_reserva', 'Crear nuevas reservas', 1, NOW(), NOW()),
       (8, 2, 'editar_reserva', 'Editar reservas', 1, NOW(), NOW()),
       (9, 2, 'cancelar_reserva', 'Cancelar reservas', 1, NOW(), NOW()),
       (10, 3, 'ver_proveedores', 'Ver proveedores', 1, NOW(), NOW()),
       (11, 3, 'crear_proveedor', 'Crear proveedores', 1, NOW(), NOW()),
       (12, 3, 'editar_proveedor', 'Editar proveedores', 1, NOW(), NOW()),
       (13, 4, 'abrir_caja', 'Abrir caja', 1, NOW(), NOW()),
       (14, 4, 'cerrar_caja', 'Cerrar caja', 1, NOW(), NOW()),
       (15, 4, 'registrar_cobro', 'Registrar cobros', 1, NOW(), NOW()),
       (16, 5, 'generar_reportes', 'Generar reportes del sistema', 1, NOW(), NOW()),
       (17, 6, 'ver_clientes', 'Ver clientes', 1, NOW(), NOW()),
       (18, 6, 'crear_cliente', 'Crear clientes', 1, NOW(), NOW()),
       (19, 6, 'editar_cliente', 'Editar clientes', 1, NOW(), NOW()),
       (20, 7, 'ver_servicios', 'Ver servicios turísticos', 1, NOW(), NOW()),
       (21, 7, 'crear_servicio', 'Crear servicios', 1, NOW(), NOW()),
       (22, 7, 'editar_servicio', 'Editar servicios', 1, NOW(), NOW()),
       (23, 8, 'ver_personal', 'Ver personal', 1, NOW(), NOW()),
       (24, 8, 'gestionar_personal', 'Gestionar personal', 1, NOW(), NOW()),
       (25, 9, 'ver_gastos', 'Ver gastos operativos', 1, NOW(), NOW()),
       (26, 9, 'registrar_gasto', 'Registrar gastos', 1, NOW(), NOW());

INSERT INTO roles_permisos (id_rol_permiso, id_rol, id_permiso, created_at)
VALUES (1, 1, 2, NOW()), (2, 1, 3, NOW()), (3, 1, 4, NOW()), (4, 1, 5, NOW()), (5, 1, 1, NOW()),
       (6, 1, 9, NOW()), (7, 1, 7, NOW()), (8, 1, 8, NOW()), (9, 1, 6, NOW()), (10, 1, 11, NOW()),
       (11, 1, 12, NOW()), (12, 1, 10, NOW()), (13, 1, 13, NOW()), (14, 1, 14, NOW()), (15, 1, 15, NOW()),
       (16, 1, 16, NOW()), (17, 1, 18, NOW()), (18, 1, 19, NOW()), (19, 1, 17, NOW()), (20, 1, 21, NOW()),
       (21, 1, 22, NOW()), (22, 1, 20, NOW()), (23, 1, 24, NOW()), (24, 1, 23, NOW()), (25, 1, 26, NOW()),
       (26, 1, 25, NOW()), (32, 2, 2, NOW()), (33, 2, 3, NOW()), (34, 2, 4, NOW()), (35, 2, 1, NOW()),
       (36, 2, 9, NOW()), (37, 2, 7, NOW()), (38, 2, 8, NOW()), (39, 2, 6, NOW()), (40, 2, 11, NOW()),
       (41, 2, 12, NOW()), (42, 2, 10, NOW()), (43, 2, 13, NOW()), (44, 2, 14, NOW()), (45, 2, 15, NOW()),
       (46, 2, 16, NOW()), (47, 2, 18, NOW()), (48, 2, 19, NOW()), (49, 2, 17, NOW()), (50, 2, 21, NOW()),
       (51, 2, 22, NOW()), (52, 2, 20, NOW()), (53, 2, 24, NOW()), (54, 2, 23, NOW()), (55, 2, 26, NOW()),
       (56, 2, 25, NOW()), (63, 3, 7, NOW()), (64, 3, 6, NOW()), (65, 3, 15, NOW()), (66, 3, 18, NOW()),
       (67, 3, 17, NOW()), (68, 3, 20, NOW()), (69, 3, 25, NOW());

INSERT INTO configuraciones_empresa (id_configuracion, id_empresa, clave, valor, tipo_dato, descripcion, created_at, updated_at)
VALUES (1, 1, 'moneda_principal', 'PEN', 'string', 'Moneda principal del sistema', NOW(), NOW()),
       (2, 1, 'timezone', 'America/Lima', 'string', 'Zona horaria del sistema', NOW(), NOW()),
       (3, 1, 'max_intentos_login', '5', 'number', 'Máximo número de intentos de login fallidos', NOW(), NOW()),
       (4, 1, 'tiempo_bloqueo_login', '900', 'number', 'Tiempo de bloqueo en segundos tras intentos fallidos', NOW(), NOW()),
       (5, 1, 'habilitar_2fa', 'false', 'boolean', 'Habilitar autenticación de dos factores', NOW(), NOW()),
       (6, 1, 'dias_vencimiento_reserva', '30', 'number', 'Días para considerar reserva vencida', NOW(), NOW()),
       (7, 1, 'email_notificaciones', 'admin@demoturistica.com', 'string', 'Email para notificaciones del sistema', NOW(), NOW()),
       (8, 1, 'porcentaje_impuestos', '18.00', 'number', 'Porcentaje de impuestos aplicable', NOW(), NOW()),
       (9, 1, 'habilitar_promociones', 'true', 'boolean', 'Habilitar sistema de promociones', NOW(), NOW()),
       (10, 1, 'max_archivos_adjuntos', '5', 'number', 'Máximo número de archivos adjuntos por registro', NOW(), NOW());

INSERT INTO categorias_servicios (id_categoria, id_empresa, nombre_categoria, descripcion, icono, color, orden, estado, created_at, updated_at)
VALUES (1, 1, 'Tours Culturales', 'Tours enfocados en historia y cultura', 'fas fa-landmark', '#28a745', 1, 1, NOW(), NOW()),
       (2, 1, 'Tours de Aventura', 'Tours de actividades extremas', 'fas fa-mountain', '#dc3545', 2, 1, NOW(), NOW()),
       (3, 1, 'Tours Gastronómicos', 'Experiencias culinarias', 'fas fa-utensils', '#ffc107', 3, 1, NOW(), NOW()),
       (4, 1, 'Transporte', 'Servicios de transporte', 'fas fa-bus', '#007bff', 4, 1, NOW(), NOW()),
       (5, 1, 'Alojamiento', 'Servicios de hospedaje', 'fas fa-hotel', '#6f42c1', 5, 1, NOW(), NOW()),
       (6, 1, 'Entradas/Atractivos', 'Acceso a atractivos turísticos', 'fas fa-ticket-alt', '#17a2b8', 6, 1, NOW(), NOW());

INSERT INTO tipos_gasto (id_tipo_gasto, id_empresa, nombre_tipo, descripcion, estado, created_at, updated_at)
VALUES (1, 1, 'Combustible', 'Gastos en combustible para vehículos', 1, NOW(), NOW()),
        (2, 1, 'Alimentación', 'Gastos en alimentación para tours', 1, NOW(), NOW()),
        (3, 1, 'Mantenimiento', 'Mantenimiento de vehículos y equipos', 1, NOW(), NOW()),
        (4, 1, 'Peajes', 'Pago de peajes en rutas', 1, NOW(), NOW()),
        (5, 1, 'Entradas', 'Pago de entradas a atractivos turísticos', 1, NOW(), NOW()),
        (6, 1, 'Equipamiento', 'Compra de equipamiento', 1, NOW(), NOW()),
        (7, 1, 'Uniformes', 'Compra y mantenimiento de uniformes', 1, NOW(), NOW()),
        (8, 1, 'Otros', 'Otros gastos operativos', 1, NOW(), NOW());

-- =====================================================
-- DATOS ADICIONALES PARA TESTING FASE 3
-- =====================================================

-- USUARIOS ADICIONALES CON DIFERENTES ROLES
INSERT INTO usuarios (id_usuario, id_empresa, id_sucursal, id_rol, nombre, apellido, email, password_hash, dni, telefono, estado, ultimo_login, intentos_fallidos, bloqueado_hasta, created_at, updated_at)
VALUES (2, 1, NULL, 1, 'Carlos', 'Rodriguez', 'superadmin@demoturistica.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '12345679', '999111222', 1, NULL, 0, NULL, NOW(), NOW()),
       (3, 1, NULL, 2, 'Ana', 'Gomez', 'admin@demoturistica.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '87654321', '999333444', 1, NULL, 0, NULL, NOW(), NOW()),
       (4, 1, NULL, 3, 'Luis', 'Martinez', 'empleado@demoturistica.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '11223344', '999555666', 1, NULL, 0, NULL, NOW(), NOW());

-- CLIENTES
INSERT INTO clientes (id_cliente, id_empresa, nombre, apellido, email, telefono, dni, fecha_nacimiento, nacionalidad, estado, created_at, updated_at)
VALUES (1, 1, 'Juan', 'Perez', 'juan.perez@email.com', '999777888', '44556677', '1990-05-15', 'Peruano', 1, NOW(), NOW()),
       (2, 1, 'Maria', 'Lopez', 'maria.lopez@email.com', '999888999', '55667788', '1985-08-20', 'Peruana', 1, NOW(), NOW()),
       (3, 1, 'Pedro', 'Garcia', 'pedro.garcia@email.com', '999000111', '66778899', '1992-12-10', 'Peruano', 1, NOW(), NOW());

-- SERVICIOS TURÍSTICOS
INSERT INTO servicios_turisticos (id_servicio, id_empresa, id_categoria, nombre_servicio, descripcion, ubicacion_destino, duracion, capacidad_maxima, precio_base, incluye, no_incluye, estado, created_at, updated_at)
VALUES (1, 1, 1, 'Tour Histórico de Lima', 'Recorrido completo por los lugares históricos más importantes de Lima', 'Lima Centro', '4 horas', 15, 45.00, 'Guía profesional, Transporte, Entradas a museos', 'Alimentación, Propinas', 1, NOW(), NOW()),
       (2, 1, 2, 'Canotaje en Cañón del Colca', 'Descenso en balsa por el cañón más profundo del mundo', 'Cañón del Colca', '6 horas', 8, 120.00, 'Equipo de seguridad, Guía certificado, Transporte ida y vuelta', 'Alimentación, Seguro personal', 1, NOW(), NOW()),
       (3, 1, 3, 'Experiencia Culinaria Peruana', 'Degustación de platos típicos peruanos con chef local', 'Miraflores, Lima', '3 horas', 12, 65.00, 'Degustación de 8 platos, Bebidas incluidas, Chef instructor', 'Transporte', 1, NOW(), NOW());

-- CALENDARIO DE DISPONIBILIDAD
INSERT INTO calendario_disponibilidad (id_calendario, id_servicio, fecha, cupos_disponibles, precio_especial, notas, bloqueado, created_at, updated_at)
VALUES (1, 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 15, NULL, 'Disponible mañana', 0, NOW(), NOW()),
       (2, 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 15, NULL, 'Disponible pasado mañana', 0, NOW(), NOW()),
       (3, 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 10, 40.00, 'Precio especial miércoles', 0, NOW(), NOW()),
       (4, 2, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 8, NULL, 'Disponible viernes', 0, NOW(), NOW()),
       (5, 2, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 6, 110.00, 'Últimas plazas disponibles', 0, NOW(), NOW()),
       (6, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), 12, NULL, 'Disponible mañana', 0, NOW(), NOW()),
       (7, 3, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 12, 60.00, 'Precio especial jueves', 0, NOW(), NOW());

-- PAQUETES TURÍSTICOS
INSERT INTO paquetes_turisticos (id_paquete, id_empresa, nombre_paquete, descripcion, precio_total, duracion_dias, promocion, descuento, estado, created_at, updated_at)
VALUES (1, 1, 'Paquete Lima Cultural', 'Tour histórico + Experiencia culinaria en Lima', 95.00, 1, 0, 0.00, 1, NOW(), NOW()),
       (2, 1, 'Aventura en Cañón', 'Canotaje + Tour cultural en Arequipa', 150.00, 2, 1, 15.00, 1, NOW(), NOW());

-- RELACIÓN PAQUETES - SERVICIOS
INSERT INTO paquetes_servicios (id_paquete_servicio, id_paquete, id_servicio, orden, created_at)
VALUES (1, 1, 1, 1, NOW()), -- Paquete Lima: Tour Histórico primero
       (2, 1, 3, 2, NOW()), -- Paquete Lima: Tour Gastronómico segundo
       (3, 2, 2, 1, NOW()), -- Paquete Aventura: Canotaje primero
       (4, 2, 1, 2, NOW()); -- Paquete Aventura: Tour Histórico segundo

-- ROL PERSONALIZADO
INSERT INTO roles (id_rol, nombre_rol, descripcion, estado, created_at, updated_at)
VALUES (4, 'Recepcionista', 'Rol limitado para recepción: solo crear reservas y ver clientes', 1, NOW(), NOW());

-- PERMISOS PARA ROL RECEPCIONISTA
INSERT INTO roles_permisos (id_rol, id_permiso, created_at)
SELECT 4, id_permiso, NOW() FROM permisos WHERE nombre_permiso IN (
    'ver_clientes',
    'crear_reserva',
    'ver_reservas'
);

-- USUARIO CON ROL PERSONALIZADO
INSERT INTO usuarios (id_usuario, id_empresa, id_sucursal, id_rol, nombre, apellido, email, password_hash, dni, telefono, estado, ultimo_login, intentos_fallidos, bloqueado_hasta, created_at, updated_at)
VALUES (5, 1, NULL, 4, 'Rosa', 'Flores', 'recepcionista@demoturistica.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '99887766', '999222333', 1, NULL, 0, NULL, NOW(), NOW());