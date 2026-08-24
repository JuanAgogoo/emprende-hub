package com.emprendehub.repository;

import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.Curso;
import com.emprendehub.model.EstadoCurso;
import com.emprendehub.model.NivelCurso;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    /**
     * Búsqueda del catálogo con todos los filtros opcionales.
     *
     * <p>Cada condición se anula cuando su parámetro llega a null, así que una
     * sola consulta cubre todas las combinaciones sin construir criterios
     * dinámicos. El estado también es opcional: el catálogo público pasa
     * {@code PUBLICADO} y el panel del administrador pasa null para verlo todo.
     *
     * <p>El {@code CAST} sobre {@code :texto} no es adorno: cuando el parámetro
     * llega nulo, PostgreSQL lo infiere como {@code bytea} y falla con
     * <em>function lower(bytea) does not exist</em>.
     */
    @Query("""
            SELECT c FROM Curso c
            WHERE (:estado IS NULL OR c.estado = :estado)
              AND (:categoria IS NULL OR c.categoria = :categoria)
              AND (:nivel IS NULL OR c.nivel = :nivel)
              AND (:gratuito IS NULL OR c.gratuito = :gratuito)
              AND (:texto IS NULL
                   OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%'))
                   OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', CAST(:texto AS string), '%')))
            """)
    Page<Curso> buscar(@Param("estado") EstadoCurso estado,
                       @Param("categoria") CategoriaCurso categoria,
                       @Param("nivel") NivelCurso nivel,
                       @Param("gratuito") Boolean gratuito,
                       @Param("texto") String texto,
                       Pageable pageable);

    /** Un curso concreto solo si está en el estado pedido (E4). */
    Optional<Curso> findByIdAndEstado(Long id, EstadoCurso estado);
}
