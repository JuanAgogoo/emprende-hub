package com.emprendehub.repository;

import com.emprendehub.model.Notificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Las notificaciones de una persona, las más recientes primero.
     *
     * <p>El filtro de lectura es opcional: a nulo llegan todas y con
     * {@code false} solo las pendientes, cuyo total sirve de aviso en el panel
     * sin necesitar un endpoint de contador.
     */
    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.destinatario.id = :destinatarioId
              AND (:leida IS NULL OR n.leida = :leida)
            ORDER BY n.fecha DESC
            """)
    Page<Notificacion> buscarDe(@Param("destinatarioId") Long destinatarioId,
                                @Param("leida") Boolean leida,
                                Pageable pageable);

    /** Una notificación concreta, pero solo si es de quien pregunta. */
    Optional<Notificacion> findByIdAndDestinatarioId(Long id, Long destinatarioId);

    /** Las pendientes, para marcarlas todas de una vez. */
    List<Notificacion> findByDestinatarioIdAndLeidaFalse(Long destinatarioId);
}
