package com.sistema.turistico.dto;

import com.sistema.turistico.entity.ReservaItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReservaItemRequest {

    private Long idReservaItem;

    @NotNull(message = "El tipo de item es obligatorio")
    private ReservaItem.TipoItem tipoItem;

    private Long servicioId;
    private Long paqueteId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio unitario debe tener máximo 10 dígitos enteros y 2 decimales")
    private BigDecimal precioUnitario;

    @DecimalMin(value = "0.00", message = "El precio total no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio total debe tener máximo 10 dígitos enteros y 2 decimales")
    private BigDecimal precioTotal;

    private String descripcionExtra;
}
