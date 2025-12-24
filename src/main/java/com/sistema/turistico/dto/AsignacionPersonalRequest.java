package com.sistema.turistico.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.sql.Date;

@Data
public class AsignacionPersonalRequest {

    @NotNull(message = "El ID del personal es obligatorio")
    private Long idPersonal;

    @NotNull(message = "El ID de la reserva es obligatorio")
    private Long idReserva;

    private String rolAsignado;

    // La fecha de asignación ahora es opcional - se tomará de la reserva automáticamente
    private Date fechaAsignacion;

    private String observaciones;

    private Long idSucursal;
}