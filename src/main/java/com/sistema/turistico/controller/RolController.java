package com.sistema.turistico.controller;

import com.sistema.turistico.dto.RolRequest;
import com.sistema.turistico.dto.RolResponse;
import com.sistema.turistico.entity.Rol;
import com.sistema.turistico.service.RolService;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roles", description = "Endpoints para gestión de roles")
public class RolController {

    private final RolService rolService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Crear rol", description = "Crea un nuevo rol")
    public ResponseEntity<Map<String, Object>> crearRol(@Valid @RequestBody RolRequest request) {
        try {
            log.info("Solicitando creación de rol");
            Rol rol = rolService.crear(request);
            RolResponse data = rolService.toResponse(rol);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rol creado exitosamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear rol: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al crear rol", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Listar roles", description = "Obtiene la lista de roles")
    public ResponseEntity<Map<String, Object>> listarRoles(
        @RequestParam(required = false) String busqueda,
        @RequestParam(required = false) Integer estado) {
        try {
            log.info("Listando roles con filtros: busqueda={}, estado={}", busqueda, estado);
            List<Rol> roles = rolService.listar(busqueda, estado);
            List<RolResponse> data = roles.stream()
                .map(rolService::toResponse)
                .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Roles obtenidos exitosamente");
            response.put("data", data);
            response.put("total", data.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar roles", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Obtener rol", description = "Obtiene un rol por su identificador")
    public ResponseEntity<Map<String, Object>> obtenerRol(@PathVariable Long id) {
        try {
            log.info("Obteniendo rol {}", id);
            Rol rol = rolService.obtenerPorId(id);
            RolResponse data = rolService.toResponse(rol);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rol obtenido exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Rol no encontrado: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener rol {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Actualizar rol", description = "Actualiza los datos de un rol")
    public ResponseEntity<Map<String, Object>> actualizarRol(@PathVariable Long id,
                                                             @Valid @RequestBody RolRequest request) {
        try {
            log.info("Actualizando rol {}", id);
            Rol rol = rolService.actualizar(id, request);
            RolResponse data = rolService.toResponse(rol);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rol actualizado exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar rol: {}", ex.getMessage());
            if ("Rol no encontrado".equalsIgnoreCase(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al actualizar rol {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Eliminar rol", description = "Realiza un borrado lógico de un rol")
    public ResponseEntity<Map<String, Object>> eliminarRol(@PathVariable Long id) {
        try {
            log.info("Eliminando rol {}", id);
            rolService.eliminar(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Rol eliminado exitosamente"
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al eliminar rol: {}", ex.getMessage());
            if ("Rol no encontrado".equalsIgnoreCase(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al eliminar rol {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
