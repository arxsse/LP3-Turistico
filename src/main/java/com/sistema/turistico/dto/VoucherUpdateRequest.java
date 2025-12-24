package com.sistema.turistico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUpdateRequest {

    @NotNull(message = "El estado es obligatorio")
    private String estado;

    private String observaciones;
}