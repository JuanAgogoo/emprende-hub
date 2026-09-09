import { consulta, obtener } from './cliente';
import type { Pagina } from '../types/pagina';
import type { FiltrosDirectorio, TarjetaNegocio } from '../types/negocio';

/**
 * Los destacados devuelven **una lista, no una página**: son seis y no se
 * paginan. Solo entran los negocios con al menos cinco opiniones (C7), así que
 * la lista puede llegar vacía con toda normalidad.
 */
export function obtenerDestacados(limite?: number): Promise<TarjetaNegocio[]> {
  return obtener<TarjetaNegocio[]>(`/directorio/destacados${consulta({ limite })}`);
}

/**
 * Página filtrable del directorio.
 *
 * El directorio **ignora `sort`** y ordena con su propio parámetro `orden`, de
 * lista cerrada. Los filtros vacíos no se envían: `consulta()` los descarta y
 * codifica los acentos, que es lo que evita el 400 de `?texto=café`.
 */
export function buscarNegocios(filtros: FiltrosDirectorio): Promise<Pagina<TarjetaNegocio>> {
  return obtener<Pagina<TarjetaNegocio>>(`/directorio${consulta({ ...filtros })}`);
}
