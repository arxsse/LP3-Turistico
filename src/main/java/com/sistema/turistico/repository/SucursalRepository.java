package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Long> {

    @Query("SELECT s FROM Sucursal s WHERE s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<Sucursal> findAllActivas();

    @Query("SELECT s FROM Sucursal s WHERE s.deletedAt IS NULL " +
	    "AND (:estado IS NULL OR s.estado = :estado) " +
	    "AND (:empresaId IS NULL OR s.empresa.idEmpresa = :empresaId) " +
	    "AND (:busqueda IS NULL " +
	    "OR LOWER(s.nombreSucursal) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
	    "OR LOWER(s.ubicacion) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
	    "OR LOWER(s.email) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
	    "OR LOWER(s.direccion) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
	    "OR LOWER(s.gerente) LIKE LOWER(CONCAT('%', :busqueda, '%'))" +
	    ") ORDER BY s.createdAt DESC")
    List<Sucursal> findByFiltros(@Param("busqueda") String busqueda,
				     @Param("estado") Integer estado,
				     @Param("empresaId") Long empresaId);

    @Query("SELECT s FROM Sucursal s WHERE s.idSucursal = :id AND s.deletedAt IS NULL")
    Optional<Sucursal> findActivaById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sucursal s " +
	    "WHERE s.deletedAt IS NULL AND LOWER(s.nombreSucursal) = LOWER(:nombre) " +
	    "AND s.empresa.idEmpresa = :empresaId")
    boolean existsActivaByNombreAndEmpresa(@Param("nombre") String nombre,
						 @Param("empresaId") Long empresaId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sucursal s " +
	    "WHERE s.deletedAt IS NULL AND LOWER(s.nombreSucursal) = LOWER(:nombre) " +
	    "AND s.empresa.idEmpresa = :empresaId AND s.idSucursal <> :id")
    boolean existsActivaByNombreAndEmpresaExcludingId(@Param("nombre") String nombre,
							     @Param("empresaId") Long empresaId,
							     @Param("id") Long id);
}
