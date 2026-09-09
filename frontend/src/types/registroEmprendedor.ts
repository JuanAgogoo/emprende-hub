import type { NivelPrecio } from './negocio';
import type { RespuestaAuth } from './sesion';

/**
 * El negocio, anidado dentro del alta.
 *
 * Es la misma forma que pide `POST /negocios`: el backend reutiliza sus
 * validaciones en cascada en vez de reescribirlas.
 */
export interface NegocioDelRegistro {
  readonly nombre: string;
  readonly descripcion: string;
  readonly telefono: string;
  readonly categoriaId: number;
  readonly ciudadId: number;
  /** Opcional: solo Medellín tiene barrios en el catálogo (G3). */
  readonly barrioId?: number;
  readonly nivelPrecio: NivelPrecio;
}

/**
 * Las redes viajan en su **propio campo**, no dentro del negocio.
 *
 * Meterlas dentro devolvía `201` y perdía el dato en silencio: el alta del
 * negocio no las declara y Jackson descarta lo que el DTO no tiene.
 */
export interface RedesDelRegistro {
  readonly instagram?: string;
  readonly linkedin?: string;
}

/**
 * La cuenta y el negocio, en una sola petición.
 *
 * Los dos primeros pasos son una división visual del formulario, no dos envíos:
 * el backend los crea en una única transacción y por eso un fallo a mitad no
 * deja el correo cogido.
 *
 * **El escaparate ya no viaja aquí.** Desde que cada producto necesita su imagen
 * obligatoria, meterlos exigiría N ficheros en una petición que además es
 * pública, sin sesión con la que subirlos. Se crean justo después, uno a uno,
 * con el token que devuelve esta llamada.
 */
export interface RegistroEmprendedor {
  readonly nombre: string;
  readonly correo: string;
  readonly contrasena: string;
  readonly negocio: NegocioDelRegistro;
  readonly redes?: RedesDelRegistro;
}

/**
 * La respuesta del acceso, más el negocio recién creado.
 *
 * Trae `negocioId` para que quien acaba de registrarse pueda subir sus fotos
 * sin pedir antes `GET /negocios/mio`. El negocio nace `PENDIENTE` (B6).
 */
export interface RespuestaRegistroEmprendedor extends RespuestaAuth {
  readonly negocioId: number;
}

/** Los campos de un producto, sin la imagen, que viaja aparte como fichero. */
export interface DatosDeProducto {
  readonly nombre: string;
  readonly precio: number;
  readonly descripcion?: string;
  readonly disponible: boolean;
}
