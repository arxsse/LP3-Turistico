# Endpoints API - Sistema Turístico

Lista completa de endpoints para importar a Postman. Todos los endpoints requieren autenticación JWT en el header `Authorization: Bearer {token}`.

## Autenticación

### POST /auth/login
- **Descripción**: Iniciar sesión
- **Body**: `{"email": "string", "password": "string"}`

### POST /auth/logout
- **Descripción**: Cerrar sesión
- **Headers**: Authorization

### POST /auth/register
- **Descripción**: Registrar nuevo usuario
- **Body**: UsuarioRequest

## Clientes

### GET /clientes
- **Descripción**: Listar clientes con filtros opcionales
- **Query Params**: empresaId, busqueda, page, size

### POST /clientes
- **Descripción**: Crear cliente
- **Body**: Cliente

### GET /clientes/{id}
- **Descripción**: Obtener cliente por ID

### PUT /clientes/{id}
- **Descripción**: Actualizar cliente
- **Body**: Cliente

### DELETE /clientes/{id}
- **Descripción**: Eliminar cliente (soft delete)

## Reservas

### GET /reservas
- **Descripción**: Listar reservas con filtros
- **Query Params**: empresaId, sucursalId, clienteId, estado, fechaInicio, fechaFin, page, size

### POST /reservas
- **Descripción**: Crear reserva
- **Body**: ReservaRequest

### GET /reservas/{id}
- **Descripción**: Obtener reserva por ID

### PUT /reservas/{id}
- **Descripción**: Actualizar reserva
- **Body**: ReservaEditRequest

### DELETE /reservas/{id}
- **Descripción**: Cancelar reserva

### GET /reservas/{id}/items
- **Descripción**: Obtener items de reserva

### POST /reservas/{id}/items
- **Descripción**: Agregar item a reserva
- **Body**: ReservaItemRequest

### PUT /reservas/{id}/items/{itemId}
- **Descripción**: Actualizar item de reserva
- **Body**: ReservaItemRequest

### DELETE /reservas/{id}/items/{itemId}
- **Descripción**: Eliminar item de reserva

### GET /reservas/proximas
- **Descripción**: Obtener reservas próximas
- **Query Params**: empresaId

## Pagos de Reservas

### GET /pagos-reservas
- **Descripción**: Listar pagos de reservas
- **Query Params**: empresaId, sucursalId, metodoPago, fechaInicio, fechaFin

### POST /pagos-reservas
- **Descripción**: Registrar pago de reserva
- **Body**: PagoReservaRequest

### GET /pagos-reservas/{id}
- **Descripción**: Obtener pago por ID

### PUT /pagos-reservas/{id}
- **Descripción**: Actualizar pago
- **Body**: PagoReservaRequest

### DELETE /pagos-reservas/{id}
- **Descripción**: Anular pago

## Ventas

### GET /ventas
- **Descripción**: Listar ventas
- **Query Params**: empresaId, sucursalId, fechaInicio, fechaFin, metodoPago, estado

### GET /ventas/{id}
- **Descripción**: Obtener venta por ID

### PUT /ventas/{id}
- **Descripción**: Actualizar venta
- **Body**: VentaRequest

### DELETE /ventas/{id}
- **Descripción**: Anular venta
- **Query Params**: cajaId, motivo

### POST /ventas/{id}/anular
- **Descripción**: Anular venta
- **Body**: VentaAnulacionRequest

### GET /ventas/proxima-numeracion
- **Descripción**: Obtener próxima numeración
- **Query Params**: empresaId

## Servicios Turísticos

### GET /servicios
- **Descripción**: Listar servicios turísticos
- **Query Params**: empresaId, sucursalId, tipoServicio, busqueda, page, size

### POST /servicios
- **Descripción**: Crear servicio turístico
- **Body**: ServicioTuristico

### GET /servicios/{id}
- **Descripción**: Obtener servicio por ID

### PUT /servicios/{id}
- **Descripción**: Actualizar servicio
- **Body**: ServicioTuristico

### DELETE /servicios/{id}
- **Descripción**: Eliminar servicio

### GET /servicios/disponibles
- **Descripción**: Obtener servicios disponibles
- **Query Params**: empresaId, sucursalId, personasRequeridas

## Paquetes Turísticos

### GET /paquetes
- **Descripción**: Listar paquetes turísticos
- **Query Params**: empresaId, sucursalId, busqueda, page, size

### POST /paquetes
- **Descripción**: Crear paquete turístico
- **Body**: PaqueteTuristico

### GET /paquetes/{id}
- **Descripción**: Obtener paquete por ID

### PUT /paquetes/{id}
- **Descripción**: Actualizar paquete
- **Body**: PaqueteTuristico

### DELETE /paquetes/{id}
- **Descripción**: Eliminar paquete

### GET /paquetes/disponibles
- **Descripción**: Obtener paquetes disponibles
- **Query Params**: empresaId, sucursalId

## Personal

### GET /personal
- **Descripción**: Listar personal
- **Query Params**: empresaId, sucursalId, cargo, estado, busqueda, page, size

### POST /personal
- **Descripción**: Crear personal
- **Body**: Personal

### GET /personal/{id}
- **Descripción**: Obtener personal por ID

### PUT /personal/{id}
- **Descripción**: Actualizar personal
- **Body**: Personal

