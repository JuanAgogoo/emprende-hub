package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.RegistroModeracion;
import com.emprendehub.model.TipoEventoModeracion;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Pruebas del log de moderación.
 *
 * <p>Existe sobre todo por el filtro de fechas: los parámetros opcionales de
 * PostgreSQL necesitan un {@code CAST} explícito, y sin esta prueba el fallo
 * solo aparece llamando al endpoint.
 */
class RegistroModeracionRepositoryTest extends PostgresTestBase {

    private static final Pageable PRIMERA_PAGINA = PageRequest.of(0, 30);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RegistroModeracionRepository repository;

    private Instant ahora;

    @BeforeEach
    void prepararDatos() {
        ahora = Instant.now();
        guardar(TipoEventoModeracion.NEGOCIO_APROBADO, "Panadería", ahora.minus(10, ChronoUnit.DAYS));
        guardar(TipoEventoModeracion.NEGOCIO_RECHAZADO, "Bordados", ahora.minus(5, ChronoUnit.DAYS));
        guardar(TipoEventoModeracion.CUENTA_SUSPENDIDA, "carlos@test.co", ahora);
        entityManager.clear();
    }

    private void guardar(TipoEventoModeracion tipo, String afectado, Instant fecha) {
        RegistroModeracion registro =
                new RegistroModeracion(tipo, afectado, null, "admin@emprendehub.co");
        // La fecha la fija el constructor; para probar el filtro hace falta
        // moverla, así que se escribe directamente con JPQL tras persistir.
        entityManager.persistAndFlush(registro);
        entityManager.getEntityManager()
                .createQuery("UPDATE RegistroModeracion r SET r.fecha = :f WHERE r.id = :id")
                .setParameter("f", fecha)
                .setParameter("id", registro.getId())
                .executeUpdate();
    }

    @Test
    @DisplayName("buscar: sin fechas devuelve todo el historial")
    void buscar_sinFechas_devuelveTodo() {
        var resultado = repository.buscar(null, null, PRIMERA_PAGINA);

        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    @DisplayName("buscar: con solo la fecha inicial filtra desde ahí")
    void buscar_soloDesde_filtra() {
        var resultado = repository.buscar(ahora.minus(7, ChronoUnit.DAYS), null, PRIMERA_PAGINA);

        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    @DisplayName("buscar: con solo la fecha final filtra hasta ahí")
    void buscar_soloHasta_filtra() {
        var resultado = repository.buscar(null, ahora.minus(7, ChronoUnit.DAYS), PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("buscar: con las dos fechas acota el rango")
    void buscar_rangoCompleto_acota() {
        var resultado = repository.buscar(ahora.minus(7, ChronoUnit.DAYS),
                ahora.minus(1, ChronoUnit.DAYS), PRIMERA_PAGINA);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Bordados", resultado.getContent().getFirst().getAfectado());
    }

    @Test
    @DisplayName("buscar: devuelve lo más reciente primero")
    void buscar_ordenaDeMasRecienteAMasAntiguo() {
        var resultado = repository.buscar(null, null, PRIMERA_PAGINA);

        assertEquals("carlos@test.co", resultado.getContent().getFirst().getAfectado());
    }

    @Test
    @DisplayName("buscar: un rango sin actividad devuelve una página vacía")
    void buscar_rangoVacio_devuelvePaginaVacia() {
        var resultado = repository.buscar(ahora.plus(1, ChronoUnit.DAYS),
                ahora.plus(2, ChronoUnit.DAYS), PRIMERA_PAGINA);

        assertTrue(resultado.isEmpty());
    }
}
