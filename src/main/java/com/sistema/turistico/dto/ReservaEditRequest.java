package com.sistema.turistico.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReservaEditRequest {

    private Long empresaId;

    @NotBlank(message = "La fecha de servicio es obligatoria")
    private String fechaServicio;

    private Integer numeroPersonas;

    @DecimalMin(value = "0.00", message = "El descuento aplicado no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El descuento debe tener máximo 10 dígitos enteros y 2 decimales")
    private BigDecimal descuentoAplicado;

    private String observaciones;

    @Valid
    @NotNull(message = "Debe especificar los items de la reserva")
    @Size(min = 1, message = "Debe registrar al menos un item")
    private List<ReservaItemRequest> items;

    @Valid
    @Size(max = 50, message = "No se pueden registrar más de 50 asignaciones")
    private List<ReservaAsignacionPayload> asignaciones;

    /**
     * Indicador para forzar la sincronización del personal asignado incluso cuando la lista llegue vacía.
     * Permite diferenciar entre "no tocar las asignaciones" (valor nulo o false) y "eliminar todas" (true con lista vacía).
     */
    private Boolean sincronizarAsignaciones;
}
