import { useState } from 'react';
import { diaCorto, fechaCorta, numero } from '../formato';
import type { PuntoSerie } from '../types/metricas';
import estilos from './GraficaDeVisitas.module.css';

interface Props {
  /** Los treinta días que ya trae la respuesta de métricas, el más viejo primero. */
  readonly serie: readonly PuntoSerie[];
}

/** Las dos vistas que pide la historia. */
type Periodo = 'SEMANA' | 'MES';

/** Cuántos días enseña cada una. Son las mismas ventanas que usan las tarjetas. */
const DIAS = { SEMANA: 7, MES: 30 } as const;

/**
 * Las visitas por día, en barras.
 *
 * **Los datos ya están**: la serie viene en la misma respuesta que las cifras,
 * así que cambiar de semana a mes no pide nada al servidor ni recarga nada, solo
 * enseña más o menos días de lo que ya hay en memoria.
 *
 * Son barras de CSS, no una librería de gráficas: treinta rectángulos con su
 * altura en porcentaje se explican en una frase, y una dependencia nueva habría
 * que defenderla.
 */
export function GraficaDeVisitas({ serie }: Props) {
  const [periodo, setPeriodo] = useState<Periodo>('SEMANA');

  // La serie llega del día más viejo al más reciente, así que la semana son los
  // últimos siete.
  const puntos = serie.slice(-DIAS[periodo]);
  const maximo = Math.max(...puntos.map((punto) => punto.visitas), 1);
  const total = puntos.reduce((suma, punto) => suma + punto.visitas, 0);

  const desde = puntos.at(0);
  const hasta = puntos.at(-1);

  return (
    <div className={estilos.grafica}>
      <fieldset className={estilos.selector}>
        <legend className={estilos.leyenda}>Periodo</legend>

        <div className={estilos.opciones}>
          {(['SEMANA', 'MES'] as const).map((cual) => (
            <label
              key={cual}
              className={periodo === cual ? `${estilos.opcion} ${estilos.elegida}` : estilos.opcion}
            >
              {/* Radios de verdad: el tabulador entra una vez en el grupo y las
                  flechas cambian de vista, sin JavaScript para el teclado. */}
              <input
                className={estilos.radio}
                type="radio"
                name="periodo-de-la-grafica"
                value={cual}
                checked={periodo === cual}
                onChange={() => setPeriodo(cual)}
              />
              {cual === 'SEMANA' ? 'Semana' : 'Mes'}
            </label>
          ))}
        </div>
      </fieldset>

      {desde !== undefined && hasta !== undefined && (
        <p className={estilos.resumen}>
          {fechaCorta(desde.fecha)} – {fechaCorta(hasta.fecha)} <span aria-hidden="true">·</span>{' '}
          {numero(total)} visitas <span aria-hidden="true">·</span> máximo {numero(maximo)} en un
          día
        </p>
      )}

      {/* El dibujo es una imagen con su descripción: quien no lo ve se queda con
          el resumen de arriba y con este texto, no con treinta divs sueltos. */}
      <div
        className={estilos.barras}
        role="img"
        aria-label={`Visitas por día de los últimos ${DIAS[periodo]} días, ${numero(
          total,
        )} en total y ${numero(maximo)} como mucho en un día`}
      >
        {puntos.map((punto, posicion) => (
          // El globo sale encima de su barra, pero lo enciende el ratón sobre
          // la columna entera: en un día flojo la barra son dos píxeles y
          // habría que acertarle.
          <div key={punto.fecha} className={estilos.columna}>
            <div
              className={estilos.barra}
              style={{ height: `${(punto.visitas / maximo) * 100}%` }}
            >
              <span className={`${estilos.globo} ${estilos[anclaje(posicion, puntos.length)]}`}>
                {fechaCorta(punto.fecha)} <span aria-hidden="true">·</span>{' '}
                {numero(punto.visitas)} {punto.visitas === 1 ? 'visita' : 'visitas'}
              </span>
            </div>
          </div>
        ))}
      </div>

      <Eje periodo={periodo} puntos={puntos} />
    </div>
  );
}

interface PropsEje {
  readonly periodo: Periodo;
  readonly puntos: readonly PuntoSerie[];
}

/**
 * De qué lado se ancla el globo.
 *
 * Centrado se sale de la tarjeta en las dos primeras columnas y en las dos
 * últimas, porque mide más que una columna. En los extremos se pega a su borde.
 */
function anclaje(posicion: number, cuantas: number): 'globoIzquierda' | 'globoDerecha' | 'globoCentrado' {
  if (posicion < 2) return 'globoIzquierda';
  if (posicion > cuantas - 3) return 'globoDerecha';
  return 'globoCentrado';
}

/**
 * El eje de abajo, que cambia con la vista.
 *
 * En la semana caben los siete días, uno bajo su barra. **En el mes no caben
 * treinta**, y salpicar números sueltos cada cinco barras ensucia el dibujo más
 * de lo que informa: se ponen tres fechas —principio, mitad y final—, que es lo
 * que hace falta para saber de cuándo a cuándo va lo que se está mirando.
 */
function Eje({ periodo, puntos }: PropsEje) {
  if (periodo === 'SEMANA') {
    return (
      <div className={estilos.eje}>
        {puntos.map((punto) => (
          <span key={punto.fecha} className={estilos.marca}>
            {diaCorto(punto.fecha)}
          </span>
        ))}
      </div>
    );
  }

  const primero = puntos.at(0);
  const medio = puntos.at(Math.floor(puntos.length / 2));
  const ultimo = puntos.at(-1);

  if (primero === undefined || medio === undefined || ultimo === undefined) return null;

  return (
    <div className={estilos.ejeAmplio}>
      <span>{fechaCorta(primero.fecha)}</span>
      <span>{fechaCorta(medio.fecha)}</span>
      <span>{fechaCorta(ultimo.fecha)}</span>
    </div>
  );
}
