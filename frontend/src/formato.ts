/**
 * Formato de números en español de Colombia. Sin librerías: `Intl` es estándar
 * y es lo que sustituye al CurrencyPipe de Angular.
 */

const NUMERO = new Intl.NumberFormat('es-CO');
const CALIFICACION = new Intl.NumberFormat('es-CO', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

export function numero(valor: number): string {
  return NUMERO.format(valor);
}

/**
 * La calificación, o el texto de que todavía no hay ninguna.
 *
 * Se compara con `null` explícitamente y no con `||`, porque un promedio de 0
 * es un valor legítimo que `||` convertiría en «Sin opiniones».
 */
export function calificacion(valor: number | null): string | null {
  return valor === null ? null : CALIFICACION.format(valor);
}

const PRECIO = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  // Los precios llegan en pesos enteros: los centavos solo estorban.
  maximumFractionDigits: 0,
});

/** Un precio en pesos colombianos. Sustituye al CurrencyPipe, sin librería. */
export function precio(valor: number): string {
  return PRECIO.format(valor);
}

const FECHA = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});

/**
 * Una fecha de la API —ISO-8601 con zona— escrita en letra.
 *
 * `Intl` la pasa a la zona de quien mira, que es lo que se quiere: una opinión
 * publicada anoche no puede salir con la fecha de mañana.
 */
export function fecha(valor: string): string {
  return FECHA.format(new Date(valor));
}

/** Los tres niveles de precio de G4, tal como se enseñan. */
export function nivelPrecio(nivel: 'BAJO' | 'MEDIO' | 'ALTO'): string {
  switch (nivel) {
    case 'BAJO':
      return '$';
    case 'MEDIO':
      return '$$';
    case 'ALTO':
      return '$$$';
  }
}
