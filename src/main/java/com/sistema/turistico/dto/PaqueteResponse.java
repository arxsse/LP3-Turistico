package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaqueteResponse {

    private Long idPaquete;
    private String nombrePaquete;
    private String descripcion;
    private BigDecimal precioTotal;
    private Integer duracionDias;
    private Boolean promocion;
    private BigDecimal descuento;
    private Integer numeroServicios;
    private Boolean estado;
    private Long idSucursal;
    private String nombreSucursal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Información simplificada de servicios incluidos
    private List<ServicioSimplificado> serviciosIncluidos;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioSimplificado {
        private Long idServicio;
        private String nombreServicio;
        private String tipoServicio;
        private Integer orden;
    }
}