package com.sistema.turistico.repository;

import com.sistema.turistico.entity.EvaluacionServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionServicioRepository extends JpaRepository<EvaluacionServicio, Long> {

    // Búsquedas por reserva
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.reserva.idReserva = :reservaId")
    List<EvaluacionServicio> findByReservaId(@Param("reservaId") Long reservaId);

    // Búsquedas por cliente
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.cliente.idCliente = :clienteId")
    List<EvaluacionServicio> findByClienteId(@Param("clienteId") Long clienteId);

    // Búsquedas por servicio
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.servicio.idServicio = :servicioId")
    List<EvaluacionServicio> findByServicioId(@Param("servicioId") Long servicioId);

    // Búsquedas por paquete
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.paquete.idPaquete = :paqueteId")
    List<EvaluacionServicio> findByPaqueteId(@Param("paqueteId") Long paqueteId);

    // Búsquedas por calificación general
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.calificacionGeneral = :calificacion")
    List<EvaluacionServicio> findByCalificacionGeneral(@Param("calificacion") Integer calificacion);

    // Búsquedas por rango de calificaciones
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.calificacionGeneral BETWEEN :min AND :max")
    List<EvaluacionServicio> findByCalificacionGeneralBetween(@Param("min") Integer min, @Param("max") Integer max);

    // Búsquedas por estado
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.estado = :estado")
    List<EvaluacionServicio> findByEstado(@Param("estado") Boolean estado);

    // Verificar si una reserva ya tiene evaluación
    @Query("SELECT COUNT(e) > 0 FROM EvaluacionServicio e WHERE e.reserva.idReserva = :reservaId")
    boolean existsByReservaId(@Param("reservaId") Long reservaId);

    // Obtener evaluación de una reserva específica
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.reserva.idReserva = :reservaId")
    Optional<EvaluacionServicio> findByReservaIdUnique(@Param("reservaId") Long reservaId);

    // Estadísticas de calificaciones por servicio
    @Query("SELECT AVG(e.calificacionGeneral), COUNT(e) FROM EvaluacionServicio e WHERE e.servicio.idServicio = :servicioId AND e.estado = true")
    Object[] getEstadisticasByServicio(@Param("servicioId") Long servicioId);

    // Estadísticas de calificaciones por paquete
    @Query("SELECT AVG(e.calificacionGeneral), COUNT(e) FROM EvaluacionServicio e WHERE e.paquete.idPaquete = :paqueteId AND e.estado = true")
    Object[] getEstadisticasByPaquete(@Param("paqueteId") Long paqueteId);

    // Para reportes: evaluaciones por rango de fecha
    @Query("SELECT e FROM EvaluacionServicio e WHERE e.reserva.empresa.idEmpresa = :empresaId AND DATE(e.fechaEvaluacion) BETWEEN :fechaInicio AND :fechaFin")
    List<EvaluacionServicio> findByFechaRango(@Param("empresaId") Long empresaId,
                                             @Param("fechaInicio") java.time.LocalDate fechaInicio,
                                             @Param("fechaFin") java.time.LocalDate fechaFin);
}