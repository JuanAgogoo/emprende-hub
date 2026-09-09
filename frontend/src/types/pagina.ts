/**
 * Una página de Spring Data, tal como la devuelven todos los listados.
 *
 * Solo se declara lo que el frontend usa: la respuesta trae además `pageable`,
 * `sort` y `numberOfElements`, que aquí no hacen falta.
 */
export interface Pagina<T> {
  readonly content: readonly T[];
  readonly totalElements: number;
  readonly totalPages: number;
  /** Índice de la página actual, empezando en 0. */
  readonly number: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
  readonly empty: boolean;
}
