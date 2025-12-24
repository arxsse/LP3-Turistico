# Plan de implementación: asignación de personal a reservas

## 1. Diagnóstico inicial y factibilidad

- **Modelo de datos**: existen las entidades `Personal`, `AsignacionPersonal` y `Reserva`; la tabla de asignaciones permite múltiples miembros por reserva y tiene restricción única `(id_personal, id_reserva, fecha_asignacion)`. La relación actual es `AsignacionPersonal -> Reserva` (ManyToOne); la entidad `Reserva` no expone todavía la colección inversa.
- **Lógica de negocio disponible**: el servicio `AsignacionPersonalService` ya expone creación, actualización, cancelación y consulta por reserva/personal, con validaciones de disponibilidad y sincronización con `TenantContext`. Esto confirma compatibilidad con el esquema multiempresa.
- **Multiempresa**: toda la capa de servicio valida que el `personal` y la `reserva` pertenezcan a la misma empresa y que el usuario actual tenga acceso. El frontend ya transmite `empresaId` (session/localStorage), por lo que la reutilización para esta característica requiere únicamente incluirlo en las peticiones nuevas.
- **Frontend actual**: los formularios `nueva_reserva.php` y `editar_reserva.php` sólo gestionan servicios/paquetes. La tabla `reservas_table.php` muestra el detalle en un modal, pero no carga asignaciones. Se detecta espacio en la UI para añadir una sección de "Personal asignado" en creación, edición y vista.
- **Conclusión**: la funcionalidad es abordable con cambios moderados. El backend necesita exponer APIs orientadas a reservas (lectura y sincronización masiva) y el frontend debe incorporar un componente reutilizable para seleccionar personal disponible. No hay impedimentos multiempresa siempre que las llamadas incluyan token y empresa como ya ocurre.

## 2. Objetivos

1. Permitir registrar uno o varios miembros del personal para una reserva durante la creación y la edición.
2. Mostrar el personal asignado en el detalle de la reserva y permitir gestionar su estado (completado/cancelado).
3. Garantizar que las validaciones de disponibilidad y multiempresa se mantengan en todos los flujos.
4. Evitar inconsistencias introduciendo operaciones atómicas/bulk para sincronizar asignaciones.

## 3. Alcance y supuestos

- El estado de la reserva condiciona la edición de asignaciones (solo `Pendiente` y `Pagada` podrán modificarse; `Completada` o estados finales no permiten cambios porque el servicio ya culminó).
- Las fechas de asignación coinciden con la fecha de servicio de la reserva (regla existente).
- El frontend continuará consumiendo la API REST desde PHP; no se introduce SPA.
- Se asume que el módulo de personal ya permite filtrar por empresa y estado (utilizaremos esos endpoints).

## 4. Backend

### 4.1 Nuevos DTO y endpoints

- [x] 1.1 Crear DTO de asignaciones (`ReservaAsignacionPayload`, `ReservaAsignacionSyncRequest`, `ReservaAsignacionResponse`) con lista de personal (idPersonal, observaciones y fecha opcional => usa fecha servicio por defecto).
- [x] 1.2 Extender `ReservaResponse` con colección `asignaciones` (DTO liviano: idAsignacion, idPersonal, nombre, cargo, estado, observaciones, timestamps).
- [x] 1.3 Añadir endpoint `GET /reservas/{id}/asignaciones` (o extender detalle) que devuelva la lista en clave `data` para consumo directo desde PHP.
- [x] 1.4 Crear endpoint `PUT /reservas/{id}/asignaciones` que sincronice la lista completa (alta, mantenimiento, eliminación) respetando las validaciones actuales.

### 4.2 Lógica de servicio

- [x] 2.1 Incorporar en `ReservaService` un método de sincronización que:
  - Verifique estado editable de la reserva.
  - Consulte asignaciones actuales vía `AsignacionPersonalRepository`.
  - Cree nuevas si no existen, actualice observaciones/rol cuando aplique y elimine las no incluidas.
  - Use `AsignacionPersonalService` para centralizar validaciones de disponibilidad/multiempresa.
- [x] 2.2 Añadir helper para obtener datos enriquecidos del personal (constructor `toAsignacionResponse`).
- [x] 2.3 Revisar `ReservaService#create` y `updateDetalle` para aceptar un bloque opcional de asignaciones y ejecutarlo dentro de la misma transacción, replicando la mecánica usada con los ítems.

### 4.3 Repositorio y rendimiento

