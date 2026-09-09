import estilos from './PasosAsistente.module.css';

interface Props {
  /** Los títulos, en orden. El índice + 1 es el número que se enseña. */
  readonly pasos: readonly string[];
  /** El paso activo, contando desde 1. */
  readonly actual: number;
}

/**
 * El progreso del asistente.
 *
 * Es una lista ordenada porque eso es lo que es, y el paso activo lleva
 * `aria-current="step"`. En móvil se enseña «Paso 2 de 4» con una barra, no
 * cuatro círculos apretados; a partir de tablet aparecen los círculos unidos.
 *
 * El paso completado se marca **con un check además del color**: quien no
 * distingue el verde tiene que poder verlo igual.
 */
export function PasosAsistente({ pasos, actual }: Props) {
  const avance = Math.round((actual / pasos.length) * 100);

  return (
    <nav className={estilos.progreso} aria-label="Progreso del registro">
      <p className={estilos.contador}>
        Paso {actual} de {pasos.length}
        <span className={estilos.tituloActual}> · {pasos[actual - 1]}</span>
      </p>

      {/* Decorativa: la información ya la da el texto de arriba. */}
      <div className={estilos.barra} aria-hidden="true">
        <div className={estilos.avance} style={{ width: `${avance}%` }} />
      </div>

      <ol className={estilos.lista}>
        {pasos.map((titulo, posicion) => {
          const numero = posicion + 1;
          const completado = numero < actual;
          const activo = numero === actual;

          return (
            <li
              key={titulo}
              className={`${estilos.paso} ${completado ? estilos.completado : ''} ${
                activo ? estilos.activo : ''
              }`}
              aria-current={activo ? 'step' : undefined}
            >
              <span className={estilos.circulo} aria-hidden="true">
                {completado ? '✓' : numero}
              </span>
              <span className={estilos.titulo}>{titulo}</span>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
