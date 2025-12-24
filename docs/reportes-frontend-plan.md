# Plan de Implementación Frontend para Reportes

## 1. Panorama del backend existente

- **Autorización**: Todos los endpoints requieren `ROLE_ADMINISTRADOR` o `ROLE_SUPERADMINISTRADOR` vía `@PreAuthorize`.
- **Contexto multi-tenant**: `TenantContext` exige `empresaId` explícito o asignado al usuario autenticado; manejarlo siempre en las solicitudes.
- **Formato de respuesta**: `Map<String, Object>` con claves estándar (`success`, `message`, `data`) + payload específico del reporte.

| Endpoint | Query params relevantes | Campos principales en `data` |
| --- | --- | --- |
| `GET /reportes/reservas` | `empresaId`, `fechaInicio`, `fechaFin`, `estado` | `fechaInicio`, `fechaFin`, `totalReservas`, `reservasPorEstado`, `totalMonto` |
| `GET /reportes/ventas` | `empresaId`, `fechaInicio`, `fechaFin` | `fechaInicio`, `fechaFin`, `totalVentas`, `montoTotal`, `ventasPorMetodoPago` |
| `GET /reportes/clientes` | `empresaId` | `totalClientes`, `clientesActivos`, `clientesPorNacionalidad`, `clientesPorNivelMembresia` |
| `GET /reportes/personal` | `empresaId` | `totalPersonal`, `personalActivo`, `personalPorCargo`, `personalPorTurno` |
| `GET /reportes/servicios` | `empresaId` | `totalServicios`, `serviciosActivos`, `serviciosPorTipo`, `serviciosPorCategoria` |
| `GET /reportes/paquetes` | `empresaId` | `totalPaquetes`, `paquetesActivos`, `paquetesConPromocion`, `precioPromedio` |
| `GET /reportes/finanzas/caja-diaria` | `empresaId`, `fecha` | `fecha`, `totalCajas`, `montoInicial`, `ingresos`, `egresos`, `saldoActual`, `saldoCalculado` |
| `GET /reportes/finanzas/impuestos` | `empresaId`, `fechaInicio`, `fechaFin`, `porcentajeImpuesto` | `fechaInicio`, `fechaFin`, `totalVentas`, `impuestos`, `neto`, `porcentajeImpuesto`, `cantidadVentas` |

_Los endpoints de evaluaciones permanecen activos en backend, pero no se abordarán en la implementación frontend solicitada._

## 2. Objetivos del frontend de reportes

- Consolidar un módulo de reportes con navegación clara por categoría (operativos, comerciales, financieros).
- Exponer métricas clave mediante tarjetas, tablas y visualizaciones.
- Permitir filtros reactivamente (empresa, rango de fechas, estado).
- Habilitar exportación/impresión ligera y reutilización de componentes entre reportes.
- Excluir del alcance las vistas relacionadas con evaluaciones hasta nueva indicación.

## 3. Lineamientos de arquitectura UI

- **Stack actual**: PHP con plantillas y JS modular en `inicio/`. Mantener consistencia (HTML + Fetch API + vanilla JS o mínima dependencia externa).
- **Estructura propuesta**: Carpeta `inicio/REPORTES/` con vistas separadas (`dashboard.php`, parciales por reporte) y un archivo JS común (`reportes.js`).
- **Gestión de estado**: Objeto global `reportesStore` para filtros y resultados; sincronizar con la UI mediante funciones `render*`.
- **Reutilización**: Helpers para formatear montos, porcentajes e iterar mapas (`{ clave: valor }`) devueltos por el backend.

Las vistas cubrirán reservas y ventas con KPIs y gráficos de tendencia; clientes y personal con distribuciones circulares y tablas ordenables; servicios y paquetes con resúmenes de mix de oferta; finanzas con paneles de caja e impuestos. Reportes de evaluaciones quedan fuera de alcance. Todo seguirá el formato multiempresa existente (selector de empresa, filtros y estilos compartidos) para mantener coherencia visual.

## 4. Fases de implementación

