import type { Sesion } from './types/sesion';

/**
 * Dónde vive la sesión entre recargas.
 *
 * Está aquí, y no dentro del contexto, porque `api/cliente.ts` también necesita
 * leer el token para la cabecera `Authorization`. Con una sola clave y un solo
 * módulo no hay dos copias que se puedan desincronizar.
 */
const CLAVE = 'emprendehub.sesion';

/** Comprueba la forma de lo leído: `localStorage` es dato externo, no `any`. */
function esSesion(valor: unknown): valor is Sesion {
  if (typeof valor !== 'object' || valor === null) return false;
  const posible = valor as Record<string, unknown>;
  return (
    typeof posible.token === 'string' &&
    typeof posible.nombre === 'string' &&
    typeof posible.correo === 'string' &&
    (posible.rol === 'CLIENTE' || posible.rol === 'EMPRENDEDOR' || posible.rol === 'ADMIN')
  );
}

export function leerSesion(): Sesion | null {
  const guardado = localStorage.getItem(CLAVE);
  if (guardado === null) return null;

  try {
    const leido: unknown = JSON.parse(guardado);
    // Un formato viejo o manipulado se descarta en vez de romper el arranque.
    if (!esSesion(leido)) {
      localStorage.removeItem(CLAVE);
      return null;
    }
    return leido;
  } catch {
    localStorage.removeItem(CLAVE);
    return null;
  }
}

export function guardarSesion(sesion: Sesion): void {
  localStorage.setItem(CLAVE, JSON.stringify(sesion));
}

export function borrarSesion(): void {
  localStorage.removeItem(CLAVE);
}

/** Lo que necesita `api/cliente.ts` para la cabecera. */
export function leerToken(): string | null {
  return leerSesion()?.token ?? null;
}
