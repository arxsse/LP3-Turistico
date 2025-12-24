package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    @Query("SELECT e FROM Empresa e WHERE e.deletedAt IS NULL ORDER BY e.createdAt DESC")
    List<Empresa> findAllActivas();

    @Query("SELECT e FROM Empresa e WHERE e.deletedAt IS NULL " +
           "AND (:estado IS NULL OR e.estado = :estado) " +
           "AND (:busqueda IS NULL " +
           "OR LOWER(e.nombreEmpresa) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR e.ruc LIKE CONCAT('%', :busqueda, '%')) ORDER BY e.createdAt DESC")
    List<Empresa> findByFiltros(@Param("busqueda") String busqueda, @Param("estado") Integer estado);

    @Query("SELECT e FROM Empresa e WHERE e.idEmpresa = :id AND e.deletedAt IS NULL")
    Optional<Empresa> findActivaById(@Param("id") Long idEmpresa);

    boolean existsByRuc(String ruc);

    boolean existsByEmail(String email);

    boolean existsByRucAndIdEmpresaNot(String ruc, Long idEmpresa);

    boolean existsByEmailAndIdEmpresaNot(String email, Long idEmpresa);
}