- [x] **Fase 1 – Preparación**: Auditar `SecurityConfig`, documentar `$apiBase`, garantizar token JWT y preparar mocks con respuestas reales.
- [x] **Fase 2 – Infraestructura de interfaz**: Crear `inicio/REPORTES/dashboard.php`, definir filtros comunes, configurar navegación/tabuladores y registrar recursos CSS/JS alineados a módulos actuales.
- [x] **Fase 3 – Integración por reportes**: Implementar llamados y renders para reservas, ventas, clientes, personal, servicios, paquetes y finanzas; centralizar helpers `buildQueryParams`, `fetchReporte`, `renderReporteX`, gestionar estados de carga y errores.
- [x] **Fase 4 – Visualizaciones y utilitarios**: Incorporar librería ligera (Chart.js/ApexCharts o canvas propio), crear `utils/formato.js` y consolidar paleta/reutilización de componentes.
- [x] **Fase 5 – Accesibilidad y exportación**: Validar contraste, etiquetas y accesibilidad, añadir exportación ligera (CSV/PNG) con APIs del navegador.
- [x] **Fase 6 – QA y despliegue**: Probar escenarios multi-empresa por rol, validar respuestas de error 4xx/5xx, documentar casos y actualizar guías internas.

## 5. Contratos de datos y validaciones

- Todos los montos llegan como `BigDecimal`; formatearlos a moneda (`S/` o moneda configurable).
- Campos `Map<String, Long>` (ej. `reservasPorEstado`) requieren conversión a listas para gráficas.
- Fechas ISO (`yyyy-MM-dd`) listas para uso con `<input type="date">` o `Date` en JS.
- Ante ausencia de datos, el backend devuelve mapas vacíos y cero; la UI debe mostrar estados "Sin datos".

## 6. Seguridad y multi-tenant

- Encabezado `Authorization: Bearer <token>` obligatorio en Fetch.
- Siempre enviar `empresaId` cuando el superadministrador elija otra empresa; para admin delegar al backend (no enviar parámetro).
- Evitar exponer IDs internos en la vista; mapearlos a nombres legibles cuando sea posible.

## 7. Plan de pruebas

- **Smoke**: Carga de dashboard con filtros por defecto.
- **Funcionales**: Cambiar filtros en cada reporte, verificar números coherentes contra datos conocidos.
- **Errores simulados**: Forzar 401/403 (expirar token), 500 (desconectar backend), 400 (fecha inválida) y confirmar mensajes.
- **Rendimiento**: Validar que múltiples requests consecutivos se cancelan o gestionan (usar controlador de abortos en Fetch si se requiere).

## 8. Entregables

- Vistas PHP + JS en `inicio/REPORTES/`.
- Utilidades compartidas (`utils/` o `js/` comunes) para formato y Fetch.
- Pruebas manuales documentadas.
- Actualización de `docs/` con guía de uso para usuarios finales si aplica.

## 9. Próximos pasos inmediatos

1. Ejecutar el checklist de QA (`docs/reportes-qa-checklist.md`) y adjuntar evidencias.
2. Recopilar feedback de usuarios finales para priorizar mejoras o nuevos reportes.
3. Preparar guía rápida para usuarios finales con explicación de KPIs y filtros disponibles.

## 10. Cobertura del análisis backend

- Se inspeccionaron `ReportesController` y `ReportesService`, registrando parámetros, filtros y agregaciones para reservas, ventas, clientes, personal, servicios y paquetes.
- Se validaron `ReporteFinancieroController` y `ReporteFinancieroService`, incluidos cálculos de caja diaria e impuestos y sus dependencias (`CajaRepository`, `MovimientoCajaRepository`, `VentaRepository`).
- No existen controladores o servicios adicionales vinculados a reportes en la base de código; el plan se ajusta a los contratos de datos identificados.

## 11. Datos de prueba (inserts y limpieza)

