import { obtener } from './cliente';
import type { EstadisticasPortada } from '../types/estadisticas';

export function obtenerEstadisticasPortada(): Promise<EstadisticasPortada> {
  return obtener<EstadisticasPortada>('/estadisticas/portada');
}
