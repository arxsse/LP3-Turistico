package com.sistema.turistico.service;

import com.sistema.turistico.dto.EmpresaRequest;
import com.sistema.turistico.dto.EmpresaResponse;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public Empresa crear(EmpresaRequest request) {
        log.info("Creando nueva empresa con RUC {}", request.getRuc());

        String ruc = request.getRuc().trim();
        if (empresaRepository.existsByRuc(ruc)) {
            throw new IllegalArgumentException("Ya existe una empresa con este RUC");
        }

        String email = request.getEmail().trim();
        if (empresaRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una empresa con este email");
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado de la empresa debe ser 0 (Inactivo) o 1 (Activo)");
        }

        Empresa empresa = new Empresa();
        empresa.setNombreEmpresa(request.getNombreEmpresa().trim());
        empresa.setRuc(ruc);
        empresa.setEmail(email);
        empresa.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);
        empresa.setDireccion(request.getDireccion() != null ? request.getDireccion().trim() : null);
        empresa.setEstado(estado != null ? estado : 1);
        empresa.setFechaRegistro(LocalDateTime.now());
        empresa.setDeletedAt(null);

        Empresa creada = empresaRepository.save(empresa);
        log.info("Empresa creada exitosamente con ID {}", creada.getIdEmpresa());
        return creada;
    }

    @Transactional(readOnly = true)
    public List<Empresa> listar(String busqueda, Integer estado) {
        String criterio = busqueda != null && !busqueda.trim().isEmpty() ? busqueda.trim() : null;
        if (criterio == null && estado == null) {
            return empresaRepository.findAllActivas();
        }
        return empresaRepository.findByFiltros(criterio, estado);
    }

    @Transactional(readOnly = true)
    public Empresa obtenerPorId(Long id) {
        return empresaRepository.findActivaById(id)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    public Empresa actualizar(Long id, EmpresaRequest request) {
        log.info("Actualizando empresa {}", id);

        Empresa existente = obtenerPorId(id);

        String nuevoRuc = request.getRuc().trim();
        if (!existente.getRuc().equals(nuevoRuc) && empresaRepository.existsByRucAndIdEmpresaNot(nuevoRuc, id)) {
            throw new IllegalArgumentException("Ya existe una empresa con este RUC");
        }

        String nuevoEmail = request.getEmail().trim();
        if (!existente.getEmail().equalsIgnoreCase(nuevoEmail) && empresaRepository.existsByEmailAndIdEmpresaNot(nuevoEmail, id)) {
            throw new IllegalArgumentException("Ya existe una empresa con este email");
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado de la empresa debe ser 0 (Inactivo) o 1 (Activo)");
        }

        existente.setNombreEmpresa(request.getNombreEmpresa().trim());
        existente.setRuc(nuevoRuc);
        existente.setEmail(nuevoEmail);
        existente.setTelefono(request.getTelefono() != null ? request.getTelefono().trim() : null);
        existente.setDireccion(request.getDireccion() != null ? request.getDireccion().trim() : null);
        if (estado != null) {
            existente.setEstado(estado);
        }

        Empresa actualizada = empresaRepository.save(existente);
        log.info("Empresa actualizada exitosamente {}", actualizada.getIdEmpresa());
        return actualizada;
    }

    public void eliminar(Long id) {
        log.info("Eliminando empresa (soft delete) {}", id);
        Empresa empresa = obtenerPorId(id);
        empresa.setEstado(0);
        empresa.setDeletedAt(LocalDateTime.now());
        empresaRepository.save(empresa);
    }

    @Transactional(readOnly = true)
    public EmpresaResponse toResponse(Empresa empresa) {
        return new EmpresaResponse(
            empresa.getIdEmpresa(),
            empresa.getNombreEmpresa(),
            empresa.getRuc(),
            empresa.getEmail(),
            empresa.getTelefono(),
            empresa.getDireccion(),
            empresa.getEstado(),
            empresa.getFechaRegistro() != null ? empresa.getFechaRegistro().toString() : null,
            empresa.getCreatedAt() != null ? empresa.getCreatedAt().toString() : null,
            empresa.getUpdatedAt() != null ? empresa.getUpdatedAt().toString() : null,
            empresa.getDeletedAt() != null ? empresa.getDeletedAt().toString() : null
        );
    }
}
