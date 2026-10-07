import type { MiNegocio, PerfilNegocio } from './negocio';

/**
 * Un negocio en la cola de revisión.
 *
 * Es la misma respuesta del backend que recibe el dueño en su panel —estado,
 * motivo y fecha de registro—; cambia el nombre porque aquí no es «mi» negocio,
 * sino el de otra persona esperando a que se decida sobre él.
 */
export type NegocioEnRevision = MiNegocio;

/**
 * El perfil tal como quedaría publicado (HU-036).
 *
 * Tiene la forma del perfil público con una diferencia: un negocio que nunca se
 * ha aprobado no tiene fecha de aprobación. Y sus fotos llegan todas, también
 * las que esperan revisión, que son justo las que hay que juzgar.
 */
export type VistaPreviaNegocio = Omit<PerfilNegocio, 'fechaAprobacion'> & {
  readonly fechaAprobacion: string | null;
};

/**
 * Una propuesta de cambio sobre un negocio ya publicado (B2-bis).
 *
 * Trae el valor actual junto al propuesto porque revisar es comparar. Una
 * propuesta puede ser solo de fotos: entonces los textos coinciden y
 * `categoriaPropuesta` llega nula.
 */
export interface CambioPendiente {
  readonly negocioId: number;
  readonly nombreActual: string;
  readonly nombrePropuesto: string;
  readonly descripcionActual: string;
  readonly descripcionPropuesta: string;
  readonly categoriaActual: string;
  readonly categoriaPropuesta: string | null;
  readonly fotosPendientes: number;
  readonly fechaSolicitud: string;
}
