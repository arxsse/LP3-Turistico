package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Búsquedas básicas por empresa
    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.sucursal WHERE r.empresa.idEmpresa = :empresaId AND r.deletedAt IS NULL")
    List<Reserva> findByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.sucursal WHERE r.empresa.idEmpresa = :empresaId AND (r.sucursal IS NULL OR r.sucursal.idSucursal = :sucursalId) AND r.deletedAt IS NULL")
    List<Reserva> findByEmpresaIdAndSucursalId(@Param("empresaId") Long empresaId, @Param("sucursalId") Long sucursalId);

    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.sucursal WHERE r.empresa.idEmpresa = :empresaId AND r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Reserva> findByEmpresaIdOrderByCreatedAtDesc(@Param("empresaId") Long empresaId);

       @Query("SELECT DISTINCT r FROM Reserva r " +
          "LEFT JOIN FETCH r.empresa " +
          "LEFT JOIN FETCH r.sucursal " +
          "LEFT JOIN FETCH r.cliente " +
          "LEFT JOIN FETCH r.usuario " +
          "LEFT JOIN FETCH r.items i " +
          "LEFT JOIN FETCH i.servicio " +
          "LEFT JOIN FETCH i.paquete " +
          "WHERE r.idReserva = :id AND r.deletedAt IS NULL")
       Optional<Reserva> findByIdWithItems(@Param("id") Long id);

    // Búsquedas por cliente
    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.sucursal WHERE r.empresa.idEmpresa = :empresaId AND r.cliente.idCliente = :clienteId AND r.deletedAt IS NULL")
    List<Reserva> findByEmpresaIdAndClienteId(@Param("empresaId") Long empresaId, @Param("clienteId") Long clienteId);

    // Búsquedas por estado
    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.sucursal WHERE r.empresa.idEmpresa = :empresaId AND r.estado = :estado AND r.deletedAt IS NULL")
    List<Reserva> findByEmpresaIdAndEstado(@Param("empresaId") Long empresaId, @Param("estado") Reserva.EstadoReserva estado);

    // Búsquedas por fecha de servicio
    @Query("SELECT r FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.fechaServicio = :fechaServicio AND r.deletedAt IS NULL")
    List<Reserva> findByEmpresaIdAndFechaServicio(@Param("empresaId") Long empresaId, @Param("fechaServicio") java.sql.Date fechaServicio);

    @Query("SELECT r FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.fechaServicio BETWEEN :fechaInicio AND :fechaFin AND r.deletedAt IS NULL")
    List<Reserva> findByEmpresaIdAndFechaServicioBetween(@Param("empresaId") Long empresaId,
                                                        @Param("fechaInicio") java.sql.Date fechaInicio,
                                                        @Param("fechaFin") java.sql.Date fechaFin);

    // Búsquedas por servicio
       @Query("SELECT DISTINCT r FROM Reserva r JOIN r.items i WHERE r.empresa.idEmpresa = :empresaId AND i.tipoItem = com.sistema.turistico.entity.ReservaItem$TipoItem.SERVICIO AND i.servicio.idServicio = :servicioId AND r.deletedAt IS NULL")
       List<Reserva> findByEmpresaIdAndServicioId(@Param("empresaId") Long empresaId, @Param("servicioId") Long servicioId);

    // Búsquedas con filtros avanzados
    @Query("SELECT DISTINCT r FROM Reserva r LEFT JOIN FETCH r.sucursal WHERE r.empresa.idEmpresa = :empresaId AND r.deletedAt IS NULL " +
           "AND (LOWER(r.codigoReserva) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(r.cliente.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(r.cliente.apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Reserva> findByEmpresaIdAndBusqueda(@Param("empresaId") Long empresaId, @Param("busqueda") String busqueda);

    // Verificaciones de unicidad
    @Query("SELECT COUNT(r) > 0 FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.codigoReserva = :codigoReserva AND r.idReserva != :excludeId")
    boolean existsByEmpresaIdAndCodigoReservaAndIdNot(@Param("empresaId") Long empresaId,
                                                      @Param("codigoReserva") String codigoReserva,
                                                      @Param("excludeId") Long excludeId);

    // Estadísticas
    @Query("SELECT COUNT(r) FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.deletedAt IS NULL")
    Long countByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT r.estado, COUNT(r) FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.deletedAt IS NULL GROUP BY r.estado")
    List<Object[]> countReservasByEstado(@Param("empresaId") Long empresaId);

    @Query("SELECT SUM(r.precioTotal) FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.estado IN ('Pagada', 'Completada') AND r.deletedAt IS NULL")
    java.math.BigDecimal sumIngresosByEmpresaId(@Param("empresaId") Long empresaId);

    // Reservas pendientes de evaluación
    @Query("SELECT r FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.estado = 'Completada' AND r.evaluada = false AND r.deletedAt IS NULL")
    List<Reserva> findReservasPendientesEvaluacion(@Param("empresaId") Long empresaId);

    // Reservas próximas (próximos 7 días)
    @Query("SELECT r FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.fechaServicio BETWEEN :hoy AND :enUnaSemana AND r.estado IN ('Confirmada', 'Pagada') AND r.deletedAt IS NULL ORDER BY r.fechaServicio")
    List<Reserva> findReservasProximas(@Param("empresaId") Long empresaId,
                                       @Param("hoy") java.sql.Date hoy,
                                       @Param("enUnaSemana") java.sql.Date enUnaSemana);

    // Para reportes: reservas por rango de fecha de creación
    @Query("SELECT r FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND DATE(r.createdAt) BETWEEN :fechaInicio AND :fechaFin AND r.deletedAt IS NULL")
    List<Reserva> findByEmpresaAndFechaRango(@Param("empresaId") Long empresaId,
                                             @Param("fechaInicio") LocalDate fechaInicio,
                                             @Param("fechaFin") LocalDate fechaFin);

    // Para generar código secuencial por empresa
    @Query("SELECT MAX(r.codigoReserva) FROM Reserva r WHERE r.empresa.idEmpresa = :empresaId AND r.codigoReserva LIKE CONCAT('RES-', :empresaId, '-%')")
    Optional<String> findMaxCodigoReservaByEmpresaId(@Param("empresaId") Long empresaId);
}