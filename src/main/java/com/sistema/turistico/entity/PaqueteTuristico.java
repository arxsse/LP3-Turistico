package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paquetes_turisticos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaqueteTuristico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paquete")
    private Long idPaquete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @NotBlank(message = "El nombre del paquete es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    @Column(name = "nombre_paquete", nullable = false)
    private String nombrePaquete;

    @Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @NotNull(message = "El precio total es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio total debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio total debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Min(value = 1, message = "La duración debe ser al menos 1 día")
    @Column(name = "duracion_dias")
    private Integer duracionDias;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean promocion = false;

    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El descuento no puede exceder 100%")
    @Digits(integer = 5, fraction = 2, message = "El descuento debe tener máximo 5 dígitos enteros y 2 decimales")
    @Column(precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0.00")
    private BigDecimal descuento = BigDecimal.ZERO;

    @NotNull(message = "El estado es obligatorio")
    @Column(columnDefinition = "TINYINT(1) DEFAULT 1 COMMENT '1=Activo, 0=Inactivo'")
    private Boolean estado = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Relaciones con servicios incluidos en el paquete
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaqueteServicio> serviciosIncluidos = new ArrayList<>();

    // Constructor personalizado para facilitar creación
    public PaqueteTuristico(Empresa empresa, String nombrePaquete, String descripcion,
                          BigDecimal precioTotal, Integer duracionDias) {
        this.empresa = empresa;
        this.nombrePaquete = nombrePaquete;
        this.descripcion = descripcion;
        this.precioTotal = precioTotal;
        this.duracionDias = duracionDias;
        this.estado = true;
        this.promocion = false;
        this.descuento = BigDecimal.ZERO;
    }

    // Métodos helper
    public boolean isActivo() {
        return estado && deletedAt == null;
    }

    public BigDecimal getPrecioFinal() {
        if (descuento != null && descuento.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal descuentoMonto = precioTotal.multiply(descuento).divide(BigDecimal.valueOf(100));
            return precioTotal.subtract(descuentoMonto);
        }
        return precioTotal;
    }

    public int getNumeroServicios() {
        return serviciosIncluidos != null ? serviciosIncluidos.size() : 0;
    }

    public boolean tieneServicios() {
        return serviciosIncluidos != null && !serviciosIncluidos.isEmpty();
    }

    // Método para agregar servicio al paquete
    public void agregarServicio(ServicioTuristico servicio, Integer orden) {
        PaqueteServicio paqueteServicio = new PaqueteServicio(this, servicio, orden);
        serviciosIncluidos.add(paqueteServicio);
    }

    // Método para remover servicio del paquete
    public void removerServicio(ServicioTuristico servicio) {
        serviciosIncluidos.removeIf(ps -> ps.getServicio().equals(servicio));
    }

    // Método para verificar si el paquete está disponible
    public boolean estaDisponible() {
        return isActivo() && tieneServicios();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}