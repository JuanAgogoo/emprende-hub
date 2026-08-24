package com.emprendehub.service;

import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistrarNegocioRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.BarrioRepository;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta y consulta del propio negocio.
 *
 * <p>Las reglas que gobiernan el registro:
 * <ul>
 *   <li>Una cuenta tiene como mucho un negocio (A2).</li>
 *   <li>El administrador no puede tener negocio (A5): sería quien aprueba el
 *       suyo propio.</li>
 *   <li>Un cliente que registra su primer negocio <strong>conserva su cuenta</strong>
 *       y pasa a emprendedor (A1-bis). No abre una segunda.</li>
 *   <li>El barrio, si se indica, tiene que pertenecer a la ciudad elegida.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class NegocioService {

    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaNegocioRepository categoriaRepository;
    private final CiudadRepository ciudadRepository;
    private final BarrioRepository barrioRepository;

    public NegocioService(NegocioRepository negocioRepository,
                          UsuarioRepository usuarioRepository,
                          CategoriaNegocioRepository categoriaRepository,
                          CiudadRepository ciudadRepository,
                          BarrioRepository barrioRepository) {
        this.negocioRepository = negocioRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.ciudadRepository = ciudadRepository;
        this.barrioRepository = barrioRepository;
    }

    /** Registra el negocio y asciende al dueño, todo en una transacción. */
    @Transactional
    public NegocioResponse registrar(Usuario solicitante, RegistrarNegocioRequest peticion) {
        if (solicitante.getRol() == Rol.ADMIN) {
            throw new ReglaDeNegocioException(
                    "El administrador no puede registrar un negocio");
        }
        if (negocioRepository.existsByUsuarioId(solicitante.getId())) {
            throw new ReglaDeNegocioException("Esta cuenta ya tiene un negocio registrado");
        }

        CategoriaNegocio categoria = categoriaRepository.findById(peticion.categoriaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría", peticion.categoriaId()));
        Ciudad ciudad = ciudadRepository.findById(peticion.ciudadId())
                .orElseThrow(() -> new ResourceNotFoundException("Ciudad", peticion.ciudadId()));
        Barrio barrio = resolverBarrio(peticion.barrioId(), ciudad);

        Negocio negocio = new Negocio(solicitante, peticion.nombre().trim(),
                peticion.descripcion().trim(), peticion.telefono().trim(),
                categoria, ciudad, barrio, peticion.nivelPrecio());

        // El ascenso a emprendedor es parte del mismo acto: si algo falla
        // después, tampoco queda un cliente con rol cambiado y sin negocio.
        if (solicitante.getRol() == Rol.CLIENTE) {
            solicitante.setRol(Rol.EMPRENDEDOR);
            usuarioRepository.save(solicitante);
        }

        return aRespuesta(negocioRepository.save(negocio));
    }

    /** El negocio de quien pregunta, en cualquier estado: es suyo (B6). */
    public NegocioResponse obtenerElMio(Usuario solicitante) {
        return negocioRepository.findByUsuarioId(solicitante.getId())
                .map(this::aRespuesta)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Negocio del usuario", solicitante.getId()));
    }

    /**
     * Un barrio de otra ciudad haría que el negocio apareciera en el sitio
     * equivocado del directorio, así que se comprueba aquí: es una relación
     * entre dos campos y Bean Validation no la ve.
     */
    private Barrio resolverBarrio(Long barrioId, Ciudad ciudad) {
        if (barrioId == null) {
            return null;
        }
        Barrio barrio = barrioRepository.findById(barrioId)
                .orElseThrow(() -> new ResourceNotFoundException("Barrio", barrioId));
        if (!barrio.getCiudad().getId().equals(ciudad.getId())) {
            throw new ReglaDeNegocioException(
                    "El barrio elegido no pertenece a la ciudad indicada");
        }
        return barrio;
    }

    private NegocioResponse aRespuesta(Negocio negocio) {
        return new NegocioResponse(
                negocio.getId(), negocio.getNombre(), negocio.getDescripcion(),
                negocio.getTelefono(), negocio.getCategoria().getNombre(),
                negocio.getCiudad().getNombre(),
                negocio.getBarrio() == null ? null : negocio.getBarrio().getNombre(),
                negocio.getNivelPrecio().name(), negocio.getEstado().name(),
                negocio.getMotivoRechazo(), negocio.getCalificacionPromedio(),
                negocio.getNumeroOpiniones());
    }
}
