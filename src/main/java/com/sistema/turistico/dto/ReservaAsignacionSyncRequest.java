package com.sistema.turistico.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload para sincronizar el personal asignado a una reserva.
 */
@Data
public class ReservaAsignacionSyncRequest {

    @Valid
    @Size(max = 50, message = "No se pueden registrar más de 50 asignaciones por solicitud")
    private List<ReservaAsignacionPayload> asignaciones = new ArrayList<>();
}
