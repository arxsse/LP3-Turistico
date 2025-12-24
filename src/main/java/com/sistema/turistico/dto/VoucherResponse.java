package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {

    private Long idVoucher;
    private String codigoQr;
    private String fechaEmision;
    private String fechaExpiracion;
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Información básica de la reserva
    private Long idReserva;
    private String codigoReserva;
    private String fechaServicio;
    private Integer numeroPersonas;

    // Información básica del cliente
    private Long idCliente;
    private String nombreCliente;
    private String apellidoCliente;
    private String emailCliente;

    // Información básica del servicio
    private Long idServicio;
    private String nombreServicio;
    private String tipoServicio;
}