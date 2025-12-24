package com.sistema.turistico.service;

import com.sistema.turistico.entity.EvaluacionServicio;
import com.sistema.turistico.entity.Reserva;
import com.sistema.turistico.repository.EvaluacionServicioRepository;
import com.sistema.turistico.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluacionServicioService {

    private final EvaluacionServicioRepository evaluacionRepository;
    private final ReservaRepository reservaRepository;

    /**
     * Crear nueva evaluación de servicio
     */
    @Transactional
    public EvaluacionServicio create(EvaluacionServicio evaluacion) {
        log.info("Creando evaluación para reserva: {}", evaluacion.getReserva().getIdReserva());

        // Validar que la reserva existe
        Reserva reserva = reservaRepository.findById(evaluacion.getReserva().getIdReserva())
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        // Validar que la reserva esté completada
        if (reserva.getEstado() != Reserva.EstadoReserva.Completada) {
            throw new IllegalArgumentException("Solo se pueden evaluar reservas completadas");
        }

        // Validar que no exista ya una evaluación para esta reserva
        if (evaluacionRepository.existsByReservaId(reserva.getIdReserva())) {
            throw new IllegalArgumentException("Ya existe una evaluación para esta reserva");
        }

        // Validar que el cliente de la evaluación coincida con el de la reserva
        if (!evaluacion.getCliente().getIdCliente().equals(reserva.getCliente().getIdCliente())) {
            throw new IllegalArgumentException("El cliente de la evaluación no coincide con el de la reserva");
        }

        evaluacion.setEstado(true);
        return evaluacionRepository.save(evaluacion);
    }

    /**
     * Buscar evaluación por ID
     */
    public Optional<EvaluacionServicio> findById(Long id) {
        return evaluacionRepository.findById(id);
    }

    /**
     * Buscar evaluación por reserva
     */
    public Optional<EvaluacionServicio> findByReservaId(Long reservaId) {
        return evaluacionRepository.findByReservaIdUnique(reservaId);
    }

    /**
     * Listar evaluaciones por reserva
     */
    public List<EvaluacionServicio> findByReservaIdList(Long reservaId) {
        return evaluacionRepository.findByReservaId(reservaId);
    }

    /**
     * Listar evaluaciones por cliente
     */
    public List<EvaluacionServicio> findByClienteId(Long clienteId) {
        return evaluacionRepository.findByClienteId(clienteId);
    }

    /**
     * Listar evaluaciones por servicio
     */
    public List<EvaluacionServicio> findByServicioId(Long servicioId) {
        return evaluacionRepository.findByServicioId(servicioId);
    }

    /**
     * Listar evaluaciones por paquete
     */
    public List<EvaluacionServicio> findByPaqueteId(Long paqueteId) {
        return evaluacionRepository.findByPaqueteId(paqueteId);
    }

    /**
     * Listar evaluaciones por calificación
     */
    public List<EvaluacionServicio> findByCalificacionGeneral(Integer calificacion) {
        return evaluacionRepository.findByCalificacionGeneral(calificacion);
    }

    /**
     * Listar evaluaciones por rango de calificaciones
     */
    public List<EvaluacionServicio> findByCalificacionBetween(Integer min, Integer max) {
        return evaluacionRepository.findByCalificacionGeneralBetween(min, max);
    }

    /**
     * Actualizar evaluación
     */
    @Transactional
    public EvaluacionServicio update(Long id, EvaluacionServicio evaluacionActualizada) {
        log.info("Actualizando evaluación ID: {}", id);

        EvaluacionServicio evaluacionExistente = evaluacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));

        // Actualizar campos permitidos
        if (evaluacionActualizada.getCalificacionGeneral() != null) {
            evaluacionExistente.setCalificacionGeneral(evaluacionActualizada.getCalificacionGeneral());
        }
        if (evaluacionActualizada.getCalificacionGuia() != null) {
            evaluacionExistente.setCalificacionGuia(evaluacionActualizada.getCalificacionGuia());
        }
        if (evaluacionActualizada.getCalificacionTransporte() != null) {
            evaluacionExistente.setCalificacionTransporte(evaluacionActualizada.getCalificacionTransporte());
        }
        if (evaluacionActualizada.getCalificacionHotel() != null) {
            evaluacionExistente.setCalificacionHotel(evaluacionActualizada.getCalificacionHotel());
        }
        if (evaluacionActualizada.getComentarioGeneral() != null) {
            evaluacionExistente.setComentarioGeneral(evaluacionActualizada.getComentarioGeneral());
        }
        if (evaluacionActualizada.getComentarioGuia() != null) {
            evaluacionExistente.setComentarioGuia(evaluacionActualizada.getComentarioGuia());
        }
        if (evaluacionActualizada.getComentarioTransporte() != null) {
            evaluacionExistente.setComentarioTransporte(evaluacionActualizada.getComentarioTransporte());
        }
        if (evaluacionActualizada.getComentarioHotel() != null) {
            evaluacionExistente.setComentarioHotel(evaluacionActualizada.getComentarioHotel());
        }
        if (evaluacionActualizada.getRecomendaciones() != null) {
            evaluacionExistente.setRecomendaciones(evaluacionActualizada.getRecomendaciones());
        }

        return evaluacionRepository.save(evaluacionExistente);
    }

    /**
     * Ocultar/mostrar evaluación (soft delete)
     */
    @Transactional
    public EvaluacionServicio toggleEstado(Long id) {
        log.info("Cambiando estado de evaluación ID: {}", id);

        EvaluacionServicio evaluacion = evaluacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));

        evaluacion.setEstado(!evaluacion.getEstado());
        return evaluacionRepository.save(evaluacion);
    }

    /**
     * Eliminar evaluación
     */
    @Transactional
    public void delete(Long id) {
        log.info("Eliminando evaluación ID: {}", id);

        EvaluacionServicio evaluacion = evaluacionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Evaluación no encontrada"));

        evaluacionRepository.delete(evaluacion);
    }

    /**
     * Obtener estadísticas de calificaciones por servicio
     */
    @Transactional(readOnly = true)
    public Object[] getEstadisticasByServicio(Long servicioId) {
        return evaluacionRepository.getEstadisticasByServicio(servicioId);
    }

    /**
     * Obtener estadísticas de calificaciones por paquete
     */
    @Transactional(readOnly = true)
    public Object[] getEstadisticasByPaquete(Long paqueteId) {
        return evaluacionRepository.getEstadisticasByPaquete(paqueteId);
    }

    /**
     * Verificar si una reserva puede ser evaluada
     */
    @Transactional(readOnly = true)
    public boolean puedeEvaluarReserva(Long reservaId) {
        Optional<Reserva> reservaOpt = reservaRepository.findById(reservaId);
        if (reservaOpt.isEmpty()) {
            return false;
        }

        Reserva reserva = reservaOpt.get();
        return reserva.getEstado() == Reserva.EstadoReserva.Completada &&
               !evaluacionRepository.existsByReservaId(reservaId);
    }
}