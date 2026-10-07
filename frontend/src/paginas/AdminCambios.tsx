import { useEffect, useState } from 'react';
import { aprobarCambio, listarCambiosPendientes, rechazarCambio } from '../api/moderacion';
import { DialogoMotivo } from '../componentes/DialogoMotivo';
import { EsqueletoLista } from '../componentes/Esqueleto';
import { FotosPropuestas } from '../componentes/FotosPropuestas';
import { fecha, tiempoTranscurrido } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { CambioPendiente } from '../types/moderacion';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';

/**
 * Las ediciones de negocios ya publicados que esperan revisión (B2-bis).
 *
 * Mientras esperan, el público sigue viendo la versión aprobada. Aquí la
 * comparación entre lo de ahora y lo propuesto **es** la vista previa, así que
 * la decisión se toma en la misma tarjeta.
 */
export function AdminCambios() {
  useTitulo('Cambios por revisar');
  const [carga, setCarga] = useState<EstadoCarga<readonly CambioPendiente[]>>({
    estado: 'CARGANDO',
  });
  /** El negocio sobre el que hay una decisión en curso, para bloquear solo esa tarjeta. */
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [porRechazar, setPorRechazar] = useState<CambioPendiente | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    listarCambiosPendientes()
      .then((cambios) => {
        if (vigente) setCarga({ estado: 'EXITO', datos: cambios });
      })
      .catch((error: unknown) => {
        const mensaje = error instanceof Error ? error.message : 'No se pudo cargar la cola';
        if (vigente) setCarga({ estado: 'ERROR', mensaje });
      });
    return () => {
      vigente = false;
    };
  }, []);

  /** Saca de la lista la propuesta ya resuelta, sin volver a pedirla entera. */
  function quitar(negocioId: number) {
    setCarga((actual) =>
      actual.estado === 'EXITO'
        ? { ...actual, datos: actual.datos.filter((c) => c.negocioId !== negocioId) }
        : actual,
    );
  }

  async function aprobar(cambio: CambioPendiente) {
    setOcupado(cambio.negocioId);
    setFallo(null);
    setAviso(null);
    try {
      await aprobarCambio(cambio.negocioId);
      quitar(cambio.negocioId);
      setAviso(`Publicado el cambio de «${cambio.nombrePropuesto}».`);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo aprobar el cambio');
    } finally {
      setOcupado(null);
    }
  }

  async function rechazar(motivo: string) {
    if (porRechazar === null) return;
    const cambio = porRechazar;
    setOcupado(cambio.negocioId);
    setFallo(null);
    setAviso(null);
    try {
      await rechazarCambio(cambio.negocioId, motivo);
      quitar(cambio.negocioId);
      setPorRechazar(null);
      setAviso(`Descartado el cambio de «${cambio.nombreActual}». Sigue publicado como estaba.`);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo rechazar el cambio');
    } finally {
      setOcupado(null);
    }
  }

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-cambios">
      <h2 id="titulo-cambios" className={estilos.tituloSeccion}>
        Cambios por revisar
      </h2>
      <p className={estilos.entrada}>
        Ediciones de negocios que ya están publicados. Mientras decides, el directorio sigue
        enseñando la versión de antes.
      </p>

      {aviso !== null && (
        <p className={estilos.aviso} role="status">
          {aviso}
        </p>
      )}
      {fallo !== null && porRechazar === null && (
        <p className={estilos.error} role="alert">
          {fallo}
        </p>
      )}

      <Cola carga={carga} ocupado={ocupado} alAprobar={aprobar} alRechazar={setPorRechazar} />

      {porRechazar !== null && (
        <DialogoMotivo
          titulo={`Rechazar el cambio de «${porRechazar.nombreActual}»`}
          texto="El negocio sigue publicado como estaba y las fotos nuevas se descartan. El motivo queda en el historial de moderación."
          etiquetaConfirmar="Rechazar el cambio"
          ocupado={ocupado !== null}
          fallo={fallo}
          alConfirmar={rechazar}
          alCancelar={() => {
            setPorRechazar(null);
            setFallo(null);
          }}
        />
      )}
    </section>
  );
}

interface PropsCola {
  readonly carga: EstadoCarga<readonly CambioPendiente[]>;
  readonly ocupado: number | null;
  readonly alAprobar: (cambio: CambioPendiente) => void;
  readonly alRechazar: (cambio: CambioPendiente) => void;
}

