package com.sistema.turistico.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VentaRequest {

    @NotNull
    private Long reservaId;

    @NotNull
    private Long cajaId;

    @NotNull
    private Long usuarioId;

    private Long clienteId;

    private String metodoPago;

    private String numeroOperacion;

    private String comprobante;

    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal descuento = BigDecimal.ZERO;

    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal propina = BigDecimal.ZERO;

    private String observaciones;
}
