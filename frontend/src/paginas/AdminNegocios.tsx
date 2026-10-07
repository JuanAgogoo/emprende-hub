import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { listarNegociosPendientes } from '../api/moderacion';
import { EsqueletoLista } from '../componentes/Esqueleto';
import { Paginacion } from '../componentes/Paginacion';
import { avisoDeNavegacion } from '../avisoDeNavegacion';
import { fecha, localidad, tiempoTranscurrido } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { NegocioEnRevision } from '../types/moderacion';
import type { Pagina } from '../types/pagina';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';

/**
 * Los negocios que esperan su primera revisión (HU-036), los que más llevan
 * esperando primero: el plazo es de tres días hábiles (B5) y es la espera lo
 * que se mira antes que nada.
 */
export function AdminNegocios() {
  useTitulo('Negocios por revisar');
  const aviso = avisoDeNavegacion(useLocation().state);

  const [pagina, setPagina] = useState(0);
  const [carga, setCarga] = useState<EstadoCarga<Pagina<NegocioEnRevision>>>({
    estado: 'CARGANDO',
  });

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    listarNegociosPendientes(pagina)
      .then((datos) => {
        if (vigente) setCarga({ estado: 'EXITO', datos });
      })
      .catch((error: unknown) => {
        const mensaje = error instanceof Error ? error.message : 'No se pudo cargar la cola';
        if (vigente) setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [pagina]);

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-negocios">
      <h2 id="titulo-negocios" className={estilos.tituloSeccion}>
        Negocios por revisar
      </h2>

      {aviso !== null && (
        <p className={estilos.aviso} role="status">
          {aviso}
        </p>
      )}

      <Cola carga={carga} alPaginar={setPagina} />
    </section>
  );
}

interface PropsCola {
  readonly carga: EstadoCarga<Pagina<NegocioEnRevision>>;
  readonly alPaginar: (pagina: number) => void;
}

function Cola({ carga, alPaginar }: PropsCola) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoLista cuantas={3} />
        </div>
      );

    case 'ERROR':
      return (
        <p className={estilos.error} role="alert">
          No se pudo cargar la cola: {carga.mensaje}. Comprueba que la API esté funcionando y
          vuelve a intentarlo.
        </p>
      );

    case 'EXITO': {
      const { content: negocios, totalElements: total } = carga.datos;

      if (total === 0) {
        return (
          <div className={estilos.vacio}>
            <p className={estilos.tituloVacio}>No hay negocios esperando revisión</p>
            <p>Cuando alguien registre uno, aparecerá aquí.</p>
          </div>
        );
      }

      return (
        <>
          {/* El total de la cola, no el de esta página (HU-036). */}
          <p className={estilos.recuento}>
            {total === 1 ? '1 negocio espera revisión' : `${total} negocios esperan revisión`}
          </p>

          <ul className={estilos.lista}>
            {negocios.map((negocio) => (
              <li key={negocio.id} className={estilos.fila}>
                <div className={estilos.filaCuerpo}>
                  <h3 className={estilos.nombre}>{negocio.nombre}</h3>
                  <dl className={estilos.datos}>
                    <div>
                      <dt>Categoría</dt>
                      <dd>{negocio.categoria}</dd>
                    </div>
                    <div>
                      <dt>Localidad</dt>
                      <dd>{localidad(negocio)}</dd>
                    </div>
                    <div>
                      <dt>Registrado</dt>
                      <dd>
                        <time dateTime={negocio.fechaCreacion}>{fecha(negocio.fechaCreacion)}</time>{' '}
                        <span className={estilos.apagado}>
                          ({tiempoTranscurrido(negocio.fechaCreacion)})
                        </span>
                      </dd>
                    </div>
                  </dl>
                </div>

                <Link
                  to={`/admin/negocios/${negocio.id}`}
                  className={estilos.secundario}
                  aria-label={`Revisar ${negocio.nombre}`}
                >
                  Revisar
                </Link>
              </li>
            ))}
          </ul>

          <Paginacion
            pagina={carga.datos.number}
            totalPaginas={carga.datos.totalPages}
            alCambiar={alPaginar}
          />
        </>
      );
    }

    default:
      return casoImposible(carga);
  }
}
