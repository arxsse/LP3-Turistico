package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Date;

@Entity
@Table(name = "asignaciones_personal", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_personal", "id_reserva", "fecha_asignacion"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionPersonal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion")
    private Long idAsignacion;

    @NotNull(message = "El personal es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_personal", nullable = false)
    private Personal personal;

    @NotNull(message = "La reserva es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @NotNull(message = "La fecha de asignación es obligatoria")
    @Column(name = "fecha_asignacion", nullable = false)
    private Date fechaAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoAsignacion estado = EstadoAsignacion.Asignado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.util.Date();
        updatedAt = new java.util.Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.util.Date();
    }

    public enum RolAsignado {
        Guía, Chofer, Staff
    }

    public enum EstadoAsignacion {
        Asignado, Completado, Cancelado
    }

    // Constructor personalizado para facilitar creación
    public AsignacionPersonal(Personal personal, Reserva reserva, Date fechaAsignacion) {
        this.personal = personal;
        this.reserva = reserva;
        this.fechaAsignacion = fechaAsignacion;
    }

    // Método para obtener el rol asignado basado en el cargo del personal
    public RolAsignado getRolAsignado() {
        if (personal != null && personal.getCargo() != null) {
            return RolAsignado.valueOf(personal.getCargo().toString());
        }
        return null;
    }
}