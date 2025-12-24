package com.sistema.turistico.controller;

import com.sistema.turistico.dto.AsignacionPersonalRequest;
import com.sistema.turistico.dto.AsignacionPersonalReservaRequest;
import com.sistema.turistico.dto.AsignacionPersonalUpdateRequest;
import com.sistema.turistico.entity.AsignacionPersonal;
import com.sistema.turistico.service.AsignacionPersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/asignaciones-personal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Asignaciones de Personal", description = "Gestión de asignaciones de personal a reservas")
public class AsignacionPersonalController {

    private final AsignacionPersonalService asignacionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear asignación", description = "Asigna personal a una reserva")
    public ResponseEntity<Map<String, Object>> crearAsignacion(@Valid @RequestBody AsignacionPersonalRequest request) {
        try {
            AsignacionPersonal nuevaAsignacion = asignacionService.create(request);

            // Crear respuesta con solo los campos de la tabla asignaciones_personal
            Map<String, Object> asignacionData = new HashMap<>();
            asignacionData.put("idAsignacion", nuevaAsignacion.getIdAsignacion());
            asignacionData.put("idPersonal", nuevaAsignacion.getPersonal().getIdPersonal());
            asignacionData.put("idReserva", nuevaAsignacion.getReserva().getIdReserva());
            asignacionData.put("rolAsignado", nuevaAsignacion.getRolAsignado().toString());
            asignacionData.put("fechaAsignacion", nuevaAsignacion.getFechaAsignacion());
            asignacionData.put("estado", nuevaAsignacion.getEstado().toString());
            asignacionData.put("observaciones", nuevaAsignacion.getObservaciones());
            asignacionData.put("createdAt", nuevaAsignacion.getCreatedAt());
            asignacionData.put("updatedAt", nuevaAsignacion.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignación creada exitosamente");
            response.put("data", asignacionData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear asignación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al crear asignación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener asignación", description = "Obtiene una asignación específica por ID")
    public ResponseEntity<Map<String, Object>> obtenerAsignacion(@PathVariable Long id) {
        try {
            AsignacionPersonal asignacion = asignacionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignación obtenida exitosamente");
            response.put("data", asignacion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Asignación no encontrada: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Error al obtener asignación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PostMapping("/reserva/{reservaId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear asignación para reserva", description = "Asigna personal a una reserva específica")
    public ResponseEntity<Map<String, Object>> crearAsignacionParaReserva(@PathVariable Long reservaId, @Valid @RequestBody AsignacionPersonalReservaRequest request) {
        try {
            // Convert to AsignacionPersonalRequest
            AsignacionPersonalRequest fullRequest = new AsignacionPersonalRequest();
            fullRequest.setIdPersonal(request.getIdPersonal());
            fullRequest.setIdReserva(reservaId);
            // rolAsignado will be set based on personal.cargo in the service
            fullRequest.setFechaAsignacion(request.getFechaAsignacion());
            fullRequest.setObservaciones(request.getObservaciones());

            AsignacionPersonal nuevaAsignacion = asignacionService.create(fullRequest);

            // Crear respuesta con solo los campos de la tabla asignaciones_personal
            Map<String, Object> asignacionData = new HashMap<>();
            asignacionData.put("idAsignacion", nuevaAsignacion.getIdAsignacion());
            asignacionData.put("idPersonal", nuevaAsignacion.getPersonal().getIdPersonal());
            asignacionData.put("idReserva", nuevaAsignacion.getReserva().getIdReserva());
            asignacionData.put("rolAsignado", nuevaAsignacion.getRolAsignado().toString());
            asignacionData.put("fechaAsignacion", nuevaAsignacion.getFechaAsignacion());
            asignacionData.put("estado", nuevaAsignacion.getEstado().toString());
            asignacionData.put("observaciones", nuevaAsignacion.getObservaciones());
            asignacionData.put("createdAt", nuevaAsignacion.getCreatedAt());
            asignacionData.put("updatedAt", nuevaAsignacion.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignación creada exitosamente");
            response.put("data", asignacionData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear asignación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al crear asignación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/reserva/{reservaId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Asignaciones por reserva", description = "Lista todas las asignaciones de una reserva")
    public ResponseEntity<Map<String, Object>> listarPorReserva(@PathVariable Long reservaId) {
        try {
            List<AsignacionPersonal> asignaciones = asignacionService.findByReservaId(reservaId);

            // Convertir cada asignación a formato simplificado
            List<Map<String, Object>> asignacionesSimplificadas = asignaciones.stream()
                .map(asignacion -> {
                    Map<String, Object> asignacionData = new HashMap<>();
                    asignacionData.put("idAsignacion", asignacion.getIdAsignacion());
                    asignacionData.put("idPersonal", asignacion.getPersonal().getIdPersonal());
                    asignacionData.put("idReserva", asignacion.getReserva().getIdReserva());
                    asignacionData.put("rolAsignado", asignacion.getRolAsignado().toString());
                    asignacionData.put("fechaAsignacion", asignacion.getFechaAsignacion());
                    asignacionData.put("estado", asignacion.getEstado().toString());
                    asignacionData.put("observaciones", asignacion.getObservaciones());
                    asignacionData.put("createdAt", asignacion.getCreatedAt());
                    asignacionData.put("updatedAt", asignacion.getUpdatedAt());
                    return asignacionData;
                })
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignaciones obtenidas exitosamente");
            response.put("data", asignacionesSimplificadas);
            response.put("total", asignaciones.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar asignaciones por reserva", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/personal/{personalId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Asignaciones por personal", description = "Lista todas las asignaciones de un miembro del personal")
    public ResponseEntity<Map<String, Object>> listarPorPersonal(@PathVariable Long personalId) {
        try {
            List<AsignacionPersonal> asignaciones = asignacionService.findByPersonalId(personalId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignaciones obtenidas exitosamente");
            response.put("data", asignaciones);
            response.put("total", asignaciones.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar asignaciones por personal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/fecha/{fecha}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Asignaciones por fecha", description = "Lista todas las asignaciones para una fecha específica")
    public ResponseEntity<Map<String, Object>> listarPorFecha(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date fecha) {
        try {
            List<AsignacionPersonal> asignaciones = asignacionService.findByFecha(fecha);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignaciones obtenidas exitosamente");
            response.put("data", asignaciones);
            response.put("total", asignaciones.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar asignaciones por fecha", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar asignación", description = "Actualiza el personal y observaciones de una asignación")
    public ResponseEntity<Map<String, Object>> actualizarAsignacion(@PathVariable Long id, @Valid @RequestBody AsignacionPersonalUpdateRequest request) {
        try {
            AsignacionPersonal asignacionActualizada = asignacionService.update(id, request);

            // Crear respuesta con solo los campos de la tabla asignaciones_personal
            Map<String, Object> asignacionData = new HashMap<>();
            asignacionData.put("idAsignacion", asignacionActualizada.getIdAsignacion());
            asignacionData.put("idPersonal", asignacionActualizada.getPersonal().getIdPersonal());
            asignacionData.put("idReserva", asignacionActualizada.getReserva().getIdReserva());
            asignacionData.put("rolAsignado", asignacionActualizada.getRolAsignado().toString());
            asignacionData.put("fechaAsignacion", asignacionActualizada.getFechaAsignacion());
            asignacionData.put("estado", asignacionActualizada.getEstado().toString());
            asignacionData.put("observaciones", asignacionActualizada.getObservaciones());
            asignacionData.put("createdAt", asignacionActualizada.getCreatedAt());
            asignacionData.put("updatedAt", asignacionActualizada.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignación actualizada exitosamente");
            response.put("data", asignacionData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar asignación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al actualizar asignación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}/completar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Completar asignación", description = "Marca una asignación como completada")
    public ResponseEntity<Map<String, Object>> completarAsignacion(@PathVariable Long id) {
        try {
            AsignacionPersonal asignacionCompletada = asignacionService.completar(id);

            // Crear respuesta con solo los campos de la tabla asignaciones_personal
            Map<String, Object> asignacionData = new HashMap<>();
            asignacionData.put("idAsignacion", asignacionCompletada.getIdAsignacion());
            asignacionData.put("idPersonal", asignacionCompletada.getPersonal().getIdPersonal());
            asignacionData.put("idReserva", asignacionCompletada.getReserva().getIdReserva());
            asignacionData.put("rolAsignado", asignacionCompletada.getRolAsignado().toString());
            asignacionData.put("fechaAsignacion", asignacionCompletada.getFechaAsignacion());
            asignacionData.put("estado", asignacionCompletada.getEstado().toString());
            asignacionData.put("observaciones", asignacionCompletada.getObservaciones());
            asignacionData.put("createdAt", asignacionCompletada.getCreatedAt());
            asignacionData.put("updatedAt", asignacionCompletada.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignación completada exitosamente");
            response.put("data", asignacionData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al completar asignación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al completar asignación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Cancelar asignación", description = "Cancela una asignación activa")
    public ResponseEntity<Map<String, Object>> cancelarAsignacion(@PathVariable Long id, @RequestParam String motivo) {
        try {
            AsignacionPersonal asignacionCancelada = asignacionService.cancelar(id, motivo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignación cancelada exitosamente");
            response.put("data", asignacionCancelada);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al cancelar asignación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al cancelar asignación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Eliminar asignación", description = "Elimina una asignación del sistema")
    public ResponseEntity<Map<String, Object>> eliminarAsignacion(@PathVariable Long id) {
        try {
            asignacionService.delete(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Asignación eliminada exitosamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al eliminar asignación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al eliminar asignación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/disponibilidad")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Verificar disponibilidad", description = "Verifica si un personal está disponible en una fecha específica")
    public ResponseEntity<Map<String, Object>> verificarDisponibilidad(@RequestParam Long personalId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fecha) {
        try {
            boolean disponible = asignacionService.verificarDisponibilidadPersonal(personalId, fecha);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Disponibilidad verificada exitosamente");
            response.put("data", Map.of(
                "personalId", personalId,
                "fecha", fecha,
                "disponible", disponible
            ));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al verificar disponibilidad", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