```sql
-- Inserts de prueba (empresa 6) para validar KPIs de reservas, ventas y finanzas
INSERT INTO reservas (id_empresa, id_cliente, id_usuario, codigo_reserva, fecha_reserva, fecha_servicio, numero_personas, precio_total, estado, observaciones)
VALUES (6, 20, 18, 'FRONT-REP-001', CURRENT_DATE - INTERVAL 10 DAY, CURRENT_DATE + INTERVAL 5 DAY, 4, 1400.00, 'Confirmada', 'Escapada corporativa demo');

SET @reserva_id := LAST_INSERT_ID();

INSERT INTO reserva_items (id_reserva, tipo_item, id_servicio, cantidad, precio_unitario, precio_total, descripcion_extra)
VALUES (@reserva_id, 'SERVICIO', 3, 4, 350.00, 1400.00, 'Tour privado para reporte');

INSERT INTO sucursales (id_empresa, nombre_sucursal, ubicacion, estado)
VALUES (6, 'Sucursal Reportes QA', 'Demo multiempresa', 1);

SET @sucursal_id := LAST_INSERT_ID();

INSERT INTO cajas (id_empresa, id_sucursal, id_usuario_apertura, fecha_apertura, hora_apertura, monto_inicial, saldo_actual, estado, observaciones)
VALUES (6, @sucursal_id, 18, CURRENT_DATE - INTERVAL 1 DAY, CURRENT_TIME, 200.00, 200.00, 'Abierta', 'Caja creada para pruebas de reportes');

SET @caja_id := LAST_INSERT_ID();

INSERT INTO ventas (id_empresa, id_cliente, id_reserva, id_usuario, id_caja, fecha_hora, monto_total, metodo_pago, observaciones)
VALUES (6, 20, @reserva_id, 18, @caja_id, NOW(), 1400.00, 'Tarjeta', 'Venta ligada a pruebas de reportes');

INSERT INTO movimientos_caja (id_caja, tipo_movimiento, monto, descripcion, fecha_hora)
VALUES (@caja_id, 'Ingreso', 1400.00, 'Ingreso por FRONT-REP-001', NOW());

-- Limpieza de datos de prueba
DELETE FROM movimientos_caja WHERE descripcion = 'Ingreso por FRONT-REP-001';
DELETE FROM ventas WHERE observaciones = 'Venta ligada a pruebas de reportes';
DELETE FROM reserva_items WHERE descripcion_extra = 'Tour privado para reporte';
DELETE FROM reservas WHERE codigo_reserva = 'FRONT-REP-001';
DELETE FROM cajas WHERE observaciones = 'Caja creada para pruebas de reportes';
DELETE FROM sucursales WHERE nombre_sucursal = 'Sucursal Reportes QA' AND id_empresa = 6;
```

