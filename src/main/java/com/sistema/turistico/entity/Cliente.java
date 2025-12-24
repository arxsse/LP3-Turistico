package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "clientes", indexes = {
    @Index(name = "idx_clientes_empresa", columnList = "id_empresa"),
    @Index(name = "idx_clientes_sucursal", columnList = "id_sucursal"),
    @Index(name = "idx_clientes_email", columnList = "email"),
    @Index(name = "idx_clientes_dni", columnList = "dni")
})
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal", nullable = true)
    private Sucursal sucursal;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Email(message = "El email debe tener un formato válido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "email_encriptado", columnDefinition = "VARBINARY(255)")
    private byte[] emailEncriptado;

    @Pattern(regexp = "^[0-9]{9}$", message = "El teléfono debe tener 9 dígitos")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "telefono_encriptado", columnDefinition = "VARBINARY(255)")
    private byte[] telefonoEncriptado;

    @Size(max = 20, message = "El DNI no puede exceder 20 caracteres")
    @Column(name = "dni", length = 20)
    private String dni;

    @Past(message = "La fecha de nacimiento debe ser anterior a la fecha actual")
    @Column(name = "fecha_nacimiento")
    private java.sql.Date fechaNacimiento;

    @Size(max = 100, message = "La nacionalidad no puede exceder 100 caracteres")
    @Column(name = "nacionalidad", length = 100)
    private String nacionalidad;

    @Column(name = "preferencias_viaje", columnDefinition = "TEXT")
    private String preferenciasViaje;

    @Min(value = 0, message = "Los puntos de fidelización no pueden ser negativos")
    @Column(name = "puntos_fidelizacion", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer puntosFidelizacion = 0;

    @NotNull(message = "El nivel de membresía es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_membresia", nullable = false, columnDefinition = "ENUM('Bronce','Plata','Oro','Platino') DEFAULT 'Bronce'")
    private NivelMembresia nivelMembresia = NivelMembresia.Bronce;

    @NotNull(message = "El estado es obligatorio")
    @Column(name = "estado", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean estado = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum NivelMembresia {
        Bronce, Plata, Oro, Platino
    }

    // Métodos helper
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public boolean isActivo() {
        return estado && deletedAt == null;
    }
}