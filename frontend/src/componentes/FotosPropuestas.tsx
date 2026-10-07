import { useEffect, useState } from 'react';
import { obtenerVistaPrevia } from '../api/moderacion';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { FotoNegocio } from '../types/negocio';
import estilos from './FotosPropuestas.module.css';

interface Props {
  readonly negocioId: number;
  readonly nombreNegocio: string;
}

/**
 * Las fotos nuevas de una propuesta de cambio, para verlas antes de publicarlas.
 *
 * La cola de cambios solo dice cuántas son; las imágenes salen de la vista
 * previa del negocio, que trae todas con su estado. Se quedan las que esperan:
 * las aprobadas ya se ven en el directorio y no hay nada que decidir sobre ellas.
 */
export function FotosPropuestas({ negocioId, nombreNegocio }: Props) {
  const [carga, setCarga] = useState<EstadoCarga<readonly FotoNegocio[]>>({ estado: 'CARGANDO' });

  useEffect(() => {
    let vigente = true;
    obtenerVistaPrevia(negocioId)
      .then((negocio) => {
        const nuevas = negocio.fotos.filter((foto) => foto.estado === 'PENDIENTE');
        if (vigente) setCarga({ estado: 'EXITO', datos: nuevas });
      })
      .catch((error: unknown) => {
        const mensaje = error instanceof Error ? error.message : 'No se pudieron cargar';
        if (vigente) setCarga({ estado: 'ERROR', mensaje });
      });
    return () => {
      vigente = false;
    };
  }, [negocioId]);

  switch (carga.estado) {
    case 'CARGANDO':
      return <div className={estilos.esqueleto} aria-busy="true" />;

    case 'ERROR':
      return (
        <p className={estilos.error} role="alert">
          No se pudieron cargar las fotos nuevas: {carga.mensaje}
        </p>
      );

    case 'EXITO':
      return (
        <figure className={estilos.bloque}>
          <figcaption className={estilos.titulo}>
            {carga.datos.length === 1 ? '1 foto nueva' : `${carga.datos.length} fotos nuevas`}
          </figcaption>
          <ul className={estilos.fotos}>
            {carga.datos.map((foto, posicion) => (
              <li key={foto.id}>
                {/* Se abre a tamaño completo: en miniatura no se juzga una foto. */}
                <a href={foto.url} target="_blank" rel="noopener noreferrer">
                  <img
                    src={foto.url}
                    alt={`Foto nueva ${posicion + 1} de ${nombreNegocio}`}
                    loading="lazy"
                  />
                </a>
              </li>
            ))}
          </ul>
        </figure>
      );

    default:
      return casoImposible(carga);
  }
}
