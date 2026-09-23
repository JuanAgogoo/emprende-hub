/**
 * El único sitio del proyecto donde se escribe `fetch`.
 *
 * Concentra tres cosas que si no acaban repetidas por todas partes: la URL
 * base, la cabecera con el token y la lectura del error que devuelve el
 * GlobalExceptionHandler del backend.
 */

import { leerToken } from '../almacenSesion';

/** Prefijo de la API. En desarrollo lo reescribe el proxy de Vite. */
const BASE = import.meta.env.VITE_API_URL ?? '/api/v1';

/**
 * Un error de la API, ya legible.
 *
 * `porCampo` trae una clave por campo inválido cuando la validación falla,
 * que es como responde el backend. Los formularios lo pintan junto al campo
 * en vez de en un cartel genérico.
 */
export class ErrorApi extends Error {
  readonly estado: number;
  readonly porCampo: Readonly<Record<string, string>>;

  constructor(estado: number, mensaje: string, porCampo: Record<string, string> = {}) {
    super(mensaje);
    this.name = 'ErrorApi';
    this.estado = estado;
    this.porCampo = porCampo;
  }
}

/** Las claves que el backend manda siempre, y que no son un campo del formulario. */
const CLAVES_FIJAS = new Set(['timestamp', 'status', 'error', 'message', 'path']);

/**
 * El cuerpo del error llega como JSON, pero es dato externo: se recorre con
 * `unknown` y se comprueba antes de usarlo, nunca con `any`.
 */
function leerError(estado: number, cuerpo: unknown): ErrorApi {
  if (typeof cuerpo !== 'object' || cuerpo === null) {
    return new ErrorApi(estado, `Error ${estado} al llamar a la API`);
  }

  const datos = cuerpo as Record<string, unknown>;
  const porCampo: Record<string, string> = {};

  for (const [clave, valor] of Object.entries(datos)) {
    if (!CLAVES_FIJAS.has(clave) && typeof valor === 'string') {
      porCampo[clave] = valor;
    }
  }

  const mensaje =
    typeof datos.message === 'string' && datos.message.length > 0
      ? datos.message
      : (Object.values(porCampo)[0] ?? `Error ${estado} al llamar a la API`);

  return new ErrorApi(estado, mensaje, porCampo);
}

interface Opciones {
  readonly metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  readonly cuerpo?: unknown;
  /** Para `multipart/form-data`: se envía tal cual y sin Content-Type. */
  readonly formulario?: FormData;
  /** Añade el token si hay sesión. */
  readonly conSesion?: boolean;
}

async function peticion<T>(ruta: string, opciones: Opciones = {}): Promise<T> {
  const { metodo = 'GET', cuerpo, formulario, conSesion = false } = opciones;

  const cabeceras: Record<string, string> = {};
  if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';

  if (conSesion) {
    const token = leerToken();
    // El navegador pone solo el Content-Type del multipart, con su boundary.
    if (token !== null) cabeceras.Authorization = `Bearer ${token}`;
  }

  const respuesta = await fetch(`${BASE}${ruta}`, {
    method: metodo,
    headers: cabeceras,
    body: formulario ?? (cuerpo === undefined ? undefined : JSON.stringify(cuerpo)),
  });

  if (!respuesta.ok) {
    const texto = await respuesta.text();
    let cuerpoError: unknown = null;
    try {
      cuerpoError = texto.length > 0 ? JSON.parse(texto) : null;
    } catch {
      cuerpoError = null;
    }
    throw leerError(respuesta.status, cuerpoError);
  }

  // 204 y compañía: no hay cuerpo que leer. El 200 de `/auth/recuperacion`
  // tampoco lo trae, así que se mira el texto en vez de fiarse del código:
  // `json()` sobre una respuesta vacía revienta con un error de sintaxis.
  if (respuesta.status === 204) return undefined as T;

  const texto = await respuesta.text();
  return (texto.length === 0 ? undefined : JSON.parse(texto)) as T;
}

export function obtener<T>(ruta: string, conSesion = false): Promise<T> {
  return peticion<T>(ruta, { conSesion });
}

export function enviar<T>(ruta: string, cuerpo: unknown, conSesion = false): Promise<T> {
  return peticion<T>(ruta, { metodo: 'POST', cuerpo, conSesion });
}

/**
 * Sube un `multipart/form-data`.
 *
 * No lleva `Content-Type`: lo pone el navegador con su `boundary`, y escribirlo
 * a mano deja la petición sin él y el backend sin saber dónde acaba cada parte.
 */
export function enviarFormulario<T>(
  ruta: string,
  formulario: FormData,
  conSesion = false,
): Promise<T> {
  return peticion<T>(ruta, { metodo: 'POST', formulario, conSesion });
}

/** Sustituye entero algo que ya existe. El backend devuelve cómo quedó. */
export function actualizar<T>(ruta: string, cuerpo: unknown, conSesion = false): Promise<T> {
  return peticion<T>(ruta, { metodo: 'PUT', cuerpo, conSesion });
}

/** Cambia una propiedad de algo que ya existe, sin sustituirlo entero. */
export function parchear<T>(ruta: string, cuerpo: unknown, conSesion = false): Promise<T> {
  return peticion<T>(ruta, { metodo: 'PATCH', cuerpo, conSesion });
}

/** El backend responde `204` sin cuerpo, que `peticion` ya contempla. */
export function eliminar(ruta: string, conSesion = false): Promise<void> {
  return peticion<void>(ruta, { metodo: 'DELETE', conSesion });
}

/**
 * Convierte los filtros en cadena de consulta. `URLSearchParams` codifica los
 * acentos, que es lo que evita el 400 de `?texto=café`.
 */
export function consulta(filtros: Readonly<Record<string, string | number | undefined>>): string {
  const parametros = new URLSearchParams();
  for (const [clave, valor] of Object.entries(filtros)) {
    if (valor !== undefined && valor !== '') parametros.set(clave, String(valor));
  }
  const cadena = parametros.toString();
  return cadena.length > 0 ? `?${cadena}` : '';
}
