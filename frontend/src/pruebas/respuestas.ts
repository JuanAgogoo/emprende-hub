import { vi } from 'vitest';
import type { Sesion } from '../types/sesion';

/**
 * Lo que comparten las pruebas de `api/`: un `fetch` simulado y una sesión
 * guardada. Ninguna prueba habla con el backend.
 */

export const SESION: Sesion = {
  token: 'token-de-prueba',
  nombre: 'Lucía Restrepo',
  correo: 'lucia@emprendehub.co',
  rol: 'EMPRENDEDOR',
};

/** Sustituye `fetch` por uno que responde siempre lo mismo, y lo devuelve. */
export function simularFetch(respuesta: Response) {
  const fetchSimulado = vi.fn<typeof fetch>().mockResolvedValue(respuesta);
  vi.stubGlobal('fetch', fetchSimulado);
  return fetchSimulado;
}

/** Una respuesta JSON con su código de estado. */
export function json(cuerpo: unknown, estado = 200): Response {
  return new Response(JSON.stringify(cuerpo), {
    status: estado,
    headers: { 'Content-Type': 'application/json' },
  });
}

/** Lo que recibió `fetch` en su primera llamada: la URL y las opciones. */
export function primeraLlamada(fetchSimulado: ReturnType<typeof simularFetch>) {
  const [url, opciones] = fetchSimulado.mock.calls[0] ?? [];
  // `cliente.ts` siempre pasa las cabeceras como objeto plano.
  const cabeceras = (opciones?.headers ?? {}) as Record<string, string>;
  return { url, opciones: opciones ?? {}, cabeceras };
}
