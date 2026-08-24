package com.emprendehub.service;

import com.emprendehub.dto.BarrioResponse;
import com.emprendehub.dto.CategoriaNegocioResponse;
import com.emprendehub.dto.CiudadResponse;
import com.emprendehub.dto.OpcionResponse;
import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de los catálogos fijos del sistema.
 *
 * <p>Son de solo lectura: las 12 categorías de negocio, las ciudades del Valle
 * de Aburrá con sus barrios y los conjuntos cerrados de los cursos. Nadie los
 * modifica desde la aplicación (G5); los carga
 * {@code CargaInicialCatalogos} al arrancar.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final CategoriaNegocioRepository categoriaNegocioRepository;
    private final CiudadRepository ciudadRepository;

    public CatalogoService(CategoriaNegocioRepository categoriaNegocioRepository,
                           CiudadRepository ciudadRepository) {
        this.categoriaNegocioRepository = categoriaNegocioRepository;
        this.ciudadRepository = ciudadRepository;
    }

    /** Las 12 categorías de negocio de la decisión G1, ordenadas por nombre. */
    public List<CategoriaNegocioResponse> obtenerCategoriasNegocio() {
        return categoriaNegocioRepository.findAllByOrderByNombreAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    /** Ciudades con sus barrios anidados, los dos niveles de G3 de una vez. */
    public List<CiudadResponse> obtenerCiudades() {
        return ciudadRepository.findAllByOrderByNombreAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    public List<OpcionResponse> obtenerCategoriasCurso() {
        return Arrays.stream(CategoriaCurso.values())
                .map(c -> new OpcionResponse(c.name(), c.getNombre()))
                .toList();
    }

    public List<OpcionResponse> obtenerNivelesCurso() {
        return Arrays.stream(NivelCurso.values())
                .map(n -> new OpcionResponse(n.name(), n.getNombre()))
                .toList();
    }

    private CategoriaNegocioResponse aRespuesta(CategoriaNegocio categoria) {
        return new CategoriaNegocioResponse(
                categoria.getId(), categoria.getNombre(), categoria.getIcono());
    }

    private CiudadResponse aRespuesta(Ciudad ciudad) {
        List<BarrioResponse> barrios = ciudad.getBarrios().stream()
                .map(b -> new BarrioResponse(b.getId(), b.getNombre()))
                .sorted((a, b) -> a.nombre().compareTo(b.nombre()))
                .toList();
        return new CiudadResponse(ciudad.getId(), ciudad.getNombre(), barrios);
    }
}
