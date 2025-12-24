-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost:3306
-- Tiempo de generación: 23-12-2025 a las 01:54:05
-- Versión del servidor: 10.11.15-MariaDB
-- Versión de PHP: 8.4.16

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `turistas_sistema`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `archivos_adjuntos`
--

CREATE TABLE `archivos_adjuntos` (
  `id_archivo` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `tabla_origen` varchar(50) NOT NULL,
  `id_registro_origen` int(11) NOT NULL,
  `nombre_original` varchar(255) NOT NULL,
  `nombre_archivo` varchar(255) NOT NULL,
  `ruta_archivo` varchar(500) NOT NULL,
  `tipo_archivo` varchar(100) NOT NULL,
  `tamano_bytes` int(11) NOT NULL,
  `extension` varchar(10) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Eliminado',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `uploaded_by` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `asignaciones_personal`
--

CREATE TABLE `asignaciones_personal` (
  `id_asignacion` bigint(20) NOT NULL,
  `id_personal` int(11) NOT NULL,
  `id_reserva` int(11) NOT NULL,
  `id_sucursal` int(11) DEFAULT NULL,
  `rol_asignado` enum('Guía','Chofer','Staff') NOT NULL,
  `fecha_asignacion` date NOT NULL,
  `estado` enum('Asignado','Completado','Cancelado') DEFAULT 'Asignado',
  `observaciones` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `asignaciones_personal`
--

INSERT INTO `asignaciones_personal` (`id_asignacion`, `id_personal`, `id_reserva`, `id_sucursal`, `rol_asignado`, `fecha_asignacion`, `estado`, `observaciones`, `created_at`, `updated_at`) VALUES
(70, 22, 109, NULL, 'Guía', '2026-12-26', 'Completado', 'Guía principal actualizado', '2025-12-22 18:14:53', '2025-12-23 00:54:09'),
(75, 21, 111, NULL, 'Guía', '2025-12-30', 'Cancelado', NULL, '2025-12-23 01:17:37', '2025-12-23 01:18:08'),
(76, 21, 112, NULL, 'Guía', '2025-12-23', 'Asignado', NULL, '2025-12-23 01:19:26', '2025-12-23 01:19:26');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `auditoria_logs`
--

CREATE TABLE `auditoria_logs` (
  `id_log` int(11) NOT NULL,
  `id_usuario` int(11) DEFAULT NULL,
  `tabla_afectada` varchar(100) NOT NULL,
  `id_registro_afectado` int(11) DEFAULT NULL,
  `accion` enum('INSERT','UPDATE','DELETE','LOGIN','LOGOUT') NOT NULL,
  `datos_anteriores` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`datos_anteriores`)),
  `datos_nuevos` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`datos_nuevos`)),
  `ip_address` varchar(45) DEFAULT NULL,
  `user_agent` text DEFAULT NULL,
  `fecha_hora` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `bloqueo_ip`
--

