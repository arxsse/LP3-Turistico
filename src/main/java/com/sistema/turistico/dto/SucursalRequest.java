package com.sistema.turistico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SucursalRequest {

    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Size(max = 255, message = "El nombre de la sucursal no puede exceder 255 caracteres")
    private String nombreSucursal;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 255, message = "La ubicación no puede exceder 255 caracteres")
    private String ubicacion;

    private String direccion;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    @Email(message = "El email debe tener un formato válido")
    private String email;

    @Size(max = 255, message = "El gerente no puede exceder 255 caracteres")
    private String gerente;

    private Integer estado;

    private Long empresaId;
}
