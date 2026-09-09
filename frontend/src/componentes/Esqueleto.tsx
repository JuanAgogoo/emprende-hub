import estilos from './Esqueleto.module.css';

interface Props {
  /** Cuántas tarjetas fingidas se dibujan mientras llegan las de verdad. */
  readonly cuantas: number;
}

/**
 * La forma del contenido que viene, no un «Cargando…» ni una rueda girando.
 * Evita que la página dé un salto cuando llegan los datos.
 */
export function EsqueletoTarjetas({ cuantas }: Props) {
  return (
    <div className={estilos.rejilla} aria-hidden="true">
      {/* Única lista del proyecto con la posición como clave: son huecos
          idénticos y sin identidad, que ni se reordenan ni se filtran. */}
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
