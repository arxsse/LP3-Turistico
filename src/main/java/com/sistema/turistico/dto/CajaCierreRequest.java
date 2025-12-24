package com.sistema.turistico.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CajaCierreRequest {

    private Long usuarioCierreId;

    @NotNull
    @DecimalMin(value = "0.00", message = "El monto de cierre no puede ser negativo")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal montoCierre;

    private String observaciones;
}
