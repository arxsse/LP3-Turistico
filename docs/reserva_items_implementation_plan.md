# Plan de implementación: Carrito mixto con `reserva_items`

## 🔁 Flujo general
- [ ] Revisar y confirmar supuestos de negocio (multiempresa activa, reservas existentes con un solo servicio/paquete).
- [ ] Alinear al equipo/docente sobre alcances (no habrá retrocompatibilidad parcial; todas las reservas pasarán por `reserva_items`).

## 🧱 Back-end (Spring Boot)
- [x] Crear entidad `ReservaItem` (`@ManyToOne` hacia `Reserva`, `ServicioTuristico`, `PaqueteTuristico`).
- [x] Mapear tabla `reserva_items` en JPA (incluye `tipoItem`, `cantidad`, `precioUnitario`, `precioTotal`, `descripcionExtra`).
- [ ] Ajustar entidad `Reserva`:
  - [x] Eliminar campos directos `servicio` / `idPaquete`.
  - [x] Agregar `@OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)` para `items`.
  - [x] Asegurar cálculo de `precioTotal` y `numeroPersonas` (sumatoria o derivado de payload).
- [x] Extender `ReservaRequest` y `ReservaItemRequest` para recibir arreglo `items`.
- [x] Validar en `ReservaService`:
  - [x] Al menos un ítem.
  - [x] Todos los ítems coinciden con la `empresa` de la reserva.
  - [x] Resolver referencias según `tipoItem` (`SERVICIO`/`PAQUETE`).
  - [x] Calcular totales (`precioTotal`, descuentos, impuestos si aplica).
- [x] Actualizar DTOs/Responses (`ReservaResponse`, listados, reportes) para incluir `items`.
- [x] Ajustar controladores (`ReservaController`) para aceptar y devolver la nueva estructura.
- [x] Revisar servicios/reportes dependientes (financieros, auditoría) que usen `idServicio`/`idPaquete`.

## 🗃️ Base de datos y migraciones
- [ ] Crear script Flyway `V3__create_reserva_items.sql` con:
  - [ ] Tabla `reserva_items`.
  - [ ] Índices por `id_reserva`, `id_servicio`, `id_paquete`.
  - [ ] Restricciones de integridad (FKs).
- [ ] Script de migración de datos:
  - [ ] Insertar en `reserva_items` un ítem por reserva existente.
  - [ ] Copiar `id_servicio` o `id_paquete` y valores asociados.
  - [ ] Limpiar columnas obsoletas en `reservas` (o mantenerlas hasta despliegue final según estrategia).
- [ ] Actualizar vistas/materializadas, stored procedures o reports manuales si referencian columnas antiguas.
- [ ] Plan de rollback (backup previo + script para revertir cambios si es necesario).

## 🖥️ Front-end (PHP)
- [x] Refactor del formulario `nueva_reserva.php`:
  - [x] Reemplazar select único por constructor de ítems (tipo, servicio/paquete, cantidad, precio).
  - [x] Agregar tabla dinámica para ítems agregados (editar/eliminar).
  - [x] Calcular totales y `numeroPersonas` antes del POST.
  - [x] Serializar `items` en JSON (`items_json`) y enviar al backend.
- [x] Validar inputs en client-side (mínimo 1 ítem, cantidades positivas, precios válidos).
- [x] Ajustar vista de listado de reservas para mostrar ítems (tooltip, modal o expandible).
- [x] Actualizar flujos de edición/cancelación si existen.

## 🔐 Multiempresa
- [x] Validar en backend que ítems correspondan a la empresa de la reserva.
- [x] Confirmar que `paquetes_servicios` también respeta la empresa (agregar chequeo si no existe).

## 🧪 Pruebas
- [ ] Casos unitarios (servicios vs paquetes, mezcla, cantidades múltiples, empresa incorrecta).
- [ ] Pruebas de integración (crear, listar, editar, cancelar reservas con ítems).
- [ ] Testing de migración (datos legacy convertidos correctamente).
- [ ] Validar reportes financieros/auditoría con nuevas estructuras.

## 🚀 Despliegue
- [ ] Preparar feature flag o ventana de mantenimiento para la migración.
- [ ] Ejecutar migración Flyway en entorno de staging → producción.
- [ ] Verificar logs y métricas post despliegue (errores, tiempos, integridad).
- [ ] Documentar nuevos endpoints y payloads en `docs/endpoints.md`.

## 📎 Seguimiento
- [ ] Actualizar manuales internos / capacitaciones.
- [ ] Recoger feedback del docente/usuarios y registrar mejoras pendientes.
