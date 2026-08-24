package com.emprendehub.service;

import com.emprendehub.dto.CambioPendienteResponse;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistroModeracionResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CambioPendiente;
import com.emprendehub.model.DecisionModeracion;
import com.emprendehub.model.EstadoFoto;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.RegistroModeracion;
import com.emprendehub.model.TipoEventoModeracion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.CambioPendienteRepository;
import com.emprendehub.repository.FotoRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.RegistroModeracionRepository;
import com.emprendehub.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Trabajo del administrador sobre negocios y cuentas (sección B y sección L).
 *
 * <p>Toda acción que cambia algo deja constancia en el log de moderación. No es
 * opcional: es lo que permite explicar después por qué un negocio está como está.
 */
@Service
@Transactional(readOnly = true)
public class ModeracionService {

    private final NegocioRepository negocioRepository;
    private final CambioPendienteRepository cambioRepository;
    private final RegistroModeracionRepository logRepository;
    private final UsuarioRepository usuarioRepository;
    private final FotoRepository fotoRepository;
    private final FotoService fotoService;

    public ModeracionService(NegocioRepository negocioRepository,
                             CambioPendienteRepository cambioRepository,
                             RegistroModeracionRepository logRepository,
                             UsuarioRepository usuarioRepository,
                             FotoRepository fotoRepository,
                             FotoService fotoService) {
        this.negocioRepository = negocioRepository;
        this.cambioRepository = cambioRepository;
        this.logRepository = logRepository;
        this.usuarioRepository = usuarioRepository;
        this.fotoRepository = fotoRepository;
        this.fotoService = fotoService;
    }

    // ---------- Cola de revisión ----------

    public Page<NegocioResponse> negociosPendientes(Pageable pageable) {
        return negocioRepository
                .findByEstadoOrderByFechaCreacionAsc(EstadoNegocio.PENDIENTE, pageable)
                .map(NegocioMapper::aRespuesta);
    }

    /**
     * Resuelve un negocio pendiente.
     *
     * <p>El {@code switch} sobre la decisión es exhaustivo: el compilador obliga
     * a tratar todas las ramas de la {@code sealed interface}.
     *
     * <p>El número de opiniones no entra en la ecuación (B7). Exigirlas sería un
     * bloqueo circular: solo se puede opinar sobre negocios ya publicados.
     */
    @Transactional
    public NegocioResponse resolverNegocio(Long negocioId, DecisionModeracion decision,
                                           Usuario admin) {
        Negocio negocio = buscarNegocio(negocioId);
        if (negocio.getEstado() != EstadoNegocio.PENDIENTE) {
            throw new ReglaDeNegocioException(
                    "Solo se pueden resolver negocios pendientes de revisión");
        }

        switch (decision) {
            case DecisionModeracion.Aprobar ignorada -> {
                negocio.setEstado(EstadoNegocio.APROBADO);
                negocio.setFechaAprobacion(Instant.now());
                negocio.setMotivoRechazo(null);
                // Las fotos que subió al registrarse se publican con él: nadie
                // las revisó por separado porque el negocio entero estaba sin
                // revisar.
                fotoService.aprobarPendientes(negocio.getId());
                registrar(TipoEventoModeracion.NEGOCIO_APROBADO, negocio.getNombre(), null, admin);
            }
            case DecisionModeracion.Rechazar rechazo -> {
                negocio.setEstado(EstadoNegocio.RECHAZADO);
                // Sin correos (I1), este texto es el único sitio donde el dueño
                // se entera de por qué le rechazaron.
                negocio.setMotivoRechazo(rechazo.motivo());
                registrar(TipoEventoModeracion.NEGOCIO_RECHAZADO, negocio.getNombre(),
                        rechazo.motivo(), admin);
            }
        }

        return NegocioMapper.aRespuesta(negocioRepository.save(negocio));
    }

    // ---------- Cambios pendientes ----------