CREATE TABLE `bloqueo_ip` (
  `id_bloqueo` int(11) NOT NULL,
  `ip_address` varchar(45) NOT NULL,
  `intentos_fallidos` int(11) DEFAULT 1,
  `primer_intento` timestamp NOT NULL DEFAULT current_timestamp(),
  `ultimo_intento` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `bloqueado_hasta` timestamp NULL DEFAULT NULL,
  `estado` enum('Activo','Expirado') DEFAULT 'Activo'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cajas`
--

CREATE TABLE `cajas` (
  `id_caja` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_sucursal` int(11) NOT NULL,
  `id_usuario_apertura` int(11) NOT NULL,
  `id_usuario_cierre` int(11) DEFAULT NULL,
  `fecha_apertura` date NOT NULL,
  `hora_apertura` time NOT NULL,
  `monto_inicial` decimal(10,2) NOT NULL DEFAULT 0.00,
  `saldo_actual` decimal(10,2) NOT NULL DEFAULT 0.00,
  `monto_cierre` decimal(10,2) DEFAULT NULL,
  `diferencia` decimal(10,2) DEFAULT NULL,
  `estado` enum('Abierta','Cerrada') DEFAULT 'Abierta',
  `observaciones` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `cajas`
--

INSERT INTO `cajas` (`id_caja`, `id_empresa`, `id_sucursal`, `id_usuario_apertura`, `id_usuario_cierre`, `fecha_apertura`, `hora_apertura`, `monto_inicial`, `saldo_actual`, `monto_cierre`, `diferencia`, `estado`, `observaciones`, `created_at`, `updated_at`) VALUES
(1, 12, 2, 26, NULL, '2025-12-21', '20:39:31', 150.00, 1400.00, NULL, NULL, 'Abierta', NULL, '2025-12-22 01:39:19', '2025-12-22 02:10:24');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `calendario_disponibilidad`
--

CREATE TABLE `calendario_disponibilidad` (
  `id_calendario` int(11) NOT NULL,
  `id_servicio` int(11) NOT NULL,
  `fecha` date NOT NULL,
  `cupos_disponibles` int(11) NOT NULL DEFAULT 0,
  `precio_especial` decimal(10,2) DEFAULT NULL,
  `notas` text DEFAULT NULL,
  `bloqueado` tinyint(1) DEFAULT 0 COMMENT '1=Bloqueado, 0=Disponible',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `capacitaciones`
--

CREATE TABLE `capacitaciones` (
  `id_capacitacion` int(11) NOT NULL,
  `id_personal` int(11) NOT NULL,
  `nombre_certificacion` varchar(255) NOT NULL,
  `institucion_emisora` varchar(255) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_vencimiento` date DEFAULT NULL,
  `documento` varchar(255) DEFAULT NULL,
  `estado` enum('Vigente','Vencida','Inactiva') DEFAULT 'Vigente',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categorias_servicios`
--

CREATE TABLE `categorias_servicios` (
  `id_categoria` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `nombre_categoria` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `icono` varchar(50) DEFAULT NULL,
  `color` varchar(7) DEFAULT '#007bff',
  `orden` int(11) DEFAULT 0,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `categorias_servicios`
--

INSERT INTO `categorias_servicios` (`id_categoria`, `id_empresa`, `nombre_categoria`, `descripcion`, `icono`, `color`, `orden`, `estado`, `created_at`, `updated_at`) VALUES
(1, 1, 'Tours Culturales', 'Tours enfocados en historia y cultura', 'fas fa-landmark', '#28a745', 1, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00'),
(2, 1, 'Tours de Aventura', 'Tours de actividades extremas', 'fas fa-mountain', '#dc3545', 2, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00'),
(3, 1, 'Tours Gastronómicos', 'Experiencias culinarias', 'fas fa-utensils', '#ffc107', 3, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00'),
(4, 1, 'Transporte', 'Servicios de transporte', 'fas fa-bus', '#007bff', 4, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00'),
(5, 1, 'Alojamiento', 'Servicios de hospedaje', 'fas fa-hotel', '#6f42c1', 5, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00'),
(6, 1, 'Entradas', 'Acceso a atractivos turísticos', 'fas fa-ticket-alt', '#17a2b8', 6, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `clientes`
--

CREATE TABLE `clientes` (
  `id_cliente` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_sucursal` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `email_encriptado` varbinary(255) DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `telefono_encriptado` varbinary(255) DEFAULT NULL,
  `dni` varchar(20) DEFAULT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `nacionalidad` varchar(100) DEFAULT NULL,
  `preferencias_viaje` text DEFAULT NULL,
  `puntos_fidelizacion` int(11) DEFAULT 0,
  `nivel_membresia` enum('Bronce','Plata','Oro','Platino') DEFAULT 'Bronce',
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `clientes`
--

INSERT INTO `clientes` (`id_cliente`, `id_empresa`, `id_sucursal`, `nombre`, `apellido`, `email`, `email_encriptado`, `telefono`, `telefono_encriptado`, `dni`, `fecha_nacimiento`, `nacionalidad`, `preferencias_viaje`, `puntos_fidelizacion`, `nivel_membresia`, `estado`, `created_at`, `updated_at`, `deleted_at`) VALUES
(9, 6, 2, 'stalin mark', 'lopez davila ', 'stalo@gmail.com', 0x7374616c6f40676d61696c2e636f6d, '987564132', 0x393837353634313332, '75428193', '2002-07-18', 'Peruano', 'gastronomía', 0, 'Bronce', 1, '2025-11-28 15:41:26', '2025-12-19 00:50:40', NULL),
(18, 6, 2, 'Lucia', 'Campos', 'lucia.campos+demo3@bya.com', NULL, '999123456', NULL, '88997778', '1992-04-10', 'Peruana', 'City tours y relax', 0, 'Bronce', 1, '2025-12-01 03:12:40', '2025-12-19 00:50:43', NULL),
(20, 6, 2, 'AlexanderRr', 'Pezo', 'alexanderp@gmail.com', 0x616c6578616e6465727040676d61696c2e636f6d, '988981272', 0x393838393831323732, '72235101', '2004-08-14', 'Peruano', 'Aventura', 0, 'Bronce', 1, '2025-12-01 19:53:09', '2025-12-19 00:50:46', NULL),
(82, 12, 2, 'MAX DELINGER', 'CRISTOBAL VASQUEZ', 'md@gmail.com', 0x6d6440676d61696c2e636f6d, '987654324', 0x393837363534333234, '71773945', '2007-06-13', 'Peruano', 'aventura', 0, 'Bronce', 1, '2025-12-16 14:05:16', '2025-12-19 00:50:49', NULL),
(84, 6, 2, 'DANNY ALEXANDER', 'PEZO INGA', 'dp@gmail.com', 0x647040676d61696c2e636f6d, '996588244', 0x393936353838323434, '73325101', '2004-07-21', 'Peruano', '.', 0, 'Bronce', 1, '2025-12-17 15:33:49', '2025-12-19 00:50:51', NULL),
(86, 14, 8, 'BRYAN JAMPIER', 'ABARCA IRIGOIN', 'ba@gmail.com', 0x626140676d61696c2e636f6d, '980555222', 0x393830353535323232, '90484997', '2006-06-14', 'Peruano', '.', 0, 'Bronce', 1, '2025-12-19 03:19:50', '2025-12-19 03:19:50', NULL),
(87, 14, 10, 'DIEGO ALONSO', 'ZAPATA ANDERSON', 'dz@gamil.com', 0x647a4067616d696c2e636f6d, '999888777', 0x393939383838373737, '74859612', '2004-07-14', 'Peruano', '.', 0, 'Bronce', 1, '2025-12-19 03:38:52', '2025-12-19 03:38:52', NULL),
(88, 14, 11, 'DANNY ALEXANDER', 'PEZO INGA', 'dp1@gmail.com', 0x64703140676d61696c2e636f6d, '999588244', 0x393939353838323434, '73325101', '2016-07-11', 'Peruano', '.', 0, 'Bronce', 1, '2025-12-22 00:44:07', '2025-12-22 00:44:07', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `configuraciones_empresa`
--

CREATE TABLE `configuraciones_empresa` (
  `id_configuracion` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `clave` varchar(100) NOT NULL,
  `valor` text DEFAULT NULL,
  `tipo_dato` enum('string','number','boolean','json') DEFAULT 'string',
  `descripcion` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `configuraciones_empresa`
--

INSERT INTO `configuraciones_empresa` (`id_configuracion`, `id_empresa`, `clave`, `valor`, `tipo_dato`, `descripcion`, `created_at`, `updated_at`) VALUES
(1, 1, 'moneda_principal', 'PEN', 'string', 'Moneda principal del sistema', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(2, 1, 'timezone', 'America/Lima', 'string', 'Zona horaria del sistema', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(3, 1, 'max_intentos_login', '5', 'number', 'Máximo número de intentos de login fallidos', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(4, 1, 'tiempo_bloqueo_login', '900', 'number', 'Tiempo de bloqueo en segundos tras intentos fallidos', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(5, 1, 'habilitar_2fa', 'false', 'boolean', 'Habilitar autenticación de dos factores', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(6, 1, 'dias_vencimiento_reserva', '30', 'number', 'Días para considerar reserva vencida', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(7, 1, 'email_notificaciones', 'admin@demoturistica.com', 'string', 'Email para notificaciones del sistema', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(8, 1, 'porcentaje_impuestos', '18.00', 'number', 'Porcentaje de impuestos aplicable', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(9, 1, 'habilitar_promociones', 'true', 'boolean', 'Habilitar sistema de promociones', '2025-11-01 22:21:59', '2025-11-01 22:21:59'),
(10, 1, 'max_archivos_adjuntos', '5', 'number', 'Máximo número de archivos adjuntos por registro', '2025-11-01 22:21:59', '2025-11-01 22:21:59');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `contratos_proveedores`
--

CREATE TABLE `contratos_proveedores` (
  `id_contrato` int(11) NOT NULL,
  `id_proveedor` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `tipo_contrato` varchar(100) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_vencimiento` date NOT NULL,
  `condiciones_especiales` text DEFAULT NULL,
  `servicios_incluidos` text DEFAULT NULL,
  `terminos_pago` text DEFAULT NULL,
  `documento_pdf` varchar(255) DEFAULT NULL,
  `estado` enum('Vigente','Vencido','Inactivo') DEFAULT 'Vigente',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `deudas_proveedores`
--

CREATE TABLE `deudas_proveedores` (
  `id_deuda` int(11) NOT NULL,
  `id_proveedor` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `numero_compra` varchar(100) DEFAULT NULL,
  `fecha_deuda` date NOT NULL,
  `fecha_limite_pago` date DEFAULT NULL,
  `monto_total` decimal(10,2) NOT NULL,
  `monto_pagado` decimal(10,2) DEFAULT 0.00,
  `saldo_pendiente` decimal(10,2) NOT NULL,
  `estado` enum('Pendiente','Pagada','Vencida') DEFAULT 'Pendiente',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `empresas`
--

CREATE TABLE `empresas` (
  `id_empresa` int(11) NOT NULL,
  `nombre_empresa` varchar(255) NOT NULL,
  `ruc` varchar(20) NOT NULL,
  `email` varchar(255) NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `direccion` text DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `fecha_registro` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `empresas`
--

INSERT INTO `empresas` (`id_empresa`, `nombre_empresa`, `ruc`, `email`, `telefono`, `direccion`, `estado`, `fecha_registro`, `created_at`, `updated_at`, `deleted_at`) VALUES
(0, 'SUPERADMIN', '15935785264', 'e@gmail.com', '958847155', 'jr. san carlos', 1, '2025-11-29 17:20:38', '2025-11-29 17:20:38', '2025-11-29 17:20:56', NULL),
(1, 'Empresa Demo Turística', '12345678901', 'admin@demoturistica.com', '999888777', 'Dirección de ejemplo', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47', '2025-12-03 02:55:45', NULL),
(2, 'Nombre Actualizado S.A.', '12345578901', 'contacto@empresa.com', '098765432', 'Calle Actualizada 456', 1, '2025-11-28 20:35:22', '2025-11-28 20:35:22', '2025-11-28 20:41:05', NULL),
(3, 'Alexander s.a', '12345678902', 'pezoinga@gmail.com', '999888777', 'jr. loreto 450', 1, '2025-11-28 20:38:49', '2025-11-28 20:38:49', '2025-11-29 03:38:09', NULL),
(4, 'Mark', '75315963258', 'm@gmail.com', '951267848', 'jr. prado 400', 1, '2025-11-28 20:53:08', '2025-11-28 20:53:08', '2025-11-29 17:39:29', NULL),
(5, 'Mark trikilin', '75321598426', 'm2@gmail.com', '951284763', 'jr.leon 200', 1, '2025-11-28 20:55:02', '2025-11-28 20:55:02', '2025-11-29 17:39:33', NULL),
(6, 'BRYAN Y ASOCIADOS S.A', '75315965258', 'bry2@gmail.com', '974941209', 'JR. LIBERTAD 500', 1, '2025-11-29 16:59:43', '2025-11-29 16:59:43', '2025-12-03 02:53:51', NULL),
(8, 'MARK S.A', '12345678903', 'ma@gmail.com', '944697707', 'jr. cusco 300', 1, '2025-12-03 13:40:29', '2025-12-03 13:40:29', '2025-12-03 13:40:29', NULL),
(9, 'ALEXANDER S.A.C', '75315965255', 'xan@gmail.com', '988981727', 'jr. pardo 400', 1, '2025-12-03 13:49:45', '2025-12-03 13:49:45', '2025-12-03 13:49:45', NULL),
(10, 'STALIN S.A', '75315965259', 'sta@gmail.com', '900777901', 'jr. prado 448', 1, '2025-12-03 13:52:18', '2025-12-03 13:52:18', '2025-12-03 13:52:18', NULL),
(11, 'MAYKEL S.A', '12345578902', 'may@gmail.com', '926445981', 'jr. loreto 496', 1, '2025-12-03 13:55:06', '2025-12-03 13:55:06', '2025-12-03 13:55:06', NULL),
(12, 'MAX S.A', '75315765258', 'max@gmail.com', '932167978', 'jr.leon 250', 1, '2025-12-03 13:58:42', '2025-12-03 13:58:42', '2025-12-03 13:58:42', NULL),
(13, 'JORGE S.A', '75315943258', 'jor@gmail.com', '902546875', 'jr.leon 202', 1, '2025-12-03 14:01:02', '2025-12-03 14:01:02', '2025-12-03 14:01:02', NULL),
(14, 'BRYAN S.A', '75315955251', 'iri@gmail.com', '974941209', 'av. circunvalación 321', 1, '2025-12-03 14:38:44', '2025-12-03 14:38:44', '2025-12-03 14:38:44', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `evaluaciones_servicios`
--

CREATE TABLE `evaluaciones_servicios` (
  `id_evaluacion` bigint(20) NOT NULL,
  `id_reserva` int(11) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_servicio` int(11) DEFAULT NULL,
  `id_paquete` int(11) DEFAULT NULL,
  `calificacion_general` int(11) DEFAULT NULL,
  `calificacion_guia` int(11) DEFAULT NULL,
  `calificacion_transporte` int(11) DEFAULT NULL,
  `calificacion_hotel` int(11) DEFAULT NULL,
  `comentario_general` text DEFAULT NULL,
  `comentario_guia` text DEFAULT NULL,
  `comentario_transporte` text DEFAULT NULL,
  `comentario_hotel` text DEFAULT NULL,
  `recomendaciones` text DEFAULT NULL,
  `fecha_evaluacion` timestamp NOT NULL DEFAULT current_timestamp(),
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Visible, 0=Oculto'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `flyway_schema_history`
--

CREATE TABLE `flyway_schema_history` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT current_timestamp(),
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `flyway_schema_history`
--

INSERT INTO `flyway_schema_history` (`installed_rank`, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`) VALUES
(1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'root', '2025-11-02 23:53:43', 0, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `gastos_operativos`
--

CREATE TABLE `gastos_operativos` (
  `id_gasto` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_tipo_gasto` int(11) NOT NULL,
  `id_proveedor` int(11) DEFAULT NULL,
  `id_personal` int(11) DEFAULT NULL,
  `id_reserva` int(11) DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `monto` decimal(10,2) NOT NULL,
  `metodo_pago` enum('Efectivo','Transferencia','Tarjeta') NOT NULL,
  `comprobante` varchar(100) DEFAULT NULL,
  `fecha_gasto` date NOT NULL,
  `vehiculo` varchar(100) DEFAULT NULL,
  `observaciones` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Anulado',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_contrasenas`
--

CREATE TABLE `historial_contrasenas` (
  `id_historial` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `fecha_cambio` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `integraciones_calendario`
--

CREATE TABLE `integraciones_calendario` (
  `id_integracion` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `proveedor_calendario` enum('Google','Outlook','Apple') NOT NULL,
  `access_token` text DEFAULT NULL,
  `refresh_token` text DEFAULT NULL,
  `token_expiracion` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `calendario_id` varchar(255) DEFAULT NULL,
  `sincronizacion_activa` tinyint(1) DEFAULT 1,
  `ultima_sincronizacion` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `log_emails`
--

CREATE TABLE `log_emails` (
  `id_log_email` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_usuario_destino` int(11) DEFAULT NULL,
  `email_destino` varchar(255) NOT NULL,
  `tipo_email` varchar(50) NOT NULL,
  `id_plantilla` int(11) DEFAULT NULL,
  `asunto` varchar(255) NOT NULL,
  `estado_envio` enum('Enviado','Fallido','Pendiente') DEFAULT 'Pendiente',
  `fecha_envio` timestamp NULL DEFAULT NULL,
  `fecha_intento` timestamp NOT NULL DEFAULT current_timestamp(),
  `error_mensaje` text DEFAULT NULL,
  `id_referencia` int(11) DEFAULT NULL COMMENT 'ID del registro relacionado (reserva, etc.)'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `modulos`
--

CREATE TABLE `modulos` (
  `id_modulo` int(11) NOT NULL,
  `nombre_modulo` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `modulos`
--

INSERT INTO `modulos` (`id_modulo`, `nombre_modulo`, `descripcion`, `estado`, `created_at`, `updated_at`) VALUES
(1, 'seguridad', 'Módulo de gestión de seguridad y usuarios', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(2, 'reservas', 'Módulo de reservas y paquetes turísticos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(3, 'proveedores', 'Módulo de gestión de proveedores', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(4, 'financiero', 'Módulo financiero y caja', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(5, 'reportes', 'Módulo de reportes', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(6, 'clientes', 'Módulo de gestión de clientes', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(7, 'inventario', 'Módulo de inventario de servicios', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(8, 'recursos_humanos', 'Módulo de recursos humanos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(9, 'gastos', 'Módulo de gastos operativos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `movimientos_caja`
--

CREATE TABLE `movimientos_caja` (
  `id_movimiento` bigint(20) NOT NULL,
  `id_caja` int(11) NOT NULL,
  `id_venta` bigint(20) DEFAULT NULL,
  `tipo_movimiento` enum('Ingreso','Egreso') NOT NULL,
  `monto` decimal(10,2) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `fecha_hora` timestamp NOT NULL DEFAULT current_timestamp(),
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `movimientos_caja`
--

INSERT INTO `movimientos_caja` (`id_movimiento`, `id_caja`, `id_venta`, `tipo_movimiento`, `monto`, `descripcion`, `fecha_hora`, `created_at`) VALUES
(54, 1, NULL, 'Ingreso', 1050.00, 'Venta Reserva #92 - max', '2025-12-22 02:08:47', '2025-12-22 02:08:47'),
(55, 1, NULL, 'Ingreso', 350.00, 'Venta Reserva #93 - max', '2025-12-22 02:10:24', '2025-12-22 02:10:24');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `notificaciones`
--

CREATE TABLE `notificaciones` (
  `id_notificacion` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_usuario_destino` int(11) DEFAULT NULL,
  `tipo_notificacion` enum('Sistema','Reserva','Pago','Alerta','Recordatorio') NOT NULL,
  `titulo` varchar(255) NOT NULL,
  `mensaje` text NOT NULL,
  `datos_adicionales` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`datos_adicionales`)),
  `leida` tinyint(1) DEFAULT 0,
  `fecha_envio` timestamp NOT NULL DEFAULT current_timestamp(),
  `fecha_lectura` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pagos_deudas`
--

CREATE TABLE `pagos_deudas` (
  `id_pago_deuda` int(11) NOT NULL,
  `id_deuda` int(11) NOT NULL,
  `monto_abonado` decimal(10,2) NOT NULL,
  `fecha_pago` date NOT NULL,
  `metodo_pago` enum('Efectivo','Transferencia','Tarjeta') NOT NULL,
  `observaciones` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pagos_reservas`
--

CREATE TABLE `pagos_reservas` (
  `id_pago` bigint(20) NOT NULL,
  `id_reserva` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL COMMENT 'Usuario que registró el pago',
  `monto_pagado` decimal(10,2) NOT NULL,
  `metodo_pago` varchar(50) NOT NULL,
  `numero_operacion` varchar(100) DEFAULT NULL,
  `comprobante` varchar(100) DEFAULT NULL,
  `fecha_pago` date NOT NULL,
  `observaciones` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Anulado',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `paquetes_servicios`
--

CREATE TABLE `paquetes_servicios` (
  `id_paquete_servicio` bigint(20) NOT NULL,
  `id_paquete` int(11) NOT NULL,
  `id_servicio` int(11) NOT NULL,
  `orden` int(11) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `paquetes_servicios`
--

INSERT INTO `paquetes_servicios` (`id_paquete_servicio`, `id_paquete`, `id_servicio`, `orden`, `created_at`) VALUES
(1, 2, 3, 1, '2025-12-01 03:08:53');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `paquetes_turisticos`
--

CREATE TABLE `paquetes_turisticos` (
  `id_paquete` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_sucursal` int(11) DEFAULT NULL,
  `nombre_paquete` varchar(255) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `precio_total` decimal(10,2) NOT NULL,
  `duracion_dias` int(11) DEFAULT NULL,
  `promocion` tinyint(1) DEFAULT 0,
  `descuento` decimal(5,2) DEFAULT 0.00,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `paquetes_turisticos`
--

INSERT INTO `paquetes_turisticos` (`id_paquete`, `id_empresa`, `id_sucursal`, `nombre_paquete`, `descripcion`, `precio_total`, `duracion_dias`, `promocion`, `descuento`, `estado`, `created_at`, `updated_at`, `deleted_at`) VALUES
(1, 1, NULL, 'Aventura en Paracas y Huacachina', 'hellow.', 500.00, 2, 0, 0.00, 0, '2025-11-15 01:27:40', '2025-11-15 01:30:55', '2025-11-15 01:30:55'),
(2, 6, NULL, 'Escápate a la Selva', 'City tour + hotel boutique 3 noches', 820.00, 4, 1, 5.00, 1, '2025-12-01 03:08:53', '2025-12-01 03:08:53', NULL),
(12, 13, NULL, 'Aventura en Paracas y Huacachina', 'Disfruta de un recorrido lleno de adrenalina por las dunas de\nHuacachina y el encanto natural de Paracas. Incluye paseo en buggy,\nsandboarding y visita a las Islas Ballestas.', 1000.00, 2, 1, 10.00, 0, '2025-12-03 16:40:31', '2025-12-03 16:47:07', '2025-12-03 16:47:07'),
(13, 13, NULL, 'Aventura en Paracas y Huacachina', 'disfruta', 100.00, 10, 1, 20.00, 1, '2025-12-17 14:52:28', '2025-12-17 14:52:28', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `permisos`
--

CREATE TABLE `permisos` (
  `id_permiso` int(11) NOT NULL,
  `id_modulo` int(11) NOT NULL,
  `nombre_permiso` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `permisos`
--

INSERT INTO `permisos` (`id_permiso`, `id_modulo`, `nombre_permiso`, `descripcion`, `estado`, `created_at`, `updated_at`) VALUES
(1, 1, 'ver_usuarios', 'Ver lista de usuarios', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(2, 1, 'crear_usuario', 'Crear nuevos usuarios', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(3, 1, 'editar_usuario', 'Editar usuarios existentes', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(4, 1, 'eliminar_usuario', 'Eliminar usuarios', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(5, 1, 'gestionar_roles', 'Gestionar roles y permisos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(6, 2, 'ver_reservas', 'Ver reservas', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(7, 2, 'crear_reserva', 'Crear nuevas reservas', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(8, 2, 'editar_reserva', 'Editar reservas', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(9, 2, 'cancelar_reserva', 'Cancelar reservas', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(10, 3, 'ver_proveedores', 'Ver proveedores', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(11, 3, 'crear_proveedor', 'Crear proveedores', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(12, 3, 'editar_proveedor', 'Editar proveedores', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(13, 4, 'abrir_caja', 'Abrir caja', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(14, 4, 'cerrar_caja', 'Cerrar caja', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(15, 4, 'registrar_cobro', 'Registrar cobros', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(16, 5, 'generar_reportes', 'Generar reportes del sistema', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(17, 6, 'ver_clientes', 'Ver clientes', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(18, 6, 'crear_cliente', 'Crear clientes', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(19, 6, 'editar_cliente', 'Editar clientes', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(20, 7, 'ver_servicios', 'Ver servicios turísticos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(21, 7, 'crear_servicio', 'Crear servicios', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(22, 7, 'editar_servicio', 'Editar servicios', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(23, 8, 'ver_personal', 'Ver personal', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(24, 8, 'gestionar_personal', 'Gestionar personal', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(25, 9, 'ver_gastos', 'Ver gastos operativos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(26, 9, 'registrar_gasto', 'Registrar gastos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `personal`
--

CREATE TABLE `personal` (
  `id_personal` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_sucursal` int(11) DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `dni` varchar(20) NOT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `direccion` text DEFAULT NULL,
  `cargo` enum('Guía','Chofer','Staff') NOT NULL,
  `fecha_ingreso` date NOT NULL,
  `turno` enum('Mañana','Tarde','Noche','Completo','Rotativo') DEFAULT 'Completo',
  `sueldo` decimal(10,2) DEFAULT NULL,
  `foto` varchar(255) DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `personal`
--

INSERT INTO `personal` (`id_personal`, `id_empresa`, `id_sucursal`, `nombre`, `apellido`, `dni`, `fecha_nacimiento`, `telefono`, `email`, `direccion`, `cargo`, `fecha_ingreso`, `turno`, `sueldo`, `foto`, `estado`, `created_at`, `updated_at`, `deleted_at`) VALUES
(1, 1, NULL, 'Carlos Alberto', 'Mendoza Ruiz', '75146930', NULL, NULL, NULL, NULL, 'Chofer', '2025-01-15', 'Completo', NULL, NULL, 1, '2025-11-13 04:18:55', '2025-11-14 22:09:24', NULL),
(2, 6, NULL, 'Alexander', 'Pezo', '111111', NULL, '944696111', 'kaeld@gmail.com', '', 'Staff', '2025-01-15', 'Completo', NULL, NULL, 1, '2025-11-13 04:32:21', '2025-12-02 22:41:00', NULL),
(3, 6, NULL, 'dingui', 'cachique pezo', '71881419', NULL, '944696707', 'm123123_0604@hotmail.com', NULL, 'Chofer', '2025-12-17', 'Completo', NULL, NULL, 0, '2025-12-02 22:33:12', '2025-12-02 22:36:58', '2025-12-02 22:36:58'),
(4, 6, NULL, 'Marco', 'cachique pezo', '718814191', NULL, '944696799', 'm123_0604@hotmail.com', NULL, 'Chofer', '2025-12-01', 'Completo', NULL, NULL, 0, '2025-12-02 22:37:31', '2025-12-02 22:38:09', '2025-12-02 22:38:09'),
(13, 6, NULL, 'Maria', 'Pezo', '111111111', NULL, '945678901', 'ana@verdurasfrescas.com', NULL, 'Chofer', '2025-10-15', 'Completo', NULL, NULL, 0, '2025-12-02 22:41:35', '2025-12-02 22:41:48', '2025-12-02 22:41:48'),
(14, 6, NULL, 'sussase', 'Pezo', '1111123', NULL, '944611115', 'ana@verdurasfrescas.com', '', 'Chofer', '2025-10-05', 'Completo', NULL, NULL, 0, '2025-12-02 23:07:05', '2025-12-02 23:07:24', '2025-12-02 23:07:24'),
(15, 6, NULL, 'dinguis', 'Pezos', '71881333', NULL, '966696444', 'magasa1234@hotmail.com', '', 'Staff', '2025-09-12', 'Completo', NULL, NULL, 1, '2025-12-02 23:17:34', '2025-12-23 01:27:36', NULL),
(16, 6, NULL, 'dinguisalasd', 'cachique pezo', '11111145', NULL, '944696999', 'aasdasdna@gmail.com', '', 'Chofer', '2025-09-17', 'Completo', NULL, NULL, 0, '2025-12-02 23:21:28', '2025-12-02 23:23:04', '2025-12-02 23:23:04'),
(17, 3, NULL, 'Marcos', 'Pezo', '71881420', NULL, '944233090', 'marcolin@gmail.com', 'Jr. San Martin', 'Staff', '2025-09-25', 'Completo', NULL, NULL, 1, '2025-12-03 14:42:54', '2025-12-03 17:39:57', NULL),
(19, 8, NULL, 'Dedo', 'cachique pezo', '71881555', NULL, '966696707', 'm123123_0604@hotmail.com', NULL, 'Staff', '2025-10-17', 'Completo', NULL, NULL, 0, '2025-12-03 14:55:50', '2025-12-03 14:55:59', '2025-12-03 14:55:59'),
(20, 12, NULL, 'Felipe', 'Quispe', '71773947', '2004-11-11', '932145867', 'felipesex@gmail.com', 'Jr. Montañas 145', 'Guía', '2025-12-14', 'Completo', 1200.00, NULL, 1, '2025-12-15 02:19:07', '2025-12-15 02:19:07', NULL),
(21, 14, 11, 'Dedo', 'cachique pezo', '71881322', NULL, '944696790', 'kdad@gmail.com', NULL, 'Guía', '2025-10-24', 'Completo', NULL, NULL, 1, '2025-12-22 01:08:55', '2025-12-22 23:33:16', NULL),
(22, 14, 10, 'Marcos', 'Pezos', '71881429', NULL, '955696709', 'marcos@gmail.com', NULL, 'Chofer', '2025-09-26', 'Completo', NULL, NULL, 1, '2025-12-22 01:09:58', '2025-12-22 06:34:53', NULL),
(23, 8, NULL, 'Marcolin', 'cachique pezo', '11111146', NULL, '944696710', 'marcolin@gmail.com', '', 'Chofer', '2025-11-28', 'Completo', NULL, NULL, 1, '2025-12-23 00:02:43', '2025-12-23 01:30:13', NULL),
(24, 14, NULL, 'pancho', 'pezo', '11111121', NULL, '955696699', 'pnacho@gmail.com', NULL, 'Staff', '2025-11-17', 'Completo', NULL, NULL, 1, '2025-12-23 00:05:03', '2025-12-23 00:05:03', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `plantillas_email`
--

CREATE TABLE `plantillas_email` (
  `id_plantilla` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `nombre_plantilla` varchar(100) NOT NULL,
  `tipo_plantilla` enum('Bienvenida','Confirmacion','Recordatorio','Cancelacion','Promocion','Evaluacion') NOT NULL,
  `asunto` varchar(255) NOT NULL,
  `contenido_html` text NOT NULL,
  `contenido_texto` text DEFAULT NULL,
  `variables_disponibles` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`variables_disponibles`)),
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activa, 0=Inactiva',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `plantillas_email`
--

INSERT INTO `plantillas_email` (`id_plantilla`, `id_empresa`, `nombre_plantilla`, `tipo_plantilla`, `asunto`, `contenido_html`, `contenido_texto`, `variables_disponibles`, `estado`, `created_at`, `updated_at`) VALUES
(1, 1, 'Confirmacion Reserva', 'Confirmacion', 'Confirmación de Reserva - {{codigo_reserva}}', '<h2>¡Reserva Confirmada!</h2><p>Estimado {{cliente_nombre}},</p><p>Su reserva {{codigo_reserva}} ha sido confirmada exitosamente.</p><p>Detalles: {{detalles_reserva}}</p><p>Atentamente,<br>Equipo Turístico</p>', 'Reserva Confirmada - {{codigo_reserva}}. Estimado {{cliente_nombre}}, su reserva ha sido confirmada. Detalles: {{detalles_reserva}}. Atentamente, Equipo Turístico', NULL, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00'),
(2, 1, 'Recordatorio Reserva', 'Recordatorio', 'Recordatorio: Reserva mañana - {{codigo_reserva}}', '<h2>Recordatorio de Reserva</h2><p>Estimado {{cliente_nombre}},</p><p>Le recordamos que mañana tiene programada su reserva {{codigo_reserva}}.</p><p>Detalles: {{detalles_reserva}}</p><p>¡Los esperamos!</p>', 'Recordatorio: Mañana tiene su reserva {{codigo_reserva}}. Detalles: {{detalles_reserva}}. Los esperamos!', NULL, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00'),
(3, 1, 'Evaluacion Servicio', 'Evaluacion', '¿Cómo fue su experiencia? - {{codigo_reserva}}', '<h2>¡Gracias por elegirnos!</h2><p>Estimado {{cliente_nombre}},</p><p>Esperamos que haya disfrutado su experiencia. Nos gustaría conocer su opinión.</p><p><a href=\"{{enlace_evaluacion}}\">Calificar Servicio</a></p><p>¡Gracias!</p>', 'Gracias por elegirnos! Esperamos que haya disfrutado su experiencia. Califique nuestro servicio en: {{enlace_evaluacion}}. Gracias!', NULL, 1, '2025-11-01 22:22:00', '2025-11-01 22:22:00');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `politicas_cancelacion`
--

CREATE TABLE `politicas_cancelacion` (
  `id_politica` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `nombre_politica` varchar(255) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `dias_anticipacion` int(11) NOT NULL,
  `porcentaje_penalidad` decimal(5,2) NOT NULL,
  `aplica_a` enum('Todos','Tours','Hoteles','Transporte') DEFAULT 'Todos',
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `promociones`
--

CREATE TABLE `promociones` (
  `id_promocion` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `nombre_promocion` varchar(255) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `tipo_promocion` enum('Descuento','2x1','Paquete','Temporada') NOT NULL,
  `valor_descuento` decimal(5,2) DEFAULT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date NOT NULL,
  `aplica_a` enum('Todos','Servicios','Paquetes','Reservas') DEFAULT 'Todos',
  `id_servicio_aplicable` int(11) DEFAULT NULL,
  `id_paquete_aplicable` int(11) DEFAULT NULL,
  `codigo_promocional` varchar(50) DEFAULT NULL,
  `usos_maximos` int(11) DEFAULT NULL,
  `usos_actuales` int(11) DEFAULT 0,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activa, 0=Inactiva',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `proveedores`
--

CREATE TABLE `proveedores` (
  `id_proveedor` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `nombre_razon_social` varchar(255) NOT NULL,
  `ruc` varchar(20) NOT NULL,
  `tipo_servicio` varchar(100) NOT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `direccion` text DEFAULT NULL,
  `pagina_web` varchar(255) DEFAULT NULL,
  `contacto_principal` varchar(255) DEFAULT NULL,
  `observaciones` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reportes_generados`
--

CREATE TABLE `reportes_generados` (
  `id_reporte` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `tipo_reporte` varchar(100) NOT NULL,
  `parametros` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`parametros`)),
  `fecha_generacion` timestamp NOT NULL DEFAULT current_timestamp(),
  `archivo` varchar(255) DEFAULT NULL,
  `estado` enum('Generado','Enviado','Expirado') DEFAULT 'Generado'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reservas`
--

CREATE TABLE `reservas` (
  `id_reserva` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_sucursal` int(11) DEFAULT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_servicio` int(11) DEFAULT NULL,
  `id_paquete` int(11) DEFAULT NULL,
  `id_usuario` int(11) NOT NULL COMMENT 'Usuario que registró la reserva',
  `codigo_reserva` varchar(50) NOT NULL,
  `fecha_reserva` date NOT NULL,
  `fecha_servicio` date NOT NULL,
  `numero_personas` int(11) NOT NULL,
  `precio_total` decimal(10,2) NOT NULL,
  `id_promocion` int(11) DEFAULT NULL,
  `descuento_aplicado` decimal(10,2) DEFAULT 0.00,
  `estado` enum('Pendiente','Confirmada','Pago Parcial','Pagada','Cancelada','Completada') DEFAULT 'Pendiente',
  `observaciones` text DEFAULT NULL,
  `evaluada` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `reservas`
--

INSERT INTO `reservas` (`id_reserva`, `id_empresa`, `id_sucursal`, `id_cliente`, `id_servicio`, `id_paquete`, `id_usuario`, `codigo_reserva`, `fecha_reserva`, `fecha_servicio`, `numero_personas`, `precio_total`, `id_promocion`, `descuento_aplicado`, `estado`, `observaciones`, `evaluada`, `created_at`, `updated_at`, `deleted_at`) VALUES
(109, 14, 10, 87, NULL, NULL, 31, 'RES-14-10-2025-00001', '2025-12-22', '2026-12-26', 6, 450.00, NULL, 75.50, 'Completada', 'Cambio de fecha por solicitud del cliente', 0, '2025-12-22 18:14:53', '2025-12-23 00:54:09', NULL),
(111, 14, 11, 88, NULL, NULL, 32, 'RES-14-11-2025-00001', '2025-12-22', '2025-12-30', 4, 500.00, NULL, 0.00, 'Cancelada', '.', 0, '2025-12-22 19:38:32', '2025-12-23 01:18:08', NULL),
(112, 14, NULL, 88, NULL, NULL, 28, 'RES-14-00012', '2025-12-23', '2025-12-23', 1, 2000.00, NULL, 0.00, 'Pendiente', '', 0, '2025-12-23 01:19:15', '2025-12-23 01:19:15', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reservas_espera`
--

CREATE TABLE `reservas_espera` (
  `id_espera` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_servicio` int(11) NOT NULL,
  `fecha_solicitada` date NOT NULL,
  `numero_personas` int(11) NOT NULL,
  `prioridad` int(11) DEFAULT 1,
  `estado` enum('En Espera','Notificado','Cancelado','Convertido') DEFAULT 'En Espera',
  `fecha_registro` timestamp NOT NULL DEFAULT current_timestamp(),
  `fecha_notificacion` timestamp NULL DEFAULT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reserva_items`
--

CREATE TABLE `reserva_items` (
  `id_reserva_item` int(11) NOT NULL,
  `id_reserva` int(11) NOT NULL,
  `tipo_item` enum('SERVICIO','PAQUETE') NOT NULL,
  `id_servicio` int(11) DEFAULT NULL,
  `id_paquete` int(11) DEFAULT NULL,
  `cantidad` int(11) NOT NULL DEFAULT 1,
  `precio_unitario` decimal(10,2) NOT NULL,
  `precio_total` decimal(10,2) NOT NULL,
  `descripcion_extra` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `reserva_items`
--

INSERT INTO `reserva_items` (`id_reserva_item`, `id_reserva`, `tipo_item`, `id_servicio`, `id_paquete`, `cantidad`, `precio_unitario`, `precio_total`, `descripcion_extra`, `created_at`, `updated_at`) VALUES
(124, 109, 'SERVICIO', 17, NULL, 3, 150.00, 450.00, 'Actualizado', '2025-12-22 23:16:06', '2025-12-22 23:16:06'),
(134, 111, 'SERVICIO', 17, NULL, 1, 500.00, 500.00, '', '2025-12-23 01:17:37', '2025-12-23 01:17:37'),
(136, 112, 'SERVICIO', 17, NULL, 2, 1000.00, 2000.00, '', '2025-12-23 01:19:26', '2025-12-23 01:19:26');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `roles`
--

CREATE TABLE `roles` (
  `id_rol` int(11) NOT NULL,
  `nombre_rol` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL,
  `id_empresa` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `roles`
--

INSERT INTO `roles` (`id_rol`, `nombre_rol`, `descripcion`, `estado`, `created_at`, `updated_at`, `deleted_at`, `id_empresa`) VALUES
(1, 'Superadministrador', 'Control total del sistema multiempresa', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47', NULL, 0),
(2, 'Administrador', 'Administrador de empresa específica', 1, '2025-11-01 22:21:47', '2025-11-28 19:24:19', NULL, 1),
(3, 'Empleado', 'Usuario con permisos limitados', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47', NULL, 0),
(4, 'Gerente', 'Administrador de empresa específica', 1, '2025-12-18 23:26:02', '2025-12-18 23:26:02', NULL, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `roles_permisos`
--

CREATE TABLE `roles_permisos` (
  `id_rol_permiso` int(11) NOT NULL,
  `id_rol` int(11) NOT NULL,
  `id_permiso` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `roles_permisos`
--

INSERT INTO `roles_permisos` (`id_rol_permiso`, `id_rol`, `id_permiso`, `created_at`) VALUES
(1, 1, 2, '2025-11-01 22:21:47'),
(2, 1, 3, '2025-11-01 22:21:47'),
(3, 1, 4, '2025-11-01 22:21:47'),
(4, 1, 5, '2025-11-01 22:21:47'),
(5, 1, 1, '2025-11-01 22:21:47'),
(6, 1, 9, '2025-11-01 22:21:47'),
(7, 1, 7, '2025-11-01 22:21:47'),
(8, 1, 8, '2025-11-01 22:21:47'),
(9, 1, 6, '2025-11-01 22:21:47'),
(10, 1, 11, '2025-11-01 22:21:47'),
(11, 1, 12, '2025-11-01 22:21:47'),
(12, 1, 10, '2025-11-01 22:21:47'),
(13, 1, 13, '2025-11-01 22:21:47'),
(14, 1, 14, '2025-11-01 22:21:47'),
(15, 1, 15, '2025-11-01 22:21:47'),
(16, 1, 16, '2025-11-01 22:21:47'),
(17, 1, 18, '2025-11-01 22:21:47'),
(18, 1, 19, '2025-11-01 22:21:47'),
(19, 1, 17, '2025-11-01 22:21:47'),
(20, 1, 21, '2025-11-01 22:21:47'),
(21, 1, 22, '2025-11-01 22:21:47'),
(22, 1, 20, '2025-11-01 22:21:47'),
(23, 1, 24, '2025-11-01 22:21:47'),
(24, 1, 23, '2025-11-01 22:21:47'),
(25, 1, 26, '2025-11-01 22:21:47'),
(26, 1, 25, '2025-11-01 22:21:47'),
(32, 2, 2, '2025-11-01 22:21:47'),
(33, 2, 3, '2025-11-01 22:21:47'),
(34, 2, 4, '2025-11-01 22:21:47'),
(35, 2, 1, '2025-11-01 22:21:47'),
(36, 2, 9, '2025-11-01 22:21:47'),
(37, 2, 7, '2025-11-01 22:21:47'),
(38, 2, 8, '2025-11-01 22:21:47'),
(39, 2, 6, '2025-11-01 22:21:47'),
(40, 2, 11, '2025-11-01 22:21:47'),
(41, 2, 12, '2025-11-01 22:21:47'),
(42, 2, 10, '2025-11-01 22:21:47'),
(43, 2, 13, '2025-11-01 22:21:47'),
(44, 2, 14, '2025-11-01 22:21:47'),
(45, 2, 15, '2025-11-01 22:21:47'),
(46, 2, 16, '2025-11-01 22:21:47'),
(47, 2, 18, '2025-11-01 22:21:47'),
(48, 2, 19, '2025-11-01 22:21:47'),
(49, 2, 17, '2025-11-01 22:21:47'),
(50, 2, 21, '2025-11-01 22:21:47'),
(51, 2, 22, '2025-11-01 22:21:47'),
(52, 2, 20, '2025-11-01 22:21:47'),
(53, 2, 24, '2025-11-01 22:21:47'),
(54, 2, 23, '2025-11-01 22:21:47'),
(55, 2, 26, '2025-11-01 22:21:47'),
(56, 2, 25, '2025-11-01 22:21:47'),
(63, 3, 7, '2025-11-01 22:21:47'),
(64, 3, 6, '2025-11-01 22:21:47'),
(65, 3, 15, '2025-11-01 22:21:47'),
(66, 3, 18, '2025-11-01 22:21:47'),
(67, 3, 17, '2025-11-01 22:21:47'),
(68, 3, 20, '2025-11-01 22:21:47'),
(69, 3, 25, '2025-11-01 22:21:47'),
(70, 4, 2, '2025-12-22 18:13:06'),
(71, 4, 3, '2025-12-22 18:13:06'),
(72, 4, 4, '2025-12-22 18:13:06'),
(73, 4, 1, '2025-12-22 18:13:06'),
(74, 4, 9, '2025-12-22 18:13:06'),
(75, 4, 7, '2025-12-22 18:13:06'),
(76, 4, 8, '2025-12-22 18:13:06'),
(77, 4, 6, '2025-12-22 18:13:06'),
(78, 4, 11, '2025-12-22 18:13:06'),
(79, 4, 12, '2025-12-22 18:13:06'),
(80, 4, 10, '2025-12-22 18:13:06'),
(81, 4, 13, '2025-12-22 18:13:06'),
(82, 4, 14, '2025-12-22 18:13:06'),
(83, 4, 15, '2025-12-22 18:13:06'),
(84, 4, 16, '2025-12-22 18:13:06'),
(85, 4, 18, '2025-12-22 18:13:06'),
(86, 4, 19, '2025-12-22 18:13:06'),
(87, 4, 17, '2025-12-22 18:13:06'),
(88, 4, 21, '2025-12-22 18:13:06'),
(89, 4, 22, '2025-12-22 18:13:06'),
(90, 4, 20, '2025-12-22 18:13:06'),
(91, 4, 24, '2025-12-22 18:13:06'),
(92, 4, 23, '2025-12-22 18:13:06'),
(93, 4, 26, '2025-12-22 18:13:06'),
(94, 4, 25, '2025-12-22 18:13:06');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `servicios_turisticos`
--

CREATE TABLE `servicios_turisticos` (
  `id_servicio` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_proveedor` int(11) DEFAULT NULL,
  `id_categoria` int(11) DEFAULT NULL,
  `tipo_servicio` enum('Tour','Hotel','Transporte','Entrada/Atractivo') NOT NULL,
  `nombre_servicio` varchar(255) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `ubicacion_destino` varchar(255) DEFAULT NULL,
  `duracion` varchar(100) DEFAULT NULL,
  `capacidad_maxima` int(11) NOT NULL,
  `precio_base` decimal(10,2) NOT NULL,
  `incluye` text DEFAULT NULL,
  `no_incluye` text DEFAULT NULL,
  `requisitos` text DEFAULT NULL,
  `politicas_especiales` text DEFAULT NULL,
  `imagenes` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`imagenes`)),
  `itinerario` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL,
  `id_sucursal` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `servicios_turisticos`
--

INSERT INTO `servicios_turisticos` (`id_servicio`, `id_empresa`, `id_proveedor`, `id_categoria`, `tipo_servicio`, `nombre_servicio`, `descripcion`, `ubicacion_destino`, `duracion`, `capacidad_maxima`, `precio_base`, `incluye`, `no_incluye`, `requisitos`, `politicas_especiales`, `imagenes`, `itinerario`, `estado`, `created_at`, `updated_at`, `deleted_at`, `id_sucursal`) VALUES
(1, 1, NULL, NULL, 'Tour', 'Tour Histórico por el Centro de Lima', 'Recorrido completo por los lugares históricos más importantes de Lima', 'Centro Histórico de Lima', '4 horas', 15, 45.00, 'Guía profesional, transporte, entradas a museos', 'Alimentación, propinas', 'DNI original, ropa cómoda', 'Cancelación gratuita hasta 24h antes', NULL, NULL, 1, '2025-11-03 01:36:15', '2025-11-03 01:36:15', NULL, NULL),
(2, 3, NULL, NULL, 'Tour', 'Motocar a Centro de Cusco', 'Recorrido completo por los lugares históricos más importantes de Lima', 'Centro Histórico de Lima', '5 horas', 10, 100.00, 'Guía profesional, transporte, entradas a museos', 'Alimentación, propinas', 'DNI original, ropa cómoda', 'Cancelación gratuita hasta 24h antes', NULL, NULL, 1, '2025-11-15 01:25:10', '2025-12-03 17:41:07', NULL, NULL),
(3, 6, NULL, NULL, 'Tour', 'City Tour Tarapoto', 'Recorrido guiado por los principales atractivos de Tarapoto', 'Tarapoto', '1 día', 20, 350.00, 'Transporte, guía bilingüe, entradas', 'Alimentación', 'DNI vigente, ropa ligera', 'Cancelación sin penalidad hasta 48h', NULL, NULL, 1, '2025-12-01 03:08:52', '2025-12-01 03:08:52', NULL, NULL),
(10, 1, NULL, NULL, 'Tour', 'Tour Histórico por el Centro de tarapoto', 'Recorrido completo por los lugares históricos más importantes de Lima', 'Centro Histórico de Lima', '8 horas', 15, 45.00, 'Guía profesional, transporte, entradas a museos', 'Alimentación, propinas', 'DNI original, ropa cómoda', 'Cancelación gratuita hasta 24h antes', NULL, NULL, 1, '2025-12-03 01:56:42', '2025-12-03 01:56:42', NULL, NULL),
(16, 12, NULL, NULL, 'Tour', 'City Tour Tarapoto', 'Recorrido guiado por los principales atractivos de Tarapoto', 'Tarapoto', '1 dia', 20, 350.00, 'Transporte, guía bilingüe, entradas', NULL, 'Alimentación', 'Cancelación sin penalidad hasta 48h', NULL, NULL, 1, '2025-12-15 02:23:45', '2025-12-15 02:23:45', NULL, NULL),
(17, 14, NULL, NULL, 'Tour', 'Tour Histórico por el Centro de Lima Tarapoto', 'Recorrido completo por los lugares históricos más ...', 'lima', '10 horas', 20, 1000.00, 'Guía profesional, transporte, entradas a museos', 'Alimentación, propinas', 'DNI original', 'Cancelación gratuita hasta 24h antes', NULL, NULL, 1, '2025-12-17 14:50:46', '2025-12-22 06:20:32', NULL, 10);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `sesiones_usuario`
--

CREATE TABLE `sesiones_usuario` (
  `id_sesion` varchar(255) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `ip_address` varchar(45) NOT NULL,
  `user_agent` text DEFAULT NULL,
  `fecha_login` timestamp NOT NULL DEFAULT current_timestamp(),
  `fecha_expiracion` timestamp NULL DEFAULT NULL,
  `estado` enum('Activa','Expirada','Cerrada') DEFAULT 'Activa',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `sucursales`
--

CREATE TABLE `sucursales` (
  `id_sucursal` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `nombre_sucursal` varchar(255) NOT NULL,
  `ubicacion` varchar(255) NOT NULL,
  `direccion` text DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `gerente` varchar(255) DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `sucursales`
--

INSERT INTO `sucursales` (`id_sucursal`, `id_empresa`, `nombre_sucursal`, `ubicacion`, `direccion`, `telefono`, `email`, `gerente`, `estado`, `created_at`, `updated_at`, `deleted_at`) VALUES
(2, 1, 'Sucursal Principal', 'Centro de Lima', NULL, NULL, NULL, NULL, 1, '2025-11-13 03:49:36', '2025-11-13 03:49:36', NULL),
(5, 6, 'Sucursal Reportes QA', 'Demo multiempresa', NULL, NULL, NULL, NULL, 1, '2025-12-03 05:36:23', '2025-12-03 05:36:23', NULL),
(6, 6, 'Sucursal Reportes QA - Emp6', 'Demo multiempresa', NULL, NULL, NULL, NULL, 1, '2025-12-03 06:45:35', '2025-12-03 06:45:35', NULL),
(8, 14, 'NUEVA CAJAMARCA', 'Nueva Cajamarca', 'Av. Cajamarca Norte', '980554222', 'nc@gmail.com', 'Alexander', 1, '2025-12-19 00:07:00', '2025-12-19 00:15:30', NULL),
(9, 13, 'TARAPOTO', 'TARAPOTO', 'Av. pikaras 500', '996688244', 'mt@gmail.com', 'Mark Trikilin', 1, '2025-12-19 00:16:46', '2025-12-19 00:16:46', NULL),
(10, 14, 'TARAPOTO', 'TARAPOTO', 'Av. Cajamarca 400', '996588244', 'pr@gmail.com', 'Efrain', 1, '2025-12-19 03:21:32', '2025-12-19 03:21:32', NULL),
(11, 14, 'MORALES', 'TARAPOTO', 'Av. peru 123', '996588244', 'mt1@gmail.com', 'Alexander Pezo', 1, '2025-12-19 03:43:08', '2025-12-19 03:43:08', NULL),
(12, 14, 'LA BANDA DE SHILCAYO', 'TARAPOTO', 'Av. La Roca 200', '999888777', 'lb@gmail.com', 'Cristian Diaz', 1, '2025-12-19 22:22:15', '2025-12-22 03:31:22', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tipos_gasto`
--

CREATE TABLE `tipos_gasto` (
  `id_tipo_gasto` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `nombre_tipo` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `tipos_gasto`
--

INSERT INTO `tipos_gasto` (`id_tipo_gasto`, `id_empresa`, `nombre_tipo`, `descripcion`, `estado`, `created_at`, `updated_at`) VALUES
(1, 1, 'Combustible', 'Gastos en combustible para vehículos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(2, 1, 'Alimentación', 'Gastos en alimentación para tours', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(3, 1, 'Mantenimiento', 'Mantenimiento de vehículos y equipos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(4, 1, 'Peajes', 'Pago de peajes en rutas', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(5, 1, 'Entradas', 'Pago de entradas a atractivos turísticos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(6, 1, 'Equipamiento', 'Compra de equipamiento', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(7, 1, 'Uniformes', 'Compra y mantenimiento de uniformes', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47'),
(8, 1, 'Otros', 'Otros gastos operativos', 1, '2025-11-01 22:21:47', '2025-11-01 22:21:47');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `id_usuario` int(11) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_sucursal` int(11) DEFAULT NULL,
  `id_rol` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `apellido` varchar(100) NOT NULL,
  `email` varchar(255) NOT NULL,
  `email_encriptado` varbinary(255) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `dni` varchar(20) DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `telefono_encriptado` varbinary(255) DEFAULT NULL,
  `fecha_nacimiento` date DEFAULT NULL,
  `estado` int(11) NOT NULL,
  `ultimo_login` timestamp NULL DEFAULT NULL,
  `intentos_fallidos` int(11) NOT NULL,
  `bloqueado_hasta` timestamp NULL DEFAULT NULL,
  `codigo_2fa` varchar(10) DEFAULT NULL,
  `secreto_2fa` varchar(32) DEFAULT NULL,
  `verificado_2fa` tinyint(1) DEFAULT 0,
  `token_recuperacion` varchar(255) DEFAULT NULL,
  `expiracion_token` timestamp NULL DEFAULT NULL,
  `id_google` varchar(50) DEFAULT NULL,
  `id_outlook` varchar(50) DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `deleted_at` timestamp NULL DEFAULT NULL,
  `token` varchar(1000) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `usuarios`
--

INSERT INTO `usuarios` (`id_usuario`, `id_empresa`, `id_sucursal`, `id_rol`, `nombre`, `apellido`, `email`, `email_encriptado`, `password_hash`, `dni`, `telefono`, `telefono_encriptado`, `fecha_nacimiento`, `estado`, `ultimo_login`, `intentos_fallidos`, `bloqueado_hasta`, `codigo_2fa`, `secreto_2fa`, `verificado_2fa`, `token_recuperacion`, `expiracion_token`, `id_google`, `id_outlook`, `avatar_url`, `created_at`, `updated_at`, `deleted_at`, `token`) VALUES
(16, 1, NULL, 2, 'cristian mark', 'lopez', 'crima@gmail.com', NULL, '$2a$10$Q7aBfM4u9ItIb1ds13qseu6u2WxkNGQL8SfKhZOQd3fK3tw5D8Wa2', '42589136', NULL, NULL, NULL, 1, '2025-12-03 02:40:08', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-11-28 19:12:56', '2025-12-03 02:55:45', NULL, NULL),
(18, 6, NULL, 2, 'Kael', 'D', 'kaeld@gmail.com', NULL, '$2a$10$SSJEC.gVmE3EYlF4JnVNougxn42EgGrBW4Be1o82I9ZQZ1TKBJWcW', '74859616', NULL, NULL, NULL, 1, '2025-12-23 01:32:01', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-11-29 02:53:28', '2025-12-23 01:32:13', NULL, NULL),
(19, 0, NULL, 1, 'Bryan', 'Abarca', 'bry@gmail.com', NULL, '$2a$10$pc68q70whoQhi4MYXFgBTu38sfTbTz.DBJtMBlN7.UKetpNtjlWd.', '74859612', NULL, NULL, NULL, 1, '2025-12-19 22:03:32', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-11-29 03:53:31', '2025-12-19 22:05:33', NULL, NULL),
(21, 8, NULL, 2, 'MARK DARLEY', 'GARCÍA SALAS', 'ma1@gmail.com', NULL, '$2a$10$R3QrLDMtdCTn.l2AyqGf1uGHf93/TK7iUY0Cm60XTTsEtOhO3HR2m', '48596712', NULL, NULL, NULL, 1, '2025-12-23 01:46:52', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-03 13:46:57', '2025-12-23 01:47:02', NULL, NULL),
(22, 9, NULL, 2, 'Danny Alexander', 'Pezo Inga', 'xan@gmail.com', NULL, '$2a$10$jPNYrFA5UEYsMOnEZ4.Rqe0BeKaSmCZaBgjoQyVLeDBl2u73SgubC', '74859613', NULL, NULL, NULL, 1, '2025-12-15 08:55:57', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-03 13:50:18', '2025-12-15 08:59:31', NULL, NULL),
(23, 10, NULL, 2, 'CRISTOFER STALIN', 'RIOS DIOPE', 'sta@gmail.com', NULL, '$2a$10$x4fkXCAiYMHK0/Bwy/cEK.vD0oXI3gOyTzPXWxJXcdriin9NfUBeu', '74859618', NULL, NULL, NULL, 1, NULL, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-03 13:53:09', '2025-12-03 14:35:10', NULL, NULL),
(24, 11, NULL, 2, 'Maykel Anggelo', 'Cachique Amasifuen', 'may@gmail.com', NULL, '$2a$10$YqvdQGFuRlHcF/grV7x.GePN1RoCQbnrAwiHnPutw1kBZCOYiS4QG', '74859614', NULL, NULL, NULL, 1, NULL, 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-03 13:56:23', '2025-12-03 14:36:50', NULL, NULL),
(26, 12, NULL, 2, 'Max Delinger', 'Cristobal Vasquez', 'max@gmail.com', NULL, '$2a$10$h9vYX4H.ftMHhgThtfxGV.O5mXrmmrQK7kpPK0riryWd6AMxGCJjy', '74859652', NULL, NULL, NULL, 1, '2025-12-22 01:04:12', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-03 13:59:19', '2025-12-22 01:04:12', NULL, 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXhAZ21haWwuY29tIiwidXNlcklkIjoyNiwiZW1wcmVzYUlkIjoxMiwicm9sZXMiOlsiUk9MRV9BRE1JTklTVFJBRE9SIl0sImlzcyI6InNpc3RlbWEtdHVyaXN0aWNvLWJhY2tlbmQiLCJpYXQiOjE3NjYzNjU0NTIsImV4cCI6MTc2NjQ1MTg1Mn0.TEjmI8dAcf-yG8BaVWvgXLQY1wInJi2aRZr16bzWcqA'),
(27, 13, NULL, 2, 'Jorge', 'Ramos', 'jor@gmail.com', NULL, '$2a$10$/men/hzwheW7MdprX917e.Gyi0edm9yjI9XByBsOTUZtsKfHxEYY6', '12344678', NULL, NULL, NULL, 1, '2025-12-17 14:46:32', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-03 14:01:41', '2025-12-17 14:46:32', NULL, 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb3JAZ21haWwuY29tIiwidXNlcklkIjoyNywiZW1wcmVzYUlkIjoxMywicm9sZXMiOlsiUk9MRV9BRE1JTklTVFJBRE9SIl0sImlzcyI6InNpc3RlbWEtdHVyaXN0aWNvLWJhY2tlbmQiLCJpYXQiOjE3NjU5ODI3OTIsImV4cCI6MTc2NjA2OTE5Mn0.GjJ-7AZfAdpEFyYABfzqca4QAegSkrPjEn9DHFCycGs'),
(28, 14, NULL, 2, 'Bryan Jampier', 'Abarca Irigoin', 'iri@gmail.com', NULL, '$2a$10$F54efEUuYYtZoSLzZNa10OSxFC/4dAmf01ELkDMqwMno93epcnYiC', '90484997', NULL, NULL, NULL, 1, '2025-12-23 01:47:41', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-03 14:39:27', '2025-12-23 01:50:04', NULL, NULL),
(31, 14, 10, 4, 'bryan', 'Abarca', 'leivatico@gmail.com', NULL, '$2a$10$3eIKxtE2xGZ3TcXZEgVf2exeS0KsBHBKWgwLavkxtLLS0v.brZiki', '733251066', NULL, NULL, NULL, 1, '2025-12-23 01:50:11', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-19 03:33:24', '2025-12-23 01:50:11', NULL, 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsZWl2YXRpY29AZ21haWwuY29tIiwidXNlcklkIjozMSwiZW1wcmVzYUlkIjoxNCwicm9sZXMiOlsiUk9MRV9HRVJFTlRFIl0sImlzcyI6InNpc3RlbWEtdHVyaXN0aWNvLWJhY2tlbmQiLCJpYXQiOjE3NjY0NTEzMDgsImV4cCI6MTc2NjUzNzcwOH0.v8PfXyYx-Mhdau7cEhLCRP-yc8DY-cA2FSjR57iiNHY'),
(32, 14, 11, 4, 'Alexander', 'Pezo', 'mt1@gmail.com', NULL, '$2a$10$.vdaruWPrihsRHThtarH/eNLf5cDey0gI4Fp6mTcw1XrUp/HnJSZu', '74859816', NULL, NULL, NULL, 1, '2025-12-22 23:36:50', 0, NULL, NULL, NULL, 0, NULL, NULL, NULL, NULL, NULL, '2025-12-19 03:44:59', '2025-12-23 00:03:53', NULL, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ventas`
--

CREATE TABLE `ventas` (
  `id_venta` bigint(20) NOT NULL,
  `id_empresa` int(11) NOT NULL,
  `id_cliente` int(11) DEFAULT NULL,
  `id_reserva` int(11) DEFAULT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_caja` int(11) NOT NULL,
  `fecha_hora` timestamp NOT NULL DEFAULT current_timestamp(),
  `monto_total` decimal(10,2) NOT NULL,
  `metodo_pago` varchar(50) NOT NULL,
  `numero_operacion` varchar(100) DEFAULT NULL,
  `comprobante` varchar(100) DEFAULT NULL,
  `descuento` decimal(10,2) DEFAULT 0.00,
  `propina` decimal(10,2) DEFAULT 0.00,
  `observaciones` text DEFAULT NULL,
  `estado` tinyint(1) DEFAULT 1 COMMENT '1=Registrada, 0=Anulada',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `id_sucursal` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `vouchers`
--

CREATE TABLE `vouchers` (
  `id_voucher` bigint(20) NOT NULL,
  `id_reserva` int(11) NOT NULL,
  `codigo_qr` varchar(255) DEFAULT NULL,
  `fecha_emision` date NOT NULL,
  `fecha_expiracion` date DEFAULT NULL,
  `estado` enum('Emitido','Usado','Expirado','Cancelado') DEFAULT 'Emitido',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `archivos_adjuntos`
--
ALTER TABLE `archivos_adjuntos`
  ADD PRIMARY KEY (`id_archivo`),
  ADD KEY `uploaded_by` (`uploaded_by`),
  ADD KEY `idx_origen` (`tabla_origen`,`id_registro_origen`),
  ADD KEY `idx_empresa_archivo` (`id_empresa`);

--
-- Indices de la tabla `asignaciones_personal`
--
ALTER TABLE `asignaciones_personal`
  ADD PRIMARY KEY (`id_asignacion`),
  ADD UNIQUE KEY `unique_personal_reserva` (`id_personal`,`id_reserva`,`fecha_asignacion`),
  ADD UNIQUE KEY `UK7lag04qo9660a1wdhauvncqj4` (`id_personal`,`id_reserva`,`fecha_asignacion`),
  ADD KEY `id_reserva` (`id_reserva`),
  ADD KEY `idx_asignaciones_personal_estado` (`estado`),
  ADD KEY `idx_asignaciones_personal_personal_fecha_estado` (`id_personal`,`fecha_asignacion`,`estado`),
  ADD KEY `idx_asignaciones_personal_sucursal` (`id_sucursal`);

--
-- Indices de la tabla `auditoria_logs`
--
ALTER TABLE `auditoria_logs`
  ADD PRIMARY KEY (`id_log`),
  ADD KEY `idx_auditoria_fecha` (`fecha_hora`);

--
-- Indices de la tabla `bloqueo_ip`
--
ALTER TABLE `bloqueo_ip`
  ADD PRIMARY KEY (`id_bloqueo`),
  ADD UNIQUE KEY `ip_address` (`ip_address`),
  ADD KEY `idx_ip_bloqueada` (`ip_address`),
  ADD KEY `idx_bloqueado_hasta` (`bloqueado_hasta`);

--
-- Indices de la tabla `cajas`
--
ALTER TABLE `cajas`
  ADD PRIMARY KEY (`id_caja`),
  ADD KEY `id_empresa` (`id_empresa`),
  ADD KEY `id_usuario_apertura` (`id_usuario_apertura`),
  ADD KEY `id_usuario_cierre` (`id_usuario_cierre`),
  ADD KEY `cajas_ibfk_2` (`id_sucursal`);

--
-- Indices de la tabla `calendario_disponibilidad`
--
ALTER TABLE `calendario_disponibilidad`
  ADD PRIMARY KEY (`id_calendario`),
  ADD UNIQUE KEY `unique_servicio_fecha` (`id_servicio`,`fecha`),
  ADD KEY `idx_calendario_fecha` (`fecha`);

--
-- Indices de la tabla `capacitaciones`
--
ALTER TABLE `capacitaciones`
  ADD PRIMARY KEY (`id_capacitacion`),
  ADD KEY `id_personal` (`id_personal`);

--
-- Indices de la tabla `categorias_servicios`
--
ALTER TABLE `categorias_servicios`
  ADD PRIMARY KEY (`id_categoria`),
  ADD UNIQUE KEY `unique_categoria_empresa` (`id_empresa`,`nombre_categoria`),
  ADD KEY `idx_orden` (`orden`);

--
-- Indices de la tabla `clientes`
--
ALTER TABLE `clientes`
  ADD PRIMARY KEY (`id_cliente`),
  ADD UNIQUE KEY `unique_email_empresa` (`id_empresa`,`email`),
  ADD UNIQUE KEY `unique_dni_empresa` (`id_empresa`,`dni`),
  ADD KEY `idx_clientes_empresa` (`id_empresa`),
  ADD KEY `idx_clientes_email` (`email`),
  ADD KEY `idx_clientes_dni` (`dni`),
  ADD KEY `id_sucursal` (`id_sucursal`);

--
-- Indices de la tabla `configuraciones_empresa`
--
ALTER TABLE `configuraciones_empresa`
  ADD PRIMARY KEY (`id_configuracion`),
  ADD UNIQUE KEY `unique_config_empresa` (`id_empresa`,`clave`),
  ADD KEY `idx_empresa_config` (`id_empresa`);

--
-- Indices de la tabla `contratos_proveedores`
--
ALTER TABLE `contratos_proveedores`
  ADD PRIMARY KEY (`id_contrato`),
  ADD KEY `id_proveedor` (`id_proveedor`),
  ADD KEY `id_empresa` (`id_empresa`);

--
-- Indices de la tabla `deudas_proveedores`
--
ALTER TABLE `deudas_proveedores`
  ADD PRIMARY KEY (`id_deuda`),
  ADD KEY `id_proveedor` (`id_proveedor`),
  ADD KEY `id_empresa` (`id_empresa`);

--
-- Indices de la tabla `empresas`
--
ALTER TABLE `empresas`
  ADD PRIMARY KEY (`id_empresa`),
  ADD UNIQUE KEY `ruc` (`ruc`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indices de la tabla `evaluaciones_servicios`
--
ALTER TABLE `evaluaciones_servicios`
  ADD PRIMARY KEY (`id_evaluacion`),
  ADD KEY `id_reserva` (`id_reserva`),
  ADD KEY `id_cliente` (`id_cliente`),
  ADD KEY `id_servicio` (`id_servicio`),
  ADD KEY `id_paquete` (`id_paquete`),
  ADD KEY `idx_calificacion` (`calificacion_general`),
  ADD KEY `idx_fecha_eval` (`fecha_evaluacion`);

--
-- Indices de la tabla `flyway_schema_history`
--
ALTER TABLE `flyway_schema_history`
  ADD PRIMARY KEY (`installed_rank`),
  ADD KEY `flyway_schema_history_s_idx` (`success`);

--
-- Indices de la tabla `gastos_operativos`
--
ALTER TABLE `gastos_operativos`
  ADD PRIMARY KEY (`id_gasto`),
  ADD KEY `id_proveedor` (`id_proveedor`),
  ADD KEY `id_personal` (`id_personal`),
  ADD KEY `id_reserva` (`id_reserva`),
  ADD KEY `idx_gastos_empresa` (`id_empresa`),
  ADD KEY `idx_gastos_fecha` (`fecha_gasto`),
  ADD KEY `idx_gastos_tipo_fecha` (`id_tipo_gasto`,`fecha_gasto`);

--
-- Indices de la tabla `historial_contrasenas`
--
ALTER TABLE `historial_contrasenas`
  ADD PRIMARY KEY (`id_historial`),
  ADD KEY `id_usuario` (`id_usuario`);

--
-- Indices de la tabla `integraciones_calendario`
--
ALTER TABLE `integraciones_calendario`
  ADD PRIMARY KEY (`id_integracion`),
  ADD UNIQUE KEY `unique_empresa_calendario` (`id_empresa`,`proveedor_calendario`);

--
-- Indices de la tabla `log_emails`
--
ALTER TABLE `log_emails`
  ADD PRIMARY KEY (`id_log_email`),
  ADD KEY `id_empresa` (`id_empresa`),
  ADD KEY `id_usuario_destino` (`id_usuario_destino`),
  ADD KEY `id_plantilla` (`id_plantilla`),
  ADD KEY `idx_fecha_envio` (`fecha_envio`),
  ADD KEY `idx_estado` (`estado_envio`);

--
-- Indices de la tabla `modulos`
--
ALTER TABLE `modulos`
  ADD PRIMARY KEY (`id_modulo`),
  ADD UNIQUE KEY `nombre_modulo` (`nombre_modulo`);

--
-- Indices de la tabla `movimientos_caja`
--
ALTER TABLE `movimientos_caja`
  ADD PRIMARY KEY (`id_movimiento`),
  ADD KEY `id_caja` (`id_caja`),
  ADD KEY `FKfbwwbwirle698wn2ruo1csw74` (`id_venta`);

--
-- Indices de la tabla `notificaciones`
--
ALTER TABLE `notificaciones`
  ADD PRIMARY KEY (`id_notificacion`),
  ADD KEY `id_empresa` (`id_empresa`),
  ADD KEY `idx_usuario_notif` (`id_usuario_destino`),
  ADD KEY `idx_tipo_fecha` (`tipo_notificacion`,`fecha_envio`),
  ADD KEY `idx_leida` (`leida`),
  ADD KEY `idx_notificaciones_usuario_fecha` (`id_usuario_destino`,`fecha_envio`,`leida`);

--
-- Indices de la tabla `pagos_deudas`
--
ALTER TABLE `pagos_deudas`
  ADD PRIMARY KEY (`id_pago_deuda`),
  ADD KEY `id_deuda` (`id_deuda`);

--
-- Indices de la tabla `pagos_reservas`
--
ALTER TABLE `pagos_reservas`
  ADD PRIMARY KEY (`id_pago`),
  ADD KEY `id_reserva` (`id_reserva`),
  ADD KEY `id_usuario` (`id_usuario`),
  ADD KEY `idx_pagos_fecha` (`fecha_pago`);

--
-- Indices de la tabla `paquetes_servicios`
--
ALTER TABLE `paquetes_servicios`
  ADD PRIMARY KEY (`id_paquete_servicio`),
  ADD UNIQUE KEY `unique_paquete_servicio` (`id_paquete`,`id_servicio`),
  ADD UNIQUE KEY `UK36yweik6l3nh8yoxqcut65a1g` (`id_paquete`,`id_servicio`),
  ADD KEY `id_servicio` (`id_servicio`);

--
-- Indices de la tabla `paquetes_turisticos`
--
ALTER TABLE `paquetes_turisticos`
  ADD PRIMARY KEY (`id_paquete`),
  ADD KEY `id_empresa` (`id_empresa`),
  ADD KEY `idx_paquetes_turisticos_sucursal` (`id_sucursal`);

--
-- Indices de la tabla `permisos`
--
ALTER TABLE `permisos`
  ADD PRIMARY KEY (`id_permiso`),
  ADD UNIQUE KEY `unique_permiso_modulo` (`id_modulo`,`nombre_permiso`);

--
-- Indices de la tabla `personal`
--
ALTER TABLE `personal`
  ADD PRIMARY KEY (`id_personal`),
  ADD UNIQUE KEY `dni` (`dni`),
  ADD KEY `id_sucursal` (`id_sucursal`),
  ADD KEY `idx_personal_empresa` (`id_empresa`);

--
-- Indices de la tabla `plantillas_email`
--
ALTER TABLE `plantillas_email`
  ADD PRIMARY KEY (`id_plantilla`),
  ADD UNIQUE KEY `unique_plantilla_empresa` (`id_empresa`,`nombre_plantilla`);

--
-- Indices de la tabla `politicas_cancelacion`
--
ALTER TABLE `politicas_cancelacion`
  ADD PRIMARY KEY (`id_politica`),
  ADD KEY `id_empresa` (`id_empresa`);

--
-- Indices de la tabla `promociones`
--
ALTER TABLE `promociones`
  ADD PRIMARY KEY (`id_promocion`),
  ADD UNIQUE KEY `codigo_promocional` (`codigo_promocional`),
  ADD KEY `id_empresa` (`id_empresa`),
  ADD KEY `id_servicio_aplicable` (`id_servicio_aplicable`),
  ADD KEY `id_paquete_aplicable` (`id_paquete_aplicable`),
  ADD KEY `idx_fecha_promocion` (`fecha_inicio`,`fecha_fin`),
  ADD KEY `idx_codigo` (`codigo_promocional`);

--
-- Indices de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  ADD PRIMARY KEY (`id_proveedor`),
  ADD UNIQUE KEY `ruc` (`ruc`),
  ADD KEY `idx_proveedores_empresa` (`id_empresa`);

--
-- Indices de la tabla `reportes_generados`
--
ALTER TABLE `reportes_generados`
  ADD PRIMARY KEY (`id_reporte`),
  ADD KEY `id_empresa` (`id_empresa`),
  ADD KEY `id_usuario` (`id_usuario`);

--
-- Indices de la tabla `reservas`
--
ALTER TABLE `reservas`
  ADD PRIMARY KEY (`id_reserva`),
  ADD UNIQUE KEY `codigo_reserva` (`codigo_reserva`),
  ADD KEY `id_servicio` (`id_servicio`),
  ADD KEY `id_paquete` (`id_paquete`),
  ADD KEY `id_usuario` (`id_usuario`),
  ADD KEY `idx_reservas_empresa` (`id_empresa`),
  ADD KEY `idx_reservas_fecha` (`fecha_reserva`),
  ADD KEY `idx_reservas_estado` (`estado`),
  ADD KEY `fk_reserva_promocion` (`id_promocion`),
  ADD KEY `idx_reservas_cliente_fecha` (`id_cliente`,`fecha_reserva`),
  ADD KEY `idx_reservas_cliente` (`id_cliente`),
  ADD KEY `id_sucursal` (`id_sucursal`);

--
-- Indices de la tabla `reservas_espera`
--
ALTER TABLE `reservas_espera`
  ADD PRIMARY KEY (`id_espera`),
  ADD KEY `id_empresa` (`id_empresa`),
  ADD KEY `id_cliente` (`id_cliente`),
  ADD KEY `id_servicio` (`id_servicio`),
  ADD KEY `idx_fecha_espera` (`fecha_solicitada`),
  ADD KEY `idx_estado_espera` (`estado`);

--
-- Indices de la tabla `reserva_items`
--
ALTER TABLE `reserva_items`
  ADD PRIMARY KEY (`id_reserva_item`),
  ADD KEY `idx_reserva_items_reserva` (`id_reserva`),
  ADD KEY `idx_reserva_items_servicio` (`id_servicio`),
  ADD KEY `idx_reserva_items_paquete` (`id_paquete`);

--
-- Indices de la tabla `roles`
--
ALTER TABLE `roles`
  ADD PRIMARY KEY (`id_rol`),
  ADD UNIQUE KEY `nombre_rol` (`nombre_rol`),
  ADD KEY `id_empresa` (`id_empresa`);

--
-- Indices de la tabla `roles_permisos`
--
ALTER TABLE `roles_permisos`
  ADD PRIMARY KEY (`id_rol_permiso`),
  ADD UNIQUE KEY `unique_rol_permiso` (`id_rol`,`id_permiso`),
  ADD KEY `id_permiso` (`id_permiso`);

--
-- Indices de la tabla `servicios_turisticos`
--
ALTER TABLE `servicios_turisticos`
  ADD PRIMARY KEY (`id_servicio`),
  ADD KEY `id_proveedor` (`id_proveedor`),
  ADD KEY `idx_servicios_empresa` (`id_empresa`),
  ADD KEY `fk_servicio_categoria` (`id_categoria`),
  ADD KEY `idx_servicios_tipo` (`tipo_servicio`),
  ADD KEY `idx_servicios_estado` (`estado`),
  ADD KEY `idx_servicios_turisticos_sucursal` (`id_sucursal`);

--
-- Indices de la tabla `sesiones_usuario`
--
ALTER TABLE `sesiones_usuario`
  ADD PRIMARY KEY (`id_sesion`),
  ADD KEY `idx_usuario_sesion` (`id_usuario`),
  ADD KEY `idx_fecha_expiracion` (`fecha_expiracion`);

--
-- Indices de la tabla `sucursales`
--
ALTER TABLE `sucursales`
  ADD PRIMARY KEY (`id_sucursal`),
  ADD UNIQUE KEY `unique_sucursal_empresa` (`id_empresa`,`nombre_sucursal`);

--
-- Indices de la tabla `tipos_gasto`
--
ALTER TABLE `tipos_gasto`
  ADD PRIMARY KEY (`id_tipo_gasto`),
  ADD UNIQUE KEY `unique_tipo_empresa` (`id_empresa`,`nombre_tipo`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id_usuario`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `dni` (`dni`),
  ADD KEY `id_sucursal` (`id_sucursal`),
  ADD KEY `id_rol` (`id_rol`),
  ADD KEY `idx_usuarios_email` (`email`),
  ADD KEY `id_empresa` (`id_empresa`);

--
-- Indices de la tabla `ventas`
--
ALTER TABLE `ventas`
  ADD PRIMARY KEY (`id_venta`),
  ADD KEY `id_cliente` (`id_cliente`),
  ADD KEY `id_reserva` (`id_reserva`),
  ADD KEY `id_usuario` (`id_usuario`),
  ADD KEY `idx_ventas_empresa` (`id_empresa`),
  ADD KEY `idx_ventas_fecha` (`fecha_hora`),
  ADD KEY `idx_ventas_caja_fecha` (`id_caja`,`fecha_hora`),
  ADD KEY `idx_ventas_sucursal` (`id_sucursal`);

--
-- Indices de la tabla `vouchers`
--
ALTER TABLE `vouchers`
  ADD PRIMARY KEY (`id_voucher`),
  ADD UNIQUE KEY `codigo_qr` (`codigo_qr`),
  ADD KEY `id_reserva` (`id_reserva`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `archivos_adjuntos`
--
ALTER TABLE `archivos_adjuntos`
  MODIFY `id_archivo` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `asignaciones_personal`
--
ALTER TABLE `asignaciones_personal`
  MODIFY `id_asignacion` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=77;

--
-- AUTO_INCREMENT de la tabla `auditoria_logs`
--
ALTER TABLE `auditoria_logs`
  MODIFY `id_log` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `bloqueo_ip`
--
ALTER TABLE `bloqueo_ip`
  MODIFY `id_bloqueo` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `cajas`
--
ALTER TABLE `cajas`
  MODIFY `id_caja` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

--
-- AUTO_INCREMENT de la tabla `calendario_disponibilidad`
--
ALTER TABLE `calendario_disponibilidad`
  MODIFY `id_calendario` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `capacitaciones`
--
ALTER TABLE `capacitaciones`
  MODIFY `id_capacitacion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `categorias_servicios`
--
ALTER TABLE `categorias_servicios`
  MODIFY `id_categoria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `clientes`
--
ALTER TABLE `clientes`
  MODIFY `id_cliente` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=89;

--
-- AUTO_INCREMENT de la tabla `configuraciones_empresa`
--
ALTER TABLE `configuraciones_empresa`
  MODIFY `id_configuracion` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT de la tabla `contratos_proveedores`
--
ALTER TABLE `contratos_proveedores`
  MODIFY `id_contrato` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `deudas_proveedores`
--
ALTER TABLE `deudas_proveedores`
  MODIFY `id_deuda` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `empresas`
--
ALTER TABLE `empresas`
  MODIFY `id_empresa` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT de la tabla `evaluaciones_servicios`
--
ALTER TABLE `evaluaciones_servicios`
  MODIFY `id_evaluacion` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `gastos_operativos`
--
ALTER TABLE `gastos_operativos`
  MODIFY `id_gasto` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `historial_contrasenas`
--
ALTER TABLE `historial_contrasenas`
  MODIFY `id_historial` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `integraciones_calendario`
--
ALTER TABLE `integraciones_calendario`
  MODIFY `id_integracion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `log_emails`
--
ALTER TABLE `log_emails`
  MODIFY `id_log_email` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `modulos`
--
ALTER TABLE `modulos`
  MODIFY `id_modulo` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT de la tabla `movimientos_caja`
--
ALTER TABLE `movimientos_caja`
  MODIFY `id_movimiento` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=56;

--
-- AUTO_INCREMENT de la tabla `notificaciones`
--
ALTER TABLE `notificaciones`
  MODIFY `id_notificacion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `pagos_deudas`
--
ALTER TABLE `pagos_deudas`
  MODIFY `id_pago_deuda` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `pagos_reservas`
--
ALTER TABLE `pagos_reservas`
  MODIFY `id_pago` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT de la tabla `paquetes_servicios`
--
ALTER TABLE `paquetes_servicios`
  MODIFY `id_paquete_servicio` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT de la tabla `paquetes_turisticos`
--
ALTER TABLE `paquetes_turisticos`
  MODIFY `id_paquete` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT de la tabla `permisos`
--
ALTER TABLE `permisos`
  MODIFY `id_permiso` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT de la tabla `personal`
--
ALTER TABLE `personal`
  MODIFY `id_personal` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=25;

--
-- AUTO_INCREMENT de la tabla `plantillas_email`
--
ALTER TABLE `plantillas_email`
  MODIFY `id_plantilla` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `politicas_cancelacion`
--
ALTER TABLE `politicas_cancelacion`
  MODIFY `id_politica` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `promociones`
--
ALTER TABLE `promociones`
  MODIFY `id_promocion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  MODIFY `id_proveedor` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT de la tabla `reportes_generados`
--
ALTER TABLE `reportes_generados`
  MODIFY `id_reporte` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `reservas`
--
ALTER TABLE `reservas`
  MODIFY `id_reserva` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=113;

--
-- AUTO_INCREMENT de la tabla `reservas_espera`
--
ALTER TABLE `reservas_espera`
  MODIFY `id_espera` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `reserva_items`
--
ALTER TABLE `reserva_items`
  MODIFY `id_reserva_item` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=137;

--
-- AUTO_INCREMENT de la tabla `roles`
--
ALTER TABLE `roles`
  MODIFY `id_rol` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `roles_permisos`
--
ALTER TABLE `roles_permisos`
  MODIFY `id_rol_permiso` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT de la tabla `servicios_turisticos`
--
ALTER TABLE `servicios_turisticos`
  MODIFY `id_servicio` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=18;

--
-- AUTO_INCREMENT de la tabla `sucursales`
--
ALTER TABLE `sucursales`
  MODIFY `id_sucursal` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT de la tabla `tipos_gasto`
--
ALTER TABLE `tipos_gasto`
  MODIFY `id_tipo_gasto` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `id_usuario` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=34;

--
-- AUTO_INCREMENT de la tabla `ventas`
--
ALTER TABLE `ventas`
  MODIFY `id_venta` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT de la tabla `vouchers`
--
ALTER TABLE `vouchers`
  MODIFY `id_voucher` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `archivos_adjuntos`
--
ALTER TABLE `archivos_adjuntos`
  ADD CONSTRAINT `archivos_adjuntos_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `archivos_adjuntos_ibfk_2` FOREIGN KEY (`uploaded_by`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `asignaciones_personal`
--
ALTER TABLE `asignaciones_personal`
  ADD CONSTRAINT `asignaciones_personal_ibfk_1` FOREIGN KEY (`id_personal`) REFERENCES `personal` (`id_personal`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `asignaciones_personal_ibfk_2` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_asignaciones_personal_sucursal` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `cajas`
--
ALTER TABLE `cajas`
  ADD CONSTRAINT `cajas_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `cajas_ibfk_2` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `cajas_ibfk_3` FOREIGN KEY (`id_usuario_apertura`) REFERENCES `usuarios` (`id_usuario`) ON UPDATE CASCADE,
  ADD CONSTRAINT `cajas_ibfk_4` FOREIGN KEY (`id_usuario_cierre`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `calendario_disponibilidad`
--
ALTER TABLE `calendario_disponibilidad`
  ADD CONSTRAINT `calendario_disponibilidad_ibfk_1` FOREIGN KEY (`id_servicio`) REFERENCES `servicios_turisticos` (`id_servicio`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `capacitaciones`
--
ALTER TABLE `capacitaciones`
  ADD CONSTRAINT `capacitaciones_ibfk_1` FOREIGN KEY (`id_personal`) REFERENCES `personal` (`id_personal`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `categorias_servicios`
--
ALTER TABLE `categorias_servicios`
  ADD CONSTRAINT `categorias_servicios_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `clientes`
--
ALTER TABLE `clientes`
  ADD CONSTRAINT `clientes_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `clientes_ibfk_2` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `configuraciones_empresa`
--
ALTER TABLE `configuraciones_empresa`
  ADD CONSTRAINT `configuraciones_empresa_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `contratos_proveedores`
--
ALTER TABLE `contratos_proveedores`
  ADD CONSTRAINT `contratos_proveedores_ibfk_1` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `contratos_proveedores_ibfk_2` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `deudas_proveedores`
--
ALTER TABLE `deudas_proveedores`
  ADD CONSTRAINT `deudas_proveedores_ibfk_1` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `deudas_proveedores_ibfk_2` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `evaluaciones_servicios`
--
ALTER TABLE `evaluaciones_servicios`
  ADD CONSTRAINT `evaluaciones_servicios_ibfk_1` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `evaluaciones_servicios_ibfk_2` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `evaluaciones_servicios_ibfk_3` FOREIGN KEY (`id_servicio`) REFERENCES `servicios_turisticos` (`id_servicio`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `evaluaciones_servicios_ibfk_4` FOREIGN KEY (`id_paquete`) REFERENCES `paquetes_turisticos` (`id_paquete`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `gastos_operativos`
--
ALTER TABLE `gastos_operativos`
  ADD CONSTRAINT `gastos_operativos_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `gastos_operativos_ibfk_2` FOREIGN KEY (`id_tipo_gasto`) REFERENCES `tipos_gasto` (`id_tipo_gasto`) ON UPDATE CASCADE,
  ADD CONSTRAINT `gastos_operativos_ibfk_3` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `gastos_operativos_ibfk_4` FOREIGN KEY (`id_personal`) REFERENCES `personal` (`id_personal`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `gastos_operativos_ibfk_5` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `historial_contrasenas`
--
ALTER TABLE `historial_contrasenas`
  ADD CONSTRAINT `historial_contrasenas_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `integraciones_calendario`
--
ALTER TABLE `integraciones_calendario`
  ADD CONSTRAINT `integraciones_calendario_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `log_emails`
--
ALTER TABLE `log_emails`
  ADD CONSTRAINT `log_emails_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `log_emails_ibfk_2` FOREIGN KEY (`id_usuario_destino`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `log_emails_ibfk_3` FOREIGN KEY (`id_plantilla`) REFERENCES `plantillas_email` (`id_plantilla`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `movimientos_caja`
--
ALTER TABLE `movimientos_caja`
  ADD CONSTRAINT `FKfbwwbwirle698wn2ruo1csw74` FOREIGN KEY (`id_venta`) REFERENCES `ventas` (`id_venta`),
  ADD CONSTRAINT `movimientos_caja_ibfk_1` FOREIGN KEY (`id_caja`) REFERENCES `cajas` (`id_caja`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `notificaciones`
--
ALTER TABLE `notificaciones`
  ADD CONSTRAINT `notificaciones_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `notificaciones_ibfk_2` FOREIGN KEY (`id_usuario_destino`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `pagos_deudas`
--
ALTER TABLE `pagos_deudas`
  ADD CONSTRAINT `pagos_deudas_ibfk_1` FOREIGN KEY (`id_deuda`) REFERENCES `deudas_proveedores` (`id_deuda`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `pagos_reservas`
--
ALTER TABLE `pagos_reservas`
  ADD CONSTRAINT `pagos_reservas_ibfk_1` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `pagos_reservas_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `paquetes_servicios`
--
ALTER TABLE `paquetes_servicios`
  ADD CONSTRAINT `paquetes_servicios_ibfk_1` FOREIGN KEY (`id_paquete`) REFERENCES `paquetes_turisticos` (`id_paquete`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `paquetes_servicios_ibfk_2` FOREIGN KEY (`id_servicio`) REFERENCES `servicios_turisticos` (`id_servicio`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `paquetes_turisticos`
--
ALTER TABLE `paquetes_turisticos`
  ADD CONSTRAINT `fk_paquetes_turisticos_sucursal` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `paquetes_turisticos_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `permisos`
--
ALTER TABLE `permisos`
  ADD CONSTRAINT `permisos_ibfk_1` FOREIGN KEY (`id_modulo`) REFERENCES `modulos` (`id_modulo`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `personal`
--
ALTER TABLE `personal`
  ADD CONSTRAINT `personal_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `personal_ibfk_2` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `plantillas_email`
--
ALTER TABLE `plantillas_email`
  ADD CONSTRAINT `plantillas_email_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `politicas_cancelacion`
--
ALTER TABLE `politicas_cancelacion`
  ADD CONSTRAINT `politicas_cancelacion_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `promociones`
--
ALTER TABLE `promociones`
  ADD CONSTRAINT `promociones_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `promociones_ibfk_2` FOREIGN KEY (`id_servicio_aplicable`) REFERENCES `servicios_turisticos` (`id_servicio`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `promociones_ibfk_3` FOREIGN KEY (`id_paquete_aplicable`) REFERENCES `paquetes_turisticos` (`id_paquete`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `proveedores`
--
ALTER TABLE `proveedores`
  ADD CONSTRAINT `proveedores_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `reportes_generados`
--
ALTER TABLE `reportes_generados`
  ADD CONSTRAINT `reportes_generados_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `reportes_generados_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `reservas`
--
ALTER TABLE `reservas`
  ADD CONSTRAINT `fk_reserva_promocion` FOREIGN KEY (`id_promocion`) REFERENCES `promociones` (`id_promocion`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_ibfk_2` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`) ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_ibfk_3` FOREIGN KEY (`id_servicio`) REFERENCES `servicios_turisticos` (`id_servicio`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_ibfk_4` FOREIGN KEY (`id_paquete`) REFERENCES `paquetes_turisticos` (`id_paquete`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_ibfk_5` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_ibfk_6` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `reservas_espera`
--
ALTER TABLE `reservas_espera`
  ADD CONSTRAINT `reservas_espera_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_espera_ibfk_2` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `reservas_espera_ibfk_3` FOREIGN KEY (`id_servicio`) REFERENCES `servicios_turisticos` (`id_servicio`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `reserva_items`
--
ALTER TABLE `reserva_items`
  ADD CONSTRAINT `fk_reserva_items_paquete` FOREIGN KEY (`id_paquete`) REFERENCES `paquetes_turisticos` (`id_paquete`),
  ADD CONSTRAINT `fk_reserva_items_reserva` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`),
  ADD CONSTRAINT `fk_reserva_items_servicio` FOREIGN KEY (`id_servicio`) REFERENCES `servicios_turisticos` (`id_servicio`);

--
-- Filtros para la tabla `roles_permisos`
--
ALTER TABLE `roles_permisos`
  ADD CONSTRAINT `roles_permisos_ibfk_1` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `roles_permisos_ibfk_2` FOREIGN KEY (`id_permiso`) REFERENCES `permisos` (`id_permiso`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `servicios_turisticos`
--
ALTER TABLE `servicios_turisticos`
  ADD CONSTRAINT `fk_servicio_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categorias_servicios` (`id_categoria`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_servicios_turisticos_sucursal` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `servicios_turisticos_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `servicios_turisticos_ibfk_2` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `sesiones_usuario`
--
ALTER TABLE `sesiones_usuario`
  ADD CONSTRAINT `sesiones_usuario_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `sucursales`
--
ALTER TABLE `sucursales`
  ADD CONSTRAINT `sucursales_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `tipos_gasto`
--
ALTER TABLE `tipos_gasto`
  ADD CONSTRAINT `tipos_gasto_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD CONSTRAINT `usuarios_ibfk_2` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `usuarios_ibfk_3` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`) ON UPDATE CASCADE,
  ADD CONSTRAINT `usuarios_ibfk_4` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `ventas`
--
ALTER TABLE `ventas`
  ADD CONSTRAINT `fk_ventas_sucursal` FOREIGN KEY (`id_sucursal`) REFERENCES `sucursales` (`id_sucursal`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `ventas_ibfk_1` FOREIGN KEY (`id_empresa`) REFERENCES `empresas` (`id_empresa`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `ventas_ibfk_2` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `ventas_ibfk_3` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `ventas_ibfk_4` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON UPDATE CASCADE,
  ADD CONSTRAINT `ventas_ibfk_5` FOREIGN KEY (`id_caja`) REFERENCES `cajas` (`id_caja`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `vouchers`
--
ALTER TABLE `vouchers`
  ADD CONSTRAINT `vouchers_ibfk_1` FOREIGN KEY (`id_reserva`) REFERENCES `reservas` (`id_reserva`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
