import {
  actualizar,
  eliminar,
  enviarFormulario,
  obtener,
  parchear,
  parchearFormulario,
} from './cliente';
import type { DatosDeProducto } from '../types/registroEmprendedor';
import type {
  DatosPublicosDelNegocio,
  FotoNegocio,
  MiNegocio,
  Producto,
} from '../types/negocio';

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
 * Edita los campos públicos: nombre, descripción y categoría (B2).
 *
 * **Puede que no cambie nada todavía.** Si el negocio ya está publicado y la
 * revisión está encendida, el backend guarda la edición como propuesta y
 * devuelve el negocio como estaba, con la propuesta en `cambioPendiente`. Quien
 * llame tiene que mirar ese campo y no dar por hecho que el cambio ya se ve.
 */
export function editarNegocio(datos: DatosPublicosDelNegocio): Promise<MiNegocio> {
  return actualizar<MiNegocio>('/negocios/mio', datos, true);
}

/**
 * Cambia el teléfono, y este **sí** se aplica al momento.
 *
 * Es la excepción que reconoce B2: un dato de contacto no es contenido que
 * alguien tenga que revisar, y esperar tres días para corregir un dígito solo
 * dejaría más tiempo a la vista el número equivocado.
 */
export function editarTelefono(telefono: string): Promise<MiNegocio> {
  return parchear<MiNegocio>('/negocios/mio/contacto', { telefono }, true);
}

/**
 * Cambia los dos enlaces a redes sociales (B8), también al instante.
 *
 * **Viajan siempre los dos**, aunque solo cambie uno: el backend sustituye el
 * par entero, así que mandar solo `instagram` borraría el LinkedIn guardado.
 * Una cadena vacía es la forma de quitar un enlace.
 */
export function editarRedes(instagram: string, linkedin: string): Promise<MiNegocio> {
  return parchear<MiNegocio>('/negocios/mio/redes', { instagram, linkedin }, true);
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

/**
 * Alta de un artículo del escaparate, **con su imagen obligatoria**.
 *
 * Va como `multipart` con los campos sueltos, que es lo que manda un formulario
 * con un fichero: el backend los recoge con `@ModelAttribute` y no hace falta
 * inventar una parte JSON aparte.
 *
 * El precio viaja como número en pesos, sin máscaras ni separadores.
 */
export function crearProducto(datos: DatosDeProducto, foto: File): Promise<Producto> {
  const cuerpo = new FormData();
  cuerpo.append('nombre', datos.nombre);
  cuerpo.append('precio', String(datos.precio));
  cuerpo.append('disponible', String(datos.disponible));
  // Una descripción vacía no se manda: el backend distingue ausente de vacía.
  if (datos.descripcion !== undefined) cuerpo.append('descripcion', datos.descripcion);
  cuerpo.append('foto', foto);

  return enviarFormulario<Producto>('/negocios/mio/productos', cuerpo, true);
}

/**
 * Edita un artículo del escaparate.
 *
 * **No cambia la imagen**: el contrato entregado edita los campos de texto, el
 * precio y la disponibilidad. Para otra foto se borra el producto y se crea de
 * nuevo.
 */
export function editarProducto(id: number, datos: DatosDeProducto): Promise<Producto> {
  return actualizar<Producto>(`/negocios/mio/productos/${id}`, datos, true);
}

/**
 * Sustituye la imagen de un producto, y solo la imagen.
 *
 * Va por su propio endpoint y no dentro de `editarProducto`: los campos de
 * texto viajan en JSON y esto es un binario. Juntarlos obligaría a volver a
 * subir la foto cada vez que se corrige una errata del nombre.
 */
export function cambiarFotoDeProducto(id: number, foto: File): Promise<Producto> {
  const cuerpo = new FormData();
  cuerpo.append('foto', foto);
  return parchearFormulario<Producto>(`/negocios/mio/productos/${id}/foto`, cuerpo, true);
}

/** El interruptor de F3: un producto no disponible sigue saliendo, marcado. */
export function cambiarDisponibilidad(id: number, disponible: boolean): Promise<Producto> {
  return parchear<Producto>(
    `/negocios/mio/productos/${id}/disponibilidad?disponible=${String(disponible)}`,
    undefined,
    true,
  );
}

/** Borra el producto y, con él, su imagen del servidor. */
export function borrarProducto(id: number): Promise<void> {
  return eliminar(`/negocios/mio/productos/${id}`, true);
}

/**
 * Cambia el orden de la galería. **La primera es la portada** (B9).
 *
 * Viaja la lista entera y no un movimiento suelto: el orden es el estado, no la
 * acción. El backend devuelve la galería ya recolocada, así que la vista no
 * tiene que recomponerla por su cuenta.
 */
export function reordenarFotos(orden: readonly number[]): Promise<FotoNegocio[]> {
  return parchear<FotoNegocio[]>('/negocios/mio/fotos/orden', { orden }, true);
}
