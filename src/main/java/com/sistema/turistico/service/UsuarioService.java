package com.sistema.turistico.service;

import com.sistema.turistico.dto.RegisterRequest;
import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.Rol;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.RolRepository;
import com.sistema.turistico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrarUsuario(RegisterRequest request) {
        log.info("Registrando nuevo usuario: {}", request.getEmail());

        // Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro con email ya existente: {}", request.getEmail());
            throw new IllegalArgumentException("Ya existe un usuario con este email");
        }

        // Validar que el DNI no exista (si se proporciona)
        if (request.getDni() != null && usuarioRepository.existsByDni(request.getDni())) {
            log.warn("Intento de registro con DNI ya existente: {}", request.getDni());
            throw new IllegalArgumentException("Ya existe un usuario con este DNI");
        }

        // Obtener rol por defecto (Superadministrador)
        Rol rol = rolRepository.findByNombreRol("Superadministrador")
            .orElseThrow(() -> new IllegalArgumentException("Rol por defecto 'Superadministrador' no encontrado"));

        // Obtener empresa por defecto (ID 1 - Empresa Demo Turística)
        Empresa empresa = empresaRepository.findById(1L)
            .orElseThrow(() -> new IllegalArgumentException("Empresa por defecto (ID: 1) no encontrada"));

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setDni(request.getDni());
        usuario.setEstado(1); // Activo
        usuario.setEmpresa(empresa);
        usuario.setRol(rol);

        try {
            Usuario usuarioGuardado = usuarioRepository.save(usuario);
            log.info("Usuario registrado exitosamente con ID: {}", usuarioGuardado.getIdUsuario());
            return usuarioGuardado;
        } catch (Exception e) {
            log.error("Error al guardar usuario en base de datos: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Error interno al registrar usuario");
        }
    }
}