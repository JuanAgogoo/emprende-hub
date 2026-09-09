/** Los tres roles del backend. */
export type Rol = 'CLIENTE' | 'EMPRENDEDOR' | 'ADMIN';

/**
 * Lo que devuelve el backend al entrar o al registrarse.
 *
 * Trae el nombre y el rol además del token, precisamente para que el cliente no
 * tenga que descodificar el JWT solo para saber a quién ha autenticado.
 */
export interface Sesion {
  readonly token: string;
  readonly nombre: string;
  readonly correo: string;
  readonly rol: Rol;
}

/** La respuesta completa de /auth: la sesión más los datos del token. */
export interface RespuestaAuth extends Sesion {
  readonly tipo: string;
  readonly expiraEnMillis: number;
}