    /**
     * La cola de propuestas de cambio, las más antiguas primero.
     *
     * <p>Devuelve el valor actual junto al propuesto: revisar es comparar. Y
     * cuenta las fotos que esperan, porque desde el PR 10 una propuesta puede
     * ser solo de imágenes y entonces los dos textos coinciden.
     */
    public List<CambioPendienteResponse> cambiosPendientes() {
        return cambioRepository.findAllByOrderByFechaSolicitudAsc().stream()
                .map(cambio -> {
                    Negocio negocio = cambio.getNegocio();
                    return new CambioPendienteResponse(
                            negocio.getId(), negocio.getNombre(), cambio.getNombrePropuesto(),
                            negocio.getDescripcion(), cambio.getDescripcionPropuesta(),
                            fotoRepository.countByNegocioIdAndEstado(
                                    negocio.getId(), EstadoFoto.PENDIENTE),
                            cambio.getFechaSolicitud());
                })
                .toList();
    }

    /**
     * Resuelve una propuesta de cambio (B2-bis).
     *
     * <p>Al aprobar se copian los valores al negocio; al rechazar se descarta la
     * propuesta. En ambos casos el negocio siguió publicado todo el tiempo con
     * sus valores anteriores.
     */
    @Transactional
    public NegocioResponse resolverCambio(Long negocioId, DecisionModeracion decision,
                                          Usuario admin) {
        CambioPendiente cambio = cambioRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cambio pendiente del negocio", negocioId));
        Negocio negocio = cambio.getNegocio();

        switch (decision) {
            case DecisionModeracion.Aprobar ignorada -> {
                negocio.setNombre(cambio.getNombrePropuesto());
                negocio.setDescripcion(cambio.getDescripcionPropuesta());
                fotoService.aprobarPendientes(negocio.getId());
                registrar(TipoEventoModeracion.CAMBIO_APROBADO, negocio.getNombre(), null, admin);
            }
            case DecisionModeracion.Rechazar rechazo -> {
                fotoService.descartarPendientes(negocio.getId());
                registrar(TipoEventoModeracion.CAMBIO_RECHAZADO, negocio.getNombre(),
                        rechazo.motivo(), admin);
            }
        }

        cambioRepository.delete(cambio);
        return NegocioMapper.aRespuesta(negocioRepository.save(negocio));
    }

    // ---------- Cuentas ----------

    /**
     * Suspende una cuenta (B4).
     *
     * <p>Su negocio deja de verse en el directorio porque
     * {@code Negocio.esVisiblePublicamente()} mira si el dueño sigue activo. Las
     * opiniones que escribió se mantienen: son de otros negocios y siguen siendo
     * información válida.
     */
    @Transactional
    public void suspender(Long usuarioId, Usuario admin) {
        Usuario usuario = buscarUsuario(usuarioId);
        if (usuario.equals(admin) || usuario.getId().equals(admin.getId())) {
            throw new ReglaDeNegocioException("El administrador no puede suspenderse a sí mismo");
        }
        if (!usuario.isActivo()) {
            throw new ReglaDeNegocioException("La cuenta ya estaba suspendida");
        }
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        registrar(TipoEventoModeracion.CUENTA_SUSPENDIDA, usuario.getCorreo(), null, admin);
    }

    @Transactional
    public void reactivar(Long usuarioId, Usuario admin) {
        Usuario usuario = buscarUsuario(usuarioId);
        if (usuario.isActivo()) {
            throw new ReglaDeNegocioException("La cuenta ya estaba activa");
        }
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        registrar(TipoEventoModeracion.CUENTA_REACTIVADA, usuario.getCorreo(), null, admin);
    }

    // ---------- Log ----------

    public Page<RegistroModeracionResponse> consultarLog(Instant desde, Instant hasta,
                                                         Pageable pageable) {
        return logRepository.buscar(desde, hasta, pageable)
                .map(r -> new RegistroModeracionResponse(
                        r.getId(), r.getTipo().name(), r.getTipo().getDescripcion(),
                        r.getAfectado(), r.getDetalle(), r.getAdministrador(), r.getFecha()));
    }

    /** Punto único de escritura del log: nadie más crea entradas. */
    void registrar(TipoEventoModeracion tipo, String afectado, String detalle, Usuario admin) {
        logRepository.save(new RegistroModeracion(tipo, afectado, detalle, admin.getCorreo()));
    }

    private Negocio buscarNegocio(Long id) {
        return negocioRepository.findWithDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", id));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }
}
