/**
 * Un mensaje de un cliente al buzón de un negocio (D1).
 *
 * `correoCliente` es **la única dirección que la API enseña de otra persona**, y
 * llega solo al dueño del buzón: sin ella, el buzón sería un montón de preguntas
 * sin forma de contestarlas, porque la plataforma no responde desde dentro (D2).
 */
export interface Consulta {
  readonly id: number;
  readonly asunto: string;
  readonly mensaje: string;
  readonly nombreCliente: string;
  readonly correoCliente: string;
  readonly leida: boolean;
  /** ISO-8601 con zona, como todas las fechas de la API. */
  readonly fechaEnvio: string;
  readonly fechaLectura: string | null;
}

/** Lo que se manda al escribir. El nombre y el correo salen de la sesión. */
export interface DatosDeConsulta {
  readonly asunto: string;
  readonly mensaje: string;
}

/** Los máximos que valida el backend. El formulario los enseña con su contador. */
export const MAXIMO_ASUNTO = 120;
export const MAXIMO_MENSAJE = 500;
