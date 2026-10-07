import { useEffect, useState } from 'react';
import {
  desestimarDenuncia,
  eliminarOpinionDenunciada,
  listarDenuncias,
} from '../api/moderacion';
import { DialogoMotivo } from '../componentes/DialogoMotivo';
import { EsqueletoLista } from '../componentes/Esqueleto';
import { Estrellas } from '../componentes/Estrellas';
import { Paginacion } from '../componentes/Paginacion';
import { fecha, tiempoTranscurrido } from '../formato';
import type { Denuncia } from '../types/denuncia';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { Pagina } from '../types/pagina';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';

/**
 * Las opiniones denunciadas, las más antiguas primero (HU-040).
 *
 * Dos salidas y las dos quedan en el log: borrar la opinión, con motivo
 * obligatorio porque no se deshace, o desestimar la denuncia y dejarla
 * publicada.
 */
export function AdminDenuncias() {
  useTitulo('Opiniones denunciadas');
  const [pagina, setPagina] = useState(0);
  // Cambiarlo vuelve a pedir la página: borrar una opinión se lleva también
  // las demás denuncias sobre ella, así que quitar una fila no basta.
  const [intento, setIntento] = useState(0);
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Denuncia>>>({ estado: 'CARGANDO' });
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [porEliminar, setPorEliminar] = useState<Denuncia | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });
    listarDenuncias(pagina)
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
  }, [pagina, intento]);

  async function eliminar(motivo: string) {
    if (porEliminar === null) return;
    const denuncia = porEliminar;
    setOcupado(denuncia.id);
    setFallo(null);
    setAviso(null);
    try {
      await eliminarOpinionDenunciada(denuncia.id, motivo);
      setPorEliminar(null);
      setAviso(
        `Se borró la opinión de ${denuncia.autorOpinion} sobre «${denuncia.negocio}». El promedio del negocio ya está recalculado.`,
      );
      setIntento((valor) => valor + 1);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo borrar la opinión');
    } finally {
      setOcupado(null);
    }
  }

  async function desestimar(denuncia: Denuncia) {
    setOcupado(denuncia.id);
    setFallo(null);
    setAviso(null);
    try {
      await desestimarDenuncia(denuncia.id);
      setAviso(
        `Denuncia desestimada: la opinión de ${denuncia.autorOpinion} sigue publicada.`,
      );
      setIntento((valor) => valor + 1);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo desestimar la denuncia');
    } finally {
      setOcupado(null);
    }
  }

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-denuncias">
      <h2 id="titulo-denuncias" className={estilos.tituloSeccion}>
        Opiniones denunciadas
      </h2>
      <p className={estilos.entrada}>
        Mientras decides, la opinión sigue publicada: denunciar no la oculta.
      </p>

      {aviso !== null && (
        <p className={estilos.aviso} role="status">
          {aviso}
        </p>
      )}
      {fallo !== null && porEliminar === null && (
        <p className={estilos.error} role="alert">
          {fallo}
        </p>
      )}

      <Cola
        carga={carga}
        ocupado={ocupado}
        alEliminar={(denuncia) => {
          setFallo(null);
          setPorEliminar(denuncia);
        }}
        alDesestimar={desestimar}
        alPaginar={setPagina}
      />

      {porEliminar !== null && (
        <DialogoMotivo
          titulo={`Borrar la opinión de ${porEliminar.autorOpinion}`}
          texto="Desaparece del perfil del negocio y su promedio se recalcula. No se puede deshacer: el motivo queda en el historial de moderación."
          etiquetaConfirmar="Borrar la opinión"
          ocupado={ocupado !== null}
          fallo={fallo}
          alConfirmar={eliminar}
          alCancelar={() => setPorEliminar(null)}
        />
      )}
    </section>
  );
}

interface PropsCola {
  readonly carga: EstadoCarga<Pagina<Denuncia>>;
  readonly ocupado: number | null;
  readonly alEliminar: (denuncia: Denuncia) => void;
  readonly alDesestimar: (denuncia: Denuncia) => void;
  readonly alPaginar: (pagina: number) => void;
}

function Cola({ carga, ocupado, alEliminar, alDesestimar, alPaginar }: PropsCola) {
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
      const { content: denuncias, totalElements: total } = carga.datos;

      if (total === 0) {
        return (
          <div className={estilos.vacio}>
            <p className={estilos.tituloVacio}>No hay denuncias pendientes</p>
            <p>Cuando alguien denuncie una opinión, aparecerá aquí.</p>
          </div>
        );
      }

      return (
        <>
          <p className={estilos.recuento}>
            {total === 1 ? '1 denuncia pendiente' : `${total} denuncias pendientes`}
          </p>
          <ul className={estilos.lista}>
            {denuncias.map((denuncia) => (
              <li key={denuncia.id} className={estilos.tarjetaCambio}>
                <div className={estilos.filaCuerpo}>
                  <h3 className={estilos.nombre}>{denuncia.negocio}</h3>
                  <dl className={estilos.datos}>
                    <div>
                      <dt>Motivo</dt>
                      <dd>
                        <span className={`${estilos.insignia} ${estilos.insigniaError}`}>
                          {denuncia.motivoDescripcion}
                        </span>
                      </dd>
                    </div>
                    <div>
                      <dt>Denunciada por</dt>
                      <dd>{denuncia.denunciante}</dd>
                    </div>
                    <div>
                      <dt>Fecha</dt>
                      <dd>
                        <time dateTime={denuncia.fecha}>{fecha(denuncia.fecha)}</time>{' '}
                        <span className={estilos.apagado}>
                          ({tiempoTranscurrido(denuncia.fecha)})
                        </span>
                      </dd>
                    </div>
                  </dl>

                  {/* Lo denunciado, entero: decidir si se borra exige leerlo. */}
                  <blockquote className={estilos.cita}>
                    <p className={estilos.citaAutor}>
                      {denuncia.autorOpinion} · <Estrellas valor={denuncia.calificacion} />{' '}
                      <span>{denuncia.calificacion} de 5</span>
                    </p>
                    {denuncia.comentario === null || denuncia.comentario === '' ? (
                      <p className={estilos.apagado}>Sin comentario: solo dejó la nota.</p>
                    ) : (
                      <p>{denuncia.comentario}</p>
                    )}
                  </blockquote>
                </div>

                <div className={estilos.acciones}>
                  <button
                    type="button"
                    className={estilos.rechazar}
                    disabled={ocupado !== null}
                    onClick={() => alEliminar(denuncia)}
                  >
                    <span aria-hidden="true">✕</span> Borrar la opinión
                  </button>
                  <button
                    type="button"
                    className={estilos.secundario}
                    disabled={ocupado !== null}
                    onClick={() => alDesestimar(denuncia)}
                  >
                    {ocupado === denuncia.id ? 'Desestimando…' : 'Desestimar la denuncia'}
                  </button>
                </div>
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
