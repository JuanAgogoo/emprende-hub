import { consulta, obtener, parchear } from './cliente';
import type {
  CambioPendiente,
  CuentaUsuario,
  NegocioEnRevision,
  RegistroModeracion,
  VistaPreviaNegocio,
} from '../types/moderacion';
import type { Denuncia } from '../types/denuncia';
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

/** Las denuncias sin resolver, las más antiguas primero (HU-040). */
export function listarDenuncias(pagina: number): Promise<Pagina<Denuncia>> {
  return obtener<Pagina<Denuncia>>(`${BASE}/denuncias?page=${pagina}`, true);
}

/**
 * Da la razón a la denuncia y borra la opinión. El motivo es obligatorio y va
 * al log; el promedio del negocio se recalcula. Responde `204`.
 */
export function eliminarOpinionDenunciada(denunciaId: number, motivo: string): Promise<void> {
  return parchear<void>(`${BASE}/denuncias/${denunciaId}/eliminar-opinion`, { motivo }, true);
}

/** La opinión se queda publicada, y queda constancia de que se miró. */
export function desestimarDenuncia(denunciaId: number): Promise<void> {
  return parchear<void>(`${BASE}/denuncias/${denunciaId}/desestimar`, undefined, true);
}

/**
 * El historial de moderación, lo más reciente primero (HU-041).
 *
 * `desde` y `hasta` son instantes ISO-8601 y los dos son opcionales; el backend
 * incluye los extremos.
 */
export function consultarHistorial(
  desde: string | undefined,
  hasta: string | undefined,
  pagina: number,
): Promise<Pagina<RegistroModeracion>> {
  return obtener<Pagina<RegistroModeracion>>(
    `${BASE}/log${consulta({ desde, hasta, page: pagina })}`,
    true,
  );
}
