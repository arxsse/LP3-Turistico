package com.sistema.turistico.controller;

import com.sistema.turistico.entity.EvaluacionServicio;
import com.sistema.turistico.service.EvaluacionServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evaluaciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Evaluaciones de Servicio", description = "Gestión de evaluaciones de servicios por clientes")
public class EvaluacionServicioController {

    private final EvaluacionServicioService evaluacionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear evaluación", description = "Crea una nueva evaluación de servicio")
    public ResponseEntity<Map<String, Object>> crearEvaluacion(@Valid @RequestBody EvaluacionServicio evaluacion) {
        try {
            EvaluacionServicio nuevaEvaluacion = evaluacionService.create(evaluacion);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluación creada exitosamente");
            response.put("data", nuevaEvaluacion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear evaluación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al crear evaluación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener evaluación", description = "Obtiene una evaluación específica por ID")
    public ResponseEntity<Map<String, Object>> obtenerEvaluacion(@PathVariable Long id) {
        try {
            EvaluacionServicio evaluacion = evaluacionService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluación obtenida exitosamente");
            response.put("data", evaluacion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Evaluación no encontrada: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Error al obtener evaluación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/reserva/{reservaId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Evaluación por reserva", description = "Obtiene la evaluación de una reserva específica")
    public ResponseEntity<Map<String, Object>> obtenerPorReserva(@PathVariable Long reservaId) {
        try {
            EvaluacionServicio evaluacion = evaluacionService.findByReservaId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No existe evaluación para esta reserva"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluación obtenida exitosamente");
            response.put("data", evaluacion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Evaluación no encontrada: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Error al obtener evaluación por reserva", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Evaluaciones por cliente", description = "Lista todas las evaluaciones de un cliente")
    public ResponseEntity<Map<String, Object>> listarPorCliente(@PathVariable Long clienteId) {
        try {
            List<EvaluacionServicio> evaluaciones = evaluacionService.findByClienteId(clienteId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluaciones obtenidas exitosamente");
            response.put("data", evaluaciones);
            response.put("total", evaluaciones.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar evaluaciones por cliente", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/servicio/{servicioId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Evaluaciones por servicio", description = "Lista todas las evaluaciones de un servicio")
    public ResponseEntity<Map<String, Object>> listarPorServicio(@PathVariable Long servicioId) {
        try {
            List<EvaluacionServicio> evaluaciones = evaluacionService.findByServicioId(servicioId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluaciones obtenidas exitosamente");
            response.put("data", evaluaciones);
            response.put("total", evaluaciones.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar evaluaciones por servicio", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/paquete/{paqueteId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Evaluaciones por paquete", description = "Lista todas las evaluaciones de un paquete turístico")
    public ResponseEntity<Map<String, Object>> listarPorPaquete(@PathVariable Long paqueteId) {
        try {
            List<EvaluacionServicio> evaluaciones = evaluacionService.findByPaqueteId(paqueteId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluaciones obtenidas exitosamente");
            response.put("data", evaluaciones);
            response.put("total", evaluaciones.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar evaluaciones por paquete", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/calificacion/{calificacion}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Evaluaciones por calificación", description = "Lista evaluaciones con una calificación específica")
    public ResponseEntity<Map<String, Object>> listarPorCalificacion(@PathVariable Integer calificacion) {
        try {
            List<EvaluacionServicio> evaluaciones = evaluacionService.findByCalificacionGeneral(calificacion);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluaciones obtenidas exitosamente");
            response.put("data", evaluaciones);
            response.put("total", evaluaciones.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar evaluaciones por calificación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar evaluación", description = "Actualiza los datos de una evaluación existente")
    public ResponseEntity<Map<String, Object>> actualizarEvaluacion(@PathVariable Long id, @RequestBody EvaluacionServicio evaluacion) {
        try {
            EvaluacionServicio evaluacionActualizada = evaluacionService.update(id, evaluacion);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluación actualizada exitosamente");
            response.put("data", evaluacionActualizada);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar evaluación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al actualizar evaluación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Cambiar estado evaluación", description = "Oculta o muestra una evaluación (soft delete)")
    public ResponseEntity<Map<String, Object>> cambiarEstadoEvaluacion(@PathVariable Long id) {
        try {
            EvaluacionServicio evaluacion = evaluacionService.toggleEstado(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estado de evaluación cambiado exitosamente");
            response.put("data", evaluacion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al cambiar estado de evaluación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al cambiar estado de evaluación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Eliminar evaluación", description = "Elimina una evaluación del sistema")
    public ResponseEntity<Map<String, Object>> eliminarEvaluacion(@PathVariable Long id) {
        try {
            evaluacionService.delete(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Evaluación eliminada exitosamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al eliminar evaluación: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al eliminar evaluación", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/estadisticas/servicio/{servicioId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Estadísticas por servicio", description = "Obtiene estadísticas de calificaciones para un servicio")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasServicio(@PathVariable Long servicioId) {
        try {
            Object[] estadisticas = evaluacionService.getEstadisticasByServicio(servicioId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estadísticas obtenidas exitosamente");
            response.put("data", Map.of(
                "servicioId", servicioId,
                "promedioCalificacion", estadisticas[0],
                "totalEvaluaciones", estadisticas[1]
            ));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al obtener estadísticas de servicio", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/estadisticas/paquete/{paqueteId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Estadísticas por paquete", description = "Obtiene estadísticas de calificaciones para un paquete")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasPaquete(@PathVariable Long paqueteId) {
        try {
            Object[] estadisticas = evaluacionService.getEstadisticasByPaquete(paqueteId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estadísticas obtenidas exitosamente");
            response.put("data", Map.of(
                "paqueteId", paqueteId,
                "promedioCalificacion", estadisticas[0],
                "totalEvaluaciones", estadisticas[1]
            ));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al obtener estadísticas de paquete", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/puede-evaluar/{reservaId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Verificar si puede evaluar", description = "Verifica si una reserva puede ser evaluada")
    public ResponseEntity<Map<String, Object>> puedeEvaluarReserva(@PathVariable Long reservaId) {
        try {
            boolean puedeEvaluar = evaluacionService.puedeEvaluarReserva(reservaId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verificación realizada exitosamente");
            response.put("data", Map.of(
                "reservaId", reservaId,
                "puedeEvaluar", puedeEvaluar
            ));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al verificar si puede evaluar reserva", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
