package com.sistema.turistico.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PagoReservaRequest {

    private Long usuarioId;

    @NotNull
    private Long cajaId;

    @NotNull
    @DecimalMin(value = "0.01", message = "El monto pagado debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal montoPagado;

    @NotBlank
    private String metodoPago;

    private String numeroOperacion;

    private String comprobante;

    private LocalDate fechaPago;

    private String observaciones;
}
