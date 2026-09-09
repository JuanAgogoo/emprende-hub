package com.emprendehub.service;

import com.emprendehub.config.AlmacenamientoFotos;
import com.emprendehub.dto.FotoResponse;
import com.emprendehub.dto.ReordenarFotosRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CambioPendiente;
import com.emprendehub.model.EstadoFoto;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Foto;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.CambioPendienteRepository;
import com.emprendehub.repository.FotoRepository;
import com.emprendehub.repository.NegocioRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * La galería de un negocio (B9).
 *
 * <p>Las tres reglas del dominio, todas comprobadas aquí y no en el controlador:
 * <ul>
 *   <li><strong>Máximo seis</strong> por negocio, contando las que esperan
 *       revisión: el límite es del espacio de la galería, no de lo publicado.</li>
 *   <li><strong>JPG o PNG</strong>, cinco megas como mucho cada una.</li>
 *   <li><strong>La principal es la primera por orden</strong>, sin campo que la
 *       marque. Al borrar una, las siguientes se recolocan.</li>
 * </ul>
 *
 * <p>Una foto nueva nace pendiente. B2 manda a revisión los cambios sobre campos
 * públicos y nombra las fotos expresamente, pero B2-bis prohíbe que el negocio
 * salga del directorio mientras espera; con el estado en cada foto se cumplen
 * las dos: el público sigue viendo las de antes y la nueva aparece cuando el
 * administrador la aprueba.
 */
@Service
@Transactional(readOnly = true)
public class FotoService {

    /** B9: seis como mucho. Es lo que enseña el prototipo, con seis huecos. */
    static final int MAXIMO_FOTOS = 6;

    private final FotoRepository fotoRepository;
    private final NegocioRepository negocioRepository;
    private final CambioPendienteRepository cambioRepository;
    private final AlmacenamientoFotos almacenamiento;

    /**
     * El mismo interruptor provisional que usa {@code NegocioService}.
     *
     * <p>Tiene que aplicarse **también aquí**, y no solo al negocio: una foto
     * subida a un negocio ya aprobado nace pendiente por B2 y el público no la
     * ve, así que aprobar el negocio y dejar la foto esperando publicaría
     * negocios sin imagen. O no hay moderación, o la hay para las dos cosas.
     */
    private final boolean moderacionAutomatica;

    public FotoService(FotoRepository fotoRepository,
                       NegocioRepository negocioRepository,
                       CambioPendienteRepository cambioRepository,
                       AlmacenamientoFotos almacenamiento,
                       @Value("${emprendehub.moderacion.automatica:false}")
                       boolean moderacionAutomatica) {
        this.moderacionAutomatica = moderacionAutomatica;
        this.fotoRepository = fotoRepository;
        this.negocioRepository = negocioRepository;
        this.cambioRepository = cambioRepository;
        this.almacenamiento = almacenamiento;
    }

    /** La galería del dueño: también las que esperan revisión, que son suyas. */
    public List<FotoResponse> listarMias(Usuario solicitante) {
        Negocio negocio = buscarElMio(solicitante);
        return NegocioMapper.aRespuestasDeFoto(
                fotoRepository.findByNegocioIdOrderByOrdenAsc(negocio.getId()));
    }

    /**
     * Añade una imagen al final de la galería.
     *
     * <p>Si el negocio ya está publicado, la subida abre además una propuesta de
     * cambio: es la vía por la que el administrador la ve y la aprueba (B2-bis).
     * Si todavía está pendiente de su primera revisión no hace falta, porque sus
     * fotos se aprueban con él.
     *
     * <p>El fichero se escribe antes de guardar la fila. Si la transacción
     * fallara después, quedaría un fichero suelto en disco que nadie referencia:
     * no afecta a lo que ve el usuario y limpiarlo no compensa la complejidad.
     */
    @Transactional
    public FotoResponse subir(Usuario solicitante, MultipartFile archivo) {
        Negocio negocio = buscarElMio(solicitante);
        String extension = AlmacenamientoFotos.extensionDe(archivo);

        long cuantas = fotoRepository.countByNegocioId(negocio.getId());
        if (cuantas >= MAXIMO_FOTOS) {
            throw new ReglaDeNegocioException(
                    "La galería admite " + MAXIMO_FOTOS + " fotos como mucho; "
                            + "borra alguna antes de subir otra");
        }

        Foto foto = new Foto(negocio, almacenamiento.guardar(archivo, extension), (int) cuantas);
        if (moderacionAutomatica) {
            foto.setEstado(EstadoFoto.APROBADA);
        }
        fotoRepository.save(foto);

        if (!moderacionAutomatica && negocio.getEstado() == EstadoNegocio.APROBADO) {
            abrirPropuestaDeCambio(negocio);
        }

        return NegocioMapper.aRespuestaDeFoto(foto, foto.getOrden() == 0);
    }

