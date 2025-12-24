package com.sistema.turistico.controller;

import com.sistema.turistico.dto.SucursalRequest;
import com.sistema.turistico.dto.SucursalResponse;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.service.SucursalService;
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
@RequestMapping("/sucursales")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sucursales", description = "Endpoints para gestión de sucursales")
public class SucursalController {

    private final SucursalService sucursalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Crear sucursal", description = "Crea una nueva sucursal")
    public ResponseEntity<Map<String, Object>> crearSucursal(@Valid @RequestBody SucursalRequest request) {
        try {
            log.info("Solicitando creación de sucursal");
            Sucursal sucursal = sucursalService.crear(request);
            SucursalResponse data = sucursalService.toResponse(sucursal);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sucursal creada exitosamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear sucursal: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al crear sucursal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Listar sucursales", description = "Obtiene la lista de sucursales")
    public ResponseEntity<Map<String, Object>> listarSucursales(
        @RequestParam(required = false) String busqueda,
        @RequestParam(required = false) Integer estado,
        @RequestParam(required = false) Long empresaId) {
        try {
            log.info("Listando sucursales con filtros: busqueda={}, estado={}, empresaId={}", busqueda, estado, empresaId);
            List<Sucursal> sucursales = sucursalService.listar(busqueda, estado, empresaId);
            List<SucursalResponse> data = sucursales.stream()
                .map(sucursalService::toResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sucursales obtenidas exitosamente");
            response.put("data", data);
            response.put("total", data.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar sucursales", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Obtener sucursal", description = "Obtiene una sucursal por su identificador")
    public ResponseEntity<Map<String, Object>> obtenerSucursal(@PathVariable Long id) {
        try {
            log.info("Obteniendo sucursal {}", id);
            Sucursal sucursal = sucursalService.obtenerPorId(id);
            SucursalResponse data = sucursalService.toResponse(sucursal);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sucursal obtenida exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Sucursal no encontrada: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener sucursal {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Actualizar sucursal", description = "Actualiza los datos de una sucursal")
    public ResponseEntity<Map<String, Object>> actualizarSucursal(@PathVariable Long id,
                                                                  @Valid @RequestBody SucursalRequest request) {
        try {
            log.info("Actualizando sucursal {}", id);
            Sucursal sucursal = sucursalService.actualizar(id, request);
            SucursalResponse data = sucursalService.toResponse(sucursal);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sucursal actualizada exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar sucursal: {}", ex.getMessage());
            if ("Sucursal no encontrada".equalsIgnoreCase(ex.getMessage())
                || "Empresa no encontrada".equalsIgnoreCase(ex.getMessage())) {
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
            log.error("Error interno al actualizar sucursal {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Eliminar sucursal", description = "Realiza un borrado lógico de una sucursal")
    public ResponseEntity<Map<String, Object>> eliminarSucursal(@PathVariable Long id) {
        try {
            log.info("Eliminando sucursal {}", id);
            sucursalService.eliminar(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sucursal eliminada exitosamente"
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al eliminar sucursal: {}", ex.getMessage());
            if ("Sucursal no encontrada".equalsIgnoreCase(ex.getMessage())) {
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
            log.error("Error interno al eliminar sucursal {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
