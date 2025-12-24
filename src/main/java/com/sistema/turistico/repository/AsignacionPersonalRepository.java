package com.sistema.turistico.repository;

import com.sistema.turistico.entity.AsignacionPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface AsignacionPersonalRepository extends JpaRepository<AsignacionPersonal, Long> {

    // Búsquedas por empresa y sucursal
    @Query("SELECT a FROM AsignacionPersonal a WHERE a.reserva.empresa.idEmpresa = :empresaId AND (a.sucursal IS NULL OR a.sucursal.idSucursal = :sucursalId)")
    List<AsignacionPersonal> findByEmpresaIdAndSucursalId(@Param("empresaId") Long empresaId, @Param("sucursalId") Long sucursalId);

    // Búsquedas por reserva
    @Query("SELECT a FROM AsignacionPersonal a WHERE a.reserva.idReserva = :reservaId")
    List<AsignacionPersonal> findByReservaId(@Param("reservaId") Long reservaId);

    @Query("""
        SELECT DISTINCT a
        FROM AsignacionPersonal a
        JOIN FETCH a.reserva r
        JOIN FETCH a.personal p
        WHERE r.idReserva = :reservaId
    """)
    List<AsignacionPersonal> findDetailedByReservaId(@Param("reservaId") Long reservaId);

    // Búsquedas por personal
    @Query("SELECT a FROM AsignacionPersonal a WHERE a.personal.idPersonal = :personalId")
    List<AsignacionPersonal> findByPersonalId(@Param("personalId") Long personalId);

    // Búsquedas por fecha
    @Query("SELECT a FROM AsignacionPersonal a WHERE a.fechaAsignacion = :fecha")
    List<AsignacionPersonal> findByFechaAsignacion(@Param("fecha") Date fecha);

    // Búsquedas por rango de fechas
    @Query("SELECT a FROM AsignacionPersonal a WHERE a.fechaAsignacion BETWEEN :fechaInicio AND :fechaFin")
    List<AsignacionPersonal> findByFechaAsignacionBetween(@Param("fechaInicio") Date fechaInicio, @Param("fechaFin") Date fechaFin);

    // Búsquedas por estado
    @Query("SELECT a FROM AsignacionPersonal a WHERE a.estado = :estado")
    List<AsignacionPersonal> findByEstado(@Param("estado") AsignacionPersonal.EstadoAsignacion estado);

    // Verificar si personal ya está asignado en una fecha específica
    @Query("SELECT COUNT(a) > 0 FROM AsignacionPersonal a WHERE a.personal.idPersonal = :personalId AND a.fechaAsignacion = :fecha AND a.estado = 'Asignado'")
    boolean existsByPersonalAndFecha(@Param("personalId") Long personalId, @Param("fecha") Date fecha);

    // Contar asignaciones activas por personal
    @Query("SELECT COUNT(a) FROM AsignacionPersonal a WHERE a.personal.idPersonal = :personalId AND a.estado = 'Asignado'")
    Long countAsignacionesActivasByPersonal(@Param("personalId") Long personalId);
}