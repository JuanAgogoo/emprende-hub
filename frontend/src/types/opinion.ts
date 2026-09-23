/**
 * Una opinión de un negocio, tal como la publica el backend.
 *
 * Del autor sale **su nombre y nada más**: ni correo, ni foto, ni enlace a un
 * perfil que no existe. El comentario es opcional (C2), así que viaja nulo
 * cuando quien opinó solo dejó la nota.
 */
export interface Opinion {
  readonly id: number;
  readonly autor: string;
  /** De 1 a 5. Obligatoria: no hay opinión sin nota. */
  readonly calificacion: number;
  readonly comentario: string | null;
  /** ISO-8601 con zona, como todas las fechas de la API. */
  readonly fechaCreacion: string;
  /** Cierto si su autor la cambió después de publicarla. */
  readonly editada: boolean;
}

/**
 * Lo que se manda al publicar o al editar una opinión.
 *
 * La calificación es obligatoria porque es lo único que entra en el promedio
 * del negocio; el comentario no se manda cuando está vacío, que es como el
 * backend distingue «sin comentario» de «comentario en blanco».
 */
export interface DatosDeOpinion {
  readonly calificacion: number;
  readonly comentario?: string;
}

/** El máximo que admite el comentario. El contador del formulario lo enseña. */
export const MAXIMO_COMENTARIO = 300;
