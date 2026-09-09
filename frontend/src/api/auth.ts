import { enviar } from './cliente';
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