    /**
     * Quita una foto de la galería, sin pasar por revisión.
     *
     * <p>Borrar no publica nada nuevo, que es el riesgo que B2 controla, así que
     * obligar a esperar tres días para retirar una imagen equivocada solo la
     * mantendría más tiempo a la vista.
     */
    @Transactional
    public void eliminar(Usuario solicitante, Long fotoId) {
        Negocio negocio = buscarElMio(solicitante);
        Foto foto = fotoRepository.findByIdAndNegocioId(fotoId, negocio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Foto", fotoId));

        fotoRepository.delete(foto);
        almacenamiento.borrar(foto.getNombreArchivo());
        recolocar(negocio.getId());
    }

    /**
     * Cambia el orden de la galería, y con él la portada (B9).
     *
     * <p><strong>No pasa por revisión</strong>, por la misma razón que borrar:
     * reordenar no publica ninguna imagen que no estuviera ya publicada, que es
     * el riesgo que controla B2. Y si la que sube al primer puesto todavía
     * espera revisión, el público sigue viendo como portada la primera de las
     * aprobadas, porque el perfil solo recibe esas.
     *
     * <p>Se exige la lista <em>completa</em>: ni una foto de menos ni una de
     * más. Aceptar una parcial obligaría a decidir dónde van las que faltan, y
     * cualquier respuesta a eso sería una invención.
     */
    @Transactional
    public List<FotoResponse> reordenar(Usuario solicitante, ReordenarFotosRequest peticion) {
        Negocio negocio = buscarElMio(solicitante);
        List<Foto> galeria = fotoRepository.findByNegocioIdOrderByOrdenAsc(negocio.getId());

        Map<Long, Foto> porId = new HashMap<>();
        galeria.forEach(foto -> porId.put(foto.getId(), foto));

        // El mapa hace las dos comprobaciones a la vez: que no sobre ninguna
        // —cada id tiene que estar en la galería— y que no falte ni se repita
        // —al final el mapa tiene que quedar vacío—.
        for (int posicion = 0; posicion < peticion.orden().size(); posicion++) {
            Foto foto = porId.remove(peticion.orden().get(posicion));
            if (foto == null) {
                throw new ReglaDeNegocioException(rechazo(galeria.size()));
            }
            foto.setOrden(posicion);
        }
        if (!porId.isEmpty()) {
            throw new ReglaDeNegocioException(rechazo(galeria.size()));
        }

        return NegocioMapper.aRespuestasDeFoto(
                fotoRepository.findByNegocioIdOrderByOrdenAsc(negocio.getId()));
    }

    private static String rechazo(int cuantas) {
        return "El orden tiene que nombrar exactamente las " + cuantas
                + " fotos de la galería, una sola vez cada una";
    }

    // ---------- Revisión, invocada por la moderación ----------

    /**
     * Publica las fotos que esperaban revisión.
     *
     * <p>La llama {@code ModeracionService} en los dos momentos en que el
     * administrador da el visto bueno: al aprobar el negocio por primera vez y
     * al aprobar una propuesta de cambio posterior.
     */
    @Transactional
    public int aprobarPendientes(Long negocioId) {
        return fotoRepository.aprobarPendientes(negocioId);
    }

    /**
     * Descarta las fotos que el administrador no aceptó, con sus ficheros.
     *
     * <p>Se borran en vez de dejarlas pendientes para siempre: si se quedaran,
     * la siguiente propuesta de cambio del dueño las publicaría de rebote, que
     * es justo lo que el administrador acaba de impedir.
     */
    @Transactional
    public int descartarPendientes(Long negocioId) {
        List<Foto> pendientes =
                fotoRepository.findByNegocioIdAndEstado(negocioId, EstadoFoto.PENDIENTE);
        if (pendientes.isEmpty()) {
            return 0;
        }

        fotoRepository.deleteAll(pendientes);
        pendientes.forEach(foto -> almacenamiento.borrar(foto.getNombreArchivo()));
        recolocar(negocioId);
        return pendientes.size();
    }

    // ---------- Apoyo ----------

    /**
     * Deja los órdenes en 0, 1, 2… sin huecos.
     *
     * <p>Importa por B9: si al borrar la primera las demás conservaran su orden,
     * la portada del negocio sería la que tuviera el número más bajo de los que
     * quedan, y bastaría con eso; pero los huecos harían que subir una nueva
     * repitiera un orden ya usado y el desempate quedaría al azar.
     */
    private void recolocar(Long negocioId) {
        List<Foto> quedan = fotoRepository.findByNegocioIdOrderByOrdenAsc(negocioId);
        for (int i = 0; i < quedan.size(); i++) {
            quedan.get(i).setOrden(i);
        }
        fotoRepository.saveAll(quedan);
    }

    /**
     * Abre la propuesta de cambio, o refresca la que ya hubiera.
     *
     * <p>Una propuesta puede llevar texto, fotos o las dos cosas. Cuando solo
     * cambian las fotos se guardan el nombre y la descripción actuales: al
     * aprobar se copian sobre sí mismos y lo que se publica de verdad son las
     * imágenes.
     */
    private void abrirPropuestaDeCambio(Negocio negocio) {
        if (cambioRepository.findByNegocioId(negocio.getId()).isEmpty()) {
            cambioRepository.save(new CambioPendiente(
                    negocio, negocio.getNombre(), negocio.getDescripcion()));
        }
    }

    private Negocio buscarElMio(Usuario solicitante) {
        return negocioRepository.findByUsuarioId(solicitante.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Negocio del usuario", solicitante.getId()));
    }
}
