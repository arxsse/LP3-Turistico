# Plan de implementación: edición de reservas

## 1. Contexto y hallazgos

- **Modelo de datos**
  - Tabla `reservas`: admite cambios en `fecha_servicio`, `numero_personas`, `descuento_aplicado`, `observaciones` y contiene claves foráneas hacia `clientes`, `usuarios` y `promociones`.
  - Tabla `reserva_items`: relación 1:N con `reservas`, `orphanRemoval` habilitado en JPA. Cada ítem guarda `tipo_item`, `id_servicio` o `id_paquete`, `cantidad`, importes y notas.
- **Capa de dominio (Java)**
  - `Reserva` recalcula totales desde los ítems (`setItems`, `recalcularTotalesDesdeItems`).
  - `ReservaService#create` valida empresa, cliente, construye ítems con lógica común (`construirItem`).
  - `ReservaService#update` actualmente solo modifica `estado` y `observaciones`.
  - DTOs: `ReservaRequest` (alta) incluye lista de ítems obligatoria; `ReservaUpdateRequest` solo `estado` y `observaciones`; `ReservaItemRequest` ya tiene validaciones reutilizables.
- **Frontend (PHP)**
  - `inicio/RESERVAS/reservas_table.php` lista y muestra detalle; no existe flujo de edición.
  - `inicio/RESERVAS/nueva_reserva.php` contiene UI y JS para armar payload completo; es candidato a reutilización.

## 2. Objetivos del cambio

1. Permitir actualizar reservas existentes en backend con los campos:
   - `fechaServicio`, `numeroPersonas`, `descuentoAplicado`, `observaciones`.
   - Sustitución completa de la colección de `items` (crear, actualizar, eliminar).
2. Exponer endpoint seguro (roles actuales) con validaciones equivalentes a la creación.
3. Habilitar en el frontend un flujo de edición que reutilice la UI de creación.
4. Mantener integridad multi-tenant y cohesión de totales/precios.

## 3. Cambios requeridos en backend

### 3.1 DTOs

- Extender `ReservaItemRequest` para aceptar opcionalmente `idReservaItem` (solo para trazabilidad; se puede ignorar si se reemplaza toda la colección).
- Crear nuevo DTO `ReservaEditRequest` (o ampliar `ReservaUpdateRequest`) con:
   - `Long empresaId` opcional para validación multiempresa.
   - `String fechaServicio` (obligatoria).
   - `Integer numeroPersonas`.
   - `BigDecimal descuentoAplicado`.
   - `String observaciones`.
   - `List<ReservaItemRequest> items` (mínimo 1).
   - La reserva mantiene su `fechaReserva` original; cualquier valor recibido debe ignorarse.
- Ajustar `ReservaResponse` si se requiere devolver `fechaReserva` actualizada y lista de ítems con nuevos `id`s.

### 3.2 Servicio (`ReservaService`)

1. Agregar método `updateDetalle(Long id, ReservaEditRequest request)` que:
   - Obtenga reserva con `obtenerReservaAutorizada`.
   - Verifique estado permitido para edición: solo se admiten reservas `Pendiente` (rechazar `Confirmada`, `Pago Parcial`, `Pagada`, `Cancelada`, `Completada`).
   - Valide fechas (`fechaServicio` futura, `fechaReserva` no vacía).
   - Reconstruya colección de ítems usando `construirItem`/`prepararItem...` reutilizando lógica actual, permitiendo agregar, modificar o eliminar ítems respecto al estado previo.
   - Sincronice totales (`setItems`, `recalcularTotalesDesdeItems`) y aplique número de personas/ descuento.
   - Reaplique validación de empresa para cada recurso referenciado.
   - Persista cambios en transacción.
2. Mantener método `update` actual para cambios simples de estado (o integrarlo en el nuevo método y deprecar el antiguo endpoint).
3. Asegurar que el estado de la reserva no sea modificado por este proceso: siempre se conserva el estado original (`Pendiente`). La transición de estado seguirá realizándose desde endpoints de pagos/cancelación/completado existentes.

### 3.3 Controlador (`ReservaController`)

- Crear endpoint `PUT /reservas/{id}/detalle` (o reutilizar `/reservas/{id}` con request polimórfico) que invoque `updateDetalle`.
- Asegurar anotaciones `@Valid` y `@PreAuthorize` coherentes.
- Documentar en OpenAPI (`@Operation`).
- Incluir validación previa al servicio para devolver 409/422 cuando el estado no sea `Pendiente`.

