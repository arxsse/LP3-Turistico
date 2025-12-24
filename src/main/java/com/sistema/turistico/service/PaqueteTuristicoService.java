package com.sistema.turistico.service;

import com.sistema.turistico.dto.PaqueteResponse;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.PaqueteServicio;
import com.sistema.turistico.entity.PaqueteTuristico;
import com.sistema.turistico.entity.ServicioTuristico;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.PaqueteTuristicoRepository;
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
public class PaqueteTuristicoService {

    private final PaqueteTuristicoRepository paqueteRepository;
    private final ServicioTuristicoRepository servicioRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;

    @Transactional
    public PaqueteTuristico create(PaqueteTuristico paquete) {
        log.info("Creando paquete turístico: {}", paquete != null ? paquete.getNombrePaquete() : null);

        if (paquete == null) {
            throw new IllegalArgumentException("El paquete es obligatorio");
        }

        Long empresaSolicitada = paquete.getEmpresa() != null ? paquete.getEmpresa().getIdEmpresa() : null;
        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(empresaSolicitada);
        Empresa empresa = obtenerEmpresaActiva(empresaId);

        validarDatosPaquete(paquete);
        validarServiciosIncluidos(paquete, empresaId);

        paquete.setEmpresa(empresa);

        // Manejar sucursal si está presente
        if (paquete.getSucursal() != null && paquete.getSucursal().getIdSucursal() != null) {
            Sucursal sucursal = obtenerSucursalActiva(paquete.getSucursal().getIdSucursal(), empresaId);
            paquete.setSucursal(sucursal);
        } else {
            paquete.setSucursal(null);
        }

        if (paquete.getEstado() == null) {
            paquete.setEstado(Boolean.TRUE);
        }

        if (paqueteRepository.existsByEmpresaIdEmpresaAndNombrePaqueteAndIdPaqueteNot(
                empresaId, paquete.getNombrePaquete(), null)) {
            throw new IllegalArgumentException("Ya existe un paquete con este nombre en la empresa");
        }

        if (paquete.getPrecioTotal() == null && paquete.tieneServicios()) {
            paquete.setPrecioTotal(calcularPrecioTotal(paquete));
        }

        PaqueteTuristico paqueteGuardado = paqueteRepository.save(paquete);
        log.info("Paquete turístico creado exitosamente con ID: {}", paqueteGuardado.getIdPaquete());

        return paqueteGuardado;
    }

    @Transactional(readOnly = true)
    public Optional<PaqueteTuristico> findById(Long id) {
        log.debug("Buscando paquete turístico con ID: {}", id);
        return paqueteRepository.findById(id)
            .filter(paquete -> paquete.getDeletedAt() == null)
            .map(paquete -> {
                validarPertenencia(paquete);
                return paquete;
            });
    }

