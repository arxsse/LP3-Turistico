package com.sistema.turistico.controller;

import com.sistema.turistico.dto.ServicioResponse;
import com.sistema.turistico.entity.ServicioTuristico;
import com.sistema.turistico.service.ServicioTuristicoService;
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
@RequestMapping("/servicios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Servicios Turísticos", description = "Endpoints para gestión de servicios turísticos")
public class ServicioTuristicoController {

    private final ServicioTuristicoService servicioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear servicio turístico", description = "Crea un nuevo servicio turístico en el sistema")
    public ResponseEntity<Map<String, Object>> crearServicio(@Valid @RequestBody ServicioTuristico servicio) {
        try {
            log.info("Creando servicio turístico: {}", servicio.getNombreServicio());

            ServicioTuristico nuevoServicio = servicioService.create(servicio);
            ServicioResponse servicioResponse = servicioService.toResponse(nuevoServicio);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicio turístico creado exitosamente");
            response.put("data", servicioResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear servicio: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error interno al crear servicio turístico", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar servicios turísticos", description = "Obtiene la lista de servicios turísticos con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarServicios(
            @RequestParam(value = "empresaId", required = false) Long empresaId,
            @RequestParam(value = "sucursalId", required = false) Long sucursalId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) ServicioTuristico.TipoServicio tipoServicio,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        try {
            if (page < 1 || size < 1) {
                throw new IllegalArgumentException("Los parámetros de paginación deben ser mayores que cero");
            }

            log.info("Listando servicios de empresa {} y sucursal {} con filtros - página: {}, tamaño: {}", empresaId, sucursalId, page, size);

            List<ServicioTuristico> servicios;

            if (tipoServicio != null) {
                servicios = servicioService.findByEmpresaIdAndTipoServicio(empresaId, tipoServicio, sucursalId);
            } else if (precioMin != null && precioMax != null) {
                servicios = servicioService.findByEmpresaIdAndPrecioBetween(empresaId, precioMin, precioMax, sucursalId);
            } else {
                servicios = servicioService.findByEmpresaIdAndBusqueda(empresaId, busqueda, sucursalId);
            }

            int totalItems = servicios.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalItems);

            List<ServicioTuristico> serviciosPaginados;
            if (startIndex >= totalItems) {
                serviciosPaginados = List.of();
            } else {
                serviciosPaginados = servicios.subList(startIndex, endIndex);
            }

            List<ServicioResponse> serviciosResponse = serviciosPaginados.stream()
                .map(servicioService::toResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicios turísticos obtenidos exitosamente");
            response.put("data", serviciosResponse);
            response.put("total", totalItems);
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) totalItems / size));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al listar servicios: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al listar servicios turísticos", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar servicios disponibles", description = "Obtiene servicios disponibles para un número de personas")
    public ResponseEntity<Map<String, Object>> listarServiciosDisponibles(
            @RequestParam(value = "empresaId", required = false) Long empresaId,
            @RequestParam(value = "sucursalId", required = false) Long sucursalId,
            @RequestParam Integer personas) {
        try {
            log.info("Buscando servicios disponibles para {} personas en empresa {} y sucursal {}", personas, empresaId, sucursalId);

            List<ServicioTuristico> servicios = servicioService.findDisponiblesByEmpresaIdAndPersonas(empresaId, personas, sucursalId);

            // Convertir a ServicioResponse
            List<ServicioResponse> serviciosResponse = servicios.stream()
                .map(servicioService::toResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicios disponibles obtenidos exitosamente");
            response.put("data", serviciosResponse);
            response.put("total", serviciosResponse.size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al listar servicios disponibles: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al listar servicios disponibles", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener servicio por ID", description = "Obtiene un servicio turístico específico por su ID")
    public ResponseEntity<Map<String, Object>> obtenerServicio(@PathVariable Long id) {
        try {
            log.info("Obteniendo servicio turístico con ID: {}", id);

            ServicioTuristico servicio = servicioService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Servicio turístico no encontrado"));

            ServicioResponse servicioResponse = servicioService.toResponse(servicio);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicio turístico obtenido exitosamente");
            response.put("data", servicioResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Servicio no encontrado: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error al obtener servicio con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar servicio turístico", description = "Actualiza los datos de un servicio turístico existente")
    public ResponseEntity<Map<String, Object>> actualizarServicio(@PathVariable Long id, @Valid @RequestBody ServicioTuristico servicio) {
        try {
            log.info("Actualizando servicio turístico ID: {}", id);

            ServicioTuristico servicioActualizado = servicioService.update(id, servicio);
            ServicioResponse servicioResponse = servicioService.toResponse(servicioActualizado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicio turístico actualizado exitosamente");
            response.put("data", servicioResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar servicio: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al actualizar servicio con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Eliminar servicio turístico", description = "Elimina un servicio turístico del sistema (soft delete)")
    public ResponseEntity<Map<String, Object>> eliminarServicio(@PathVariable Long id) {
        try {
            log.info("Eliminando servicio turístico ID: {}", id);

            servicioService.delete(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Servicio turístico eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al eliminar servicio: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al eliminar servicio con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}/precio")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Calcular precio total", description = "Calcula el precio total de un servicio para un número de personas")
    public ResponseEntity<Map<String, Object>> calcularPrecio(
            @PathVariable Long id,
            @RequestParam Integer personas) {
        try {
            log.info("Calculando precio para servicio {} y {} personas", id, personas);

            BigDecimal precioTotal = servicioService.calcularPrecioTotal(id, personas);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Precio calculado exitosamente");
            response.put("data", Map.of(
                "servicioId", id,
                "personas", personas,
                "precioTotal", precioTotal
            ));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al calcular precio: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al calcular precio para servicio {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
