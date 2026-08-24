package com.emprendehub.config;

import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los catálogos fijos la primera vez que arranca la aplicación.
 *
 * <p>Es idempotente: si las tablas ya tienen datos no hace nada, así que
 * reiniciar no duplica nada.
 */
@Configuration
public class CargaInicialCatalogos {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialCatalogos.class);

    /** Las 12 categorías de la decisión G1, con el emoji del prototipo. */
    private static final Map<String, String> CATEGORIAS = new java.util.LinkedHashMap<>(Map.of());

    static {
        CATEGORIAS.put("Gastronomía", "🍴");
        CATEGORIAS.put("Moda", "👗");
        CATEGORIAS.put("Tecnología", "💻");
        CATEGORIAS.put("Belleza", "💄");
        CATEGORIAS.put("Artesanías", "🎨");
        CATEGORIAS.put("Salud y bienestar", "🌿");
        CATEGORIAS.put("Educación", "📚");
        CATEGORIAS.put("Hogar", "🏠");
        CATEGORIAS.put("Finanzas", "💰");
        CATEGORIAS.put("Deportes", "⚽");
        CATEGORIAS.put("Mascotas", "🐾");
        CATEGORIAS.put("Eventos", "🎉");
    }

    /**
     * Municipios del Valle de Aburrá (G2) con sus barrios (G3).
     *
     * <p>Solo Medellín tiene barrios: son los únicos que aparecen en el
     * prototipo. No se inventan los de los demás municipios.
     */
    private static final Map<String, List<String>> CIUDADES = Map.of(
            "Medellín", List.of("El Poblado", "Laureles"),
            "Envigado", List.of(),
            "Itagüí", List.of(),
            "Bello", List.of());

    @Bean
    @Transactional
    public CommandLineRunner cargarCatalogos(CategoriaNegocioRepository categoriaRepository,
                                             CiudadRepository ciudadRepository) {
        return args -> {
            cargarCategorias(categoriaRepository);
            cargarCiudades(ciudadRepository);
        };
    }

    private void cargarCategorias(CategoriaNegocioRepository repositorio) {
        if (repositorio.count() > 0) {
            log.info("Las categorías de negocio ya estaban cargadas, no se toca nada.");
            return;
        }
        CATEGORIAS.forEach((nombre, icono) ->
                repositorio.save(new CategoriaNegocio(nombre, icono)));
        log.info("Cargadas {} categorías de negocio.", CATEGORIAS.size());
    }

    private void cargarCiudades(CiudadRepository repositorio) {
        if (repositorio.count() > 0) {
            log.info("Las ciudades ya estaban cargadas, no se toca nada.");
            return;
        }
        CIUDADES.forEach((nombreCiudad, barrios) -> {
            Ciudad ciudad = new Ciudad(nombreCiudad);
            barrios.forEach(nombreBarrio -> ciudad.agregarBarrio(new Barrio(nombreBarrio)));
            repositorio.save(ciudad);
        });
        log.info("Cargadas {} ciudades del Valle de Aburrá.", CIUDADES.size());
    }
}
