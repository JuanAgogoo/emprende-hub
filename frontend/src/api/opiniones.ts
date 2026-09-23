import { consulta, obtener } from './cliente';
import type { Opinion } from '../types/opinion';
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
