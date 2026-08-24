package com.emprendehub.repository;

import com.emprendehub.model.Negocio;
import com.emprendehub.model.EstadoNegocio;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Cola de revisión del administrador, los más antiguos primero. */
    @EntityGraph(attributePaths = {"categoria", "ciudad", "barrio", "usuario"})
    Page<Negocio> findByEstadoOrderByFechaCreacionAsc(EstadoNegocio estado, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "ciudad", "barrio", "usuario"})
    Optional<Negocio> findWithDetalleById(Long id);
}
