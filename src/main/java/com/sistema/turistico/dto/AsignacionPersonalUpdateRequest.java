package com.sistema.turistico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignacionPersonalUpdateRequest {

    @NotNull(message = "El ID del personal es obligatorio")
    private Long idPersonal;

    private String observaciones;
}