package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResponse {
    private Long idEmpresa;
    private String nombreEmpresa;
    private String ruc;
    private String email;
    private String telefono;
    private String direccion;
    private Integer estado;
    private String fechaRegistro;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
