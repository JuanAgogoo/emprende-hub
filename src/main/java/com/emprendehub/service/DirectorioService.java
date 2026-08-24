package com.emprendehub.service;

import com.emprendehub.dto.BusquedaDirectorioRequest;
import com.emprendehub.dto.NegocioPublicoResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.OrdenDirectorio;
import com.emprendehub.repository.NegocioRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El directorio público: búsqueda, destacados y perfil.
 *
 * <p>Se explora sin sesión, así que la única regla que gobierna todo lo demás es
 * de visibilidad: aquí solo existen los negocios aprobados de cuentas activas
 * (B6, B4). Eso lo garantizan las consultas del repositorio, no este servicio.
 *
 * <p>Lo que sí decide este servicio:
 * <ul>
 *   <li>Cómo se traduce cada opción de ordenación a columnas (G7).</li>
 *   <li>Qué se considera destacado (C7).</li>
 *   <li>Que un texto en blanco no filtra nada.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DirectorioService {

    /**
     * Opiniones mínimas para salir en Destacados (C7).
     *
     * <p>Sin ese mínimo, una sola opinión de 5★ desplazaría de la portada a un
     * negocio con 4,9 y 320 opiniones. No tiene ninguna relación con la
     * aprobación, donde exigir opiniones sería un bloqueo circular (B7).
     */
    private static final int MINIMO_OPINIONES_DESTACADO = 5;

    /** Cuántos destacados devuelve la portada si no se pide otra cosa. */
    private static final int DESTACADOS_POR_DEFECTO = 6;

    /** Tope de destacados. La portada es una fila, no un segundo directorio. */
    private static final int DESTACADOS_MAXIMO = 12;

    private final NegocioRepository repositorio;

    public DirectorioService(NegocioRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Busca en el directorio. Todos los filtros son opcionales y combinables.
     *
     * <p>La ordenación no se toma del {@code Pageable}: se construye aquí a
     * partir del criterio pedido, de modo que el cliente elige entre tres
     * opciones y no entre cualquier columna de la tabla.
     */
    public Page<NegocioPublicoResponse> buscar(BusquedaDirectorioRequest filtros,
                                               OrdenDirectorio orden, Pageable pageable) {
        Pageable pagina = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), aOrdenacion(orden));

        return repositorio.buscarEnDirectorio(
                        normalizar(filtros.texto()), filtros.categoriaId(), filtros.ciudadId(),
                        filtros.barrioId(), filtros.calificacionMinima(), filtros.nivelPrecio(),
                        pagina)
                .map(NegocioMapper::aRespuestaPublica);
    }

    /**
     * Los destacados de la portada: mejor calificados con al menos cinco
     * opiniones (C7).
     */
    public List<NegocioPublicoResponse> destacados(Integer limite) {
        Pageable cuantos = PageRequest.of(0, acotarLimite(limite));

        return repositorio.buscarDestacados(MINIMO_OPINIONES_DESTACADO, cuantos).stream()
                .map(NegocioMapper::aRespuestaPublica)
                .toList();
    }

    /**
     * El perfil público de un negocio.
     *
     * <p>Uno pendiente, rechazado o de una cuenta suspendida responde 404 y no
     * 403: no se filtra información sobre lo que existe sin publicar (B6).
     */
    public NegocioPublicoResponse obtenerPerfilPublico(Long id) {
        return repositorio.buscarVisibleEnDirectorio(id)
                .map(NegocioMapper::aRespuestaPublica)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", id));
    }

    // ---------- Apoyo ----------

    /**
     * Traduce el criterio elegido a columnas reales.
     *
     * <p>Dos detalles que no se ven en la interfaz:
     * <ul>
     *   <li>«Más recientes» ordena por <strong>fecha de aprobación</strong>, que
     *       es cuando el negocio apareció de verdad en el directorio, no por la
     *       de creación (G7).</li>
     *   <li>Los nulos van al final. Un negocio sin opiniones no tiene
     *       calificación (C5) y PostgreSQL coloca los nulos primero al ordenar
     *       de mayor a menor, así que sin esto encabezarían la lista de mejor
     *       calificados justamente por no tener ninguna.</li>
     * </ul>
     *
     * <p>El {@code switch} es exhaustivo: añadir un cuarto criterio rompe la
     * compilación aquí en vez de caer en un orden por defecto silencioso.
     */
    private Sort aOrdenacion(OrdenDirectorio orden) {
        OrdenDirectorio elegido = orden == null ? OrdenDirectorio.CALIFICACION : orden;

        return switch (elegido) {
            case CALIFICACION -> Sort.by(
                    Sort.Order.desc("calificacionPromedio").nullsLast(),
                    Sort.Order.desc("numeroOpiniones"));
            case NOMBRE -> Sort.by(Sort.Order.asc("nombre"));
            case RECIENTES -> Sort.by(Sort.Order.desc("fechaAprobacion").nullsLast());
        };
    }

    /** Un texto en blanco no filtra nada, así que se trata como ausente. */
    private String normalizar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    /** Sin límite pedido van seis; uno absurdo se recorta en lugar de fallar. */
    private int acotarLimite(Integer limite) {
        if (limite == null) {
            return DESTACADOS_POR_DEFECTO;
        }
        return Math.clamp(limite, 1, DESTACADOS_MAXIMO);
    }
}
