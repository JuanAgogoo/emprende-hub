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

/** Un artículo del escaparate. El precio viaja como número, en pesos. */
export interface ProductoDelRegistro {
  readonly nombre: string;
  readonly precio: number;
  readonly descripcion?: string;
  readonly disponible: boolean;
}

/**
 * Todo lo que el asistente recoge, en una sola petición.
 *
 * Los tres pasos son una división visual del formulario, no tres envíos: el
 * backend crea cuenta, negocio y escaparate en una única transacción, y por eso
 * un fallo a mitad no deja el correo cogido.
 */
export interface RegistroEmprendedor {
  readonly nombre: string;
  readonly correo: string;
  readonly contrasena: string;
  readonly negocio: NegocioDelRegistro;
  readonly redes?: RedesDelRegistro;
  readonly productos: readonly ProductoDelRegistro[];
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
