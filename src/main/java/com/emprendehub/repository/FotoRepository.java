package com.emprendehub.repository;

import com.emprendehub.model.EstadoFoto;
import com.emprendehub.model.Foto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FotoRepository extends JpaRepository<Foto, Long> {

    /** La galería que ve el dueño en su panel: todas, revisadas o no. */
    List<Foto> findByNegocioIdOrderByOrdenAsc(Long negocioId);

    /** La galería que ve el público: solo aprobadas. La primera es la principal (B9). */
    List<Foto> findByNegocioIdAndEstadoOrderByOrdenAsc(Long negocioId, EstadoFoto estado);

    /**
     * Cuenta todas, pendientes incluidas, porque el máximo de seis (B9) es del
     * espacio de la galería. Contar solo las aprobadas dejaría subir seis más
     * mientras las primeras esperan revisión.
     */
    long countByNegocioId(Long negocioId);

    /**
     * Las fotos aprobadas de varios negocios de una vez.
     *
     * <p>La usa el directorio para poner portada a cada tarjeta: una consulta
     * para la página entera en lugar de una por negocio. Vienen ordenadas por
     * negocio y por orden, así que la primera de cada grupo es su principal (B9).
     */
    List<Foto> findByNegocioIdInAndEstadoOrderByNegocioIdAscOrdenAsc(
            Collection<Long> negocioIds, EstadoFoto estado);

    Optional<Foto> findByIdAndNegocioId(Long id, Long negocioId);

    List<Foto> findByNegocioIdAndEstado(Long negocioId, EstadoFoto estado);

    /** Cuántas imágenes espera publicar la propuesta de cambio de un negocio. */
    int countByNegocioIdAndEstado(Long negocioId, EstadoFoto estado);

    /**
     * Publica de golpe las fotos que esperaban revisión.
     *
     * <p>Va como consulta y no recorriendo la lista porque es exactamente lo
     * que hace el administrador al aprobar: un acto sobre todas a la vez.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Foto f SET f.estado = com.emprendehub.model.EstadoFoto.APROBADA
            WHERE f.negocio.id = :negocioId
              AND f.estado = com.emprendehub.model.EstadoFoto.PENDIENTE
            """)
    int aprobarPendientes(@Param("negocioId") Long negocioId);
}
