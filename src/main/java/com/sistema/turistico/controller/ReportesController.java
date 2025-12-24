package com.sistema.turistico.controller;

import com.sistema.turistico.service.ReportesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reportes Generales", description = "Reportes de reservas, ventas, clientes, personal y servicios")
public class ReportesController {

    private final ReportesService reportesService;

    @GetMapping("/reservas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Reporte de reservas", description = "Obtiene estadísticas de reservas por empresa en un período")
    public ResponseEntity<Map<String, Object>> reporteReservas(@RequestParam(required = false) Long empresaId,
                                                               @RequestParam(required = false) Long idSucursal,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                                                               @RequestParam(required = false) String estado) {
        try {
            Map<String, Object> data = reportesService.reporteReservas(empresaId, idSucursal, fechaInicio, fechaFin, estado);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reporte de reservas obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para reporte de reservas: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener reporte de reservas", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/ventas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Reporte de ventas", description = "Obtiene estadísticas de ventas por empresa en un período")
    public ResponseEntity<Map<String, Object>> reporteVentas(@RequestParam(required = false) Long empresaId,
                                                             @RequestParam(required = false) Long idSucursal,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            Map<String, Object> data = reportesService.reporteVentas(empresaId, idSucursal, fechaInicio, fechaFin);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reporte de ventas obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para reporte de ventas: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener reporte de ventas", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Reporte de clientes", description = "Obtiene estadísticas de clientes por empresa")
    public ResponseEntity<Map<String, Object>> reporteClientes(@RequestParam(required = false) Long empresaId,
                                                              @RequestParam(required = false) Long idSucursal) {
        try {
            Map<String, Object> data = reportesService.reporteClientes(empresaId, idSucursal);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reporte de clientes obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para reporte de clientes: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener reporte de clientes", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/personal")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Reporte de personal", description = "Obtiene estadísticas de personal por empresa")
    public ResponseEntity<Map<String, Object>> reportePersonal(@RequestParam(required = false) Long empresaId,
                                                              @RequestParam(required = false) Long idSucursal) {
        try {
            Map<String, Object> data = reportesService.reportePersonal(empresaId, idSucursal);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reporte de personal obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para reporte de personal: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener reporte de personal", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/servicios")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Reporte de servicios turísticos", description = "Obtiene estadísticas de servicios por empresa")
    public ResponseEntity<Map<String, Object>> reporteServicios(@RequestParam(required = false) Long empresaId,
                                                               @RequestParam(required = false) Long idSucursal) {
        try {
            Map<String, Object> data = reportesService.reporteServicios(empresaId, idSucursal);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reporte de servicios obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para reporte de servicios: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener reporte de servicios", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/paquetes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Reporte de paquetes turísticos", description = "Obtiene estadísticas de paquetes por empresa")
    public ResponseEntity<Map<String, Object>> reportePaquetes(@RequestParam(required = false) Long empresaId,
                                                              @RequestParam(required = false) Long idSucursal) {
        try {
            Map<String, Object> data = reportesService.reportePaquetes(empresaId, idSucursal);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reporte de paquetes obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para reporte de paquetes: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener reporte de paquetes", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
