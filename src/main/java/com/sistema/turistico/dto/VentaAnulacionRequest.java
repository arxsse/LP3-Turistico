package com.sistema.turistico.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VentaAnulacionRequest {

    @NotNull
    private Long usuarioId;

    @NotNull
    private Long cajaId;

    @NotBlank
    private String motivo;
}