### DELETE /personal/{id}
- **Descripción**: Eliminar personal

### GET /personal/cargo/{cargo}
- **Descripción**: Obtener personal por cargo
- **Query Params**: empresaId, sucursalId

### GET /personal/activo/count
- **Descripción**: Contar personal activo
- **Query Params**: empresaId, sucursalId

## Asignaciones de Personal

### GET /asignaciones-personal
- **Descripción**: Listar asignaciones de personal
- **Query Params**: empresaId, sucursalId, personalId, reservaId, fecha, estado

### POST /asignaciones-personal
- **Descripción**: Crear asignación de personal
- **Body**: AsignacionPersonalRequest

### GET /asignaciones-personal/{id}
- **Descripción**: Obtener asignación por ID

### PUT /asignaciones-personal/{id}
- **Descripción**: Actualizar asignación
- **Body**: AsignacionPersonalUpdateRequest

### DELETE /asignaciones-personal/{id}
- **Descripción**: Eliminar asignación

## Cajas

### GET /cajas
- **Descripción**: Listar cajas
- **Query Params**: empresaId, sucursalId, estado

### POST /cajas/apertura
- **Descripción**: Abrir caja
- **Body**: CajaAperturaRequest

### POST /cajas/{id}/cierre
- **Descripción**: Cerrar caja
- **Body**: CajaCierreRequest

### GET /cajas/abiertas
- **Descripción**: Obtener cajas abiertas
- **Query Params**: empresaId, sucursalId

### POST /cajas/movimientos
- **Descripción**: Registrar movimiento de caja
- **Body**: MovimientoCajaRequest

## Usuarios

### GET /usuarios
- **Descripción**: Listar usuarios
- **Query Params**: busqueda, estado, empresaId, rolId

### POST /usuarios
- **Descripción**: Crear usuario
- **Body**: UsuarioRequest

### GET /usuarios/{id}
- **Descripción**: Obtener usuario por ID

### PUT /usuarios/{id}
- **Descripción**: Actualizar usuario
- **Body**: UsuarioRequest

### DELETE /usuarios/{id}
- **Descripción**: Eliminar usuario

## Roles

### GET /roles
- **Descripción**: Listar roles

### POST /roles
- **Descripción**: Crear rol
- **Body**: RolRequest

### GET /roles/{id}
- **Descripción**: Obtener rol por ID

### PUT /roles/{id}
- **Descripción**: Actualizar rol
- **Body**: RolRequest

### DELETE /roles/{id}
- **Descripción**: Eliminar rol

## Empresas

### GET /empresas
- **Descripción**: Listar empresas

### POST /empresas
- **Descripción**: Crear empresa
- **Body**: EmpresaRequest

### GET /empresas/{id}
- **Descripción**: Obtener empresa por ID

### PUT /empresas/{id}
- **Descripción**: Actualizar empresa
- **Body**: EmpresaRequest

### DELETE /empresas/{id}
- **Descripción**: Eliminar empresa

## Sucursales

### GET /sucursales
- **Descripción**: Listar sucursales
- **Query Params**: empresaId

### POST /sucursales
- **Descripción**: Crear sucursal
- **Body**: SucursalRequest

### GET /sucursales/{id}
- **Descripción**: Obtener sucursal por ID

### PUT /sucursales/{id}
- **Descripción**: Actualizar sucursal
- **Body**: SucursalRequest

### DELETE /sucursales/{id}
- **Descripción**: Eliminar sucursal

## Vouchers

### GET /vouchers/{codigo}
- **Descripción**: Obtener voucher por código QR

### PUT /vouchers/{codigo}/usar
- **Descripción**: Usar voucher

## Evaluaciones de Servicio

### GET /evaluaciones
- **Descripción**: Listar evaluaciones
- **Query Params**: empresaId, sucursalId

### POST /evaluaciones
- **Descripción**: Crear evaluación
- **Body**: EvaluacionServicio

### GET /evaluaciones/{id}
- **Descripción**: Obtener evaluación por ID

## Reportes

### GET /reportes/reservas
- **Descripción**: Reporte de reservas
- **Query Params**: empresaId, fechaInicio, fechaFin, estado

### GET /reportes/ventas
- **Descripción**: Reporte de ventas
- **Query Params**: empresaId, fechaInicio, fechaFin

### GET /reportes/clientes
- **Descripción**: Reporte de clientes
- **Query Params**: empresaId

### GET /reportes/personal
- **Descripción**: Reporte de personal
- **Query Params**: empresaId

### GET /reportes/servicios
- **Descripción**: Reporte de servicios
- **Query Params**: empresaId

### GET /reportes/paquetes
- **Descripción**: Reporte de paquetes
- **Query Params**: empresaId

### GET /reportes/evaluaciones
- **Descripción**: Reporte de evaluaciones
- **Query Params**: empresaId, fechaInicio, fechaFin

## Reportes Financieros

### GET /reportes-financieros/caja-diario
- **Descripción**: Resumen diario de caja
- **Query Params**: empresaId, fecha

### GET /reportes-financieros/ventas-impuestos
- **Descripción**: Reporte de ventas con impuestos
- **Query Params**: empresaId, fechaInicio, fechaFin, porcentajeImpuesto

## Health Check

### GET /health
- **Descripción**: Verificar estado del sistema

## Admin

### GET /admin/stats
- **Descripción**: Estadísticas del sistema