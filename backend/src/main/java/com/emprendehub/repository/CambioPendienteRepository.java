package com.emprendehub.repository;

import com.emprendehub.model.CambioPendiente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CambioPendienteRepository extends JpaRepository<CambioPendiente, Long> {

    Optional<CambioPendiente> findByNegocioId(Long negocioId);

    @EntityGraph(attributePaths = "negocio")
    List<CambioPendiente> findAllByOrderByFechaSolicitudAsc();

    void deleteByNegocioId(Long negocioId);
}
