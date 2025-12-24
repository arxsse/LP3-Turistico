package com.sistema.turistico.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CajaAperturaRequest {

    private Long empresaId;

    @NotNull
    private Long sucursalId;

    private Long usuarioAperturaId;

    @NotNull
    @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal montoInicial;

    private String observaciones;
}
