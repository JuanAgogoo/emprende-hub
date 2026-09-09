/**
 * Las cuatro cifras de la portada. Se calculan (H4), no son fijas: con la base
 * recién levantada salen a cero, y eso es correcto.
 */
export interface EstadisticasPortada {
  readonly negociosActivos: number;
  readonly categorias: number;
  readonly usuariosRegistrados: number;
  /** Nula mientras no exista ninguna opinión en toda la plataforma. */
  readonly calificacionPromedio: number | null;
}
