package com.sistema.turistico.controller;

import com.sistema.turistico.entity.Personal;
import com.sistema.turistico.service.PersonalService;
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
@RequestMapping("/personal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Personal", description = "Gestión del personal de la empresa")
public class PersonalController {

    private final PersonalService personalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear personal", description = "Registra un nuevo miembro del personal")
    public ResponseEntity<Map<String, Object>> crearPersonal(@Valid @RequestBody Personal personal) {
        try {
            Personal nuevoPersonal = personalService.crearPersonal(personal);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Personal creado exitosamente");
            response.put("data", nuevoPersonal);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear personal: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al crear personal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener personal", description = "Obtiene la información de un miembro del personal específico")
    public ResponseEntity<Map<String, Object>> obtenerPersonal(@PathVariable Long id) {
        try {
            Personal personal = personalService.obtenerPersonalPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Personal obtenido exitosamente");
            response.put("data", personal);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Personal no encontrado: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener personal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar personal", description = "Lista el personal con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarPersonal(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) Long sucursalId,
            @RequestParam(required = false) Personal.Cargo cargo,
            @RequestParam(required = false) Boolean estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<Personal> personal = personalService.listarPersonalConFiltros(empresaId, sucursalId, cargo, estado, busqueda);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Personal obtenido exitosamente");
            response.put("data", personal);
            response.put("total", personal.size());
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) personal.size() / size));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar personal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar personal", description = "Actualiza la información de un miembro del personal")
    public ResponseEntity<Map<String, Object>> actualizarPersonal(@PathVariable Long id, @Valid @RequestBody Personal personal) {
        try {
            Personal personalActualizado = personalService.actualizarPersonal(id, personal);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Personal actualizado exitosamente");
            response.put("data", personalActualizado);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar personal: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al actualizar personal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Desactivar personal", description = "Desactiva un miembro del personal")
    public ResponseEntity<Map<String, Object>> eliminarPersonal(@PathVariable Long id) {
        try {
            personalService.eliminarPersonal(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Personal desactivado exitosamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al desactivar personal: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al desactivar personal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar personal por empresa", description = "Obtiene todo el personal de una empresa específica")
    public ResponseEntity<Map<String, Object>> listarPersonalPorEmpresa(@PathVariable Long empresaId) {
        try {
            List<Personal> personal = personalService.listarPersonalPorEmpresa(empresaId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Personal obtenido exitosamente");
            response.put("data", personal);
            response.put("total", personal.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar personal por empresa", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/cargo/{cargo}/empresa/{empresaId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar personal por cargo", description = "Obtiene el personal de un cargo específico en una empresa")
    public ResponseEntity<Map<String, Object>> listarPersonalPorCargo(@PathVariable Personal.Cargo cargo, @PathVariable Long empresaId) {
        try {
            List<Personal> personal = personalService.obtenerPersonalPorCargo(empresaId, cargo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Personal obtenido exitosamente");
            response.put("data", personal);
            response.put("total", personal.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar personal por cargo", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
