/**
 * Un aviso de la persona, no del negocio (H2).
 *
 * El texto se compone y se guarda cuando ocurre el hecho, así que un aviso
 * sigue diciendo lo mismo aunque después cambie aquello de lo que avisaba.
 */
export interface Notificacion {
  readonly id: number;
  readonly tipo: TipoNotificacion;
  readonly texto: string;
  readonly leida: boolean;
  /** ISO-8601 con zona, como todas las fechas de la API. */
  readonly fecha: string;
}

/** Los cuatro hechos que avisan. El backend manda el nombre del enum. */
export type TipoNotificacion =
  | 'OPINION_NUEVA'
  | 'CONSULTA_NUEVA'
  | 'NEGOCIO_APROBADO'
  | 'NEGOCIO_RECHAZADO';
