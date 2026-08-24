package com.emprendehub.repository;

import com.emprendehub.model.Ciudad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadRepository extends JpaRepository<Ciudad, Long> {

    Optional<Ciudad> findByNombre(String nombre);

    /**
     * La configuración usa {@code open-in-view: false}, así que la colección de
     * barrios tiene que venir resuelta desde la consulta. El {@code EntityGraph}
     * la trae en el mismo viaje y evita tanto el fallo de carga perezosa como el
     * problema N+1.
     */
    @EntityGraph(attributePaths = "barrios")
    List<Ciudad> findAllByOrderByNombreAsc();
}
