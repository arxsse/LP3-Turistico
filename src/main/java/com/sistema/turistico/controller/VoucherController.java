package com.sistema.turistico.controller;

import com.sistema.turistico.dto.VoucherResponse;
import com.sistema.turistico.dto.VoucherUpdateRequest;
import com.sistema.turistico.entity.Voucher;
import com.sistema.turistico.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vouchers", description = "Endpoints para gestión de vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Listar vouchers", description = "Obtiene la lista de vouchers con filtros opcionales")
    public ResponseEntity<Map<String, Object>> listarVouchers(
            @RequestParam(required = false) Long empresaId,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Voucher.EstadoVoucher estado,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        try {
            log.info("Listando vouchers con filtros - empresa: {}, página: {}, tamaño: {}", empresaId, page, size);

            VoucherService.VoucherPageResult result = voucherService.listarVouchers(empresaId, busqueda, estado, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vouchers obtenidos exitosamente");
            response.put("data", result.data());
            response.put("total", result.total());
            response.put("page", result.page());
            response.put("size", result.size());
            response.put("totalPages", result.totalPages());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            log.warn("Solicitud inválida al listar vouchers: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error al listar vouchers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/{codigoQr}")
    @Operation(summary = "Obtener voucher por QR", description = "Obtiene un voucher específico por su código QR")
    public ResponseEntity<Map<String, Object>> obtenerVoucherPorQr(@PathVariable String codigoQr) {
        try {
            log.info("Obteniendo voucher con código QR: {}", codigoQr);

            VoucherResponse voucherResponse = voucherService.obtenerPorCodigoQr(codigoQr);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voucher obtenido exitosamente");
            response.put("data", voucherResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Voucher no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error al obtener voucher con código QR: {}", codigoQr, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PutMapping("/{codigoQr}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Actualizar voucher", description = "Actualiza el estado de un voucher específico")
    public ResponseEntity<Map<String, Object>> actualizarVoucher(@PathVariable String codigoQr,
                                                                 @Valid @RequestBody VoucherUpdateRequest request) {
        try {
            log.info("Actualizando voucher con código QR: {}", codigoQr);

            VoucherResponse voucherResponse = voucherService.actualizarEstado(codigoQr, request.getEstado());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voucher actualizado exitosamente");
            response.put("data", voucherResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al actualizar voucher: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al actualizar voucher con código QR: {}", codigoQr, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/{codigoQr}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMINISTRADOR', 'SUPERADMINISTRADOR', 'GERENTE')")
    @Operation(summary = "Cancelar voucher", description = "Cancela un voucher cambiando su estado a 'Cancelado'")
    public ResponseEntity<Map<String, Object>> cancelarVoucher(@PathVariable String codigoQr) {
        try {
            log.info("Cancelando voucher con código QR: {}", codigoQr);

            VoucherResponse voucherResponse = voucherService.cancelar(codigoQr);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voucher cancelado exitosamente");
            response.put("data", voucherResponse);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error al cancelar voucher: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("Error al cancelar voucher con código QR: {}", codigoQr, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}
