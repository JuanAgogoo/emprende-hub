import { consulta, obtener } from './cliente';
import type { TarjetaNegocio } from '../types/negocio';

/**
 * Los destacados devuelven **una lista, no una página**: son seis y no se
 * paginan. Solo entran los negocios con al menos cinco opiniones (C7), así que
 * la lista puede llegar vacía con toda normalidad.
 */
export function obtenerDestacados(limite?: number): Promise<TarjetaNegocio[]> {
  return obtener<TarjetaNegocio[]>(`/directorio/destacados${consulta({ limite })}`);
}
