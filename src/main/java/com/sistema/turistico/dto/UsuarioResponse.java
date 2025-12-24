package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String dni;
    private Integer estado;
    private Long empresaId;
    private String empresaNombre;
    private Long rolId;
    private String rolNombre;
    private Long sucursalId;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;
}
