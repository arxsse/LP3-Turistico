package com.sistema.turistico.service;

import com.sistema.turistico.entity.Permiso;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailWithRolAndEmpresa(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        // Verificar si el usuario está activo
        if (usuario.getEstado() != 1) {
            throw new UsernameNotFoundException("Usuario inactivo: " + email);
        }

        // Verificar si el usuario está bloqueado
        if (usuario.getBloqueadoHasta() != null &&
            usuario.getBloqueadoHasta().isAfter(java.time.LocalDateTime.now())) {
            throw new UsernameNotFoundException("Usuario bloqueado temporalmente: " + email);
        }

        // Obtener permisos del usuario
        List<GrantedAuthority> authorities = getAuthoritiesForUser(usuario);

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    private List<GrantedAuthority> getAuthoritiesForUser(Usuario usuario) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Agregar el rol como autoridad
        if (usuario.getRol() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombreRol().toUpperCase()));
        }

        // Agregar permisos específicos si están disponibles desde el rol
        if (usuario.getRol() != null && usuario.getRol().getPermisos() != null && !usuario.getRol().getPermisos().isEmpty()) {
            for (Permiso permiso : usuario.getRol().getPermisos()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + permiso.getNombrePermiso().toUpperCase()));
            }
        }

        return authorities;
    }
}