```sql
-- Ventas adicionales para probar la rotación de comprobantes en el dashboard (empresa 6)
-- Asegúrate de reutilizar la reserva y la caja generadas en el bloque anterior.
SET @empresa_id := 6;
SET @cliente_id := 20;
SET @usuario_id := 18;
SET @reserva_id := (SELECT id_reserva FROM reservas WHERE codigo_reserva = 'FRONT-REP-001' ORDER BY id_reserva DESC LIMIT 1);
SET @caja_id := (SELECT id_caja FROM cajas WHERE observaciones = 'Caja creada para pruebas de reportes' ORDER BY id_caja DESC LIMIT 1);

-- Verifica que las variables no queden nulas antes de continuar
SELECT @reserva_id AS reserva_en_uso, @caja_id AS caja_en_uso;

-- Venta 1: Boleta de hace 2 días
INSERT INTO ventas (
	id_empresa, id_cliente, id_reserva, id_usuario, id_caja,
	fecha_hora, monto_total, metodo_pago, numero_operacion, comprobante, observaciones, estado
) VALUES (
	@empresa_id, @cliente_id, @reserva_id, @usuario_id, @caja_id,
	NOW() - INTERVAL 2 DAY, 980.00, 'Efectivo', 'BOL-TEST-1001', 'Boleta', 'Venta demo 1', 1
);

-- Venta 2: Factura de ayer
INSERT INTO ventas (
	id_empresa, id_cliente, id_reserva, id_usuario, id_caja,
	fecha_hora, monto_total, metodo_pago, numero_operacion, comprobante, observaciones, estado
) VALUES (
	@empresa_id, @cliente_id, @reserva_id, @usuario_id, @caja_id,
	NOW() - INTERVAL 1 DAY, 1450.50, 'Tarjeta Credito', 'FAC-TEST-2001', 'Factura', 'Venta demo 2', 1
);

-- Venta 3: Nota de hoy temprano
INSERT INTO ventas (
	id_empresa, id_cliente, id_reserva, id_usuario, id_caja,
	fecha_hora, monto_total, metodo_pago, numero_operacion, comprobante, observaciones, estado
) VALUES (
	@empresa_id, @cliente_id, @reserva_id, @usuario_id, @caja_id,
	NOW() - INTERVAL 3 HOUR, 620.75, 'Transferencia', 'NOT-TEST-3001', 'Nota de venta', 'Venta demo 3', 1
);

-- Venta 4: Registro más reciente (debe quedar primero en el dashboard)
INSERT INTO ventas (
	id_empresa, id_cliente, id_reserva, id_usuario, id_caja,
	fecha_hora, monto_total, metodo_pago, numero_operacion, comprobante, observaciones, estado
) VALUES (
	@empresa_id, @cliente_id, @reserva_id, @usuario_id, @caja_id,
	NOW(), 1875.90, 'Yape/Plin', 'BOL-TEST-4001', 'Boleta', 'Venta demo 4', 1
);

DELETE FROM ventas
WHERE numero_operacion IN ('BOL-TEST-1001', 'FAC-TEST-2001', 'NOT-TEST-3001', 'BOL-TEST-4001');

```sql
-- Reservas y ventas para la empresa 6 utilizando el dump "turistas_sistema DEFINITIVO V3"
SET @empresa_id := 6;
SET @usuario_id := 18; -- Usuario activo de la empresa 6
SET @cliente_corporativo := 20; -- AlexanderRr Pezo
SET @cliente_referido := 15; -- Lucia Campos

-- Crea sucursal y caja de pruebas solo si no existen
INSERT INTO sucursales (id_empresa, nombre_sucursal, ubicacion, estado, created_at)
SELECT @empresa_id, 'Sucursal Reportes QA - Emp6', 'Demo multiempresa', 1, NOW()
WHERE NOT EXISTS (
	SELECT 1 FROM sucursales
	WHERE id_empresa = @empresa_id AND nombre_sucursal = 'Sucursal Reportes QA - Emp6'
);

SET @sucursal_id := (
	SELECT id_sucursal
	FROM sucursales
	WHERE id_empresa = @empresa_id AND nombre_sucursal = 'Sucursal Reportes QA - Emp6'
	ORDER BY id_sucursal DESC
	LIMIT 1
);

INSERT INTO cajas (
	id_empresa, id_sucursal, id_usuario_apertura, fecha_apertura,
	hora_apertura, monto_inicial, saldo_actual, estado, observaciones,
	created_at, updated_at
)
SELECT
	@empresa_id, @sucursal_id, @usuario_id, '2025-11-24', '08:30:00',
	250.00, 250.00, 'Abierta', 'Caja reportes empresa 6', NOW(), NOW()
WHERE NOT EXISTS (
	SELECT 1 FROM cajas
	WHERE id_empresa = @empresa_id AND observaciones = 'Caja reportes empresa 6'
);

SET @caja_id := (
	SELECT id_caja
	FROM cajas
	WHERE id_empresa = @empresa_id AND observaciones = 'Caja reportes empresa 6'
	ORDER BY id_caja DESC
	LIMIT 1
);

SELECT @caja_id AS caja_en_uso;

