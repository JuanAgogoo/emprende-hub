package com.emprendehub.service;

import com.emprendehub.dto.DenunciarOpinionRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Denuncia;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.DenunciaRepository;
import com.emprendehub.repository.OpinionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Denuncias sobre opiniones publicadas (C3, C6).
 *
 * <p>Es la única puerta por la que una opinión llega al administrador: se
 * publican al instante y solo se moderan si alguien las señala (C4).
 *
 * <p>Denuncia <strong>cualquier usuario con sesión</strong>, incluido el dueño
 * del negocio afectado, que es quien más motivos tiene para leerlas. No se
 * restringe a él porque una opinión con lenguaje inapropiado le molesta a
 * cualquiera que la lea, no solo a quien va dirigida.
 */
@Service
@Transactional(readOnly = true)
public class DenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final OpinionRepository opinionRepository;

    public DenunciaService(DenunciaRepository denunciaRepository,
                           OpinionRepository opinionRepository) {
        this.denunciaRepository = denunciaRepository;
        this.opinionRepository = opinionRepository;
    }

    /**
     * Señala una opinión para que el administrador la revise.
     *
     * <p>No la oculta: la opinión sigue publicada mientras se decide. Esconderla
     * al primer aviso convertiría el botón de denunciar en un botón de censurar,
     * que es lo contrario de lo que C4 quiere.
     */
    @Transactional
    public void denunciar(Usuario denunciante, Long opinionId,
                          DenunciarOpinionRequest peticion) {
        Opinion opinion = opinionRepository.findWithDetalleById(opinionId)
                .orElseThrow(() -> new ResourceNotFoundException("Opinión", opinionId));

        if (opinion.getAutor().getId().equals(denunciante.getId())) {
            throw new ReglaDeNegocioException(
                    "No tiene sentido denunciar tu propia opinión; puedes borrarla");
        }
        if (denunciaRepository.existsByOpinionIdAndDenuncianteId(
                opinionId, denunciante.getId())) {
            throw new ReglaDeNegocioException("Ya denunciaste esta opinión");
        }

        denunciaRepository.save(new Denuncia(opinion, denunciante, peticion.motivo()));
    }
}
