/**
 * Verifica el contraste de la paleta contra WCAG 2.2. `npm run contraste`.
 *
 * Lee los tokens de src/estilos/tokens.css en vez de repetirlos aquí: si
 * alguien cambia un color, esto lo comprueba con el valor nuevo y no con una
 * copia que envejece.
 *
 * Umbrales: 4,5:1 para texto (1.4.3) y 3:1 para bordes de control y anillos de
 * foco (1.4.11). Los bordes decorativos no entran, porque la regla habla de
 * componentes de interfaz, no de separadores.
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const AQUI = dirname(fileURLToPath(import.meta.url));
const TOKENS = join(AQUI, '..', 'src', 'estilos', 'tokens.css');

function leerTokens(ruta) {
  const css = readFileSync(ruta, 'utf8');
  const colores = new Map();
  for (const [, nombre, valor] of css.matchAll(/--(color-[\w-]+)\s*:\s*(#[0-9a-fA-F]{6})\s*;/g)) {
    colores.set(nombre, valor.toLowerCase());
  }
  return colores;
}

const aLineal = (canal) => {
  const c = canal / 255;
  return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
};

function luminancia(hex) {
  const n = hex.slice(1);
  const [r, g, b] = [0, 2, 4].map((i) => Number.parseInt(n.slice(i, i + 2), 16));
  return 0.2126 * aLineal(r) + 0.7152 * aLineal(g) + 0.0722 * aLineal(b);
}

function ratio(a, b) {
  const [la, lb] = [luminancia(a), luminancia(b)];
  const [alto, bajo] = la > lb ? [la, lb] : [lb, la];
  return (alto + 0.05) / (bajo + 0.05);
}

/** [frente, fondo, para qué, umbral] */
const CASOS = [
  ['color-tinta', 'color-lienzo', 'Titular sobre lienzo', 4.5],
  ['color-tinta', 'color-lienzo-alt', 'Titular sobre lienzo-alt', 4.5],
  ['color-cuerpo', 'color-lienzo', 'Cuerpo sobre lienzo', 4.5],
  ['color-cuerpo', 'color-lienzo-alt', 'Cuerpo sobre lienzo-alt', 4.5],
  ['color-apagado', 'color-lienzo', 'Secundario y placeholder', 4.5],
  ['color-apagado', 'color-lienzo-alt', 'Secundario sobre lienzo-alt', 4.5],
  ['color-sobre-senal', 'color-acento', 'Texto de botón primario', 4.5],
  ['color-sobre-senal', 'color-acento-fuerte', 'Botón primario en hover', 4.5],
  ['color-acento', 'color-lienzo', 'Enlace y botón fantasma', 4.5],
  ['color-acento', 'color-acento-suave', 'Acento sobre acento-suave', 4.5],
  ['color-acento', 'color-lienzo-alt', 'Acento sobre lienzo-alt', 4.5],
  ['color-sobre-senal', 'color-exito', 'Texto sobre éxito', 4.5],
  ['color-sobre-senal', 'color-error', 'Texto sobre error', 4.5],
  ['color-sobre-senal', 'color-aviso', 'Texto sobre aviso', 4.5],
  ['color-error', 'color-lienzo', 'Error de formulario', 4.5],
  ['color-exito', 'color-lienzo', 'Mensaje de éxito', 4.5],
  ['color-aviso', 'color-lienzo', 'Mensaje de aviso', 4.5],
  ['color-borde-control', 'color-lienzo', 'Borde de campo (1.4.11)', 3.0],
  ['color-borde-control', 'color-lienzo-alt', 'Borde de campo sobre gris', 3.0],
  ['color-acento', 'color-lienzo', 'Anillo de foco (1.4.11)', 3.0],
  ['color-acento', 'color-lienzo-alt', 'Anillo de foco sobre gris', 3.0],
];

const colores = leerTokens(TOKENS);
let fallos = 0;

console.log('\n  Contraste de la paleta · WCAG 2.2\n');
console.log(`  ${'Combinación'.padEnd(30)} ${'Ratio'.padStart(7)} ${'Mín'.padStart(5)}  Nivel`);
console.log(`  ${'-'.repeat(30)} ${'-'.repeat(7)} ${'-'.repeat(5)}  ${'-'.repeat(6)}`);

for (const [frente, fondo, uso, umbral] of CASOS) {
  const a = colores.get(frente);
  const b = colores.get(fondo);

  if (a === undefined || b === undefined) {
    console.log(`  ${uso.padEnd(30)}   token ausente: ${a === undefined ? frente : fondo}`);
    fallos += 1;
    continue;
  }

  const r = ratio(a, b);
  const pasa = r >= umbral;
  if (!pasa) fallos += 1;

  const nivel = r >= 7 ? 'AAA' : r >= 4.5 ? 'AA' : r >= 3 ? 'AA UI' : 'FALLA';
  const marca = pasa ? '·' : '×';
  console.log(
    `  ${uso.padEnd(30)} ${r.toFixed(2).padStart(7)} ${umbral.toFixed(1).padStart(5)}  ${marca} ${nivel}`,
  );
}

console.log('');
if (fallos > 0) {
  console.error(`  ${fallos} de ${CASOS.length} combinaciones no llegan al mínimo.\n`);
  process.exit(1);
}
console.log(`  ${CASOS.length} de ${CASOS.length} combinaciones en verde.\n`);
