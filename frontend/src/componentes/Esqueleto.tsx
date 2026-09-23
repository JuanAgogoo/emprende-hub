import estilos from './Esqueleto.module.css';

interface Props {
  /** Cuántos huecos fingidos se dibujan mientras llegan los datos de verdad. */
  readonly cuantas: number;
}

/**
 * La forma del contenido que viene, no un «Cargando…» ni una rueda girando.
 * Evita que la página dé un salto cuando llegan los datos.
 */
export function EsqueletoTarjetas({ cuantas }: Props) {
  return (
    <div className={estilos.rejilla} aria-hidden="true">
      {/* Los esqueletos son las únicas listas del proyecto con la posición
          como clave: son huecos idénticos y sin identidad, que ni se
          reordenan ni se filtran. */}
      {Array.from({ length: cuantas }, (_, posicion) => (
        <div key={posicion} className={estilos.tarjeta}>
          <div className={estilos.marco} />
          <div className={estilos.cuerpo}>
            <div className={`${estilos.linea} ${estilos.lineaAncha}`} />
            <div className={estilos.linea} />
          </div>
        </div>
      ))}
    </div>
  );
}

/**
 * La forma de una lista de opiniones mientras llega: la fila del autor, la
 * calificación y un par de renglones de comentario.
 */
export function EsqueletoOpiniones({ cuantas }: Props) {
  return (
    <div className={estilos.lista} aria-hidden="true">
      {Array.from({ length: cuantas }, (_, posicion) => (
        <div key={posicion} className={estilos.opinion}>
          <div className={`${estilos.linea} ${estilos.lineaCorta}`} />
          <div className={estilos.linea} />
          <div className={`${estilos.linea} ${estilos.lineaAncha}`} />
        </div>
      ))}
    </div>
  );
}