-- Reserva EMP6-ROT-101: tour corporativo con pagos parciales y propina
INSERT INTO reservas (
	id_empresa, id_cliente, id_servicio, id_usuario,
	codigo_reserva, fecha_reserva, fecha_servicio,
	numero_personas, precio_total, estado, observaciones
)
VALUES (
	@empresa_id, @cliente_corporativo, 3, @usuario_id,
	'EMP6-ROT-101', '2025-11-25', '2025-12-05',
	4, 1400.00, 'Pagada', 'City tour corporativo Tarapoto (empresa 6)'
);

SET @reserva_emp6_a := LAST_INSERT_ID();

INSERT INTO reserva_items (
	id_reserva, tipo_item, id_servicio, cantidad,
	precio_unitario, precio_total, descripcion_extra
)
VALUES (
	@reserva_emp6_a, 'SERVICIO', 3, 4, 350.00, 1400.00, 'Tour personalizado con movilidad privada'
);

INSERT INTO pagos_reservas (
	id_reserva, id_usuario, monto_pagado, metodo_pago,
	numero_operacion, comprobante, fecha_pago, observaciones
)
VALUES
	(@reserva_emp6_a, @usuario_id, 560.00, 'Transferencia', 'EMP6-ROT-101-DEP1', 'Voucher bancario', '2025-11-27', 'Anticipo 40% reserva EMP6-ROT-101'),
	(@reserva_emp6_a, @usuario_id, 840.00, 'Tarjeta Crédito', 'EMP6-ROT-101-DEP2', 'Voucher TPV', '2025-12-02', 'Saldo 60% reserva EMP6-ROT-101');

INSERT INTO movimientos_caja (
	id_caja, id_venta, tipo_movimiento, monto, descripcion, fecha_hora
)
VALUES
	(@caja_id, NULL, 'Ingreso', 560.00, 'Anticipo EMP6-ROT-101 - Transferencia', '2025-11-27 11:15:00'),
	(@caja_id, NULL, 'Ingreso', 840.00, 'Saldo EMP6-ROT-101 - Tarjeta', '2025-12-02 18:40:00');

INSERT INTO ventas (
	id_empresa, id_cliente, id_reserva, id_usuario, id_caja,
	fecha_hora, monto_total, metodo_pago, numero_operacion,
	comprobante, descuento, propina, observaciones, estado
)
VALUES (
	@empresa_id, @cliente_corporativo, @reserva_emp6_a, @usuario_id, @caja_id,
	'2025-12-02 19:05:00', 1450.00, 'Tarjeta Crédito', 'EMP6-ROT-101-REC',
	'Factura', 0.00, 50.00, 'Liquidación final EMP6-ROT-101 (incluye propina)', 1
);

SET @venta_emp6_a := LAST_INSERT_ID();

INSERT INTO movimientos_caja (
	id_caja, id_venta, tipo_movimiento, monto, descripcion, fecha_hora
)
VALUES (
	@caja_id, @venta_emp6_a, 'Ingreso', 50.00, 'Propina EMP6-ROT-101 - Venta', '2025-12-02 19:10:00'
);

-- Reserva EMP6-ROT-102: combo paquete + tour con upgrade gastronómico
INSERT INTO reservas (
	id_empresa, id_cliente, id_servicio, id_paquete, id_usuario,
	codigo_reserva, fecha_reserva, fecha_servicio,
	numero_personas, precio_total, estado, observaciones
)
VALUES (
	@empresa_id, @cliente_referido, 3, 2, @usuario_id,
	'EMP6-ROT-102', '2025-11-26', '2025-12-12',
	2, 1520.00, 'Pagada', 'Paquete selva + upgrade gastronómico (empresa 6)'
);

SET @reserva_emp6_b := LAST_INSERT_ID();

INSERT INTO reserva_items (
	id_reserva, tipo_item, id_servicio, id_paquete, cantidad,
	precio_unitario, precio_total, descripcion_extra
)
VALUES
	(@reserva_emp6_b, 'SERVICIO', 3, NULL, 2, 350.00, 700.00, 'City tour premium con guía bilingüe'),
	(@reserva_emp6_b, 'PAQUETE', NULL, 2, 1, 820.00, 820.00, 'Paquete Escápate con cena de bienvenida');

