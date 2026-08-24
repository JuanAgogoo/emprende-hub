package com.emprendehub.repository;

import com.emprendehub.model.Negocio;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    /**
     * El negocio de una cuenta. Trae resueltas las relaciones porque la
     * configuración usa {@code open-in-view: false} y el servicio mapea a DTO
     * fuera de la transacción de la consulta.
     */
    @EntityGraph(attributePaths = {"categoria", "ciudad", "barrio", "usuario"})
    Optional<Negocio> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);
}
