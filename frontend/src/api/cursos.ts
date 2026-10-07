import { actualizar, consulta, eliminar, enviar, obtener, parchear } from './cliente';
import type { Curso, DatosCurso, FiltrosCursos } from '../types/curso';
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

// ---------- Gestión, solo ADMIN (E3) ----------

/** El catálogo del administrador: aquí sí están los borradores. */
export function listarCursosAdmin(pagina: number): Promise<Pagina<Curso>> {
  return obtener<Pagina<Curso>>(`/admin/cursos?page=${pagina}`, true);
}

export function obtenerCursoAdmin(id: number): Promise<Curso> {
  return obtener<Curso>(`/admin/cursos/${id}`, true);
}

/** Nace siempre en borrador: publicarlo es otra petición, a propósito. */
export function crearCurso(datos: DatosCurso): Promise<Curso> {
  return enviar<Curso>('/admin/cursos', datos, true);
}

export function actualizarCurso(id: number, datos: DatosCurso): Promise<Curso> {
  return actualizar<Curso>(`/admin/cursos/${id}`, datos, true);
}

/** Lo saca al catálogo público y lo deja en el historial de moderación (L). */
export function publicarCurso(id: number): Promise<Curso> {
  return parchear<Curso>(`/admin/cursos/${id}/publicar`, undefined, true);
}

/** Lo retira del catálogo sin borrarlo: vuelve a borrador. */
export function retirarCurso(id: number): Promise<Curso> {
  return parchear<Curso>(`/admin/cursos/${id}/borrador`, undefined, true);
}

export function eliminarCurso(id: number): Promise<void> {
  return eliminar(`/admin/cursos/${id}`, true);
}
