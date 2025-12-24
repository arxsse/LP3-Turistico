package com.sistema.turistico.repository;

import com.sistema.turistico.entity.PagoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoReservaRepository extends JpaRepository<PagoReserva, Long> {

    @Query("SELECT COALESCE(SUM(p.montoPagado), 0) FROM PagoReserva p WHERE p.reserva.idReserva = :reservaId AND p.estado = true")
    BigDecimal sumMontosActivosPorReserva(@Param("reservaId") Long reservaId);

    List<PagoReserva> findByReserva_IdReservaAndEstadoTrueOrderByFechaPagoAsc(Long reservaId);

    List<PagoReserva> findByReserva_IdReservaOrderByFechaPagoAsc(Long reservaId);

    Optional<PagoReserva> findByIdPagoAndReserva_IdReservaAndEstadoTrue(Long idPago, Long idReserva);

    @Query("SELECT p FROM PagoReserva p WHERE (:empresaId IS NULL OR p.reserva.empresa.idEmpresa = :empresaId) " +
           "AND (:sucursalId IS NULL OR p.sucursal.idSucursal = :sucursalId) " +
           "AND (:metodoPago IS NULL OR p.metodoPago = :metodoPago) " +
           "AND (:estado IS NULL OR p.estado = :estado) " +
           "AND (:fechaDesde IS NULL OR p.fechaPago >= :fechaDesde) " +
           "AND (:fechaHasta IS NULL OR p.fechaPago <= :fechaHasta) ORDER BY p.fechaPago DESC, p.createdAt DESC")
       List<PagoReserva> findByFiltros(@Param("empresaId") Long empresaId,
                                                               @Param("sucursalId") Long sucursalId,
                                                               @Param("metodoPago") String metodoPago,
                                                               @Param("estado") Boolean estado,
                                                               @Param("fechaDesde") LocalDate fechaDesde,
                                                               @Param("fechaHasta") LocalDate fechaHasta);
}
