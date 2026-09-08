package com.emprendehub.service;

import com.emprendehub.dto.NotificacionResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Notificacion;
import com.emprendehub.model.TipoNotificacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.NotificacionRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los avisos del panel del emprendedor (H2, H3).
 *
 * <p>Cada notificación <strong>nace de un hecho</strong> —una opinión, una
 * consulta, una decisión del administrador— y la crea el servicio donde ese
 * hecho ocurre. Aquí solo se guardan, se leen y se marcan.
 *
 * <p>Sin correos (I1) esto no es un adorno: el panel es el único sitio donde el
 * emprendedor se entera de que le rechazaron el negocio o de que alguien le
 * escribió.
 *
 * <p>Fuera quedan los hitos de visitas que enseñaba el prototipo. No son un
 * hecho del que haya que enterarse, y H2 los descarta expresamente.
 */
@Service
@Transactional(readOnly = true)
public class NotificacionService {

    private final NotificacionRepository repositorio;

    public NotificacionService(NotificacionRepository repositorio) {
        this.repositorio = repositorio;
    }

    // ---------- Creación, desde los hechos ----------

    /**
     * Deja un aviso para alguien.
     *
     * <p>El texto se compone aquí y se guarda tal cual, en vez de rehacerlo al
     * leerlo: si después se borra la opinión que lo provocó, el aviso sigue
     * diciendo lo que pasó en su momento.
     *
     * <p>Recorta al máximo de la columna en lugar de fallar. Que un nombre largo
     * tumbe la publicación de una opinión sería absurdo: el aviso es lo
     * accesorio.
     */
    @Transactional
    public void avisar(Usuario destinatario, TipoNotificacion tipo, String texto) {
        repositorio.save(new Notificacion(destinatario, tipo, recortar(texto)));
    }

    // ---------- Consulta y marcado ----------

    /**
     * Los avisos propios, los más recientes primero.
     *
     * <p>Con {@code leida} a {@code false} llegan solo los pendientes, y el
     * total de esa página es el número del icono de la campana.
     */
    public Page<NotificacionResponse> mias(Usuario solicitante, Boolean leida,
                                           Pageable pageable) {
        return repositorio.buscarDe(solicitante.getId(), leida, pageable)
                .map(this::aRespuesta);
    }

    /** Marca un aviso como leído, o lo devuelve a pendiente (H3). */
    @Transactional
    public NotificacionResponse marcarLectura(Usuario solicitante, Long notificacionId,
                                              boolean leida) {
        Notificacion notificacion = repositorio
                .findByIdAndDestinatarioId(notificacionId, solicitante.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación", notificacionId));

        notificacion.setLeida(leida);

        return aRespuesta(repositorio.save(notificacion));
    }

    /**
     * Marca todo lo pendiente de una vez.
     *
     * <p>Es lo que hace cualquiera al abrir la campana con quince avisos
     * acumulados, y sin este atajo tendría que ir uno por uno.
     *
     * @return cuántos avisos se marcaron
     */
    @Transactional
    public int marcarTodasLeidas(Usuario solicitante) {
        List<Notificacion> pendientes =
                repositorio.findByDestinatarioIdAndLeidaFalse(solicitante.getId());

        pendientes.forEach(n -> n.setLeida(true));
        repositorio.saveAll(pendientes);

        return pendientes.size();
    }

    // ---------- Apoyo ----------

    private String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= Notificacion.MAXIMO_TEXTO
                ? texto
                : texto.substring(0, Notificacion.MAXIMO_TEXTO - 1) + "…";
    }

    private NotificacionResponse aRespuesta(Notificacion notificacion) {
        return new NotificacionResponse(notificacion.getId(), notificacion.getTipo().name(),
                notificacion.getTexto(), notificacion.isLeida(), notificacion.getFecha());
    }
}
