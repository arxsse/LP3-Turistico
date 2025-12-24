package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Personal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalRepository extends JpaRepository<Personal, Long> {

    // Búsquedas por empresa
    @Query("SELECT p FROM Personal p WHERE p.empresa.idEmpresa = :empresaId AND p.estado = true")
    List<Personal> findByEmpresaId(@Param("empresaId") Long empresaId);

    // Búsquedas por sucursal
    @Query("SELECT p FROM Personal p WHERE p.sucursal.idSucursal = :sucursalId AND p.estado = true")
    List<Personal> findBySucursalId(@Param("sucursalId") Long sucursalId);

    // Búsquedas por cargo
    @Query("SELECT p FROM Personal p WHERE p.cargo = :cargo AND p.empresa.idEmpresa = :empresaId AND p.estado = true")
    List<Personal> findByCargoAndEmpresaId(@Param("cargo") Personal.Cargo cargo, @Param("empresaId") Long empresaId);

    // Búsquedas por estado
    @Query("SELECT p FROM Personal p WHERE p.estado = :estado AND p.empresa.idEmpresa = :empresaId")
    List<Personal> findByEstadoAndEmpresaId(@Param("estado") Boolean estado, @Param("empresaId") Long empresaId);

    // Búsqueda por DNI
    @Query("SELECT p FROM Personal p WHERE p.dni = :dni AND p.empresa.idEmpresa = :empresaId AND p.estado = true")
    Optional<Personal> findByDniAndEmpresaId(@Param("dni") String dni, @Param("empresaId") Long empresaId);

    // Búsqueda por email
    @Query("SELECT p FROM Personal p WHERE p.email = :email AND p.empresa.idEmpresa = :empresaId AND p.estado = true")
    Optional<Personal> findByEmailAndEmpresaId(@Param("email") String email, @Param("empresaId") Long empresaId);

    // Búsqueda con filtros combinados
    @Query("SELECT p FROM Personal p WHERE " +
           "(:empresaId IS NULL OR p.empresa.idEmpresa = :empresaId) AND " +
           "(:sucursalId IS NULL OR p.sucursal.idSucursal = :sucursalId) AND " +
           "(:cargo IS NULL OR p.cargo = :cargo) AND " +
           "(:estado IS NULL OR p.estado = :estado) AND " +
           "(:busqueda IS NULL OR LOWER(CONCAT(p.nombre, ' ', p.apellido)) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<Personal> findWithFilters(@Param("empresaId") Long empresaId,
                                   @Param("sucursalId") Long sucursalId,
                                   @Param("cargo") Personal.Cargo cargo,
                                   @Param("estado") Boolean estado,
                                   @Param("busqueda") String busqueda);

    // Verificar unicidad de DNI por empresa
    @Query("SELECT COUNT(p) > 0 FROM Personal p WHERE p.dni = :dni AND p.empresa.idEmpresa = :empresaId AND p.idPersonal != :excludeId AND p.estado = true")
    boolean existsByDniAndEmpresaIdAndIdNot(@Param("dni") String dni, @Param("empresaId") Long empresaId, @Param("excludeId") Long excludeId);

    // Verificar unicidad de email por empresa
    @Query("SELECT COUNT(p) > 0 FROM Personal p WHERE p.email = :email AND p.empresa.idEmpresa = :empresaId AND p.idPersonal != :excludeId AND p.estado = true")
    boolean existsByEmailAndEmpresaIdAndIdNot(@Param("email") String email, @Param("empresaId") Long empresaId, @Param("excludeId") Long excludeId);

    // Contar personal activo por empresa
    @Query("SELECT COUNT(p) FROM Personal p WHERE p.empresa.idEmpresa = :empresaId AND p.estado = true")
    Long countActivoByEmpresaId(@Param("empresaId") Long empresaId);
}