package com.sistema.turistico.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {

    private Long idVenta;
    private Long empresaId;
    private Long clienteId;
    private Long reservaId;
    private Long usuarioId;
    private Long cajaId;
    private Long sucursalId;
    private String sucursalNombre;
    private LocalDateTime fechaHora;
    private BigDecimal montoTotal;
    private String metodoPago;
    private String numeroOperacion;
    private String comprobante;
    private BigDecimal descuento;
    private BigDecimal propina;
    private String observaciones;
    private Boolean estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}