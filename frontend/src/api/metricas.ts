import { obtener } from './cliente';
import type { MetricasVisitas } from '../types/metricas';

/**
 * Las visitas del negocio propio, con su comparación y su serie diaria (H1).
 *
 * Solo las ve el dueño: el backend las saca de su sesión, así que la ruta no
 * lleva identificador de negocio.
 *
 * **No hay endpoint para registrar una visita.** Se anota sola al pedir el
 * perfil público, y solo ahí: uno aparte dejaría inflar el contador.
 */
export function obtenerMetricasDeVisitas(): Promise<MetricasVisitas> {
  return obtener<MetricasVisitas>('/negocios/mio/metricas/visitas', true);
}
