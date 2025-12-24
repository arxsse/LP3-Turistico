package com.sistema.turistico.service;

import com.sistema.turistico.dto.AsignacionPersonalRequest;
import com.sistema.turistico.dto.AsignacionPersonalUpdateRequest;
import com.sistema.turistico.entity.AsignacionPersonal;
import com.sistema.turistico.entity.Personal;
import com.sistema.turistico.entity.Reserva;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.Sucursal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sistema.turistico.repository.AsignacionPersonalRepository;
import com.sistema.turistico.repository.PersonalRepository;
import com.sistema.turistico.repository.ReservaRepository;
import com.sistema.turistico.repository.SucursalRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsignacionPersonalService {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    private final AsignacionPersonalRepository asignacionRepository;
    private final PersonalRepository personalRepository;
    private final ReservaRepository reservaRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Crear nueva asignación de personal
     */
    @Transactional
    public AsignacionPersonal create(AsignacionPersonalRequest request) {
        log.info("Creando asignación de personal: {} para reserva {}", request.getIdPersonal(), request.getIdReserva());

        // Validar que el personal existe y está activo
        Personal personal = personalRepository.findById(request.getIdPersonal())
            .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado"));

        if (!personal.isActivo()) {
            throw new IllegalArgumentException("El personal no está activo");
        }

        Reserva reserva = reservaRepository.findById(request.getIdReserva())
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        Long empresaReserva = extractEmpresaId(reserva);
        ensureEmpresaAcceso(empresaReserva);
        ensureMismaEmpresa(personal, empresaReserva);

        // Usar la fecha de asignación proporcionada o tomar la fecha del servicio de la reserva
        Date fechaAsignacion = request.getFechaAsignacion() != null ?
            request.getFechaAsignacion() : reserva.getFechaServicio();

        // Validar que no haya conflicto de asignación en la misma fecha
        if (asignacionRepository.existsByPersonalAndFecha(request.getIdPersonal(), fechaAsignacion)) {
            throw new IllegalArgumentException("El personal ya está asignado en esta fecha");
        }

        // Validar que la fecha de asignación coincida con la fecha del servicio
        if (!fechaAsignacion.equals(reserva.getFechaServicio())) {
            throw new IllegalArgumentException("La fecha de asignación debe coincidir con la fecha del servicio");
        }

        // Crear la asignación
        AsignacionPersonal asignacion = new AsignacionPersonal(personal, reserva, fechaAsignacion);
        asignacion.setObservaciones(request.getObservaciones());
        asignacion.setEstado(AsignacionPersonal.EstadoAsignacion.Asignado);

        AsignacionPersonal persisted = asignacionRepository.save(asignacion);
        registrarAudit("ASIGNACION_CREATE", persisted, request.getObservaciones());
        return persisted;
    }

    /**
     * Crear nueva asignación de personal (sobrecarga para compatibilidad)
     */
    @Transactional
    public AsignacionPersonal create(AsignacionPersonal asignacion) {
        log.info("Creando asignación de personal: {} para reserva {}", asignacion.getPersonal().getIdPersonal(), asignacion.getReserva().getIdReserva());

        // Validar que el personal existe y está activo
        Personal personal = personalRepository.findById(asignacion.getPersonal().getIdPersonal())
            .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado"));

        if (!personal.isActivo()) {
            throw new IllegalArgumentException("El personal no está activo");
        }

        Reserva reserva = reservaRepository.findById(asignacion.getReserva().getIdReserva())
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        Long empresaReserva = extractEmpresaId(reserva);
        ensureEmpresaAcceso(empresaReserva);
        ensureMismaEmpresa(personal, empresaReserva);

        // Validar que no haya conflicto de asignación en la misma fecha
        if (asignacionRepository.existsByPersonalAndFecha(personal.getIdPersonal(), asignacion.getFechaAsignacion())) {
            throw new IllegalArgumentException("El personal ya está asignado en esta fecha");
        }

        // Validar que la fecha de asignación coincida con la fecha del servicio
        if (!asignacion.getFechaAsignacion().equals(reserva.getFechaServicio())) {
            throw new IllegalArgumentException("La fecha de asignación debe coincidir con la fecha del servicio");
        }

        asignacion.setEstado(AsignacionPersonal.EstadoAsignacion.Asignado);
        AsignacionPersonal persisted = asignacionRepository.save(asignacion);
        registrarAudit("ASIGNACION_CREATE", persisted, asignacion.getObservaciones());
        return persisted;
    }

    /**
     * Buscar asignación por ID
     */
    public Optional<AsignacionPersonal> findById(Long id) {
        return asignacionRepository.findById(id)
            .map(asignacion -> {
                ensureAsignacionAcceso(asignacion);
                return asignacion;
            });
    }

    /**
     * Listar asignaciones por reserva
     */
    public List<AsignacionPersonal> findByReservaId(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        Long empresaReserva = extractEmpresaId(reserva);
        ensureEmpresaAcceso(empresaReserva);

        return asignacionRepository.findDetailedByReservaId(reservaId).stream()
            .filter(this::perteneceALaEmpresaActual)
            .collect(Collectors.toList());
    }

    /**
     * Listar asignaciones por personal
     */
    public List<AsignacionPersonal> findByPersonalId(Long personalId) {
        Personal personal = personalRepository.findById(personalId)
            .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado"));

        Long empresaPersonal = extractEmpresaId(personal);
        ensureEmpresaAcceso(empresaPersonal);

        return asignacionRepository.findByPersonalId(personalId).stream()
            .filter(this::perteneceALaEmpresaActual)
            .collect(Collectors.toList());
    }

    /**
     * Listar asignaciones por fecha
     */
    public List<AsignacionPersonal> findByFecha(Date fecha) {
        return asignacionRepository.findByFechaAsignacion(fecha).stream()
            .filter(this::perteneceALaEmpresaActual)
            .collect(Collectors.toList());
    }

    /**
     * Listar asignaciones por rango de fechas
     */
    public List<AsignacionPersonal> findByFechaBetween(Date fechaInicio, Date fechaFin) {
        return asignacionRepository.findByFechaAsignacionBetween(fechaInicio, fechaFin).stream()
            .filter(this::perteneceALaEmpresaActual)
            .collect(Collectors.toList());
    }

    /**
     * Actualizar asignación
     */
    @Transactional
    public AsignacionPersonal update(Long id, AsignacionPersonal asignacionActualizada) {
        log.info("Actualizando asignación ID: {}", id);

        AsignacionPersonal asignacionExistente = asignacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        ensureAsignacionAcceso(asignacionExistente);

        // Validar cambios si es necesario
        if (asignacionActualizada.getObservaciones() != null) {
            asignacionExistente.setObservaciones(asignacionActualizada.getObservaciones());
        }

        AsignacionPersonal persisted = asignacionRepository.save(asignacionExistente);
        registrarAudit("ASIGNACION_UPDATE", persisted, asignacionActualizada.getObservaciones());
        return persisted;
    }

    /**
     * Actualizar asignación con cambio de personal
     */
    @Transactional
    public AsignacionPersonal update(Long id, AsignacionPersonalUpdateRequest request) {
        log.info("Actualizando asignación ID: {} con nuevo personal: {}", id, request.getIdPersonal());

        AsignacionPersonal asignacionExistente = asignacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        ensureAsignacionAcceso(asignacionExistente);

        // Validar que el nuevo personal existe y está activo
        if (!request.getIdPersonal().equals(asignacionExistente.getPersonal().getIdPersonal())) {
            Personal nuevoPersonal = personalRepository.findById(request.getIdPersonal())
                .orElseThrow(() -> new IllegalArgumentException("Nuevo personal no encontrado"));

            if (!nuevoPersonal.isActivo()) {
                throw new IllegalArgumentException("El nuevo personal no está activo");
            }

            ensureMismaEmpresa(nuevoPersonal, extractEmpresaId(asignacionExistente.getReserva()));

            if (asignacionRepository.existsByPersonalAndFecha(request.getIdPersonal(), asignacionExistente.getFechaAsignacion())) {
                throw new IllegalArgumentException("El nuevo personal ya está asignado en esta fecha");
            }

            asignacionExistente.setPersonal(nuevoPersonal);
        }

        // Actualizar observaciones
        asignacionExistente.setObservaciones(request.getObservaciones());

        AsignacionPersonal persisted = asignacionRepository.save(asignacionExistente);
        registrarAudit("ASIGNACION_UPDATE", persisted, request.getObservaciones());
        return persisted;
    }

    /**
     * Marcar asignación como completada
     */
    @Transactional
    public AsignacionPersonal completar(Long id) {
        log.info("Marcando asignación ID {} como completada", id);

        AsignacionPersonal asignacion = asignacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        ensureAsignacionAcceso(asignacion);

        if (asignacion.getEstado() != AsignacionPersonal.EstadoAsignacion.Asignado) {
            throw new IllegalArgumentException("Solo se pueden completar asignaciones en estado 'Asignado'");
        }

        asignacion.setEstado(AsignacionPersonal.EstadoAsignacion.Completado);
        AsignacionPersonal persisted = asignacionRepository.save(asignacion);
        registrarAudit("ASIGNACION_COMPLETE", persisted, "Asignación completada");
        return persisted;
    }

    /**
     * Cancelar asignación
     */
    @Transactional
    public AsignacionPersonal cancelar(Long id, String motivo) {
        log.info("Cancelando asignación ID: {}", id);

        AsignacionPersonal asignacion = asignacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        ensureAsignacionAcceso(asignacion);

        if (asignacion.getEstado() == AsignacionPersonal.EstadoAsignacion.Completado) {
            throw new IllegalArgumentException("No se puede cancelar una asignación completada");
        }

        asignacion.setEstado(AsignacionPersonal.EstadoAsignacion.Cancelado);
        asignacion.setObservaciones(motivo != null ? motivo : "Cancelado por el sistema");

        AsignacionPersonal persisted = asignacionRepository.save(asignacion);
        registrarAudit("ASIGNACION_CANCEL", persisted, motivo);
        return persisted;
    }

    /**
     * Eliminar asignación
     */
    @Transactional
    public void delete(Long id) {
        log.info("Eliminando asignación ID: {}", id);

        AsignacionPersonal asignacion = asignacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        ensureAsignacionAcceso(asignacion);

        asignacionRepository.delete(asignacion);
        registrarAudit("ASIGNACION_DELETE", asignacion, "Asignación eliminada");
    }

    /**
     * Verificar disponibilidad de personal en fecha específica
     */
    @Transactional(readOnly = true)
    public boolean verificarDisponibilidadPersonal(Long personalId, Date fecha) {
        Personal personal = personalRepository.findById(personalId)
            .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado"));

        ensureEmpresaAcceso(extractEmpresaId(personal));

        return !asignacionRepository.existsByPersonalAndFecha(personalId, fecha);
    }

    /**
     * Obtener asignaciones activas por personal
     */
    public Long countAsignacionesActivasByPersonal(Long personalId) {
        Personal personal = personalRepository.findById(personalId)
            .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado"));

        ensureEmpresaAcceso(extractEmpresaId(personal));

        return asignacionRepository.countAsignacionesActivasByPersonal(personalId);
    }

    private void registrarAudit(String accion, AsignacionPersonal asignacion, String detalle) {
        if (asignacion == null) {
            return;
        }

        Long usuarioId = TenantContext.getUserId().orElse(null);
        Long reservaId = asignacion.getReserva() != null ? asignacion.getReserva().getIdReserva() : null;
        Long empresaId = null;
        if (asignacion.getReserva() != null) {
            try {
                empresaId = extractEmpresaId(asignacion.getReserva());
            } catch (IllegalArgumentException ex) {
                log.warn("No se pudo resolver la empresa para la auditoría de la asignación {}", asignacion.getIdAsignacion(), ex);
            }
        }
        Long personalId = asignacion.getPersonal() != null ? asignacion.getPersonal().getIdPersonal() : null;

        AUDIT_LOG.info(
            "accion={}, asignacionId={}, reservaId={}, personalId={}, empresaId={}, usuarioId={}, estado={}, detalle={}",
            accion,
            asignacion.getIdAsignacion(),
            reservaId,
            personalId,
            empresaId,
            usuarioId,
            asignacion.getEstado(),
            detalle != null ? detalle : ""
        );
    }

    private void ensureAsignacionAcceso(AsignacionPersonal asignacion) {
        Long empresaReserva = extractEmpresaId(asignacion.getReserva());
        ensureEmpresaAcceso(empresaReserva);
    }

    private void ensureEmpresaAcceso(Long empresaId) {
        TenantContext.requireEmpresaIdOrCurrent(empresaId, "No tiene acceso a la empresa especificada");
    }

    private void ensureMismaEmpresa(Personal personal, Long empresaReserva) {
        Long empresaPersonal = extractEmpresaId(personal);
        if (!empresaPersonal.equals(empresaReserva)) {
            throw new IllegalArgumentException("El personal no pertenece a la empresa de la reserva");
        }
    }

    private Long extractEmpresaId(Reserva reserva) {
        if (reserva == null || reserva.getEmpresa() == null) {
            throw new IllegalArgumentException("La reserva no tiene una empresa asociada");
        }
        Empresa empresa = reserva.getEmpresa();
        if (empresa.getIdEmpresa() == null) {
            throw new IllegalArgumentException("La reserva no tiene una empresa válida");
        }
        return empresa.getIdEmpresa();
    }

    private Long extractEmpresaId(Personal personal) {
        if (personal == null || personal.getEmpresa() == null) {
            throw new IllegalArgumentException("El personal no tiene una empresa asociada");
        }
        Empresa empresa = personal.getEmpresa();
        if (empresa.getIdEmpresa() == null) {
            throw new IllegalArgumentException("El personal no tiene una empresa válida");
        }
        return empresa.getIdEmpresa();
    }

    private boolean perteneceALaEmpresaActual(AsignacionPersonal asignacion) {
        if (TenantContext.isSuperAdmin()) {
            return true;
        }
        Long empresaActual = TenantContext.getEmpresaId().orElse(null);
        if (empresaActual == null) {
            return false;
        }
        Long empresaAsignacion = extractEmpresaId(asignacion.getReserva());
        return empresaActual.equals(empresaAsignacion);
    }
}