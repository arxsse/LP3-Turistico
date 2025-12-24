package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombreRol(String nombreRol);

    boolean existsByNombreRol(String nombreRol);

    @Query("SELECT r FROM Rol r WHERE r.deletedAt IS NULL ORDER BY r.createdAt DESC")
    List<Rol> findAllActivos();

    @Query("SELECT r FROM Rol r WHERE r.deletedAt IS NULL " +
           "AND (:estado IS NULL OR r.estado = :estado) " +
           "AND (:busqueda IS NULL " +
           "OR LOWER(r.nombreRol) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Rol> findByFiltros(@Param("busqueda") String busqueda, @Param("estado") Integer estado);

    @Query("SELECT r FROM Rol r WHERE r.idRol = :id AND r.deletedAt IS NULL")
    Optional<Rol> findActivoById(@Param("id") Long id);

    boolean existsByNombreRolAndIdRolNot(String nombreRol, Long idRol);
}