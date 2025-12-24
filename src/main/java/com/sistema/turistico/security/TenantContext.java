package com.sistema.turistico.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utilidad centralizada para obtener el tenant (empresa) del usuario autenticado.
 */
public final class TenantContext {

    private TenantContext() {
    }

    public static Optional<AuthenticatedUser> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser);
        }
        return Optional.empty();
    }

    public static Optional<Long> getEmpresaId() {
        return getAuthenticatedUser().map(AuthenticatedUser::getEmpresaId);
    }

    public static Optional<Long> getUserId() {
        return getAuthenticatedUser().map(AuthenticatedUser::getUserId);
    }

    public static Long requireEmpresaId() {
        return getEmpresaId()
            .orElseThrow(() -> new IllegalStateException("No se pudo determinar la empresa del usuario autenticado"));
    }

    public static Long requireUserId() {
        return getUserId()
            .orElseThrow(() -> new IllegalStateException("No se pudo determinar el usuario autenticado"));
    }

    public static boolean hasAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth != null && auth.equalsIgnoreCase(authority));
    }

    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String formatted = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
        return hasAuthority(formatted);
    }

    public static boolean isSuperAdmin() {
        return hasRole("SUPERADMINISTRADOR");
    }

    public static Long resolveEmpresaId(Long explicitEmpresaId) {
        if (isSuperAdmin()) {
            return explicitEmpresaId;
        }
        Long currentEmpresa = requireEmpresaId();
        if (explicitEmpresaId != null && !explicitEmpresaId.equals(currentEmpresa)) {
            throw new IllegalArgumentException("No tiene acceso a la empresa solicitada");
        }
        return currentEmpresa;
    }

    public static Long requireEmpresaIdOrCurrent(Long explicitEmpresaId) {
        return requireEmpresaIdOrCurrent(explicitEmpresaId, "La empresa es obligatoria para esta operación");
    }

    public static Long requireEmpresaIdOrCurrent(Long explicitEmpresaId, String message) {
        Long resolved = resolveEmpresaId(explicitEmpresaId);
        if (resolved == null) {
            throw new IllegalArgumentException(message != null ? message : "La empresa es obligatoria para esta operación");
        }
        return resolved;
    }
}
