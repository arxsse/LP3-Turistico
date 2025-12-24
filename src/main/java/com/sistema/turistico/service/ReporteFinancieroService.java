package com.sistema.turistico.service;

import com.sistema.turistico.entity.Caja;
import com.sistema.turistico.entity.MovimientoCaja;
import com.sistema.turistico.entity.Venta;
import com.sistema.turistico.repository.CajaRepository;
import com.sistema.turistico.repository.MovimientoCajaRepository;
import com.sistema.turistico.repository.VentaRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReporteFinancieroService {

    private static final BigDecimal IMPUESTO_POR_DEFECTO = new BigDecimal("18.00");

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final VentaRepository ventaRepository;

    public Map<String, Object> resumenCajaDiario(Long empresaId, LocalDate fecha) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        LocalDate fechaReporte = fecha != null ? fecha : LocalDate.now();
        List<Caja> cajas = cajaRepository.findByEmpresaAndFecha(empresaFiltrada, fechaReporte).stream()
            .filter(this::puedeVerCaja)
            .toList();

        BigDecimal totalInicial = BigDecimal.ZERO;
        BigDecimal totalIngresos = BigDecimal.ZERO;
        BigDecimal totalEgresos = BigDecimal.ZERO;
        BigDecimal totalSaldo = BigDecimal.ZERO;

        for (Caja caja : cajas) {
            totalInicial = totalInicial.add(caja.getMontoInicial());
            totalSaldo = totalSaldo.add(caja.getSaldoActual());
            totalIngresos = totalIngresos.add(movimientoCajaRepository.sumByCajaAndTipoAndRango(
                caja.getIdCaja(),
                MovimientoCaja.TipoMovimiento.Ingreso,
                fechaReporte.atStartOfDay(),
                fechaReporte.atTime(LocalTime.MAX)
            ));
            totalEgresos = totalEgresos.add(movimientoCajaRepository.sumByCajaAndTipoAndRango(
                caja.getIdCaja(),
                MovimientoCaja.TipoMovimiento.Egreso,
                fechaReporte.atStartOfDay(),
                fechaReporte.atTime(LocalTime.MAX)
            ));
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("fecha", fechaReporte);
        resumen.put("totalCajas", cajas.size());
        resumen.put("montoInicial", totalInicial);
        resumen.put("ingresos", totalIngresos);
        resumen.put("egresos", totalEgresos);
        resumen.put("saldoActual", totalSaldo);
        resumen.put("saldoCalculado", totalInicial.add(totalIngresos).subtract(totalEgresos));
        return resumen;
    }

    public Map<String, Object> resumenVentasImpuestos(Long empresaId, LocalDate inicio, LocalDate fin, BigDecimal porcentajeImpuesto) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        LocalDate fechaInicio = inicio != null ? inicio : LocalDate.now().withDayOfMonth(1);
        LocalDate fechaFin = fin != null ? fin : LocalDate.now();

        LocalDateTime inicioRango = fechaInicio.atStartOfDay();
        LocalDateTime finRango = fechaFin.atTime(LocalTime.MAX);

        List<Venta> ventas = ventaRepository.findByFiltros(empresaFiltrada, null, inicioRango, finRango, null, Boolean.TRUE);
        BigDecimal totalVentas = ventas.stream()
            .map(Venta::getMontoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal porcentaje = porcentajeImpuesto != null ? porcentajeImpuesto : IMPUESTO_POR_DEFECTO;
        BigDecimal factorImpuesto = porcentaje.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal impuestos = totalVentas.multiply(factorImpuesto).setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = totalVentas.subtract(impuestos);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("fechaInicio", fechaInicio);
        resumen.put("fechaFin", fechaFin);
        resumen.put("totalVentas", totalVentas);
        resumen.put("impuestos", impuestos);
        resumen.put("neto", neto);
        resumen.put("porcentajeImpuesto", porcentaje);
        resumen.put("cantidadVentas", ventas.size());
        return resumen;
    }

    private boolean puedeVerCaja(Caja caja) {
        if (caja == null || caja.getEmpresa() == null) {
            return false;
        }
        if (TenantContext.isSuperAdmin()) {
            return true;
        }
        Long empresaActual = TenantContext.requireEmpresaId();
        Long empresaCaja = caja.getEmpresa().getIdEmpresa();
        return empresaActual.equals(empresaCaja);
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }
}
