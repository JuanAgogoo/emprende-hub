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
