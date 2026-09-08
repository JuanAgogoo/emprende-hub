package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.Barrio;
import com.emprendehub.model.Ciudad;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class CiudadRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CiudadRepository repository;

    @BeforeEach
    void prepararDatos() {
        Ciudad medellin = new Ciudad("Medellín");
        medellin.agregarBarrio(new Barrio("El Poblado"));
        medellin.agregarBarrio(new Barrio("Laureles"));
        entityManager.persistAndFlush(medellin);
        entityManager.persistAndFlush(new Ciudad("Envigado"));
        entityManager.clear();
    }

    @Test
    @DisplayName("findAllByOrderByNombreAsc: devuelve las ciudades ordenadas por nombre")
    void findAllOrdenado_devuelveCiudadesOrdenadas() {
        List<Ciudad> ciudades = repository.findAllByOrderByNombreAsc();

        assertEquals(2, ciudades.size());
        assertEquals("Envigado", ciudades.get(0).getNombre());
        assertEquals("Medellín", ciudades.get(1).getNombre());
    }

    @Test
    @DisplayName("findAllByOrderByNombreAsc: trae los barrios resueltos por el EntityGraph")
    void findAllOrdenado_traeLosBarriosResueltos() {
        // Sin el @EntityGraph esta colección estaría sin inicializar y fallaría
        // fuera de la transacción, porque open-in-view está desactivado.
        Ciudad medellin = repository.findAllByOrderByNombreAsc().stream()
                .filter(c -> c.getNombre().equals("Medellín"))
                .findFirst()
                .orElseThrow();

        assertEquals(2, medellin.getBarrios().size());
    }

    @Test
    @DisplayName("findByNombre: encuentra la ciudad cuando existe")
    void findByNombre_existente_devuelveLaCiudad() {
        assertTrue(repository.findByNombre("Envigado").isPresent());
    }

    @Test
    @DisplayName("findByNombre: devuelve vacío cuando no existe")
    void findByNombre_inexistente_devuelveVacio() {
        assertTrue(repository.findByNombre("Sabaneta").isEmpty());
    }
}
