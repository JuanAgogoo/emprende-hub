import { obtener, parchear } from './cliente';
import type {
  CambioPendiente,
  CuentaUsuario,
  NegocioEnRevision,
  VistaPreviaNegocio,
} from '../types/moderacion';
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

/**
 * Las propuestas de cambio, las más antiguas primero. Es una lista y no una
 * página: el backend la devuelve entera.
 */
export function listarCambiosPendientes(): Promise<CambioPendiente[]> {
  return obtener<CambioPendiente[]>(`${BASE}/cambios-pendientes`, true);
}

/** Copia al negocio los valores propuestos y publica sus fotos nuevas. */
export function aprobarCambio(negocioId: number): Promise<NegocioEnRevision> {
  return parchear<NegocioEnRevision>(
    `${BASE}/negocios/${negocioId}/cambio/aprobar`,
    undefined,
    true,
  );
}

/** Descarta la propuesta y sus fotos. El negocio sigue como estaba. */
export function rechazarCambio(negocioId: number, motivo: string): Promise<NegocioEnRevision> {
  return parchear<NegocioEnRevision>(
    `${BASE}/negocios/${negocioId}/cambio/rechazar`,
    { motivo },
    true,
  );
}

/** Todas las cuentas, las más recientes primero. */
export function listarUsuarios(pagina: number): Promise<Pagina<CuentaUsuario>> {
  return obtener<Pagina<CuentaUsuario>>(`${BASE}/usuarios?page=${pagina}`, true);
}

/**
 * Suspende la cuenta (B4): no podrá entrar y su negocio sale del directorio.
 * Responde `204`, sin cuerpo.
 */
export function suspenderUsuario(id: number): Promise<void> {
  return parchear<void>(`${BASE}/usuarios/${id}/suspender`, undefined, true);
}

export function reactivarUsuario(id: number): Promise<void> {
  return parchear<void>(`${BASE}/usuarios/${id}/reactivar`, undefined, true);
}
