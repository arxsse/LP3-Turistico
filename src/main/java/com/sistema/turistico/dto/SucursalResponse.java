package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SucursalResponse {
    private Long idSucursal;
    private String nombreSucursal;
    private String ubicacion;
    private String direccion;
    private String telefono;
    private String email;
    private String gerente;
    private Integer estado;
    private Long empresaId;
    private String empresaNombre;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
