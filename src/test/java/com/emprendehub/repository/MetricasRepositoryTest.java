package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Notificacion;
import com.emprendehub.model.Rol;
import com.emprendehub.model.TipoNotificacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.model.Visita;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

/**
 * Las consultas de visitas y notificaciones contra PostgreSQL de verdad.
 *
 * <p>La que obliga a esta clase es el {@code GROUP BY} por día: agrupar y contar
 * es justo lo que una prueba con el repositorio simulado no puede comprobar, y
 * de ahí salen las cuatro cifras del panel.
 */
class MetricasRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VisitaRepository visitaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    private static final LocalDate HOY = LocalDate.of(2026, 8, 26);

    private Long idPanaderia;
    private Long idFloristeria;
    private Long idDuena;
    private Long idNotificacionPendiente;

    @BeforeEach
    void prepararMetricas() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        CategoriaNegocio categoria =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));

        Usuario duena = usuario("María", "maria@test.co");
        Usuario otra = usuario("Sofía", "sofia@test.co");
        Negocio panaderia = negocio("Panadería", duena, categoria, medellin);
        Negocio floristeria = negocio("Floristería", otra, categoria, medellin);

        // Dos visitas hoy de sesiones distintas, una ayer, y una de otro negocio.
        visita(panaderia, "u:3", HOY);
        visita(panaderia, "a:abc", HOY);
        visita(panaderia, "u:3", HOY.minusDays(1));
        visita(floristeria, "u:3", HOY);

        Notificacion pendiente = entityManager.persistAndFlush(new Notificacion(
                duena, TipoNotificacion.OPINION_NUEVA, "Carlos opinó: 5 estrellas"));
        Notificacion leida = new Notificacion(
                duena, TipoNotificacion.CONSULTA_NUEVA, "Nueva consulta de Sofía");
        leida.setLeida(true);
        entityManager.persistAndFlush(leida);
        entityManager.persistAndFlush(new Notificacion(
                otra, TipoNotificacion.NEGOCIO_APROBADO, "Tu negocio ya está publicado"));

        idPanaderia = panaderia.getId();
        idFloristeria = floristeria.getId();
        idDuena = duena.getId();
        idNotificacionPendiente = pendiente.getId();
        entityManager.clear();
    }

    private Usuario usuario(String nombre, String correo) {
        return entityManager.persistAndFlush(
                new Usuario(nombre, correo, "hash", Rol.EMPRENDEDOR));
    }

    private Negocio negocio(String nombre, Usuario dueno, CategoriaNegocio categoria,
                            Ciudad ciudad) {
        Negocio negocio = new Negocio(dueno, nombre,
                "Descripción larga de prueba con más de ochenta caracteres para pasar la "
                        + "validación de longitud del registro.",
                "3001234567", categoria, ciudad, null, NivelPrecio.MEDIO);
        negocio.setEstado(EstadoNegocio.APROBADO);
        return entityManager.persistAndFlush(negocio);
    }

    private void visita(Negocio negocio, String huella, LocalDate fecha) {
        entityManager.persistAndFlush(new Visita(negocio, huella, fecha, Instant.now()));
    }

    // ---------- Visitas ----------

    @Test
    @DisplayName("La consulta agrupa por día y cuenta cada uno")
    void contarPorDia_agrupaYCuenta() {
        List<VisitaRepository.ConteoDiario> conteos =
                visitaRepository.contarPorDiaDesde(idPanaderia, HOY.minusDays(30));

        assertEquals(2, conteos.size());
        assertEquals(HOY.minusDays(1), conteos.getFirst().getFecha());
        assertEquals(1, conteos.getFirst().getTotal());
        assertEquals(HOY, conteos.getLast().getFecha());
        assertEquals(2, conteos.getLast().getTotal());
    }

    @Test
    @DisplayName("Los días sin visitas no aparecen: rellenarlos es del servicio")
    void contarPorDia_noInventaDiasVacios() {
        assertEquals(2, visitaRepository
                .contarPorDiaDesde(idPanaderia, HOY.minusDays(30)).size());
    }

    @Test
    @DisplayName("La fecha de corte deja fuera lo anterior")
    void contarPorDia_respetaLaFechaDeCorte() {
        assertEquals(1, visitaRepository.contarPorDiaDesde(idPanaderia, HOY).size());
    }

    @Test
    @DisplayName("Las visitas de un negocio no se mezclan con las de otro")
    void contarPorDia_noCruzaNegocios() {
        assertEquals(3, visitaRepository.countByNegocioId(idPanaderia));
        assertEquals(1, visitaRepository.countByNegocioId(idFloristeria));
    }

    @Test
    @DisplayName("La deduplicación distingue negocio, huella y día (H1)")
    void exists_distingueLosTresCampos() {
        assertTrue(visitaRepository
                .existsByNegocioIdAndHuellaSesionAndFecha(idPanaderia, "u:3", HOY));
        assertFalse(visitaRepository
                .existsByNegocioIdAndHuellaSesionAndFecha(idPanaderia, "u:9", HOY));
        assertFalse(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(
                idPanaderia, "u:3", HOY.minusDays(5)));
        assertFalse(visitaRepository
                .existsByNegocioIdAndHuellaSesionAndFecha(idFloristeria, "a:abc", HOY));
    }

    // ---------- Notificaciones ----------

    @Test
    @DisplayName("Con el filtro a nulo llegan todas las de esa persona")
    void notificaciones_sinFiltro_devuelveTodas() {
        // Es la prueba del parámetro opcional: con :leida a null la consulta
        // tiene que seguir siendo válida.
        assertEquals(2, notificacionRepository
                .buscarDe(idDuena, null, PageRequest.of(0, 20)).getTotalElements());
    }

    @Test
    @DisplayName("Con leida=false llegan solo las pendientes (H3)")
    void notificaciones_soloPendientes() {
        var pendientes = notificacionRepository.buscarDe(idDuena, false, PageRequest.of(0, 20));

        assertEquals(1, pendientes.getTotalElements());
        assertEquals(TipoNotificacion.OPINION_NUEVA,
                pendientes.getContent().getFirst().getTipo());
    }

    @Test
    @DisplayName("Nadie ve las notificaciones de otra persona")
    void notificaciones_noCruzanDestinatario() {
        assertTrue(notificacionRepository
                .findByIdAndDestinatarioId(idNotificacionPendiente, idDuena).isPresent());
        assertTrue(notificacionRepository
                .findByIdAndDestinatarioId(idNotificacionPendiente, 999999L).isEmpty());
    }

    @Test
    @DisplayName("Las pendientes se pueden traer todas para marcarlas de una vez")
    void notificaciones_pendientesParaMarcarTodas() {
        assertEquals(1, notificacionRepository
                .findByDestinatarioIdAndLeidaFalse(idDuena).size());
    }
}