INSERT INTO pagos_reservas (
	id_reserva, id_usuario, monto_pagado, metodo_pago,
	numero_operacion, comprobante, fecha_pago, observaciones
)
VALUES
	(@reserva_emp6_b, @usuario_id, 1000.00, 'Transferencia', 'EMP6-ROT-102-DEP1', 'Voucher bancario', '2025-12-04', 'Anticipo 65% reserva EMP6-ROT-102'),
	(@reserva_emp6_b, @usuario_id, 520.00, 'Yape/Plin', 'EMP6-ROT-102-DEP2', 'Recibo interno', '2025-12-09', 'Saldo 35% reserva EMP6-ROT-102');

INSERT INTO movimientos_caja (
	id_caja, id_venta, tipo_movimiento, monto, descripcion, fecha_hora
)
VALUES
	(@caja_id, NULL, 'Ingreso', 1000.00, 'Anticipo EMP6-ROT-102 - Transferencia', '2025-12-04 09:20:00'),
	(@caja_id, NULL, 'Ingreso', 520.00, 'Saldo EMP6-ROT-102 - Yape/Plin', '2025-12-09 17:35:00');

INSERT INTO ventas (
	id_empresa, id_cliente, id_reserva, id_usuario, id_caja,
	fecha_hora, monto_total, metodo_pago, numero_operacion,
	comprobante, descuento, propina, observaciones, estado
)
VALUES (
	@empresa_id, @cliente_referido, @reserva_emp6_b, @usuario_id, @caja_id,
	'2025-12-09 18:00:00', 1550.00, 'Yape/Plin', 'EMP6-ROT-102-REC',
	'Boleta', 0.00, 30.00, 'Liquidación final EMP6-ROT-102 (propina incluida)', 1
);

SET @venta_emp6_b := LAST_INSERT_ID();

INSERT INTO movimientos_caja (
	id_caja, id_venta, tipo_movimiento, monto, descripcion, fecha_hora
)
VALUES (
	@caja_id, @venta_emp6_b, 'Ingreso', 30.00, 'Propina EMP6-ROT-102 - Venta', '2025-12-09 18:05:00'
);

-- Limpieza del bloque anterior
SET @reserva_emp6_a := (SELECT id_reserva FROM reservas WHERE codigo_reserva = 'EMP6-ROT-101' LIMIT 1);
SET @reserva_emp6_b := (SELECT id_reserva FROM reservas WHERE codigo_reserva = 'EMP6-ROT-102' LIMIT 1);

DELETE FROM movimientos_caja
WHERE descripcion IN (
	'Anticipo EMP6-ROT-101 - Transferencia',
	'Saldo EMP6-ROT-101 - Tarjeta',
	'Propina EMP6-ROT-101 - Venta',
	'Anticipo EMP6-ROT-102 - Transferencia',
	'Saldo EMP6-ROT-102 - Yape/Plin',
	'Propina EMP6-ROT-102 - Venta'
);

DELETE FROM ventas
WHERE numero_operacion IN ('EMP6-ROT-101-REC', 'EMP6-ROT-102-REC');

DELETE FROM pagos_reservas
WHERE numero_operacion IN (
	'EMP6-ROT-101-DEP1',
	'EMP6-ROT-101-DEP2',
	'EMP6-ROT-102-DEP1',
	'EMP6-ROT-102-DEP2'
);

DELETE FROM reserva_items
WHERE id_reserva IN (@reserva_emp6_a, @reserva_emp6_b);

DELETE FROM reservas
WHERE id_reserva IN (@reserva_emp6_a, @reserva_emp6_b);

-- Opcional: eliminar la caja/sucursal si solo se usan para pruebas
-- DELETE FROM cajas WHERE observaciones = 'Caja reportes empresa 6' AND id_empresa = @empresa_id;
-- DELETE FROM sucursales WHERE nombre_sucursal = 'Sucursal Reportes QA - Emp6' AND id_empresa = @empresa_id;
```
```
