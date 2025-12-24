# Multi-tenant Hardening Roadmap

> Objetivo: Asegurar que todos los endpoints ejecuten la lógica dentro del tenant correspondiente (empresa) usando el `empresaId` del JWT, sin depender de parámetros manipulables desde el cliente.

## 1. Contexto de autenticación
- [x] **Custom principal**: crear `AuthenticatedUser` (userId, email, empresaId, roles).
- [x] **JwtAuthenticationFilter**: después de validar el token, obtener `empresaId`, `userId` y construir el `UsernamePasswordAuthenticationToken` con el nuevo principal.
- [x] **Helper**: exponer un bean `TenantContext` (o utilidad estática) que lea el principal actual y entregue `getEmpresaActual()` y `getUsuarioActual()`.

## 2. Endpoints prioritarios (CRUD núcleo)
Enfocar primero en entidades que se usan en casi todas las vistas.
- [x] `UsuarioController` / `UsuarioCrudService`
  - [x] `create/update`: ignorar `empresaId` entrante salvo rol `SUPERADMINISTRADOR`; setear con la empresa del contexto.
  - [x] `list/get/delete`: filtrar por `empresaActual` y validar pertenencia del recurso.
- [x] `SucursalController` / `SucursalService`
  - [x] `create/update`: imponer empresa del contexto salvo superadmin.
  - [x] `list/get/delete`: filtrar y validar pertenencia.
- [x] `ServicioTuristicoController` / `ServicioTuristicoService`
  - [x] Reemplazar extracción manual de JWT con `TenantContext`.
  - [x] Normalizar entradas y validar pertenencia en cada consulta/mutación.
  - [x] Evitar soft deletes repetidos y paginar de forma segura.
- [x] `PaqueteTuristicoController` / `PaqueteTuristicoService`
  - [x] Sustituir extracción manual de JWT por `TenantContext`.
  - [x] Validar pertenencia de recursos (empresa actual vs. IDs entrantes).
  - [x] Reutilizar helpers multi-tenant y respuestas consistentes (`PaqueteResponse`).
- [x] `ReservaController` / `ReservaService`
- [x] `VentaController` / `VentaService`
- [x] `ClienteController` / `ClienteService`
  - [x] Eliminar dependencias de `JwtUtil` y resolver empresa desde `TenantContext`.
  - [x] Normalizar entradas, validar unicidad dentro del tenant y proteger soft deletes.
- [x] `CajaController` / `CajaService`
  - [x] Resolver empresa, sucursal y usuario desde `TenantContext` (override solo superadmin).
  - [x] Validar pertenencia en aperturas/cierres/movimientos y normalizar payloads.
- [x] `PagoReservaController` / `PagoReservaService`
  - [x] Resolver usuario/caja vía contexto y reforzar validaciones multi-tenant.
  - [x] Normalizar campos de texto y reutilizar reglas financieras con movimientos de caja.
- [x] `ReporteFinancieroController` / `ReporteFinancieroService`
  - [x] Forzar empresa desde contexto (salvo superadmin) y filtrar resultados por tenant.
  - [x] Validar parámetros y manejar errores de entrada de forma consistente.
- [x] `VoucherController` / `VoucherService`
  - [x] Eliminar dependencia de `JwtUtil`, usar `TenantContext` y validar pertenencia de la reserva.
  - [x] Centralizar transiciones de estado y filtros/paginación en un servicio multitenant.

## 3. DTOs y validaciones
- [x] Marcar `empresaId` como opcional/solo lectura para roles normales (quitar `@NotNull`).
- [x] Añadir validaciones "recurso pertenece a la empresa actual" para IDs entrantes (`usuarioId`, `sucursalId`, etc.).
- [x] Permitir que el superadmin envíe empresa explícita solo cuando su rol lo permita.

## 4. Seguridad adicional
- [ ] Añadir `@PreAuthorize` con expresiones que consulten el `TenantContext` cuando sea posible.
- [ ] Extender `JwtUtil` si se necesita cargar claims adicionales (ej. `roles` vs `permisos`).
- [ ] Registrar auditoría simple (empresa/usuario) para detectar accesos cruzados.

## 5. Pruebas
- [ ] Pruebas unitarias de `TenantContext` y del filtro.
- [ ] Tests de integración: autenticar como usuario de Empresa A, intentar leer/escribir recursos de Empresa B y confirmar que falla.
- [ ] Test manual con los scripts PHP: verificar que no es necesario enviar `empresaId` en los payloads y que el backend impone el tenant correcto.

## 6. Despliegue gradual
1. Implementar `TenantContext` + adaptar `UsuarioCrudService` como piloto.
2. Validar con pruebas y scripts PHP.
3. Replicar en los servicios restantes por lotes pequeños.
4. Actualizar documentación/Swagger para reflejar las restricciones de multiempresa.

> Nota: Mantener un branch específico para este refactor y fusionar por etapas para aislar problemas rápidamente.
