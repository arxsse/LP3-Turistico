package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Búsquedas básicas
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);

    // Búsquedas por empresa (multiempresa)
    @Query("SELECT c FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND c.estado = true AND c.deletedAt IS NULL")
    List<Cliente> findByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT c FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND (c.sucursal IS NULL OR c.sucursal.idSucursal = :sucursalId) AND c.estado = true AND c.deletedAt IS NULL")
    List<Cliente> findByEmpresaIdAndSucursalId(@Param("empresaId") Long empresaId, @Param("sucursalId") Long sucursalId);

    @Query("SELECT c FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND c.estado = true AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<Cliente> findByEmpresaIdOrderByCreatedAtDesc(@Param("empresaId") Long empresaId);

    // Búsquedas con filtros
    @Query("SELECT c FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND c.estado = true AND c.deletedAt IS NULL " +
           "AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(c.apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR c.dni LIKE CONCAT('%', :busqueda, '%'))")
    List<Cliente> findByEmpresaIdAndBusqueda(@Param("empresaId") Long empresaId, @Param("busqueda") String busqueda);

    // Verificaciones de unicidad por empresa
    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND c.email = :email AND c.idCliente != :excludeId")
    boolean existsByEmpresaIdAndEmailAndIdNot(@Param("empresaId") Long empresaId, @Param("email") String email, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(c) > 0 FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND c.dni = :dni AND c.idCliente != :excludeId")
    boolean existsByEmpresaIdAndDniAndIdNot(@Param("empresaId") Long empresaId, @Param("dni") String dni, @Param("excludeId") Long excludeId);

    // Estadísticas
    @Query("SELECT COUNT(c) FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND c.estado = true AND c.deletedAt IS NULL")
    Long countByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT c.nivelMembresia, COUNT(c) FROM Cliente c WHERE c.empresa.idEmpresa = :empresaId AND c.estado = true AND c.deletedAt IS NULL GROUP BY c.nivelMembresia")
    List<Object[]> countClientesByNivelMembresia(@Param("empresaId") Long empresaId);
}