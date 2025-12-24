package com.sistema.turistico.service;

import com.sistema.turistico.dto.VoucherResponse;
import com.sistema.turistico.entity.Cliente;
import com.sistema.turistico.entity.Reserva;
import com.sistema.turistico.entity.Voucher;
import com.sistema.turistico.repository.VoucherRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ReservaService reservaService;

    @Transactional(readOnly = true)
    public VoucherPageResult listarVouchers(Long empresaId, String busqueda, Voucher.EstadoVoucher estado, int page, int size) {
        int pagina = Math.max(page, 1);
        int tamanio = Math.max(size, 1);

        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "Debe especificar una empresa para listar vouchers");

        String termino = normalizarTexto(busqueda);
        List<Voucher> filtrados = voucherRepository.findByReserva_Empresa_IdEmpresa(empresaFiltrada).stream()
            .filter(this::puedeVerVoucher)
            .filter(v -> estado == null || v.getEstado() == estado)
            .filter(v -> coincideBusqueda(v, termino))
            .sorted(Comparator.comparing(Voucher::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .toList();

        int total = filtrados.size();
        int startIndex = Math.min((pagina - 1) * tamanio, total);
        int endIndex = Math.min(startIndex + tamanio, total);

        List<VoucherResponse> data = filtrados.subList(startIndex, endIndex).stream()
            .map(reservaService::toVoucherResponse)
            .toList();

        return new VoucherPageResult(data, total, pagina, tamanio);
    }

    @Transactional(readOnly = true)
    public VoucherResponse obtenerPorCodigoQr(String codigoQr) {
        Voucher voucher = voucherRepository.findByCodigoQr(codigoQr)
            .orElseThrow(() -> new IllegalArgumentException("Voucher no encontrado"));
        return reservaService.toVoucherResponse(voucher);
    }

    public VoucherResponse actualizarEstado(String codigoQr, String estadoSolicitado) {
        Voucher voucher = obtenerVoucherAutorizado(codigoQr);
        Voucher.EstadoVoucher nuevoEstado = parseEstado(estadoSolicitado);

        if (voucher.getEstado() == nuevoEstado) {
            throw new IllegalArgumentException("El estado ya es " + nuevoEstado);
        }

        validarCambioEstado(voucher.getEstado(), nuevoEstado);

        voucher.setEstado(nuevoEstado);
        Voucher actualizado = voucherRepository.save(voucher);
        log.info("Voucher {} actualizado a estado {}", codigoQr, nuevoEstado);
        return reservaService.toVoucherResponse(actualizado);
    }

    public VoucherResponse cancelar(String codigoQr) {
        Voucher voucher = obtenerVoucherAutorizado(codigoQr);

        if (voucher.getEstado() == Voucher.EstadoVoucher.Usado) {
            throw new IllegalArgumentException("No se puede cancelar un voucher usado");
        }
        if (voucher.getEstado() == Voucher.EstadoVoucher.Expirado) {
            throw new IllegalArgumentException("No se puede cancelar un voucher expirado");
        }
        if (voucher.getEstado() == Voucher.EstadoVoucher.Cancelado) {
            throw new IllegalArgumentException("El voucher ya está cancelado");
        }

        voucher.setEstado(Voucher.EstadoVoucher.Cancelado);
        Voucher cancelado = voucherRepository.save(voucher);
        log.info("Voucher {} cancelado", codigoQr);
        return reservaService.toVoucherResponse(cancelado);
    }

    private Voucher obtenerVoucherAutorizado(String codigoQr) {
        Voucher voucher = voucherRepository.findByCodigoQr(codigoQr)
            .orElseThrow(() -> new IllegalArgumentException("Voucher no encontrado"));

        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaVoucher = obtenerEmpresaDelVoucher(voucher);
            if (!empresaActual.equals(empresaVoucher)) {
                throw new IllegalArgumentException("El voucher no pertenece a la empresa actual");
            }
        }
        return voucher;
    }

    private Long obtenerEmpresaDelVoucher(Voucher voucher) {
        Reserva reserva = voucher.getReserva();
        if (reserva == null || reserva.getEmpresa() == null) {
            return null;
        }
        return reserva.getEmpresa().getIdEmpresa();
    }

    private boolean coincideBusqueda(Voucher voucher, String termino) {
        if (termino == null) {
            return true;
        }
        String lower = termino.toLowerCase(Locale.ROOT);

        if (voucher.getCodigoQr() != null && voucher.getCodigoQr().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }

        Reserva reserva = voucher.getReserva();
        if (reserva != null) {
            if (reserva.getCodigoReserva() != null && reserva.getCodigoReserva().toLowerCase(Locale.ROOT).contains(lower)) {
                return true;
            }
            Cliente cliente = reserva.getCliente();
            if (cliente != null) {
                String nombreCompleto = Stream.of(cliente.getNombre(), cliente.getApellido())
                    .map(this::normalizarTexto)
                    .filter(Objects::nonNull)
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
                if (!nombreCompleto.isBlank() && nombreCompleto.toLowerCase(Locale.ROOT).contains(lower)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Voucher.EstadoVoucher parseEstado(String estadoSolicitado) {
        if (estadoSolicitado == null || estadoSolicitado.isBlank()) {
            throw new IllegalArgumentException("Debe proporcionar el estado para actualizar");
        }
        String valor = estadoSolicitado.trim();
        if ("Canjeado".equalsIgnoreCase(valor)) {
            valor = Voucher.EstadoVoucher.Usado.name();
        }
        try {
            return Voucher.EstadoVoucher.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado no válido: " + estadoSolicitado);
        }
    }

    private void validarCambioEstado(Voucher.EstadoVoucher estadoActual, Voucher.EstadoVoucher estadoNuevo) {
        if (estadoActual == estadoNuevo) {
            return;
        }
        switch (estadoActual) {
            case Emitido -> {
                if (estadoNuevo == Voucher.EstadoVoucher.Usado
                    || estadoNuevo == Voucher.EstadoVoucher.Expirado
                    || estadoNuevo == Voucher.EstadoVoucher.Cancelado) {
                    return;
                }
            }
            case Usado -> throw new IllegalArgumentException("No se puede cambiar el estado de un voucher usado");
            case Expirado -> throw new IllegalArgumentException("No se puede cambiar el estado de un voucher expirado");
            case Cancelado -> throw new IllegalArgumentException("No se puede cambiar el estado de un voucher cancelado");
            default -> {
            }
        }
        throw new IllegalArgumentException("Cambio de estado no permitido de " + estadoActual + " a " + estadoNuevo);
    }

    private boolean puedeVerVoucher(Voucher voucher) {
        if (TenantContext.isSuperAdmin()) {
            return true;
        }
        Long empresaActual = TenantContext.requireEmpresaId();
        Long empresaVoucher = obtenerEmpresaDelVoucher(voucher);
        return empresaActual.equals(empresaVoucher);
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record VoucherPageResult(List<VoucherResponse> data, int total, int page, int size) {
        public int totalPages() {
            if (size <= 0) {
                return 1;
            }
            return (int) Math.max(1, Math.ceil((double) total / size));
        }
    }
}
