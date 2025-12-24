package com.sistema.turistico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Representa la información necesaria para asociar personal a una reserva.
 */
@Data
public class ReservaAsignacionPayload {

    private Long idAsignacion;

    @NotNull(message = "El ID del personal es obligatorio")
    private Long idPersonal;

    /**
     * Fecha de asignación en formato ISO (yyyy-MM-dd). Se tomará la fecha del servicio si es nula.
     */
    private String fechaAsignacion;

    private String observaciones;
}
