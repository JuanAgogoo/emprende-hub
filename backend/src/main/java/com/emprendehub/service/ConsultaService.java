package com.emprendehub.service;

import com.emprendehub.dto.ConsultaResponse;
import com.emprendehub.dto.EnviarConsultaRequest;
import com.emprendehub.dto.ResponderConsultaRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Consulta;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.TipoNotificacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.ConsultaRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.OpinionRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El buzón de consultas de un negocio (sección D).
 *
 * <p>Las tres decisiones que lo definen:
 * <ul>
 *   <li><strong>D1</strong> — el mensaje se guarda dentro de la plataforma y el
 *       dueño lo lee en su panel. No sale ningún correo, porque la aplicación no
 *       envía ninguno (I1).</li>
 *   <li><strong>D2</strong> — el dueño responde desde el buzón y el cliente lo
 *       lee en su chat. El buzón enseña además el correo del cliente.</li>
 *   <li><strong>D3</strong> — leídas y no leídas, que es lo que convierte el
 *       buzón en una lista de pendientes y no en un archivo.</li>
 *   <li><strong>D4</strong> — solo escribe quien ya calificó el negocio. Así el
 *       buzón recibe mensajes de clientes reales y no de cualquiera con cuenta.</li>
 * </ul>
 *
 * <p>Contactar exige sesión, así que quien pregunta siempre tiene nombre y
 * correo: no hay consultas anónimas que el dueño no pueda contestar.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final NegocioRepository negocioRepository;
    private final NotificacionService notificacionService;
    private final OpinionRepository opinionRepository;

    public ConsultaService(ConsultaRepository consultaRepository,
                           NegocioRepository negocioRepository,
                           NotificacionService notificacionService,
                           OpinionRepository opinionRepository) {
        this.consultaRepository = consultaRepository;
        this.negocioRepository = negocioRepository;
        this.notificacionService = notificacionService;
        this.opinionRepository = opinionRepository;
    }

    /**
     * Envía una consulta al buzón de un negocio publicado.
     *
     * <p>Solo se puede escribir a negocios que se ven en el directorio (B6): uno
     * pendiente de revisión no existe para el público, y su dueño no tendría por
     * qué recibir mensajes de un perfil que todavía no ha publicado.
     */
    @Transactional
    public ConsultaResponse enviar(Usuario cliente, Long negocioId,
                                   EnviarConsultaRequest peticion) {
        Negocio negocio = negocioRepository.buscarVisibleEnDirectorio(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", negocioId));

        if (negocio.getUsuario().getId().equals(cliente.getId())) {
            throw new ReglaDeNegocioException(
                    "No tiene sentido escribirte a tu propio buzón");
        }
        if (!opinionRepository.existsByNegocioIdAndAutorId(negocioId, cliente.getId())) {
            throw new ReglaDeNegocioException(
                    "Para escribirle al negocio primero tienes que calificarlo");
        }

        Consulta consulta = consultaRepository.save(new Consulta(
                negocio, cliente, peticion.asunto().trim(), peticion.mensaje().trim()));

        // El buzón no avisa por sí solo: la notificación es lo que hace que el
        // dueño mire (H2).
        notificacionService.avisar(negocio.getUsuario(), TipoNotificacion.CONSULTA_NUEVA,
                "Nueva consulta de %s: %s".formatted(
                        cliente.getNombre(), consulta.getAsunto()));

        return aRespuesta(consulta);
    }

    /**
     * El buzón del negocio propio.
     *
     * <p>Con {@code leida} a {@code false} llegan solo las pendientes, y el
     * total de esa página es el número que el panel enseña como aviso. A nulo
     * llega el buzón entero.
     */
    public Page<ConsultaResponse> buzon(Usuario solicitante, Boolean leida, Pageable pageable) {
        Negocio negocio = buscarElMio(solicitante);
        return consultaRepository.buscarEnBuzon(negocio.getId(), leida, pageable)
                .map(this::aRespuesta);
    }

    /**
     * Marca una consulta como leída o la devuelve al montón (D3).
     *
     * <p>No se marca sola al listar el buzón: abrir la pantalla para echar un
     * vistazo no es lo mismo que haber atendido lo que hay dentro, y el estado
     * solo sirve de algo si lo decide el dueño.
     */
    @Transactional
    public ConsultaResponse marcarLectura(Usuario solicitante, Long consultaId, boolean leida) {
        Negocio negocio = buscarElMio(solicitante);
        Consulta consulta = consultaRepository
                .findByIdAndNegocioId(consultaId, negocio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Consulta", consultaId));

        consulta.marcarLectura(leida);

        return aRespuesta(consultaRepository.save(consulta));
    }

    /**
     * Lo que quien pregunta le ha escrito a un negocio, en orden de conversación.
     *
     * <p>Cada persona ve solo lo suyo: el filtro por cliente va en la consulta,
     * no en un paso posterior que alguien pueda olvidar.
     */
    public List<ConsultaResponse> misConsultas(Usuario cliente, Long negocioId) {
        negocioRepository.buscarVisibleEnDirectorio(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", negocioId));
        return consultaRepository
                .findByNegocioIdAndClienteIdOrderByFechaEnvioAsc(negocioId, cliente.getId())
                .stream().map(this::aRespuesta).toList();
    }

    /**
     * El dueño responde una consulta de su buzón, o corrige su respuesta (D2).
     *
     * <p>Solo se encuentra la consulta desde el buzón al que pertenece: nadie
     * responde por el negocio de otro.
     */
    @Transactional
    public ConsultaResponse responder(Usuario solicitante, Long consultaId,
                                      ResponderConsultaRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        Consulta consulta = consultaRepository
                .findByIdAndNegocioId(consultaId, negocio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Consulta", consultaId));

        consulta.responder(peticion.respuesta().trim());

        return aRespuesta(consultaRepository.save(consulta));
    }

    // ---------- Apoyo ----------

    private Negocio buscarElMio(Usuario solicitante) {
        return negocioRepository.findByUsuarioId(solicitante.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Negocio del usuario", solicitante.getId()));
    }

    /** Aquí, y solo aquí, sale el correo de otra persona (D2). */
    private ConsultaResponse aRespuesta(Consulta consulta) {
        return new ConsultaResponse(consulta.getId(), consulta.getAsunto(),
                consulta.getMensaje(), consulta.getCliente().getNombre(),
                consulta.getCliente().getCorreo(), consulta.isLeida(),
                consulta.getFechaEnvio(), consulta.getFechaLectura(),
                consulta.getRespuesta(), consulta.getFechaRespuesta());
    }
}
