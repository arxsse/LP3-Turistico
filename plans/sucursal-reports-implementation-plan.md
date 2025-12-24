# Plan de Implementación: Filtrado por Sucursales en Reportes

## Contexto
Implementar filtrado por sucursales en el módulo de reportes siguiendo el patrón híbrido:
- **Backend**: Modificar para devolver datos crudos + agregados
- **Frontend**: Filtrar por sucursal y recalcular agregados cuando el usuario es gerente

**Consideración importante**: Las reservas/ventas creadas por administradores tienen `sucursal = NULL`, mientras que las de gerentes tienen `sucursal.idSucursal` asignado. El filtrado debe incluir ambos casos.

## Arquitectura General

### Backend Changes
- Modificar `ReportesService` para devolver listas crudas junto con agregados
- Mantener compatibilidad con llamadas existentes (sin `idSucursal`)
- Usar queries existentes que ya soportan filtrado por sucursal

### Frontend Changes
- Detectar rol de gerente y obtener `idSucursal`
- Filtrar datos crudos por sucursal cuando aplique
- Recalcular agregados después del filtrado

## Checklist de Implementación

### 1. Backend - Modificaciones en ReportesService

#### 1.1 Modificar reporteReservas()
- [x] Agregar parámetro `Long idSucursal` al método
- [x] Modificar lógica para filtrar por sucursal cuando se proporciona
- [x] Agregar campo `reservasRaw` con lista completa de reservas filtradas
- [x] Mantener campos agregados existentes (`totalReservas`, `reservasPorEstado`, etc.)

#### 1.2 Modificar reporteVentas()
- [x] Agregar parámetro `Long idSucursal`
- [x] Usar `ventaRepository.findByFiltros()` con `sucursalId`
- [x] Agregar campo `ventasRaw` con lista completa de ventas filtradas
- [x] Mantener campos agregados existentes

#### 1.3 Modificar reporteClientes()
- [x] Agregar parámetro `Long idSucursal`
- [x] Agregar método `findByEmpresaIdAndSucursalId` en `ClienteRepository`
- [x] Agregar campo `clientesRaw` con lista completa de clientes filtrados
- [x] Mantener campos agregados existentes

#### 1.4 Modificar reportePersonal()
- [x] Agregar parámetro `Long idSucursal`
- [x] Usar `personalRepository.findWithFilters()` con `sucursalId`
- [x] Agregar campo `personalRaw` con lista completa de personal filtrado
- [x] Mantener campos agregados existentes

#### 1.5 Modificar reporteServicios()
- [x] Agregar parámetro `Long idSucursal`
- [x] Usar `servicioTuristicoRepository.findByEmpresaIdAndSucursalId()`
- [x] Agregar campo `serviciosRaw` con lista completa de servicios filtrados
- [x] Mantener campos agregados existentes

#### 1.6 Modificar reportePaquetes()
- [x] Agregar parámetro `Long idSucursal`
- [x] Usar `paqueteTuristicoRepository.findByEmpresaIdAndSucursalId()`
- [x] Agregar campo `paquetesRaw` con lista completa de paquetes filtrados
- [x] Mantener campos agregados existentes

### 2. Backend - Modificaciones en ClienteRepository

#### 2.1 Agregar método de filtrado por sucursal
- [x] Implementar `findByEmpresaIdAndSucursalId()` con query que incluya `sucursal IS NULL OR sucursal.idSucursal = :sucursalId` (Ya existía en ClienteRepository)

### 3. Frontend - Modificaciones en dashboard.php

#### 3.1 Detectar rol de gerente
- [x] Agregar lógica para detectar si usuario es gerente (similar a `reservas.php`)
- [x] Extraer `idSucursal` de parámetros GET cuando es gerente
- [x] Pasar `esGerente` e `idSucursal` a JavaScript

#### 3.2 Modificar reportes.js

##### 3.2.1 Estado y configuración
- [x] Agregar `idSucursal` y `esGerente` al objeto `state`
- [x] Modificar `determineEmpresa()` para detectar rol y sucursal

