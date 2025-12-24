package com.sistema.turistico.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Información con detalle del personal asignado a una reserva.
 */
@Data
public class ReservaAsignacionResponse {

    private Long idAsignacion;
    private Long idPersonal;
    private String nombrePersonal;
    private String apellidoPersonal;
    private String dniPersonal;
    private String telefonoPersonal;
    private String emailPersonal;
    private String cargoPersonal;
    private String rolAsignado;
    private String estado;
    private String observaciones;
    private String fechaAsignacion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
