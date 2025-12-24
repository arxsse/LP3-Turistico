package com.sistema.turistico.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administración", description = "Endpoints para administración del sistema")
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    @Operation(summary = "Dashboard administrativo", description = "Panel de control para superadministradores")
    public ResponseEntity<Map<String, Object>> dashboard() {
        try {
            log.info("Accediendo al dashboard administrativo");

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("titulo", "Dashboard Administrativo");
            dashboard.put("descripcion", "Panel de control del sistema");
            dashboard.put("usuario", "Superadministrador");
            dashboard.put("modulos", new String[]{"Usuarios", "Empresas", "Reportes", "Configuración"});

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dashboard cargado exitosamente");
            response.put("data", dashboard);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error al cargar dashboard administrativo", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error interno del servidor");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
