import { eliminar, enviarFormulario, obtener } from './cliente';
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

/**
 * Añade una imagen a la galería (B9).
 *
 * Va como `multipart` y no como JSON con la imagen en base64, que es lo que
 * manda un formulario de fichero y no infla un tercio cada petición. El campo se
 * llama `archivo` porque así lo espera el controlador.
 *
 * La foto nace **pendiente de revisión** (B2) y se sube al final de la galería:
 * el orden de las llamadas decide cuál queda de portada.
 */
export function subirFoto(archivo: File): Promise<FotoNegocio> {
  const cuerpo = new FormData();
  cuerpo.append('archivo', archivo);
  return enviarFormulario<FotoNegocio>('/negocios/mio/fotos', cuerpo, true);
}

/**
 * Quita una foto, al momento y sin pasar por revisión.
 *
 * Borrar no publica nada nuevo, que es el riesgo que B2 controla. El backend
 * recoloca las que quedan, así que la portada pasa a ser la siguiente.
 */
export function borrarFoto(id: number): Promise<void> {
  return eliminar(`/negocios/mio/fotos/${id}`, true);
}