- [x] 3.1 Añadir query con `JOIN FETCH` en `AsignacionPersonalRepository` para evitar `N+1` al listar por reserva (`findDetailedByReservaId`).
- [x] 3.2 Evaluar índices necesarios (`id_reserva`, `estado`) si la tabla crece (se añadió `V4__optimize_asignaciones_personal_indexes.sql` con índices para `estado` y la combinación `id_personal/fecha/estado`).

### 4.4 Seguridad y validaciones

- [x] 4.1 Mantener uso de `TenantContext.requireEmpresaIdOrCurrent` en cada flujo.
- [x] 4.2 Registrar eventos en logs para auditoría (alta/edición/cancelación de asignaciones) usando el logger dedicado `AUDIT`.
- [x] 4.3 Actualizar documentación OpenAPI.

## 5. Frontend (PHP)

### 5.1 Formulario de nueva reserva (`inicio/RESERVAS/nueva_reserva.php`)

- [x] 1.0 Preparar helper PHP para normalizar `asignaciones_json` y retener selección tras errores de validación (usar `buildAssignmentsPayload` y `$personalSeleccionados`).
- [x] 1.1 Incorporar sección "Personal asignado" con selector múltiple.
- [x] 1.2 Consumir `GET /personal?empresaId=...&estado=true` para cargar catálogo (incluir cargo, disponibilidad o badge de ocupación si se amplía backend).
- [x] 1.3 Tras crear la reserva, invocar `PUT /reservas/{id}/asignaciones` enviando la lista seleccionada (manejar flujos de éxito/error). Se acordó mantener todo en el `POST /reservas`; no se hará llamada adicional.
- [x] 1.4 Mostrar advertencias si algún miembro ya está ocupado (mensaje devuelto por API).

### 5.2 Formulario de edición (`inicio/RESERVAS/editar_reserva.php`)

- [x] 2.1 Pre-cargar asignaciones existentes usando `GET /reservas/{id}/asignaciones` y marcar los seleccionados.
- [x] 2.2 Permitir añadir/eliminar miembros y modificar observaciones/rol.
- [x] 2.3 Enviar cambios junto con el guardado del detalle (o como paso posterior) usando el endpoint de sincronización.

### 5.3 Detalle y tabla (`inicio/RESERVAS/reservas_table.php`)

- [x] 3.1 Ampliar modal de detalle para listar asignaciones (nombre, cargo, estado, observaciones).
- [x] 3.2 Añadir acciones rápidas: completar/cancelar asignación (reutilizar endpoints `PUT /asignaciones-personal/{id}/completar` y `/cancelar`).
- [x] 3.3 Mostrar contador de personal asignado en la tabla (opcional, badge).

### 5.4 Experiencia de usuario

- [x] 4.1 Reutilizar componente de alertas para avisos de asignación.
- [x] 4.2 Deshabilitar selección de personal cuando la reserva no sea editable (solo habilitado para estados `Pendiente`, `Confirmada` o `Pagada`).
- [x] 4.3 Garantizar que `empresaId` se propaga desde `userData` para las peticiones nuevas.

## 6. Validaciones y QA

- [ ] Probar creación de reserva con múltiples asignaciones (exitoso y con personal duplicado esperado).
- [ ] Probar edición removiendo y añadiendo personal, confirmando que se respetan reglas de disponibilidad.
- [ ] Verificar que usuarios de otra empresa no pueden listar ni asignar personal ajeno.
- [ ] Validar acciones completar/cancelar y su reflejo en la UI.
- [ ] Ejecutar smoke tests backend (`mvn test -D...`) y pruebas manuales en el frontend.

## 7. Riesgos y mitigaciones

- **Conflictos de disponibilidad**: manejar gracefully mensajes del backend y permitir reintentos tras actualizar la lista.
- **Desfase de datos**: minimizarlo vinculando la asignación al mismo request de creación/edición de la reserva; solo si se decide enviar en un segundo paso considerarlo un fallback con reintento.
- **Cargas grandes de personal**: podría requerir paginación/filtrado en el selector; considerar búsqueda incremental si la lista crece.
- **Estados de reserva**: definir claramente en backend y frontend cuándo se permite asignar para evitar inconsistencias con pagos/completado.

## 8. Dependencias

- Endpoints existentes: `/personal`, `/asignaciones-personal/*`, `/reservas`.
- Token JWT almacenado en `sessionStorage/localStorage` (reutilizado).
- Estilos y componentes de alertas (`alertas.css`) ya disponibles para la nueva sección.
