package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "paquetes_servicios", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"id_paquete", "id_servicio"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaqueteServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paquete_servicio")
    private Long idPaqueteServicio;

    @NotNull(message = "El paquete es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paquete", nullable = false)
    private PaqueteTuristico paquete;

    @NotNull(message = "El servicio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private ServicioTuristico servicio;

    @NotNull(message = "El orden es obligatorio")
    @Min(value = 1, message = "El orden debe ser mayor a 0")
    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer orden = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructor personalizado
    public PaqueteServicio(PaqueteTuristico paquete, ServicioTuristico servicio, Integer orden) {
        this.paquete = paquete;
        this.servicio = servicio;
        this.orden = orden != null ? orden : 1;
    }

    // Método helper para verificar si el servicio está activo
    public boolean isServicioActivo() {
        return servicio != null && servicio.isActivo();
    }

    // Método helper para obtener precio del servicio
    public java.math.BigDecimal getPrecioServicio() {
        return servicio != null ? servicio.getPrecioBase() : java.math.BigDecimal.ZERO;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (orden == null) {
            orden = 1;
        }
    }
}