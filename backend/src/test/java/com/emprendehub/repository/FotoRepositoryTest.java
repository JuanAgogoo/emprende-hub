package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.EstadoFoto;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Foto;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Las consultas de la galería contra PostgreSQL de verdad.
 *
 * <p>La que obliga a esta clase es {@code aprobarPendientes}: un {@code UPDATE}
 * con {@code @Modifying} no se parece a nada que una prueba con el repositorio
 * simulado pueda comprobar, y si dejara sin tocar alguna fila el negocio se
 * quedaría con fotos invisibles para siempre.
 */
class FotoRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FotoRepository repository;

    private Long idPanaderia;
    private Long idFloristeria;

    @BeforeEach
    void prepararGalerias() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        CategoriaNegocio gastronomia =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));

        Negocio panaderia = negocio("Panadería", "panaderia@test.co", gastronomia, medellin);
        Negocio floristeria = negocio("Floristería", "flores@test.co", gastronomia, medellin);

        // Dos aprobadas y una pendiente, con los órdenes desordenados a
        // propósito para que la consulta tenga que ordenarlos ella.
        foto(panaderia, "b.jpg", 1, EstadoFoto.APROBADA);
        foto(panaderia, "a.jpg", 0, EstadoFoto.APROBADA);
        foto(panaderia, "c.jpg", 2, EstadoFoto.PENDIENTE);
        foto(floristeria, "flor.jpg", 0, EstadoFoto.APROBADA);

        idPanaderia = panaderia.getId();
        idFloristeria = floristeria.getId();
        entityManager.clear();
    }

    private Negocio negocio(String nombre, String correo, CategoriaNegocio categoria,
                            Ciudad ciudad) {
        Usuario dueno = entityManager.persistAndFlush(
                new Usuario("Dueño de " + nombre, correo, "hash", Rol.EMPRENDEDOR));
        Negocio negocio = new Negocio(dueno, nombre,
                "Descripción larga de prueba con más de ochenta caracteres para pasar la "
                        + "validación de longitud del registro.",
                "3001234567", categoria, ciudad, null, NivelPrecio.MEDIO);
        negocio.setEstado(EstadoNegocio.APROBADO);
        return entityManager.persistAndFlush(negocio);
    }

    private void foto(Negocio negocio, String archivo, int orden, EstadoFoto estado) {
        Foto foto = new Foto(negocio, archivo, orden);
        foto.setEstado(estado);
        entityManager.persistAndFlush(foto);
    }

    private List<String> archivosDe(List<Foto> fotos) {
        return fotos.stream().map(Foto::getNombreArchivo).toList();
    }

    @Test
    @DisplayName("La galería del dueño llega ordenada e incluye las pendientes")
    void findByNegocioId_devuelveTodasOrdenadas() {
        assertEquals(List.of("a.jpg", "b.jpg", "c.jpg"),
                archivosDe(repository.findByNegocioIdOrderByOrdenAsc(idPanaderia)));
    }

    @Test
    @DisplayName("La galería pública deja fuera lo que no se ha revisado (B2)")
    void findByEstado_soloLasAprobadas() {
        assertEquals(List.of("a.jpg", "b.jpg"),
                archivosDe(repository.findByNegocioIdAndEstadoOrderByOrdenAsc(
                        idPanaderia, EstadoFoto.APROBADA)));
    }

    @Test
    @DisplayName("El máximo de seis cuenta también las pendientes (B9)")
    void countByNegocioId_cuentaTodas() {
        assertEquals(3, repository.countByNegocioId(idPanaderia));
        assertEquals(1, repository.countByNegocioId(idFloristeria));
    }

    @Test
    @DisplayName("Contar por estado dice cuántas espera publicar una propuesta")
    void countByEstado_cuentaLasPendientes() {
        assertEquals(1, repository.countByNegocioIdAndEstado(idPanaderia, EstadoFoto.PENDIENTE));
        assertEquals(0, repository.countByNegocioIdAndEstado(
                idFloristeria, EstadoFoto.PENDIENTE));
    }

    @Test
    @DisplayName("Una foto solo se encuentra desde el negocio al que pertenece")
    void findByIdAndNegocioId_noCruzaNegocios() {
        Foto ajena = repository.findByNegocioIdOrderByOrdenAsc(idFloristeria).getFirst();

        assertTrue(repository.findByIdAndNegocioId(ajena.getId(), idFloristeria).isPresent());
        assertTrue(repository.findByIdAndNegocioId(ajena.getId(), idPanaderia).isEmpty());
    }

    @Test
    @DisplayName("Las portadas de varios negocios se traen en una sola consulta")
    void findByNegocioIdIn_devuelveLasAprobadasDeCadaUno() {
        // Es lo que usa el directorio para poner portada a cada tarjeta sin
        // hacer una consulta por negocio.
        List<Foto> fotos = repository.findByNegocioIdInAndEstadoOrderByNegocioIdAscOrdenAsc(
                List.of(idPanaderia, idFloristeria), EstadoFoto.APROBADA);

        assertEquals(3, fotos.size());
        assertTrue(archivosDe(fotos).contains("flor.jpg"));
        assertTrue(!archivosDe(fotos).contains("c.jpg"), "la pendiente no es portada de nadie");
    }

    @Test
    @DisplayName("Aprobar publica las pendientes de ese negocio y solo las de ese")
    void aprobarPendientes_soloTocaSuNegocio() {
        int publicadas = repository.aprobarPendientes(idPanaderia);
        entityManager.clear();

        assertEquals(1, publicadas);
        assertEquals(3, repository.findByNegocioIdAndEstadoOrderByOrdenAsc(
                idPanaderia, EstadoFoto.APROBADA).size());
        assertEquals(1, repository.findByNegocioIdAndEstadoOrderByOrdenAsc(
                idFloristeria, EstadoFoto.APROBADA).size());
    }

    @Test
    @DisplayName("Aprobar sin nada pendiente no cambia ninguna fila")
    void aprobarPendientes_sinPendientes_noTocaNada() {
        assertEquals(0, repository.aprobarPendientes(idFloristeria));
    }
}
