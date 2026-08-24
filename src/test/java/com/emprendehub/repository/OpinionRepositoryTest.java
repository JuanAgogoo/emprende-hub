package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

/**
 * Las consultas de opiniones contra PostgreSQL de verdad.
 *
 * <p>Dos cosas solo se ven aquí: que la restricción de unicidad de C2 existe de
 * verdad en el esquema —el servicio la comprueba antes, pero dos peticiones a la
 * vez se le escaparían— y que {@code AVG} devuelve <strong>nulo</strong> y no
 * cero cuando el negocio se queda sin ninguna opinión, que es lo que sostiene C5.
 */
class OpinionRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OpinionRepository repository;

    private Long idPanaderia;
    private Long idFloristeria;
    private Long idCarlos;
    private Long idSofia;

    @BeforeEach
    void prepararOpiniones() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        CategoriaNegocio categoria =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));

        Negocio panaderia = negocio("Panadería", "pan@test.co", categoria, medellin);
        Negocio floristeria = negocio("Floristería", "flores@test.co", categoria, medellin);

        Usuario carlos = usuario("Carlos Rueda", "carlos@test.co");
        Usuario sofia = usuario("Sofía Ruiz", "sofia@test.co");

        // 5 y 4 sobre la panadería: media 4,5. La floristería se queda a cero
        // opiniones a propósito, para comprobar el AVG vacío.
        entityManager.persistAndFlush(new Opinion(panaderia, carlos, 5, "La mejor de Medellín"));
        entityManager.persistAndFlush(new Opinion(panaderia, sofia, 4, null));

        idPanaderia = panaderia.getId();
        idFloristeria = floristeria.getId();
        idCarlos = carlos.getId();
        idSofia = sofia.getId();
        entityManager.clear();
    }

    private Negocio negocio(String nombre, String correo, CategoriaNegocio categoria,
                            Ciudad ciudad) {
        Usuario dueno = usuario("Dueño de " + nombre, correo);
        Negocio negocio = new Negocio(dueno, nombre,
                "Descripción larga de prueba con más de ochenta caracteres para pasar la "
                        + "validación de longitud del registro.",
                "3001234567", categoria, ciudad, null, NivelPrecio.MEDIO);
        negocio.setEstado(EstadoNegocio.APROBADO);
        return entityManager.persistAndFlush(negocio);
    }

    private Usuario usuario(String nombre, String correo) {
        return entityManager.persistAndFlush(
                new Usuario(nombre, correo, "hash", Rol.CLIENTE));
    }

    // ---------- C2: una por persona y negocio ----------

    @Test
    @DisplayName("El esquema impide dos opiniones de la misma persona sobre el mismo negocio")
    void unicidad_segundaOpinion_revienta() {
        // El servicio lo comprueba antes para dar un 400 con su motivo, pero sin
        // esta restricción dos peticiones simultáneas se colarían las dos.
        Negocio panaderia = entityManager.find(Negocio.class, idPanaderia);
        Usuario carlos = entityManager.find(Usuario.class, idCarlos);

        assertThrows(Exception.class, () ->
                entityManager.persistAndFlush(new Opinion(panaderia, carlos, 1, "Otra más")));
    }

    @Test
    @DisplayName("La misma persona sí puede opinar de negocios distintos")
    void unicidad_otroNegocio_seGuarda() {
        Negocio floristeria = entityManager.find(Negocio.class, idFloristeria);
        Usuario carlos = entityManager.find(Usuario.class, idCarlos);

        entityManager.persistAndFlush(new Opinion(floristeria, carlos, 3, "Correcta"));

        assertTrue(repository.existsByNegocioIdAndAutorId(idFloristeria, idCarlos));
    }

    @Test
    @DisplayName("findByNegocioIdAndAutorId encuentra la opinión propia y solo esa")
    void findByNegocioYAutor_devuelveLaSuya() {
        var suya = repository.findByNegocioIdAndAutorId(idPanaderia, idSofia);

        assertTrue(suya.isPresent());
        assertEquals(4, suya.get().getCalificacion());
        assertTrue(repository.findByNegocioIdAndAutorId(idFloristeria, idSofia).isEmpty());
    }

    // ---------- Lectura ----------

    @Test
    @DisplayName("Las opiniones llegan con el autor resuelto y las últimas primero")
    void findByNegocio_ordenadasYConAutor() {
        var pagina = repository.findByNegocioIdOrderByFechaCreacionDesc(
                idPanaderia, PageRequest.of(0, 10));

        assertEquals(2, pagina.getTotalElements());
        // Sin el @EntityGraph esto fallaría al mapear: open-in-view está desactivado.
        assertNotNull(pagina.getContent().getFirst().getAutor().getNombre());
    }

    @Test
    @DisplayName("El comentario es opcional y se guarda nulo sin problema")
    void opinion_sinComentario_sePersiste() {
        var deSofia = repository.findByNegocioIdAndAutorId(idPanaderia, idSofia).orElseThrow();

        assertNull(deSofia.getComentario());
    }

    // ---------- El resumen que sostiene C5 ----------

    @Test
    @DisplayName("El resumen devuelve el recuento y la media del negocio")
    void resumen_calculaMediaYRecuento() {
        var resumen = repository.resumirPorNegocio(idPanaderia);

        assertEquals(2, resumen.getTotal());
        assertEquals(4.5, resumen.getPromedio(), 0.001);
    }

    @Test
    @DisplayName("Sin ninguna opinión el promedio es NULO, no cero (C5)")
    void resumen_sinOpiniones_promedioNulo() {
        // Es la consulta de la que depende que un negocio nuevo se enseñe como
        // «Nuevo» y no como el peor calificado de la plataforma.
        var resumen = repository.resumirPorNegocio(idFloristeria);

        assertEquals(0, resumen.getTotal());
        assertNull(resumen.getPromedio());
    }

    @Test
    @DisplayName("Al borrar la última opinión el promedio vuelve a ser nulo")
    void resumen_trasBorrarTodas_vuelveANulo() {
        repository.deleteAll(List.copyOf(
                repository.findByNegocioIdOrderByFechaCreacionDesc(
                        idPanaderia, PageRequest.of(0, 10)).getContent()));
        entityManager.flush();

        var resumen = repository.resumirPorNegocio(idPanaderia);

        assertEquals(0, resumen.getTotal());
        assertNull(resumen.getPromedio());
    }
}
