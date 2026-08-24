package com.emprendehub.service;

import com.emprendehub.dto.ActualizarOpinionRequest;
import com.emprendehub.dto.CrearOpinionRequest;
import com.emprendehub.dto.OpinionResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.DenunciaRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.OpinionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opiniones y calificaciones de los negocios (sección C).
 *
 * <p>Las reglas que gobiernan todo lo demás:
 * <ul>
 *   <li><strong>Una por persona y negocio</strong> (C2). Su autor la edita o la
 *       borra; nadie más.</li>
 *   <li><strong>Nunca sobre el negocio propio</strong> (A4). Es la única regla
 *       de la sección que no viene de la sección C.</li>
 *   <li><strong>Solo sobre negocios publicados</strong> (B6). Uno pendiente no
 *       existe para el público, así que tampoco se puede opinar de él.</li>
 *   <li>Se publican <strong>al instante</strong> (C4); solo llegan al
 *       administrador si alguien las denuncia.</li>
 * </ul>
 *
 * <p>Cada cambio deja al día el promedio y el recuento del negocio. Es la razón
 * de que esas dos columnas estén desnormalizadas: el directorio ordena y filtra
 * por ellas, y calcularlas en cada búsqueda saldría caro.
 */
@Service
@Transactional(readOnly = true)
public class OpinionService {

    private final OpinionRepository opinionRepository;
    private final NegocioRepository negocioRepository;
    private final DenunciaRepository denunciaRepository;

    public OpinionService(OpinionRepository opinionRepository,
                          NegocioRepository negocioRepository,
                          DenunciaRepository denunciaRepository) {
        this.opinionRepository = opinionRepository;
        this.negocioRepository = negocioRepository;
        this.denunciaRepository = denunciaRepository;
    }

    // ---------- Lectura ----------

    /** Las opiniones de un negocio publicado. Se leen sin sesión. */
    public Page<OpinionResponse> listarDeNegocio(Long negocioId, Pageable pageable) {
        buscarNegocioVisible(negocioId);
        return opinionRepository.findByNegocioIdOrderByFechaCreacionDesc(negocioId, pageable)
                .map(this::aRespuesta);
    }

    /** La opinión propia, para que el cliente sepa si ya opinó y con qué. */
    public OpinionResponse obtenerLaMia(Usuario solicitante, Long negocioId) {
        return opinionRepository.findByNegocioIdAndAutorId(negocioId, solicitante.getId())
                .map(this::aRespuesta)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Opinión propia sobre el negocio", negocioId));
    }

    // ---------- Escritura por su autor ----------

    /**
     * Publica una opinión (C1, C2, C4, A4).
     *
     * <p>Se hace visible en el mismo momento: no hay revisión previa. Lo que sí
     * cambia al instante es el promedio del negocio.
     */
    @Transactional
    public OpinionResponse crear(Usuario autor, Long negocioId, CrearOpinionRequest peticion) {
        Negocio negocio = buscarNegocioVisible(negocioId);

        if (esSuPropioNegocio(negocio, autor)) {
            throw new ReglaDeNegocioException("No puedes opinar sobre tu propio negocio");
        }
        if (opinionRepository.existsByNegocioIdAndAutorId(negocioId, autor.getId())) {
            throw new ReglaDeNegocioException(
                    "Ya opinaste sobre este negocio; edita tu opinión en vez de crear otra");
        }

        Opinion opinion = opinionRepository.save(new Opinion(
                negocio, autor, peticion.calificacion(), normalizar(peticion.comentario())));
        recalcular(negocio);

        return aRespuesta(opinion);
    }

    /** Cambia la opinión propia. Solo la toca su autor (C2). */
    @Transactional
    public OpinionResponse actualizar(Usuario autor, Long negocioId,
                                      ActualizarOpinionRequest peticion) {
        Opinion opinion = buscarLaMia(autor, negocioId);

        opinion.setCalificacion(peticion.calificacion());
        opinion.setComentario(normalizar(peticion.comentario()));
        opinion.setFechaEdicion(Instant.now());
        opinionRepository.save(opinion);

        // La nota pudo cambiar, así que el promedio del negocio también.
        recalcular(opinion.getNegocio());

        return aRespuesta(opinion);
    }

