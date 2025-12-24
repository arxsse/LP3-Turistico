package com.sistema.turistico.controller;

import com.sistema.turistico.dto.LoginRequest;
import com.sistema.turistico.dto.LoginResponse;
import com.sistema.turistico.dto.LogoutRequest;
import com.sistema.turistico.dto.RegisterRequest;
import com.sistema.turistico.entity.Permiso;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.repository.UsuarioRepository;
import com.sistema.turistico.security.AuthenticatedUser;
import com.sistema.turistico.service.UsuarioService;
import com.sistema.turistico.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para autenticación y gestión de usuarios")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Autenticar usuario
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            // Obtener usuario con permisos
            Usuario usuario = usuarioRepository.findByEmailWithRolAndEmpresa(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

            String token = ensurePersistentToken(usuario, roles);

            // Obtener lista de permisos desde el rol
            List<String> permisos = usuario.getRol().getPermisos().stream()
                .map(Permiso::getNombrePermiso)
                .collect(Collectors.toList());

            // Crear respuesta
            LoginResponse response = new LoginResponse(
                token,
                usuario.getIdUsuario(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol().getNombreRol(),
                usuario.getEmpresa().getIdEmpresa(),
                usuario.getEmpresa().getNombreEmpresa(),
                permisos
            );

            // Actualizar último login
            usuario.setUltimoLogin(java.time.LocalDateTime.now());
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en login para usuario: {}", loginRequest.getEmail(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario en el sistema con rol Superadministrador por defecto")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            log.info("Registrando usuario: {}", registerRequest.getEmail());

            // Registrar usuario
            usuarioService.registrarUsuario(registerRequest);

            // Obtener usuario con permisos para generar token
            Usuario usuario = usuarioRepository.findByEmailWithRolAndEmpresa(registerRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado después del registro"));

            List<String> roles = List.of(usuario.getRol().getNombreRol());
            List<String> permisos = usuario.getRol().getPermisos().stream()
                .map(Permiso::getNombrePermiso)
                .collect(Collectors.toList());

            String token = ensurePersistentToken(usuario, roles);

            usuario.setUltimoLogin(java.time.LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Crear respuesta
            LoginResponse response = new LoginResponse(
                token,
                usuario.getIdUsuario(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol().getNombreRol(),
                usuario.getEmpresa().getIdEmpresa(),
                usuario.getEmpresa().getNombreEmpresa(),
                permisos
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Error en registro para usuario: {} - {}", registerRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error inesperado en registro para usuario: {}", registerRequest.getEmail(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Invalida el token del usuario especificado mediante el body")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        try {
            Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if ("".equals(request.getToken())) {
                usuario.setToken(null);
                usuarioRepository.save(usuario);
                log.debug("Token invalidado para el usuario {}", usuario.getEmail());
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error en logout para usuario id: {}", request.getId(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    private String ensurePersistentToken(Usuario usuario, List<String> roles) {
        String currentToken = usuario.getToken();
        if (currentToken != null && jwtUtil.isTokenValidForUser(currentToken, usuario.getEmail())) {
            return currentToken;
        }

        String newToken = jwtUtil.generateToken(
            usuario.getEmail(),
            roles,
            usuario.getIdUsuario(),
            usuario.getEmpresa().getIdEmpresa()
        );

        usuario.setToken(newToken);
        return newToken;
    }

    private Optional<Usuario> resolveAuthenticatedUsuario(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return usuarioRepository.findById(authenticatedUser.getUserId())
                .or(() -> usuarioRepository.findByEmail(authentication.getName()));
        }

        String username = authentication.getName();
        if (username != null) {
            return usuarioRepository.findByEmail(username);
        }

        return Optional.empty();
    }
}
