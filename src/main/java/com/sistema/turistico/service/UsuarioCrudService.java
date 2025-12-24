package com.sistema.turistico.service;

import com.sistema.turistico.dto.UsuarioRequest;
import com.sistema.turistico.dto.UsuarioResponse;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.Rol;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.RolRepository;
import com.sistema.turistico.repository.SucursalRepository;
import com.sistema.turistico.repository.UsuarioRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UsuarioCrudService {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final RolRepository rolRepository;
    private final SucursalRepository sucursalRepository;
    private final PasswordEncoder passwordEncoder;

    public Usuario crear(UsuarioRequest request) {
        log.info("Creando usuario con email {}", request.getEmail());

        String email = request.getEmail().trim();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con este email");
        }

        String dni = request.getDni() != null ? request.getDni().trim() : null;
        if (dni != null && !dni.isEmpty() && usuarioRepository.existsByDni(dni)) {
            throw new IllegalArgumentException("Ya existe un usuario con este DNI");
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado del usuario debe ser 0 (Inactivo) o 1 (Activo)");
        }

        boolean esSuperAdmin = TenantContext.hasRole("SUPERADMINISTRADOR");
        Long empresaId = request.getEmpresaId();
        if (!esSuperAdmin) {
            empresaId = TenantContext.requireEmpresaId();
            request.setEmpresaId(empresaId);
        } else if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para crear usuarios");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

        Rol rol = rolRepository.findActivoById(request.getRolId())
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));

        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursal = obtenerSucursal(request.getSucursalId());
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        String password = request.getPassword().trim();

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre().trim());
        usuario.setApellido(request.getApellido().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setDni(dni);
        usuario.setEstado(estado != null ? estado : 1);
        usuario.setEmpresa(empresa);
        usuario.setRol(rol);
        usuario.setSucursal(sucursal);
        usuario.setDeletedAt(null);

        Usuario creado = usuarioRepository.save(usuario);
        log.info("Usuario creado exitosamente con ID {}", creado.getIdUsuario());
        return creado;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar(String busqueda, Integer estado, Long empresaId, Long rolId) {
        String criterio = busqueda != null && !busqueda.trim().isEmpty() ? busqueda.trim() : null;
        boolean esSuperAdmin = TenantContext.hasRole("SUPERADMINISTRADOR");
        Long empresaFiltro = empresaId;
        if (!esSuperAdmin) {
            empresaFiltro = TenantContext.requireEmpresaId();
        }

        List<Usuario> resultados;
        if (criterio == null && estado == null && empresaFiltro == null && rolId == null && esSuperAdmin) {
            resultados = usuarioRepository.findAllActivos();
        } else {
            resultados = usuarioRepository.findByFiltros(criterio, estado, empresaFiltro, rolId);
        }

        if (!esSuperAdmin) {
            Long empresaActual = TenantContext.requireEmpresaId();
            resultados = resultados.stream()
                .filter(usuario -> usuario.getEmpresa() != null && empresaActual.equals(usuario.getEmpresa().getIdEmpresa()))
                .toList();
        }
        return resultados;
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findActivoById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        validarPertenencia(usuario);
        return usuario;
    }

    public Usuario actualizar(Long id, UsuarioRequest request) {
        log.info("Actualizando usuario {}", id);

        Usuario existente = obtenerPorId(id);

        String email = request.getEmail().trim();
        if (!existente.getEmail().equalsIgnoreCase(email) && usuarioRepository.existsByEmailAndIdUsuarioNot(email, id)) {
            throw new IllegalArgumentException("Ya existe un usuario con este email");
        }

        String dni = request.getDni() != null ? request.getDni().trim() : null;
        if (dni != null && !dni.isEmpty() && usuarioRepository.existsByDniAndIdUsuarioNot(dni, id)) {
            throw new IllegalArgumentException("Ya existe un usuario con este DNI");
        }

        Integer estado = request.getEstado();
        if (estado != null && estado != 0 && estado != 1) {
            throw new IllegalArgumentException("El estado del usuario debe ser 0 (Inactivo) o 1 (Activo)");
        }

        boolean esSuperAdmin = TenantContext.hasRole("SUPERADMINISTRADOR");
        Long empresaId = request.getEmpresaId();
        if (!esSuperAdmin) {
            empresaId = TenantContext.requireEmpresaId();
            request.setEmpresaId(empresaId);
        } else if (empresaId == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para actualizar usuarios");
        }

        Empresa empresa = empresaRepository.findById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

        Rol rol = rolRepository.findActivoById(request.getRolId())
            .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));

        Sucursal sucursal = null;
        if (request.getSucursalId() != null) {
            sucursal = obtenerSucursal(request.getSucursalId());
        }

        existente.setNombre(request.getNombre().trim());
        existente.setApellido(request.getApellido().trim());
        existente.setEmail(email);
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            existente.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }
        existente.setDni(dni);
        if (estado != null) {
            existente.setEstado(estado);
        }
        existente.setEmpresa(empresa);
        existente.setRol(rol);
        existente.setSucursal(sucursal);

        Usuario actualizado = usuarioRepository.save(existente);
        log.info("Usuario actualizado exitosamente {}", actualizado.getIdUsuario());
        return actualizado;
    }

    public void eliminar(Long id) {
        log.info("Eliminando usuario (soft delete) {}", id);
        Usuario usuario = obtenerPorId(id);
        usuario.setEstado(0);
        usuario.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getIdUsuario(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getEmail(),
            usuario.getDni(),
            usuario.getEstado(),
            usuario.getEmpresa() != null ? usuario.getEmpresa().getIdEmpresa() : null,
            usuario.getEmpresa() != null ? usuario.getEmpresa().getNombreEmpresa() : null,
            usuario.getRol() != null ? usuario.getRol().getIdRol() : null,
            usuario.getRol() != null ? usuario.getRol().getNombreRol() : null,
            usuario.getSucursal() != null ? usuario.getSucursal().getIdSucursal() : null,
            usuario.getCreatedAt() != null ? usuario.getCreatedAt().toString() : null,
            usuario.getUpdatedAt() != null ? usuario.getUpdatedAt().toString() : null,
            usuario.getDeletedAt() != null ? usuario.getDeletedAt().toString() : null
        );
    }

    private Sucursal obtenerSucursal(Long sucursalId) {
        Sucursal sucursal = sucursalRepository.findActivaById(sucursalId)
            .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
        if (!TenantContext.hasRole("SUPERADMINISTRADOR")) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaSucursal = sucursal.getEmpresa() != null ? sucursal.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaSucursal)) {
                throw new IllegalArgumentException("La sucursal no pertenece a la empresa actual");
            }
        }
        return sucursal;
    }

    private void validarPertenencia(Usuario usuario) {
        if (!TenantContext.hasRole("SUPERADMINISTRADOR")) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaUsuario = usuario.getEmpresa() != null ? usuario.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaUsuario)) {
                throw new IllegalArgumentException("El usuario no pertenece a la empresa actual");
            }
        }
    }
}
