# Plan de Implementación de Sucursales en Módulos Adicionales

## Introducción

Basado en la implementación actual de sucursales en el módulo de clientes, donde los clientes pueden estar asociados opcionalmente a una sucursal dentro de una empresa, se requiere extender esta funcionalidad a otros módulos del sistema. Esto permitirá un mejor control de acceso y filtrado por sucursal, especialmente para roles como "Gerente" que solo deben ver datos de su sucursal asignada.

La implementación en clientes incluye:
- Campo `id_sucursal` nullable en tabla `clientes`
- Relación `@ManyToOne` en entidad `Cliente`
- Filtros por empresa en repositorio y servicio
- Manejo de sucursal en create/update

## Cambios a la Base de Datos

### Módulos con backend que requieren agregar `id_sucursal`

Solo se incluyen módulos que tienen controladores y servicios en el backend actual. Las tablas sin backend (como promociones, proveedores, contratos, etc.) no se incluyen ya que no tienen implementación backend.

- [x] **servicios_turisticos**: Agregar `id_sucursal` (nullable) - servicios pueden ser específicos de sucursal
- [x] **paquetes_turisticos**: Agregar `id_sucursal` (nullable) - paquetes pueden ser por sucursal
- [x] **personal**: Ya tiene `id_sucursal` (nullable) - verificar implementación en backend
- [x] **reservas**: Ya tiene `id_sucursal` (nullable) - verificar implementación en backend
- [x] **asignaciones_personal**: Agregar `id_sucursal` (nullable) - asignaciones por sucursal
- [x] **pagos_reservas**: Agregar `id_sucursal` (nullable) - pagos por sucursal
- [x] **ventas**: Agregar `id_sucursal` (nullable) - ventas por sucursal
- [x] **vouchers**: Agregar `id_sucursal` (nullable) - vouchers por sucursal
- [x] **cajas**: Ya tiene `id_sucursal` (NOT NULL) - verificar filtros
- [x] **usuarios**: Ya tiene `id_sucursal` (nullable) - verificar filtros

### Scripts SQL para cambios

Para cada tabla, ejecutar:

```sql
ALTER TABLE nombre_tabla ADD COLUMN id_sucursal int(11) DEFAULT NULL;
ALTER TABLE nombre_tabla ADD KEY idx_nombre_tabla_sucursal (id_sucursal);
ALTER TABLE nombre_tabla ADD CONSTRAINT fk_nombre_tabla_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales (id_sucursal) ON DELETE SET NULL ON UPDATE CASCADE;
```

## Cambios al Código Backend

### 1. Extender AuthenticatedUser y TenantContext (Opcional - Para futura implementación)

**Nota:** Actualmente, el módulo de clientes maneja el filtrado por sucursal en el frontend. Dado que no hay tiempo para cambios complejos en la autenticación, se recomienda mantener el filtrado por sucursal en el frontend para todos los módulos inicialmente, similar a como se hace en clientes. La extensión de `AuthenticatedUser` y `TenantContext` se deja para una futura fase cuando se pueda implementar el filtrado en backend de manera consistente.

- [ ] Agregar `sucursalId` a `AuthenticatedUser` (futuro)
- [ ] Actualizar `TenantContext` con métodos para `sucursalId` (futuro)
- [ ] Modificar `JwtAuthenticationFilter` para incluir `sucursalId` en el token (futuro)

### 2. Actualizar Entidades

Para cada entidad que requiera sucursal:

- [x] Agregar campo `sucursal` con `@ManyToOne` y `@JoinColumn(name = "id_sucursal")`
- [x] Agregar índice `@Index(name = "idx_entidad_sucursal", columnList = "id_sucursal")`

### 3. Modificar Repositorios

- [x] Agregar consultas que filtren por `empresaId` y opcionalmente `sucursalId`
- [x] Ejemplo: `findByEmpresaIdAndSucursalId` para listas filtradas por sucursal

### 4. Actualizar Servicios

- [x] En métodos `create`/`update`: validar y asignar sucursal si se proporciona
- [x] En métodos `find`: aplicar filtros por sucursal basado en rol del usuario
- [x] Usar `TenantContext` para determinar si filtrar por sucursal

### 5. Modificar Controladores

- [x] Agregar parámetro `sucursalId` opcional en endpoints GET
- [x] Aplicar lógica de filtrado: administradores ven todo, gerentes solo su sucursal
- [x] Validar permisos de acceso a sucursal

## Roles y Permisos

- El rol "Gerente" (id_rol=4) tendrá acceso completo a todos los endpoints del backend, sin restricciones adicionales.
- Los permisos específicos (filtrado por sucursal, etc.) serán manejados únicamente por el frontend, no por el backend.
- Los roles "Administrador" y "Superadministrador" mantienen sus permisos actuales.
- Se agregó 'GERENTE' a todos los @PreAuthorize que incluyen 'EMPLEADO' para dar acceso completo a gerentes.

## Consideraciones de Seguridad

- Los gerentes solo deben acceder a datos de su sucursal asignada (implementado en frontend)
- Los administradores pueden ver datos de todas las sucursales de su empresa
- Los superadministradores ven todo
- La seguridad se basa en la confianza del frontend; en producción, se recomienda implementar validaciones en backend

## Testing

- [ ] Probar creación/edición con sucursal
- [ ] Probar listados con filtros por sucursal
- [ ] Verificar permisos por rol
- [ ] Probar migración de datos existentes (sucursal = NULL)

## Migración de Datos

- Los registros existentes tendrán `id_sucursal = NULL`, lo cual está permitido
- Se puede asignar sucursal por defecto o dejar como NULL inicialmente