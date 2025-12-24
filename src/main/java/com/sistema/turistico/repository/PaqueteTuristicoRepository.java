package com.sistema.turistico.repository;

import com.sistema.turistico.entity.PaqueteTuristico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaqueteTuristicoRepository extends JpaRepository<PaqueteTuristico, Long> {

    // Búsquedas multiempresa
    List<PaqueteTuristico> findByEmpresaIdEmpresa(Long empresaId);

    @Query("SELECT p FROM PaqueteTuristico p WHERE p.empresa.idEmpresa = :empresaId AND (p.sucursal IS NULL OR p.sucursal.idSucursal = :sucursalId) AND p.estado = true AND p.deletedAt IS NULL")
    List<PaqueteTuristico> findByEmpresaIdAndSucursalId(@Param("empresaId") Long empresaId, @Param("sucursalId") Long sucursalId);

    List<PaqueteTuristico> findByEmpresaIdEmpresaAndEstado(Long empresaId, Boolean estado);

    List<PaqueteTuristico> findByEmpresaIdEmpresaOrderByCreatedAtDesc(Long empresaId);

    // Búsquedas con filtros
    @Query("SELECT p FROM PaqueteTuristico p WHERE p.empresa.idEmpresa = :empresaId AND " +
           "(LOWER(p.nombrePaquete) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))")
    List<PaqueteTuristico> findByEmpresaIdAndBusqueda(@Param("empresaId") Long empresaId,
                                                     @Param("busqueda") String busqueda);

    // Filtros por precio
    List<PaqueteTuristico> findByEmpresaIdEmpresaAndPrecioTotalBetween(Long empresaId,
                                                                       BigDecimal precioMin,
                                                                       BigDecimal precioMax);

    // Filtros por duración
    List<PaqueteTuristico> findByEmpresaIdEmpresaAndDuracionDiasBetween(Long empresaId,
                                                                        Integer duracionMin,
                                                                        Integer duracionMax);

    // Paquetes en promoción
    List<PaqueteTuristico> findByEmpresaIdEmpresaAndPromocionAndEstado(Long empresaId,
                                                                       Boolean promocion,
                                                                       Boolean estado);

    // Verificaciones de unicidad
    boolean existsByEmpresaIdEmpresaAndNombrePaqueteAndIdPaqueteNot(Long empresaId,
                                                                   String nombrePaquete,
                                                                   Long idPaquete);

    // Estadísticas
    long countByEmpresaIdEmpresa(Long empresaId);

    long countByEmpresaIdEmpresaAndEstado(Long empresaId, Boolean estado);

    @Query("SELECT SUM(p.precioTotal) FROM PaqueteTuristico p WHERE p.empresa.idEmpresa = :empresaId AND p.estado = true")
    BigDecimal sumPrecioTotalByEmpresaId(@Param("empresaId") Long empresaId);

    // Paquetes con servicios incluidos (JOIN FETCH para evitar N+1)
    @Query("SELECT DISTINCT p FROM PaqueteTuristico p " +
           "LEFT JOIN FETCH p.serviciosIncluidos ps " +
           "LEFT JOIN FETCH ps.servicio s " +
           "WHERE p.empresa.idEmpresa = :empresaId AND p.estado = true " +
           "ORDER BY p.nombrePaquete")
    List<PaqueteTuristico> findPaquetesConServiciosByEmpresaId(@Param("empresaId") Long empresaId);

    // Paquetes disponibles (con al menos un servicio activo)
    @Query("SELECT p FROM PaqueteTuristico p WHERE p.empresa.idEmpresa = :empresaId " +
           "AND p.estado = true AND p.deletedAt IS NULL " +
           "AND EXISTS (SELECT 1 FROM p.serviciosIncluidos ps WHERE ps.servicio.estado = true)")
    List<PaqueteTuristico> findPaquetesDisponiblesByEmpresaId(@Param("empresaId") Long empresaId);

    // Paquetes por rango de precios con descuento aplicado
    @Query("SELECT p FROM PaqueteTuristico p WHERE p.empresa.idEmpresa = :empresaId " +
           "AND p.estado = true AND " +
           "(p.precioTotal - (p.precioTotal * p.descuento / 100)) BETWEEN :precioMin AND :precioMax")
    List<PaqueteTuristico> findByEmpresaIdAndPrecioFinalBetween(@Param("empresaId") Long empresaId,
                                                               @Param("precioMin") BigDecimal precioMin,
                                                               @Param("precioMax") BigDecimal precioMax);
}