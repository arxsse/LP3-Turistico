package com.sistema.turistico.controller;

import com.sistema.turistico.dto.ClienteResponse;
import com.sistema.turistico.entity.Cliente;
import com.sistema.turistico.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clientes", description = "Endpoints para gestión de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Crear cliente", description = "Crea un nuevo cliente en el sistema")
    public ResponseEntity<Map<String, Object>> crearCliente(@Valid @RequestBody Cliente cliente) {
        try {
            log.info("Creando cliente: {} {}", cliente.getNombre(), cliente.getApellido());

            Cliente nuevoCliente = clienteService.create(cliente);
            ClienteResponse clienteResponse = clienteService.toResponse(nuevoCliente);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente creado exitosamente");
            response.put("data", clienteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al crear cliente: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error interno al crear cliente", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar clientes", description = "Obtiene la lista de clientes con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarClientes(
            @RequestParam(value = "empresaId", required = false) Long empresaId,
            @RequestParam(value = "busqueda", required = false) String busqueda,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        try {
            log.info("Listando clientes de empresa {} con búsqueda: {} - página: {}, tamaño: {}", empresaId, busqueda, page, size);

            List<Cliente> clientes = clienteService.findByEmpresaIdAndBusqueda(empresaId, busqueda);

            if (page < 1) {
                page = 1;
            }
            if (size < 1) {
                size = 10;
            }

            int startIndex = Math.min((page - 1) * size, clientes.size());
            int endIndex = Math.min(startIndex + size, clientes.size());

            List<Cliente> clientesPaginados = clientes.isEmpty() ? Collections.emptyList() : clientes.subList(startIndex, endIndex);

            // Convertir a ClienteResponse
            List<ClienteResponse> clientesResponse = clientesPaginados.stream()
                .map(clienteService::toResponse)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Clientes obtenidos exitosamente");
            response.put("data", clientesResponse);
            response.put("total", clientes.size());
            response.put("page", page);
            response.put("size", size);
            response.put("totalPages", (int) Math.ceil((double) clientes.size() / size));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al listar clientes", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener cliente por ID", description = "Obtiene un cliente específico por su ID")
    public ResponseEntity<Map<String, Object>> obtenerCliente(@PathVariable Long id) {
        try {
            log.info("Obteniendo cliente con ID: {}", id);

            Cliente cliente = clienteService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

            ClienteResponse clienteResponse = clienteService.toResponse(cliente);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente obtenido exitosamente");
            response.put("data", clienteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Cliente no encontrado: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);

        } catch (Exception e) {
            log.error("Error al obtener cliente con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente existente")
    public ResponseEntity<Map<String, Object>> actualizarCliente(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {
        try {
            log.info("Actualizando cliente ID: {}", id);

            Cliente clienteActualizado = clienteService.update(id, cliente);
            ClienteResponse clienteResponse = clienteService.toResponse(clienteActualizado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente actualizado exitosamente");
            response.put("data", clienteResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar cliente: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al actualizar cliente con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Eliminar cliente", description = "Elimina un cliente del sistema (soft delete)")
    public ResponseEntity<Map<String, Object>> eliminarCliente(@PathVariable Long id) {
        try {
            log.info("Eliminando cliente ID: {}", id);

            clienteService.delete(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente eliminado exitosamente");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al eliminar cliente: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al eliminar cliente con ID: {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