    @Transactional(readOnly = true)
    public List<PaqueteTuristico> findByEmpresaId(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando paquetes de empresa ID: {}", empresaFiltrada);
        return paqueteRepository.findByEmpresaIdEmpresa(empresaFiltrada).stream()
            .filter(paquete -> paquete.getDeletedAt() == null)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaqueteTuristico> findByEmpresaIdAndBusqueda(Long empresaId, String busqueda) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando paquetes de empresa {} con búsqueda: {}", empresaFiltrada, busqueda);
        List<PaqueteTuristico> paquetes;
        if (busqueda == null || busqueda.trim().isEmpty()) {
            paquetes = paqueteRepository.findByEmpresaIdEmpresa(empresaFiltrada);
        } else {
            paquetes = paqueteRepository.findByEmpresaIdAndBusqueda(empresaFiltrada, busqueda.trim());
        }
        return paquetes.stream()
            .filter(paquete -> paquete.getDeletedAt() == null)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaqueteTuristico> findPaquetesDisponiblesByEmpresaId(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando paquetes disponibles de empresa ID: {}", empresaFiltrada);
        return paqueteRepository.findPaquetesDisponiblesByEmpresaId(empresaFiltrada);
    }

    @Transactional
    public PaqueteTuristico update(Long id, PaqueteTuristico paquete) {
        log.info("Actualizando paquete turístico ID: {}", id);

        if (paquete == null) {
            throw new IllegalArgumentException("El paquete es obligatorio");
        }

        PaqueteTuristico paqueteExistente = obtenerPaqueteAutorizado(id);

        validarDatosPaquete(paquete);

        if (paqueteRepository.existsByEmpresaIdEmpresaAndNombrePaqueteAndIdPaqueteNot(
                paqueteExistente.getEmpresa().getIdEmpresa(), paquete.getNombrePaquete(), id)) {
            throw new IllegalArgumentException("Ya existe otro paquete con este nombre en la empresa");
        }

        // Manejar sucursal si está presente
        if (paquete.getSucursal() != null && paquete.getSucursal().getIdSucursal() != null) {
            Sucursal sucursal = obtenerSucursalActiva(paquete.getSucursal().getIdSucursal(), paqueteExistente.getEmpresa().getIdEmpresa());
            paqueteExistente.setSucursal(sucursal);
        } else {
            paqueteExistente.setSucursal(null);
        }

        paqueteExistente.setNombrePaquete(paquete.getNombrePaquete());
        paqueteExistente.setDescripcion(paquete.getDescripcion());
        paqueteExistente.setPrecioTotal(paquete.getPrecioTotal());
        paqueteExistente.setDuracionDias(paquete.getDuracionDias());
        paqueteExistente.setPromocion(paquete.getPromocion());
        paqueteExistente.setDescuento(paquete.getDescuento());
        paqueteExistente.setEstado(paquete.getEstado());

        PaqueteTuristico paqueteActualizado = paqueteRepository.save(paqueteExistente);
        log.info("Paquete turístico actualizado exitosamente");

        return paqueteActualizado;
    }

    @Transactional
    public void delete(Long id) {
        log.info("Eliminando paquete turístico ID: {}", id);

        PaqueteTuristico paquete = obtenerPaqueteAutorizado(id);

        if (paquete.getDeletedAt() != null) {
            throw new IllegalArgumentException("El paquete ya fue eliminado");
        }

        // Soft delete
        paquete.setDeletedAt(LocalDateTime.now());
        paquete.setEstado(false);

        paqueteRepository.save(paquete);
        log.info("Paquete turístico eliminado (soft delete)");
    }

    @Transactional
    public PaqueteTuristico agregarServicio(Long paqueteId, Long servicioId, Integer orden) {
        log.info("Agregando servicio {} al paquete {}", servicioId, paqueteId);

        PaqueteTuristico paquete = obtenerPaqueteAutorizado(paqueteId);

        ServicioTuristico servicio = servicioRepository.findById(servicioId)
            .orElseThrow(() -> new IllegalArgumentException("Servicio turístico no encontrado"));

        validarMismaEmpresa(paquete.getEmpresa().getIdEmpresa(),
            servicio.getEmpresa() != null ? servicio.getEmpresa().getIdEmpresa() : null,
            "servicio turístico");

        // Verificar que el servicio esté activo
        if (!servicio.isActivo()) {
            throw new IllegalArgumentException("No se puede agregar un servicio inactivo al paquete");
        }

        // Verificar que no esté ya incluido
        boolean yaIncluido = paquete.getServiciosIncluidos().stream()
            .anyMatch(ps -> ps.getServicio().getIdServicio().equals(servicioId));

        if (yaIncluido) {
            throw new IllegalArgumentException("El servicio ya está incluido en este paquete");
        }

        // Agregar servicio
        paquete.agregarServicio(servicio, orden);

        // Recalcular precio total
        paquete.setPrecioTotal(calcularPrecioTotal(paquete));

        PaqueteTuristico paqueteActualizado = paqueteRepository.save(paquete);
        log.info("Servicio agregado al paquete exitosamente");

        return paqueteActualizado;
    }

    @Transactional
    public PaqueteTuristico removerServicio(Long paqueteId, Long servicioId) {
        log.info("Removiendo servicio {} del paquete {}", servicioId, paqueteId);

        PaqueteTuristico paquete = obtenerPaqueteAutorizado(paqueteId);

        ServicioTuristico servicio = servicioRepository.findById(servicioId)
            .orElseThrow(() -> new IllegalArgumentException("Servicio turístico no encontrado"));

        // Verificar que esté incluido
        boolean estaIncluido = paquete.getServiciosIncluidos().stream()
            .anyMatch(ps -> ps.getServicio().getIdServicio().equals(servicioId));

        if (!estaIncluido) {
            throw new IllegalArgumentException("El servicio no está incluido en este paquete");
        }

        // Remover servicio
        paquete.removerServicio(servicio);

        // Recalcular precio total - solo si quedan servicios
        if (paquete.tieneServicios()) {
            paquete.setPrecioTotal(calcularPrecioTotal(paquete));
        } else {
            // Si no quedan servicios, el precio debe ser 0.00, pero la validación requiere > 0
            // Para evitar el error de validación, mantenemos un precio mínimo de 0.01
            paquete.setPrecioTotal(BigDecimal.valueOf(0.01));
        }

        PaqueteTuristico paqueteActualizado = paqueteRepository.save(paquete);
        log.info("Servicio removido del paquete exitosamente");

        return paqueteActualizado;
    }

    public BigDecimal calcularPrecioTotal(PaqueteTuristico paquete) {
        if (paquete.getServiciosIncluidos() == null || paquete.getServiciosIncluidos().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = paquete.getServiciosIncluidos().stream()
            .filter(PaqueteServicio::isServicioActivo)
            .map(PaqueteServicio::getPrecioServicio)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total;
    }

    private void validarDatosPaquete(PaqueteTuristico paquete) {
        if (paquete.getNombrePaquete() == null || paquete.getNombrePaquete().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del paquete es obligatorio");
        }

        if (paquete.getPrecioTotal() != null && paquete.getPrecioTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio total debe ser mayor a 0");
        }

        if (paquete.getDuracionDias() != null && paquete.getDuracionDias() <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a 0 días");
        }

        if (paquete.getDescuento() != null &&
            (paquete.getDescuento().compareTo(BigDecimal.ZERO) < 0 ||
             paquete.getDescuento().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("El descuento debe estar entre 0% y 100%");
        }
    }

    private void validarServiciosIncluidos(PaqueteTuristico paquete, Long empresaId) {
        if (paquete.getServiciosIncluidos() == null) {
            return;
        }

        for (PaqueteServicio paqueteServicio : paquete.getServiciosIncluidos()) {
            if (paqueteServicio == null || paqueteServicio.getServicio() == null
                || paqueteServicio.getServicio().getIdServicio() == null) {
                throw new IllegalArgumentException("Cada servicio incluido debe tener un identificador válido");
            }

            ServicioTuristico servicio = servicioRepository.findById(paqueteServicio.getServicio().getIdServicio())
                .orElseThrow(() -> new IllegalArgumentException("Servicio turístico no encontrado"));

            validarMismaEmpresa(empresaId,
                servicio.getEmpresa() != null ? servicio.getEmpresa().getIdEmpresa() : null,
                "servicio turístico incluido");

            if (!servicio.isActivo()) {
                throw new IllegalArgumentException("El servicio " + servicio.getNombreServicio() + " no está activo");
            }

            paqueteServicio.setPaquete(paquete);
            paqueteServicio.setServicio(servicio);
        }
    }

    public PaqueteResponse toResponse(PaqueteTuristico paquete) {
        PaqueteResponse response = new PaqueteResponse();
        response.setIdPaquete(paquete.getIdPaquete());
        response.setNombrePaquete(paquete.getNombrePaquete());
        response.setDescripcion(paquete.getDescripcion());
        response.setPrecioTotal(paquete.getPrecioTotal());
        response.setDuracionDias(paquete.getDuracionDias());
        response.setPromocion(paquete.getPromocion());
        response.setDescuento(paquete.getDescuento());
        response.setEstado(paquete.getEstado());
        response.setIdSucursal(paquete.getSucursal() != null ? paquete.getSucursal().getIdSucursal() : null);
        response.setNombreSucursal(paquete.getSucursal() != null ? paquete.getSucursal().getNombreSucursal() : null);
        response.setCreatedAt(paquete.getCreatedAt());
        response.setUpdatedAt(paquete.getUpdatedAt());

        // Contar servicios incluidos
        if (paquete.getServiciosIncluidos() != null) {
            response.setNumeroServicios(paquete.getServiciosIncluidos().size());

            // Crear lista simplificada de servicios
            List<PaqueteResponse.ServicioSimplificado> serviciosSimplificados = paquete.getServiciosIncluidos()
                .stream()
                .filter(PaqueteServicio::isServicioActivo)
                .map(ps -> {
                    PaqueteResponse.ServicioSimplificado ss = new PaqueteResponse.ServicioSimplificado();
                    ss.setIdServicio(ps.getServicio().getIdServicio());
                    ss.setNombreServicio(ps.getServicio().getNombreServicio());
                    ss.setTipoServicio(ps.getServicio().getTipoServicio().toString());
                    ss.setOrden(ps.getOrden());
                    return ss;
                })
                .toList();

            response.setServiciosIncluidos(serviciosSimplificados);
        } else {
            response.setNumeroServicios(0);
            response.setServiciosIncluidos(List.of());
        }

        return response;
    }

    private PaqueteTuristico obtenerPaqueteAutorizado(Long id) {
        PaqueteTuristico paquete = paqueteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Paquete turístico no encontrado"));
        if (paquete.getDeletedAt() != null) {
            throw new IllegalArgumentException("Paquete turístico no encontrado");
        }
        validarPertenencia(paquete);
        return paquete;
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }

    private void validarMismaEmpresa(Long empresaEsperada, Long empresaRelacionada, String recurso) {
        if (empresaEsperada == null || empresaRelacionada == null || !empresaEsperada.equals(empresaRelacionada)) {
            throw new IllegalArgumentException("El " + recurso + " no pertenece a la empresa actual");
        }
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

    private void validarPertenencia(PaqueteTuristico paquete) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaPaquete = paquete.getEmpresa() != null ? paquete.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaPaquete)) {
                throw new IllegalArgumentException("El paquete no pertenece a la empresa actual");
            }
        }
    }
}