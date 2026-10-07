import { consulta, obtener } from './cliente';
import type { Curso, FiltrosCursos } from '../types/curso';
import type { Pagina } from '../types/pagina';

/**
 * Página filtrable del catálogo público. **Solo trae los publicados** (E4): los
 * borradores no existen para quien no administra.
 *
 * El texto busca en título y descripción. Los filtros vacíos no se envían, y
 * `consulta()` codifica los acentos.
 */
export function buscarCursos(filtros: FiltrosCursos): Promise<Pagina<Curso>> {
  return obtener<Pagina<Curso>>(`/cursos${consulta({ ...filtros })}`);
}
