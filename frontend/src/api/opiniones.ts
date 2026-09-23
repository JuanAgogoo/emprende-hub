import { actualizar, consulta, eliminar, enviar, obtener } from './cliente';
import type { DatosDeOpinion, Opinion } from '../types/opinion';
import type { Pagina } from '../types/pagina';

/**
 * Página de opiniones de un negocio, **las últimas primero**.
 *
 * Leerlas es público (C1): no hace falta sesión. El orden lo fija la consulta
 * del backend, así que aquí no se manda `sort`. Un negocio sin publicar
 * responde 404, igual que su perfil (B6).
 */
export function listarOpiniones(
  negocioId: number,
  pagina: number,
  porPagina: number,
): Promise<Pagina<Opinion>> {
  return obtener<Pagina<Opinion>>(
    `/negocios/${negocioId}/opiniones${consulta({ page: pagina, size: porPagina })}`,
  );
}

/**
 * La opinión propia sobre un negocio, o `404` si todavía no ha opinado.
 *
 * **Ese 404 no es un error**: es la respuesta normal de quien entra por
 * primera vez, igual que en `/negocios/mio`. Quien la consume decide si
 * enseña el formulario de publicar o la opinión que ya existe (C2).
 */
export function obtenerMiOpinion(negocioId: number): Promise<Opinion> {
  return obtener<Opinion>(`/negocios/${negocioId}/opiniones/mia`, true);
}

/**
 * Publica la opinión de quien tiene la sesión abierta (C1).
 *
 * Se publica al instante, sin revisión previa (C4), y el backend recalcula con
 * ella el promedio del negocio. Responde `400` a la segunda de la misma
 * persona (C2) y a la del dueño sobre su propio negocio (A4).
 */
export function publicarOpinion(negocioId: number, datos: DatosDeOpinion): Promise<Opinion> {
  return enviar<Opinion>(`/negocios/${negocioId}/opiniones`, datos, true);
}

/**
 * Cambia la opinión propia sobre un negocio. La deja marcada como editada.
 *
 * La ruta **no lleva identificador de opinión**: cada persona tiene como mucho
 * una por negocio (C2), así que `/mia` la identifica sin ambigüedad y sin dar
 * pie a probar con el número de otra.
 */
export function editarOpinion(negocioId: number, datos: DatosDeOpinion): Promise<Opinion> {
  return actualizar<Opinion>(`/negocios/${negocioId}/opiniones/mia`, datos, true);
}

/**
 * Borra la opinión propia. Responde `204`, sin cuerpo.
 *
 * El backend recalcula el promedio del negocio, y si era la única vuelve a ser
 * **nulo y no cero** (C5). Después de esto, volver a opinar es posible.
 */
export function borrarOpinion(negocioId: number): Promise<void> {
  return eliminar(`/negocios/${negocioId}/opiniones/mia`, true);
}
