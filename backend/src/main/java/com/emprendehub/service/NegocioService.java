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

    /**
     * El negocio de quien pregunta, en cualquier estado: es suyo (B6).
     *
     * <p>Viaja con la propuesta de cambio que tenga en cola, si la tiene: es la
     * única forma de que el dueño sepa que su edición está esperando revisión y
     * no se perdió.
     */
    public NegocioResponse obtenerElMio(Usuario solicitante) {
        return negocioRepository.findByUsuarioId(solicitante.getId())
                .map(this::conSuPropuesta)
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
     * Edita los campos públicos: nombre, descripción y categoría (B2 y B2-bis).
     *
     * <p>Dónde acaba el cambio depende de dos cosas, y esta es la única regla
     * que hay que recordar: <strong>solo espera revisión lo que ya está
     * publicado, y solo si la revisión está encendida.</strong>
     *
     * <ul>
     *   <li><b>Negocio publicado y revisión encendida</b>: se guarda la
     *       propuesta aparte y el público sigue viendo la versión aprobada, así
     *       que corregir una errata nunca saca al negocio del directorio
     *       (B2-bis). Solo hay una propuesta viva por negocio: una nueva
     *       sustituye a la anterior.
     *   <li><b>Negocio publicado y revisión apagada</b>: se aplica al momento.
     *       Es el mismo interruptor que ya decidía si un negocio nace publicado
     *       y si sus fotos nacen aprobadas; tenerlo puesto para el registro y no
     *       para la edición dejaba a medias justo el caso que se usa al
     *       depurar.
     *   <li><b>Negocio sin publicar</b>: se aplica al momento, haya revisión o
     *       no. No hay versión pública que proteger, y el negocio entero pasa
     *       por revisión igualmente antes de salir al directorio.
     * </ul>
     *
     * <p>Un negocio rechazado no entra por aquí: tiene su propio camino en
     * {@link #corregirYReenviar}, que además lo devuelve a la cola (B1).
     */
    @Transactional
    public NegocioResponse proponerCambioPublico(Usuario solicitante,
                                                 EditarNegocioPublicoRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        if (negocio.getEstado() == EstadoNegocio.RECHAZADO) {
            throw new ReglaDeNegocioException(
                    "Un negocio rechazado corrige y vuelve a enviar, no propone cambios");
        }

        CategoriaNegocio categoria = resolverCategoria(peticion.categoriaId());

        if (moderacionAutomatica || negocio.getEstado() != EstadoNegocio.APROBADO) {
            aplicar(negocio, peticion, categoria);
            return conSuPropuesta(negocioRepository.save(negocio));
        }

        CambioPendiente cambio = cambioRepository.findByNegocioId(negocio.getId())
                .orElseGet(() -> new CambioPendiente(negocio, peticion.nombre().trim(),
                        peticion.descripcion().trim()));
        cambio.setNombrePropuesto(peticion.nombre().trim());
        cambio.setDescripcionPropuesta(peticion.descripcion().trim());
        cambio.setCategoriaPropuesta(categoria);
        cambioRepository.save(cambio);

        return NegocioMapper.aRespuesta(negocio, cambio);
    }

    /** Copia los valores editados sobre el negocio. La categoría es opcional. */
    private void aplicar(Negocio negocio, EditarNegocioPublicoRequest peticion,
                         CategoriaNegocio categoria) {
        negocio.setNombre(peticion.nombre().trim());
        negocio.setDescripcion(peticion.descripcion().trim());
        if (categoria != null) {
            negocio.setCategoria(categoria);
        }
    }

    /** Una categoría nula no es un error: significa que no se cambia. */
    private CategoriaNegocio resolverCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", categoriaId));
    }

    /** El teléfono se actualiza al instante: no pasa por revisión (B2). */
    @Transactional
    public NegocioResponse actualizarContacto(Usuario solicitante,
                                              EditarContactoRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        negocio.setTelefono(peticion.telefono().trim());
        // Devuelve también la propuesta en cola: quien corrige el teléfono
        // mientras espera revisión no puede perder de vista que la tiene.
        return conSuPropuesta(negocioRepository.save(negocio));
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
        aplicar(negocio, peticion, resolverCategoria(peticion.categoriaId()));
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
        return conSuPropuesta(negocioRepository.save(negocio));
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

    /** El negocio con su propuesta en cola, si tiene alguna esperando. */
    private NegocioResponse conSuPropuesta(Negocio negocio) {
        return NegocioMapper.aRespuesta(negocio,
                cambioRepository.findByNegocioId(negocio.getId()).orElse(null));
    }
}
