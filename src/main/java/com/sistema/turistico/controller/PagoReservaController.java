package com.sistema.turistico.controller;

import com.sistema.turistico.dto.PagoReservaRequest;
import com.sistema.turistico.entity.PagoReserva;
import com.sistema.turistico.service.PagoReservaService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagos de Reserva", description = "Gestión de pagos y devoluciones")
public class PagoReservaController {

    private final PagoReservaService pagoReservaService;

    @PostMapping("/reservas/{reservaId}/pagos")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Registrar pago", description = "Registra un pago parcial o total para una reserva")
    public ResponseEntity<Map<String, Object>> registrarPago(@PathVariable Long reservaId,
                                                             @Valid @RequestBody PagoReservaRequest request) {
        try {
            PagoReserva pago = pagoReservaService.registrarPago(reservaId, request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pago registrado correctamente");
            Map<String, Object> pagoData = new HashMap<>();
            pagoData.put("idPago", pago.getIdPago());
            pagoData.put("idReserva", pago.getReserva().getIdReserva());
            pagoData.put("idUsuario", pago.getUsuario().getIdUsuario());
            pagoData.put("montoPagado", pago.getMontoPagado());
            pagoData.put("metodoPago", pago.getMetodoPago());
            pagoData.put("numeroOperacion", pago.getNumeroOperacion());
            pagoData.put("comprobante", pago.getComprobante());
            pagoData.put("fechaPago", pago.getFechaPago());
            pagoData.put("observaciones", pago.getObservaciones());
            pagoData.put("estado", pago.getEstado());
            pagoData.put("createdAt", pago.getCreatedAt());
            pagoData.put("updatedAt", pago.getUpdatedAt());

            response.put("data", pagoData);
            response.put("saldoPendiente", pagoReservaService.obtenerSaldoPendiente(reservaId));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al registrar pago: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al registrar pago", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/reservas/{reservaId}/pagos")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar pagos de reserva", description = "Obtiene el historial de pagos de una reserva")
    public ResponseEntity<Map<String, Object>> listarPagosReserva(@PathVariable Long reservaId) {
        try {
            List<PagoReserva> pagos = pagoReservaService.listarPagosReserva(reservaId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pagos obtenidos correctamente");
            List<Map<String, Object>> pagosData = pagos.stream()
                .map(pago -> {
                    Map<String, Object> pagoData = new HashMap<>();
                    pagoData.put("idPago", pago.getIdPago());
                    pagoData.put("idReserva", pago.getReserva().getIdReserva());
                    pagoData.put("idUsuario", pago.getUsuario().getIdUsuario());
                    pagoData.put("montoPagado", pago.getMontoPagado());
                    pagoData.put("metodoPago", pago.getMetodoPago());
                    pagoData.put("numeroOperacion", pago.getNumeroOperacion());
                    pagoData.put("comprobante", pago.getComprobante());
                    pagoData.put("fechaPago", pago.getFechaPago());
                    pagoData.put("observaciones", pago.getObservaciones());
                    pagoData.put("estado", pago.getEstado());
                    pagoData.put("createdAt", pago.getCreatedAt());
                    pagoData.put("updatedAt", pago.getUpdatedAt());
                    return pagoData;
                })
                .toList();

            response.put("data", pagosData);
            response.put("total", pagos.size());
            response.put("saldoPendiente", pagoReservaService.obtenerSaldoPendiente(reservaId));
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar pagos de reserva", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/reservas/{reservaId}/pagos/{pagoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Anular pago", description = "Anula un pago previamente registrado")
    public ResponseEntity<Map<String, Object>> anularPago(@PathVariable Long reservaId,
                                                           @PathVariable Long pagoId,
                                                           @RequestParam Long cajaId,
                                                           @RequestParam(required = false) String motivo) {
        try {
            PagoReserva pago = pagoReservaService.anularPago(reservaId, pagoId, cajaId, motivo);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pago anulado correctamente");
            Map<String, Object> pagoData = new HashMap<>();
            pagoData.put("idPago", pago.getIdPago());
            pagoData.put("idReserva", pago.getReserva().getIdReserva());
            pagoData.put("idUsuario", pago.getUsuario().getIdUsuario());
            pagoData.put("montoPagado", pago.getMontoPagado());
            pagoData.put("metodoPago", pago.getMetodoPago());
            pagoData.put("numeroOperacion", pago.getNumeroOperacion());
            pagoData.put("comprobante", pago.getComprobante());
            pagoData.put("fechaPago", pago.getFechaPago());
            pagoData.put("observaciones", pago.getObservaciones());
            pagoData.put("estado", pago.getEstado());
            pagoData.put("createdAt", pago.getCreatedAt());
            pagoData.put("updatedAt", pago.getUpdatedAt());

            response.put("data", pagoData);
            response.put("saldoPendiente", pagoReservaService.obtenerSaldoPendiente(reservaId));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Error al anular pago: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error inesperado al anular pago", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/pagos/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Obtener pago específico", description = "Obtiene un pago por su ID")
    public ResponseEntity<Map<String, Object>> obtenerPago(@PathVariable Long id) {
        try {
            PagoReserva pago = pagoReservaService.obtenerPago(id);
            Map<String, Object> pagoData = new HashMap<>();
            pagoData.put("idPago", pago.getIdPago());
            pagoData.put("idReserva", pago.getReserva().getIdReserva());
            pagoData.put("idUsuario", pago.getUsuario().getIdUsuario());
            pagoData.put("montoPagado", pago.getMontoPagado());
            pagoData.put("metodoPago", pago.getMetodoPago());
            pagoData.put("numeroOperacion", pago.getNumeroOperacion());
            pagoData.put("comprobante", pago.getComprobante());
            pagoData.put("fechaPago", pago.getFechaPago());
            pagoData.put("observaciones", pago.getObservaciones());
            pagoData.put("estado", pago.getEstado());
            pagoData.put("createdAt", pago.getCreatedAt());
            pagoData.put("updatedAt", pago.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pago obtenido correctamente");
            response.put("data", pagoData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Pago no encontrado: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            log.error("Error al obtener pago", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/pagos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar pago", description = "Actualiza información de un pago")
    public ResponseEntity<Map<String, Object>> actualizarPago(@PathVariable Long id,
                                                             @Valid @RequestBody PagoReservaRequest request) {
        try {
            PagoReserva pago = pagoReservaService.actualizarPago(id, request);
            Map<String, Object> pagoData = new HashMap<>();
            pagoData.put("idPago", pago.getIdPago());
            pagoData.put("idReserva", pago.getReserva().getIdReserva());
            pagoData.put("idUsuario", pago.getUsuario().getIdUsuario());
            pagoData.put("montoPagado", pago.getMontoPagado());
            pagoData.put("metodoPago", pago.getMetodoPago());
            pagoData.put("numeroOperacion", pago.getNumeroOperacion());
            pagoData.put("comprobante", pago.getComprobante());
            pagoData.put("fechaPago", pago.getFechaPago());
            pagoData.put("observaciones", pago.getObservaciones());
            pagoData.put("estado", pago.getEstado());
            pagoData.put("createdAt", pago.getCreatedAt());
            pagoData.put("updatedAt", pago.getUpdatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pago actualizado correctamente");
            response.put("data", pagoData);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar pago: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al actualizar pago", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/pagos")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar pagos", description = "Listar pagos aplicando filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarPagos(@RequestParam(required = false) Long empresaId,
                                                             @RequestParam(required = false) Long sucursalId,
                                                             @RequestParam(required = false) String metodoPago,
                                                             @RequestParam(required = false) Boolean estado,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        try {
            List<PagoReserva> pagos = pagoReservaService.listarPagosPorFiltros(empresaId, sucursalId, metodoPago, estado, fechaDesde, fechaHasta);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pagos obtenidos correctamente");
            List<Map<String, Object>> pagosData = pagos.stream()
                .map(pago -> {
                    Map<String, Object> pagoData = new HashMap<>();
                    pagoData.put("idPago", pago.getIdPago());
                    pagoData.put("idReserva", pago.getReserva().getIdReserva());
                    pagoData.put("idUsuario", pago.getUsuario().getIdUsuario());
                    pagoData.put("montoPagado", pago.getMontoPagado());
                    pagoData.put("metodoPago", pago.getMetodoPago());
                    pagoData.put("numeroOperacion", pago.getNumeroOperacion());
                    pagoData.put("comprobante", pago.getComprobante());
                    pagoData.put("fechaPago", pago.getFechaPago());
                    pagoData.put("observaciones", pago.getObservaciones());
                    pagoData.put("estado", pago.getEstado());
                    pagoData.put("createdAt", pago.getCreatedAt());
                    pagoData.put("updatedAt", pago.getUpdatedAt());
                    return pagoData;
                })
                .toList();

            response.put("data", pagosData);
            response.put("total", pagos.size());
            Map<String, Object> filtros = new HashMap<>();
            filtros.put("empresaId", empresaId);
            filtros.put("metodoPago", metodoPago);
            filtros.put("estado", estado);
            filtros.put("fechaDesde", fechaDesde);
            filtros.put("fechaHasta", fechaHasta);
            response.put("filtros", filtros);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al listar pagos: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al listar pagos", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
