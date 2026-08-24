package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.Curso;
import com.emprendehub.model.EstadoCurso;
import com.emprendehub.model.NivelCurso;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class CursoRepositoryTest extends PostgresTestBase {

    private static final Pageable PRIMERA_PAGINA = PageRequest.of(0, 12);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CursoRepository repository;

    @BeforeEach
    void prepararDatos() {
        // Publicado, gratuito, marketing, básico
        guardar("Marketing Digital Básico", "Fundamentos de redes sociales",
                CategoriaCurso.MARKETING, NivelCurso.BASICO, true, null, EstadoCurso.PUBLICADO);
        // Publicado, de pago, ventas, intermedio
        guardar("Estrategias de Ventas", "Técnicas de cierre y fidelización",
                CategoriaCurso.VENTAS, NivelCurso.INTERMEDIO, false,
                new BigDecimal("49000"), EstadoCurso.PUBLICADO);
        // En borrador: no debe salir en el catálogo público
        guardar("Automatización con IA", "Herramientas de IA para tu negocio",
                CategoriaCurso.DIGITAL, NivelCurso.AVANZADO, false,
                new BigDecimal("79000"), EstadoCurso.BORRADOR);
        entityManager.clear();
    }

    private void guardar(String titulo, String descripcion, CategoriaCurso categoria,
                         NivelCurso nivel, boolean gratuito, BigDecimal precio,
                         EstadoCurso estado) {
        Curso curso = new Curso(titulo, descripcion, "4 horas", categoria, nivel,
                gratuito, precio, "https://ejemplo.co", "📚");
        curso.setEstado(estado);
        entityManager.persistAndFlush(curso);
    }

    @Test
    @DisplayName("buscar: con estado PUBLICADO deja fuera los borradores")
    void buscar_publicados_excluyeBorradores() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, null, null, null, null,
                PRIMERA_PAGINA);

        assertEquals(2, resultado.getTotalElements());
        assertTrue(resultado.getContent().stream().allMatch(Curso::estaPublicado));
    }

    @Test
    @DisplayName("buscar: con estado nulo devuelve también los borradores")
    void buscar_sinEstado_incluyeBorradores() {
        var resultado = repository.buscar(null, null, null, null, null, PRIMERA_PAGINA);

        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    @DisplayName("buscar: filtra por categoría")
    void buscar_porCategoria_devuelveSoloEsa() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, CategoriaCurso.VENTAS,
                null, null, null, PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Estrategias de Ventas", resultado.getContent().getFirst().getTitulo());
    }

    @Test
    @DisplayName("buscar: filtra por nivel")
    void buscar_porNivel_devuelveSoloEse() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, null, NivelCurso.BASICO,
                null, null, PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("buscar: filtra los gratuitos")
    void buscar_soloGratuitos_devuelveSoloEsos() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, null, null, true, null,
                PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
        assertTrue(resultado.getContent().getFirst().isGratuito());
    }

    @Test
    @DisplayName("buscar: el texto busca en el título sin distinguir mayúsculas")
    void buscar_porTextoEnTitulo_ignoraMayusculas() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, null, null, null, "MARKETING",
                PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("buscar: el texto también busca en la descripción")
    void buscar_porTextoEnDescripcion_encuentraElCurso() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, null, null, null, "fidelización",
                PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Estrategias de Ventas", resultado.getContent().getFirst().getTitulo());
    }

    @Test
    @DisplayName("buscar: combina varios filtros a la vez")
    void buscar_variosFiltros_losAplicaTodos() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, CategoriaCurso.MARKETING,
                NivelCurso.BASICO, true, "digital", PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("buscar: sin coincidencias devuelve una página vacía")
    void buscar_sinCoincidencias_devuelvePaginaVacia() {
        var resultado = repository.buscar(EstadoCurso.PUBLICADO, null, null, null, "inexistente",
                PRIMERA_PAGINA);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("findByIdAndEstado: no devuelve un borrador cuando se pide publicado")
    void findByIdAndEstado_borradorPedidoComoPublicado_devuelveVacio() {
        Long idBorrador = repository.buscar(EstadoCurso.BORRADOR, null, null, null, null,
                PRIMERA_PAGINA).getContent().getFirst().getId();

        assertTrue(repository.findByIdAndEstado(idBorrador, EstadoCurso.PUBLICADO).isEmpty());
    }
}
