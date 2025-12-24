package com.sistema.turistico.controller;

import com.sistema.turistico.dto.UsuarioRequest;
import com.sistema.turistico.dto.UsuarioResponse;
import com.sistema.turistico.entity.Usuario;
import com.sistema.turistico.service.UsuarioCrudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "Endpoints para gestión de usuarios")
public class UsuarioController {

    private final UsuarioCrudService usuarioService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario")
    public ResponseEntity<Map<String, Object>> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        try {
            log.info("Solicitando creación de usuario");
            Usuario usuario = usuarioService.crear(request);
            UsuarioResponse data = usuarioService.toResponse(usuario);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuario creado exitosamente");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("Error al crear usuario: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al crear usuario", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Listar usuarios", description = "Obtiene la lista de usuarios")
    public ResponseEntity<Map<String, Object>> listarUsuarios(
        @RequestParam(required = false) String busqueda,
        @RequestParam(required = false) Integer estado,
        @RequestParam(required = false) Long empresaId,
        @RequestParam(required = false) Long rolId) {
        try {
            log.info("Listando usuarios con filtros: busqueda={}, estado={}, empresaId={}, rolId={}",
                busqueda, estado, empresaId, rolId);
            List<Usuario> usuarios = usuarioService.listar(busqueda, estado, empresaId, rolId);
            List<UsuarioResponse> data = usuarios.stream()
                .map(usuarioService::toResponse)
                .toList();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Usuarios obtenidos exitosamente");
            response.put("data", data);
            response.put("total", data.size());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error al listar usuarios", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Obtener usuario", description = "Obtiene un usuario por su identificador")
    public ResponseEntity<Map<String, Object>> obtenerUsuario(@PathVariable Long id) {
        try {
            log.info("Obteniendo usuario {}", id);
            Usuario usuario = usuarioService.obtenerPorId(id);
            UsuarioResponse data = usuarioService.toResponse(usuario);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuario obtenido exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Usuario no encontrado: {}", ex.getMessage());
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error al obtener usuario {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario")
    public ResponseEntity<Map<String, Object>> actualizarUsuario(@PathVariable Long id,
                                                                 @Valid @RequestBody UsuarioRequest request) {
        try {
            log.info("Actualizando usuario {}", id);
            Usuario usuario = usuarioService.actualizar(id, request);
            UsuarioResponse data = usuarioService.toResponse(usuario);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuario actualizado exitosamente",
                "data", data
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al actualizar usuario: {}", ex.getMessage());
            if ("Usuario no encontrado".equalsIgnoreCase(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al actualizar usuario {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR','GERENTE')")
    @Operation(summary = "Eliminar usuario", description = "Realiza un borrado lógico de un usuario")
    public ResponseEntity<Map<String, Object>> eliminarUsuario(@PathVariable Long id) {
        try {
            log.info("Eliminando usuario {}", id);
            usuarioService.eliminar(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Usuario eliminado exitosamente"
            ));
        } catch (IllegalArgumentException ex) {
            log.warn("Error al eliminar usuario: {}", ex.getMessage());
            if ("Usuario no encontrado".equalsIgnoreCase(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Error interno al eliminar usuario {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Error interno del servidor"
            ));
        }
    }
}
