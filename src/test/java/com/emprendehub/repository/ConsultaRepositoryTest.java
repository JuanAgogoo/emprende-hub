package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Consulta;
import com.emprendehub.model.EstadoNegocio;
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
import org.springframework.data.domain.PageRequest;

/**
 * Las consultas del buzón contra PostgreSQL de verdad.
 *
 * <p>La que obliga a esta clase es el filtro opcional de lectura: con
 * {@code :leida} a nulo, una consulta con filtros opcionales es justo donde este
 * proyecto se ha roto ya dos veces, y las pruebas de servicio —con el
 * repositorio simulado— no pueden ver un error de SQL.
 */
class ConsultaRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ConsultaRepository repository;

    private Long idPanaderia;
    private Long idFloristeria;
    private Long idConsultaAjena;

    @BeforeEach
    void prepararBuzones() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        CategoriaNegocio categoria =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));

        Negocio panaderia = negocio("Panadería", "pan@test.co", categoria, medellin);
        Negocio floristeria = negocio("Floristería", "flores@test.co", categoria, medellin);

        Usuario carlos = usuario("Carlos Rueda", "carlos@test.co");
        Usuario sofia = usuario("Sofía Ruiz", "sofia@test.co");

        // Dos sin leer y una leída, para que el filtro tenga algo que separar.
        consulta(panaderia, carlos, "Reserva", false);
        consulta(panaderia, sofia, "Horario del domingo", false);
        consulta(panaderia, carlos, "Pedido grande", true);
        Consulta ajena = consulta(floristeria, sofia, "Ramo para boda", false);

        idPanaderia = panaderia.getId();
        idFloristeria = floristeria.getId();
        idConsultaAjena = ajena.getId();
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
        return entityManager.persistAndFlush(new Usuario(nombre, correo, "hash", Rol.CLIENTE));
    }

    private Consulta consulta(Negocio negocio, Usuario cliente, String asunto, boolean leida) {
        Consulta consulta = new Consulta(negocio, cliente, asunto, "Texto de la consulta.");
        consulta.marcarLectura(leida);
        return entityManager.persistAndFlush(consulta);
    }

    private List<String> asuntosDe(Long negocioId, Boolean leida) {
        return repository.buscarEnBuzon(negocioId, leida, PageRequest.of(0, 20))
                .getContent().stream().map(Consulta::getAsunto).toList();
    }

    @Test
    @DisplayName("Con el filtro a nulo llega el buzón entero y no revienta")
    void buscarEnBuzon_sinFiltro_devuelveTodas() {
        // Es la prueba del parámetro opcional: con :leida a null, la consulta
        // tiene que seguir siendo válida.
        assertEquals(3, asuntosDe(idPanaderia, null).size());
    }

    @Test
    @DisplayName("El buzón llega con las más recientes primero")
    void buscarEnBuzon_ordenadoPorFecha() {
        assertEquals(List.of("Pedido grande", "Horario del domingo", "Reserva"),
                asuntosDe(idPanaderia, null));
    }

    @Test
    @DisplayName("Con leida=false llegan solo las pendientes (D3)")
    void buscarEnBuzon_soloNoLeidas() {
        var pendientes = repository.buscarEnBuzon(idPanaderia, false, PageRequest.of(0, 20));

        assertEquals(2, pendientes.getTotalElements());
        // Ese total es el número que el panel enseña como aviso.
        assertTrue(pendientes.getContent().stream().noneMatch(Consulta::isLeida));
    }

    @Test
    @DisplayName("Con leida=true llegan solo las ya atendidas")
    void buscarEnBuzon_soloLeidas() {
        assertEquals(List.of("Pedido grande"), asuntosDe(idPanaderia, true));
    }

    @Test
    @DisplayName("El buzón de un negocio no enseña las consultas de otro")
    void buscarEnBuzon_noCruzaNegocios() {
        assertEquals(List.of("Ramo para boda"), asuntosDe(idFloristeria, null));
    }

    @Test
    @DisplayName("La consulta trae resuelto el cliente, con su nombre y su correo (D2)")
    void buscarEnBuzon_traeElClienteResuelto() {
        // Sin el @EntityGraph esto fallaría al mapear: open-in-view está desactivado.
        Consulta consulta = repository.buscarEnBuzon(idPanaderia, null, PageRequest.of(0, 20))
                .getContent().getFirst();

        assertNotNull(consulta.getCliente().getNombre());
        assertNotNull(consulta.getCliente().getCorreo());
    }

    @Test
    @DisplayName("Una consulta solo se encuentra desde el buzón al que pertenece")
    void findByIdAndNegocioId_noCruzaNegocios() {
        assertTrue(repository.findByIdAndNegocioId(idConsultaAjena, idFloristeria).isPresent());
        assertTrue(repository.findByIdAndNegocioId(idConsultaAjena, idPanaderia).isEmpty());
    }

    @Test
    @DisplayName("Marcar como leída y desmarcar deja la fecha coherente con el estado")
    void marcarLectura_persisteLaFecha() {
        Consulta consulta = repository.findByIdAndNegocioId(idConsultaAjena, idFloristeria)
                .orElseThrow();

        consulta.marcarLectura(true);
        entityManager.persistAndFlush(consulta);
        assertNotNull(entityManager.find(Consulta.class, idConsultaAjena).getFechaLectura());

        consulta.marcarLectura(false);
        entityManager.persistAndFlush(consulta);
        assertEquals(null, entityManager.find(Consulta.class, idConsultaAjena).getFechaLectura());
    }
}
