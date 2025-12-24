package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("SELECT v FROM Venta v WHERE (:empresaId IS NULL OR v.empresa.idEmpresa = :empresaId) " +
           "AND (:sucursalId IS NULL OR v.sucursal.idSucursal = :sucursalId) " +
           "AND (:fechaInicio IS NULL OR v.fechaHora >= :fechaInicio) " +
           "AND (:fechaFin IS NULL OR v.fechaHora <= :fechaFin) " +
           "AND (:metodoPago IS NULL OR v.metodoPago = :metodoPago) " +
           "AND (:estado IS NULL OR v.estado = :estado) " +
           "ORDER BY v.fechaHora DESC")
    List<Venta> findByFiltros(@Param("empresaId") Long empresaId,
                              @Param("sucursalId") Long sucursalId,
                              @Param("fechaInicio") LocalDateTime fechaInicio,
                              @Param("fechaFin") LocalDateTime fechaFin,
                              @Param("metodoPago") String metodoPago,
                              @Param("estado") Boolean estado);

    List<Venta> findByCaja_IdCajaOrderByFechaHoraDesc(Long cajaId);
}
