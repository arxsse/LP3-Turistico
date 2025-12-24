package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicioResponse {
    private Long idServicio;
    private String tipoServicio;
    private String nombreServicio;
    private String descripcion;
    private String ubicacionDestino;
    private String duracion;
    private Integer capacidadMaxima;
    private BigDecimal precioBase;
    private String incluye;
    private String noIncluye;
    private String requisitos;
    private String politicasEspeciales;
    private boolean estado;
    private Long idSucursal;
    private String nombreSucursal;
    private String createdAt;
    private String updatedAt;
}