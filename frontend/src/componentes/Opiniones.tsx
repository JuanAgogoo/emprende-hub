import { useEffect, useState } from 'react';
import { listarOpiniones } from '../api/opiniones';
import { EsqueletoOpiniones } from './Esqueleto';
import { Estrellas } from './Estrellas';
import { Paginacion } from './Paginacion';
import { fecha, numero } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { Opinion } from '../types/opinion';
import type { Pagina } from '../types/pagina';
import estilos from './Opiniones.module.css';

interface Props {
  readonly negocioId: number;
}

/**
 * Cinco por página. El perfil ya es largo, y con cinco el paginador aparece
 * pronto en vez de esconder las opiniones antiguas detrás de un listado
 * interminable.
 */
const POR_PAGINA = 5;

/**
 * El listado de opiniones del perfil público. Se lee sin sesión (C1).
 *
 * Pide su propia página porque no viene dentro del perfil: son dos endpoints
 * distintos y paginar aquí no tiene por qué recargar el negocio entero.
 */
export function Opiniones({ negocioId }: Props) {
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Opinion>>>({ estado: 'CARGANDO' });
  const [pagina, setPagina] = useState(0);
  // Cambiarlo es lo que vuelve a disparar el efecto cuando se pulsa «Reintentar».
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    listarOpiniones(negocioId, pagina, POR_PAGINA)
      .then((datos) => {
        if (vigente) setCarga({ estado: 'EXITO', datos });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        const mensaje =
          error instanceof Error ? error.message : 'No se pudieron cargar las opiniones';
        setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [negocioId, pagina, intento]);

  const total = carga.estado === 'EXITO' ? carga.datos.totalElements : 0;

  return (
    <section className={estilos.bloque}>
      <h2 className={estilos.titulo}>
        Opiniones
        {total > 0 && (
          <span className={estilos.contador}>
            {numero(total)} {total === 1 ? 'opinión' : 'opiniones'}
          </span>
        )}
      </h2>

      <Listado
        carga={carga}
        alPaginar={setPagina}
        alReintentar={() => setIntento((valor) => valor + 1)}
      />
    </section>
  );
}

interface PropsListado {
  readonly carga: EstadoCarga<Pagina<Opinion>>;
  readonly alPaginar: (pagina: number) => void;
  readonly alReintentar: () => void;
}

function Listado({ carga, alPaginar, alReintentar }: PropsListado) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoOpiniones cuantas={3} />
        </div>
      );

    case 'ERROR':
      return (
        <div className={estilos.error} role="alert">
          <p>
            No se pudieron cargar las opiniones: {carga.mensaje}. El resto del perfil sí se ve.
          </p>
          <button type="button" className={estilos.reintentar} onClick={alReintentar}>
            Reintentar
          </button>
        </div>
      );

    case 'EXITO': {
      const pagina = carga.datos;

      // Un negocio recién publicado no tiene ninguna, y eso es lo normal: no es
      // un hueco ni un error.
      if (pagina.content.length === 0) {
        return (
          <p className={estilos.vacio}>
            Todavía nadie ha opinado sobre este negocio.
          </p>
        );
      }

      return (
        <>
          <ul className={estilos.lista}>
            {pagina.content.map((opinion) => (
              <OpinionPublicada key={opinion.id} opinion={opinion} />
            ))}
          </ul>
          <Paginacion
            pagina={pagina.number}
            totalPaginas={pagina.totalPages}
            alCambiar={alPaginar}
          />
        </>
      );
    }

    default:
      return casoImposible(carga);
  }
}

function OpinionPublicada({ opinion }: { readonly opinion: Opinion }) {
  return (
    <li className={estilos.opinion}>
      <div className={estilos.cabecera}>
        <span className={estilos.autor}>{opinion.autor}</span>
        <time className={estilos.fecha} dateTime={opinion.fechaCreacion}>
          {fecha(opinion.fechaCreacion)}
        </time>
      </div>

      <p className={estilos.nota}>
        <Estrellas valor={opinion.calificacion} />
        {/* Las estrellas son decorativas: la nota que se lee es esta. */}
        <span className={estilos.cifra}>{opinion.calificacion} de 5</span>
        {opinion.editada && <span className={estilos.editada}>Editada</span>}
      </p>

      {opinion.comentario !== null && opinion.comentario !== '' && (
        <p className={estilos.comentario}>{opinion.comentario}</p>
      )}
    </li>
  );
}
