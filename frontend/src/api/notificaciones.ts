import { consulta, obtener, parchear } from './cliente';
import type { Notificacion } from '../types/notificacion';
import type { Pagina } from '../types/pagina';

/**
 * Los avisos de quien tiene la sesión abierta, los más recientes primero.
 *
 * Cuelgan de la persona y no del negocio (H2): un emprendedor recibe aquí las
 * opiniones y las consultas que llegan a su negocio, y también las decisiones
 * del administrador sobre él.
 */
export function listarNotificaciones(
  pagina: number,
  porPagina: number,
): Promise<Pagina<Notificacion>> {
  return obtener<Pagina<Notificacion>>(
    `/notificaciones${consulta({ page: pagina, size: porPagina })}`,
    true,
  );
}

/** Marca un aviso como leído, o lo devuelve a pendiente. */
export function marcarNotificacionLeida(id: number, leida: boolean): Promise<Notificacion> {
  return parchear<Notificacion>(
    `/notificaciones/${id}/lectura${consulta({ leida: String(leida) })}`,
    undefined,
    true,
  );
}

/** Marca de una vez todo lo pendiente. Responde cuántos avisos eran. */
export function leerTodasLasNotificaciones(): Promise<{ readonly marcadas: number }> {
  return parchear<{ readonly marcadas: number }>('/notificaciones/leer-todas', undefined, true);
}
