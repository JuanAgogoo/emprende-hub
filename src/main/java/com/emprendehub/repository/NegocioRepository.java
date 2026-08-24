package com.emprendehub.repository;

import com.emprendehub.model.Negocio;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.NivelPrecio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ---------- Directorio público ----------

    /**
     * Búsqueda del directorio con todos los filtros opcionales.
     *
     * <p>La visibilidad no es un filtro más y por eso está escrita en la propia
     * consulta y no llega por parámetro: al directorio solo salen los aprobados
     * (B6) de cuentas que no estén suspendidas (B4). Ningún error de un servicio
     * puede pedir otra cosa.
     *
     * <p>Cada condición se anula cuando su parámetro llega a nulo, así que una
     * sola consulta cubre todas las combinaciones sin construir criterios
     * dinámicos. El {@code CAST} sobre {@code :texto} no es adorno: con el
     * parámetro nulo, PostgreSQL lo infiere como {@code bytea} y falla con
     * <em>function lower(bytea) does not exist</em>.
     *
     * <p>El filtro de calificación deja fuera por sí solo a los negocios sin
     * opiniones, porque su calificación es nula y ninguna comparación con nulo
     * es cierta. Es justo lo que pide C5.
     *
     * <p>{@code n.barrio.id} no genera ningún JOIN —Hibernate lee la clave ajena
     * de la propia fila—, así que un negocio sin barrio sigue apareciendo cuando
     * se filtra por su ciudad. Con un JOIN implícito se habría quedado fuera.
     */
    @EntityGraph(attributePaths = {"categoria", "ciudad", "barrio"})
    @Query("""
            SELECT n FROM Negocio n
            WHERE n.estado = com.emprendehub.model.EstadoNegocio.APROBADO
              AND n.usuario.activo = true
              AND (:categoriaId IS NULL OR n.categoria.id = :categoriaId)
              AND (:ciudadId IS NULL OR n.ciudad.id = :ciudadId)
              AND (:barrioId IS NULL OR n.barrio.id = :barrioId)
              AND (:nivelPrecio IS NULL OR n.nivelPrecio = :nivelPrecio)
              AND (:calificacionMinima IS NULL
                   OR n.calificacionPromedio >= :calificacionMinima)
              AND (:texto IS NULL
                   OR LOWER(n.nombre) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%'))
                   OR LOWER(n.descripcion) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%')))
            """)
    Page<Negocio> buscarEnDirectorio(@Param("texto") String texto,
                                     @Param("categoriaId") Long categoriaId,
                                     @Param("ciudadId") Long ciudadId,
                                     @Param("barrioId") Long barrioId,
                                     @Param("calificacionMinima") BigDecimal calificacionMinima,
                                     @Param("nivelPrecio") NivelPrecio nivelPrecio,
                                     Pageable pageable);

    /**
     * Un negocio por su identificador público, solo si se puede enseñar (B6).
     *
     * <p>Devuelve vacío tanto si no existe como si está pendiente, rechazado o
     * es de una cuenta suspendida: el servicio responde 404 en los cuatro casos
     * y no filtra información sobre lo que hay sin publicar.
     */
    @EntityGraph(attributePaths = {"categoria", "ciudad", "barrio"})
    @Query("""
            SELECT n FROM Negocio n
            WHERE n.id = :id
              AND n.estado = com.emprendehub.model.EstadoNegocio.APROBADO
              AND n.usuario.activo = true
            """)
    Optional<Negocio> buscarVisibleEnDirectorio(@Param("id") Long id);

    /**
     * Destacados de la portada: los mejor calificados con un mínimo de opiniones
     * (C7).
     *
     * <p>El orden va escrito aquí y no en el {@code Pageable}, que llega sin
     * ordenación y solo aporta el límite. No hace falta tratar los nulos: exigir
     * opiniones garantiza que todos tienen calificación.
     */
    @EntityGraph(attributePaths = {"categoria", "ciudad", "barrio"})
    @Query("""
            SELECT n FROM Negocio n
            WHERE n.estado = com.emprendehub.model.EstadoNegocio.APROBADO
              AND n.usuario.activo = true
              AND n.numeroOpiniones >= :minimoOpiniones
            ORDER BY n.calificacionPromedio DESC, n.numeroOpiniones DESC
            """)
    List<Negocio> buscarDestacados(@Param("minimoOpiniones") int minimoOpiniones,
                                   Pageable limite);

    /** Negocios activos de la portada: los que de verdad se ven (H4). */
    @Query("""
            SELECT COUNT(n) FROM Negocio n
            WHERE n.estado = com.emprendehub.model.EstadoNegocio.APROBADO
              AND n.usuario.activo = true
            """)
    long contarVisiblesEnDirectorio();

    /**
     * Media de las calificaciones de la portada (H4).
     *
     * <p>Solo entran los negocios que tienen calificación: uno sin opiniones no
     * vale cero (C5) y contarlo como tal hundiría la media de toda la plataforma.
     * Devuelve nulo mientras no haya ninguna.
     */
    @Query("""
            SELECT AVG(n.calificacionPromedio) FROM Negocio n
            WHERE n.estado = com.emprendehub.model.EstadoNegocio.APROBADO
              AND n.usuario.activo = true
              AND n.calificacionPromedio IS NOT NULL
            """)
    Double promedioDeCalificaciones();
}
