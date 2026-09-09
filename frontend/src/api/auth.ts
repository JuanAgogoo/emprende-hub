import { enviar } from './cliente';
import type {
  RegistroEmprendedor,
  RespuestaRegistroEmprendedor,
} from '../types/registroEmprendedor';
import type { RespuestaAuth } from '../types/sesion';

/**
 * Un único inicio de sesión para todo el mundo: la respuesta trae el rol y es
 * ella la que decide a dónde va cada quien. No hay una pantalla por tipo de
 * usuario.
 */
export function iniciarSesion(correo: string, contrasena: string): Promise<RespuestaAuth> {
  return enviar<RespuestaAuth>('/auth/login', { correo, contrasena });
}

/**
 * Alta de cliente (A1): el registro corto, solo nombre, correo y contraseña.
 *
 * Devuelve `201` con el token, así que quien se registra queda dentro sin pasar
 * por el inicio de sesión. El alta de emprendedor es otra cosa y tiene su propio
 * endpoint.
 */
export function registrarCliente(
  nombre: string,
  correo: string,
  contrasena: string,
): Promise<RespuestaAuth> {
  return enviar<RespuestaAuth>('/auth/registro', { nombre, correo, contrasena });
}

/**
 * Alta de emprendedor con su negocio (A1-ter).
 *
 * No es el registro de cliente con más campos: cuenta, negocio, redes y
 * escaparate entran **en una sola transacción**, así que un fallo a mitad no
 * deja una cuenta creada con el negocio sin registrar.
 *
 * Las fotos se quedan fuera a propósito —son binarios y tienen su endpoint
 * multipart— y se suben después con el token que devuelve esta llamada.
 */
export function registrarEmprendedor(
  peticion: RegistroEmprendedor,
): Promise<RespuestaRegistroEmprendedor> {
  return enviar<RespuestaRegistroEmprendedor>('/auth/registro-emprendedor', peticion);
}