##### 3.2.2 Funciones de carga de reportes
- [x] Modificar `cargarReporteReservas()` para filtrar y recalcular si es gerente
- [x] Modificar `cargarReporteVentas()` para filtrar y recalcular si es gerente
- [x] Modificar `cargarReporteClientes()` para filtrar y recalcular si es gerente
- [x] Modificar `cargarReportePersonal()` para filtrar y recalcular si es gerente
- [x] Modificar `cargarReporteServicios()` para filtrar y recalcular si es gerente
- [x] Modificar `cargarReportePaquetes()` para filtrar y recalcular si es gerente

##### 3.2.3 Funciones de filtrado y recálculo
- [x] Implementar `filtrarReservasPorSucursal(reservasRaw, sucursalId)`
- [x] Implementar `filtrarVentasPorSucursal(ventasRaw, sucursalId)`
- [x] Implementar `filtrarClientesPorSucursal(clientesRaw, sucursalId)`
- [x] Implementar `filtrarPersonalPorSucursal(personalRaw, sucursalId)`
- [x] Implementar `filtrarServiciosPorSucursal(serviciosRaw, sucursalId)`
- [x] Implementar `filtrarPaquetesPorSucursal(paquetesRaw, sucursalId)`

##### 3.2.4 Funciones de recálculo de agregados
- [x] Implementar `recalcularAgregadosReservas(reservasFiltradas)`
- [x] Implementar `recalcularAgregadosVentas(ventasFiltradas)`
- [x] Implementar `recalcularAgregadosClientes(clientesFiltrados)`
- [x] Implementar `recalcularAgregadosPersonal(personalFiltrado)`
- [x] Implementar `recalcularAgregadosServicios(serviciosFiltrados)`
- [x] Implementar `recalcularAgregadosPaquetes(paquetesFiltrados)`

#### 3.3 Modificar dashboard.php
- [x] Agregar detección de rol gerente en PHP
- [x] Extraer `idSucursal` de parámetros GET cuando es gerente
- [x] Pasar `esGerente` e `idSucursal` a JavaScript via `REPORTES_CONFIG`

### 4. Testing y Validación

#### 4.1 Casos de prueba
- [ ] Verificar que administradores ven todos los datos de la empresa
- [ ] Verificar que gerentes ven solo datos de su sucursal + datos con sucursal NULL
- [ ] Verificar que superadmin puede ver todas las empresas
- [ ] Verificar cálculos de agregados después del filtrado

#### 4.2 Validaciones de seguridad
- [ ] Asegurar que gerentes no puedan ver datos de otras sucursales
- [ ] Validar que `idSucursal` pertenece a la empresa del usuario
- [ ] Verificar manejo de errores cuando `idSucursal` es inválido

### 5. Documentación

#### 5.1 Actualizar documentación
- [ ] Documentar nuevos campos en respuestas de API (`*Raw`)
- [ ] Actualizar ejemplos de uso para gerentes
- [ ] Documentar lógica de filtrado (sucursal NULL vs asignada)

## Validación de Base de Datos

### Campos de Sucursal en Tablas
- ✅ **reservas**: `id_sucursal` (nullable) - EXISTE
- ✅ **ventas**: `id_sucursal` (nullable) - EXISTE
- ✅ **clientes**: `id_sucursal` (NOT NULL en BD, nullable en entity) - EXISTE
- ✅ **personal**: `id_sucursal` (nullable) - EXISTE
- ✅ **servicios_turisticos**: `id_sucursal` (nullable) - EXISTE
- ✅ **paquetes_turisticos**: `id_sucursal` (nullable) - EXISTE

### Inconsistencias Detectadas
- **clientes.id_sucursal**: En BD es `NOT NULL`, pero en `Cliente.java` entity es `nullable = true`
- **Impacto**: Puede causar problemas al guardar clientes sin sucursal asignada
- **Recomendación**: Revisar si debe ser nullable o no según reglas de negocio

