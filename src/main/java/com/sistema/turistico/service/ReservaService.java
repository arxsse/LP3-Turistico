package com.sistema.turistico.service;

import com.sistema.turistico.dto.AsignacionPersonalRequest;
import com.sistema.turistico.dto.AsignacionPersonalUpdateRequest;
import com.sistema.turistico.dto.ReservaAsignacionPayload;
import com.sistema.turistico.dto.ReservaAsignacionResponse;
import com.sistema.turistico.dto.ReservaAsignacionSyncRequest;
import com.sistema.turistico.dto.ReservaEditRequest;
import com.sistema.turistico.dto.ReservaItemRequest;
import com.sistema.turistico.dto.ReservaItemResponse;
import com.sistema.turistico.dto.ReservaRequest;
import com.sistema.turistico.dto.ReservaResponse;
import com.sistema.turistico.dto.ReservaUpdateRequest;
import com.sistema.turistico.dto.VoucherResponse;
import com.sistema.turistico.entity.Cliente;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.AsignacionPersonal;
import com.sistema.turistico.entity.Reserva;
import com.sistema.turistico.entity.ReservaItem;
import com.sistema.turistico.entity.ServicioTuristico;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.entity.Voucher;
import com.sistema.turistico.entity.PaqueteTuristico;
import com.sistema.turistico.entity.PaqueteServicio;
import com.sistema.turistico.repository.AsignacionPersonalRepository;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.ReservaRepository;
import com.sistema.turistico.repository.SucursalRepository;
import com.sistema.turistico.repository.UsuarioRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteService clienteService;
    private final ServicioTuristicoService servicioService;
    private final PaqueteTuristicoService paqueteService;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionPersonalService asignacionPersonalService;
    private final AsignacionPersonalRepository asignacionPersonalRepository;

    /**
     * Crear una nueva reserva
     */
    public Reserva create(ReservaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La reserva es obligatoria");
        }

        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(request.getEmpresaId());
        Empresa empresa = obtenerEmpresaActiva(empresaId);

        log.info("Creando nueva reserva para cliente ID: {} en empresa {}", request.getClienteId(), empresaId);

        Cliente cliente = clienteService.findById(request.getClienteId())
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        validarMismaEmpresa(empresaId, cliente.getEmpresa() != null ? cliente.getEmpresa().getIdEmpresa() : null, "cliente");

        if (!cliente.isActivo()) {
            throw new IllegalArgumentException("El cliente no está activo");
        }

        List<ReservaItemRequest> itemRequests = request.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("Debe especificar al menos un item para la reserva");
        }

        Reserva reserva = new Reserva();
        reserva.setEmpresa(empresa);
        reserva.setCliente(cliente);
        reserva.setUsuario(obtenerUsuarioActual(empresa));
        reserva.setEstado(Reserva.EstadoReserva.Pendiente);
        reserva.setEvaluada(false);
        reserva.setObservaciones(request.getObservaciones());
        reserva.setIdPromocion(request.getPromocionId());
        reserva.setDescuentoAplicado(request.getDescuentoAplicado() != null ? request.getDescuentoAplicado() : BigDecimal.ZERO);

        // Manejar sucursal si está presente
        if (request.getIdSucursal() != null) {
            Sucursal sucursal = obtenerSucursalActiva(request.getIdSucursal(), empresaId);
            reserva.setSucursal(sucursal);
        } else {
            reserva.setSucursal(null);
        }

        Date fechaReserva = request.getFechaReserva() != null && !request.getFechaReserva().isBlank()
            ? parseSqlDate(request.getFechaReserva(), "fecha de reserva")
            : Date.valueOf(LocalDate.now());
        Date fechaServicio = parseSqlDate(request.getFechaServicio(), "fecha de servicio");

        reserva.setFechaReserva(fechaReserva);
        reserva.setFechaServicio(fechaServicio);

        validarFechas(reserva);

        List<ReservaItem> items = new ArrayList<>();
        int totalPersonas = 0;
        BigDecimal totalReserva = BigDecimal.ZERO;

        for (ReservaItemRequest itemRequest : itemRequests) {
            ReservaItem item = construirItem(itemRequest, empresaId);
            items.add(item);
            if (item.getCantidad() != null) {
                totalPersonas += item.getCantidad();
            }
            if (item.getPrecioTotal() != null) {
                totalReserva = totalReserva.add(item.getPrecioTotal());
            }
        }

        reserva.setItems(items);

        if (request.getNumeroPersonas() != null && request.getNumeroPersonas() > 0) {
            reserva.setNumeroPersonas(request.getNumeroPersonas());
        } else {
            reserva.setNumeroPersonas(Math.max(totalPersonas, 1));
        }

        if (totalReserva.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio total debe ser mayor a 0");
        }

        reserva.setPrecioTotal(totalReserva);
        if (reserva.getDescuentoAplicado() == null) {
            reserva.setDescuentoAplicado(BigDecimal.ZERO);
        }

        String codigoReserva = request.getCodigoReserva() != null ? request.getCodigoReserva().trim() : null;
        if (codigoReserva == null || codigoReserva.isEmpty()) {
            codigoReserva = generarCodigoReserva(empresaId);
        }

        reserva.setCodigoReserva(codigoReserva);
        if (reservaRepository.existsByEmpresaIdAndCodigoReservaAndIdNot(empresaId, codigoReserva, 0L)) {
            throw new IllegalArgumentException("Error interno: código de reserva duplicado, intente nuevamente");
        }

        Reserva savedReserva = reservaRepository.save(reserva);
        sincronizarAsignaciones(savedReserva, request.getAsignaciones());
        log.info("Reserva creada exitosamente con código: {}", savedReserva.getCodigoReserva());
        return savedReserva;
    }

    private ReservaItem construirItem(ReservaItemRequest itemRequest, Long empresaId) {
        if (itemRequest == null) {
            throw new IllegalArgumentException("El detalle de la reserva es obligatorio");
        }

        ReservaItem item = new ReservaItem();
        item.setTipoItem(itemRequest.getTipoItem());
        item.setCantidad(itemRequest.getCantidad() != null ? itemRequest.getCantidad() : 1);
        item.setDescripcionExtra(itemRequest.getDescripcionExtra());

        switch (itemRequest.getTipoItem()) {
            case SERVICIO -> prepararItemServicio(item, itemRequest, empresaId);
            case PAQUETE -> prepararItemPaquete(item, itemRequest, empresaId);
            default -> throw new IllegalArgumentException("Tipo de item desconocido");
        }

        BigDecimal precioUnitario = itemRequest.getPrecioUnitario();
        BigDecimal precioTotal = itemRequest.getPrecioTotal();

        if (precioUnitario == null && precioTotal != null) {
            precioUnitario = precioTotal.divide(BigDecimal.valueOf(item.getCantidad()), 2, RoundingMode.HALF_UP);
        }

        if (precioUnitario == null) {
            precioUnitario = item.esServicio()
                ? item.getServicio().getPrecioBase()
                : item.getPaquete().getPrecioFinal();
        }

        if (precioTotal == null) {
            precioTotal = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));
        } else {
            BigDecimal recalculado = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));
            if (recalculado.compareTo(precioTotal) != 0) {
                precioTotal = recalculado;
            }
        }

        item.setPrecioUnitario(precioUnitario);
        item.setPrecioTotal(precioTotal);

        return item;
    }

    private void prepararItemServicio(ReservaItem item, ReservaItemRequest itemRequest, Long empresaId) {
        Long servicioId = itemRequest.getServicioId();
        if (servicioId == null) {
            throw new IllegalArgumentException("Debe especificar el servicio turístico");
        }

        ServicioTuristico servicio = servicioService.findById(servicioId)
            .orElseThrow(() -> new IllegalArgumentException("Servicio turístico no encontrado"));
        validarMismaEmpresa(empresaId, servicio.getEmpresa() != null ? servicio.getEmpresa().getIdEmpresa() : null, "servicio turístico");

        if (!servicio.isActivo()) {
            throw new IllegalArgumentException("El servicio seleccionado no está activo");
        }

        if (!servicio.tieneDisponibilidad(item.getCantidad())) {
            throw new IllegalArgumentException("No hay disponibilidad para el número de personas solicitado en el servicio " + servicio.getNombreServicio());
        }

        item.setServicio(servicio);
    }

    private void prepararItemPaquete(ReservaItem item, ReservaItemRequest itemRequest, Long empresaId) {
        Long paqueteId = itemRequest.getPaqueteId();
        if (paqueteId == null) {
            throw new IllegalArgumentException("Debe especificar el paquete turístico");
        }

        PaqueteTuristico paquete = paqueteService.findById(paqueteId)
            .orElseThrow(() -> new IllegalArgumentException("Paquete turístico no encontrado"));
        validarPaquetePerteneceAEmpresa(paquete, empresaId);

        if (!paquete.isActivo()) {
            throw new IllegalArgumentException("El paquete seleccionado no está activo");
        }

        item.setPaquete(paquete);
    }

    private void validarPaquetePerteneceAEmpresa(PaqueteTuristico paquete, Long empresaId) {
        validarMismaEmpresa(empresaId,
            paquete.getEmpresa() != null ? paquete.getEmpresa().getIdEmpresa() : null,
            "paquete turístico");

        List<PaqueteServicio> serviciosIncluidos = paquete.getServiciosIncluidos();
        if (serviciosIncluidos == null || serviciosIncluidos.isEmpty()) {
            return;
        }

        for (PaqueteServicio paqueteServicio : serviciosIncluidos) {
            if (paqueteServicio == null) {
                throw new IllegalArgumentException("El paquete contiene registros de servicios inválidos");
            }

            ServicioTuristico servicio = paqueteServicio.getServicio();
            if (servicio == null) {
                throw new IllegalArgumentException("El paquete contiene un servicio no cargado. Revisa la configuración del paquete.");
            }

            validarMismaEmpresa(empresaId,
                servicio.getEmpresa() != null ? servicio.getEmpresa().getIdEmpresa() : null,
                "servicio '" + servicio.getNombreServicio() + "' incluido en el paquete");

            if (!servicio.isActivo()) {
                throw new IllegalArgumentException("El paquete contiene el servicio " + servicio.getNombreServicio() + " que está inactivo");
            }
        }
    }

    private Date parseSqlDate(String fecha, String nombreCampo) {
        try {
            return Date.valueOf(LocalDate.parse(fecha.trim()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("La " + nombreCampo + " no tiene un formato válido (yyyy-MM-dd)");
        }
    }

    private ReservaItemResponse toItemResponse(ReservaItem item) {
        ReservaItemResponse response = new ReservaItemResponse();
        response.setIdReservaItem(item.getIdReservaItem());
        response.setTipoItem(item.getTipoItem() != null ? item.getTipoItem().name() : null);
        response.setCantidad(item.getCantidad());
        response.setPrecioUnitario(item.getPrecioUnitario());
        response.setPrecioTotal(item.getPrecioTotal());
        response.setDescripcionExtra(item.getDescripcionExtra());

        if (item.getServicio() != null) {
            response.setIdServicio(item.getServicio().getIdServicio());
            response.setNombreServicio(item.getServicio().getNombreServicio());
            response.setTipoServicio(item.getServicio().getTipoServicio() != null
                ? item.getServicio().getTipoServicio().toString()
                : null);
        }

        if (item.getPaquete() != null) {
            response.setIdPaquete(item.getPaquete().getIdPaquete());
            response.setNombrePaquete(item.getPaquete().getNombrePaquete());
        }

        return response;
    }

    /**
     * Buscar reserva por ID
     */
    @Transactional(readOnly = true)
    public Optional<Reserva> findById(Long id) {
        log.debug("Buscando reserva con ID: {}", id);
        Optional<Reserva> reservaOpt = reservaRepository.findByIdWithItems(id)
            .filter(reserva -> reserva.getDeletedAt() == null);
        
        if (reservaOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Reserva reserva = reservaOpt.get();
        validarPertenencia(reserva);
        
        // Inicializamos relaciones perezosas críticas para el detalle dentro de la transacción
        // Esto asegura que todas las relaciones estén cargadas antes de que termine la transacción
        
        // Inicializar items y sus relaciones
        if (reserva.getItems() != null) {
            reserva.getItems().forEach(item -> {
                if (item.getServicio() != null) {
                    // Forzar inicialización accediendo a propiedades
                    item.getServicio().getNombreServicio();
                    item.getServicio().getTipoServicio();
                }
                if (item.getPaquete() != null) {
                    item.getPaquete().getNombrePaquete();
                }
            });
        }
        
        // Asegurar que la empresa se inicialice (necesaria para validarPertenencia)
        if (reserva.getEmpresa() != null) {
            reserva.getEmpresa().getIdEmpresa();
        }
        
        // Asegurar que la sucursal se inicialice si existe
        if (reserva.getSucursal() != null) {
            reserva.getSucursal().getNombreSucursal();
            reserva.getSucursal().getIdSucursal();
        }
        
        // Asegurar que el cliente se inicialice
        if (reserva.getCliente() != null) {
            reserva.getCliente().getNombre();
            reserva.getCliente().getApellido();
            reserva.getCliente().getEmail();
            reserva.getCliente().getTelefono();
            reserva.getCliente().getIdCliente();
        }
        
        // Asegurar que el usuario se inicialice
        if (reserva.getUsuario() != null) {
            reserva.getUsuario().getNombre();
            reserva.getUsuario().getApellido();
            reserva.getUsuario().getIdUsuario();
        }
        
        return Optional.of(reserva);
    }

    /**
     * Buscar reserva por ID y convertir a ReservaResponse en una sola transacción
     */
    @Transactional(readOnly = true)
    public Optional<ReservaResponse> findByIdAndConvert(Long id) {
        log.debug("Buscando reserva con ID y convirtiendo a response: {}", id);
        return findById(id)
            .map(this::toResponse);
    }

    /**
     * Listar reservas por empresa
     */
    @Transactional(readOnly = true)
    public List<Reserva> findByEmpresaId(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Listando reservas de empresa ID: {}", empresaFiltrada);
        return reservaRepository.findByEmpresaId(empresaFiltrada);
    }

    /**
     * Listar reservas por cliente
     */
    @Transactional(readOnly = true)
    public List<Reserva> findByEmpresaIdAndClienteId(Long empresaId, Long clienteId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Listando reservas de empresa {} y cliente {}", empresaFiltrada, clienteId);

        Cliente cliente = clienteService.findById(clienteId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        validarMismaEmpresa(empresaFiltrada, cliente.getEmpresa() != null ? cliente.getEmpresa().getIdEmpresa() : null, "cliente");

        return reservaRepository.findByEmpresaIdAndClienteId(empresaFiltrada, clienteId);
    }

    /**
     * Listar reservas por estado
     */
    @Transactional(readOnly = true)
    public List<Reserva> findByEmpresaIdAndEstado(Long empresaId, Long idSucursal, Reserva.EstadoReserva estado) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Listando reservas de empresa {} con estado {}", empresaFiltrada, estado);
        List<Reserva> reservas = reservaRepository.findByEmpresaIdAndEstado(empresaFiltrada, estado);
        return filtrarReservasPorSucursal(reservas, idSucursal);
    }

    /**
     * Buscar reservas con filtros
     */
    @Transactional(readOnly = true)
    public List<Reserva> findByEmpresaIdAndBusqueda(Long empresaId, Long idSucursal, String busqueda) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando reservas en empresa {} con término: {}", empresaFiltrada, busqueda);
        if (busqueda == null || busqueda.trim().isEmpty()) {
            List<Reserva> reservas = reservaRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaFiltrada);
            return filtrarReservasPorSucursal(reservas, idSucursal);
        }
        List<Reserva> reservas = reservaRepository.findByEmpresaIdAndBusqueda(empresaFiltrada, busqueda.trim());
        return filtrarReservasPorSucursal(reservas, idSucursal);
    }

    private List<Reserva> filtrarReservasPorSucursal(List<Reserva> reservas, Long idSucursal) {
        if (idSucursal == null) {
            return reservas;
        }
        return reservas.stream()
            .filter(reserva -> reserva.getSucursal() != null && reserva.getSucursal().getIdSucursal() != null
                && idSucursal.equals(reserva.getSucursal().getIdSucursal()))
            .toList();
    }

    /**
     * Actualizar reserva
     */
    public Reserva update(Long id, ReservaUpdateRequest request) {
        log.info("Actualizando reserva ID: {}", id);

        Reserva reservaExistente = obtenerReservaAutorizada(id);

        // Validaciones según el estado actual
        if (!reservaExistente.isActiva()) {
            throw new IllegalArgumentException("No se puede modificar una reserva cancelada o completada");
        }

        // Validar que al menos un campo esté presente
        if (request.getEstado() == null && request.getObservaciones() == null) {
            throw new IllegalArgumentException("Debe proporcionar al menos un campo para actualizar");
        }

        // Actualizar campos permitidos
        if (request.getObservaciones() != null) {
            reservaExistente.setObservaciones(request.getObservaciones());
        }

        // Solo permitir cambio de estado en ciertas condiciones
        if (request.getEstado() != null && request.getEstado() != reservaExistente.getEstado()) {
            validarCambioEstado(reservaExistente.getEstado(), request.getEstado());
            reservaExistente.setEstado(request.getEstado());
        }

        Reserva updatedReserva = reservaRepository.save(reservaExistente);
        log.info("Reserva actualizada exitosamente: {}", updatedReserva.getIdReserva());
        return updatedReserva;
    }

    public Reserva updateDetalle(Long id, ReservaEditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de edición es obligatoria");
        }

        log.info("Actualizando detalle de reserva ID: {}", id);

        Reserva reservaExistente = obtenerReservaAutorizada(id);

        if (reservaExistente.getEstado() != Reserva.EstadoReserva.Pendiente) {
            throw new IllegalStateException("Solo se pueden editar reservas en estado Pendiente");
        }

        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(request.getEmpresaId());
        Long empresaReserva = reservaExistente.getEmpresa() != null
            ? reservaExistente.getEmpresa().getIdEmpresa()
            : null;
        validarMismaEmpresa(empresaId, empresaReserva, "reserva");

        if (request.getFechaServicio() == null || request.getFechaServicio().isBlank()) {
            throw new IllegalArgumentException("La fecha de servicio es obligatoria");
        }

        List<ReservaItemRequest> itemRequests = request.getItems();
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("Debe especificar al menos un item para la reserva");
        }

        Date nuevaFechaServicio = parseSqlDate(request.getFechaServicio(), "fecha de servicio");

        List<ReservaItem> nuevosItems = new ArrayList<>();
        int totalPersonas = 0;
        BigDecimal totalReserva = BigDecimal.ZERO;

        for (ReservaItemRequest itemRequest : itemRequests) {
            ReservaItem item = construirItem(itemRequest, empresaId);
            nuevosItems.add(item);

            if (item.getCantidad() != null) {
                totalPersonas += item.getCantidad();
            }

            if (item.getPrecioTotal() != null) {
                totalReserva = totalReserva.add(item.getPrecioTotal());
            }
        }

        if (totalReserva.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio total debe ser mayor a 0");
        }

        reservaExistente.setFechaServicio(nuevaFechaServicio);
        reservaExistente.setItems(nuevosItems);
        reservaExistente.setPrecioTotal(totalReserva);

        if (request.getNumeroPersonas() != null && request.getNumeroPersonas() > 0) {
            reservaExistente.setNumeroPersonas(request.getNumeroPersonas());
        } else {
            reservaExistente.setNumeroPersonas(Math.max(totalPersonas, 1));
        }

        if (request.getDescuentoAplicado() != null) {
            reservaExistente.setDescuentoAplicado(request.getDescuentoAplicado());
        } else if (reservaExistente.getDescuentoAplicado() == null) {
            reservaExistente.setDescuentoAplicado(BigDecimal.ZERO);
        }

        if (request.getObservaciones() != null) {
            reservaExistente.setObservaciones(request.getObservaciones());
        }

        validarFechas(reservaExistente);

        Reserva updatedReserva = reservaRepository.save(reservaExistente);

        List<ReservaAsignacionPayload> asignacionesPayload = request.getAsignaciones();
        if (Boolean.TRUE.equals(request.getSincronizarAsignaciones())) {
            sincronizarAsignaciones(updatedReserva, asignacionesPayload != null ? asignacionesPayload : List.of());
        } else if (asignacionesPayload != null) {
            sincronizarAsignaciones(updatedReserva, asignacionesPayload);
        }
        log.info("Detalle de reserva {} actualizado exitosamente", updatedReserva.getIdReserva());
        return updatedReserva;
    }

    /**
     * Convertir Reserva a ReservaResponse
     * Nota: Este método debe ser llamado dentro de un contexto transaccional
     * para evitar LazyInitializationException
     */
    @Transactional(readOnly = true)
    public ReservaResponse toResponse(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.setIdReserva(reserva.getIdReserva());
        response.setCodigoReserva(reserva.getCodigoReserva());
        response.setFechaReserva(reserva.getFechaReserva() != null ? reserva.getFechaReserva().toString() : null);
        response.setFechaServicio(reserva.getFechaServicio() != null ? reserva.getFechaServicio().toString() : null);
        response.setNumeroPersonas(reserva.getNumeroPersonas());
        response.setPrecioTotal(reserva.getPrecioTotal());
        response.setDescuentoAplicado(reserva.getDescuentoAplicado());
        response.setEstado(reserva.getEstado().toString());
        response.setObservaciones(reserva.getObservaciones());
        response.setEvaluada(reserva.getEvaluada());
        response.setCreatedAt(reserva.getCreatedAt());
        response.setUpdatedAt(reserva.getUpdatedAt());

        // Información del cliente
        if (reserva.getCliente() != null) {
            response.setIdCliente(reserva.getCliente().getIdCliente());
            response.setNombreCliente(reserva.getCliente().getNombre());
            response.setApellidoCliente(reserva.getCliente().getApellido());
            response.setEmailCliente(reserva.getCliente().getEmail());
            response.setTelefonoCliente(reserva.getCliente().getTelefono());
        }

        List<ReservaItemResponse> items = reserva.getItems() != null
            ? reserva.getItems().stream().map(this::toItemResponse).toList()
            : List.of();
        response.setItems(items);

        items.stream()
            .filter(item -> "SERVICIO".equals(item.getTipoItem()))
            .findFirst()
            .ifPresent(item -> {
                response.setIdServicio(item.getIdServicio());
                response.setNombreServicio(item.getNombreServicio());
                response.setTipoServicio(item.getTipoServicio());
            });

        items.stream()
            .filter(item -> "PAQUETE".equals(item.getTipoItem()))
            .findFirst()
            .ifPresent(item -> {
                response.setIdPaquete(item.getIdPaquete());
                response.setNombrePaquete(item.getNombrePaquete());
            });

        // Información del usuario
        if (reserva.getUsuario() != null) {
            response.setIdUsuario(reserva.getUsuario().getIdUsuario());
            response.setNombreUsuario(reserva.getUsuario().getNombre());
            response.setApellidoUsuario(reserva.getUsuario().getApellido());
        }

        // Información de la sucursal
        if (reserva.getSucursal() != null) {
            try {
                Sucursal sucursal = reserva.getSucursal();
                response.setIdSucursal(sucursal.getIdSucursal());
                response.setNombreSucursal(sucursal.getNombreSucursal());
            } catch (Exception e) {
                log.warn("Error al obtener información de sucursal para reserva {}: {}", reserva.getIdReserva(), e.getMessage());
            }
        }

        // Obtener asignaciones dentro del contexto transaccional
        try {
            List<ReservaAsignacionResponse> asignaciones = asignacionPersonalRepository.findDetailedByReservaId(reserva.getIdReserva()).stream()
                .map(this::toAsignacionResponse)
                .toList();
            response.setAsignaciones(asignaciones);
        } catch (Exception e) {
            log.warn("Error al obtener asignaciones para reserva {}: {}", reserva.getIdReserva(), e.getMessage());
            // Si hay un error, establecemos una lista vacía
            response.setAsignaciones(List.of());
        }

        return response;
    }

    /**
     * Convertir Voucher a VoucherResponse
     */
    public VoucherResponse toVoucherResponse(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setIdVoucher(voucher.getIdVoucher());
        response.setCodigoQr(voucher.getCodigoQr());
        response.setFechaEmision(voucher.getFechaEmision() != null ? voucher.getFechaEmision().toString() : null);
        response.setFechaExpiracion(voucher.getFechaExpiracion() != null ? voucher.getFechaExpiracion().toString() : null);
        response.setEstado(voucher.getEstado().toString());
        response.setCreatedAt(voucher.getCreatedAt());
        response.setUpdatedAt(voucher.getUpdatedAt());

        // Información de la reserva relacionada
        if (voucher.getReserva() != null) {
            Reserva reserva = voucher.getReserva();
            response.setIdReserva(reserva.getIdReserva());
            response.setCodigoReserva(reserva.getCodigoReserva());
            response.setFechaServicio(reserva.getFechaServicio() != null ? reserva.getFechaServicio().toString() : null);
            response.setNumeroPersonas(reserva.getNumeroPersonas());

            // Información del cliente
            if (reserva.getCliente() != null) {
                response.setIdCliente(reserva.getCliente().getIdCliente());
                response.setNombreCliente(reserva.getCliente().getNombre());
                response.setApellidoCliente(reserva.getCliente().getApellido());
                response.setEmailCliente(reserva.getCliente().getEmail());
            }

            // Información del servicio
            List<ReservaItem> items = reserva.getItems() != null ? reserva.getItems() : List.of();
            items.stream()
                .map(this::toItemResponse)
                .filter(item -> "SERVICIO".equals(item.getTipoItem()))
                .findFirst()
                .ifPresent(item -> {
                    response.setIdServicio(item.getIdServicio());
                    response.setNombreServicio(item.getNombreServicio());
                    response.setTipoServicio(item.getTipoServicio());
                });
        }

        return response;
    }

    private ReservaAsignacionResponse toAsignacionResponse(AsignacionPersonal asignacion) {
        ReservaAsignacionResponse response = new ReservaAsignacionResponse();
        response.setIdAsignacion(asignacion.getIdAsignacion());
        if (asignacion.getPersonal() != null) {
            response.setIdPersonal(asignacion.getPersonal().getIdPersonal());
            response.setNombrePersonal(asignacion.getPersonal().getNombre());
            response.setApellidoPersonal(asignacion.getPersonal().getApellido());
            response.setDniPersonal(asignacion.getPersonal().getDni());
            response.setTelefonoPersonal(asignacion.getPersonal().getTelefono());
            response.setEmailPersonal(asignacion.getPersonal().getEmail());
            response.setCargoPersonal(asignacion.getPersonal().getCargo() != null
                ? asignacion.getPersonal().getCargo().name()
                : null);
        }
        response.setRolAsignado(asignacion.getRolAsignado() != null ? asignacion.getRolAsignado().name() : null);
        response.setEstado(asignacion.getEstado() != null ? asignacion.getEstado().name() : null);
        response.setObservaciones(asignacion.getObservaciones());
        response.setFechaAsignacion(asignacion.getFechaAsignacion() != null
            ? asignacion.getFechaAsignacion().toString()
            : null);
        if (asignacion.getCreatedAt() != null) {
            response.setCreatedAt(LocalDateTime.ofInstant(asignacion.getCreatedAt().toInstant(), ZoneId.systemDefault()));
        }
        if (asignacion.getUpdatedAt() != null) {
            response.setUpdatedAt(LocalDateTime.ofInstant(asignacion.getUpdatedAt().toInstant(), ZoneId.systemDefault()));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<ReservaAsignacionResponse> obtenerAsignaciones(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        return asignacionPersonalRepository.findDetailedByReservaId(reserva.getIdReserva()).stream()
            .map(this::toAsignacionResponse)
            .toList();
    }

    public Reserva actualizarAsignaciones(Long reservaId, ReservaAsignacionSyncRequest request) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        sincronizarAsignaciones(reserva, request != null ? request.getAsignaciones() : null);
        return reservaRepository.findByIdWithItems(reservaId).orElse(reserva);
    }

    private void sincronizarAsignaciones(Reserva reserva, List<ReservaAsignacionPayload> asignacionesPayload) {
        if (asignacionesPayload == null) {
            return;
        }

        if (reserva.getIdReserva() == null) {
            throw new IllegalStateException("La reserva debe existir antes de gestionar asignaciones");
        }

        if (!puedeActualizarAsignaciones(reserva.getEstado())) {
            throw new IllegalStateException("Solo se pueden gestionar asignaciones cuando la reserva está en estado Pendiente o Pagada");
        }

        validarAsignacionesDuplicadas(asignacionesPayload);

        Date fechaServicio = reserva.getFechaServicio();
        List<AsignacionPersonal> existentes = asignacionPersonalRepository.findByReservaId(reserva.getIdReserva());
        Map<Long, AsignacionPersonal> existentesPorId = existentes.stream()
            .filter(asignacion -> asignacion.getIdAsignacion() != null)
            .collect(Collectors.toMap(AsignacionPersonal::getIdAsignacion, asignacion -> asignacion, (primera, segunda) -> primera));
        Map<Long, AsignacionPersonal> existentesPorPersonal = existentes.stream()
            .filter(asignacion -> asignacion.getPersonal() != null && asignacion.getPersonal().getIdPersonal() != null)
            .collect(Collectors.toMap(asignacion -> asignacion.getPersonal().getIdPersonal(), asignacion -> asignacion, (primera, segunda) -> primera));

        Set<Long> asignacionesConservadas = new HashSet<>();

        for (ReservaAsignacionPayload payload : asignacionesPayload) {
            Date fechaAsignacion = parseFechaAsignacion(payload.getFechaAsignacion(), fechaServicio);
            AsignacionPersonal existente = null;

            if (payload.getIdAsignacion() != null) {
                existente = existentesPorId.get(payload.getIdAsignacion());
                if (existente == null) {
                    throw new IllegalArgumentException("Asignación no encontrada para la reserva");
                }
            } else {
                // Fallback: si la UI no envía el ID persistido, conciliamos por idPersonal para evitar duplicados fantasma.
                existente = existentesPorPersonal.get(payload.getIdPersonal());
            }

            if (existente != null) {
                if (existente.getIdAsignacion() != null) {
                    asignacionesConservadas.add(existente.getIdAsignacion());
                    existentesPorId.remove(existente.getIdAsignacion());
                }
                Long personalExistenteId = existente.getPersonal() != null ? existente.getPersonal().getIdPersonal() : null;
                if (personalExistenteId != null) {
                    existentesPorPersonal.remove(personalExistenteId);
                }
                if (!existente.getReserva().getIdReserva().equals(reserva.getIdReserva())) {
                    throw new IllegalArgumentException("La asignación no pertenece a la reserva");
                }

                // Si la fecha de asignación no coincide con la nueva fecha de servicio, actualizarla
                if (!fechaAsignacion.equals(reserva.getFechaServicio())) {
                    fechaAsignacion = reserva.getFechaServicio();
                }

                boolean requiereActualizacion = !Objects.equals(existente.getPersonal().getIdPersonal(), payload.getIdPersonal())
                    || !Objects.equals(normalizarObservacion(existente.getObservaciones()), normalizarObservacion(payload.getObservaciones()));

                AsignacionPersonal asignacionPersistida = existente;
                if (requiereActualizacion) {
                    AsignacionPersonalUpdateRequest updateRequest = new AsignacionPersonalUpdateRequest();
                    updateRequest.setIdPersonal(payload.getIdPersonal());
                    updateRequest.setObservaciones(payload.getObservaciones());
                    asignacionPersistida = asignacionPersonalService.update(existente.getIdAsignacion(), updateRequest);
                }

                if (!Objects.equals(asignacionPersistida.getFechaAsignacion(), fechaAsignacion)) {
                    if (asignacionPersonalRepository.existsByPersonalAndFecha(payload.getIdPersonal(), fechaAsignacion)) {
                        throw new IllegalArgumentException("El personal ya está asignado en esta fecha");
                    }
                    asignacionPersistida.setFechaAsignacion(fechaAsignacion);
                    asignacionPersonalRepository.save(asignacionPersistida);
                }
            } else {
                AsignacionPersonalRequest nuevo = new AsignacionPersonalRequest();
                nuevo.setIdPersonal(payload.getIdPersonal());
                nuevo.setIdReserva(reserva.getIdReserva());
                nuevo.setFechaAsignacion(fechaAsignacion);
                nuevo.setObservaciones(payload.getObservaciones());
                asignacionPersonalService.create(nuevo);
            }
        }

        for (AsignacionPersonal existente : existentes) {
            if (existente.getIdAsignacion() != null && !asignacionesConservadas.contains(existente.getIdAsignacion())) {
                asignacionPersonalService.delete(existente.getIdAsignacion());
            }
        }
    }

    /**
     * Cancelar reserva
     */
    public Reserva cancel(Long id) {
        log.info("Cancelando reserva ID: {}", id);

        Reserva reserva = obtenerReservaAutorizada(id);

        if (!reserva.puedeCancelarse()) {
            throw new IllegalArgumentException("La reserva no puede ser cancelada");
        }

        reserva.setEstado(Reserva.EstadoReserva.Cancelada);
        Reserva cancelledReserva = reservaRepository.save(reserva);
        actualizarEstadoAsignaciones(cancelledReserva, AsignacionPersonal.EstadoAsignacion.Cancelado);

        log.info("Reserva cancelada exitosamente: {}", cancelledReserva.getIdReserva());
        return cancelledReserva;
    }

    /**
     * Eliminar reserva (soft delete)
     */
    public void delete(Long id) {
        log.info("Eliminando reserva ID: {} (soft delete)", id);

        Reserva reserva = obtenerReservaAutorizada(id);

        if (reserva.getEstado() == Reserva.EstadoReserva.Completada) {
            throw new IllegalArgumentException("No se puede eliminar una reserva completada");
        }

        reserva.setDeletedAt(LocalDateTime.now());
        reservaRepository.save(reserva);

        log.info("Reserva eliminada exitosamente: {}", id);
    }

    /**
     * Completar reserva (marcar como completada después de finalizar el servicio)
     */
    public Reserva completar(Long id) {
        log.info("Completando reserva ID {}", id);

        Reserva reserva = obtenerReservaAutorizada(id);

        if (reserva.getEstado() != Reserva.EstadoReserva.Pagada) {
            throw new IllegalArgumentException("Solo se pueden completar reservas pagadas");
        }

        reserva.setEstado(Reserva.EstadoReserva.Completada);
        Reserva completedReserva = reservaRepository.save(reserva);
        actualizarEstadoAsignaciones(completedReserva, AsignacionPersonal.EstadoAsignacion.Completado);

        log.info("Reserva completada exitosamente: {}", completedReserva.getIdReserva());
        return completedReserva;
    }

    private void actualizarEstadoAsignaciones(Reserva reserva, AsignacionPersonal.EstadoAsignacion nuevoEstado) {
        if (reserva == null || reserva.getIdReserva() == null) {
            return;
        }

        List<AsignacionPersonal> asignaciones = asignacionPersonalRepository.findByReservaId(reserva.getIdReserva());
        if (asignaciones.isEmpty()) {
            return;
        }

        boolean requiereActualizacion = false;
        for (AsignacionPersonal asignacion : asignaciones) {
            if (asignacion.getEstado() != nuevoEstado) {
                asignacion.setEstado(nuevoEstado);
                requiereActualizacion = true;
            }
        }

        if (requiereActualizacion) {
            asignacionPersonalRepository.saveAll(asignaciones);
        }
    }

    /**
     * Marcar reserva como evaluada
     */
    public Reserva marcarComoEvaluada(Long id) {
        log.info("Marcando reserva ID {} como evaluada", id);

        Reserva reserva = obtenerReservaAutorizada(id);

        if (!reserva.puedeEvaluarse()) {
            throw new IllegalArgumentException("La reserva no puede ser evaluada");
        }

        reserva.setEvaluada(true);
        Reserva evaluatedReserva = reservaRepository.save(reserva);

        log.info("Reserva marcada como evaluada: {}", evaluatedReserva.getIdReserva());
        return evaluatedReserva;
    }

    private Reserva obtenerReservaAutorizada(Long id) {
        Reserva reserva = reservaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));
        if (reserva.getDeletedAt() != null) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        validarPertenencia(reserva);
        return reserva;
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
        validarMismaEmpresa(empresaId, sucursal.getEmpresa() != null ? sucursal.getEmpresa().getIdEmpresa() : null, "sucursal");
        return sucursal;
    }

    private Usuario obtenerUsuarioActual(Empresa empresa) {
        Long usuarioId = TenantContext.requireUserId();
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
        if (!esSuperAdmin()) {
            Long empresaUsuario = usuario.getEmpresa() != null ? usuario.getEmpresa().getIdEmpresa() : null;
            if (!empresa.getIdEmpresa().equals(empresaUsuario)) {
                throw new IllegalArgumentException("El usuario autenticado no pertenece a la empresa actual");
            }
        }
        return usuario;
    }

    private void validarPertenencia(Reserva reserva) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaReserva = reserva.getEmpresa() != null ? reserva.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaReserva)) {
                throw new IllegalArgumentException("La reserva no pertenece a la empresa actual");
            }
        }
    }

    /**
     * Generar código secuencial de reserva por empresa
     */
    private String generarCodigoReserva(Long empresaId) {
        Optional<String> lastCodeOpt = reservaRepository.findMaxCodigoReservaByEmpresaId(empresaId);
        int nextNumber = 1;
        if (lastCodeOpt.isPresent()) {
            String lastCode = lastCodeOpt.get();
            String[] parts = lastCode.split("-");
            if (parts.length >= 3) {
                try {
                    int lastNumber = Integer.parseInt(parts[2]);
                    nextNumber = lastNumber + 1;
                } catch (NumberFormatException e) {
                    // Si falla el parsing, empezar desde 1
                }
            }
        }
        return String.format("RES-%d-%05d", empresaId, nextNumber);
    }

    /**
     * Validar fechas de la reserva
     */
    private void validarFechas(Reserva reserva) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaServicio = reserva.getFechaServicio().toLocalDate();

        if (fechaServicio.isBefore(hoy)) {
            throw new IllegalArgumentException("La fecha de servicio no puede ser anterior a hoy");
        }

        // Validar que la fecha de reserva no sea futura (máximo 1 año)
        LocalDate fechaReserva = reserva.getFechaReserva().toLocalDate();
        if (fechaReserva.isAfter(hoy.plusYears(1))) {
            throw new IllegalArgumentException("La fecha de reserva no puede ser más de 1 año en el futuro");
        }
    }

    private Date parseFechaAsignacion(String fechaAsignacion, Date fechaServicio) {
        if (fechaServicio == null) {
            throw new IllegalArgumentException("La fecha del servicio es obligatoria para asignar personal");
        }

        if (fechaAsignacion == null || fechaAsignacion.isBlank()) {
            return fechaServicio;
        }

        Date parsed = parseSqlDate(fechaAsignacion, "fecha de asignación");
        // Si no coincide con la fecha de servicio, usar la fecha de servicio para forzar coincidencia
        if (!parsed.equals(fechaServicio)) {
            return fechaServicio;
        }
        return parsed;
    }

    private void validarAsignacionesDuplicadas(List<ReservaAsignacionPayload> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            return;
        }

        Set<Long> personalIds = new HashSet<>();
        for (ReservaAsignacionPayload payload : asignaciones) {
            if (!personalIds.add(payload.getIdPersonal())) {
                throw new IllegalArgumentException("El personal con ID " + payload.getIdPersonal() + " está duplicado en la solicitud");
            }
        }
    }

    private boolean puedeActualizarAsignaciones(Reserva.EstadoReserva estado) {
        return estado == Reserva.EstadoReserva.Pendiente || estado == Reserva.EstadoReserva.Pagada;
    }

    private String normalizarObservacion(String observacion) {
        if (observacion == null) {
            return null;
        }
        String valor = observacion.trim();
        return valor.isEmpty() ? null : valor;
    }

    /**
     * Validar cambio de estado
     */
    private void validarCambioEstado(Reserva.EstadoReserva estadoActual, Reserva.EstadoReserva estadoNuevo) {
        if (estadoActual == estadoNuevo) {
            return;
        }

        switch (estadoActual) {
            case Pendiente:
                if (estadoNuevo == Reserva.EstadoReserva.Confirmada
                    || estadoNuevo == Reserva.EstadoReserva.PagoParcial
                    || estadoNuevo == Reserva.EstadoReserva.Pagada
                    || estadoNuevo == Reserva.EstadoReserva.Cancelada) {
                    return;
                }
                break;
            case Confirmada:
                if (estadoNuevo == Reserva.EstadoReserva.PagoParcial
                    || estadoNuevo == Reserva.EstadoReserva.Pagada
                    || estadoNuevo == Reserva.EstadoReserva.Cancelada) {
                    return;
                }
                break;
            case PagoParcial:
                if (estadoNuevo == Reserva.EstadoReserva.Pagada
                    || estadoNuevo == Reserva.EstadoReserva.Cancelada
                    || estadoNuevo == Reserva.EstadoReserva.Confirmada) {
                    return;
                }
                break;
            case Pagada:
                if (estadoNuevo == Reserva.EstadoReserva.Completada
                    || estadoNuevo == Reserva.EstadoReserva.PagoParcial) {
                    return;
                }
                break;
            default:
                break;
        }

        throw new IllegalArgumentException("Cambio de estado no permitido de " + estadoActual + " a " + estadoNuevo);
    }

    public Reserva actualizarEstadoFinanciero(Reserva reserva, Reserva.EstadoReserva nuevoEstado) {
        validarPertenencia(reserva);
        if (reserva.getDeletedAt() != null) {
            throw new IllegalArgumentException("Reserva no encontrada");
        }
        validarCambioEstado(reserva.getEstado(), nuevoEstado);
        reserva.setEstado(nuevoEstado);
        return reservaRepository.save(reserva);
    }

    public Reserva actualizarEstadoFinanciero(Long reservaId, Reserva.EstadoReserva nuevoEstado) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        return actualizarEstadoFinanciero(reserva, nuevoEstado);
    }

    /**
     * Obtener estadísticas de reservas por empresa
     */
    @Transactional(readOnly = true)
    public Long countByEmpresaId(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        return reservaRepository.countByEmpresaId(empresaFiltrada);
    }

    /**
     * Obtener reservas próximas
     */
    @Transactional(readOnly = true)
    public List<Reserva> findReservasProximas(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        Date hoy = Date.valueOf(LocalDate.now());
        Date enUnaSemana = Date.valueOf(LocalDate.now().plusDays(7));
        return reservaRepository.findReservasProximas(empresaFiltrada, hoy, enUnaSemana);
    }
}