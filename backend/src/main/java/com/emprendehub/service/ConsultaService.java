package com.emprendehub.service;

import com.emprendehub.dto.ConsultaResponse;
import com.emprendehub.dto.EnviarConsultaRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Consulta;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.TipoNotificacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.ConsultaRepository;
import com.emprendehub.repository.NegocioRepository;
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
 *   <li><strong>D2</strong> — no se responde desde aquí. El buzón enseña el
 *       correo del cliente y la conversación sigue fuera.</li>
 *   <li><strong>D3</strong> — leídas y no leídas, que es lo que convierte el
 *       buzón en una lista de pendientes y no en un archivo.</li>
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

    public ConsultaService(ConsultaRepository consultaRepository,
                           NegocioRepository negocioRepository,
                           NotificacionService notificacionService) {
        this.consultaRepository = consultaRepository;
        this.negocioRepository = negocioRepository;
        this.notificacionService = notificacionService;
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
                consulta.getFechaEnvio(), consulta.getFechaLectura());
    }
}
