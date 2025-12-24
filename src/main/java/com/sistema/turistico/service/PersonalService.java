package com.sistema.turistico.service;

import com.sistema.turistico.entity.Empresa;
import com.sistema.turistico.entity.Personal;
import com.sistema.turistico.entity.Sucursal;
import com.sistema.turistico.repository.EmpresaRepository;
import com.sistema.turistico.repository.PersonalRepository;
import com.sistema.turistico.repository.SucursalRepository;
import com.sistema.turistico.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalService {

    private final PersonalRepository personalRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalRepository sucursalRepository;

    @Transactional
    public Personal crearPersonal(Personal personal) {
        log.info("Creando nuevo personal: {} {}", personal != null ? personal.getNombre() : null, personal != null ? personal.getApellido() : null);

        if (personal == null) {
            throw new IllegalArgumentException("El personal es obligatorio");
        }

        Long empresaSolicitada = personal.getEmpresa() != null ? personal.getEmpresa().getIdEmpresa() : null;
        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(empresaSolicitada);
        Empresa empresa = obtenerEmpresaActiva(empresaId);

        normalizarPersonal(personal);
        validarIdentificadores(personal.getDni(), personal.getEmail(), empresaId, null);

        Sucursal sucursal = null;
        if (personal.getSucursal() != null && personal.getSucursal().getIdSucursal() != null) {
            sucursal = obtenerSucursalAutorizada(personal.getSucursal().getIdSucursal(), empresaId);
        }

        personal.setEmpresa(empresa);
        personal.setSucursal(sucursal);
        personal.setEstado(personal.getEstado() != null ? personal.getEstado() : Boolean.TRUE);
        personal.setDeletedAt(null);

        Personal creado = personalRepository.save(personal);
        log.info("Personal creado exitosamente con ID: {}", creado.getIdPersonal());
        return creado;
    }

    @Transactional(readOnly = true)
    public Optional<Personal> obtenerPersonalPorId(Long id) {
        return personalRepository.findById(id)
            .filter(personal -> personal.getDeletedAt() == null)
            .map(personal -> {
                validarPertenencia(personal);
                return personal;
            });
    }

    @Transactional(readOnly = true)
    public List<Personal> listarPersonalPorEmpresa(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "La empresa es obligatoria para listar personal");
        if (empresaFiltrada == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para listar personal");
        }
        return personalRepository.findByEmpresaId(empresaFiltrada).stream()
            .filter(personal -> personal.getDeletedAt() == null)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<Personal> listarPersonalConFiltros(Long empresaId, Long sucursalId, Personal.Cargo cargo,
                                                   Boolean estado, String busqueda) {
        boolean superAdmin = TenantContext.isSuperAdmin();
        Long empresaFiltrada = TenantContext.resolveEmpresaId(empresaId);

        Long sucursalFiltrada = sucursalId;
        if (sucursalId != null) {
            if (superAdmin) {
                obtenerSucursalAutorizada(sucursalId, empresaFiltrada);
            } else {
                obtenerSucursalAutorizada(sucursalId, TenantContext.requireEmpresaId());
            }
        }

        return personalRepository.findWithFilters(empresaFiltrada, sucursalFiltrada, cargo, estado, normalizarBusqueda(busqueda)).stream()
            .filter(personal -> personal.getDeletedAt() == null)
            .filter(personal -> superAdmin || perteneceAEmpresaActual(personal))
            .toList();
    }

    @Transactional
    public Personal actualizarPersonal(Long id, Personal personalActualizado) {
        log.info("Actualizando personal ID: {}", id);

        if (personalActualizado == null) {
            throw new IllegalArgumentException("El personal es obligatorio");
        }

        Personal personalExistente = obtenerPersonalAutorizado(id);

        Long empresaSolicitada = personalActualizado.getEmpresa() != null ? personalActualizado.getEmpresa().getIdEmpresa() : personalExistente.getEmpresa().getIdEmpresa();
        Long empresaId = TenantContext.requireEmpresaIdOrCurrent(empresaSolicitada);
        Empresa empresa = obtenerEmpresaActiva(empresaId);

        normalizarPersonal(personalActualizado);
        validarIdentificadores(personalActualizado.getDni(), personalActualizado.getEmail(), empresaId, id);

        Sucursal sucursal = null;
        if (personalActualizado.getSucursal() != null && personalActualizado.getSucursal().getIdSucursal() != null) {
            sucursal = obtenerSucursalAutorizada(personalActualizado.getSucursal().getIdSucursal(), empresaId);
        }

        personalExistente.setNombre(personalActualizado.getNombre());
        personalExistente.setApellido(personalActualizado.getApellido());
        personalExistente.setDni(personalActualizado.getDni());
        personalExistente.setFechaNacimiento(personalActualizado.getFechaNacimiento());
        personalExistente.setTelefono(personalActualizado.getTelefono());
        personalExistente.setEmail(personalActualizado.getEmail());
        personalExistente.setDireccion(personalActualizado.getDireccion());
        personalExistente.setCargo(personalActualizado.getCargo());
        personalExistente.setFechaIngreso(personalActualizado.getFechaIngreso());
        personalExistente.setTurno(personalActualizado.getTurno());
        personalExistente.setSueldo(personalActualizado.getSueldo());
        personalExistente.setFoto(personalActualizado.getFoto());
        if (personalActualizado.getEstado() != null) {
            personalExistente.setEstado(personalActualizado.getEstado());
        }
        personalExistente.setEmpresa(empresa);
        personalExistente.setSucursal(sucursal);

        Personal actualizado = personalRepository.save(personalExistente);
        log.info("Personal actualizado exitosamente: {}", actualizado.getIdPersonal());
        return actualizado;
    }

    @Transactional
    public void eliminarPersonal(Long id) {
        log.info("Desactivando personal ID: {}", id);

        Personal personal = obtenerPersonalAutorizado(id);
        if (personal.getDeletedAt() != null) {
            throw new IllegalArgumentException("El personal ya fue eliminado");
        }

        personal.setEstado(false);
        personal.setDeletedAt(new Date());
        personalRepository.save(personal);
        log.info("Personal desactivado exitosamente: {}", id);
    }

    @Transactional(readOnly = true)
    public List<Personal> obtenerPersonalPorCargo(Long empresaId, Personal.Cargo cargo) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "La empresa es obligatoria para listar por cargo");
        if (empresaFiltrada == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para listar por cargo");
        }
        return personalRepository.findByCargoAndEmpresaId(cargo, empresaFiltrada).stream()
            .filter(personal -> personal.getDeletedAt() == null)
            .toList();
    }

    @Transactional(readOnly = true)
    public Long contarPersonalActivo(Long empresaId) {
        Long empresaFiltrada = TenantContext.requireEmpresaIdOrCurrent(empresaId, "La empresa es obligatoria para contar personal");
        if (empresaFiltrada == null) {
            throw new IllegalArgumentException("La empresa es obligatoria para contar personal");
        }
        return personalRepository.countActivoByEmpresaId(empresaFiltrada);
    }

    private void normalizarPersonal(Personal personal) {
        if (personal.getNombre() != null) {
            personal.setNombre(personal.getNombre().trim());
        }
        if (personal.getApellido() != null) {
            personal.setApellido(personal.getApellido().trim());
        }
        if (personal.getDni() != null) {
            personal.setDni(personal.getDni().trim());
        }
        if (personal.getTelefono() != null) {
            personal.setTelefono(personal.getTelefono().trim());
        }
        if (personal.getEmail() != null && !personal.getEmail().isBlank()) {
            personal.setEmail(personal.getEmail().trim().toLowerCase());
        } else {
            personal.setEmail(null);
        }
        if (personal.getDireccion() != null) {
            personal.setDireccion(personal.getDireccion().trim());
        }
    }

    private void validarIdentificadores(String dni, String email, Long empresaId, Long personalId) {
        if (dni != null && !dni.isBlank()) {
            boolean existeDni = personalId == null
                ? personalRepository.findByDniAndEmpresaId(dni, empresaId).isPresent()
                : personalRepository.existsByDniAndEmpresaIdAndIdNot(dni, empresaId, personalId);
            if (existeDni) {
                throw new IllegalArgumentException("Ya existe personal con este DNI en la empresa");
            }
        }

        if (email != null && !email.isBlank()) {
            boolean existeEmail = personalId == null
                ? personalRepository.findByEmailAndEmpresaId(email, empresaId).isPresent()
                : personalRepository.existsByEmailAndEmpresaIdAndIdNot(email, empresaId, personalId);
            if (existeEmail) {
                throw new IllegalArgumentException("Ya existe personal con este email en la empresa");
            }
        }
    }

    private Personal obtenerPersonalAutorizado(Long id) {
        Personal personal = personalRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado"));
        if (personal.getDeletedAt() != null) {
            throw new IllegalArgumentException("Personal no encontrado");
        }
        validarPertenencia(personal);
        return personal;
    }

    private Sucursal obtenerSucursalAutorizada(Long sucursalId, Long empresaId) {
        Sucursal sucursal = sucursalRepository.findActivaById(sucursalId)
            .orElseThrow(() -> new IllegalArgumentException("Sucursal no encontrada"));
        if (empresaId != null) {
            Long empresaSucursal = sucursal.getEmpresa() != null ? sucursal.getEmpresa().getIdEmpresa() : null;
            if (!empresaId.equals(empresaSucursal)) {
                throw new IllegalArgumentException("La sucursal no pertenece a la empresa actual");
            }
        } else if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaSucursal = sucursal.getEmpresa() != null ? sucursal.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaSucursal)) {
                throw new IllegalArgumentException("La sucursal no pertenece a la empresa actual");
            }
        }
        return sucursal;
    }

    private boolean perteneceAEmpresaActual(Personal personal) {
        Long empresaActual = TenantContext.requireEmpresaId();
        Long empresaPersonal = personal.getEmpresa() != null ? personal.getEmpresa().getIdEmpresa() : null;
        return empresaActual.equals(empresaPersonal);
    }

    private boolean esSuperAdmin() {
        return TenantContext.isSuperAdmin();
    }

    private Empresa obtenerEmpresaActiva(Long empresaId) {
        return empresaRepository.findActivaById(empresaId)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }

    private void validarPertenencia(Personal personal) {
        if (!esSuperAdmin()) {
            Long empresaActual = TenantContext.requireEmpresaId();
            Long empresaPersonal = personal.getEmpresa() != null ? personal.getEmpresa().getIdEmpresa() : null;
            if (!empresaActual.equals(empresaPersonal)) {
                throw new IllegalArgumentException("El personal no pertenece a la empresa actual");
            }
        }
    }

    private String normalizarBusqueda(String busqueda) {
        return busqueda != null && !busqueda.trim().isEmpty() ? busqueda.trim() : null;
    }
}