package com.emprendehub.service;

import com.emprendehub.dto.MetricasVisitasResponse;
import com.emprendehub.dto.PeriodoMetricaResponse;
import com.emprendehub.dto.PuntoSerieResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Usuario;
import com.emprendehub.model.Visita;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.VisitaRepository;
import com.emprendehub.repository.VisitaRepository.ConteoDiario;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro y agregación de las visitas a un perfil (H1).
 *
 * <p>Las reglas del registro:
 * <ul>
 *   <li><strong>Una por perfil, sesión y día.</strong> Recargar la página no
 *       suma.</li>
 *   <li><strong>Las del propio dueño no cuentan.</strong> Solo se le puede
 *       descartar cuando hay sesión: el directorio es público y una visita
 *       anónima siempre cuenta, aunque venga de él sin haber entrado.</li>
 *   <li>El día se calcula en la zona del proyecto. El corte de la medianoche es
 *       el de Medellín, no el de UTC.</li>
 * </ul>
 *
 * <p>La agregación por periodo es aritmética pura sobre lo que devuelve una
 * consulta: la base de datos agrupa por día y aquí se comparan los periodos y se
 * rellenan los huecos. Es lógica de negocio y se prueba como tal, sin base de
 * datos de por medio.
 */
@Service
@Transactional(readOnly = true)
public class VisitaService {

    /** Los dos periodos que enseña el panel del prototipo. */
    static final int DIAS_SEMANA = 7;
    static final int DIAS_MES = 30;

    private final VisitaRepository visitaRepository;
    private final NegocioRepository negocioRepository;
    private final Clock reloj;

    public VisitaService(VisitaRepository visitaRepository,
                         NegocioRepository negocioRepository,
                         Clock reloj) {
        this.visitaRepository = visitaRepository;
        this.negocioRepository = negocioRepository;
        this.reloj = reloj;
    }

    // ---------- Registro ----------

    /**
     * Anota una visita al perfil, si toca anotarla.
     *
     * <p>No devuelve nada ni falla nunca por no poder contar: es un efecto
     * lateral de mirar un perfil público, y ninguna métrica justifica romper la
     * página que la produce.
     *
     * @param visitante quien mira, o {@code null} si no ha iniciado sesión
     * @param huellaPeticion algo estable de la petición anónima —dirección y
     *     navegador— con lo que distinguir una sesión de otra
     */
    @Transactional
    public void registrar(Negocio negocio, Usuario visitante, String huellaPeticion) {
        if (visitante != null && negocio.getUsuario().getId().equals(visitante.getId())) {
            // El dueño mirando su propio perfil no es una visita (H1).
            return;
        }

        LocalDate hoy = LocalDate.now(reloj);
        String huella = huellaDe(visitante, huellaPeticion);

        if (visitaRepository.existsByNegocioIdAndHuellaSesionAndFecha(
                negocio.getId(), huella, hoy)) {
            return;
        }

        visitaRepository.save(new Visita(negocio, huella, hoy, reloj.instant()));
    }

    /**
     * Con quién se agrupa la visita.
     *
     * <p>Con sesión iniciada es la cuenta, así que la misma persona cuenta una
     * vez al día aunque mire desde el móvil y desde el portátil. Sin sesión se
     * resume la petición en un hash: no identifica a nadie y solo sirve para
     * decir «esto ya lo conté hoy».
     *
     * <p>La aproximación tiene un límite conocido: varias personas tras la misma
     * salida a internet comparten dirección y navegador, y contarían como una.
     * Sin cookies ni sesión de servidor no hay forma mejor, y para el alcance del
     * proyecto (K1) sobra.
     */
    private String huellaDe(Usuario visitante, String huellaPeticion) {
        if (visitante != null) {
            return "u:" + visitante.getId();
        }
        return "a:" + resumir(huellaPeticion == null ? "desconocido" : huellaPeticion);
    }

    private String resumir(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 debería existir siempre", e);
        }
    }

    // ---------- Agregación ----------

    /** El panel de visitas del negocio propio. */
    public MetricasVisitasResponse metricasDeMiNegocio(Usuario solicitante) {
        Negocio negocio = negocioRepository.findByUsuarioId(solicitante.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Negocio del usuario", solicitante.getId()));

        LocalDate hoy = LocalDate.now(reloj);
        // Se piden dos meses de golpe: el mes actual y el anterior, que es con
        // lo que se compara. Una sola consulta para las cuatro cifras.
        Map<LocalDate, Long> porDia = visitaRepository
                .contarPorDiaDesde(negocio.getId(), hoy.minusDays(2L * DIAS_MES - 1))
                .stream()
                .collect(Collectors.toMap(ConteoDiario::getFecha, ConteoDiario::getTotal,
                        Long::sum));

        return new MetricasVisitasResponse(
                visitaRepository.countByNegocioId(negocio.getId()),
                periodo(porDia, hoy, DIAS_SEMANA),
                periodo(porDia, hoy, DIAS_MES),
                serie(porDia, hoy, DIAS_MES));
    }

    /**
     * Compara los últimos {@code dias} con los {@code dias} anteriores.
     *
     * <p>El periodo actual incluye hoy, así que «la semana» son hoy y los seis
     * días anteriores, no la semana del calendario. Es lo que enseña el
     * prototipo —«vs semana anterior»— y lo que tiene sentido en un panel que se
     * mira cualquier día.
     */
    private PeriodoMetricaResponse periodo(Map<LocalDate, Long> porDia, LocalDate hoy, int dias) {
        long actual = sumar(porDia, hoy.minusDays(dias - 1L), hoy);
        long anterior = sumar(porDia, hoy.minusDays(2L * dias - 1), hoy.minusDays(dias));

        return new PeriodoMetricaResponse(actual, anterior, variacion(actual, anterior));
    }

    private long sumar(Map<LocalDate, Long> porDia, LocalDate desde, LocalDate hasta) {
        long total = 0;
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            total += porDia.getOrDefault(dia, 0L);
        }
        return total;
    }

    /**
     * Cuánto cambió, en porcentaje y con un decimal.
     *
     * <p>Sin periodo anterior no hay variación: se devuelve nula. Dividir entre
     * cero daría infinito, y llamarlo «+100%» sería inventarse una comparación
     * que nadie hizo.
     */
    private BigDecimal variacion(long actual, long anterior) {
        if (anterior == 0) {
            return null;
        }
        return BigDecimal.valueOf(actual - anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(anterior), 1, RoundingMode.HALF_UP);
    }

    /** Un punto por día, incluidos los que no tuvieron ninguna visita. */
    private List<PuntoSerieResponse> serie(Map<LocalDate, Long> porDia, LocalDate hoy, int dias) {
        List<PuntoSerieResponse> puntos = new ArrayList<>(dias);
        for (int i = dias - 1; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            puntos.add(new PuntoSerieResponse(dia, porDia.getOrDefault(dia, 0L)));
        }
        return puntos;
    }
}
