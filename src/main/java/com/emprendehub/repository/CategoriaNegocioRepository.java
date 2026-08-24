package com.emprendehub.repository;

import com.emprendehub.model.CategoriaNegocio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaNegocioRepository extends JpaRepository<CategoriaNegocio, Long> {

    Optional<CategoriaNegocio> findByNombre(String nombre);

    List<CategoriaNegocio> findAllByOrderByNombreAsc();
}
