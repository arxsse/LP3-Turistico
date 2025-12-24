package com.sistema.turistico.service;

import com.sistema.turistico.dto.CajaAperturaRequest;
import com.sistema.turistico.dto.CajaCierreRequest;
import com.sistema.turistico.dto.MovimientoCajaRequest;
import com.sistema.turistico.entity.Caja;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.MovimientoCaja;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.entity.Venta;
import com.sistema.turistico.repository.CajaRepository;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.MovimientoCajaRepository;
import com.sistema.turistico.repository.SucursalRepository;
import com.sistema.turistico.repository.UsuarioRepository;
import com.sistema.turistico.repository.VentaRepository;
import com.sistema.turistico.security.TenantContext;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CajaService {

    private static final String MSG_CAJA_NO_ENCONTRADA = "Caja no encontrada";

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;

    public Caja abrirCaja(CajaAperturaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }

        log.info("Aperturando caja para sucursal {} y usuario {}", request.getSucursalId(), request.getUsuarioAperturaId());

        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(request.getEmpresaId());
        Empresa empresa = obtenerEmpresaActiva(empresaId);
        Sucursal sucursal = obtenerSucursalAutorizada(request.getSucursalId(), empresaId);
        Usuario usuarioApertura = obtenerUsuarioAutorizado(request.getUsuarioAperturaId(), empresaId);

        if (cajaRepository.existsBySucursal_IdSucursalAndEstado(sucursal.getIdSucursal(), Caja.EstadoCaja.Abierta)) {
            throw new IllegalArgumentException("Ya existe una caja abierta para la sucursal seleccionada");
        }

        Caja caja = new Caja();
        caja.setEmpresa(empresa);
        caja.setSucursal(sucursal);
        caja.setUsuarioApertura(usuarioApertura);
        caja.setFechaApertura(LocalDate.now());
        caja.setHoraApertura(LocalTime.now());
        caja.setMontoInicial(request.getMontoInicial());
        caja.setSaldoActual(request.getMontoInicial());
        caja.setObservaciones(normalizarTexto(request.getObservaciones()));
        caja.setEstado(Caja.EstadoCaja.Abierta);
        caja.setUsuarioCierre(null);
        caja.setMontoCierre(null);
        caja.setDiferencia(null);

        Caja cajaGuardada = cajaRepository.save(caja);
        log.info("Caja {} aperturada correctamente", cajaGuardada.getIdCaja());
        return cajaGuardada;
    }

    @Transactional(readOnly = true)
    public Caja obtenerCaja(Long cajaId) {
        return obtenerCajaAutorizada(cajaId);
    }

    @Transactional(readOnly = true)
    public Caja obtenerCajaActiva(Long cajaId) {
        Caja caja = obtenerCajaAutorizada(cajaId);
        if (!caja.estaAbierta()) {
            throw new IllegalStateException("La caja seleccionada no se encuentra abierta");
        }
        return caja;
    }

    @Transactional(readOnly = true)
    public List<Caja> listarCajasAbiertas(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        return cajaRepository.findByEmpresa_IdEmpresaAndEstado(empresaFiltrada, Caja.EstadoCaja.Abierta).stream()
            .filter(this::puedeVerCaja)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Caja> listarCajas(Long empresaId, Long sucursalId, Caja.EstadoCaja estado) {
        Long empresaFiltrada = TenantContext.resolveEmpresaId(empresaId);

        if (sucursalId != null) {
            if (empresaFiltrada != null) {
                obtenerSucursalAutorizada(sucursalId, empresaFiltrada);
            } else {
                obtenerSucursalAutorizada(sucursalId, null);
            }
        }

        return cajaRepository.findByFilters(empresaFiltrada, sucursalId, estado).stream()
            .filter(this::puedeVerCaja)
            .toList();
    }

    @Transactional(readOnly = true)
    public MovimientoCaja obtenerMovimiento(Long cajaId, Long movimientoId) {
        Caja caja = obtenerCajaAutorizada(cajaId);
        return movimientoCajaRepository.findByIdMovimientoAndCaja_IdCaja(movimientoId, caja.getIdCaja())
            .filter(movimiento -> puedeVerCaja(movimiento.getCaja()))
            .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado en la caja especificada"));
    }

    public Caja cerrarCaja(Long cajaId, CajaCierreRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }

        log.info("Cerrando caja {} por usuario {}", cajaId, request.getUsuarioCierreId());

        Caja caja = obtenerCajaAutorizada(cajaId);
        if (!caja.estaAbierta()) {
            throw new IllegalStateException("La caja ya se encontraba cerrada");
        }

        Usuario usuarioCierre = obtenerUsuarioAutorizado(request.getUsuarioCierreId(), caja.getEmpresa().getIdEmpresa());
        caja.setUsuarioCierre(usuarioCierre);
        caja.setMontoCierre(request.getMontoCierre());
        BigDecimal diferencia = request.getMontoCierre().subtract(caja.getSaldoActual());
        caja.setDiferencia(diferencia);

        String observacion = normalizarTexto(request.getObservaciones());
        if (observacion != null) {
            caja.setObservaciones(observacion);
        }
        caja.setEstado(Caja.EstadoCaja.Cerrada);

        Caja cajaCerrada = cajaRepository.save(caja);
        log.info("Caja {} cerrada. Diferencia registrada: {}", cajaId, diferencia);
        return cajaCerrada;
    }

    public MovimientoCaja registrarMovimiento(Long cajaId, MovimientoCajaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }

        Caja caja = obtenerCajaActiva(cajaId);

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setCaja(caja);
        movimiento.setTipoMovimiento(request.getTipoMovimiento());
        movimiento.setMonto(request.getMonto());
        movimiento.setDescripcion(normalizarTexto(request.getDescripcion()));
        movimiento.setFechaHora(LocalDateTime.now());

        if (request.getVentaId() != null) {
            Venta venta = ventaRepository.findById(request.getVentaId())
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
            validarMismaEmpresa(caja.getEmpresa().getIdEmpresa(), venta.getEmpresa() != null ? venta.getEmpresa().getIdEmpresa() : null, "venta");
            if (venta.getCaja() != null && !venta.getCaja().getIdCaja().equals(caja.getIdCaja())) {
                throw new IllegalArgumentException("La venta no pertenece a la caja seleccionada");
            }
            movimiento.setVenta(venta);
        }

        BigDecimal nuevoSaldo = calcularNuevoSaldo(caja.getSaldoActual(), request.getTipoMovimiento(), request.getMonto());
        caja.setSaldoActual(nuevoSaldo);

        MovimientoCaja movimientoGuardado = movimientoCajaRepository.save(movimiento);
        cajaRepository.save(caja);
        log.info("Movimiento {} registrado para caja {}", movimientoGuardado.getIdMovimiento(), cajaId);
        return movimientoGuardado;
    }

    @Transactional(readOnly = true)
    public List<MovimientoCaja> obtenerMovimientos(Long cajaId, LocalDateTime inicio, LocalDateTime fin, MovimientoCaja.TipoMovimiento tipo) {
        Caja caja = obtenerCajaAutorizada(cajaId);
        return movimientoCajaRepository.findByCajaAndFilters(caja.getIdCaja(), tipo, inicio, fin);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerArqueo(Long cajaId, LocalDate fechaInicio, LocalDate fechaFin) {
        Caja caja = obtenerCajaAutorizada(cajaId);

        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(LocalTime.MAX) : null;

        BigDecimal ingresos = movimientoCajaRepository.sumByCajaAndTipoAndRango(caja.getIdCaja(), MovimientoCaja.TipoMovimiento.Ingreso, inicio, fin);
        BigDecimal egresos = movimientoCajaRepository.sumByCajaAndTipoAndRango(caja.getIdCaja(), MovimientoCaja.TipoMovimiento.Egreso, inicio, fin);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("cajaId", caja.getIdCaja());
        resumen.put("fechaApertura", caja.getFechaApertura());
        resumen.put("saldoInicial", caja.getMontoInicial());
        resumen.put("saldoActual", caja.getSaldoActual());
        resumen.put("ingresos", ingresos);
        resumen.put("egresos", egresos);
        resumen.put("saldoCalculado", caja.getMontoInicial().add(ingresos).subtract(egresos));

        return resumen;
    }

    public MovimientoCaja anularMovimiento(Long cajaId, Long movimientoId, String motivo) {
        log.info("Anulando movimiento {} de caja {}", movimientoId, cajaId);

        Caja caja = obtenerCajaActiva(cajaId);
        MovimientoCaja movimiento = movimientoCajaRepository.findById(movimientoId)
            .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado"));

        if (!movimiento.getCaja().getIdCaja().equals(caja.getIdCaja())) {
            throw new IllegalArgumentException("El movimiento no pertenece a la caja especificada");
        }

        if (movimiento.getVenta() != null) {
            throw new IllegalArgumentException("No se pueden anular movimientos automáticos generados por ventas");
        }

        MovimientoCaja movimientoReverso = new MovimientoCaja();
        movimientoReverso.setCaja(caja);
        movimientoReverso.setTipoMovimiento(
            MovimientoCaja.TipoMovimiento.Ingreso.equals(movimiento.getTipoMovimiento())
                ? MovimientoCaja.TipoMovimiento.Egreso
                : MovimientoCaja.TipoMovimiento.Ingreso
        );
        movimientoReverso.setMonto(movimiento.getMonto());
        String descripcion = movimiento.getDescripcion() != null ? movimiento.getDescripcion() : "";
        movimientoReverso.setDescripcion("ANULACIÓN: " + descripcion + (motivo != null && !motivo.isBlank() ? " - Motivo: " + motivo.trim() : ""));
        movimientoReverso.setFechaHora(LocalDateTime.now());

        BigDecimal nuevoSaldo = calcularNuevoSaldo(caja.getSaldoActual(), movimientoReverso.getTipoMovimiento(), movimientoReverso.getMonto());
        caja.setSaldoActual(nuevoSaldo);

        MovimientoCaja movimientoGuardado = movimientoCajaRepository.save(movimientoReverso);
        cajaRepository.save(caja);

        log.info("Movimiento {} anulado exitosamente con reverso {}", movimientoId, movimientoGuardado.getIdMovimiento());
        return movimientoGuardado;
    }

    private Caja obtenerCajaAutorizada(Long cajaId) {
        Caja caja = cajaRepository.findById(cajaId)
            .orElseThrow(() -> new IllegalArgumentException(MSG_CAJA_NO_ENCONTRADA));
        validarPertenencia(caja);
        return caja;
    }

    private Usuario obtenerUsuarioAutorizado(Long usuarioId, Long empresaId) {
        Long resolvedId = usuarioId != null ? usuarioId : TenantContext.requireUserId();
        Usuario usuario = usuarioRepository.findActivoById(resolvedId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Long empresaUsuario = usuario.getEmpresa() != null ? usuario.getEmpresa().getIdEmpresa() : null;
        validarMismaEmpresa(empresaId, empresaUsuario, "usuario");

        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            if (!empresaActual.equals(empresaUsuario)) {
                throw new IllegalArgumentException("El usuario no pertenece a la empresa actual");
            }
        }

        return usuario;
    }

    private Sucursal obtenerSucursalAutorizada(Long sucursalId, Long empresaId) {
        if (sucursalId == null) {
            throw new IllegalArgumentException("La sucursal es obligatoria");
        }
        Sucursal sucursal = sucursalRepository.findActivaById(sucursalId)
            .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));

        Long empresaSucursal = sucursal.getEmpresa() != null ? sucursal.getEmpresa().getIdEmpresa() : null;
        validarMismaEmpresa(empresaId, empresaSucursal, "sucursal");

        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            if (!empresaActual.equals(empresaSucursal)) {
                throw new IllegalArgumentException("La sucursal no pertenece a la empresa actual");
            }
        }

        return sucursal;
    }

    private Empresa obtenerEmpresaActiva(Long empresaId) {
        return empresaRepository.findActivaById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    private void validarPertenencia(Caja caja) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaCaja = caja.getEmpresa() != null ? caja.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaCaja)) {
                throw new IllegalArgumentException("La caja no pertenece a la empresa actual");
            }
        }
    }

    private boolean puedeVerCaja(Caja caja) {
        if (esSuperAdmin()) {
            return true;
        }
        Long empresaActual = TenantContext.requireEmpresaId();
        Long empresaCaja = caja.getEmpresa() != null ? caja.getEmpresa().getIdEmpresa() : null;
        return empresaActual.equals(empresaCaja);
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }

    private void validarMismaEmpresa(Long empresaEsperada, Long empresaRelacionada, String recurso) {
        if (empresaEsperada == null) {
            return;
        }
        if (empresaRelacionada == null || !empresaEsperada.equals(empresaRelacionada)) {
            throw new IllegalArgumentException("El " + recurso + " no pertenece a la empresa actual");
        }
    }

    private BigDecimal calcularNuevoSaldo(BigDecimal saldoActual, MovimientoCaja.TipoMovimiento tipo, BigDecimal monto) {
        BigDecimal resultado = MovimientoCaja.TipoMovimiento.Ingreso.equals(tipo)
            ? saldoActual.add(monto)
            : saldoActual.subtract(monto);

        if (resultado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El saldo de caja no puede ser negativo");
        }
        return resultado;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
