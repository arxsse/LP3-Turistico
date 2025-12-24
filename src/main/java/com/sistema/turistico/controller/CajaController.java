package com.sistema.turistico.controller;

import com.sistema.turistico.dto.CajaAperturaRequest;
import com.sistema.turistico.dto.CajaCierreRequest;
import com.sistema.turistico.dto.MovimientoCajaRequest;
import com.sistema.turistico.entity.Caja;
import com.sistema.turistico.entity.MovimientoCaja;
import com.sistema.turistico.service.CajaService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cajas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Caja", description = "Gestión de cajas y movimientos")
public class CajaController {

    private final CajaService cajaService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Abrir caja", description = "Crea una nueva apertura de caja")
    public ResponseEntity<Map<String, Object>> abrirCaja(@Valid @RequestBody CajaAperturaRequest request) {
        try {
            Caja caja = cajaService.abrirCaja(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Caja aperturada correctamente");
            response.put("data", buildCajaData(caja));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al aperturar caja: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al aperturar caja", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar cajas", description = "Obtiene todas las cajas con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarCajas(@RequestParam(required = false) Long empresaId,
                                                           @RequestParam(required = false) Long sucursalId,
                                                           @RequestParam(required = false) Caja.EstadoCaja estado) {
        try {
            List<Caja> cajas = cajaService.listarCajas(empresaId, sucursalId, estado);
            List<Map<String, Object>> cajasData = cajas.stream()
                .map(this::buildCajaData)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cajas obtenidas correctamente");
            response.put("data", cajasData);
            response.put("total", cajas.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar cajas", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/abiertas")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar cajas abiertas", description = "Obtiene las cajas abiertas por empresa")
    public ResponseEntity<Map<String, Object>> listarCajasAbiertas(@RequestParam(required = false) Long empresaId) {
        try {
            List<Caja> cajas = cajaService.listarCajasAbiertas(empresaId);
            List<Map<String, Object>> cajasData = cajas.stream()
                .map(this::buildCajaData)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cajas abiertas obtenidas correctamente");
            response.put("data", cajasData);
            response.put("total", cajas.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar cajas abiertas", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Detalle de caja", description = "Obtiene la información de una caja específica")
    public ResponseEntity<Map<String, Object>> obtenerCaja(@PathVariable Long id) {
        try {
            Caja caja = cajaService.obtenerCaja(id);
            Map<String, Object> cajaData = buildCajaData(caja);
            cajaData.put("idUsuarioCierre", caja.getUsuarioCierre() != null ? caja.getUsuarioCierre().getIdUsuario() : null);
            cajaData.put("montoCierre", caja.getMontoCierre());
            cajaData.put("diferencia", caja.getDiferencia());
            cajaData.put("createdAt", caja.getCreatedAt());
            cajaData.put("updatedAt", caja.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Caja obtenida correctamente");
            response.put("data", cajaData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Caja no encontrada: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener caja", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Cerrar caja", description = "Registra el cierre de una caja abierta")
    public ResponseEntity<Map<String, Object>> cerrarCaja(@PathVariable Long id, @Valid @RequestBody CajaCierreRequest request) {
        try {
            Caja caja = cajaService.cerrarCaja(id, request);
            Map<String, Object> cajaData = buildCajaData(caja);
            cajaData.put("idUsuarioCierre", caja.getUsuarioCierre() != null ? caja.getUsuarioCierre().getIdUsuario() : null);
            cajaData.put("montoCierre", caja.getMontoCierre());
            cajaData.put("diferencia", caja.getDiferencia());
            cajaData.put("createdAt", caja.getCreatedAt());
            cajaData.put("updatedAt", caja.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Caja cerrada correctamente");
            response.put("data", cajaData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al cerrar caja: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al cerrar caja", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Registrar movimiento", description = "Agrega un movimiento manual a la caja")
    public ResponseEntity<Map<String, Object>> registrarMovimiento(@PathVariable Long id,
                                                                    @Valid @RequestBody MovimientoCajaRequest request) {
        try {
            MovimientoCaja movimiento = cajaService.registrarMovimiento(id, request);
            Map<String, Object> movimientoData = buildMovimientoData(movimiento);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Movimiento registrado correctamente");
            response.put("data", movimientoData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al registrar movimiento: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al registrar movimiento", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar movimientos", description = "Lista los movimientos de una caja con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarMovimientos(@PathVariable Long id,
        @RequestParam(required = false) MovimientoCaja.TipoMovimiento tipo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        try {
            List<MovimientoCaja> movimientos = cajaService.obtenerMovimientos(id, fechaInicio, fechaFin, tipo);
            List<Map<String, Object>> movimientosData = movimientos.stream()
                .map(this::buildMovimientoData)
                .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Movimientos obtenidos correctamente");
            response.put("data", movimientosData);
            response.put("total", movimientos.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al listar movimientos: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al listar movimientos", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}/movimientos/{movimientoId}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Detalle de movimiento", description = "Obtiene la información de un movimiento específico de caja")
    public ResponseEntity<Map<String, Object>> obtenerMovimiento(@PathVariable Long id, @PathVariable Long movimientoId) {
        try {
            MovimientoCaja movimiento = cajaService.obtenerMovimiento(id, movimientoId);
            Map<String, Object> movimientoData = buildMovimientoData(movimiento);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Movimiento obtenido correctamente");
            response.put("data", movimientoData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Movimiento no encontrado: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener movimiento", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}/movimientos/{movimientoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Anular movimiento", description = "Anula un movimiento de caja (solo movimientos manuales)")
    public ResponseEntity<Map<String, Object>> anularMovimiento(@PathVariable Long id,
                                                               @PathVariable Long movimientoId,
                                                               @RequestParam String motivo) {
        try {
            MovimientoCaja movimientoReverso = cajaService.anularMovimiento(id, movimientoId, motivo);
            Map<String, Object> movimientoData = buildMovimientoData(movimientoReverso);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Movimiento anulado correctamente");
            response.put("data", movimientoData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al anular movimiento: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al anular movimiento", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}/arqueo")
    private Map<String, Object> buildCajaData(Caja caja) {
        Map<String, Object> data = new HashMap<>();
        data.put("idCaja", caja.getIdCaja());
        data.put("idEmpresa", caja.getEmpresa() != null ? caja.getEmpresa().getIdEmpresa() : null);
        data.put("idSucursal", caja.getSucursal() != null ? caja.getSucursal().getIdSucursal() : null);
        data.put("idUsuarioApertura", caja.getUsuarioApertura() != null ? caja.getUsuarioApertura().getIdUsuario() : null);
        data.put("fechaApertura", caja.getFechaApertura());
        data.put("horaApertura", caja.getHoraApertura());
        data.put("montoInicial", caja.getMontoInicial());
        data.put("saldoActual", caja.getSaldoActual());
        data.put("estado", caja.getEstado());
        data.put("observaciones", caja.getObservaciones());
        return data;
    }

    private Map<String, Object> buildMovimientoData(MovimientoCaja movimiento) {
        Map<String, Object> data = new HashMap<>();
        data.put("idMovimiento", movimiento.getIdMovimiento());
        data.put("idCaja", movimiento.getCaja() != null ? movimiento.getCaja().getIdCaja() : null);
        data.put("tipoMovimiento", movimiento.getTipoMovimiento());
        data.put("monto", movimiento.getMonto());
        data.put("descripcion", movimiento.getDescripcion());
        data.put("fechaHora", movimiento.getFechaHora());
        data.put("createdAt", movimiento.getCreatedAt());
        return data;
    }
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Arqueo de caja", description = "Obtiene el resumen de ingresos y egresos de una caja")
    public ResponseEntity<Map<String, Object>> obtenerArqueo(@PathVariable Long id,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            Map<String, Object> resumen = cajaService.obtenerArqueo(id, fechaInicio, fechaFin);
            resumen.put("success", true);
            resumen.put("message", "Arqueo generado correctamente");
            return ResponseEntity.ok(resumen);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al generar arqueo: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al generar arqueo", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