### Repositorios con Métodos de Filtrado
- ✅ **ReservaRepository**: `findByEmpresaIdAndSucursalId()` - EXISTE
- ✅ **VentaRepository**: `findByFiltros()` (incluye sucursalId) - EXISTE
- ✅ **ClienteRepository**: `findByEmpresaIdAndSucursalId()` - EXISTE (Ya implementado)
- ✅ **PersonalRepository**: `findWithFilters()` (incluye sucursalId) - EXISTE
- ✅ **ServicioTuristicoRepository**: `findByEmpresaIdAndSucursalId()` - EXISTE
- ✅ **PaqueteTuristicoRepository**: `findByEmpresaIdAndSucursalId()` - EXISTE

### Cambios Requeridos en Plan
- [x] Agregar método `findByEmpresaIdAndSucursalId()` en `ClienteRepository` (Ya existía)
- [ ] Considerar corregir inconsistencia nullable en `Cliente.id_sucursal`

## Consideraciones Técnicas

### Lógica de Filtrado
```javascript
// Para cada entidad, filtrar incluyendo NULL
function filtrarPorSucursal(items, sucursalId) {
    return items.filter(item => {
        const itemSucursalId = item.sucursal?.idSucursal || item.idSucursal;
        return itemSucursalId === null || itemSucursalId === sucursalId;
    });
}
```

### Compatibilidad
- Mantener compatibilidad con llamadas existentes (sin `idSucursal`)
- Los campos `*Raw` son opcionales para mantener compatibilidad

### Performance
- El backend filtra en BD, frontend solo recalcula agregados
- Datos crudos permiten filtrado eficiente en memoria

## Riesgos y Mitigaciones

### Riesgo: Errores en recálculo de agregados
**Mitigación**: Implementar funciones de recálculo bien testeadas, comparar con valores del backend cuando no hay filtro

### Riesgo: Exposición de datos
**Mitigación**: Validar permisos en frontend y backend, asegurar que gerentes solo vean su sucursal

### Riesgo: Inconsistencias entre backend y frontend
**Mitigación**: Mantener lógica de agregación sincronizada, usar mismas fórmulas

### Riesgo: Inconsistencia nullable en clientes.id_sucursal
**Mitigación**: Definir regla de negocio clara: ¿Los clientes pueden no tener sucursal asignada?

## ✅ Implementación Completada

### Resumen de Cambios Realizados

#### Backend (Java) - ✅ Completado
- **ReportesService**: Todos los métodos `reporte*()` modificados para aceptar `Long idSucursal`
- **Campos Raw**: Agregados `reservasRaw`, `ventasRaw`, `clientesRaw`, `personalRaw`, `serviciosRaw`, `paquetesRaw`
- **ClienteRepository**: Método `findByEmpresaIdAndSucursalId()` ya existía
- **Correcciones**: Conversión Date→LocalDate, comparación EstadoReserva enum, parámetros findWithFilters
- **Compilación**: ✅ Exitosa (`mvn compile` exit code 0)

#### Frontend (PHP/JavaScript) - ✅ Completado
- **dashboard.php**: Detección de rol gerente en PHP y paso de `esGerente`/`idSucursal` a JavaScript
- **reportes.js**: Estado `idSucursal` y `esGerente` agregado desde PHP
- **Funciones de filtrado**: `filtrar*PorSucursal()` para cada entidad
- **Funciones de recálculo**: `recalcularAgregados*()` para estadísticas
- **Carga de reportes**: Filtrado y recálculo cuando `esGerente = true`

### Funcionamiento Implementado
1. **Administradores**: Ven **todos** los datos de la empresa
2. **Gerentes**: Ven **solo** datos de su sucursal + datos con `sucursal = NULL`
3. **Compatibilidad**: Llamadas existentes funcionan sin cambios

### Estado Final
- ✅ **Backend**: 100% implementado y compilando
- ✅ **Frontend**: 100% implementado
- ⏳ **Testing**: Pendiente verificación funcional
- ✅ **Documentación**: Plan actualizado con checkmarks