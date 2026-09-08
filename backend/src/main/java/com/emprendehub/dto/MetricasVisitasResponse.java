package com.emprendehub.dto;

import java.util.List;

/**
 * El panel de visitas de un negocio (H1).
 *
 * <p>Son cifras <strong>calculadas de verdad</strong>, no las fijas del
 * prototipo: cada visita al perfil deja una fila y de ahí sale todo lo demás.
 *
 * @param serie un punto por día de los últimos treinta, para la gráfica
 */
public record MetricasVisitasResponse(
        long totalHistorico,
        PeriodoMetricaResponse semana,
        PeriodoMetricaResponse mes,
        List<PuntoSerieResponse> serie) {
}