function Cola({ carga, ocupado, alAprobar, alRechazar }: PropsCola) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoLista cuantas={2} />
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
      const cambios = carga.datos;

      if (cambios.length === 0) {
        return (
          <div className={estilos.vacio}>
            <p className={estilos.tituloVacio}>No hay cambios esperando revisión</p>
            <p>Cuando un negocio publicado edite sus datos o suba fotos, aparecerá aquí.</p>
          </div>
        );
      }

      return (
        <>
          <p className={estilos.recuento}>
            {cambios.length === 1
              ? '1 cambio espera revisión'
              : `${cambios.length} cambios esperan revisión`}
          </p>
          <ul className={estilos.lista}>
            {cambios.map((cambio) => (
              <li key={cambio.negocioId} className={estilos.tarjetaCambio}>
                <Comparacion cambio={cambio} />

                <div className={estilos.acciones}>
                  <button
                    type="button"
                    className={estilos.aprobar}
                    disabled={ocupado !== null}
                    onClick={() => alAprobar(cambio)}
                  >
                    <span aria-hidden="true">✓</span>{' '}
                    {ocupado === cambio.negocioId ? 'Publicando…' : 'Aprobar el cambio'}
                  </button>
                  <button
                    type="button"
                    className={estilos.rechazar}
                    disabled={ocupado !== null}
                    onClick={() => alRechazar(cambio)}
                  >
                    <span aria-hidden="true">✕</span> Rechazar
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </>
      );
    }

    default:
      return casoImposible(carga);
  }
}

/**
 * Lo de ahora al lado de lo propuesto, **solo en lo que cambia**: repetir los
 * campos iguales obligaría a buscar la diferencia a ojo.
 */
function Comparacion({ cambio }: { readonly cambio: CambioPendiente }) {
  const cambiaNombre = cambio.nombrePropuesto !== cambio.nombreActual;
  const cambiaDescripcion = cambio.descripcionPropuesta !== cambio.descripcionActual;
  const cambiaCategoria =
    cambio.categoriaPropuesta !== null && cambio.categoriaPropuesta !== cambio.categoriaActual;
  const soloFotos = !cambiaNombre && !cambiaDescripcion && !cambiaCategoria;

  return (
    <div className={estilos.filaCuerpo}>
      <h3 className={estilos.nombre}>{cambio.nombreActual}</h3>
      <p className={estilos.apagado}>
        Propuesto el <time dateTime={cambio.fechaSolicitud}>{fecha(cambio.fechaSolicitud)}</time>{' '}
        ({tiempoTranscurrido(cambio.fechaSolicitud)})
      </p>

      {soloFotos && cambio.fotosPendientes > 0 && (
        <p>Solo sube fotos nuevas: los textos se quedan como están.</p>
      )}

      <dl className={estilos.comparacion}>
        {cambiaNombre && (
          <Fila campo="Nombre" actual={cambio.nombreActual} propuesto={cambio.nombrePropuesto} />
        )}
        {cambiaCategoria && (
          <Fila
            campo="Categoría"
            actual={cambio.categoriaActual}
            propuesto={cambio.categoriaPropuesta ?? ''}
          />
        )}
        {cambiaDescripcion && (
          <Fila
            campo="Descripción"
            actual={cambio.descripcionActual}
            propuesto={cambio.descripcionPropuesta}
          />
        )}
      </dl>

      {cambio.fotosPendientes > 0 && (
        <FotosPropuestas negocioId={cambio.negocioId} nombreNegocio={cambio.nombreActual} />
      )}
    </div>
  );
}

interface PropsFila {
  readonly campo: string;
  readonly actual: string;
  readonly propuesto: string;
}

function Fila({ campo, actual, propuesto }: PropsFila) {
  return (
    <div className={estilos.filaComparacion}>
      <dt>{campo}</dt>
      <dd>
        <span className={estilos.etiquetaComparacion}>Ahora</span>
        <span className={estilos.valorActual}>{actual}</span>
      </dd>
      <dd>
        <span className={estilos.etiquetaComparacion}>Propuesto</span>
        <span className={estilos.valorPropuesto}>{propuesto}</span>
      </dd>
    </div>
  );
}
