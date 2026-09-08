package com.emprendehub.repository;

import com.emprendehub.model.Visita;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

    /** ¿Ya se contó hoy esta sesión sobre este perfil? (H1) */
    boolean existsByNegocioIdAndHuellaSesionAndFecha(Long negocioId, String huellaSesion,
                                                     LocalDate fecha);

    long countByNegocioId(Long negocioId);

    /**
     * Cuántas visitas hubo cada día desde una fecha.
     *
     * <p>Devuelve <strong>solo los días con visitas</strong>: los que no tuvieron
     * ninguna sencillamente no salen. Rellenar los huecos con ceros es trabajo
     * del servicio, que es donde vive la agregación y donde se prueba sin base de
     * datos de por medio.
     */
    @Query("""
            SELECT v.fecha AS fecha, COUNT(v) AS total
            FROM Visita v
            WHERE v.negocio.id = :negocioId AND v.fecha >= :desde
            GROUP BY v.fecha
            ORDER BY v.fecha ASC
            """)
    List<ConteoDiario> contarPorDiaDesde(@Param("negocioId") Long negocioId,
                                         @Param("desde") LocalDate desde);

    /** Las visitas de un día concreto, tal como las agrupa la consulta. */
    interface ConteoDiario {

        LocalDate getFecha();

        long getTotal();
    }
}
