package com.emprendehub.repository;

import com.emprendehub.model.Denuncia;
import com.emprendehub.model.EstadoDenuncia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {

    /**
     * La cola del administrador, las más antiguas primero.
     *
     * <p>Trae resuelta la opinión con su autor y su negocio: la tabla del panel
     * enseña el texto denunciado, de quién es y sobre qué negocio, y sin esto
     * serían tres consultas más por fila.
     */
    @EntityGraph(attributePaths = {"opinion", "opinion.autor", "opinion.negocio", "denunciante"})
    Page<Denuncia> findByEstadoOrderByFechaAsc(EstadoDenuncia estado, Pageable pageable);

    @EntityGraph(attributePaths = {"opinion", "opinion.autor", "opinion.negocio", "denunciante"})
    Optional<Denuncia> findWithDetalleById(Long id);

    boolean existsByOpinionIdAndDenuncianteId(Long opinionId, Long denuncianteId);

    /** Las de una opinión, que se van con ella cuando el administrador la borra. */
    List<Denuncia> findByOpinionId(Long opinionId);
}
