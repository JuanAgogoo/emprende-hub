package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class NegocioRepositoryTest extends PostgresTestBase {

    private static final String DESCRIPCION =
            "Panadería artesanal con recetas familiares de más de cincuenta años, "
                    + "pan de masa madre horneado cada mañana en horno de leña.";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NegocioRepository repository;

    private Long idUsuarioConNegocio;
    private Long idUsuarioSinNegocio;

    @BeforeEach
    void prepararDatos() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        Barrio poblado = new Barrio("El Poblado");
        poblado.setCiudad(medellin);
        entityManager.persistAndFlush(poblado);
        CategoriaNegocio gastronomia =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));

        Usuario duena = entityManager.persistAndFlush(
                new Usuario("María", "maria@test.co", "hash", Rol.EMPRENDEDOR));
        Usuario suelta = entityManager.persistAndFlush(
                new Usuario("Carlos", "carlos@test.co", "hash", Rol.CLIENTE));

        entityManager.persistAndFlush(new Negocio(duena, "Panadería La Tradicional",
                DESCRIPCION, "3001234567", gastronomia, medellin, poblado, NivelPrecio.MEDIO));

        idUsuarioConNegocio = duena.getId();
        idUsuarioSinNegocio = suelta.getId();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByUsuarioId: devuelve el negocio de esa cuenta")
    void findByUsuarioId_conNegocio_loDevuelve() {
        var negocio = repository.findByUsuarioId(idUsuarioConNegocio);

        assertTrue(negocio.isPresent());
        assertEquals("Panadería La Tradicional", negocio.get().getNombre());
    }

    @Test
    @DisplayName("findByUsuarioId: trae resueltas categoría, ciudad y barrio")
    void findByUsuarioId_traeLasRelacionesResueltas() {
        // Sin el @EntityGraph esto fallaría fuera de la transacción, porque
        // open-in-view está desactivado.
        var negocio = repository.findByUsuarioId(idUsuarioConNegocio).orElseThrow();

        assertNotNull(negocio.getCategoria().getNombre());
        assertNotNull(negocio.getCiudad().getNombre());
        assertEquals("El Poblado", negocio.getBarrio().getNombre());
    }

    @Test
    @DisplayName("findByUsuarioId: una cuenta sin negocio devuelve vacío")
    void findByUsuarioId_sinNegocio_devuelveVacio() {
        assertTrue(repository.findByUsuarioId(idUsuarioSinNegocio).isEmpty());
    }

    @Test
    @DisplayName("existsByUsuarioId: distingue quién tiene negocio y quién no")
    void existsByUsuarioId_distingueAmbosCasos() {
        assertTrue(repository.existsByUsuarioId(idUsuarioConNegocio));
        assertFalse(repository.existsByUsuarioId(idUsuarioSinNegocio));
    }

    @Test
    @DisplayName("El negocio guardado nace pendiente y sin calificación")
    void negocioGuardado_naceePendienteYSinCalificacion() {
        var negocio = repository.findByUsuarioId(idUsuarioConNegocio).orElseThrow();

        assertEquals("PENDIENTE", negocio.getEstado().name());
        assertFalse(negocio.esVisiblePublicamente());
        assertEquals(0, negocio.getNumeroOpiniones());
    }
}
