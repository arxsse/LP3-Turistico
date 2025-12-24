package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolResponse {
    private Long idRol;
    private String nombreRol;
    private String descripcion;
    private Integer estado;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
