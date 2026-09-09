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
