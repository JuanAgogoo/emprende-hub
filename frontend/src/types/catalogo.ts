/** Una de las 12 categorías de negocio (G1). El icono es un emoji del catálogo. */
export interface CategoriaNegocio {
  readonly id: number;
  readonly nombre: string;
  readonly icono: string;
}

/** Un barrio dentro de su ciudad (G3). */
export interface Barrio {
  readonly id: number;
  readonly nombre: string;
}

/**
 * Una ciudad del Valle de Aburrá con sus barrios anidados, que es como los
 * devuelve el catálogo: un solo viaje y el selector de barrio se repuebla sin
 * pedir nada más. No todas tienen barrios cargados.
 */
export interface Ciudad {
  readonly id: number;
  readonly nombre: string;
  readonly barrios: readonly Barrio[];
}

/**
 * Una opción de un catálogo cerrado del backend: el código que viaja en las
 * peticiones y el nombre que se enseña.
 *
 * El código se tipa con la unión de cada catálogo, así que un `Opcion<NivelCurso>`
 * no admite un código que el backend no tenga.
 */
export interface Opcion<C extends string> {
  readonly codigo: C;
  readonly nombre: string;
}
