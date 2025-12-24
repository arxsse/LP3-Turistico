package com.sistema.turistico.service;

import com.sistema.turistico.dto.ReservaResponse;
import com.sistema.turistico.dto.VentaResponse;
import com.sistema.turistico.entity.*;
import com.sistema.turistico.repository.*;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportesService {

    private final ReservaRepository reservaRepository;
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final PersonalRepository personalRepository;
    private final ServicioTuristicoRepository servicioTuristicoRepository;
    private final PaqueteTuristicoRepository paqueteTuristicoRepository;
    private final CategoriaServicioRepository categoriaServicioRepository;
    private final EvaluacionServicioRepository evaluacionServicioRepository;
    private final ReservaService reservaService;
    private final VentaService ventaService;

    // Reporte de Reservas
    public Map<String, Object> reporteReservas(Long empresaId, Long idSucursal, LocalDate fechaInicio, LocalDate fechaFin, String estado) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        LocalDate inicio = fechaInicio != null ? fechaInicio : LocalDate.now().minusMonths(1);
        LocalDate fin = fechaFin != null ? fechaFin : LocalDate.now();

        List<Reserva> reservas;
        if (idSucursal != null) {
            reservas = reservaRepository.findByEmpresaIdAndSucursalId(empresaFiltrada, idSucursal);
            reservas = reservas.stream()
                .filter(r -> {
                    Long sucursalReserva = r.getSucursal() != null ? r.getSucursal().getIdSucursal() : null;
                    return sucursalReserva != null && idSucursal.equals(sucursalReserva);
                })
                .filter(r -> {
                    java.sql.Date fechaServicio = r.getFechaServicio();
                    if (fechaServicio == null) return false;
                    LocalDate fechaLocal = fechaServicio.toLocalDate();
                    return !fechaLocal.isBefore(inicio) && !fechaLocal.isAfter(fin);
                })
                .collect(Collectors.toList());
        } else {
            reservas = reservaRepository.findByEmpresaAndFechaRango(empresaFiltrada, inicio, fin);
        }

        if (estado != null && !estado.isEmpty()) {
            reservas = reservas.stream()
                .filter(r -> r.getEstado() != null && estado.equals(r.getEstado().name()))
                .collect(Collectors.toList());
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("fechaInicio", inicio);
        resumen.put("fechaFin", fin);
        List<ReservaResponse> reservasDtos = reservas.stream()
            .map(this::buildReservaReporte)
            .collect(Collectors.toList());

        resumen.put("reservasRaw", reservasDtos); // Add raw data for frontend filtering
        resumen.put("totalReservas", reservas.size());
        resumen.put("reservasPorEstado", reservas.stream()
            .collect(Collectors.groupingBy(Reserva::getEstado, Collectors.counting())));
        resumen.put("totalMonto", reservas.stream()
            .map(Reserva::getPrecioTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        return resumen;
    }

    // Reporte de Ventas
    public Map<String, Object> reporteVentas(Long empresaId, Long idSucursal, LocalDate fechaInicio, LocalDate fechaFin) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        LocalDate inicio = fechaInicio != null ? fechaInicio : LocalDate.now().minusMonths(1);
        LocalDate fin = fechaFin != null ? fechaFin : LocalDate.now();

        LocalDateTime inicioDT = inicio.atStartOfDay();
        LocalDateTime finDT = fin.atTime(LocalTime.MAX);

        List<Venta> ventas = ventaRepository.findByFiltros(empresaFiltrada, idSucursal, inicioDT, finDT, null, Boolean.TRUE);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("fechaInicio", inicio);
        resumen.put("fechaFin", fin);
        List<VentaResponse> ventasDtos = ventas.stream()
            .map(ventaService::toResponse)
            .collect(Collectors.toList());

        resumen.put("ventasRaw", ventasDtos); // Add raw data for frontend filtering
        resumen.put("totalVentas", ventas.size());
        resumen.put("montoTotal", ventas.stream()
            .map(Venta::getMontoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        resumen.put("ventasPorMetodoPago", ventas.stream()
            .collect(Collectors.groupingBy(Venta::getMetodoPago, Collectors.counting())));

        return resumen;
    }

    // Reporte de Clientes
    public Map<String, Object> reporteClientes(Long empresaId, Long idSucursal) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        List<Cliente> clientes;
        if (idSucursal != null) {
            clientes = clienteRepository.findByEmpresaIdAndSucursalId(empresaFiltrada, idSucursal);
        } else {
            clientes = clienteRepository.findByEmpresaId(empresaFiltrada);
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("clientesRaw", clientes); // Add raw data for frontend filtering
        resumen.put("totalClientes", clientes.size());
        resumen.put("clientesActivos", clientes.stream()
            .filter(Cliente::getEstado)
            .count());
        resumen.put("clientesPorNacionalidad", clientes.stream()
            .filter(c -> c.getNacionalidad() != null)
            .collect(Collectors.groupingBy(Cliente::getNacionalidad, Collectors.counting())));
        resumen.put("clientesPorNivelMembresia", clientes.stream()
            .collect(Collectors.groupingBy(Cliente::getNivelMembresia, Collectors.counting())));

        return resumen;
    }

    // Reporte de Personal
    public Map<String, Object> reportePersonal(Long empresaId, Long idSucursal) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        List<Personal> personal;
        if (idSucursal != null) {
            personal = personalRepository.findWithFilters(empresaFiltrada, idSucursal, null, null, null);
        } else {
            personal = personalRepository.findByEmpresaId(empresaFiltrada);
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("personalRaw", personal); // Add raw data for frontend filtering
        resumen.put("totalPersonal", personal.size());
        resumen.put("personalActivo", personal.stream()
            .filter(Personal::getEstado)
            .count());
        resumen.put("personalPorCargo", personal.stream()
            .collect(Collectors.groupingBy(Personal::getCargo, Collectors.counting())));
        resumen.put("personalPorTurno", personal.stream()
            .collect(Collectors.groupingBy(Personal::getTurno, Collectors.counting())));

        return resumen;
    }

    // Reporte de Servicios Turísticos
    public Map<String, Object> reporteServicios(Long empresaId, Long idSucursal) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        List<ServicioTuristico> servicios;
        if (idSucursal != null) {
            servicios = servicioTuristicoRepository.findByEmpresaIdAndSucursalId(empresaFiltrada, idSucursal);
        } else {
            servicios = servicioTuristicoRepository.findByEmpresaId(empresaFiltrada);
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("serviciosRaw", servicios); // Add raw data for frontend filtering
        resumen.put("totalServicios", servicios.size());
        resumen.put("serviciosActivos", servicios.stream()
            .filter(ServicioTuristico::getEstado)
            .count());
        resumen.put("serviciosPorTipo", servicios.stream()
            .collect(Collectors.groupingBy(ServicioTuristico::getTipoServicio, Collectors.counting())));

        Map<Long, Long> serviciosPorCategoria = servicios.stream()
            .filter(s -> s.getIdCategoria() != null)
            .collect(Collectors.groupingBy(ServicioTuristico::getIdCategoria, Collectors.counting()));

        Map<String, Long> categoriasConNombre = new LinkedHashMap<>();
        if (!serviciosPorCategoria.isEmpty()) {
            Set<Long> categoriasIds = serviciosPorCategoria.keySet();
            Map<Long, String> nombresCategorias = categoriaServicioRepository.findByIdCategoriaIn(categoriasIds).stream()
                .collect(Collectors.toMap(CategoriaServicio::getIdCategoria, CategoriaServicio::getNombreCategoria));

            serviciosPorCategoria.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> {
                    Long categoriaId = entry.getKey();
                    String nombre = nombresCategorias.getOrDefault(categoriaId, "Categoria #" + categoriaId);
                    categoriasConNombre.put(nombre, entry.getValue());
                });
        }

        resumen.put("serviciosPorCategoria", categoriasConNombre);

        return resumen;
    }

    // Reporte de Paquetes Turísticos
    public Map<String, Object> reportePaquetes(Long empresaId, Long idSucursal) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        List<PaqueteTuristico> paquetes;
        if (idSucursal != null) {
            paquetes = paqueteTuristicoRepository.findByEmpresaIdAndSucursalId(empresaFiltrada, idSucursal);
        } else {
            paquetes = paqueteTuristicoRepository.findByEmpresaIdEmpresa(empresaFiltrada);
        }

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("paquetesRaw", paquetes); // Add raw data for frontend filtering
        resumen.put("totalPaquetes", paquetes.size());
        resumen.put("paquetesActivos", paquetes.stream()
            .filter(PaqueteTuristico::getEstado)
            .count());
        resumen.put("paquetesConPromocion", paquetes.stream()
            .filter(PaqueteTuristico::getPromocion)
            .count());
        if (!paquetes.isEmpty()) {
            BigDecimal total = paquetes.stream()
                .map(PaqueteTuristico::getPrecioTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            resumen.put("precioPromedio", total.divide(BigDecimal.valueOf(paquetes.size()), 2, RoundingMode.HALF_UP));
        } else {
            resumen.put("precioPromedio", BigDecimal.ZERO);
        }

        return resumen;
    }

    // Reporte de Evaluaciones
    public Map<String, Object> reporteEvaluaciones(Long empresaId, LocalDate fechaInicio, LocalDate fechaFin) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para generar el reporte");

        LocalDate inicio = fechaInicio != null ? fechaInicio : LocalDate.now().minusMonths(1);
        LocalDate fin = fechaFin != null ? fechaFin : LocalDate.now();

        List<EvaluacionServicio> evaluaciones = evaluacionServicioRepository.findByFechaRango(empresaFiltrada, inicio, fin);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("fechaInicio", inicio);
        resumen.put("fechaFin", fin);
        resumen.put("totalEvaluaciones", evaluaciones.size());
        resumen.put("promedioGeneral", evaluaciones.stream()
            .filter(e -> e.getCalificacionGeneral() != null)
            .mapToDouble(e -> e.getCalificacionGeneral().doubleValue())
            .average()
            .orElse(0.0));
        resumen.put("promedioGuia", evaluaciones.stream()
            .filter(e -> e.getCalificacionGuia() != null)
            .mapToDouble(e -> e.getCalificacionGuia().doubleValue())
            .average()
            .orElse(0.0));

        return resumen;
    }

    private ReservaResponse buildReservaReporte(Reserva reserva) {
        ReservaResponse response = reservaService.toResponse(reserva);
        if (reserva.getSucursal() != null) {
            response.setIdSucursal(reserva.getSucursal().getIdSucursal());
            if (response.getNombreSucursal() == null) {
                response.setNombreSucursal(reserva.getSucursal().getNombreSucursal());
            }
        }
        return response;
    }
}