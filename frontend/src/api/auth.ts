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
