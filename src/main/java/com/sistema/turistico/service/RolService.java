package com.sistema.turistico.service;

import com.sistema.turistico.dto.RolRequest;
import com.sistema.turistico.dto.RolResponse;
import com.sistema.turistico.entity.Rol;
import com.sistema.turistico.repository.RolRepository;
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
public class RolService {

    private final RolRepository rolRepository;

    public Rol crear(RolRequest request) {
        log.info("Creando rol con nombre {}", request.getNombreRol());

        String nombre = request.getNombreRol().trim();
        if (rolRepository.existsByNombreRol(nombre)) {
            throw new IllegalArgumentException("Ya existe un rol con este nombre");
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado del rol debe ser 0 (Inactivo) o 1 (Activo)");
        }

        Rol rol = new Rol();
        rol.setNombreRol(nombre);
        rol.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        rol.setEstado(estado != null ? estado : 1);
        rol.setDeletedAt(null);

        Rol creado = rolRepository.save(rol);
        log.info("Rol creado exitosamente con ID {}", creado.getIdRol());
        return creado;
    }

    @Transactional(readOnly = true)
    public List<Rol> listar(String busqueda, Integer estado) {
        String criterio = busqueda != null && !busqueda.trim().isEmpty() ? busqueda.trim() : null;
        if (criterio == null && estado == null) {
            return rolRepository.findAllActivos();
        }
        return rolRepository.findByFiltros(criterio, estado);
    }

    @Transactional(readOnly = true)
    public Rol obtenerPorId(Long id) {
        return rolRepository.findActivoById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));
    }

    public Rol actualizar(Long id, RolRequest request) {
        log.info("Actualizando rol {}", id);

        Rol existente = obtenerPorId(id);

        String nombre = request.getNombreRol().trim();
        if (!existente.getNombreRol().equalsIgnoreCase(nombre) && rolRepository.existsByNombreRolAndIdRolNot(nombre, id)) {
            throw new IllegalArgumentException("Ya existe un rol con este nombre");
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado del rol debe ser 0 (Inactivo) o 1 (Activo)");
        }

        existente.setNombreRol(nombre);
        existente.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);
        if (estado != null) {
            existente.setEstado(estado);
        }

        Rol actualizado = rolRepository.save(existente);
        log.info("Rol actualizado exitosamente {}", actualizado.getIdRol());
        return actualizado;
    }

    public void eliminar(Long id) {
        log.info("Eliminando rol (soft delete) {}", id);
        Rol rol = obtenerPorId(id);
        rol.setEstado(0);
        rol.setDeletedAt(LocalDateTime.now());
        rolRepository.save(rol);
    }

    @Transactional(readOnly = true)
    public RolResponse toResponse(Rol rol) {
        return new RolResponse(
            rol.getIdRol(),
            rol.getNombreRol(),
            rol.getDescripcion(),
            rol.getEstado(),
            rol.getCreatedAt() != null ? rol.getCreatedAt().toString() : null,
            rol.getUpdatedAt() != null ? rol.getUpdatedAt().toString() : null,
            rol.getDeletedAt() != null ? rol.getDeletedAt().toString() : null
        );
    }
}
