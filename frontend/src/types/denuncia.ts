/** Los motivos para denunciar una opinión: una lista cerrada, no texto libre (C6). */
export type MotivoDenuncia =
  | 'LENGUAJE_INAPROPIADO'
  | 'INFORMACION_FALSA'
  | 'SPAM'
  | 'NO_ES_SOBRE_EL_NEGOCIO'
  | 'DATOS_PERSONALES';

/**
 * Una denuncia en la cola del administrador (HU-040).
 *
 * Trae el texto denunciado, de quién es y sobre qué negocio: decidir si una
 * opinión se borra exige leerla, sin abrir otra pantalla.
 */
export interface Denuncia {
  readonly id: number;
  readonly opinionId: number;
  readonly negocio: string;
  readonly autorOpinion: string;
  readonly calificacion: number;
  readonly comentario: string | null;
  readonly motivo: MotivoDenuncia;
  /** El motivo ya escrito para leerlo, tal como lo da el catálogo. */
  readonly motivoDescripcion: string;
  readonly denunciante: string;
  readonly fecha: string;
}
