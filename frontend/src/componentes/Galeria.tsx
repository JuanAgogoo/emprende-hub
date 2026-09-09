import { useState } from 'react';
import type { FotoNegocio } from '../types/negocio';
import estilos from './Galeria.module.css';

interface Props {
  readonly fotos: readonly FotoNegocio[];
  readonly nombreNegocio: string;
  /** El mismo nombre que lleva la foto en la tarjeta, para que crezca desde ella. */
  readonly nombreTransicion: string;
}

export function Galeria({ fotos, nombreNegocio, nombreTransicion }: Props) {
  const [elegida, setElegida] = useState(0);

  if (fotos.length === 0) {
    return (
      <div className={estilos.sinFotos}>
        <span className={estilos.inicial} aria-hidden="true">
          {nombreNegocio.charAt(0)}
        </span>
        <p>Este negocio todavía no ha publicado fotos.</p>
      </div>
    );
  }

  // Si el índice se saliera del rango, se cae a la primera en vez de romperse.
  const principal = fotos[elegida] ?? fotos[0];

  return (
    <div className={estilos.galeria}>
      <img
        className={estilos.principal}
        src={principal.url}
        alt={`Foto de ${nombreNegocio}`}
        style={{ viewTransitionName: nombreTransicion }}
        /* Es lo primero que se ve: no se carga en diferido. */
        width={1200}
        height={900}
      />

      {fotos.length > 1 && (
        <ul className={estilos.miniaturas}>
          {fotos.map((foto, posicion) => (
            <li key={foto.id}>
              <button
                type="button"
                className={
                  posicion === elegida ? `${estilos.miniatura} ${estilos.activa}` : estilos.miniatura
                }
                onClick={() => setElegida(posicion)}
                aria-label={`Ver la foto ${posicion + 1} de ${fotos.length}`}
                aria-current={posicion === elegida}
              >
                <img src={foto.url} alt="" loading="lazy" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
