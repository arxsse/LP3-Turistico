package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaItemResponse {

    private Long idReservaItem;
    private String tipoItem;

    private Long idServicio;
    private String nombreServicio;
    private String tipoServicio;

    private Long idPaquete;
    private String nombrePaquete;

    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioTotal;
    private String descripcionExtra;
}
