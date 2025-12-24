package com.sistema.turistico.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String rol;
    private Long idEmpresa;
    private String nombreEmpresa;
    private List<String> permisos;

    public LoginResponse(String token, Long id, String email, String nombre, String apellido,
                        String rol, Long idEmpresa, String nombreEmpresa, List<String> permisos) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
        this.idEmpresa = idEmpresa;
        this.nombreEmpresa = nombreEmpresa;
        this.permisos = permisos;
    }
}