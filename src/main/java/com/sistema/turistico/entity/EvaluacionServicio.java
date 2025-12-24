package com.sistema.turistico.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "evaluaciones_servicios", indexes = {
    @Index(name = "idx_calificacion", columnList = "calificacion_general"),
    @Index(name = "idx_fecha_eval", columnList = "fecha_evaluacion")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluacion")
    private Long idEvaluacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio")
    private ServicioTuristico servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paquete")
    private PaqueteTuristico paquete;

    @Min(1)
    @Max(5)
    @Column(name = "calificacion_general")
    private Integer calificacionGeneral;

    @Min(1)
    @Max(5)
    @Column(name = "calificacion_guia")
    private Integer calificacionGuia;

    @Min(1)
    @Max(5)
    @Column(name = "calificacion_transporte")
    private Integer calificacionTransporte;

    @Min(1)
    @Max(5)
    @Column(name = "calificacion_hotel")
    private Integer calificacionHotel;

    @Column(name = "comentario_general", columnDefinition = "TEXT")
    private String comentarioGeneral;

    @Column(name = "comentario_guia", columnDefinition = "TEXT")
    private String comentarioGuia;

    @Column(name = "comentario_transporte", columnDefinition = "TEXT")
    private String comentarioTransporte;

    @Column(name = "comentario_hotel", columnDefinition = "TEXT")
    private String comentarioHotel;

    @Column(name = "recomendaciones", columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(name = "fecha_evaluacion", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date fechaEvaluacion;

    @Column(name = "estado", nullable = false)
    private Boolean estado = true;

    @PrePersist
    protected void onCreate() {
        if (fechaEvaluacion == null) {
            fechaEvaluacion = new java.util.Date();
        }
    }

    // Método helper para obtener calificación promedio
    public Double getCalificacionPromedio() {
        int count = 0;
        int sum = 0;

        if (calificacionGeneral != null) {
            sum += calificacionGeneral;
            count++;
        }
        if (calificacionGuia != null) {
            sum += calificacionGuia;
            count++;
        }
        if (calificacionTransporte != null) {
            sum += calificacionTransporte;
            count++;
        }
        if (calificacionHotel != null) {
            sum += calificacionHotel;
            count++;
        }

        return count > 0 ? (double) sum / count : null;
    }

    // Método helper para verificar si es visible
    public boolean isVisible() {
        return estado != null && estado;
    }
}