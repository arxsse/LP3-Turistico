package com.sistema.turistico.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

/**
 * Principal enriquecido que mantiene el identificador del usuario y la empresa actual.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final Long empresaId;
    private final UserDetails delegate;

    private AuthenticatedUser(Long userId, Long empresaId, UserDetails delegate) {
        this.userId = userId;
        this.empresaId = empresaId;
        this.delegate = delegate;
    }

    public static AuthenticatedUser of(Long userId, Long empresaId, UserDetails delegate) {
        Objects.requireNonNull(delegate, "delegate must not be null");
        return new AuthenticatedUser(userId, empresaId, delegate);
    }

    public Long getUserId() {
        return userId;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return delegate.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return delegate.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return delegate.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }
}
