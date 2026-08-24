package com.emprendehub.repository;

import com.emprendehub.model.Barrio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BarrioRepository extends JpaRepository<Barrio, Long> {

    /** Con la ciudad resuelta: el servicio comprueba que el barrio le pertenece. */
    @EntityGraph(attributePaths = "ciudad")
    Optional<Barrio> findById(Long id);
}
