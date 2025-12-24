package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {
    private Long idCliente;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String dni;
    private LocalDate fechaNacimiento;
    private String nacionalidad;
    private String preferenciasViaje;
    private boolean estado;
    private Long idSucursal;
    private String nombreSucursal;
    private String createdAt;
    private String updatedAt;
}