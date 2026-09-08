package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.MetricasVisitasResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.model.Visita;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.VisitaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La agregación de visitas, que es aritmética pura sobre lo que devuelve una
 * consulta.
 *
 * <p>El reloj va fijo a propósito: con {@code LocalDate.now()} dentro del
 * servicio, comprobar «los últimos siete días frente a los siete anteriores»
 * dependería del día en que se ejecutara la prueba.
 */
@ExtendWith(MockitoExtension.class)
class VisitaServiceTest {

    /** Un miércoles cualquiera, para que las cuentas se puedan seguir a mano. */
    private static final LocalDate HOY = LocalDate.of(2026, 8, 26);

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final Clock reloj = Clock.fixed(
            HOY.atStartOfDay(ZONA).plusHours(10).toInstant(), ZONA);

    @Mock
    private VisitaRepository visitaRepository;

    @Mock
    private NegocioRepository negocioRepository;

    private VisitaService service;

    private final Usuario duena = usuario(4L, "María");
    private final Usuario visitante = usuario(3L, "Carlos");

    private VisitaService servicio() {
        if (service == null) {
            service = new VisitaService(visitaRepository, negocioRepository, reloj);
        }
        return service;
    }

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario(nombre, nombre.toLowerCase() + "@test.co", "hash",
                Rol.CLIENTE);
        usuario.setId(id);
        return usuario;
    }

    private Negocio negocio() {
        Negocio negocio = new Negocio(duena, "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.", "3001234567",
                new CategoriaNegocio("Gastronomía", "🍴"), new Ciudad("Medellín"), null,
                NivelPrecio.MEDIO);
        negocio.setId(7L);
        return negocio;
    }

    /** Un día con su número de visitas, como lo agrupa la consulta. */
    private VisitaRepository.ConteoDiario conteo(LocalDate fecha, long total) {
        return new VisitaRepository.ConteoDiario() {
            @Override
            public LocalDate getFecha() {
                return fecha;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private void conVisitas(List<VisitaRepository.ConteoDiario> conteos) {
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.of(negocio()));
        when(visitaRepository.contarPorDiaDesde(eq(7L), any())).thenReturn(conteos);
        when(visitaRepository.countByNegocioId(7L)).thenReturn(
                conteos.stream().mapToLong(VisitaRepository.ConteoDiario::getTotal).sum());
    }

    // ---------- Registro (H1) ----------

    @Test
    @DisplayName("Una visita anónima se anota con la fecha de hoy en la zona del proyecto")
    void registrar_anonima_seAnota() {
        Negocio negocio = negocio();
        when(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(eq(7L), any(), eq(HOY)))
                .thenReturn(false);

        servicio().registrar(negocio, null, "190.0.0.1|Firefox");

        ArgumentCaptor<Visita> captor = ArgumentCaptor.forClass(Visita.class);
        verify(visitaRepository).save(captor.capture());
        assertEquals(HOY, captor.getValue().getFecha());
        assertEquals(negocio, captor.getValue().getNegocio());
    }

    @Test
    @DisplayName("Recargar la página no suma otra visita")
    void registrar_repetidaElMismoDia_noSuma() {
        when(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(eq(7L), any(), eq(HOY)))
                .thenReturn(true);

        servicio().registrar(negocio(), null, "190.0.0.1|Firefox");

        verify(visitaRepository, never()).save(any());
    }

    @Test
    @DisplayName("El dueño mirando su propio perfil no cuenta como visita (H1)")
    void registrar_elDueno_noCuenta() {
        servicio().registrar(negocio(), duena, "190.0.0.1|Firefox");

        verify(visitaRepository, never()).save(any());
        verify(visitaRepository, never())
                .existsByNegocioIdAndHuellaSesionAndFecha(any(), any(), any());
    }

    @Test
    @DisplayName("Otro usuario con sesión sí cuenta, y se agrupa por su cuenta")
    void registrar_otroUsuario_seAgrupaPorCuenta() {
        when(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(eq(7L), any(), eq(HOY)))
                .thenReturn(false);

        servicio().registrar(negocio(), visitante, "190.0.0.1|Firefox");

        ArgumentCaptor<Visita> captor = ArgumentCaptor.forClass(Visita.class);
        verify(visitaRepository).save(captor.capture());
        // Con sesión, la misma persona cuenta una vez al día aunque cambie de
        // dispositivo: la huella es su cuenta, no la petición.
        assertEquals("u:3", captor.getValue().getHuellaSesion());
    }

    @Test
    @DisplayName("La huella anónima resume la petición y no guarda ni la dirección")
    void registrar_anonima_noGuardaLaDireccion() {
        when(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(eq(7L), any(), eq(HOY)))
                .thenReturn(false);

        servicio().registrar(negocio(), null, "190.0.0.1|Firefox");

        ArgumentCaptor<Visita> captor = ArgumentCaptor.forClass(Visita.class);
        verify(visitaRepository).save(captor.capture());
        String huella = captor.getValue().getHuellaSesion();
        assertEquals(34, huella.length(), "prefijo «a:» más 32 de hash");
        org.junit.jupiter.api.Assertions.assertFalse(huella.contains("190.0.0.1"));
    }

    @Test
    @DisplayName("Dos peticiones anónimas distintas se cuentan por separado")
    void registrar_dosAnonimasDistintas_danHuellasDistintas() {
        when(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(eq(7L), any(), eq(HOY)))
                .thenReturn(false);

        servicio().registrar(negocio(), null, "190.0.0.1|Firefox");
        servicio().registrar(negocio(), null, "181.0.0.9|Chrome");

        ArgumentCaptor<Visita> captor = ArgumentCaptor.forClass(Visita.class);
        verify(visitaRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(2, captor.getAllValues().stream()
                .map(Visita::getHuellaSesion).distinct().count());
    }

    @Test
    @DisplayName("Una petición sin datos de origen sigue contando, sin romperse")
    void registrar_sinHuella_noRevienta() {
        when(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(eq(7L), any(), eq(HOY)))
                .thenReturn(false);

        servicio().registrar(negocio(), null, null);

        verify(visitaRepository).save(any());
    }

    // ---------- Agregación por periodo (H1) ----------

    @Test
    @DisplayName("La semana son hoy y los seis días anteriores")
    void metricas_semana_incluyeHoy() {
        conVisitas(List.of(
                conteo(HOY, 5),
                conteo(HOY.minusDays(6), 3),
                conteo(HOY.minusDays(7), 100)));   // ya fuera de la semana

        var semana = servicio().metricasDeMiNegocio(duena).semana();

        assertEquals(8, semana.actual());
        assertEquals(100, semana.anterior());
    }

    @Test
    @DisplayName("La variación porcentual se calcula con un decimal")
    void metricas_variacion_conUnDecimal() {
        // 12 esta semana frente a 8 la anterior: +50%.
        conVisitas(List.of(
                conteo(HOY, 12),
                conteo(HOY.minusDays(8), 8)));

        var semana = servicio().metricasDeMiNegocio(duena).semana();

        assertEquals(new BigDecimal("50.0"), semana.variacionPorcentual());
    }

    @Test
    @DisplayName("Una bajada da variación negativa")
    void metricas_bajada_daVariacionNegativa() {
        conVisitas(List.of(
                conteo(HOY, 5),
                conteo(HOY.minusDays(8), 20)));

        assertEquals(new BigDecimal("-75.0"),
                servicio().metricasDeMiNegocio(duena).semana().variacionPorcentual());
    }

    @Test
    @DisplayName("Sin periodo anterior no hay variación: se devuelve nula, no infinito")
    void metricas_sinPeriodoAnterior_variacionNula() {
        // Pasar de ninguna visita a cinco no es un aumento del quinientos por
        // ciento: es que antes no había con qué comparar.
        conVisitas(List.of(conteo(HOY, 5)));

        var metricas = servicio().metricasDeMiNegocio(duena);

        assertEquals(5, metricas.semana().actual());
        assertEquals(0, metricas.semana().anterior());
        assertNull(metricas.semana().variacionPorcentual());
    }

    @Test
    @DisplayName("Un negocio sin ninguna visita devuelve ceros y no falla")
    void metricas_sinVisitas_devuelveCeros() {
        conVisitas(List.of());

        var metricas = servicio().metricasDeMiNegocio(duena);

        assertEquals(0, metricas.totalHistorico());
        assertEquals(0, metricas.semana().actual());
        assertNull(metricas.mes().variacionPorcentual());
    }

    @Test
    @DisplayName("El mes son treinta días y se compara con los treinta anteriores")
    void metricas_mes_comparaTreintaConTreinta() {
        conVisitas(List.of(
                conteo(HOY.minusDays(29), 10),   // último día del mes actual
                conteo(HOY.minusDays(30), 4),    // primero del anterior
                conteo(HOY.minusDays(59), 6)));  // último del anterior

        var mes = servicio().metricasDeMiNegocio(duena).mes();

        assertEquals(10, mes.actual());
        assertEquals(10, mes.anterior());
        assertEquals(new BigDecimal("0.0"), mes.variacionPorcentual());
    }

    // ---------- La serie de la gráfica ----------

    @Test
    @DisplayName("La serie trae treinta puntos, uno por día y en orden")
    void metricas_serie_treintaPuntosEnOrden() {
        conVisitas(List.of(conteo(HOY, 5)));

        var serie = servicio().metricasDeMiNegocio(duena).serie();

        assertEquals(30, serie.size());
        assertEquals(HOY.minusDays(29), serie.getFirst().fecha());
        assertEquals(HOY, serie.getLast().fecha());
    }

    @Test
    @DisplayName("Los días sin visitas salen en cero, no se saltan")
    void metricas_serie_rellenaLosHuecos() {
        // Si se saltaran, la gráfica uniría el lunes con el jueves y aparentaría
        // una caída que no existió.
        conVisitas(List.of(conteo(HOY, 5), conteo(HOY.minusDays(2), 3)));

        var serie = servicio().metricasDeMiNegocio(duena).serie();

        assertEquals(5, serie.getLast().visitas());
        assertEquals(0, serie.get(serie.size() - 2).visitas());
        assertEquals(3, serie.get(serie.size() - 3).visitas());
    }

    @Test
    @DisplayName("Quien no tiene negocio no tiene métricas")
    void metricas_sinNegocio_lanzaNoEncontrado() {
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio().metricasDeMiNegocio(duena));
    }

    @Test
    @DisplayName("El total histórico no se limita a los últimos treinta días")
    void metricas_totalHistorico_esDeSiempre() {
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.of(negocio()));
        when(visitaRepository.contarPorDiaDesde(eq(7L), any())).thenReturn(List.of());
        when(visitaRepository.countByNegocioId(7L)).thenReturn(1034L);

        var metricas = servicio().metricasDeMiNegocio(duena);

        assertEquals(1034, metricas.totalHistorico());
        assertEquals(0, metricas.mes().actual());
    }

    @Test
    @DisplayName("El instante que se guarda sale del reloj inyectado")
    void registrar_usaElRelojInyectado() {
        when(visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(eq(7L), any(), eq(HOY)))
                .thenReturn(false);

        servicio().registrar(negocio(), null, "190.0.0.1|Firefox");

        ArgumentCaptor<Visita> captor = ArgumentCaptor.forClass(Visita.class);
        verify(visitaRepository).save(captor.capture());
        assertEquals(Instant.from(reloj.instant()), captor.getValue().getInstante());
    }
}
