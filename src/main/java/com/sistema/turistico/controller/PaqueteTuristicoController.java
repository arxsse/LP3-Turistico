package com.sistema.turistico.controller;

import com.sistema.turistico.dto.PaqueteResponse;
import com.sistema.turistico.entity.PaqueteTuristico;
import com.sistema.turistico.service.PaqueteTuristicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/paquetes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Paquetes Turísticos", description = "Endpoints para gestión de paquetes turísticos")
public class PaqueteTuristicoController {

    private final PaqueteTuristicoService paqueteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear paquete turístico", description = "Crea un nuevo paquete turístico en el sistema")
    public ResponseEntity<Map<String, Object>> crearPaquete(@Valid @RequestBody PaqueteTuristico paquete) {
        try {
            log.info("Creando paquete turístico: {}", paquete.getNombrePaquete());

            PaqueteTuristico nuevoPaquete = paqueteService.create(paquete);
            PaqueteResponse paqueteResponse = paqueteService.toResponse(nuevoPaquete);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paquete turístico creado exitosamente");
            response.put("data", paqueteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear paquete: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error interno al crear paquete turístico", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar paquetes turísticos", description = "Obtiene la lista de paquetes turísticos con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarPaquetes(
            @RequestParam(value = "empresaId", required = false) Long empresaId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean disponibles,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        try {
            if (page < 1 || size < 1) {
                throw new IllegalArgumentException("Los parámetros de paginación deben ser mayores que cero");
            }

            log.info("Listando paquetes de empresa {} con filtros", empresaId);

            List<PaqueteTuristico> paquetes;

            if (Boolean.TRUE.equals(disponibles)) {
                paquetes = paqueteService.findPaquetesDisponiblesByEmpresaId(empresaId);
            } else {
                paquetes = paqueteService.findByEmpresaIdAndBusqueda(empresaId, busqueda);
            }

            List<PaqueteResponse> paquetesResponse = paquetes.stream()
                .map(paqueteService::toResponse)
                .toList();

            int totalItems = paquetesResponse.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalItems);

            List<PaqueteResponse> paginatedData;
            if (startIndex >= totalItems) {
                paginatedData = List.of();
            } else {
                paginatedData = paquetesResponse.subList(startIndex, endIndex);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paquetes turísticos obtenidos exitosamente");
            response.put("data", paginatedData);
            response.put("total", totalItems);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) totalItems / size));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al listar paquetes: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al listar paquetes turísticos", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener paquete por ID", description = "Obtiene un paquete turístico específico por su ID")
    public ResponseEntity<Map<String, Object>> obtenerPaquete(@PathVariable Long id) {
        try {
            log.info("Obteniendo paquete turístico con ID: {}", id);

            PaqueteTuristico paquete = paqueteService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paquete turístico no encontrado"));

            PaqueteResponse paqueteResponse = paqueteService.toResponse(paquete);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paquete turístico obtenido exitosamente");
            response.put("data", paqueteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Paquete no encontrado: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error al obtener paquete con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar paquete turístico", description = "Actualiza los datos de un paquete turístico existente")
    public ResponseEntity<Map<String, Object>> actualizarPaquete(@PathVariable Long id, @Valid @RequestBody PaqueteTuristico paquete) {
        try {
            log.info("Actualizando paquete turístico ID: {}", id);

            PaqueteTuristico paqueteActualizado = paqueteService.update(id, paquete);
            PaqueteResponse paqueteResponse = paqueteService.toResponse(paqueteActualizado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paquete turístico actualizado exitosamente");
            response.put("data", paqueteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar paquete: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al actualizar paquete con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Eliminar paquete turístico", description = "Elimina un paquete turístico del sistema (soft delete)")
    public ResponseEntity<Map<String, Object>> eliminarPaquete(@PathVariable Long id) {
        try {
            log.info("Eliminando paquete turístico ID: {}", id);

            paqueteService.delete(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Paquete turístico eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al eliminar paquete: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al eliminar paquete con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/{id}/servicios")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Agregar servicio al paquete", description = "Agrega un servicio turístico a un paquete existente")
    public ResponseEntity<Map<String, Object>> agregarServicio(
            @PathVariable Long id,
            @RequestParam Long servicioId,
            @RequestParam(defaultValue = "1") Integer orden) {
        try {
            log.info("Agregando servicio {} al paquete {}", servicioId, id);

            PaqueteTuristico paqueteActualizado = paqueteService.agregarServicio(id, servicioId, orden);
            PaqueteResponse paqueteResponse = paqueteService.toResponse(paqueteActualizado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicio agregado al paquete exitosamente");
            response.put("data", paqueteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al agregar servicio al paquete: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al agregar servicio al paquete {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}/servicios/{servicioId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Remover servicio del paquete", description = "Remueve un servicio turístico de un paquete existente")
    public ResponseEntity<Map<String, Object>> removerServicio(
            @PathVariable Long id,
            @PathVariable Long servicioId) {
        try {
            log.info("Removiendo servicio {} del paquete {}", servicioId, id);

            PaqueteTuristico paqueteActualizado = paqueteService.removerServicio(id, servicioId);
            PaqueteResponse paqueteResponse = paqueteService.toResponse(paqueteActualizado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicio removido del paquete exitosamente");
            response.put("data", paqueteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al remover servicio del paquete: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al remover servicio del paquete {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}/precio")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Calcular precio del paquete", description = "Calcula el precio total del paquete con descuento aplicado")
    public ResponseEntity<Map<String, Object>> calcularPrecio(@PathVariable Long id) {
        try {
            log.info("Calculando precio del paquete {}", id);

            PaqueteTuristico paquete = paqueteService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paquete turístico no encontrado"));

            BigDecimal precioTotal = paquete.getPrecioTotal();
            BigDecimal precioFinal = paquete.getPrecioFinal();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Precio del paquete calculado exitosamente");
            response.put("data", Map.of(
                "paqueteId", id,
                "precioTotal", precioTotal,
                "descuento", paquete.getDescuento(),
                "precioFinal", precioFinal,
                "ahorro", precioTotal.subtract(precioFinal)
            ));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al calcular precio del paquete: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al calcular precio del paquete {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
