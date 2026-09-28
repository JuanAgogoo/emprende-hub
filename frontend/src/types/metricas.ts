/**
 * Un periodo comparado con el anterior: esta semana contra la pasada, este mes
 * contra el anterior.
 */
export interface PeriodoMetrica {
  readonly actual: number;
  readonly anterior: number;
  /**
   * Cuánto cambió, en porcentaje. **Viaja nula cuando el periodo anterior fue
   * cero** (H1): pasar de ninguna visita a cinco no es un aumento del
   * quinientos por ciento, es que antes no había con qué comparar. Se comprueba
   * con `null` y nunca con `||`, como el promedio de C5.
   */
  readonly variacionPorcentual: number | null;
}

/** Un día de la serie. Los días sin visitas vienen en cero, no se saltan. */
export interface PuntoSerie {
  /** `YYYY-MM-DD`, el día cortado en America/Bogota y no en UTC. */
  readonly fecha: string;
  readonly visitas: number;
}

/** Las visitas del negocio propio (H1). Solo las ve su dueño. */
export interface MetricasVisitas {
  readonly totalHistorico: number;
  readonly semana: PeriodoMetrica;
  readonly mes: PeriodoMetrica;
  readonly serie: readonly PuntoSerie[];
}
