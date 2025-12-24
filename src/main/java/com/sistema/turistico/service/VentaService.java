package com.sistema.turistico.service;

import com.sistema.turistico.dto.MovimientoCajaRequest;
import com.sistema.turistico.dto.VentaAnulacionRequest;
import com.sistema.turistico.dto.VentaRequest;
import com.sistema.turistico.dto.VentaResponse;
import com.sistema.turistico.entity.Caja;
import com.sistema.turistico.entity.Cliente;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.MovimientoCaja;
import com.sistema.turistico.entity.Reserva;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.entity.Venta;
import com.sistema.turistico.entity.Voucher;
import com.sistema.turistico.repository.PagoReservaRepository;
import com.sistema.turistico.repository.UsuarioRepository;
import com.sistema.turistico.repository.VentaRepository;
import com.sistema.turistico.repository.VoucherRepository;
import com.sistema.turistico.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VentaService {

    private static final Set<String> METODOS_VALIDOS = Set.of(
        "Efectivo",
        "Tarjeta Crédito",
        "Tarjeta Débito",
        "Transferencia",
        "Yape/Plin",
        "Otros"
    );

    private final VentaRepository ventaRepository;
    private final VoucherRepository voucherRepository;
    private final PagoReservaRepository pagoReservaRepository;
    private final ReservaService reservaService;
    private final CajaService cajaService;
    private final ClienteService clienteService;
    private final UsuarioRepository usuarioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public VentaResponse emitirVenta(VentaRequest request) {
        log.info("Emitiendo venta para reserva {}", request.getReservaId());

        validarMetodoPago(request.getMetodoPago());

        Reserva reserva = reservaService.findById(request.getReservaId())
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        Long empresaId = reserva.getEmpresa() != null ? reserva.getEmpresa().getIdEmpresa() : null;
        if (empresaId == null) {
            throw new IllegalStateException("La reserva no posee una empresa asociada");
        }

        Long usuarioId = esSuperAdmin() && request.getUsuarioId() != null
            ? request.getUsuarioId()
            : TenantContext.requireUserId();
        Usuario usuario = obtenerUsuarioParaOperacion(usuarioId, empresaId);

        Caja caja = cajaService.obtenerCajaActiva(request.getCajaId());
        validarMismaEmpresa(empresaId, caja.getEmpresa() != null ? caja.getEmpresa().getIdEmpresa() : null, "caja");

        BigDecimal totalReserva = reserva.getPrecioFinal();
        BigDecimal totalPagado = pagoReservaRepository.sumMontosActivosPorReserva(reserva.getIdReserva());
        if (totalPagado.compareTo(totalReserva) < 0) {
            throw new IllegalStateException("La reserva aún presenta saldo pendiente");
        }

        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        BigDecimal propina = request.getPropina() != null ? request.getPropina() : BigDecimal.ZERO;

        BigDecimal montoFinal = totalReserva.subtract(descuento).add(propina);
        if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto final de la venta debe ser mayor a 0");
        }

        Venta venta = new Venta();
    venta.setEmpresa(reserva.getEmpresa());
    venta.setCliente(obtenerClienteVenta(request, reserva));
        venta.setReserva(reserva);
    venta.setUsuario(usuario);
        venta.setCaja(caja);
        venta.setSucursal(caja.getSucursal());
        venta.setFechaHora(LocalDateTime.now());
        venta.setMontoTotal(montoFinal);
        venta.setMetodoPago(normalizarMetodoPago(request.getMetodoPago()));
        venta.setNumeroOperacion(request.getNumeroOperacion());
        venta.setComprobante(request.getComprobante());
        venta.setDescuento(descuento);
        venta.setPropina(propina);
        venta.setObservaciones(request.getObservaciones());
        venta.setEstado(Boolean.TRUE);

        Venta ventaGuardada = ventaRepository.save(venta);
        generarOVincularVoucher(reserva);

        reservaService.actualizarEstadoFinanciero(reserva, Reserva.EstadoReserva.Pagada);
        log.info("Venta {} emitida correctamente", ventaGuardada.getIdVenta());
        return toResponse(ventaGuardada);
    }

    @Transactional(readOnly = true)
    public VentaResponse obtenerVenta(Long ventaId) {
        Venta venta = obtenerVentaInterna(ventaId);
        return toResponse(venta);
    }

    @Transactional(readOnly = true)
    private Venta obtenerVentaInterna(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId)
            .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
        validarPertenencia(venta);
        return venta;
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarVentas(Long empresaId, Long idSucursal, LocalDateTime inicio, LocalDateTime fin, String metodoPago, Boolean estado) {
        String metodoNormalizado = metodoPago != null ? normalizarMetodoPago(metodoPago) : null;
        if (metodoNormalizado != null) {
            validarMetodoPago(metodoNormalizado);
        }
        Long empresaFiltrada = TenantContext.resolveEmpresaId(empresaId);
        List<Venta> ventas = ventaRepository.findByFiltros(empresaFiltrada, idSucursal, inicio, fin, metodoNormalizado, estado);
        if (!esSuperAdmin() && empresaFiltrada == null) {
            Long empresaActual = TenantContext.requireEmpresaId();
            ventas = ventas.stream()
                .filter(venta -> venta.getEmpresa() != null && empresaActual.equals(venta.getEmpresa().getIdEmpresa()))
                .toList();
        }
        return ventas.stream()
            .map(this::toResponse)
            .toList();
    }

    public VentaResponse anularVenta(Long ventaId, VentaAnulacionRequest request) {
        log.info("Anulando venta {}", ventaId);

        Venta venta = obtenerVentaInterna(ventaId);
        if (Boolean.FALSE.equals(venta.getEstado())) {
            throw new IllegalStateException("La venta ya se encuentra anulada");
        }

        Caja cajaActiva = cajaService.obtenerCajaActiva(request.getCajaId());
        if (!venta.getCaja().getIdCaja().equals(cajaActiva.getIdCaja())) {
            throw new IllegalArgumentException("La anulación debe realizarse en la misma caja donde se registró la venta");
        }

        venta.setEstado(Boolean.FALSE);
        String motivo = request.getMotivo();
        if (motivo != null && !motivo.isBlank()) {
            String observaciones = venta.getObservaciones();
            String nuevaObs = "Anulada: " + motivo + " (" + LocalDateTime.now() + ")";
            venta.setObservaciones(observaciones == null ? nuevaObs : observaciones + " | " + nuevaObs);
        }
        Venta ventaAnulada = ventaRepository.save(venta);

        MovimientoCajaRequest movimientoRequest = new MovimientoCajaRequest();
        movimientoRequest.setTipoMovimiento(MovimientoCaja.TipoMovimiento.Egreso);
        movimientoRequest.setMonto(ventaAnulada.getMontoTotal());
        movimientoRequest.setDescripcion("Reverso venta reserva " + (venta.getReserva() != null ? venta.getReserva().getCodigoReserva() : venta.getIdVenta()));
        movimientoRequest.setVentaId(ventaAnulada.getIdVenta());
        cajaService.registrarMovimiento(cajaActiva.getIdCaja(), movimientoRequest);

        if (venta.getReserva() != null) {
            BigDecimal totalPagado = pagoReservaRepository.sumMontosActivosPorReserva(venta.getReserva().getIdReserva());
            Reserva.EstadoReserva estado = totalPagado.compareTo(venta.getReserva().getPrecioFinal()) >= 0
                ? Reserva.EstadoReserva.Pagada
                : (totalPagado.compareTo(BigDecimal.ZERO) > 0 ? Reserva.EstadoReserva.PagoParcial : Reserva.EstadoReserva.Confirmada);
            reservaService.actualizarEstadoFinanciero(venta.getReserva(), estado);
        }

        voucherRepository.findByReserva_IdReserva(venta.getReserva() != null ? venta.getReserva().getIdReserva() : null)
            .ifPresent(voucher -> {
                voucher.setEstado(Voucher.EstadoVoucher.Cancelado);
                voucherRepository.save(voucher);
            });

        return toResponse(ventaAnulada);
    }

    public VentaResponse actualizarVenta(Long ventaId, VentaRequest request) {
        log.info("Actualizando venta {}", ventaId);

        Venta venta = obtenerVentaInterna(ventaId);
        if (Boolean.FALSE.equals(venta.getEstado())) {
            throw new IllegalStateException("No se puede actualizar una venta anulada");
        }

        // Validar método de pago si se proporciona
        if (request.getMetodoPago() != null) {
            validarMetodoPago(request.getMetodoPago());
            venta.setMetodoPago(normalizarMetodoPago(request.getMetodoPago()));
        }

        // Actualizar campos permitidos
        if (request.getNumeroOperacion() != null) {
            venta.setNumeroOperacion(request.getNumeroOperacion());
        }
        if (request.getComprobante() != null) {
            venta.setComprobante(request.getComprobante());
        }
        if (request.getObservaciones() != null) {
            venta.setObservaciones(request.getObservaciones());
        }
        if (request.getDescuento() != null) {
            venta.setDescuento(request.getDescuento());
        }
        if (request.getPropina() != null) {
            venta.setPropina(request.getPropina());
        }
        if (request.getClienteId() != null) {
            venta.setCliente(obtenerClientePorId(request.getClienteId(), venta.getEmpresa()));
        }

        // Recalcular monto total si cambiaron descuento o propina
        if (request.getDescuento() != null || request.getPropina() != null) {
            BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : venta.getDescuento();
            BigDecimal propina = request.getPropina() != null ? request.getPropina() : venta.getPropina();
            BigDecimal precioBase = venta.getReserva() != null ? venta.getReserva().getPrecioFinal() : venta.getMontoTotal();
            BigDecimal montoFinal = precioBase.subtract(descuento).add(propina);
            if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El monto final de la venta debe ser mayor a 0");
            }
            venta.setMontoTotal(montoFinal);
        }

        return toResponse(ventaRepository.save(venta));
    }

    public VentaResponse eliminarVenta(Long ventaId, Long cajaId, String motivo) {
        log.info("Eliminando venta {}", ventaId);

        Venta venta = obtenerVentaInterna(ventaId);
        if (Boolean.FALSE.equals(venta.getEstado())) {
            throw new IllegalStateException("La venta ya se encuentra eliminada");
        }

        // Solo permitir eliminar ventas del día actual
        LocalDate hoy = LocalDate.now();
        if (!venta.getFechaHora().toLocalDate().equals(hoy)) {
            throw new IllegalArgumentException("Solo se pueden eliminar ventas del día actual");
        }

        Caja cajaActiva = cajaService.obtenerCajaActiva(cajaId);
        if (!venta.getCaja().getIdCaja().equals(cajaActiva.getIdCaja())) {
            throw new IllegalArgumentException("La eliminación debe realizarse en la misma caja donde se registró la venta");
        }

        venta.setEstado(Boolean.FALSE);
        if (motivo != null && !motivo.isBlank()) {
            String observaciones = venta.getObservaciones();
            String nuevaObs = "Eliminada: " + motivo + " (" + LocalDateTime.now() + ")";
            venta.setObservaciones(observaciones == null ? nuevaObs : observaciones + " | " + nuevaObs);
        }
        Venta ventaEliminada = ventaRepository.save(venta);

        MovimientoCajaRequest movimientoRequest = new MovimientoCajaRequest();
        movimientoRequest.setTipoMovimiento(MovimientoCaja.TipoMovimiento.Egreso);
        movimientoRequest.setMonto(ventaEliminada.getMontoTotal());
        movimientoRequest.setDescripcion("Reverso eliminación venta reserva " + (venta.getReserva() != null ? venta.getReserva().getCodigoReserva() : venta.getIdVenta()));
        movimientoRequest.setVentaId(ventaEliminada.getIdVenta());
        cajaService.registrarMovimiento(cajaActiva.getIdCaja(), movimientoRequest);

        if (venta.getReserva() != null) {
            BigDecimal totalPagado = pagoReservaRepository.sumMontosActivosPorReserva(venta.getReserva().getIdReserva());
            Reserva.EstadoReserva estado = totalPagado.compareTo(venta.getReserva().getPrecioFinal()) >= 0
                ? Reserva.EstadoReserva.Pagada
                : (totalPagado.compareTo(BigDecimal.ZERO) > 0 ? Reserva.EstadoReserva.PagoParcial : Reserva.EstadoReserva.Confirmada);
            reservaService.actualizarEstadoFinanciero(venta.getReserva(), estado);
        }

        voucherRepository.findByReserva_IdReserva(venta.getReserva() != null ? venta.getReserva().getIdReserva() : null)
            .ifPresent(voucher -> {
                voucher.setEstado(Voucher.EstadoVoucher.Cancelado);
                voucherRepository.save(voucher);
            });

        return toResponse(ventaEliminada);
    }

    @Transactional(readOnly = true)
    public Map<String, String> obtenerProximaNumeracion(Long empresaId) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicio = hoy.atStartOfDay();
        LocalDateTime fin = hoy.atTime(LocalTime.MAX);
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        long cantidadHoy = ventaRepository.findByFiltros(empresaFiltrada, null, inicio, fin, null, Boolean.TRUE).size();
        String numero = String.format("%s-%04d", hoy.toString().replace("-", ""), cantidadHoy + 1);
        Map<String, String> response = new HashMap<>();
        response.put("siguienteNumero", numero);
        return response;
    }

    private Cliente obtenerClienteVenta(VentaRequest request, Reserva reserva) {
        if (request.getClienteId() != null) {
            return obtenerClientePorId(request.getClienteId(), reserva.getEmpresa());
        }
        return reserva.getCliente();
    }

    private void generarOVincularVoucher(Reserva reserva) {
        Voucher voucher = voucherRepository.findByReserva_IdReserva(reserva.getIdReserva())
            .orElseGet(Voucher::new);

        voucher.setReserva(reserva);
        voucher.setSucursal(reserva.getSucursal());
        voucher.setFechaEmision(LocalDate.now());
        voucher.setFechaExpiracion(reserva.getFechaServicio().toLocalDate());
        voucher.setEstado(Voucher.EstadoVoucher.Emitido);
        if (voucher.getCodigoQr() == null || voucher.getCodigoQr().isBlank()) {
            voucher.setCodigoQr(UUID.randomUUID().toString());
        }
        voucherRepository.save(voucher);
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

    public VentaResponse toResponse(Venta venta) {
        VentaResponse response = new VentaResponse();
        response.setIdVenta(venta.getIdVenta());
        response.setEmpresaId(venta.getEmpresa() != null ? venta.getEmpresa().getIdEmpresa() : null);
        response.setClienteId(venta.getCliente() != null ? venta.getCliente().getIdCliente() : null);
        response.setReservaId(venta.getReserva() != null ? venta.getReserva().getIdReserva() : null);
        response.setUsuarioId(venta.getUsuario() != null ? venta.getUsuario().getIdUsuario() : null);
        response.setCajaId(venta.getCaja() != null ? venta.getCaja().getIdCaja() : null);
        response.setSucursalId(venta.getSucursal() != null ? venta.getSucursal().getIdSucursal() : null);
        response.setSucursalNombre(venta.getSucursal() != null ? venta.getSucursal().getNombreSucursal() : null);
        response.setFechaHora(venta.getFechaHora());
        response.setMontoTotal(venta.getMontoTotal());
        response.setMetodoPago(venta.getMetodoPago());
        response.setNumeroOperacion(venta.getNumeroOperacion());
        response.setComprobante(venta.getComprobante());
        response.setDescuento(venta.getDescuento());
        response.setPropina(venta.getPropina());
        response.setObservaciones(venta.getObservaciones());
        response.setEstado(venta.getEstado());
        response.setCreatedAt(venta.getCreatedAt());
        response.setUpdatedAt(venta.getUpdatedAt());
        return response;
    }

    private Cliente obtenerClientePorId(Long clienteId, Empresa empresa) {
        Cliente cliente = clienteService.findById(clienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        Long empresaEsperada = empresa != null ? empresa.getIdEmpresa() : TenantContext.getEmpresaId().orElse(null);
        validarMismaEmpresa(empresaEsperada, cliente.getEmpresa() != null ? cliente.getEmpresa().getIdEmpresa() : null, "cliente");
        return cliente;
    }

    private Usuario obtenerUsuarioParaOperacion(Long usuarioId, Long empresaId) {
        Usuario usuario = usuarioRepository.findActivoById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (!esSuperAdmin()) {
            validarMismaEmpresa(empresaId, usuario.getEmpresa() != null ? usuario.getEmpresa().getIdEmpresa() : null, "usuario");
        } else if (usuario.getEmpresa() != null && empresaId != null && !empresaId.equals(usuario.getEmpresa().getIdEmpresa())) {
            throw new IllegalArgumentException("El usuario no pertenece a la empresa seleccionada");
        }
        return usuario;
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }

    private void validarPertenencia(Venta venta) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaVenta = venta.getEmpresa() != null ? venta.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaVenta)) {
                throw new IllegalArgumentException("La venta no pertenece a la empresa actual");
            }
        }
    }

    private void validarMismaEmpresa(Long empresaEsperada, Long empresaRelacionada, String recurso) {
        if (empresaEsperada == null || empresaRelacionada == null || !empresaEsperada.equals(empresaRelacionada)) {
            throw new IllegalArgumentException("El " + recurso + " no pertenece a la empresa actual");
        }
    }
}
