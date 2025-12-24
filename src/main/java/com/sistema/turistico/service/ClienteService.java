package com.sistema.turistico.service;

import com.sistema.turistico.dto.ClienteResponse;
import com.sistema.turistico.entity.Cliente;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.repository.ClienteRepository;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.SucursalRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;

    /**
     * Crear un nuevo cliente
     */
    public Cliente create(Cliente cliente) {
        log.info("Creando nuevo cliente: {} {}", cliente != null ? cliente.getNombre() : null, cliente != null ? cliente.getApellido() : null);

        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }

        Long empresaSolicitada = cliente.getEmpresa() != null ? cliente.getEmpresa().getIdEmpresa() : null;
        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(empresaSolicitada);

        normalizarCliente(cliente);

        if (cliente.getEmail() != null && clienteRepository.existsByEmpresaIdAndEmailAndIdNot(empresaId, cliente.getEmail(), 0L)) {
            throw new IllegalArgumentException("Ya existe un cliente con este email en la empresa");
        }

        if (cliente.getDni() != null && clienteRepository.existsByEmpresaIdAndDniAndIdNot(empresaId, cliente.getDni(), 0L)) {
            throw new IllegalArgumentException("Ya existe un cliente con este DNI en la empresa");
        }

        cliente.setEmpresa(obtenerEmpresaActiva(empresaId));
        
        // Manejar sucursal si está presente
        if (cliente.getSucursal() != null && cliente.getSucursal().getIdSucursal() != null) {
            Sucursal sucursal = obtenerSucursalActiva(cliente.getSucursal().getIdSucursal(), empresaId);
            cliente.setSucursal(sucursal);
        } else {
            cliente.setSucursal(null);
        }
        
        if (cliente.getNivelMembresia() == null) {
            cliente.setNivelMembresia(Cliente.NivelMembresia.Bronce);
        }
        if (cliente.getPuntosFidelizacion() == null) {
            cliente.setPuntosFidelizacion(0);
        }
        cliente.setEstado(Boolean.TRUE);
        cliente.setDeletedAt(null);

        aplicarEncriptacion(cliente);

        Cliente savedCliente = clienteRepository.save(cliente);
        log.info("Cliente creado exitosamente con ID: {}", savedCliente.getIdCliente());
        return savedCliente;
    }

    /**
     * Buscar cliente por ID
     */
    @Transactional(readOnly = true)
    public Optional<Cliente> findById(Long id) {
        log.debug("Buscando cliente con ID: {}", id);
        return clienteRepository.findById(id)
            .filter(cliente -> cliente.getDeletedAt() == null)
            .map(cliente -> {
                validarPertenencia(cliente);
                return cliente;
            });
    }

    /**
     * Listar clientes por empresa
     */
    @Transactional(readOnly = true)
    public List<Cliente> findByEmpresaId(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Listando clientes de empresa ID: {}", empresaFiltrada);
        return clienteRepository.findByEmpresaId(empresaFiltrada);
    }

    /**
     * Listar clientes por empresa con búsqueda
     */
    @Transactional(readOnly = true)
    public List<Cliente> findByEmpresaIdAndBusqueda(Long empresaId, String busqueda) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        log.debug("Buscando clientes en empresa {} con término: {}", empresaFiltrada, busqueda);
        if (busqueda == null || busqueda.trim().isEmpty()) {
            return clienteRepository.findByEmpresaIdOrderByCreatedAtDesc(empresaFiltrada);
        }
        return clienteRepository.findByEmpresaIdAndBusqueda(empresaFiltrada, busqueda.trim());
    }

    /**
     * Actualizar cliente
     */
    public Cliente update(Long id, Cliente clienteActualizado) {
        log.info("Actualizando cliente ID: {}", id);

        if (clienteActualizado == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }

        Cliente clienteExistente = obtenerClienteAutorizado(id);

        normalizarCliente(clienteActualizado);

        if (clienteActualizado.getEmail() != null && clienteRepository.existsByEmpresaIdAndEmailAndIdNot(clienteExistente.getEmpresa().getIdEmpresa(), clienteActualizado.getEmail(), id)) {
            throw new IllegalArgumentException("Ya existe un cliente con este email en la empresa");
        }

        if (clienteActualizado.getDni() != null && clienteRepository.existsByEmpresaIdAndDniAndIdNot(clienteExistente.getEmpresa().getIdEmpresa(), clienteActualizado.getDni(), id)) {
            throw new IllegalArgumentException("Ya existe un cliente con este DNI en la empresa");
        }

        clienteExistente.setNombre(clienteActualizado.getNombre());
        clienteExistente.setApellido(clienteActualizado.getApellido());
        clienteExistente.setEmail(clienteActualizado.getEmail());
        clienteExistente.setTelefono(clienteActualizado.getTelefono());
        clienteExistente.setDni(clienteActualizado.getDni());
        clienteExistente.setFechaNacimiento(clienteActualizado.getFechaNacimiento());
        clienteExistente.setNacionalidad(clienteActualizado.getNacionalidad());
        clienteExistente.setPreferenciasViaje(clienteActualizado.getPreferenciasViaje());
        if (clienteActualizado.getNivelMembresia() != null) {
            clienteExistente.setNivelMembresia(clienteActualizado.getNivelMembresia());
        }
        
        // Manejar sucursal si está presente
        if (clienteActualizado.getSucursal() != null && clienteActualizado.getSucursal().getIdSucursal() != null) {
            Sucursal sucursal = obtenerSucursalActiva(clienteActualizado.getSucursal().getIdSucursal(), clienteExistente.getEmpresa().getIdEmpresa());
            clienteExistente.setSucursal(sucursal);
        } else {
            clienteExistente.setSucursal(null);
        }

        aplicarEncriptacion(clienteExistente);

        Cliente updatedCliente = clienteRepository.save(clienteExistente);
        log.info("Cliente actualizado exitosamente: {}", updatedCliente.getIdCliente());
        return updatedCliente;
    }

    /**
     * Eliminar cliente (soft delete)
     */
    public void delete(Long id) {
        log.info("Eliminando cliente ID: {} (soft delete)", id);

        Cliente cliente = obtenerClienteAutorizado(id);

        if (cliente.getDeletedAt() != null) {
            throw new IllegalArgumentException("El cliente ya fue eliminado");
        }

        cliente.setEstado(false);
        cliente.setDeletedAt(LocalDateTime.now());
        clienteRepository.save(cliente);

        log.info("Cliente eliminado exitosamente: {}", id);
    }

    /**
     * Verificar si existe cliente por email en empresa
     */
    @Transactional(readOnly = true)
    public boolean existsByEmpresaAndEmail(Long empresaId, String email) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        String emailNormalizado = email != null ? email.trim().toLowerCase() : null;
        return emailNormalizado != null && clienteRepository.existsByEmpresaIdAndEmailAndIdNot(empresaFiltrada, emailNormalizado, 0L);
    }

    /**
     * Verificar si existe cliente por DNI en empresa
     */
    @Transactional(readOnly = true)
    public boolean existsByEmpresaAndDni(Long empresaId, String dni) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        String dniNormalizado = dni != null ? dni.trim() : null;
        return dniNormalizado != null && clienteRepository.existsByEmpresaIdAndDniAndIdNot(empresaFiltrada, dniNormalizado, 0L);
    }

    /**
     * Obtener estadísticas de clientes por empresa
     */
    @Transactional(readOnly = true)
    public Long countByEmpresaId(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId);
        return clienteRepository.countByEmpresaId(empresaFiltrada);
    }

    /**
     * Convertir Cliente a ClienteResponse
     */
    public ClienteResponse toResponse(Cliente cliente) {
        ClienteResponse response = new ClienteResponse();
        response.setIdCliente(cliente.getIdCliente());
        response.setNombre(cliente.getNombre());
        response.setApellido(cliente.getApellido());
        response.setEmail(cliente.getEmail());
        response.setTelefono(cliente.getTelefono());
        response.setDni(cliente.getDni());
        response.setFechaNacimiento(cliente.getFechaNacimiento() != null ? cliente.getFechaNacimiento().toLocalDate() : null);
        response.setNacionalidad(cliente.getNacionalidad());
        response.setPreferenciasViaje(cliente.getPreferenciasViaje());
        response.setEstado(cliente.getEstado());
        response.setIdSucursal(cliente.getSucursal() != null ? cliente.getSucursal().getIdSucursal() : null);
        response.setNombreSucursal(cliente.getSucursal() != null ? cliente.getSucursal().getNombreSucursal() : null);
        response.setCreatedAt(cliente.getCreatedAt() != null ? cliente.getCreatedAt().toString() : null);
        response.setUpdatedAt(cliente.getUpdatedAt() != null ? cliente.getUpdatedAt().toString() : null);
        return response;
    }

    private void normalizarCliente(Cliente cliente) {
        if (cliente.getNombre() != null) {
            cliente.setNombre(cliente.getNombre().trim());
        }
        if (cliente.getApellido() != null) {
            cliente.setApellido(cliente.getApellido().trim());
        }
        if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
            cliente.setEmail(cliente.getEmail().trim().toLowerCase());
        } else {
            cliente.setEmail(null);
        }
        if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()) {
            cliente.setTelefono(cliente.getTelefono().trim());
        } else {
            cliente.setTelefono(null);
        }
        if (cliente.getDni() != null && !cliente.getDni().isBlank()) {
            cliente.setDni(cliente.getDni().trim());
        } else {
            cliente.setDni(null);
        }
        if (cliente.getNacionalidad() != null) {
            cliente.setNacionalidad(cliente.getNacionalidad().trim());
        }
        if (cliente.getPreferenciasViaje() != null) {
            cliente.setPreferenciasViaje(cliente.getPreferenciasViaje().trim());
        }
    }

    private void aplicarEncriptacion(Cliente cliente) {
        if (cliente.getEmail() != null) {
            cliente.setEmailEncriptado(cliente.getEmail().getBytes(StandardCharsets.UTF_8));
        } else {
            cliente.setEmailEncriptado(null);
        }
        if (cliente.getTelefono() != null) {
            cliente.setTelefonoEncriptado(cliente.getTelefono().getBytes(StandardCharsets.UTF_8));
        } else {
            cliente.setTelefonoEncriptado(null);
        }
    }

    private Cliente obtenerClienteAutorizado(Long id) {
        Cliente cliente = clienteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (cliente.getDeletedAt() != null) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        validarPertenencia(cliente);
        return cliente;
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
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

    private void validarPertenencia(Cliente cliente) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaCliente = cliente.getEmpresa() != null ? cliente.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaCliente)) {
                throw new IllegalArgumentException("El cliente no pertenece a la empresa actual");
            }
        }
    }
}