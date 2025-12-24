package com.sistema.turistico.controller;

import com.sistema.turistico.service.ReporteFinancieroService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/reportes/finanzas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reportes Financieros", description = "Resumenes de caja y ventas")
public class ReporteFinancieroController {

    private final ReporteFinancieroService reporteFinancieroService;

    @GetMapping("/caja-diaria")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Resumen diario de caja", description = "Obtiene totales de cajas por fecha para una empresa")
    public ResponseEntity<Map<String, Object>> obtenerResumenCajaDiaria(@RequestParam(required = false) Long empresaId,
                                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            Map<String, Object> data = reporteFinancieroService.resumenCajaDiario(empresaId, fecha);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Resumen de caja obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para resumen de caja: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener resumen de caja", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/impuestos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Resumen de impuestos", description = "Calcula impuestos estimados de las ventas en un periodo")
    public ResponseEntity<Map<String, Object>> obtenerResumenImpuestos(@RequestParam(required = false) Long empresaId,
                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                                                                        @RequestParam(required = false) BigDecimal porcentajeImpuesto) {
        try {
            Map<String, Object> data = reporteFinancieroService.resumenVentasImpuestos(empresaId, fechaInicio, fechaFin, porcentajeImpuesto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Resumen de impuestos obtenido correctamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida para resumen de impuestos: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener resumen de impuestos", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
