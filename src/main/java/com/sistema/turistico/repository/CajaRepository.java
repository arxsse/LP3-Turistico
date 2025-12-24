package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<Caja, Long> {

    boolean existsBySucursal_IdSucursalAndEstado(Long idSucursal, Caja.EstadoCaja estado);

    Optional<Caja> findFirstBySucursal_IdSucursalAndEstadoOrderByCreatedAtDesc(Long idSucursal, Caja.EstadoCaja estado);

    List<Caja> findByEmpresa_IdEmpresaAndEstado(Long idEmpresa, Caja.EstadoCaja estado);

    @Query("SELECT c FROM Caja c WHERE c.empresa.idEmpresa = :empresaId AND (:fecha IS NULL OR c.fechaApertura = :fecha) ORDER BY c.createdAt DESC")
    List<Caja> findByEmpresaAndFecha(@Param("empresaId") Long empresaId, @Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Caja c WHERE (:empresaId IS NULL OR c.empresa.idEmpresa = :empresaId) AND (:sucursalId IS NULL OR c.sucursal.idSucursal = :sucursalId) AND (:estado IS NULL OR c.estado = :estado) ORDER BY c.createdAt DESC")
    List<Caja> findByFilters(@Param("empresaId") Long empresaId, @Param("sucursalId") Long sucursalId, @Param("estado") Caja.EstadoCaja estado);
}
