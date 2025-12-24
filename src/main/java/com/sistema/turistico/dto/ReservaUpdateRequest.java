package com.sistema.turistico.dto;

import com.sistema.turistico.entity.Reserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaUpdateRequest {

    private Reserva.EstadoReserva estado;
    private String observaciones;
}