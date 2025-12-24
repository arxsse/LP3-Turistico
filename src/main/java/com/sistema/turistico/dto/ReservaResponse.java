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
public class ReservaResponse {

    private Long idReserva;
    private String codigoReserva;
    private String fechaReserva;
    private String fechaServicio;
    private Integer numeroPersonas;
    private BigDecimal precioTotal;
    private BigDecimal descuentoAplicado;
    private String estado;
    private String observaciones;
    private Boolean evaluada;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Información básica del cliente
    private Long idCliente;
    private String nombreCliente;
    private String apellidoCliente;
    private String emailCliente;
    private String telefonoCliente;

    // Información básica del servicio
    private Long idServicio;
    private String nombreServicio;
    private String tipoServicio;
    private Long idPaquete;
    private String nombrePaquete;

    // Información básica del usuario
    private Long idUsuario;
    private String nombreUsuario;
    private String apellidoUsuario;

    // Información de la sucursal
    private Long idSucursal;
    private String nombreSucursal;

    private List<ReservaItemResponse> items;
    private List<ReservaAsignacionResponse> asignaciones;
}