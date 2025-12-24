package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "reservas", indexes = {
    @Index(name = "idx_reservas_empresa", columnList = "id_empresa"),
    @Index(name = "idx_reservas_cliente", columnList = "id_cliente"),
    @Index(name = "idx_reservas_fecha", columnList = "fecha_reserva"),
    @Index(name = "idx_reservas_estado", columnList = "estado"),
    @Index(name = "fk_reserva_promocion", columnList = "id_promocion")
})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long idReserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservaItem> items = new ArrayList<>();

    @NotNull(message = "El usuario que registra es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotBlank(message = "El código de reserva es obligatorio")
    @Size(max = 50, message = "El código de reserva no puede exceder 50 caracteres")
    @Column(name = "codigo_reserva", nullable = false, length = 50, unique = true)
    private String codigoReserva;

    @NotNull(message = "La fecha de reserva es obligatoria")
    @Column(name = "fecha_reserva", nullable = false)
    private java.sql.Date fechaReserva;

    @NotNull(message = "La fecha de servicio es obligatoria")
    @Column(name = "fecha_servicio", nullable = false)
    private java.sql.Date fechaServicio;

    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    @Column(name = "numero_personas", nullable = false)
    private Integer numeroPersonas;

    @NotNull(message = "El precio total es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio total debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio total debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "precio_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioTotal;

    // Relación opcional con Promocion - se implementará cuando exista la entidad
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "id_promocion")
    // private Promocion promocion;

    @Column(name = "id_promocion")
    private Long idPromocion;

    @DecimalMin(value = "0.00", message = "El descuento aplicado no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El descuento debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "descuento_aplicado", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;

    @NotNull(message = "El estado es obligatorio")
    @Column(name = "estado", nullable = false, columnDefinition = "ENUM('Pendiente','Confirmada','Pago Parcial','Pagada','Cancelada','Completada') DEFAULT 'Pendiente'")
    @Convert(converter = EstadoReservaConverter.class)
    private EstadoReserva estado = EstadoReserva.Pendiente;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "evaluada", columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean evaluada = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum EstadoReserva {
        Pendiente("Pendiente"),
        Confirmada("Confirmada"),
        PagoParcial("Pago Parcial"),
        Pagada("Pagada"),
        Cancelada("Cancelada"),
        Completada("Completada");

        private final String valorBD;

        EstadoReserva(String valorBD) {
            this.valorBD = valorBD;
        }

        public String getValorBD() {
            return valorBD;
        }

        @Override
        public String toString() {
            return valorBD;
        }
    }

    @Converter(autoApply = true)
    public static class EstadoReservaConverter implements AttributeConverter<EstadoReserva, String> {

        @Override
        public String convertToDatabaseColumn(EstadoReserva attribute) {
            return attribute != null ? attribute.getValorBD() : null;
        }

        @Override
        public EstadoReserva convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            for (EstadoReserva estado : EstadoReserva.values()) {
                if (estado.getValorBD().equals(dbData)) {
                    return estado;
                }
            }
            throw new IllegalArgumentException("Unknown database value: " + dbData);
        }
    }

    // Métodos helper
    public boolean isActiva() {
        return deletedAt == null && (estado == EstadoReserva.Pendiente ||
                                     estado == EstadoReserva.Confirmada ||
                                     estado == EstadoReserva.PagoParcial ||
                                     estado == EstadoReserva.Pagada);
    }

    public BigDecimal getPrecioFinal() {
        return precioTotal.subtract(descuentoAplicado);
    }

    public boolean puedeCancelarse() {
        return isActiva() && !evaluada;
    }

    public boolean puedeEvaluarse() {
        return estado == EstadoReserva.Completada && !evaluada;
    }

    public void addItem(ReservaItem item) {
        if (item == null) {
            return;
        }
        item.setReserva(this);
        items.add(item);
    }

    public void setItems(List<ReservaItem> nuevosItems) {
        items.clear();
        if (nuevosItems == null) {
            return;
        }
        nuevosItems.forEach(this::addItem);
        recalcularTotalesDesdeItems();
    }

    public void recalcularTotalesDesdeItems() {
        int totalPersonas = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (ReservaItem item : items) {
            if (item.getCantidad() != null) {
                totalPersonas += item.getCantidad();
            }
            if (item.getPrecioTotal() != null) {
                total = total.add(item.getPrecioTotal());
            }
        }

        if (totalPersonas > 0) {
            setNumeroPersonas(totalPersonas);
        }

        setPrecioTotal(total);
    }
}