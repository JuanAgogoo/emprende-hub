package com.emprendehub.service;

import com.emprendehub.dto.EditarContactoRequest;
import com.emprendehub.dto.EditarNegocioPublicoRequest;
import com.emprendehub.dto.EditarRedesRequest;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistrarNegocioRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Barrio;
import com.emprendehub.model.CambioPendiente;
import com.emprendehub.model.EstadoNegocio;
import java.time.Instant;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.BarrioRepository;
import com.emprendehub.repository.CambioPendienteRepository;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
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
    private final CambioPendienteRepository cambioRepository;

    /**
     * Interruptor **provisional** para trabajar sin moderación.
     *
     * <p>Con él puesto el negocio nace aprobado y no hay que pasar por el
     * administrador para verlo en el directorio. **No cambia la regla B6**, que
     * sigue siendo la de por defecto: solo la desactiva mientras se construye.
     * Se enciende con {@code MODERACION_AUTOMATICA=true}.
     */
    private final boolean moderacionAutomatica;

    public NegocioService(NegocioRepository negocioRepository,
                          UsuarioRepository usuarioRepository,
                          CategoriaNegocioRepository categoriaRepository,
                          CiudadRepository ciudadRepository,
                          BarrioRepository barrioRepository,
                          CambioPendienteRepository cambioRepository,
                          @Value("${emprendehub.moderacion.automatica:false}")
                          boolean moderacionAutomatica) {
        this.moderacionAutomatica = moderacionAutomatica;
        this.cambioRepository = cambioRepository;
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

        // Sin moderación, el negocio sale publicado del propio registro. La
        // fecha de aprobación se sella igual porque de ella depende el orden
        // RECIENTES del directorio (G7).
        if (moderacionAutomatica) {
            negocio.setEstado(EstadoNegocio.APROBADO);
            negocio.setFechaAprobacion(Instant.now());
        }

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

    // ---------- Edición por el dueño ----------

    /**
     * Propone un cambio de los campos públicos (B2 y B2-bis).
     *
     * <p>No toca el negocio: guarda la propuesta aparte. El público sigue viendo
     * la versión aprobada mientras el administrador decide, así que corregir una
     * errata nunca saca al negocio del directorio.
     *
     * <p>Solo hay una propuesta viva por negocio: una nueva sustituye a la
     * anterior.
     */
    @Transactional
    public NegocioResponse proponerCambioPublico(Usuario solicitante,
                                                 EditarNegocioPublicoRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        if (negocio.getEstado() != EstadoNegocio.APROBADO) {
            throw new ReglaDeNegocioException(
                    "Solo un negocio aprobado propone cambios; corrige y vuelve a enviar");
        }

        CambioPendiente cambio = cambioRepository.findByNegocioId(negocio.getId())
                .orElseGet(() -> new CambioPendiente(negocio, peticion.nombre().trim(),
                        peticion.descripcion().trim()));
        cambio.setNombrePropuesto(peticion.nombre().trim());
        cambio.setDescripcionPropuesta(peticion.descripcion().trim());
        cambioRepository.save(cambio);

        return aRespuesta(negocio);
    }

    /** El teléfono se actualiza al instante: no pasa por revisión (B2). */
    @Transactional
    public NegocioResponse actualizarContacto(Usuario solicitante,
                                              EditarContactoRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        negocio.setTelefono(peticion.telefono().trim());
        return aRespuesta(negocioRepository.save(negocio));
    }

    /**
     * Corrige y vuelve a enviar un negocio rechazado (B1).
     *
     * <p>Sin límite de intentos. El motivo del rechazo anterior se borra al
     * reenviar: ya no describe el estado actual.
     */
    @Transactional
    public NegocioResponse corregirYReenviar(Usuario solicitante,
                                             EditarNegocioPublicoRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        if (negocio.getEstado() != EstadoNegocio.RECHAZADO) {
            throw new ReglaDeNegocioException("Solo se reenvía un negocio rechazado");
        }
        negocio.setNombre(peticion.nombre().trim());
        negocio.setDescripcion(peticion.descripcion().trim());
        negocio.setEstado(EstadoNegocio.PENDIENTE);
        negocio.setMotivoRechazo(null);
        return aRespuesta(negocioRepository.save(negocio));
    }

    /**
     * Actualiza los enlaces a redes sociales (B8).
     *
     * <p>Se aplican al instante, como el teléfono: son datos de contacto, no
     * contenido que el administrador tenga que revisar. El formato ya lo
     * comprobó Bean Validation; aquí solo se decide que una cadena vacía
     * significa «no tengo», que es lo que manda el formulario al borrar el campo.
     */
    @Transactional
    public NegocioResponse actualizarRedes(Usuario solicitante, EditarRedesRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        negocio.setInstagram(normalizar(peticion.instagram()));
        negocio.setLinkedin(normalizar(peticion.linkedin()));
        return aRespuesta(negocioRepository.save(negocio));
    }

    /** Un enlace en blanco es lo mismo que no tenerlo: los dos son opcionales. */
    private String normalizar(String enlace) {
        return (enlace == null || enlace.isBlank()) ? null : enlace.trim();
    }

    private Negocio buscarElMio(Usuario solicitante) {
        return negocioRepository.findByUsuarioId(solicitante.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Negocio del usuario", solicitante.getId()));
    }

    private NegocioResponse aRespuesta(Negocio negocio) {
        return NegocioMapper.aRespuesta(negocio);
    }
}
