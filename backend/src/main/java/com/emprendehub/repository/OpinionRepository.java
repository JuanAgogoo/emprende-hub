package com.emprendehub.repository;

import com.emprendehub.model.Opinion;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpinionRepository extends JpaRepository<Opinion, Long> {

    /**
     * Las opiniones de un negocio, las últimas primero.
     *
     * <p>Trae resuelto el autor porque la respuesta enseña su nombre y la
     * configuración usa {@code open-in-view: false}.
     */
    @EntityGraph(attributePaths = "autor")
    Page<Opinion> findByNegocioIdOrderByFechaCreacionDesc(Long negocioId, Pageable pageable);

    /** La opinión de una persona sobre un negocio. Solo puede haber una (C2). */
    @EntityGraph(attributePaths = "autor")
    Optional<Opinion> findByNegocioIdAndAutorId(Long negocioId, Long autorId);

    boolean existsByNegocioIdAndAutorId(Long negocioId, Long autorId);

    @EntityGraph(attributePaths = {"autor", "negocio"})
    Optional<Opinion> findWithDetalleById(Long id);

    /**
     * El recuento y la media de un negocio, en una sola consulta.
     *
     * <p>La usa el servicio para dejar al día las dos columnas desnormalizadas
     * del negocio cada vez que una opinión cambia. Se pide a la base de datos y
     * no se ajusta a mano sumando y restando: recalcular no puede desviarse,
     * y un ajuste incremental sí.
     *
     * <p>{@code AVG} devuelve nulo cuando no hay ninguna fila, que es justo lo
     * que pide C5: sin opiniones no hay calificación, y no es un cero.
     */
    @Query("""
            SELECT COUNT(o) AS total, AVG(o.calificacion) AS promedio
            FROM Opinion o WHERE o.negocio.id = :negocioId
            """)
    ResumenDeCalificacion resumirPorNegocio(@Param("negocioId") Long negocioId);

    /** Proyección del recuento y la media. */
    interface ResumenDeCalificacion {

        long getTotal();

        /** Nulo mientras el negocio no tenga ninguna opinión (C5). */
        Double getPromedio();
    }
}
