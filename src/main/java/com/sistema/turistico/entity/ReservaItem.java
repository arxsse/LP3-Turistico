package com.sistema.turistico.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "reserva_items", indexes = {
    @Index(name = "idx_reserva_items_reserva", columnList = "id_reserva"),
    @Index(name = "idx_reserva_items_servicio", columnList = "id_servicio"),
    @Index(name = "idx_reserva_items_paquete", columnList = "id_paquete")
})
public class ReservaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva_item")
    private Long idReservaItem;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    @ToString.Exclude
    private Reserva reserva;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_item", nullable = false, length = 20)
    private TipoItem tipoItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio")
    private ServicioTuristico servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paquete")
    private PaqueteTuristico paquete;

    @NotNull
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    @NotNull
    @DecimalMin(value = "0.00", message = "El precio unitario no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio unitario debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @NotNull
    @DecimalMin(value = "0.00", message = "El precio total no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio total debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "descripcion_extra", length = 500)
    private String descripcionExtra;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum TipoItem {
        SERVICIO,
        PAQUETE
    }

    public boolean esServicio() {
        return TipoItem.SERVICIO.equals(tipoItem);
    }

    public boolean esPaquete() {
        return TipoItem.PAQUETE.equals(tipoItem);
    }

    @PrePersist
    @PreUpdate
    private void sincronizarImportes() {
        if (precioUnitario != null && cantidad != null) {
            BigDecimal calculado = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
            if (precioTotal == null || precioTotal.compareTo(calculado) != 0) {
                precioTotal = calculado;
            }
        }
    }
}
