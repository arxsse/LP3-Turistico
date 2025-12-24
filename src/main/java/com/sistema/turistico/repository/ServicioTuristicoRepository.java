package com.sistema.turistico.repository;

import com.sistema.turistico.entity.ServicioTuristico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ServicioTuristicoRepository extends JpaRepository<ServicioTuristico, Long> {

    // Búsquedas básicas por empresa
    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.estado = true AND s.deletedAt IS NULL")
    List<ServicioTuristico> findByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND (s.sucursal IS NULL OR s.sucursal.idSucursal = :sucursalId) AND s.estado = true AND s.deletedAt IS NULL")
    List<ServicioTuristico> findByEmpresaIdAndSucursalId(@Param("empresaId") Long empresaId, @Param("sucursalId") Long sucursalId);

    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.estado = true AND s.deletedAt IS NULL ORDER BY s.createdAt DESC")
    List<ServicioTuristico> findByEmpresaIdOrderByCreatedAtDesc(@Param("empresaId") Long empresaId);

    // Búsquedas por tipo de servicio
    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.tipoServicio = :tipoServicio AND s.estado = true AND s.deletedAt IS NULL")
    List<ServicioTuristico> findByEmpresaIdAndTipoServicio(@Param("empresaId") Long empresaId, @Param("tipoServicio") ServicioTuristico.TipoServicio tipoServicio);

    // Búsquedas por categoría
    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.idCategoria = :categoriaId AND s.estado = true AND s.deletedAt IS NULL")
    List<ServicioTuristico> findByEmpresaIdAndCategoriaId(@Param("empresaId") Long empresaId, @Param("categoriaId") Long categoriaId);

    // Búsquedas con filtros avanzados
    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.estado = true AND s.deletedAt IS NULL " +
           "AND (LOWER(s.nombreServicio) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(s.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')) " +
           "OR LOWER(s.ubicacionDestino) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<ServicioTuristico> findByEmpresaIdAndBusqueda(@Param("empresaId") Long empresaId, @Param("busqueda") String busqueda);

    // Búsquedas por rango de precios
    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.estado = true AND s.deletedAt IS NULL " +
           "AND s.precioBase BETWEEN :precioMin AND :precioMax")
    List<ServicioTuristico> findByEmpresaIdAndPrecioBetween(@Param("empresaId") Long empresaId,
                                                            @Param("precioMin") BigDecimal precioMin,
                                                            @Param("precioMax") BigDecimal precioMax);

    // Servicios disponibles (con capacidad suficiente)
    @Query("SELECT s FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.estado = true AND s.deletedAt IS NULL " +
           "AND s.capacidadMaxima >= :personasRequeridas")
    List<ServicioTuristico> findDisponiblesByEmpresaIdAndPersonas(@Param("empresaId") Long empresaId,
                                                                  @Param("personasRequeridas") Integer personasRequeridas);

    // Estadísticas
    @Query("SELECT COUNT(s) FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.estado = true AND s.deletedAt IS NULL")
    Long countByEmpresaId(@Param("empresaId") Long empresaId);

    @Query("SELECT s.tipoServicio, COUNT(s) FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.estado = true AND s.deletedAt IS NULL GROUP BY s.tipoServicio")
    List<Object[]> countServiciosByTipo(@Param("empresaId") Long empresaId);

    // Verificaciones de unicidad
    @Query("SELECT COUNT(s) > 0 FROM ServicioTuristico s WHERE s.empresa.idEmpresa = :empresaId AND s.nombreServicio = :nombreServicio AND s.idServicio != :excludeId")
    boolean existsByEmpresaIdAndNombreServicioAndIdNot(@Param("empresaId") Long empresaId,
                                                       @Param("nombreServicio") String nombreServicio,
                                                       @Param("excludeId") Long excludeId);
}