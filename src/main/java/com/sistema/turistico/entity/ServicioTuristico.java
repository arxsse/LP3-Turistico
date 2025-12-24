package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "servicios_turisticos", indexes = {
    @Index(name = "idx_servicios_empresa", columnList = "id_empresa"),
    @Index(name = "idx_servicios_sucursal", columnList = "id_sucursal"),
    @Index(name = "fk_servicio_categoria", columnList = "id_categoria"),
    @Index(name = "idx_servicios_tipo", columnList = "tipo_servicio"),
    @Index(name = "idx_servicios_estado", columnList = "estado")
})
public class ServicioTuristico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long idServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empresa", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sucursal")
    private Sucursal sucursal;

    // Relaciones opcionales - se implementarán cuando existan las entidades
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "id_proveedor")
    // private Proveedor proveedor;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "id_categoria")
    // private CategoriaServicio categoria;

    // Campos temporales para compatibilidad
    @Column(name = "id_proveedor")
    private Long idProveedor;

    @Column(name = "id_categoria")
    private Long idCategoria;

    @NotNull(message = "El tipo de servicio es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_servicio", nullable = false, columnDefinition = "ENUM('Tour','Hotel','Transporte','Entrada/Atractivo')")
    private TipoServicio tipoServicio;

    @NotBlank(message = "El nombre del servicio es obligatorio")
    @Size(max = 255, message = "El nombre no puede exceder 255 caracteres")
    @Column(name = "nombre_servicio", nullable = false, length = 255)
    private String nombreServicio;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Size(max = 255, message = "La ubicación no puede exceder 255 caracteres")
    @Column(name = "ubicacion_destino", length = 255)
    private String ubicacionDestino;

    @Size(max = 100, message = "La duración no puede exceder 100 caracteres")
    @Column(name = "duracion", length = 100)
    private String duracion;

    @NotNull(message = "La capacidad máxima es obligatoria")
    @Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
    @Column(name = "capacidad_maxima", nullable = false)
    private Integer capacidadMaxima;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio base debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio base debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "incluye", columnDefinition = "TEXT")
    private String incluye;

    @Column(name = "no_incluye", columnDefinition = "TEXT")
    private String noIncluye;

    @Column(name = "requisitos", columnDefinition = "TEXT")
    private String requisitos;

    @Column(name = "politicas_especiales", columnDefinition = "TEXT")
    private String politicasEspeciales;

    @Column(name = "imagenes", columnDefinition = "LONGTEXT")
    private String imagenes; // JSON array de URLs

    @Column(name = "itinerario", columnDefinition = "TEXT")
    private String itinerario;

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

    public enum TipoServicio {
        Tour, Hotel, Transporte, EntradaAtractivo
    }

    // Métodos helper
    public boolean isActivo() {
        return estado && deletedAt == null;
    }

    public boolean tieneDisponibilidad(Integer personasRequeridas) {
        return isActivo() && capacidadMaxima >= personasRequeridas;
    }

    public BigDecimal calcularPrecioTotal(Integer personas) {
        return precioBase.multiply(BigDecimal.valueOf(personas));
    }
}