package com.sistema.turistico.service;

import com.sistema.turistico.dto.SucursalRequest;
import com.sistema.turistico.dto.SucursalResponse;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.SucursalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.sistema.turistico.security.TenantContext.hasRole;
import static com.sistema.turistico.security.TenantContext.requireEmpresaId;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;

    public Sucursal crear(SucursalRequest request) {
        log.info("Creando sucursal con nombre {} para empresa {}", request.getNombreSucursal(), request.getEmpresaId());

        boolean esSuperAdmin = hasRole("SUPERADMINISTRADOR");
        Long empresaId = request.getEmpresaId();
        if (!esSuperAdmin) {
            empresaId = requireEmpresaId();
            request.setEmpresaId(empresaId);
        } else if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para crear sucursales");
        }

        Empresa empresa = obtenerEmpresaActiva(empresaId);

        String nombre = request.getNombreSucursal().trim();
        if (sucursalRepository.existsActivaByNombreAndEmpresa(nombre, empresa.getIdEmpresa())) {
            throw new IllegalArgumentException("Ya existe una sucursal con este nombre para la empresa seleccionada");
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado de la sucursal debe ser 0 (Inactivo) o 1 (Activo)");
        }

        Sucursal sucursal = new Sucursal();
        sucursal.setNombreSucursal(nombre);
        sucursal.setUbicacion(request.getUbicacion().trim());
        sucursal.setDireccion(request.getDireccion() != null ? request.getDireccion().trim() : null);
        sucursal.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);
        sucursal.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        sucursal.setGerente(request.getGerente() != null ? request.getGerente().trim() : null);
        sucursal.setEstado(estado != null ? estado : 1);
        sucursal.setEmpresa(empresa);
        sucursal.setDeletedAt(null);

        Sucursal creada = sucursalRepository.save(sucursal);
        log.info("Sucursal creada exitosamente con ID {}", creada.getIdSucursal());
        return creada;
    }

    @Transactional(readOnly = true)
    public List<Sucursal> listar(String busqueda, Integer estado, Long empresaId) {
        String criterio = busqueda != null && !busqueda.trim().isEmpty() ? busqueda.trim() : null;
        boolean esSuperAdmin = hasRole("SUPERADMINISTRADOR");
        Long empresaFiltro = empresaId;
        if (!esSuperAdmin) {
            empresaFiltro = requireEmpresaId();
        }

        List<Sucursal> resultados;
        if (criterio == null && estado == null && empresaFiltro == null && esSuperAdmin) {
            resultados = sucursalRepository.findAllActivas();
        } else {
            resultados = sucursalRepository.findByFiltros(criterio, estado, empresaFiltro);
        }

        if (!esSuperAdmin) {
            Long empresaActual = requireEmpresaId();
            resultados = resultados.stream()
                .filter(sucursal -> sucursal.getEmpresa() != null && empresaActual.equals(sucursal.getEmpresa().getIdEmpresa()))
                .toList();
        }

        return resultados;
    }

    @Transactional(readOnly = true)
    public Sucursal obtenerPorId(Long id) {
        Sucursal sucursal = sucursalRepository.findActivaById(id)
            .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
        validarPertenencia(sucursal);
        return sucursal;
    }

    public Sucursal actualizar(Long id, SucursalRequest request) {
        log.info("Actualizando sucursal {}", id);

        Sucursal existente = obtenerPorId(id);
        boolean esSuperAdmin = hasRole("SUPERADMINISTRADOR");
        Long empresaId = request.getEmpresaId();
        if (!esSuperAdmin) {
            empresaId = requireEmpresaId();
            request.setEmpresaId(empresaId);
        } else if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para actualizar sucursales");
        }

        Empresa empresa = obtenerEmpresaActiva(empresaId);

        String nombre = request.getNombreSucursal().trim();
        if (!existente.getNombreSucursal().equalsIgnoreCase(nombre)
            || !existente.getEmpresa().getIdEmpresa().equals(empresa.getIdEmpresa())) {
            if (sucursalRepository.existsActivaByNombreAndEmpresaExcludingId(nombre, empresa.getIdEmpresa(), id)) {
                throw new IllegalArgumentException("Ya existe una sucursal con este nombre para la empresa seleccionada");
            }
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado de la sucursal debe ser 0 (Inactivo) o 1 (Activo)");
        }

        existente.setNombreSucursal(nombre);
        existente.setUbicacion(request.getUbicacion().trim());
        existente.setDireccion(request.getDireccion() != null ? request.getDireccion().trim() : null);
        existente.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);
        existente.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        existente.setGerente(request.getGerente() != null ? request.getGerente().trim() : null);
        if (estado != null) {
            existente.setEstado(estado);
        }
        existente.setEmpresa(empresa);

        Sucursal actualizada = sucursalRepository.save(existente);
        log.info("Sucursal actualizada exitosamente {}", actualizada.getIdSucursal());
        return actualizada;
    }

    public void eliminar(Long id) {
        log.info("Eliminando sucursal (soft delete) {}", id);
        Sucursal sucursal = obtenerPorId(id);
        sucursal.setEstado(0);
        sucursal.setDeletedAt(LocalDateTime.now());
        sucursalRepository.save(sucursal);
    }

    @Transactional(readOnly = true)
    public SucursalResponse toResponse(Sucursal sucursal) {
        return new SucursalResponse(
            sucursal.getIdSucursal(),
            sucursal.getNombreSucursal(),
            sucursal.getUbicacion(),
            sucursal.getDireccion(),
            sucursal.getTelefono(),
            sucursal.getEmail(),
            sucursal.getGerente(),
            sucursal.getEstado(),
            sucursal.getEmpresa() != null ? sucursal.getEmpresa().getIdEmpresa() : null,
            sucursal.getEmpresa() != null ? sucursal.getEmpresa().getNombreEmpresa() : null,
            sucursal.getCreatedAt() != null ? sucursal.getCreatedAt().toString() : null,
            sucursal.getUpdatedAt() != null ? sucursal.getUpdatedAt().toString() : null,
            sucursal.getDeletedAt() != null ? sucursal.getDeletedAt().toString() : null
        );
    }

    private Empresa obtenerEmpresaActiva(Long empresaId) {
        return empresaRepository.findActivaById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    private void validarPertenencia(Sucursal sucursal) {
        if (!hasRole("SUPERADMINISTRADOR")) {
            Long empresaActual = requireEmpresaId();
            Long empresaSucursal = sucursal.getEmpresa() != null ? sucursal.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaSucursal)) {
                throw new IllegalArgumentException("La sucursal no pertenece a la empresa actual");
            }
        }
    }
}
