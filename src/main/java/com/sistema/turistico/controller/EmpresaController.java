package com.sistema.turistico.controller;

import com.sistema.turistico.dto.EmpresaRequest;
import com.sistema.turistico.dto.EmpresaResponse;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.service.EmpresaService;
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
@RequestMapping("/empresas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Empresas", description = "Endpoints para gestión de empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    @PostMapping
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    @Operation(summary = "Crear empresa", description = "Crea una nueva empresa")
    public ResponseEntity<Map<String, Object>> crearEmpresa(@Valid @RequestBody EmpresaRequest request) {
        try {
            log.info("Solicitando creación de empresa");
            Empresa empresa = empresaService.crear(request);
            EmpresaResponse data = empresaService.toResponse(empresa);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Empresa creada exitosamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear empresa: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al crear empresa", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    @Operation(summary = "Listar empresas", description = "Obtiene la lista de empresas (solo para administradores)")
    public ResponseEntity<Map<String, Object>> listarEmpresas(
        @RequestParam(required = false) String busqueda,
        @RequestParam(required = false) Integer estado) {
        try {
            log.info("Listando empresas con filtros: busqueda={}, estado={}", busqueda, estado);
            List<Empresa> empresas = empresaService.listar(busqueda, estado);
            List<EmpresaResponse> data = empresas.stream()
                .map(empresaService::toResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Empresas obtenidas exitosamente");
            response.put("data", data);
            response.put("total", data.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar empresas", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    @Operation(summary = "Obtener empresa", description = "Obtiene una empresa por su identificador")
    public ResponseEntity<Map<String, Object>> obtenerEmpresa(@PathVariable Long id) {
        try {
            log.info("Obteniendo empresa {}", id);
            Empresa empresa = empresaService.obtenerPorId(id);
            EmpresaResponse data = empresaService.toResponse(empresa);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Empresa obtenida exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Empresa no encontrada: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener empresa {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    @Operation(summary = "Actualizar empresa", description = "Actualiza los datos de una empresa")
    public ResponseEntity<Map<String, Object>> actualizarEmpresa(@PathVariable Long id,
                                                                 @Valid @RequestBody EmpresaRequest request) {
        try {
            log.info("Actualizando empresa {}", id);
            Empresa empresa = empresaService.actualizar(id, request);
            EmpresaResponse data = empresaService.toResponse(empresa);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Empresa actualizada exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar empresa: {}", ex.getMessage());
            if ("Empresa no encontrada".equalsIgnoreCase(ex.getMessage())) {
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
            log.error("Error interno al actualizar empresa {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    @Operation(summary = "Eliminar empresa", description = "Realiza un borrado lógico de una empresa")
    public ResponseEntity<Map<String, Object>> eliminarEmpresa(@PathVariable Long id) {
        try {
            log.info("Eliminando empresa {}", id);
            empresaService.eliminar(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Empresa eliminada exitosamente"
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al eliminar empresa: {}", ex.getMessage());
            if ("Empresa no encontrada".equalsIgnoreCase(ex.getMessage())) {
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
            log.error("Error interno al eliminar empresa {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
