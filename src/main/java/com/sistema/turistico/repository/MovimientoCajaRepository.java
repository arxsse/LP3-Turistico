package com.sistema.turistico.repository;

import com.sistema.turistico.entity.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByCaja_IdCajaOrderByFechaHoraAsc(Long cajaId);

    @Query("SELECT m FROM MovimientoCaja m WHERE m.caja.idCaja = :cajaId " +
           "AND (:tipo IS NULL OR m.tipoMovimiento = :tipo) " +
           "AND (:inicio IS NULL OR m.fechaHora >= :inicio) " +
           "AND (:fin IS NULL OR m.fechaHora <= :fin) " +
           "ORDER BY m.fechaHora ASC")
    List<MovimientoCaja> findByCajaAndFilters(@Param("cajaId") Long cajaId,
                                              @Param("tipo") MovimientoCaja.TipoMovimiento tipo,
                                              @Param("inicio") LocalDateTime inicio,
                                              @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCaja m WHERE m.caja.idCaja = :cajaId " +
           "AND m.tipoMovimiento = :tipo " +
           "AND (:inicio IS NULL OR m.fechaHora >= :inicio) " +
           "AND (:fin IS NULL OR m.fechaHora <= :fin)")
    BigDecimal sumByCajaAndTipoAndRango(@Param("cajaId") Long cajaId,
                                        @Param("tipo") MovimientoCaja.TipoMovimiento tipo,
                                        @Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin);

    Optional<MovimientoCaja> findByIdMovimientoAndCaja_IdCaja(Long idMovimiento, Long idCaja);
}
