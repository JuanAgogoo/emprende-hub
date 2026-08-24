package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Producto;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class ProductoRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductoRepository repository;

    private Long idPanaderia;
    private Long idFloristeria;
    private Long idProductoAjeno;

    @BeforeEach
    void prepararEscaparates() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        CategoriaNegocio categoria =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));

        Negocio panaderia = negocio("Panadería", "panaderia@test.co", categoria, medellin);
        Negocio floristeria = negocio("Floristería", "flores@test.co", categoria, medellin);

        producto(panaderia, "Croissant", new BigDecimal("5500.00"), "De mantequilla", true);
        producto(panaderia, "Pan de masa madre", new BigDecimal("12000.00"), null, false);
        Producto ajeno = producto(floristeria, "Ramo de girasoles",
                new BigDecimal("45000.00"), null, true);

        idPanaderia = panaderia.getId();
        idFloristeria = floristeria.getId();
        idProductoAjeno = ajeno.getId();
        entityManager.clear();
    }

    private Negocio negocio(String nombre, String correo, CategoriaNegocio categoria,
                            Ciudad ciudad) {
        Usuario dueno = entityManager.persistAndFlush(
                new Usuario("Dueño de " + nombre, correo, "hash", Rol.EMPRENDEDOR));
        return entityManager.persistAndFlush(new Negocio(dueno, nombre,
                "Descripción larga de prueba con más de ochenta caracteres para pasar la "
                        + "validación de longitud del registro.",
                "3001234567", categoria, ciudad, null, NivelPrecio.MEDIO));
    }

    private Producto producto(Negocio negocio, String nombre, BigDecimal precio,
                              String descripcion, boolean disponible) {
        return entityManager.persistAndFlush(
                new Producto(negocio, nombre, precio, descripcion, disponible));
    }

    @Test
    @DisplayName("El escaparate de un negocio llega ordenado por nombre")
    void findByNegocioId_ordenadoPorNombre() {
        List<Producto> escaparate = repository.findByNegocioIdOrderByNombreAsc(idPanaderia);

        assertEquals(List.of("Croissant", "Pan de masa madre"),
                escaparate.stream().map(Producto::getNombre).toList());
    }

    @Test
    @DisplayName("Un producto no disponible sigue en el escaparate, marcado (F3)")
    void findByNegocioId_incluyeLosNoDisponibles() {
        Producto pan = repository.findByNegocioIdOrderByNombreAsc(idPanaderia).getLast();

        assertEquals("Pan de masa madre", pan.getNombre());
        assertTrue(!pan.isDisponible());
    }

    @Test
    @DisplayName("La descripción es opcional y se guarda nula sin problema (F2)")
    void producto_sinDescripcion_sePersiste() {
        Producto pan = repository.findByNegocioIdOrderByNombreAsc(idPanaderia).getLast();

        assertEquals(null, pan.getDescripcion());
        assertEquals(0, new BigDecimal("12000.00").compareTo(pan.getPrecio()));
    }

    @Test
    @DisplayName("Un producto solo se encuentra desde su propio negocio")
    void findByIdAndNegocioId_noCruzaNegocios() {
        assertTrue(repository.findByIdAndNegocioId(idProductoAjeno, idFloristeria).isPresent());
        assertTrue(repository.findByIdAndNegocioId(idProductoAjeno, idPanaderia).isEmpty());
    }
}
