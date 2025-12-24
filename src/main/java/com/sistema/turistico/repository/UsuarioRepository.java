package com.sistema.turistico.repository;

import com.sistema.turistico.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.rol r LEFT JOIN FETCH u.empresa LEFT JOIN FETCH r.permisos WHERE u.email = :email AND u.estado = 1")
    Optional<Usuario> findByEmailWithRolAndEmpresa(@Param("email") String email);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.permisos WHERE u.idUsuario = :idUsuario")
    Optional<Usuario> findByIdWithPermisos(@Param("idUsuario") Long idUsuario);

    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NULL ORDER BY u.createdAt DESC")
    List<Usuario> findAllActivos();

    @Query("SELECT u FROM Usuario u WHERE u.deletedAt IS NULL " +
           "AND (:empresaId IS NULL OR u.empresa.idEmpresa = :empresaId) " +
           "AND (:rolId IS NULL OR u.rol.idRol = :rolId) " +
           "AND (:estado IS NULL OR u.estado = :estado) " +
           "AND (:busqueda IS NULL " +
           "OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR u.dni LIKE CONCAT('%', :busqueda, '%')) ORDER BY u.createdAt DESC")
    List<Usuario> findByFiltros(@Param("busqueda") String busqueda,
                                @Param("estado") Integer estado,
                                @Param("empresaId") Long empresaId,
                                @Param("rolId") Long rolId);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.rol LEFT JOIN FETCH u.empresa WHERE u.idUsuario = :id AND u.deletedAt IS NULL")
    Optional<Usuario> findActivoById(@Param("id") Long id);

    boolean existsByEmailAndIdUsuarioNot(String email, Long idUsuario);

    boolean existsByDniAndIdUsuarioNot(String dni, Long idUsuario);
}