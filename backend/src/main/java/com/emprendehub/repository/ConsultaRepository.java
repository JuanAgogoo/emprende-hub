package com.emprendehub.repository;

import com.emprendehub.model.Consulta;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    /**
     * El buzón de un negocio, las más recientes primero.
     *
     * <p>El filtro de lectura es opcional: a nulo llegan todas, y con
     * {@code false} solo las que quedan por leer, que es la vista con la que se
     * trabaja. Como devuelve una página, su {@code totalElements} sirve además
     * de contador para el aviso del panel, sin un endpoint aparte.
     *
     * <p>Trae resuelto el cliente porque la respuesta enseña su nombre y su
     * correo (D2), y la configuración usa {@code open-in-view: false}.
     */
    @EntityGraph(attributePaths = "cliente")
    @Query("""
            SELECT c FROM Consulta c
            WHERE c.negocio.id = :negocioId
              AND (:leida IS NULL OR c.leida = :leida)
            ORDER BY c.fechaEnvio DESC
            """)
    Page<Consulta> buscarEnBuzon(@Param("negocioId") Long negocioId,
                                 @Param("leida") Boolean leida,
                                 Pageable pageable);

    /**
     * Una consulta concreta, pero solo si es del buzón de ese negocio.
     *
     * <p>Lleva el negocio en la consulta a propósito: buscar por identificador y
     * comprobar el dueño después deja la puerta abierta a que alguien se olvide
     * de comprobarlo.
     */
    @EntityGraph(attributePaths = "cliente")
    Optional<Consulta> findByIdAndNegocioId(Long id, Long negocioId);
}
