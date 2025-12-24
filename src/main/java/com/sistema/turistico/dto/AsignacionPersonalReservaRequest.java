package com.sistema.turistico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.sql.Date;

@Data
public class AsignacionPersonalReservaRequest {

    @NotNull(message = "El ID del personal es obligatorio")
    private Long idPersonal;

    // La fecha de asignación ahora es opcional - se tomará de la reserva automáticamente
    private Date fechaAsignacion;

    private String observaciones;
}