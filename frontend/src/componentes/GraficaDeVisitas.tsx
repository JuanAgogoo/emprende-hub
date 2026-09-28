import { useState } from 'react';
import { diaCorto, diaDelMes, fechaCorta, numero } from '../formato';
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

/** Con treinta barras no caben treinta fechas: se rotula una de cada cinco. */
const CADA_CUANTAS_ETIQUETAS = { SEMANA: 1, MES: 5 } as const;

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
          <div key={punto.fecha} className={estilos.columna}>
            <div className={estilos.hueco}>
              {/* El título es el que sale al pasar el ratón, y lo pone el
                  navegador: no hace falta montar un tooltip propio. */}
              <div
                className={estilos.barra}
                style={{ height: `${(punto.visitas / maximo) * 100}%` }}
                title={`${fechaCorta(punto.fecha)}: ${numero(punto.visitas)} visitas`}
              />
            </div>

            <span className={estilos.eje}>
              {posicion % CADA_CUANTAS_ETIQUETAS[periodo] === 0
                ? periodo === 'SEMANA'
                  ? diaCorto(punto.fecha)
                  : diaDelMes(punto.fecha)
                : ''}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
