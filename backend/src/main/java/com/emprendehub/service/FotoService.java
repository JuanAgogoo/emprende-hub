package com.emprendehub.service;

import com.emprendehub.config.AlmacenamientoFotos;
import com.emprendehub.dto.FotoResponse;
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
import java.util.List;
import java.util.Map;
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

    /** B9: cinco megas por imagen. */
    static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    /**
     * B9: solo JPG y PNG, con la extensión que le corresponde a cada uno.
     *
     * <p>El tipo sale de la cabecera, nunca del nombre del fichero: renombrar un
     * ejecutable a {@code .jpg} es trivial.
     */
    private static final Map<String, String> EXTENSION_POR_TIPO = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png");

    private final FotoRepository fotoRepository;
    private final NegocioRepository negocioRepository;
    private final CambioPendienteRepository cambioRepository;
    private final AlmacenamientoFotos almacenamiento;

    public FotoService(FotoRepository fotoRepository,
                       NegocioRepository negocioRepository,
                       CambioPendienteRepository cambioRepository,
                       AlmacenamientoFotos almacenamiento) {
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
        String extension = validar(archivo);

        long cuantas = fotoRepository.countByNegocioId(negocio.getId());
        if (cuantas >= MAXIMO_FOTOS) {
            throw new ReglaDeNegocioException(
                    "La galería admite " + MAXIMO_FOTOS + " fotos como mucho; "
                            + "borra alguna antes de subir otra");
        }

        Foto foto = new Foto(negocio, almacenamiento.guardar(archivo, extension), (int) cuantas);
        fotoRepository.save(foto);

        if (negocio.getEstado() == EstadoNegocio.APROBADO) {
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

    /** Devuelve la extensión que toca, o explica por qué el archivo no sirve. */
    private String validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaDeNegocioException("No llegó ninguna imagen");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ReglaDeNegocioException("Cada imagen puede pesar 5 MB como mucho");
        }

        String tipo = archivo.getContentType();
        String extension = tipo == null ? null : EXTENSION_POR_TIPO.get(tipo.toLowerCase());
        if (extension == null) {
            throw new ReglaDeNegocioException("La imagen tiene que ser JPG o PNG");
        }
        return extension;
    }

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
