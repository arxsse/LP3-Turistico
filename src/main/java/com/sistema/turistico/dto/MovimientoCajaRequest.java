package com.sistema.turistico.dto;

import com.sistema.turistico.entity.MovimientoCaja;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MovimientoCajaRequest {

    @NotNull
    private MovimientoCaja.TipoMovimiento tipoMovimiento;

    @NotNull
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal monto;

    private String descripcion;

    private Long ventaId;
}
