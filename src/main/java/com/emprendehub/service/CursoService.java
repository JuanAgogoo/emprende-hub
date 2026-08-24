package com.emprendehub.service;

import com.emprendehub.dto.ActualizarCursoRequest;
import com.emprendehub.dto.CrearCursoRequest;
import com.emprendehub.dto.CursoResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.Curso;
import com.emprendehub.model.EstadoCurso;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.repository.CursoRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio del catálogo de cursos.
 *
 * <p>Las dos reglas que gobiernan todo lo demás:
 * <ul>
 *   <li>Un curso gratuito no lleva precio y uno de pago está obligado a
 *       llevarlo. La plataforma no cobra (E1), pero el importe se muestra.</li>
 *   <li>El catálogo público solo enseña los publicados (E4). Los borradores
 *       existen únicamente para el administrador.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CursoService {

    private final CursoRepository repositorio;

    public CursoService(CursoRepository repositorio) {
        this.repositorio = repositorio;
    }

    // ---------- Catálogo público ----------

    /** Busca entre los cursos publicados. Todos los filtros son opcionales. */
    public Page<CursoResponse> buscarPublicados(CategoriaCurso categoria, NivelCurso nivel,
                                                Boolean gratuito, String texto, Pageable pageable) {
        return repositorio
                .buscar(EstadoCurso.PUBLICADO, categoria, nivel, gratuito, normalizar(texto), pageable)
                .map(this::aRespuesta);
    }

    /** Un curso publicado. Un borrador se comporta como si no existiera. */
    public CursoResponse obtenerPublicado(Long id) {
        return repositorio.findByIdAndEstado(id, EstadoCurso.PUBLICADO)
                .map(this::aRespuesta)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", id));
    }

    // ---------- Gestión del administrador ----------

    /** Todos los cursos, borradores incluidos, con los mismos filtros. */
    public Page<CursoResponse> buscarTodos(CategoriaCurso categoria, NivelCurso nivel,
                                           Boolean gratuito, String texto, Pageable pageable) {
        return repositorio.buscar(null, categoria, nivel, gratuito, normalizar(texto), pageable)
                .map(this::aRespuesta);
    }

    public CursoResponse obtener(Long id) {
        return aRespuesta(buscarEntidad(id));
    }

    @Transactional
    public CursoResponse crear(CrearCursoRequest peticion) {
        validarPrecio(peticion.gratuito(), peticion.precio());

        Curso curso = new Curso(
                peticion.titulo(), peticion.descripcion(), peticion.duracion(),
                peticion.categoria(), peticion.nivel(), peticion.gratuito(),
                peticion.gratuito() ? null : peticion.precio(),
                peticion.urlRecurso(), peticion.emoji());

        return aRespuesta(repositorio.save(curso));
    }

    @Transactional
    public CursoResponse actualizar(Long id, ActualizarCursoRequest peticion) {
        validarPrecio(peticion.gratuito(), peticion.precio());

        Curso curso = buscarEntidad(id);
        curso.setTitulo(peticion.titulo());
        curso.setDescripcion(peticion.descripcion());
        curso.setDuracion(peticion.duracion());
        curso.setCategoria(peticion.categoria());
        curso.setNivel(peticion.nivel());
        curso.setGratuito(peticion.gratuito());
        curso.setPrecio(peticion.gratuito() ? null : peticion.precio());
        curso.setUrlRecurso(peticion.urlRecurso());
        curso.setEmoji(peticion.emoji());

        return aRespuesta(repositorio.save(curso));
    }

    /** Publicar es un acto deliberado: un curso nace siempre en borrador. */
    @Transactional
    public CursoResponse publicar(Long id) {
        Curso curso = buscarEntidad(id);
        if (curso.estaPublicado()) {
            throw new ReglaDeNegocioException("El curso ya estaba publicado");
        }
        curso.setEstado(EstadoCurso.PUBLICADO);
        return aRespuesta(repositorio.save(curso));
    }

    /** Retirar del catálogo sin borrar: vuelve a borrador. */
    @Transactional
    public CursoResponse pasarABorrador(Long id) {
        Curso curso = buscarEntidad(id);
        if (!curso.estaPublicado()) {
            throw new ReglaDeNegocioException("El curso ya estaba en borrador");
        }
        curso.setEstado(EstadoCurso.BORRADOR);
        return aRespuesta(repositorio.save(curso));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repositorio.existsById(id)) {
            throw new ResourceNotFoundException("Curso", id);
        }
        repositorio.deleteById(id);
    }

    // ---------- Apoyo ----------

    /**
     * Coherencia entre gratuidad y precio.
     *
     * <p>Bean Validation no puede comprobarlo porque depende de dos campos a la
     * vez, así que la regla vive aquí.
     */
    private void validarPrecio(boolean gratuito, BigDecimal precio) {
        if (gratuito && precio != null) {
            throw new ReglaDeNegocioException("Un curso gratuito no puede llevar precio");
        }
        if (!gratuito && precio == null) {
            throw new ReglaDeNegocioException("Un curso de pago tiene que llevar precio");
        }
    }

    private Curso buscarEntidad(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", id));
    }

    /** Un texto en blanco no filtra nada, así que se trata como ausente. */
    private String normalizar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private CursoResponse aRespuesta(Curso curso) {
        return new CursoResponse(
                curso.getId(), curso.getTitulo(), curso.getDescripcion(), curso.getDuracion(),
                curso.getCategoria().name(), curso.getNivel().name(), curso.isGratuito(),
                curso.getPrecio(), curso.getUrlRecurso(), curso.getEmoji(),
                curso.getEstado().name());
    }
}
