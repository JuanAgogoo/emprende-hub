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
