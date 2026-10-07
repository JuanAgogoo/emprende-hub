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