    /** Retira la opinión propia y deja el promedio al día. */
    @Transactional
    public void eliminar(Usuario autor, Long negocioId) {
        Opinion opinion = buscarLaMia(autor, negocioId);
        Negocio negocio = opinion.getNegocio();

        borrarConSusDenuncias(opinion);
        recalcular(negocio);
    }

    // ---------- Escritura por la moderación ----------

    /**
     * Borra una opinión por decisión del administrador.
     *
     * <p>Existe aquí, y no en {@code ModeracionService}, para que el recálculo
     * del promedio sea imposible de olvidar: la moderación es el cuarto momento
     * en que una opinión cambia y el que más fácil se escapa, porque no lo
     * dispara su autor.
     */
    @Transactional
    public void eliminarPorModeracion(Opinion opinion) {
        Negocio negocio = opinion.getNegocio();
        borrarConSusDenuncias(opinion);
        recalcular(negocio);
    }

    // ---------- Apoyo ----------

    /**
     * Deja al día las dos columnas desnormalizadas del negocio.
     *
     * <p>Se vuelve a preguntar a la base de datos en vez de sumar o restar sobre
     * lo que había: un ajuste incremental se desvía en cuanto una operación
     * falla a medias, y este no puede.
     *
     * <p>Sin ninguna opinión el promedio queda a <strong>nulo, no a cero</strong>
     * (C5): el negocio se enseña como «Nuevo» y sale del filtro de estrellas, en
     * vez de aparecer como el peor calificado de la plataforma.
     */
    private void recalcular(Negocio negocio) {
        var resumen = opinionRepository.resumirPorNegocio(negocio.getId());

        negocio.setNumeroOpiniones((int) resumen.getTotal());
        negocio.setCalificacionPromedio(resumen.getPromedio() == null ? null
                : BigDecimal.valueOf(resumen.getPromedio()).setScale(2, RoundingMode.HALF_UP));

        negocioRepository.save(negocio);
    }

    /**
     * Borra la opinión junto a las denuncias que la señalaban.
     *
     * <p>Sin esto, una denuncia sobreviviría a la opinión que denunciaba y la
     * cola del administrador se llenaría de avisos sobre textos que ya no
     * existen.
     */
    private void borrarConSusDenuncias(Opinion opinion) {
        denunciaRepository.deleteAll(denunciaRepository.findByOpinionId(opinion.getId()));
        opinionRepository.delete(opinion);
    }

    /** Un negocio que no se ve en el directorio no admite opiniones (B6, B4). */
    private Negocio buscarNegocioVisible(Long negocioId) {
        return negocioRepository.buscarVisibleEnDirectorio(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", negocioId));
    }

    private Opinion buscarLaMia(Usuario autor, Long negocioId) {
        return opinionRepository.findByNegocioIdAndAutorId(negocioId, autor.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Opinión propia sobre el negocio", negocioId));
    }

    /** A4: el emprendedor opina sobre otros negocios, nunca sobre el suyo. */
    private boolean esSuPropioNegocio(Negocio negocio, Usuario autor) {
        return negocio.getUsuario().getId().equals(autor.getId());
    }

    /** Un comentario en blanco es lo mismo que no escribir nada: es opcional. */
    private String normalizar(String comentario) {
        return (comentario == null || comentario.isBlank()) ? null : comentario.trim();
    }

    private OpinionResponse aRespuesta(Opinion opinion) {
        return new OpinionResponse(opinion.getId(), opinion.getAutor().getNombre(),
                opinion.getCalificacion(), opinion.getComentario(),
                opinion.getFechaCreacion(), opinion.fueEditada());
    }
}
