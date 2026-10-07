import { obtener, parchear } from './cliente';
import type { NegocioEnRevision, VistaPreviaNegocio } from '../types/moderacion';
import type { Pagina } from '../types/pagina';

/**
 * Todo cuelga de `/admin/moderacion`, que el backend reserva al rol ADMIN: con
 * otro token responde 403 aunque esta capa se saltara.
 */
const BASE = '/admin/moderacion';

/** La cola de revisión, los que más llevan esperando primero (B5). */
export function listarNegociosPendientes(pagina: number): Promise<Pagina<NegocioEnRevision>> {
  return obtener<Pagina<NegocioEnRevision>>(`${BASE}/negocios-pendientes?page=${pagina}`, true);
}

/** El perfil entero, con las fotos sin aprobar. Uno que no existe responde 404. */
export function obtenerVistaPrevia(id: number): Promise<VistaPreviaNegocio> {
  return obtener<VistaPreviaNegocio>(`${BASE}/negocios/${id}`, true);
}

/** Lo publica y avisa a su dueño, en el panel y por correo (I1-ter). */
export function aprobarNegocio(id: number): Promise<NegocioEnRevision> {
  return parchear<NegocioEnRevision>(`${BASE}/negocios/${id}/aprobar`, undefined, true);
}

/** El motivo es obligatorio: es lo que el dueño lee para corregir y reenviar (B1). */
export function rechazarNegocio(id: number, motivo: string): Promise<NegocioEnRevision> {
  return parchear<NegocioEnRevision>(`${BASE}/negocios/${id}/rechazar`, { motivo }, true);
}
