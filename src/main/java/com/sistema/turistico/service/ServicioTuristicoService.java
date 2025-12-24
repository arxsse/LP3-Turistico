package com.sistema.turistico.service;

import com.sistema.turistico.dto.ServicioResponse;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.ServicioTuristico;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.ServicioTuristicoRepository;
import com.sistema.turistico.repository.SucursalRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ServicioTuristicoService {

    private final ServicioTuristicoRepository servicioRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Crear un nuevo servicio turístico
     */
    public ServicioTuristico create(ServicioTuristico servicio) {
        log.info("Creando nuevo servicio turístico: {}", servicio != null ? servicio.getNombreServicio() : null);

        if (servicio == null) {
            throw new IllegalArgumentException("El servicio es obligatorio");
        }

        Long empresaSolicitada = servicio.getEmpresa() != null ? servicio.getEmpresa().getIdEmpresa() : null;
        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(empresaSolicitada);
        Empresa empresa = obtenerEmpresaActiva(empresaId);

        normalizarServicio(servicio);
        validarServicio(servicio);

        if (servicioRepository.existsByEmpresaIdAndNombreServicioAndIdNot(empresaId, servicio.getNombreServicio(), 0L)) {
            throw new IllegalArgumentException("Ya existe un servicio con este nombre en la empresa");
        }

        servicio.setEmpresa(empresa);

        // Manejar sucursal si está presente
        if (servicio.getSucursal() != null && servicio.getSucursal().getIdSucursal() != null) {
            Sucursal sucursal = obtenerSucursalActiva(servicio.getSucursal().getIdSucursal(), empresaId);
            servicio.setSucursal(sucursal);
        } else {
            servicio.setSucursal(null);
        }

        servicio.setEstado(Boolean.TRUE);
        servicio.setDeletedAt(null);

        ServicioTuristico savedServicio = servicioRepository.save(servicio);
        log.info("Servicio turístico creado exitosamente con ID: {}", savedServicio.getIdServicio());
        return savedServicio;
    }

    /**
     * Buscar servicio por ID
     */
    @Transactional(readOnly = true)
    public Optional<ServicioTuristico> findById(Long id) {
        log.debug("Buscando servicio turístico con ID: {}", id);
        return servicioRepository.findById(id)
            .filter(servicio -> servicio.getDeletedAt() == null)
            .map(servicio -> {
                validarPertenencia(servicio);
                return servicio;
            });
    }

    /**
     * Listar servicios por empresa
     */
    @Transactional(readOnly = true)
    public List<ServicioTuristico> findByEmpresaId(Long empresaId, Long sucursalId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Listando servicios turísticos de empresa ID: {} y sucursal ID: {}", empresaFiltrada, sucursalId);
        if (sucursalId != null) {
            return servicioRepository.findByEmpresaIdAndSucursalId(empresaFiltrada, sucursalId);
        }
        return servicioRepository.findByEmpresaId(empresaFiltrada);
    }

    /**
     * Listar servicios por empresa y tipo
     */
    @Transactional(readOnly = true)
    public List<ServicioTuristico> findByEmpresaIdAndTipoServicio(Long empresaId, ServicioTuristico.TipoServicio tipoServicio, Long sucursalId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Listando servicios de empresa {}, tipo {} y sucursal {}", empresaFiltrada, tipoServicio, sucursalId);
        List<ServicioTuristico> servicios = servicioRepository.findByEmpresaIdAndTipoServicio(empresaFiltrada, tipoServicio);
        if (sucursalId != null) {
            return servicios.stream()
                .filter(s -> s.getSucursal() == null || s.getSucursal().getIdSucursal().equals(sucursalId))
                .toList();
        }
        return servicios;
    }

    /**
     * Listar servicios disponibles para un número de personas
     */
    @Transactional(readOnly = true)
    public List<ServicioTuristico> findDisponiblesByEmpresaIdAndPersonas(Long empresaId, Integer personasRequeridas, Long sucursalId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando servicios disponibles para {} personas en empresa {} y sucursal {}", personasRequeridas, empresaFiltrada, sucursalId);
        List<ServicioTuristico> servicios = servicioRepository.findDisponiblesByEmpresaIdAndPersonas(empresaFiltrada, personasRequeridas);
        if (sucursalId != null) {
            return servicios.stream()
                .filter(s -> s.getSucursal() == null || s.getSucursal().getIdSucursal().equals(sucursalId))
                .toList();
        }
        return servicios;
    }

    /**
     * Buscar servicios con filtros avanzados
     */
    @Transactional(readOnly = true)
    public List<ServicioTuristico> findByEmpresaIdAndBusqueda(Long empresaId, String busqueda, Long sucursalId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando servicios en empresa {} con término: {} y sucursal {}", empresaFiltrada, busqueda, sucursalId);
        List<ServicioTuristico> servicios;
        if (busqueda == null || busqueda.trim().isEmpty()) {
            servicios = servicioRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaFiltrada);
        } else {
            servicios = servicioRepository.findByEmpresaIdAndBusqueda(empresaFiltrada, busqueda.trim());
        }
        if (sucursalId != null) {
            return servicios.stream()
                .filter(s -> s.getSucursal() == null || s.getSucursal().getIdSucursal().equals(sucursalId))
                .toList();
        }
        return servicios;
    }

    /**
     * Buscar servicios por rango de precios
     */
    @Transactional(readOnly = true)
    public List<ServicioTuristico> findByEmpresaIdAndPrecioBetween(Long empresaId, BigDecimal precioMin, BigDecimal precioMax, Long sucursalId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando servicios en empresa {} entre precios {} - {} y sucursal {}", empresaFiltrada, precioMin, precioMax, sucursalId);
        List<ServicioTuristico> servicios = servicioRepository.findByEmpresaIdAndPrecioBetween(empresaFiltrada, precioMin, precioMax);
        if (sucursalId != null) {
            return servicios.stream()
                .filter(s -> s.getSucursal() == null || s.getSucursal().getIdSucursal().equals(sucursalId))
                .toList();
        }
        return servicios;
    }

    /**
     * Actualizar servicio turístico
     */
    public ServicioTuristico update(Long id, ServicioTuristico servicioActualizado) {
        log.info("Actualizando servicio turístico ID: {}", id);

        if (servicioActualizado == null) {
            throw new IllegalArgumentException("El servicio es obligatorio");
        }

        ServicioTuristico servicioExistente = obtenerServicioAutorizado(id);

        normalizarServicio(servicioActualizado);
        validarServicio(servicioActualizado);

        // Validar unicidad de nombre por empresa (excluyendo el servicio actual)
        if (servicioRepository.existsByEmpresaIdAndNombreServicioAndIdNot(servicioExistente.getEmpresa().getIdEmpresa(), servicioActualizado.getNombreServicio(), id)) {
            throw new IllegalArgumentException("Ya existe un servicio con este nombre en la empresa");
        }

        // Manejar sucursal si está presente
        if (servicioActualizado.getSucursal() != null && servicioActualizado.getSucursal().getIdSucursal() != null) {
            Sucursal sucursal = obtenerSucursalActiva(servicioActualizado.getSucursal().getIdSucursal(), servicioExistente.getEmpresa().getIdEmpresa());
            servicioExistente.setSucursal(sucursal);
        } else {
            servicioExistente.setSucursal(null);
        }

        // Actualizar campos
        servicioExistente.setNombreServicio(servicioActualizado.getNombreServicio());
        servicioExistente.setTipoServicio(servicioActualizado.getTipoServicio());
        servicioExistente.setDescripcion(servicioActualizado.getDescripcion());
        servicioExistente.setUbicacionDestino(servicioActualizado.getUbicacionDestino());
        servicioExistente.setDuracion(servicioActualizado.getDuracion());
        servicioExistente.setCapacidadMaxima(servicioActualizado.getCapacidadMaxima());
        servicioExistente.setPrecioBase(servicioActualizado.getPrecioBase());
        servicioExistente.setIncluye(servicioActualizado.getIncluye());
        servicioExistente.setNoIncluye(servicioActualizado.getNoIncluye());
        servicioExistente.setRequisitos(servicioActualizado.getRequisitos());
        servicioExistente.setPoliticasEspeciales(servicioActualizado.getPoliticasEspeciales());
        servicioExistente.setImagenes(servicioActualizado.getImagenes());
        servicioExistente.setItinerario(servicioActualizado.getItinerario());
        servicioExistente.setIdCategoria(servicioActualizado.getIdCategoria());
        servicioExistente.setIdProveedor(servicioActualizado.getIdProveedor());
        if (servicioActualizado.getEstado() != null) {
            servicioExistente.setEstado(servicioActualizado.getEstado());
        }

        ServicioTuristico updatedServicio = servicioRepository.save(servicioExistente);
        log.info("Servicio turístico actualizado exitosamente: {}", updatedServicio.getIdServicio());
        return updatedServicio;
    }

    /**
     * Eliminar servicio turístico (soft delete)
     */
    public void delete(Long id) {
        log.info("Eliminando servicio turístico ID: {} (soft delete)", id);

        ServicioTuristico servicio = obtenerServicioAutorizado(id);

        if (servicio.getDeletedAt() != null) {
            throw new IllegalArgumentException("El servicio ya fue eliminado");
        }

        servicio.setEstado(false);
        servicio.setDeletedAt(LocalDateTime.now());
        servicioRepository.save(servicio);

        log.info("Servicio turístico eliminado exitosamente: {}", id);
    }

    /**
     * Verificar disponibilidad de servicio para un número de personas
     */
    @Transactional(readOnly = true)
    public boolean verificarDisponibilidad(Long servicioId, Integer personasRequeridas) {
        return findById(servicioId)
            .map(servicio -> servicio.isActivo() && servicio.tieneDisponibilidad(personasRequeridas))
            .orElse(false);
    }

    /**
     * Calcular precio total para un servicio
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularPrecioTotal(Long servicioId, Integer personas) {
        ServicioTuristico servicio = obtenerServicioAutorizado(servicioId);

        if (!servicio.isActivo()) {
            throw new IllegalArgumentException("El servicio no está disponible");
        }

        return servicio.calcularPrecioTotal(personas);
    }

    /**
     * Validaciones de negocio para servicio
     */
    private void validarServicio(ServicioTuristico servicio) {
        if (servicio.getCapacidadMaxima() <= 0) {
            throw new IllegalArgumentException("La capacidad máxima debe ser mayor a 0");
        }

        if (servicio.getPrecioBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio base debe ser mayor a 0");
        }

        // Validaciones adicionales según tipo de servicio
        switch (servicio.getTipoServicio()) {
            case Tour:
                if (servicio.getDuracion() == null || servicio.getDuracion().trim().isEmpty()) {
                    throw new IllegalArgumentException("Los tours requieren especificar la duración");
                }
                break;
            case Hotel:
                if (servicio.getUbicacionDestino() == null || servicio.getUbicacionDestino().trim().isEmpty()) {
                    throw new IllegalArgumentException("Los hoteles requieren especificar la ubicación");
                }
                break;
            case Transporte:
                // Validaciones específicas para transporte si es necesario
                break;
            case EntradaAtractivo:
                // Validaciones específicas para entradas si es necesario
                break;
        }
    }

    /**
     * Obtener estadísticas de servicios por empresa
     */
    @Transactional(readOnly = true)
    public Long countByEmpresaId(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        return servicioRepository.countByEmpresaId(empresaFiltrada);
    }

    /**
     * Convertir ServicioTuristico a ServicioResponse
     */
    public ServicioResponse toResponse(ServicioTuristico servicio) {
        return new ServicioResponse(
            servicio.getIdServicio(),
            servicio.getTipoServicio().toString(),
            servicio.getNombreServicio(),
            servicio.getDescripcion(),
            servicio.getUbicacionDestino(),
            servicio.getDuracion(),
            servicio.getCapacidadMaxima(),
            servicio.getPrecioBase(),
            servicio.getIncluye(),
            servicio.getNoIncluye(),
            servicio.getRequisitos(),
            servicio.getPoliticasEspeciales(),
            servicio.getEstado(),
            servicio.getSucursal() != null ? servicio.getSucursal().getIdSucursal() : null,
            servicio.getSucursal() != null ? servicio.getSucursal().getNombreSucursal() : null,
            servicio.getCreatedAt() != null ? servicio.getCreatedAt().toString() : null,
            servicio.getUpdatedAt() != null ? servicio.getUpdatedAt().toString() : null
        );
    }

    private void normalizarServicio(ServicioTuristico servicio) {
        if (servicio.getNombreServicio() != null) {
            servicio.setNombreServicio(servicio.getNombreServicio().trim());
        }
        if (servicio.getDescripcion() != null) {
            servicio.setDescripcion(servicio.getDescripcion().trim());
        }
        if (servicio.getUbicacionDestino() != null) {
            servicio.setUbicacionDestino(servicio.getUbicacionDestino().trim());
        }
        if (servicio.getDuracion() != null) {
            servicio.setDuracion(servicio.getDuracion().trim());
        }
        if (servicio.getIncluye() != null) {
            servicio.setIncluye(servicio.getIncluye().trim());
        }
        if (servicio.getNoIncluye() != null) {
            servicio.setNoIncluye(servicio.getNoIncluye().trim());
        }
        if (servicio.getRequisitos() != null) {
            servicio.setRequisitos(servicio.getRequisitos().trim());
        }
        if (servicio.getPoliticasEspeciales() != null) {
            servicio.setPoliticasEspeciales(servicio.getPoliticasEspeciales().trim());
        }
        if (servicio.getItinerario() != null) {
            servicio.setItinerario(servicio.getItinerario().trim());
        }
    }

    private ServicioTuristico obtenerServicioAutorizado(Long id) {
        ServicioTuristico servicio = servicioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Servicio turístico no encontrado"));
        if (servicio.getDeletedAt() != null) {
            throw new IllegalArgumentException("Servicio turístico no encontrado");
        }
        validarPertenencia(servicio);
        return servicio;
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }

    private Empresa obtenerEmpresaActiva(Long empresaId) {
        return empresaRepository.findActivaById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    private Sucursal obtenerSucursalActiva(Long sucursalId, Long empresaId) {
        Sucursal sucursal = sucursalRepository.findActivaById(sucursalId)
            .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));

        // Validar que la sucursal pertenezca a la empresa
        if (sucursal.getEmpresa() == null || !sucursal.getEmpresa().getIdEmpresa().equals(empresaId)) {
            throw new IllegalArgumentException("La sucursal no pertenece a la empresa especificada");
        }

        return sucursal;
    }

    private void validarPertenencia(ServicioTuristico servicio) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaServicio = servicio.getEmpresa() != null ? servicio.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaServicio)) {
                throw new IllegalArgumentException("El servicio no pertenece a la empresa actual");
            }
        }
    }
}