### 3.4 Conversión y validaciones complementarias

- Asegurar que `TenantContext` se actualiza con empresa del request si llega.
- Mantener `fechaReserva` sin cambios durante la edición para preservar el registro original.
- Recalcular `numeroPersonas` únicamente cuando el request no envíe valor; si el usuario envía un número manual se mantiene aunque no coincida con la suma de cantidades de los ítems.

### 3.5 Pruebas/manual testing

- Preparar casos de prueba manuales (Postman) cubriendo:
  1. Edición exitosa con nuevos ítems.
  2. Eliminación de un ítem existente.
  3. Validación por servicio inactivo o de otra empresa.
  4. Actualización con fecha de servicio pasada (debe fallar).

## 4. Cambios requeridos en frontend (PHP)

1. **API Wrapper**
   - Añadir llamada `PUT` en `nueva_reserva.php` (renombrar archivo o crear `editar_reserva.php`) con soporte para cargar una reserva existente y enviar payload.
   - Ajustar `fetchReservaDetalle` para incluir datos necesarios (`idReservaItem`, `tipoItem`, etc.).
2. **UI/JS**
   - Reutilizar formulario actual cargando datos (prefilling) desde `/reservas/{id}`.
   - Habilitar botón “Guardar cambios” y `fetch` con método `PUT`.
   - Mantener lógica de alertas y validación en cliente.
3. **Listado** (`reservas_table.php`)
   - Agregar acción “Editar” que abra el mismo formulario con modo edición (pasando `reservaId`).
   - Evitar confundir con flujo de sólo lectura.

## 5. Ajustes de BD / Migración

- No se requieren cambios estructurales.
- Orphan removal en JPA ya elimina ítems retirados, pero verificar cascada.
- Revisar triggers o procesos posteriores (pagos/vouchers) que pudieran depender de totales para evitar inconsistencias.

## 6. Plan de trabajo

1. **Backend**
   - [x] 1.1 Crear DTO `ReservaEditRequest` y actualizar `ReservaItemRequest` si aplica.
   - [x] 1.2 Implementar `ReservaService#updateDetalle` con validaciones.
   - [x] 1.3 Exponer nuevo endpoint (`ReservaController`).
   - [x] 1.4 Actualizar documentación Swagger (si se usa `@Schema`).
   - [x] 1.5 Revisar y, si es necesario, reutilizar el flujo del `POST /reservas` para garantizar consistencia multi-tenant (leer empresa del `TenantContext`, validar pertenencia de servicios/paquetes, setear usuario actual).
2. **Frontend**
   - [x] 2.1 Crear vista/JS de edición reutilizando formulario actual.
   - [x] 2.2 Ajustar listado para abrir modo edición.
   - [x] 2.3 Garantizar que se mapeen `id`s y se envíe payload conforme al nuevo DTO.
   - [x] 2.4 Agregar acción dedicada en la tabla (checkbox/botón "Completar") que solo aparezca cuando la reserva esté en estado `Pagada` y llame al endpoint existente `PUT /reservas/{id}/completar`. Este flujo no debe permitir pasar directamente de `Pendiente` a `Completada`.
3. **Validación**
   - [ ] 3.1 Pruebas Postman para API.
   - [ ] 3.2 Pruebas manuales en interfaz (crear, editar, eliminar ítem).
   - [ ] 3.3 Revisar logs backend en cPanel para errores.
4. **Despliegue**
   - [ ] Empaquetar `.jar` actualizado.
   - [ ] Respaldar base de datos (ya realizado según nota) antes de aplicar cambios en producción.
   - [ ] Ejecutar smoke test tras despliegue.

## 7. Riesgos y consideraciones

- **Consistencia de pagos/vouchers**: al restringir la edición a reservas en estado `Pendiente`, se elimina el riesgo de desbalancear pagos o vouchers existentes.
- **Disponibilidad de servicios/paquetes**: al reconstituir items se debe validar estado y disponibilidad actual.
- **Integraciones externas**: si existen notificaciones automáticas, revisar que la actualización no dispare eventos no deseados.
- **Control de estados**: reforzar en backend y frontend que solo se permite editar cuando la reserva está en `Pendiente`; el resto de transiciones continúan mediante pagos/cancelación/completado.

---

> Con este plan podemos avanzar primero implementando la capa backend y posteriormente integrar el flujo completo en la interfaz PHP.
