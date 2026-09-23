import estilos from './Estrellas.module.css';

interface Props {
  /** La calificación, de 1 a 5. Se redondea para decidir cuántas se pintan. */
  readonly valor: number;
}

/** Las cinco posiciones. Cada número es la identidad de su estrella, no un índice. */
const POSICIONES = [1, 2, 3, 4, 5];

/**
 * La calificación dibujada, en modo lectura.
 *
 * **Va entera en `aria-hidden`**: «★★★☆☆» no se lee. Quien la use pone el
 * número en texto al lado, y eso es lo que anuncia el lector de pantalla.
 */
export function Estrellas({ valor }: Props) {
  const llenas = Math.round(valor);

  return (
    <span className={estilos.estrellas} aria-hidden="true">
      {POSICIONES.map((posicion) => (
        <span
          key={posicion}
          className={posicion <= llenas ? estilos.llena : estilos.vacia}
        >
          ★
        </span>
      ))}
    </span>
  );
}

interface PropsElegibles {
  /** La elegida, o 0 mientras no se ha elegido ninguna. */
  readonly valor: number;
  readonly alElegir: (valor: number) => void;
  /** El id del mensaje de error o de ayuda, si lo hay. */
  readonly describedBy?: string;
  readonly invalido?: boolean;
}

/**
 * Las mismas estrellas, pero para elegir la calificación.
 *
 * Son **cinco radios de verdad** dentro de su `<fieldset>`, no cinco `<div>`
 * con `onClick`: así el tabulador entra en el grupo, las flechas cambian la
 * nota y el lector de pantalla anuncia «3 de 5» sin una línea de JavaScript
 * para el teclado. Lo que se ve es la estrella; el radio está debajo, y sigue
 * estando para quien no mira la pantalla.
 */
export function EstrellasElegibles({ valor, alElegir, describedBy, invalido }: PropsElegibles) {
  return (
    <fieldset
      className={estilos.grupo}
      aria-describedby={describedBy}
      aria-invalid={invalido === true}
    >
      <legend className={estilos.leyenda}>Tu calificación *</legend>

      <div className={estilos.opciones}>
        {POSICIONES.map((posicion) => (
          <label key={posicion} className={estilos.opcion}>
            <input
              className={estilos.radio}
              type="radio"
              name="calificacion"
              value={posicion}
              checked={valor === posicion}
              onChange={() => alElegir(posicion)}
            />
            <span
              className={posicion <= valor ? estilos.llena : estilos.vacia}
              aria-hidden="true"
            >
              ★
            </span>
            <span className={estilos.textoOculto}>
              {posicion} {posicion === 1 ? 'estrella' : 'estrellas'}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
