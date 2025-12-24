package com.sistema.turistico.repository;

import com.sistema.turistico.entity.CategoriaServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CategoriaServicioRepository extends JpaRepository<CategoriaServicio, Long> {

    List<CategoriaServicio> findByIdCategoriaIn(Collection<Long> ids);
}
