import { obtener } from './cliente';
import type { FotoNegocio, MiNegocio, Producto } from '../types/negocio';

/**
 * El negocio de quien tiene la sesión abierta.
 *
 * Responde `404` a quien no tenga ninguno, que es el caso de cualquier cliente:
 * no es un error a mostrar, es que todavía no ha registrado su negocio.
 */
export function obtenerMiNegocio(): Promise<MiNegocio> {
  return obtener<MiNegocio>('/negocios/mio', true);
}

/** La galería del dueño: incluye las fotos **sin revisar**, con su estado. */
export function obtenerMisFotos(): Promise<FotoNegocio[]> {
  return obtener<FotoNegocio[]>('/negocios/mio/fotos', true);
}

export function obtenerMisProductos(): Promise<Producto[]> {
  return obtener<Producto[]>('/negocios/mio/productos', true);
}
