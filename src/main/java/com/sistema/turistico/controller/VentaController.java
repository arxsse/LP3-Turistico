package com.sistema.turistico.controller;

import com.sistema.turistico.dto.VentaAnulacionRequest;
import com.sistema.turistico.dto.VentaRequest;
import com.sistema.turistico.dto.VentaResponse;
import com.sistema.turistico.entity.Venta;
import com.sistema.turistico.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ventas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ventas", description = "Emisión y gestión de ventas")
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Emitir venta", description = "Genera una venta asociada a una reserva pagada")
    public ResponseEntity<Map<String, Object>> emitirVenta(@Valid @RequestBody VentaRequest request) {
        try {
            VentaResponse venta = ventaService.emitirVenta(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta emitida correctamente");
            response.put("data", venta);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al emitir venta: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al emitir venta", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar ventas", description = "Lista las ventas con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarVentas(@RequestParam(value = "empresaId", required = false) Long empresaId,
                                                            @RequestParam(value = "idSucursal", required = false) Long idSucursal,
                                                            @RequestParam(value = "fechaDesde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                                            @RequestParam(value = "fechaHasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                                                            @RequestParam(value = "metodoPago", required = false) String metodoPago,
                                                            @RequestParam(value = "estado", required = false) Boolean estado) {
        try {
            LocalDateTime inicio = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
            LocalDateTime fin = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;
            if (inicio != null && fin != null && fin.isBefore(inicio)) {
                throw new IllegalArgumentException("El rango de fechas es inválido");
            }
            List<VentaResponse> ventas = ventaService.listarVentas(empresaId, idSucursal, inicio, fin, metodoPago, estado);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Ventas obtenidas correctamente");
            response.put("data", ventas);
            response.put("total", ventas.size());
            Map<String, Object> filtros = new HashMap<>();
            filtros.put("empresaId", empresaId);
            filtros.put("idSucursal", idSucursal);
            filtros.put("fechaDesde", fechaDesde);
            filtros.put("fechaHasta", fechaHasta);
            filtros.put("metodoPago", metodoPago);
            filtros.put("estado", estado);
            response.put("filtros", filtros);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al listar ventas: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al listar ventas", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Detalle de venta", description = "Obtiene la información de una venta específica")
    public ResponseEntity<Map<String, Object>> obtenerVenta(@PathVariable Long id) {
        try {
            VentaResponse venta = ventaService.obtenerVenta(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta obtenida correctamente");
            response.put("data", venta);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Venta no encontrada: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener venta", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar venta", description = "Actualiza información de una venta (solo campos permitidos)")
    public ResponseEntity<Map<String, Object>> actualizarVenta(@PathVariable Long id,
                                                              @Valid @RequestBody VentaRequest request) {
        try {
            VentaResponse venta = ventaService.actualizarVenta(id, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta actualizada correctamente");
            response.put("data", venta);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al actualizar venta: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al actualizar venta", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Eliminar venta", description = "Elimina una venta (solo ventas del día actual)")
    public ResponseEntity<Map<String, Object>> eliminarVenta(@PathVariable Long id,
                                                            @RequestParam(value = "cajaId") Long cajaId,
                                                            @RequestParam(value = "motivo", required = false) String motivo) {
        try {
            ventaService.eliminarVenta(id, cajaId, motivo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta eliminada correctamente");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al eliminar venta: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al eliminar venta", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Anular venta", description = "Anula una venta y genera el movimiento de reverso")
    public ResponseEntity<Map<String, Object>> anularVenta(@PathVariable Long id,
                                                            @Valid @RequestBody VentaAnulacionRequest request) {
        try {
            VentaResponse venta = ventaService.anularVenta(id, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Venta anulada correctamente");
            response.put("data", venta);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al anular venta: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al anular venta", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/siguiente-numero")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener siguiente numeración", description = "Sugiere la siguiente numeración para ventas del día")
    public ResponseEntity<Map<String, Object>> obtenerSiguienteNumero(@RequestParam(value = "empresaId", required = false) Long empresaId) {
        try {
            Map<String, String> numeracion = ventaService.obtenerProximaNumeracion(empresaId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Numeración obtenida correctamente");
            response.put("data", numeracion);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al obtener siguiente numeración: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener siguiente numeración", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
