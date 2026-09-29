/** Los tres niveles de G4. No hay «Gratis». */
export type NivelPrecio = 'BAJO' | 'MEDIO' | 'ALTO';

/**
 * Un negocio tal como lo devuelve el directorio: la tarjeta, no el perfil.
 *
 * `calificacionPromedio` viaja nula mientras nadie haya opinado, que no es lo
 * mismo que valer cero (C5). `fotoPrincipal` es nula mientras no haya subido
 * ninguna foto aprobada.
 */
export interface TarjetaNegocio {
  readonly id: number;
  readonly nombre: string;
  readonly descripcion: string;
  readonly telefono: string;
  readonly categoria: string;
  readonly ciudad: string;
  readonly barrio: string | null;
  readonly nivelPrecio: NivelPrecio;
  readonly calificacionPromedio: number | null;
  readonly numeroOpiniones: number;
  readonly fechaAprobacion: string;
  readonly fotoPrincipal: string | null;
}

/**
 * La ordenación del directorio es un conjunto cerrado, no un nombre de columna:
 * el backend rechaza con 400 cualquier otro valor. RECIENTES ordena por fecha de
 * aprobación, no de creación (G7).
 */
export type OrdenDirectorio = 'CALIFICACION' | 'NOMBRE' | 'RECIENTES';

/** Los seis filtros del directorio, todos opcionales y combinables. */
export interface FiltrosDirectorio {
  readonly texto?: string;
  readonly categoriaId?: number;
  readonly ciudadId?: number;
  readonly barrioId?: number;
  readonly calificacionMinima?: number;
  readonly nivelPrecio?: NivelPrecio;
  readonly orden?: OrdenDirectorio;
  readonly page?: number;
  readonly size?: number;
}

/** Una imagen de la galería. El público solo recibe las aprobadas. */
export interface FotoNegocio {
  readonly id: number;
  readonly url: string;
  readonly orden: number;
  /** La primera por orden, que hace de portada (B9). */
  readonly principal: boolean;
  readonly estado: string;
}

/**
 * Un artículo del escaparate (F1). El precio llega como número, en pesos.
 *
 * `foto` **no es opcional**: un producto sin imagen dejaría un hueco en el
 * escaparate, y el backend no deja crearlo. Por eso las vistas no necesitan
 * marcador de posición como sí lo necesitan los negocios.
 */
export interface Producto {
  readonly id: number;
  readonly nombre: string;
  readonly precio: number;
  readonly descripcion: string | null;
  readonly disponible: boolean;
  readonly foto: string;
}

/**
 * El perfil público de un negocio, que **no** tiene la misma forma que la
 * tarjeta del listado: trae la galería, el escaparate y las redes, y en cambio
 * no lleva `fotoPrincipal`.
 *
 * Tampoco lleva el estado ni el motivo del rechazo: son conversación entre el
 * dueño y el administrador. El teléfono sí es público; el correo no aparece.
 */
export interface PerfilNegocio {
  readonly id: number;
  readonly nombre: string;
  readonly descripcion: string;
  readonly telefono: string;
  readonly categoria: string;
  readonly ciudad: string;
  readonly barrio: string | null;
  readonly nivelPrecio: NivelPrecio;
  readonly calificacionPromedio: number | null;
  readonly numeroOpiniones: number;
  readonly fechaAprobacion: string;
  readonly instagram: string | null;
  readonly linkedin: string | null;
  readonly fotos: readonly FotoNegocio[];
  readonly productos: readonly Producto[];
}

/**
 * La edición que el dueño mandó y que todavía espera revisión (B2-bis).
 *
 * Existe porque el negocio sigue publicado con sus valores de antes: sin esto,
 * quien edita recarga la pantalla, vuelve a leer lo viejo y cree que su cambio
 * se perdió.
 *
 * `categoria` llega nula cuando la propuesta no la cambia, que es el caso de la
 * que abre subir una foto a un negocio ya publicado.
 */
export interface CambioPropuesto {
  readonly nombre: string;
  readonly descripcion: string;
  readonly categoria: string | null;
  readonly fechaSolicitud: string;
}

/** Los tres estados por los que pasa un negocio. Nace PENDIENTE (B6). */
export type EstadoNegocio = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO';

/**
 * El negocio visto por su dueño.
 *
 * A diferencia del perfil público, este **sí** trae el estado y el motivo del
 * rechazo, que son conversación entre el dueño y quien administra (B1). En
 * cambio no trae la galería ni el escaparate: van por sus propios endpoints.
 */
export interface MiNegocio {
  readonly id: number;
  readonly nombre: string;
  readonly descripcion: string;
  readonly telefono: string;
  readonly categoria: string;
  readonly ciudad: string;
  readonly barrio: string | null;
  readonly nivelPrecio: NivelPrecio;
  readonly estado: EstadoNegocio;
  readonly motivoRechazo: string | null;
  readonly calificacionPromedio: number | null;
  readonly numeroOpiniones: number;
  readonly instagram: string | null;
  readonly linkedin: string | null;
  /** La edición esperando revisión, o `null` si no hay ninguna en cola. */
  readonly cambioPendiente: CambioPropuesto | null;
}

/** Lo que se edita de un negocio y pasa por revisión: los campos públicos. */
export interface DatosPublicosDelNegocio {
  readonly nombre: string;
  readonly descripcion: string;
  readonly categoriaId: number;
}

