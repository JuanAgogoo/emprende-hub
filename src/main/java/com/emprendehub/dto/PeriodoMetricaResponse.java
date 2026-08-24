package com.emprendehub.dto;

import java.math.BigDecimal;

/**
 * Un periodo comparado con el anterior de la misma duración.
 *
 * @param actual visitas del periodo que termina hoy
 * @param anterior visitas del periodo inmediatamente anterior
 * @param variacionPorcentual cuánto cambió, en porcentaje. <strong>Nula cuando
 *     el periodo anterior fue cero</strong>: pasar de ninguna visita a cinco no
 *     es un aumento del quinientos por ciento ni del infinito, es que antes no
 *     había con qué comparar
 */
public record PeriodoMetricaResponse(
        long actual,
        long anterior,
        BigDecimal variacionPorcentual) {
}
