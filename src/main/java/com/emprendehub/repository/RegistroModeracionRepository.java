package com.emprendehub.repository;

import com.emprendehub.model.RegistroModeracion;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroModeracionRepository extends JpaRepository<RegistroModeracion, Long> {

    /**
     * Log filtrado por rango de fechas, como el panel del prototipo. Ambos
     * extremos son opcionales.
     *
     * <p>El {@code CAST} es obligatorio: con el parámetro a null PostgreSQL no
     * sabe inferir su tipo y responde <em>could not determine data type of
     * parameter</em>. Es el mismo problema que el {@code lower(bytea)} del
     * catálogo de cursos, y la razón de que toda consulta con filtros
     * opcionales necesite su prueba de repositorio.
     */
    @Query("""
            SELECT r FROM RegistroModeracion r
            WHERE (CAST(:desde AS Instant) IS NULL OR r.fecha >= :desde)
              AND (CAST(:hasta AS Instant) IS NULL OR r.fecha <= :hasta)
            ORDER BY r.fecha DESC
            """)
    Page<RegistroModeracion> buscar(@Param("desde") Instant desde,
                                    @Param("hasta") Instant hasta,
                                    Pageable pageable);
}
