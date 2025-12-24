package com.sistema.turistico.config;

import com.sistema.turistico.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Qualifier("corsConfigurationSource")
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Endpoints públicos (sin context-path /api/v1)
                .requestMatchers("/auth/**", "/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Endpoints solo para superadministradores
                .requestMatchers("/admin/**").hasRole("SUPERADMINISTRADOR")
                // Endpoints solo para superadministradores
                .requestMatchers("/empresas/**").hasRole("SUPERADMINISTRADOR")
                // Endpoints para empleados, administradores, superadministradores y gerentes
                .requestMatchers("/reservas/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                .requestMatchers("/vouchers/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                .requestMatchers("/clientes/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                .requestMatchers("/servicios/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                .requestMatchers("/cajas/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                .requestMatchers("/pagos/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                .requestMatchers("/ventas/**").hasAnyRole("EMPLEADO", "ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                .requestMatchers("/reportes/finanzas/**").hasAnyRole("ADMINISTRADOR", "SUPERADMINISTRADOR", "GERENTE")
                // Todos los demás endpoints requieren autenticación
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}