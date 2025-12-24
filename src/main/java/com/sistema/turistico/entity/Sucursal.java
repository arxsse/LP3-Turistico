package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "sucursales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sucursal")
    private Long idSucursal;

    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Size(max = 255, message = "El nombre de la sucursal no puede exceder 255 caracteres")
    @Column(name = "nombre_sucursal", nullable = false, length = 255)
    private String nombreSucursal;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 255, message = "La ubicación no puede exceder 255 caracteres")
    @Column(name = "ubicacion", nullable = false, length = 255)
    private String ubicacion;

    @Column(name = "direccion", columnDefinition = "TEXT")
    private String direccion;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    @Column(name = "email", length = 255)
    private String email;

    @Size(max = 255, message = "El gerente no puede exceder 255 caracteres")
    @Column(name = "gerente", length = 255)
    private String gerente;

    @Column(name = "estado", nullable = false)
    private Integer estado = 1; // 1=Activo, 0=Inactivo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}