package com.sistema.turistico.controller;

import com.sistema.turistico.dto.ReservaAsignacionResponse;
import com.sistema.turistico.dto.ReservaAsignacionSyncRequest;
import com.sistema.turistico.dto.ReservaEditRequest;
import com.sistema.turistico.dto.ReservaRequest;
import com.sistema.turistico.dto.ReservaResponse;
import com.sistema.turistico.dto.ReservaUpdateRequest;
import com.sistema.turistico.dto.VoucherResponse;
import com.sistema.turistico.entity.Reserva;
import com.sistema.turistico.entity.Voucher;
import com.sistema.turistico.repository.VoucherRepository;
import com.sistema.turistico.security.TenantContext;
import com.sistema.turistico.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reservas", description = "Endpoints para gestión de reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final VoucherRepository voucherRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear reserva", description = "Crea una nueva reserva en el sistema")
    public ResponseEntity<Map<String, Object>> crearReserva(@Valid @RequestBody ReservaRequest request) {
        try {
            log.info("Creando nueva reserva para cliente ID: {}", request.getClienteId());

            Reserva nuevaReserva = reservaService.create(request);
            ReservaResponse reservaResponse = reservaService.toResponse(nuevaReserva);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva creada exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error de validación al crear reserva: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error interno al crear reserva", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar reservas", description = "Obtiene la lista de reservas con filtros opcionales")
        public ResponseEntity<Map<String, Object>> listarReservas(
            @RequestParam(name = "busqueda", required = false) String busqueda,
            @RequestParam(name = "estado", required = false) Reserva.EstadoReserva estado,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "empresaId", required = false) Long empresaId,
            @RequestParam(name = "idSucursal", required = false) Long idSucursal) {
        try {
            Long empresaLog = empresaId != null ? empresaId : TenantContext.getEmpresaId().orElse(null);
            int pageNumber = Math.max(page, 1);
            int pageSize = size > 0 ? size : 10;

            log.info("Listando reservas de empresa {} con filtros - página: {}, tamaño: {}", empresaLog, pageNumber, pageSize);

            List<Reserva> reservas;

            if (estado != null) {
                reservas = reservaService.findByEmpresaIdAndEstado(empresaId, idSucursal, estado);
            } else {
                reservas = reservaService.findByEmpresaIdAndBusqueda(empresaId, idSucursal, busqueda);
            }

            // Implementar paginación simple
            int startIndex = (pageNumber - 1) * pageSize;
            if (startIndex >= reservas.size()) {
                startIndex = reservas.size();
            }
            int endIndex = Math.min(startIndex + pageSize, reservas.size());

            List<Reserva> reservasPaginadas = reservas.subList(startIndex, endIndex);

            // Convertir a ReservaResponse
            List<ReservaResponse> reservasResponse = reservasPaginadas.stream()
                .map(reservaService::toResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reservas obtenidas exitosamente");
            response.put("data", reservasResponse);
            response.put("total", reservas.size());
            response.put("page", pageNumber);
            response.put("size", pageSize);
            response.put("totalPages", pageSize > 0 ? (int) Math.ceil((double) reservas.size() / pageSize) : 1);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error de validación al listar reservas: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al listar reservas", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener reserva por ID", description = "Obtiene una reserva específica por su ID")
    public ResponseEntity<Map<String, Object>> obtenerReserva(@PathVariable Long id) {
        try {
            log.info("Obteniendo reserva con ID: {}", id);

            // Obtener y convertir en una sola llamada transaccional
            ReservaResponse reservaResponse = reservaService.findByIdAndConvert(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva obtenida exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Reserva no encontrada: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error al obtener reserva con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar reserva", description = "Actualiza los datos de una reserva existente")
    public ResponseEntity<Map<String, Object>> actualizarReserva(@PathVariable Long id, @RequestBody ReservaUpdateRequest request) {
        try {
            log.info("Actualizando reserva ID: {}", id);

            Reserva reservaActualizada = reservaService.update(id, request);
            ReservaResponse reservaResponse = reservaService.toResponse(reservaActualizada);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva actualizada exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error de validación al actualizar reserva: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al actualizar reserva con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/detalle")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar detalle de reserva", description = "Actualiza fecha de servicio, observaciones, descuento, número de personas e items de una reserva pendiente")
    public ResponseEntity<Map<String, Object>> actualizarDetalleReserva(@PathVariable Long id, @Valid @RequestBody ReservaEditRequest request) {
        try {
            log.info("Actualizando detalle completo de reserva ID: {}", id);

            Reserva reservaActual = reservaService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

            if (reservaActual.getEstado() != Reserva.EstadoReserva.Pendiente) {
                Map<String, Object> conflictResponse = new HashMap<>();
                conflictResponse.put("success", false);
                conflictResponse.put("message", "Solo se pueden editar reservas en estado Pendiente");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse);
            }

            Reserva reservaActualizada = reservaService.updateDetalle(id, request);
            ReservaResponse reservaResponse = reservaService.toResponse(reservaActualizada);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva actualizada exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar detalle de reserva: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (IllegalStateException e) {
            log.warn("Conflicto al actualizar detalle de reserva: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (Exception e) {
            log.error("Error al actualizar detalle de reserva con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}/asignaciones")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listado de personal asignado", description = "Obtiene el personal asociado a la reserva")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asignaciones obtenidas exitosamente", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Error de validación", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, Object>> obtenerAsignacionesReserva(@PathVariable Long id) {
        try {
            List<ReservaAsignacionResponse> asignaciones = reservaService.obtenerAsignaciones(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignaciones obtenidas exitosamente");
            response.put("data", asignaciones);
            response.put("total", asignaciones.size());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al obtener asignaciones para reserva {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Error interno al obtener asignaciones de la reserva {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/asignaciones")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Sincronizar personal asignado", description = "Reemplaza el personal asociado a la reserva. Solo válido para reservas en estado Pendiente o Pagada")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asignaciones actualizadas exitosamente", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Error de validación", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, Object>> sincronizarAsignaciones(
        @PathVariable Long id,
        @Valid @RequestBody ReservaAsignacionSyncRequest request
    ) {
        try {
            Reserva reservaActualizada = reservaService.actualizarAsignaciones(id, request);
            ReservaResponse reservaResponse = reservaService.toResponse(reservaActualizada);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignaciones actualizadas exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al sincronizar asignaciones de la reserva {}: {}", id, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("Error interno al sincronizar asignaciones de la reserva {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Eliminar reserva", description = "Elimina una reserva del sistema (soft delete)")
    public ResponseEntity<Map<String, Object>> eliminarReserva(@PathVariable Long id) {
        try {
            log.info("Eliminando reserva ID: {}", id);

            reservaService.delete(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva eliminada exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al eliminar reserva: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al eliminar reserva con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva activa")
    public ResponseEntity<Map<String, Object>> cancelarReserva(@PathVariable Long id) {
        try {
            log.info("Cancelando reserva ID: {}", id);

            Reserva reservaCancelada = reservaService.cancel(id);
            ReservaResponse reservaResponse = reservaService.toResponse(reservaCancelada);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva cancelada exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al cancelar reserva: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al cancelar reserva con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/completar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Completar reserva", description = "Marca una reserva como completada después de finalizar el servicio")
    public ResponseEntity<Map<String, Object>> completarReserva(@PathVariable Long id) {
        try {
            log.info("Completando reserva ID {}", id);

            Reserva reservaCompletada = reservaService.completar(id);
            ReservaResponse reservaResponse = reservaService.toResponse(reservaCompletada);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva completada exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al completar reserva: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al completar reserva con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}/evaluar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Marcar reserva como evaluada", description = "Marca una reserva completada como evaluada")
    public ResponseEntity<Map<String, Object>> marcarComoEvaluada(@PathVariable Long id) {
        try {
            log.info("Marcando reserva ID {} como evaluada", id);

            Reserva reservaEvaluada = reservaService.marcarComoEvaluada(id);
            ReservaResponse reservaResponse = reservaService.toResponse(reservaEvaluada);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reserva marcada como evaluada exitosamente");
            response.put("data", reservaResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al marcar reserva como evaluada: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al marcar reserva como evaluada con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/proximas")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Reservas próximas", description = "Obtiene las reservas próximas (próximos 7 días)")
    public ResponseEntity<Map<String, Object>> obtenerReservasProximas(@RequestParam(required = false) Long empresaId) {
        try {
            Long empresaLog = empresaId != null ? empresaId : TenantContext.getEmpresaId().orElse(null);
            log.info("Obteniendo reservas próximas para empresa {}", empresaLog);

            List<Reserva> reservas = reservaService.findReservasProximas(empresaId);

            // Convertir a ReservaResponse
            List<ReservaResponse> reservasResponse = reservas.stream()
                .map(reservaService::toResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reservas próximas obtenidas exitosamente");
            response.put("data", reservasResponse);
            response.put("total", reservasResponse.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al obtener reservas próximas: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al obtener reservas próximas", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}/voucher")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener voucher de reserva", description = "Obtiene el voucher asociado a una reserva específica")
    public ResponseEntity<Map<String, Object>> obtenerVoucherReserva(@PathVariable Long id) {
        try {
            log.info("Obteniendo voucher para reserva ID: {}", id);

            // Verificar que la reserva existe
            Reserva reserva = reservaService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

            // Buscar el voucher de la reserva
            Voucher voucher = voucherRepository.findByReserva_IdReserva(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher no encontrado para esta reserva"));

            VoucherResponse voucherResponse = reservaService.toVoucherResponse(voucher);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voucher obtenido exitosamente");
            response.put("data", voucherResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Error al obtener voucher: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error al obtener voucher para reserva ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
