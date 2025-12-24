package com.sistema.turistico.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReservaRequest {

    private Long empresaId;

    private Long idSucursal;

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    private Long promocionId;

    @Size(max = 50, message = "El código de reserva no puede exceder 50 caracteres")
    private String codigoReserva;

    @NotBlank(message = "La fecha de servicio es obligatoria")
    private String fechaServicio;

    private String fechaReserva;

    private Integer numeroPersonas;

    private BigDecimal descuentoAplicado;

    private String observaciones;

    @Valid
    @NotNull(message = "Debe especificar los items de la reserva")
    @Size(min = 1, message = "Debe registrar al menos un item")
    private List<ReservaItemRequest> items;

    @Valid
    @Size(max = 50, message = "No se pueden registrar más de 50 asignaciones")
    private List<ReservaAsignacionPayload> asignaciones;
}
