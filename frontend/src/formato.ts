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

const RELATIVO = new Intl.RelativeTimeFormat('es-CO', {
  // «ayer» y «la semana pasada» en vez de «hace 1 día» y «hace 1 semana».
  numeric: 'auto',
});

const MINUTO = 60_000;
const HORA = 60 * MINUTO;
const DIA = 24 * HORA;
const SEMANA = 7 * DIA;
const MES = 30 * DIA;

/**
 * Cuánto hace de algo: «hace 5 minutos», «ayer», «la semana pasada».
 *
 * La unidad se elige por el tamaño del hueco, que es como se lee de verdad: a
 * nadie le dice nada «hace 2.880 minutos». El caso raro de una fecha en el
 * futuro —los relojes no siempre coinciden— cae en «ahora mismo» en vez de
 * enseñar un «dentro de» que no tendría sentido en un aviso ya recibido.
 */
export function tiempoTranscurrido(instante: string): string {
  const transcurrido = Date.now() - new Date(instante).getTime();

  if (transcurrido < MINUTO) return 'ahora mismo';
  if (transcurrido < HORA) return RELATIVO.format(-Math.floor(transcurrido / MINUTO), 'minute');
  if (transcurrido < DIA) return RELATIVO.format(-Math.floor(transcurrido / HORA), 'hour');
  if (transcurrido < SEMANA) return RELATIVO.format(-Math.floor(transcurrido / DIA), 'day');
  if (transcurrido < MES) return RELATIVO.format(-Math.floor(transcurrido / SEMANA), 'week');
  return RELATIVO.format(-Math.floor(transcurrido / MES), 'month');
}

const DIA_CORTO = new Intl.DateTimeFormat('es-CO', { weekday: 'short' });
const FECHA_CORTA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short' });

/**
 * Un día suelto de la API (`YYYY-MM-DD`) como fecha local.
 *
 * **No vale `new Date('2026-08-30')`**: eso se interpreta como medianoche UTC,
 * que en Colombia es el día anterior a las siete de la tarde, y la gráfica
 * enseñaría cada barra corrida un día. Construyéndola por partes, el día es el
 * que dice la cadena.
 */
function comoDiaLocal(dia: string): Date {
  const [anio, mes, numeroDeDia] = dia.split('-').map(Number);
  return new Date(anio, mes - 1, numeroDeDia);
}

/** El día de la semana, abreviado: «lun», «mar». */
export function diaCorto(dia: string): string {
  return DIA_CORTO.format(comoDiaLocal(dia));
}

/** El día y el mes: «30 de ago». Para decir de cuándo a cuándo va algo. */
export function fechaCorta(dia: string): string {
  return FECHA_CORTA.format(comoDiaLocal(dia));
}

const VARIACION = new Intl.NumberFormat('es-CO', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
  // El signo se enseña también cuando sube: «+12,2 %» dice más que «12,2 %».
  signDisplay: 'exceptZero',
});

/**
 * Una variación porcentual, con su signo.
 *
 * El backend manda 12.2 queriendo decir 12,2 %, así que **no se usa
 * `style: 'percent'`**, que multiplicaría por cien y enseñaría 1220 %.
 *
 * No trata el nulo a propósito: «no hay con qué comparar» se dice distinto en
 * cada sitio, así que lo decide quien lo dibuja y no el formato.
 */
export function variacion(valor: number): string {
  return `${VARIACION.format(valor)} %`;
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

/** «Medellín» o «El Poblado, Medellín»: el barrio solo existe en Medellín (G3). */
export function localidad(lugar: { readonly ciudad: string; readonly barrio: string | null }) {
  return lugar.barrio === null ? lugar.ciudad : `${lugar.barrio}, ${lugar.ciudad}`;
}

const FECHA_Y_HORA = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
});

/** Una fecha con su hora, en la zona de quien mira: «6 oct 2026, 9:24 p. m.». */
export function fechaYHora(valor: string): string {
  return FECHA_Y_HORA.format(new Date(valor));
}
