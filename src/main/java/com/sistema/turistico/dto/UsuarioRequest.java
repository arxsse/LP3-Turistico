package com.sistema.turistico.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    private String email;

    @Size(max = 255, message = "La contraseña no puede exceder 255 caracteres")
    private String password;

    @Size(max = 20, message = "El DNI no puede exceder 20 caracteres")
    private String dni;

    private Integer estado;

    private Long empresaId;

    @NotNull(message = "El rol es obligatorio")
    private Long rolId;

    private Long sucursalId;
}
