package com.sistema.turistico.service;

import com.sistema.turistico.dto.MovimientoCajaRequest;
import com.sistema.turistico.dto.PagoReservaRequest;
import com.sistema.turistico.entity.Caja;
import com.sistema.turistico.entity.MovimientoCaja;
import com.sistema.turistico.entity.PagoReserva;
import com.sistema.turistico.entity.Reserva;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.repository.PagoReservaRepository;
import com.sistema.turistico.repository.UsuarioRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PagoReservaService {

    private static final Set<String> METODOS_VALIDOS = Set.of(
        "Efectivo",
        "Tarjeta Crédito",
        "Tarjeta Débito",
        "Transferencia",
        "Yape/Plin",
        "Otros"
    );

    private final PagoReservaRepository pagoReservaRepository;
    private final ReservaService reservaService;
    private final CajaService cajaService;
    private final UsuarioRepository usuarioRepository;

    public PagoReserva registrarPago(Long reservaId, PagoReservaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }

        log.info("Registrando pago para reserva {} por usuario {}", reservaId, request.getUsuarioId());

        if (request.getCajaId() == null) {
            throw new IllegalArgumentException("La caja es obligatoria");
        }
        if (request.getMontoPagado() == null) {
            throw new IllegalArgumentException("El monto pagado es obligatorio");
        }

        validarMetodoPago(request.getMetodoPago());

        Reserva reserva = obtenerReservaAutorizada(reservaId);
        Long empresaId = reserva.getEmpresa() != null ? reserva.getEmpresa().getIdEmpresa() : null;

        Usuario usuario = obtenerUsuarioAutorizado(request.getUsuarioId(), empresaId);

        Caja caja = cajaService.obtenerCajaActiva(request.getCajaId());
        validarMismaEmpresa(empresaId, caja.getEmpresa() != null ? caja.getEmpresa().getIdEmpresa() : null, "caja");

        BigDecimal totalReserva = reserva.getPrecioFinal();
        BigDecimal acumuladoAnterior = pagoReservaRepository.sumMontosActivosPorReserva(reservaId);
        BigDecimal saldoPendiente = totalReserva.subtract(acumuladoAnterior);

        if (request.getMontoPagado().compareTo(saldoPendiente) > 0) {
            throw new IllegalArgumentException("El monto excede el saldo pendiente de la reserva");
        }

        PagoReserva pago = new PagoReserva();
        pago.setReserva(reserva);
        pago.setSucursal(reserva.getSucursal());
        pago.setUsuario(usuario);
        pago.setMontoPagado(request.getMontoPagado());
        pago.setMetodoPago(normalizarMetodoPago(request.getMetodoPago()));
        pago.setNumeroOperacion(normalizarTexto(request.getNumeroOperacion()));
        pago.setComprobante(normalizarTexto(request.getComprobante()));
        pago.setFechaPago(request.getFechaPago() != null ? request.getFechaPago() : LocalDate.now());
        pago.setObservaciones(normalizarTexto(request.getObservaciones()));
        pago.setEstado(Boolean.TRUE);

        PagoReserva pagoGuardado = pagoReservaRepository.save(pago);

        MovimientoCajaRequest movimientoRequest = new MovimientoCajaRequest();
        movimientoRequest.setTipoMovimiento(MovimientoCaja.TipoMovimiento.Ingreso);
        movimientoRequest.setMonto(pagoGuardado.getMontoPagado());
        movimientoRequest.setDescripcion("Ingreso por pago de reserva " + reserva.getCodigoReserva());
        cajaService.registrarMovimiento(caja.getIdCaja(), movimientoRequest);

        BigDecimal totalPagado = acumuladoAnterior.add(pagoGuardado.getMontoPagado());
        Reserva.EstadoReserva nuevoEstado = determinarEstadoFinanciero(totalReserva, totalPagado);
        reservaService.actualizarEstadoFinanciero(reserva, nuevoEstado);

        return pagoGuardado;
    }

    @Transactional(readOnly = true)
    public List<PagoReserva> listarPagosReserva(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        return pagoReservaRepository.findByReserva_IdReservaOrderByFechaPagoAsc(reserva.getIdReserva());
    }

    @Transactional(readOnly = true)
    public List<PagoReserva> listarPagosActivosReserva(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        return pagoReservaRepository.findByReserva_IdReservaAndEstadoTrueOrderByFechaPagoAsc(reserva.getIdReserva());
    }

    @Transactional(readOnly = true)
    public List<PagoReserva> listarPagosPorFiltros(Long empresaId, Long sucursalId, String metodoPago, Boolean estado,
                                                   LocalDate fechaDesde, LocalDate fechaHasta) {
        String metodoNormalizado = null;
        if (metodoPago != null) {
            validarMetodoPago(metodoPago);
            metodoNormalizado = normalizarMetodoPago(metodoPago);
        }
        if (fechaDesde != null && fechaHasta != null && fechaHasta.isBefore(fechaDesde)) {
            throw new IllegalArgumentException("El rango de fechas es inválido");
        }
        Long empresaFiltrada = TenantContext.resolveEmpresaId(empresaId);
        return pagoReservaRepository.findByFiltros(empresaFiltrada, sucursalId, metodoNormalizado, estado, fechaDesde, fechaHasta).stream()
            .filter(this::puedeVerPago)
            .toList();
    }

    public PagoReserva anularPago(Long reservaId, Long pagoId, Long cajaId, String motivo) {
        log.info("Anulando pago {} de reserva {}", pagoId, reservaId);

        if (cajaId == null) {
            throw new IllegalArgumentException("La caja es obligatoria");
        }

        Reserva reserva = obtenerReservaAutorizada(reservaId);
        Caja caja = cajaService.obtenerCajaActiva(cajaId);
        validarMismaEmpresa(reserva.getEmpresa() != null ? reserva.getEmpresa().getIdEmpresa() : null,
            caja.getEmpresa() != null ? caja.getEmpresa().getIdEmpresa() : null, "caja");

        PagoReserva pago = pagoReservaRepository.findByIdPagoAndReserva_IdReservaAndEstadoTrue(pagoId, reserva.getIdReserva())
            .orElseThrow(() -> new IllegalArgumentException("El pago indicado no está activo"));

        validarPertenencia(pago);

        pago.setEstado(Boolean.FALSE);
        String motivoNormalizado = normalizarTexto(motivo);
        if (motivoNormalizado != null) {
            String observaciones = pago.getObservaciones();
            String anotacion = "Anulado: " + motivoNormalizado + " (" + LocalDateTime.now() + ")";
            pago.setObservaciones(observaciones == null ? anotacion : observaciones + " | " + anotacion);
        }
        PagoReserva pagoAnulado = pagoReservaRepository.save(pago);

        MovimientoCajaRequest movimientoRequest = new MovimientoCajaRequest();
        movimientoRequest.setTipoMovimiento(MovimientoCaja.TipoMovimiento.Egreso);
        movimientoRequest.setMonto(pagoAnulado.getMontoPagado());
        movimientoRequest.setDescripcion("Reverso pago reserva " + pago.getReserva().getCodigoReserva());
        cajaService.registrarMovimiento(caja.getIdCaja(), movimientoRequest);

        BigDecimal totalPagado = pagoReservaRepository.sumMontosActivosPorReserva(reserva.getIdReserva());
        BigDecimal totalReserva = pago.getReserva().getPrecioFinal();
        Reserva.EstadoReserva nuevoEstado = determinarEstadoFinanciero(totalReserva, totalPagado);

        reservaService.actualizarEstadoFinanciero(pago.getReserva(), nuevoEstado);
        return pagoAnulado;
    }

    @Transactional(readOnly = true)
    public PagoReserva obtenerPago(Long pagoId) {
        return obtenerPagoAutorizado(pagoId);
    }

    public PagoReserva actualizarPago(Long pagoId, PagoReservaRequest request) {
        log.info("Actualizando pago {}", pagoId);

        PagoReserva pago = obtenerPagoAutorizado(pagoId);

        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }

        // Validar método de pago si se proporciona
        if (request.getMetodoPago() != null) {
            validarMetodoPago(request.getMetodoPago());
            pago.setMetodoPago(normalizarMetodoPago(request.getMetodoPago()));
        }

        // Actualizar campos permitidos
        if (request.getNumeroOperacion() != null) {
            pago.setNumeroOperacion(normalizarTexto(request.getNumeroOperacion()));
        }
        if (request.getComprobante() != null) {
            pago.setComprobante(normalizarTexto(request.getComprobante()));
        }
        if (request.getFechaPago() != null) {
            pago.setFechaPago(request.getFechaPago());
        }
        if (request.getObservaciones() != null) {
            pago.setObservaciones(normalizarTexto(request.getObservaciones()));
        }

        // Nota: montoPagado no se permite actualizar por razones de integridad financiera
        // Si se necesita corregir el monto, se debe anular el pago y crear uno nuevo

        return pagoReservaRepository.save(pago);
    }

    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoPendiente(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        BigDecimal pagado = pagoReservaRepository.sumMontosActivosPorReserva(reservaId);
        return reserva.getPrecioFinal().subtract(pagado);
    }

    private Reserva obtenerReservaAutorizada(Long reservaId) {
        return reservaService.findById(reservaId)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
    }

    private PagoReserva obtenerPagoAutorizado(Long pagoId) {
        PagoReserva pago = pagoReservaRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));
        validarPertenencia(pago);
        return pago;
    }

    private Usuario obtenerUsuarioAutorizado(Long usuarioId, Long empresaEsperada) {
        Long resolvedId = usuarioId != null ? usuarioId : TenantContext.requireUserId();
        Usuario usuario = usuarioRepository.findActivoById(resolvedId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Long empresaUsuario = usuario.getEmpresa() != null ? usuario.getEmpresa().getIdEmpresa() : null;
        validarMismaEmpresa(empresaEsperada, empresaUsuario, "usuario");

        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            if (!empresaActual.equals(empresaUsuario)) {
                throw new IllegalArgumentException("El usuario no pertenece a la empresa actual");
            }
        }

        return usuario;
    }

    private void validarPertenencia(PagoReserva pago) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaPago = pago.getReserva() != null && pago.getReserva().getEmpresa() != null
                ? pago.getReserva().getEmpresa().getIdEmpresa()
                : null;
            if (!empresaActual.equals(empresaPago)) {
                throw new IllegalArgumentException("El pago no pertenece a la empresa actual");
            }
        }
    }

    private boolean puedeVerPago(PagoReserva pago) {
        if (esSuperAdmin()) {
            return true;
        }
        Long empresaActual = TenantContext.requireEmpresaId();
        Long empresaPago = pago.getReserva() != null && pago.getReserva().getEmpresa() != null
            ? pago.getReserva().getEmpresa().getIdEmpresa()
            : null;
        return empresaActual.equals(empresaPago);
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }

    private void validarMismaEmpresa(Long empresaEsperada, Long empresaRelacionada, String recurso) {
        if (empresaEsperada == null) {
            return;
        }
        if (empresaRelacionada == null || !empresaEsperada.equals(empresaRelacionada)) {
            throw new IllegalArgumentException("El recurso " + recurso + " no pertenece a la empresa actual");
        }
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Reserva.EstadoReserva determinarEstadoFinanciero(BigDecimal totalReserva, BigDecimal totalPagado) {
        if (totalPagado.compareTo(BigDecimal.ZERO) <= 0) {
            return Reserva.EstadoReserva.Confirmada;
        }
        if (totalPagado.compareTo(totalReserva) >= 0) {
            return Reserva.EstadoReserva.Pagada;
        }
        return Reserva.EstadoReserva.PagoParcial;
    }

    private void validarMetodoPago(String metodo) {
        if (metodo == null || metodo.trim().isEmpty()) {
            throw new IllegalArgumentException("El método de pago es obligatorio");
        }
        String metodoNormalizado = normalizarMetodoPago(metodo);
        if (!METODOS_VALIDOS.contains(metodoNormalizado)) {
            throw new IllegalArgumentException("Método de pago no soportado: " + metodo + ". Métodos válidos: " + METODOS_VALIDOS);
        }
    }

    private String normalizarMetodoPago(String metodoPago) {
        if (metodoPago == null) return null;
        String trimmed = metodoPago.trim();
        // Convertir primera letra a mayúscula y resto a minúscula para coincidir con METODOS_VALIDOS
        if (trimmed.length() > 1) {
            return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
        }
        return trimmed.toUpperCase();
    }
}
